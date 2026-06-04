/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React, { useMemo, useState } from 'react';
import { Box, Text } from '@radix-ui/themes';
import { ShieldCheck } from 'lucide-react';

import { ExtJS } from '../../../../../../../interface/ExtJS';

import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsCheckbox,
  SettingsAlert,
  SettingsButton,
} from '../../../../../shared/form';

import { CertificateViewDialog } from './CertificateViewDialog';

import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

import {
  RepositoryFormData,
  RepositoryFormErrors,
} from '../types';

const EDITOR = {
  ENABLED_CHECKBOX_DESCR: 'Enabled',
  PREEMPTIVE_PULL_LABEL: 'Pre-emptive Pull',
  PREEMPTIVE_PULL_SUBLABEL: 'If enabled, the remote storage will be monitored for changes, and new components will be replicated automatically, and cached locally',
  ASSET_NAME_LABEL: 'Asset Name Matcher',
  ASSET_NAME_DESCRIPTION: 'Enter a regular expression to match asset names. When left blank, all assets are matched.',
  BLOCKING_LABEL: 'Blocking',
  BLOCK_DESCR: 'Block outbound connections to the repository',
  AUTO_BLOCK_DESCR: 'Auto-block outbound connections to the repository if remote peer is detected as unreachable/unresponsive',
};

const REPLICATION_FEATURE = 'replicationFeatureEnabled';
const REPLICATION_FORMATS = 'replicationSupportedFormats';

interface ProxyFacetProps {
  formData: RepositoryFormData;
  onChange: (updates: Partial<RepositoryFormData>) => void;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  format?: string;
  originChangeWarning?: boolean;
}

// Remote URL examples by format
const REMOTE_URL_EXAMPLES: Record<string, string> = {
  maven2: 'e.g., https://repo1.maven.org/maven2/',
  npm: 'e.g., https://registry.npmjs.org/',
  nuget: 'e.g., https://api.nuget.org/v3/index.json',
  pypi: 'e.g., https://pypi.org/',
  docker: 'e.g., https://registry-1.docker.io',
  raw: 'e.g., https://example.com/files/',
  yum: 'e.g., http://mirror.centos.org/centos/',
  ansiblegalaxy: 'e.g., https://galaxy.ansible.com',
  default: 'e.g., https://example.com/repository/',
};

/**
 * ProxyFacet - Remote URL and proxy settings
 */
