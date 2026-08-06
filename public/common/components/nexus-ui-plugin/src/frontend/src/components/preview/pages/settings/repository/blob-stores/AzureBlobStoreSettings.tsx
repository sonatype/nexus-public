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

import React from 'react';
import { Cloud, CheckCircle, XCircle } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';
import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsSelect,
  SettingsCheckbox,
  SettingsButton,
  SettingsAlert
} from '../../../../shared/form';
import { useAzureConnectionTest } from './useBlobStores';
import type { BlobStoreFormData, AzureBlobStoreConfig } from './types';
import './AzureBlobStoreSettings.scss';

interface AzureBlobStoreSettingsProps {
  data: BlobStoreFormData;
  onChange: (path: string, value: unknown) => void;
  disabled?: boolean;
  isEdit?: boolean;
  errors?: Record<string, string | null>;
}

const STRINGS = {
  TITLE: 'Azure Blob Storage Configuration',
  DESCRIPTION: 'Configure your Azure Storage account for blob storage',
  ACCOUNT_NAME: {
    label: 'Account Name',
    helpText: 'The name of the Azure storage account',
    placeholder: 'mystorageaccount'
  },
  CONTAINER_NAME: {
    label: 'Container Name',
    helpText: 'The name of a container to be used for storage; the container will be created if it does not already exist',
    placeholder: 'nexus-blobs'
  },
  AUTH: {
    label: 'Authentication Method',
    ENVIRONMENT: {
      value: 'ENVIRONMENTVARIABLE',
      label: 'Use Environment Variables'
    },
    MANAGED: {
      value: 'MANAGEDIDENTITY',
      label: 'Managed Identity (System)'
    },
    ACCOUNT_KEY: {
      value: 'ACCOUNTKEY',
      label: 'Account Key',
      field: {
        label: 'Account Key',
        helpText: 'Account key found under Access keys for the storage account',
        placeholder: 'Your Azure storage account key'
      }
    }
  },
  DIRECT_DOWNLOAD: {
    label: 'Direct Download (SAS URLs)',
    description: 'Redirect downloads directly to Azure Blob Storage. Reduces server bandwidth and improves download speed.',
    rbacNote: "Requires the 'Storage Blob Delegator' role on the storage account. Verified on save."
  },
  TEST_CONNECTION: {
    button: 'Test Connection',
    testing: 'Testing connection...',
    success: 'Connection succeeded',
    error: 'Connection failed, check the logs for more information'
  }
};

const AUTH_OPTIONS = [
  { value: STRINGS.AUTH.ENVIRONMENT.value, label: STRINGS.AUTH.ENVIRONMENT.label },
  { value: STRINGS.AUTH.MANAGED.value, label: STRINGS.AUTH.MANAGED.label },
  { value: STRINGS.AUTH.ACCOUNT_KEY.value, label: STRINGS.AUTH.ACCOUNT_KEY.label }
];

