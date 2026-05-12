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

import React, { useState, useRef } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Shield, Upload, CheckCircle, XCircle, Loader } from 'lucide-react';
import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsSelect,
  SettingsCheckbox,
  SettingsAlert,
  SettingsButton,
} from '../../../shared/form';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { useS3DropdownValues, useAzureConnectionTest } from './useBlobStores';
import type { BlobStoreFormData, S3BlobStoreConfig, AzureBlobStoreConfig } from './types';
import { BLOB_STORE_TYPE_IDS } from './blobStoreFormMachine';
import type { BlobStoreTypeId } from './BlobStoreTypeSelector';

const AUTH_OPTIONS_AZURE = [
  { value: 'ENVIRONMENTVARIABLE', label: 'Use Environment Variables' },
  { value: 'MANAGEDIDENTITY', label: 'Managed Identity (System)' },
  { value: 'ACCOUNTKEY', label: 'Account Key' },
];

const AUTH_OPTIONS_GOOGLE = [
  { value: 'applicationDefault', label: 'Application Default Credentials' },
  { value: 'accountKey', label: 'Credential JSON file' },
];

export interface BlobStoreWizardStepCredentialsProps {
  data: BlobStoreFormData;
  selectedType: BlobStoreTypeId;
  onChange: (path: string, value: unknown) => void;
}