export function ProxyFacet({
  formData,
  onChange,
  onNestedChange,
  errors,
  format,
  originChangeWarning,
}: ProxyFacetProps) {
  const [showCertDialog, setShowCertDialog] = useState(false);

  const handleRemoteUrlChange = (value: string) => {
    onNestedChange('proxy', { remoteUrl: value });
    if (!value.startsWith('https://')) {
      onNestedChange('httpClient', {
        connection: { ...(formData.httpClient?.connection ?? {}), useTrustStore: false },
      });
    }
  };

  const handleContentMaxAgeChange = (value: string) => {
    const numValue = parseInt(value, 10);
    onNestedChange('proxy', { contentMaxAge: isNaN(numValue) ? -1 : numValue });
  };

  const handleMetadataMaxAgeChange = (value: string) => {
    const numValue = parseInt(value, 10);
    onNestedChange('proxy', { metadataMaxAge: isNaN(numValue) ? 1440 : numValue });
  };

  const handlePreemptivePullChange = (checked: boolean) => {
    onNestedChange('replication', { preemptivePullEnabled: checked });
  };

  const handleConnectionFieldChange = (field: string, value: string | number | boolean) => {
    onNestedChange('httpClient', {
      connection: { ...formData.httpClient?.connection, [field]: value },
    });
  };

  const handleAssetPathRegexChange = (value: string) => {
    onNestedChange('replication', { assetPathRegex: value });
  };

  const urlExample = format ? REMOTE_URL_EXAMPLES[format] || REMOTE_URL_EXAMPLES.default : REMOTE_URL_EXAMPLES.default;
  const remoteUrl = formData.proxy?.remoteUrl || '';
  const showTrustStore = remoteUrl.startsWith('https://');

  const httpClient = useMemo(() => formData.httpClient || {
    blocked: false,
    autoBlock: true,
    connection: null,
    authentication: null,
  }, [formData.httpClient]);

  // Check if replication feature is enabled — feature flags don't change during lifetime
  const isReplicationEnabled = useMemo(() => {
    try {
      return ExtJS.state().getValue(REPLICATION_FEATURE) || false;
    } catch {
      return false;
    }
  }, []);

  // Check if format supports replication — feature flags don't change during lifetime
  const formatReplicationSupported = useMemo(() => {
    try {
      const supportedFormats = ExtJS.state().getValue(REPLICATION_FORMATS);
      return (supportedFormats instanceof Array) && format && supportedFormats.includes(format);
    } catch {
      return false;
    }
  }, [format]);

  const preemptivePullEnabled = formData.replication?.preemptivePullEnabled || false;

  return (
    <SettingsFormSection title="Proxy">
      <SettingsTextInput
        name="proxy-remoteUrl"
        label="Remote Storage"
        value={formData.proxy?.remoteUrl || ''}
        onChange={handleRemoteUrlChange}
        error={errors?.proxy?.remoteUrl}
        required
        placeholder="https://"
        helpText={`Location of the remote repository being proxied. ${urlExample}`}
      />

      {originChangeWarning && (
        <SettingsAlert type="warning">
            Remote URL has changed. Authentication credentials have been reset and must be re-entered.
        </SettingsAlert>
      )}

      {showTrustStore && (
        <Box>
          <SettingsCheckbox
            name="httpClient-useTrustStore"
            label="Use the Nexus Repository truststore"
            checked={formData.httpClient?.connection?.useTrustStore ?? false}
            onChange={(v) => handleConnectionFieldChange('useTrustStore', v)}
            description="Use certificates stored in the Nexus Repository truststore to connect to external systems"
          />
          <Box mt="2" ml="6">
            <SettingsButton
              variant="secondary"
              size="small"
              onClick={() => setShowCertDialog(true)}
              icon={ShieldCheck}
            >
              View Certificate
            </SettingsButton>
          </Box>
        </Box>
      )}

      {showCertDialog && (
        <CertificateViewDialog
          remoteUrl={remoteUrl}
          onClose={() => setShowCertDialog(false)}
        />
      )}

      {isReplicationEnabled && formatReplicationSupported && (
        <>
          <Box mt="4">
            <Text size="2" weight="medium" as="div" mb="2">
              {EDITOR.PREEMPTIVE_PULL_LABEL}
            </Text>
            <Text size="2" color="gray" as="div" mb="3">
              {EDITOR.PREEMPTIVE_PULL_SUBLABEL}
            </Text>
            <SettingsCheckbox
              name="replication-preemptivePullEnabled"
              label={EDITOR.ENABLED_CHECKBOX_DESCR}
              checked={preemptivePullEnabled}
              onChange={handlePreemptivePullChange}
            />
          </Box>

          <SettingsTextInput
            name="replication-assetPathRegex"
            label={EDITOR.ASSET_NAME_LABEL}
            value={formData.replication?.assetPathRegex || ''}
            onChange={handleAssetPathRegexChange}
            disabled={!preemptivePullEnabled}
            helpText={EDITOR.ASSET_NAME_DESCRIPTION}
          />
        </>
      )}

      <Box mt="2" mb="2">
        <Text size="2" weight="medium" as="div" mb="2">
          {EDITOR.BLOCKING_LABEL}
        </Text>
        <SettingsCheckbox
          name="httpClient-blocked"
          label="Blocked"
          checked={httpClient.blocked ?? false}
          onChange={(checked) => onNestedChange('httpClient', { blocked: checked })}
          description={EDITOR.BLOCK_DESCR}
        />
        <SettingsCheckbox
          name="httpClient-autoBlock"
          label="Auto blocking enabled"
          checked={httpClient.autoBlock ?? true}
          onChange={(checked) => onNestedChange('httpClient', { autoBlock: checked })}
          description={EDITOR.AUTO_BLOCK_DESCR}
        />
      </Box>

      <SettingsTextInput
        name="proxy-contentMaxAge"
        label="Maximum Component Age"
        value={String(formData.proxy?.contentMaxAge ?? -1)}
        onChange={handleContentMaxAgeChange}
        type="number"
        helpText="How long (in minutes) to cache artifacts before rechecking the remote repository. Set to -1 to disable caching."
      />

      <SettingsTextInput
        name="proxy-metadataMaxAge"
        label="Maximum Metadata Age"
        value={String(formData.proxy?.metadataMaxAge ?? 1440)}
        onChange={handleMetadataMaxAgeChange}
        type="number"
        helpText="How long (in minutes) to cache metadata before rechecking the remote repository."
      />
    </SettingsFormSection>
  );
}

export default ProxyFacet;

