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
import { Cloud, Upload, Info } from 'lucide-react';
import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsSelect,
  SettingsCheckbox,
  SettingsAlert
} from '../../../../shared/form';
import type { BlobStoreFormData, GoogleBlobStoreConfig } from './types';
import './GoogleBlobStoreSettings.scss';

interface GoogleBlobStoreSettingsProps {
  data: BlobStoreFormData;
  onChange: (path: string, value: unknown) => void;
  disabled?: boolean;
  isEdit?: boolean;
  errors?: Record<string, string | null>;
}

const STRINGS = {
  BASIC: {
    title: 'Google Cloud Storage Configuration',
    description: 'Configure your GCP bucket for blob storage',
    REGION: {
      label: 'Region',
      helpText: 'The region is automatically set based on where Nexus Repository is running in GCP. Ensure the bucket is in the same region.'
    },
    PROJECT_ID: {
      label: 'Project ID',
      helpText: 'Your GCP Project ID is a unique identifier for your Google Cloud project',
      placeholder: 'my-gcp-project'
    },
    BUCKET: {
      label: 'Bucket',
      helpText: 'Google Cloud Platform bucket name (must be between 3 and 63 characters)',
      placeholder: 'my-nexus-bucket'
    },
    PREFIX: {
      label: 'Prefix',
      helpText: 'Google Cloud Storage path prefix',
      placeholder: 'nexus/'
    }
  },
  AUTH: {
    title: 'Authentication',
    label: 'Authentication Method',
    APPLICATION_DEFAULT: {
      value: 'applicationDefault',
      label: 'Use Google Application Default Credentials'
    },
    ACCOUNT_KEY: {
      value: 'accountKey',
      label: 'Use a separate credential JSON file'
    },
    FILE: {
      label: 'JSON Credential File',
      helpText: 'Upload a .json file (maximum size: 4KB)',
      button: 'Choose File',
      selected: (name: string) => `Selected: ${name}`
    }
  },
  ENCRYPTION: {
    title: 'Encryption',
    description: 'Data is encrypted by default. Enable KMS to use a custom encryption key.',
    note: 'Encryption settings cannot be changed once the bucket is created',
    KMS_ENABLED: {
      label: 'Enable KMS managed encryption'
    },
    KEY_NAME: {
      label: 'KMS Key ID',
      helpText: 'Enter the KMS Key ID to use for encryption',
      placeholder: 'projects/PROJECT_ID/locations/LOCATION/keyRings/KEY_RING_NAME/cryptoKeys/KEY_NAME'
    }
  }
};

const AUTH_OPTIONS = [
  { value: STRINGS.AUTH.APPLICATION_DEFAULT.value, label: STRINGS.AUTH.APPLICATION_DEFAULT.label },
  { value: STRINGS.AUTH.ACCOUNT_KEY.value, label: STRINGS.AUTH.ACCOUNT_KEY.label }
];