export default function AzureBlobStoreSettings({
  data,
  onChange,
  disabled = false,
  isEdit = false,
  errors = {},
}: AzureBlobStoreSettingsProps) {
  const { testing, result, testConnection, reset } = useAzureConnectionTest();

  const isProEdition = ExtJS.isProEdition();
  const isAzureSasEnabled = ExtJS.state()?.getValue?.('azureSasUrlEnabled') === true;

  const config: AzureBlobStoreConfig = (data.bucketConfiguration as AzureBlobStoreConfig) || {
    accountName: '',
    containerName: '',
    authentication: { authenticationMethod: 'ENVIRONMENTVARIABLE' }
  };

  const updateConfig = (field: string, value: unknown) => {
    onChange(`bucketConfiguration.${field}`, value);
    reset(); // Clear test result when config changes
  };

  const updateAuth = (field: string, value: unknown) => {
    onChange(`bucketConfiguration.authentication.${field}`, value);
    reset();
  };

  const handleTestConnection = () => {
    testConnection({
      blobStoreName: isEdit ? data.name : undefined,
      accountName: config.accountName,
      containerName: config.containerName,
      authenticationMethod: config.authentication?.authenticationMethod || 'ENVIRONMENTVARIABLE',
      accountKey: config.authentication?.accountKey
    });
  };

  const showAccountKey = config.authentication?.authenticationMethod === 'ACCOUNTKEY';
  const isTokenCredentialAuth =
      config.authentication?.authenticationMethod === 'MANAGEDIDENTITY' ||
      config.authentication?.authenticationMethod === 'ENVIRONMENTVARIABLE';
  const showRbacNote =
      isProEdition && isAzureSasEnabled && !!config.preSignedUrlEnabled && isTokenCredentialAuth;

  return (
    <div className="azure-blob-store-settings">
      <SettingsFormSection
        title={STRINGS.TITLE}
        description={STRINGS.DESCRIPTION}
        icon={<Cloud size={20} />}
      >
        <SettingsTextInput
          name="azure-account-name"
          label={STRINGS.ACCOUNT_NAME.label}
          value={config.accountName || ''}
          onChange={(value) => updateConfig('accountName', value)}
          helpText={STRINGS.ACCOUNT_NAME.helpText}
          placeholder={STRINGS.ACCOUNT_NAME.placeholder}
          error={errors['bucketConfiguration.accountName'] ?? undefined}
          required
          disabled={disabled}
        />

        <SettingsTextInput
          name="azure-container-name"
          label={STRINGS.CONTAINER_NAME.label}
          value={config.containerName || ''}
          onChange={(value) => updateConfig('containerName', value)}
          helpText={STRINGS.CONTAINER_NAME.helpText}
          placeholder={STRINGS.CONTAINER_NAME.placeholder}
          error={errors['bucketConfiguration.containerName'] ?? undefined}
          required
          disabled={disabled}
        />

        <SettingsSelect
          name="azure-auth-method"
          label={STRINGS.AUTH.label}
          value={config.authentication?.authenticationMethod || 'ENVIRONMENTVARIABLE'}
          onChange={(value) => updateAuth('authenticationMethod', value)}
          options={AUTH_OPTIONS}
          disabled={disabled}
        />

        {showAccountKey && (
          <SettingsPasswordInput
            name="azure-account-key"
            label={STRINGS.AUTH.ACCOUNT_KEY.field.label}
            value={config.authentication?.accountKey || ''}
            onChange={(value) => updateAuth('accountKey', value)}
            helpText={STRINGS.AUTH.ACCOUNT_KEY.field.helpText}
            placeholder={STRINGS.AUTH.ACCOUNT_KEY.field.placeholder}
            required
            disabled={disabled}
            autoComplete="new-password"
          />
        )}

        {isProEdition && isAzureSasEnabled && (
          <div className="azure-blob-store-settings__sas-section">
            <SettingsCheckbox
              name="azure-direct-download"
              label={STRINGS.DIRECT_DOWNLOAD.label}
              description={STRINGS.DIRECT_DOWNLOAD.description}
              checked={config.preSignedUrlEnabled || false}
              onChange={(checked) => onChange('bucketConfiguration.preSignedUrlEnabled', checked)}
              disabled={disabled}
            />

            {showRbacNote && (
              <SettingsAlert type="info">
                {STRINGS.DIRECT_DOWNLOAD.rbacNote}
              </SettingsAlert>
            )}
          </div>
        )}

        <div className="azure-blob-store-settings__test-section">
          <SettingsButton
            variant="secondary"
            onClick={handleTestConnection}
            disabled={disabled || testing || !config.accountName || !config.containerName}
            testId="azure-test-connection"
            loading={testing}
          >
            {testing ? STRINGS.TEST_CONNECTION.testing : STRINGS.TEST_CONNECTION.button}
          </SettingsButton>

          {result === 'success' && (
            <SettingsAlert variant="success" icon={<CheckCircle size={16} />}>
              {STRINGS.TEST_CONNECTION.success}
            </SettingsAlert>
          )}

          {result === 'error' && (
            <SettingsAlert variant="error" icon={<XCircle size={16} />}>
              {STRINGS.TEST_CONNECTION.error}
            </SettingsAlert>
          )}
        </div>
      </SettingsFormSection>
    </div>
  );
}