export function BlobStoreWizardStepCredentials({
  data,
  selectedType,
  onChange,
}: BlobStoreWizardStepCredentialsProps) {
  const { values: s3Dropdown, loading: s3Loading } = useS3DropdownValues();
  const { testing: azureTesting, result: azureResult, testConnection, reset } = useAzureConnectionTest();
  const [googleFileName, setGoogleFileName] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const sec = (data.bucketConfiguration as S3BlobStoreConfig)?.bucketSecurity || {};
  const enc = (data.bucketConfiguration as S3BlobStoreConfig)?.encryption;
  const azureConfig = (data.bucketConfiguration || {}) as AzureBlobStoreConfig;
  const azureAuth = azureConfig.authentication || { authenticationMethod: 'ENVIRONMENTVARIABLE' };
  const gConfig = data.bucketConfiguration as Record<string, unknown>;
  const gSecurity = (gConfig?.bucketSecurity || {}) as Record<string, string>;
  const gEnc = (gConfig?.encryption || {}) as Record<string, string>;
  const encryptionTypes = s3Dropdown?.encryptionTypes || [];

  if (selectedType === BLOB_STORE_TYPE_IDS.FILE || selectedType === BLOB_STORE_TYPE_IDS.GROUP) {
    return (
      <Box
        p="6"
        style={{
          background: 'var(--gray-2)',
          borderRadius: 'var(--radius-4)',
          border: '1px solid var(--gray-4)',
        }}
      >
        <Flex align="center" gap="4">
          <Shield size={32} color="var(--green-9)" />
          <Box>
            <Text weight="bold" size="3" as="p">
              No credentials required
            </Text>
            <Text color="gray" size="2" as="p">
              {selectedType === BLOB_STORE_TYPE_IDS.FILE
                ? 'File blob stores use local filesystem access. '
                : 'Group blob stores aggregate existing blob stores. '}
              Click Next to continue to advanced options.
            </Text>
          </Box>
        </Flex>
      </Box>
    );
  }

  if (selectedType === BLOB_STORE_TYPE_IDS.S3) {
    return (
      <>
        <SettingsFormSection
          title="Authentication"
          description="AWS credentials. Leave blank for IAM roles or environment variables."
        >
          <SettingsTextInput
            name="s3-access-key"
            label="Access Key ID"
            value={sec.accessKeyId || ''}
            onChange={(v) => onChange('bucketConfiguration.bucketSecurity.accessKeyId', v)}
            placeholder="AKIAIOSFODNN7EXAMPLE"
          />
          <SettingsPasswordInput
            name="s3-secret-key"
            label="Secret Access Key"
            value={sec.secretAccessKey || ''}
            onChange={(v) => onChange('bucketConfiguration.bucketSecurity.secretAccessKey', v)}
            placeholder="••••••••••••••••••••"
            autoComplete="new-password"
          />
          <SettingsTextInput
            name="s3-role-arn"
            label="Assume Role ARN"
            value={sec.role || ''}
            onChange={(v) => onChange('bucketConfiguration.bucketSecurity.role', v)}
            placeholder="arn:aws:iam::123456789012:role/S3Access"
          />
          <SettingsPasswordInput
            name="s3-session-token"
            label="Session Token"
            value={sec.sessionToken || ''}
            onChange={(v) => onChange('bucketConfiguration.bucketSecurity.sessionToken', v)}
            placeholder="Temporary token (optional)"
          />
        </SettingsFormSection>
        <SettingsFormSection title="Encryption" description="Optional server-side encryption for S3 objects">
          <SettingsSelect
            name="s3-encryption-type"
            label="Encryption Type"
            value={enc?.encryptionType || ''}
            onChange={(v) => onChange('bucketConfiguration.encryption.encryptionType', v)}
            options={[
              { value: '', label: 'None' },
              ...encryptionTypes.map((e) => ({ value: e.id, label: e.name })),
            ]}
            disabled={s3Loading}
          />
          <SettingsTextInput
            name="s3-kms-key"
            label="KMS Key ID (optional)"
            value={enc?.encryptionKey || ''}
            onChange={(v) => onChange('bucketConfiguration.encryption.encryptionKey', v)}
            placeholder="arn:aws:kms:us-east-1:123456789012:key/..."
          />
        </SettingsFormSection>
      </>
    );
  }

  if (selectedType === BLOB_STORE_TYPE_IDS.AZURE) {
    const showKey = azureAuth.authenticationMethod === 'ACCOUNTKEY';
    return (
      <SettingsFormSection
        title="Authentication"
        description="Configure how Nexus authenticates to Azure Storage"
      >
        <SettingsSelect
          name="azure-auth"
          label="Authentication Method"
          value={azureAuth.authenticationMethod || 'ENVIRONMENTVARIABLE'}
          onChange={(v) => {
            onChange('bucketConfiguration.authentication.authenticationMethod', v);
            reset();
          }}
          options={AUTH_OPTIONS_AZURE}
        />
        {showKey && (
          <SettingsPasswordInput
            name="azure-account-key"
            label="Account Key"
            value={azureAuth.accountKey || ''}
            onChange={(v) => {
              onChange('bucketConfiguration.authentication.accountKey', v);
              reset();
            }}
            placeholder="Your Azure storage account key"
            required
            autoComplete="new-password"
          />
        )}
        <Flex gap="2" mt="4" align="center">
          <SettingsButton
            variant="secondary"
            onClick={() =>
              testConnection({
                accountName: azureConfig.accountName,
                containerName: azureConfig.containerName,
                authenticationMethod: azureAuth.authenticationMethod || 'ENVIRONMENTVARIABLE',
                accountKey: azureAuth.accountKey,
              })
            }
            disabled={azureTesting || !azureConfig.accountName || !azureConfig.containerName}
          >
            {azureTesting ? (
              <>
                <Loader size={16} style={{ marginRight: 8 }} />
                Testing...
              </>
            ) : (
              'Test Connection'
            )}
          </SettingsButton>
          {azureResult === 'success' && (
            <SettingsAlert variant="success" icon={<CheckCircle size={16} />}>
              Connection succeeded
            </SettingsAlert>
          )}
          {azureResult === 'error' && (
            <SettingsAlert variant="error" icon={<XCircle size={16} />}>
              Connection failed
            </SettingsAlert>
          )}
        </Flex>
      </SettingsFormSection>
    );
  }

  if (selectedType === BLOB_STORE_TYPE_IDS.GOOGLE) {
    const showFile = gSecurity.authenticationMethod === 'accountKey';
    const isKms = gEnc.encryptionType === 'kmsManagedEncryption';
    return (
      <>
        <SettingsFormSection title="Authentication" description="How Nexus authenticates to GCP">
          <SettingsSelect
            name="google-auth"
            label="Method"
            value={gSecurity.authenticationMethod || 'applicationDefault'}
            onChange={(v) => onChange('bucketConfiguration.bucketSecurity.authenticationMethod', v)}
            options={AUTH_OPTIONS_GOOGLE}
          />
          {showFile && (
            <Box mt="2">
              <input
                ref={fileInputRef}
                type="file"
                accept=".json"
                onChange={(e) => {
                  const f = e.target.files?.[0];
                  if (f) {
                    setGoogleFileName(f.name);
                    onChange('bucketConfiguration.bucketSecurity.file', f);
                  }
                }}
                style={{ display: 'none' }}
              />
              <SettingsButton
                variant="secondary"
                onClick={() => fileInputRef.current?.click()}
                icon={<Upload size={16} />}
              >
                Choose JSON credential file
              </SettingsButton>
              {googleFileName && (
                <Text size="1" color="gray" ml="2" as="span">
                  Selected: {googleFileName}
                </Text>
              )}
            </Box>
          )}
        </SettingsFormSection>
        <SettingsFormSection title="Encryption" description="KMS encryption (optional)">
          <SettingsCheckbox
            name="google-kms"
            label="Enable KMS managed encryption"
            checked={isKms}
            onChange={(v) => onChange('bucketConfiguration.encryption.encryptionType', v ? 'kmsManagedEncryption' : 'default')}
          />
          {isKms && (
            <SettingsTextInput
              name="google-kms-key"
              label="KMS Key ID"
              value={gEnc.encryptionKey || ''}
              onChange={(v) => onChange('bucketConfiguration.encryption.encryptionKey', v)}
              placeholder="projects/PROJECT/locations/LOC/keyRings/RING/cryptoKeys/KEY"
              required
              monospace
            />
          )}
        </SettingsFormSection>
      </>
    );
  }

  return null;
}