export default function GoogleBlobStoreSettings({
  data,
  onChange,
  disabled = false,
  isEdit = false,
  errors = {},
}: GoogleBlobStoreSettingsProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFileName, setSelectedFileName] = useState<string | null>(null);

  const config: GoogleBlobStoreConfig = (data.bucketConfiguration as GoogleBlobStoreConfig) || {
    bucket: { name: '' },
    bucketSecurity: { authenticationMethod: 'applicationDefault' },
    encryption: { encryptionType: 'default' }
  };

  const updateBucket = (field: string, value: unknown) => {
    onChange(`bucketConfiguration.bucket.${field}`, value);
  };

  const updateSecurity = (field: string, value: unknown) => {
    onChange(`bucketConfiguration.bucketSecurity.${field}`, value);
  };

  const updateEncryption = (field: string, value: unknown) => {
    onChange(`bucketConfiguration.encryption.${field}`, value);
  };

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFileName(file.name);
      onChange('bucketConfiguration.bucketSecurity.file', file);
    }
  };

  const handleKmsToggle = (enabled: boolean) => {
    updateEncryption('encryptionType', enabled ? 'kmsManagedEncryption' : 'default');
  };

  const showFileUpload = config.bucketSecurity?.authenticationMethod === 'accountKey';
  const isKmsEnabled = config.encryption?.encryptionType === 'kmsManagedEncryption';

  return (
    <div className="google-blob-store-settings">
      {/* Basic Configuration */}
      <SettingsFormSection
        title={STRINGS.BASIC.title}
        description={STRINGS.BASIC.description}
        icon={<Cloud size={20} />}
      >
        <SettingsAlert variant="info" icon={<Info size={16} />}>
          {STRINGS.BASIC.REGION.helpText}
        </SettingsAlert>

        <SettingsTextInput
          name="google-project-id"
          label={STRINGS.BASIC.PROJECT_ID.label}
          value={config.bucket?.projectId || ''}
          onChange={(value) => updateBucket('projectId', value)}
          helpText={STRINGS.BASIC.PROJECT_ID.helpText}
          placeholder={STRINGS.BASIC.PROJECT_ID.placeholder}
          disabled={disabled}
        />

        <SettingsTextInput
          name="google-bucket"
          label={STRINGS.BASIC.BUCKET.label}
          value={config.bucket?.name || ''}
          onChange={(value) => updateBucket('name', value)}
          helpText={STRINGS.BASIC.BUCKET.helpText}
          placeholder={STRINGS.BASIC.BUCKET.placeholder}
          error={errors['bucketConfiguration.bucket.name'] ?? undefined}
          required
          disabled={disabled}
        />

        <SettingsTextInput
          name="google-prefix"
          label={STRINGS.BASIC.PREFIX.label}
          value={config.bucket?.prefix || ''}
          onChange={(value) => updateBucket('prefix', value)}
          helpText={STRINGS.BASIC.PREFIX.helpText}
          placeholder={STRINGS.BASIC.PREFIX.placeholder}
          disabled={disabled}
        />
      </SettingsFormSection>

      {/* Authentication */}
      <SettingsFormSection title={STRINGS.AUTH.title}>
        <SettingsSelect
          name="google-auth-method"
          label={STRINGS.AUTH.label}
          value={config.bucketSecurity?.authenticationMethod || 'applicationDefault'}
          onChange={(value) => updateSecurity('authenticationMethod', value)}
          options={AUTH_OPTIONS}
          disabled={disabled}
        />

        {showFileUpload && (
          <div className="google-blob-store-settings__file-upload">
            <label className="google-blob-store-settings__file-label">
              {STRINGS.AUTH.FILE.label}
              {!isEdit && <span className="google-blob-store-settings__required">*</span>}
            </label>
            <p className="google-blob-store-settings__file-help">{STRINGS.AUTH.FILE.helpText}</p>
            
            <input
              ref={fileInputRef}
              type="file"
              accept=".json"
              onChange={handleFileSelect}
              className="google-blob-store-settings__file-input"
              disabled={disabled}
            />
            
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="google-blob-store-settings__file-button"
              disabled={disabled}
            >
              <Upload size={16} />
              {STRINGS.AUTH.FILE.button}
            </button>

            {selectedFileName && (
              <p className="google-blob-store-settings__file-selected">
                {STRINGS.AUTH.FILE.selected(selectedFileName)}
              </p>
            )}
          </div>
        )}
      </SettingsFormSection>

      {/* Encryption */}
      <SettingsFormSection
        title={STRINGS.ENCRYPTION.title}
        description={STRINGS.ENCRYPTION.description}
      >
        <SettingsAlert variant="info" icon={<Info size={16} />}>
          {STRINGS.ENCRYPTION.note}
        </SettingsAlert>

        <SettingsCheckbox
          name="google-kms-enabled"
          label={STRINGS.ENCRYPTION.KMS_ENABLED.label}
          checked={isKmsEnabled}
          onChange={handleKmsToggle}
          disabled={disabled}
        />

        {isKmsEnabled && (
          <SettingsTextInput
            name="google-kms-key"
            label={STRINGS.ENCRYPTION.KEY_NAME.label}
            value={config.encryption?.encryptionKey || ''}
            onChange={(value) => updateEncryption('encryptionKey', value)}
            helpText={STRINGS.ENCRYPTION.KEY_NAME.helpText}
            placeholder={STRINGS.ENCRYPTION.KEY_NAME.placeholder}
            required={!isEdit}
            disabled={disabled}
            monospace
          />
        )}
      </SettingsFormSection>
    </div>
  );
}

