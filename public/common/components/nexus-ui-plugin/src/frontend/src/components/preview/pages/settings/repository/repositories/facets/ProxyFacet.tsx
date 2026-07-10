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
  yum: 'e.g., https://mirror.stream.centos.org/',
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
    if (value === '' || value === '-') {
      onNestedChange('proxy', { contentMaxAge: undefined });
    } else {
      const numValue = parseInt(value, 10);
      if (!isNaN(numValue)) {
        // Clamp values below -1 to -1 (minimum allowed value)
        onNestedChange('proxy', { contentMaxAge: Math.max(-1, numValue) });
      }
    }
  };

  const handleMetadataMaxAgeChange = (value: string) => {
    // Mirrors handleContentMaxAgeChange — same semantics for both fields:
    // - allow empty / lone '-' so the user can clear and retype
    // - clamp values < -1 to -1 (the lower bound; -1 means "cache forever")
    if (value === '' || value === '-') {
      onNestedChange('proxy', { metadataMaxAge: undefined });
    } else {
      const numValue = parseInt(value, 10);
      if (!isNaN(numValue)) {
        onNestedChange('proxy', { metadataMaxAge: Math.max(-1, numValue) });
      }
    }
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
    <SettingsFormSection title={UIStrings.PROXY.SECTION.title}>
      <SettingsTextInput
        name="proxy-remoteUrl"
        label={UIStrings.PROXY.REMOTE_STORAGE.label}
        value={formData.proxy?.remoteUrl || ''}
        onChange={handleRemoteUrlChange}
        error={errors?.proxy?.remoteUrl}
        required
        placeholder={UIStrings.PROXY.REMOTE_STORAGE.placeholder}
        helpText={UIStrings.PROXY.REMOTE_STORAGE.helpText(urlExample)}
      />

      {originChangeWarning && (
        <SettingsAlert type="warning">
          {UIStrings.PROXY.ORIGIN_CHANGE_WARNING}
        </SettingsAlert>
      )}

      {showTrustStore && (
        <Box>
          <SettingsCheckbox
            name="httpClient-useTrustStore"
            label={UIStrings.PROXY.TRUST_STORE.label}
            checked={formData.httpClient?.connection?.useTrustStore ?? false}
            onChange={(v) => handleConnectionFieldChange('useTrustStore', v)}
            description={UIStrings.PROXY.TRUST_STORE.description}
          />
          <Box mt="2" ml="6">
            <SettingsButton
              variant="secondary"
              size="small"
              onClick={() => setShowCertDialog(true)}
              icon={ShieldCheck}
            >
              {UIStrings.PROXY.TRUST_STORE.viewCertificate}
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
              {UIStrings.PROXY.PREEMPTIVE_PULL.label}
            </Text>
            <Text size="2" color="gray" as="div" mb="3">
              {UIStrings.PROXY.PREEMPTIVE_PULL.description}
            </Text>
            <SettingsCheckbox
              name="replication-preemptivePullEnabled"
              label={UIStrings.PROXY.PREEMPTIVE_PULL.enabledCheckbox}
              checked={preemptivePullEnabled}
              onChange={handlePreemptivePullChange}
            />
          </Box>

          <SettingsTextInput
            name="replication-assetPathRegex"
            label={UIStrings.PROXY.ASSET_NAME_MATCHER.label}
            value={formData.replication?.assetPathRegex || ''}
            onChange={handleAssetPathRegexChange}
            disabled={!preemptivePullEnabled}
            helpText={UIStrings.PROXY.ASSET_NAME_MATCHER.helpText}
          />
        </>
      )}

      <SettingsCheckbox
        name="proxy-preserveEncodedCharacters"
        label={UIStrings.PROXY.PRESERVE_ENCODED_CHARACTERS.label}
        checked={formData.proxy?.preserveEncodedCharacters ?? false}
        onChange={(checked) => onNestedChange('proxy', { preserveEncodedCharacters: checked })}
        description={UIStrings.PROXY.PRESERVE_ENCODED_CHARACTERS.description}
      />

      <Box mt="2" mb="2">
        <Text size="2" weight="medium" as="div" mb="2">
          {UIStrings.PROXY.BLOCKING.sectionLabel}
        </Text>
        <SettingsCheckbox
          name="httpClient-blocked"
          label={UIStrings.PROXY.BLOCKING.BLOCKED.label}
          checked={httpClient.blocked ?? false}
          onChange={(checked) => onNestedChange('httpClient', { blocked: checked })}
          description={UIStrings.PROXY.BLOCKING.BLOCKED.description}
        />
        <SettingsCheckbox
          name="httpClient-autoBlock"
          label={UIStrings.PROXY.BLOCKING.AUTO_BLOCK.label}
          checked={httpClient.autoBlock ?? true}
          onChange={(checked) => onNestedChange('httpClient', { autoBlock: checked })}
          description={UIStrings.PROXY.BLOCKING.AUTO_BLOCK.description}
        />
      </Box>

      <SettingsTextInput
        name="proxy-contentMaxAge"
        label={UIStrings.PROXY.CONTENT_MAX_AGE.label}
        value={formData.proxy?.contentMaxAge?.toString() ?? ''}
        onChange={handleContentMaxAgeChange}
        type="number"
        helpText={UIStrings.PROXY.CONTENT_MAX_AGE.helpText}
        error={errors?.proxy?.contentMaxAge}
      />

      <SettingsTextInput
        name="proxy-metadataMaxAge"
        label={UIStrings.PROXY.METADATA_MAX_AGE.label}
        value={formData.proxy?.metadataMaxAge?.toString() ?? ''}
        onChange={handleMetadataMaxAgeChange}
        type="number"
        helpText={UIStrings.PROXY.METADATA_MAX_AGE.helpText}
        error={errors?.proxy?.metadataMaxAge}
      />
    </SettingsFormSection>
  );
}

export default ProxyFacet;

