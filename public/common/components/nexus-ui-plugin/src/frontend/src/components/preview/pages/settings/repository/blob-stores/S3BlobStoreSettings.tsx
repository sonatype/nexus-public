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
import { Cloud, Plus, Trash2, AlertTriangle, Info, Globe, Edit, X } from 'lucide-react';
import { Flex } from '@radix-ui/themes';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { useLicenseLevel, LicenseLevel } from '../../../../shared/hooks/useLicenseLevel';
import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsSelect,
  SettingsCheckbox,
  SettingsAlert,
  SettingsButton,
  SettingsTextArea
} from '../../../../shared/form';
import { useIsLicensedEdition } from '../../../../shared/hooks';
import { useS3DropdownValues } from './useBlobStores';
import { EditionPill } from '../../../../shared/Badges/EditionPill';
import { LicenseRequiredCallout } from './LicenseRequiredCallout';
import { getGlobalDistributionTypeOptions } from './globalDistributionTypeOptions';
import type { BlobStoreFormData, S3BlobStoreConfig, S3CloudFrontConfig } from './types';
import './S3BlobStoreSettings.scss';

interface S3BlobStoreSettingsProps {
  data: BlobStoreFormData;
  onChange: (path: string, value: unknown) => void;
  disabled?: boolean;
  isEdit?: boolean;
  errors?: Record<string, string | null>;
}

const STRINGS = {
  BASIC: {
    title: 'S3 Bucket Configuration',
    description: 'Configure your AWS S3 bucket for blob storage',
    REGION: {
      label: 'Region',
      helpText: 'Select an AWS Region'
    },
    BUCKET: {
      label: 'Bucket',
      helpText: 'S3 Bucket Name (must be between 3 and 63 characters)',
      placeholder: 'my-blob-store-bucket'
    },
    PREFIX: {
      label: 'Prefix',
      helpText: 'S3 Path prefix (optional)',
      placeholder: 'nexus/'
    },
    PRESIGNED: {
      label: 'Pre-Signed URL',
      description: 'Allow Nexus Repository to redirect users to download binaries from S3'
    }
  },
  AUTH: {
    title: 'Authentication (Optional)',
    description: 'AWS credentials for S3 access. Leave blank to use IAM roles or environment variables.',
    ACCESS_KEY: {
      label: 'Access Key ID',
      placeholder: 'AKIAIOSFODNN7EXAMPLE'
    },
    SECRET_KEY: {
      label: 'Secret Access Key',
      placeholder: '••••••••••••••••••••'
    },
    ROLE_ARN: {
      label: 'Assume Role ARN',
      placeholder: 'arn:aws:iam::123456789012:role/S3Access'
    },
    SESSION_TOKEN: {
      label: 'Session Token',
      placeholder: 'Temporary session token (optional)'
    }
  },
  ENCRYPTION: {
    title: 'Encryption (Optional)',
    description: 'Configure server-side encryption for objects in S3',
    TYPE: {
      label: 'Encryption Type',
      helpText: 'The type of encryption for objects in the S3 Blob Store'
    },
    KEY_ID: {
      label: 'KMS Key ID (Optional)',
      helpText: 'If using KMS encryption, you can supply a Key ID. If left blank, the default will be used.',
      placeholder: 'arn:aws:kms:us-east-1:123456789012:key/...'
    }
  },
  ADVANCED: {
    title: 'Advanced Connection Settings (Optional)',
    description: 'Configure custom endpoints and connection settings',
    ENDPOINT: {
      label: 'Endpoint URL',
      helpText: 'A custom endpoint URL for third party object stores using the S3 API',
      placeholder: 'https://s3.example.com'
    },
    MAX_CONNECTIONS: {
      label: 'Max Connection Pool Size',
      helpText: 'Overrides the default connection pool size',
      placeholder: '100'
    },
    PATH_STYLE: {
      label: 'Use path-style access',
      description: 'Setting this flag will result in path-style access being used for all requests'
    }
  },
  FAILOVER: {
    title: 'AWS S3 Replication Buckets (Optional)',
    description: 'Configure failover buckets for high availability',
    ADD_BUTTON: 'Add Replication Bucket',
    REMOVE_BUTTON: 'Remove',
    MAX_WARNING: 'You have reached the maximum number of failover buckets allowed (5).',
    REGION: {
      label: 'Region',
      helpText: 'Select an AWS Region'
    },
    BUCKET: {
      label: 'Bucket Name',
      helpText: 'S3 Bucket Name for replication'
    }
  },
  GLOBAL_DISTRIBUTION: {
    title: 'Global Distribution (Optional)',
    description: 'Configure content distribution for improved download performance across regions',
    TYPE: {
      label: 'Distribution Type',
      helpText: 'Select a global distribution method. Options are mutually exclusive.'
    },
    CLOUDFRONT: {
      DISTRIBUTION_URL: {
        label: 'Distribution URL',
        helpText: 'The CloudFront distribution URL',
        placeholder: 'https://d111111abcdef8.cloudfront.net/'
      },
      KEY_ID: {
        label: 'Key ID',
        helpText: 'The identifier in CloudFront for the provided private key'
      },
      PRIVATE_KEY: {
        label: 'Private Key',
        helpText: 'The private key used to create pre-signed URLs (PEM PKCS#8 format)',
        placeholder: '-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----'
      },
      PRIVATE_KEY_PASSWORD: {
        label: 'Private Key Password',
        helpText: 'The password for the private key (optional)'
      }
    }
  },
  HELP: 'S3 blob stores require specific permissions to support full provisioning and functionality. Consult our documentation for the required set of permissions.'
};

const MAX_FAILOVER_BUCKETS = 5;

export const PLACEHOLDER_PASSWORD = '#~NXRM~PLACEHOLDER~PASSWORD~#';

export default function S3BlobStoreSettings({
  data,
  onChange,
  disabled = false,
  isEdit = false,
  errors = {},
}: S3BlobStoreSettingsProps) {
  const { values: dropdownValues, loading: dropdownLoading } = useS3DropdownValues();
  const [isEditingPrivateKey, setIsEditingPrivateKey] = useState(false);

  // Track whether CloudFront was configured when the component initially loaded
  // This prevents the UI from jumping to "Edit" mode when user fills in fields while editing
  // We initialize directly in the ref to avoid useEffect timing issues
  const hadCloudFrontOnMountRef = useRef(
    isEdit &&
    data.globalDistribution?.type === 'CLOUDFRONT' &&
    Boolean((data.globalDistribution as S3CloudFrontConfig)?.distributionUrl) &&
    Boolean((data.globalDistribution as S3CloudFrontConfig)?.keyId)
  );

  const isLicensedEdition = useIsLicensedEdition();
  const isFailoverAvailable = ExtJS.state().getValue('S3FailoverEnabled', false);

  const config: S3BlobStoreConfig = (data.bucketConfiguration as S3BlobStoreConfig) || {
    bucket: { region: '', name: '' },
    bucketSecurity: {},
    encryption: {},
    advancedBucketConnection: {},
    failoverBuckets: []
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

  const updateAdvanced = (field: string, value: unknown) => {
    onChange(`bucketConfiguration.advancedBucketConnection.${field}`, value);
  };

  const addFailoverBucket = () => {
    const current = config.failoverBuckets || [];
    onChange('bucketConfiguration.failoverBuckets', [...current, { region: '', bucketName: '' }]);
  };

  const removeFailoverBucket = (index: number) => {
    const current = config.failoverBuckets || [];
    onChange('bucketConfiguration.failoverBuckets', current.filter((_, i) => i !== index));
  };

  const updateFailoverBucket = (index: number, field: string, value: string) => {
    const current = config.failoverBuckets || [];
    const updated = current.map((bucket, i) =>
      i === index ? { ...bucket, [field]: value } : bucket
    );
    onChange('bucketConfiguration.failoverBuckets', updated);
  };

  const updateGlobalDistribution = (type: string) => {
    // Reset edit state when switching distribution types
    setIsEditingPrivateKey(false);

    if (type === '') {
      onChange('globalDistribution', undefined);
    } else if (type === 'CLOUDFRONT') {
      const current = data.globalDistribution as S3CloudFrontConfig;
      onChange('globalDistribution', {
        type: 'CLOUDFRONT',
        distributionUrl: current?.distributionUrl || '',
        keyId: current?.keyId || '',
        privateKey: current?.privateKey || '',
        privateKeyPassword: current?.privateKeyPassword || ''
      });
    } else if (type === 'TRANSFER_ACCELERATION') {
      onChange('globalDistribution', { type: 'TRANSFER_ACCELERATION' });
    }
  };

  const updateCloudFront = (field: keyof S3CloudFrontConfig, value: string) => {
    onChange(`globalDistribution.${field}`, value);
  };

  const handleEditPrivateKey = () => {
    setIsEditingPrivateKey(true);
    // Clear the private key and password fields when entering edit mode
    updateCloudFront('privateKey', '');
    updateCloudFront('privateKeyPassword', '');
  };

  const handleCancelEditPrivateKey = () => {
    setIsEditingPrivateKey(false);
    // Clear the values when canceling
    updateCloudFront('privateKey', '');
    updateCloudFront('privateKeyPassword', '');
  };

  const regions = dropdownValues?.regions || [];
  const encryptionTypes = dropdownValues?.encryptionTypes || [];
  const hasAuth = Boolean(config.bucketSecurity?.accessKeyId);
  const hasEncryption = Boolean(config.encryption?.encryptionType || config.encryption?.encryptionKey);
  const hasAdvanced = Boolean(config.advancedBucketConnection?.endpoint || config.advancedBucketConnection?.forcePathStyle);
  const hasFailover = (config.failoverBuckets?.length || 0) > 0;
  const hasGlobalDistribution = Boolean(data.globalDistribution);

  // CloudFront and S3 Transfer Acceleration need an Enterprise license; below it they render disabled with a pill
  const isGlobalDistributionAvailable = useLicenseLevel(LicenseLevel.ENTERPRISE);
  const isCloudFrontLocked = disabled || !isGlobalDistributionAvailable;

  // Check if we're editing an existing CloudFront configuration (has a saved private key)
  const isEditingExistingCloudFront = hadCloudFrontOnMountRef.current === true;

  // Don't show private key fields when editing a configuration with CloudFront saved
  const shouldShowPrivateKeyFields = !isEdit || isEditingPrivateKey || !hadCloudFrontOnMountRef.current;

  return (
    <div className="s3-blob-store-settings">
      <SettingsAlert variant="info" icon={<Info size={16} />}>
        {STRINGS.HELP}
        <a
          href="https://links.sonatype.com/products/nexus/blobstores/s3/docs"
          target="_blank"
          rel="noopener noreferrer"
          className="s3-blob-store-settings__link"
        >
          View Documentation →
        </a>
      </SettingsAlert>

      {/* Basic Configuration */}
      <SettingsFormSection
        title={STRINGS.BASIC.title}
        description={STRINGS.BASIC.description}
        icon={<Cloud size={20} />}
        collapsible
        defaultExpanded
      >
        <SettingsSelect
          name="s3-region"
          label={STRINGS.BASIC.REGION.label}
          value={config.bucket?.region || ''}
          onChange={(value) => updateBucket('region', value)}
          options={[
            { value: '', label: 'Select a region...' },
            ...regions.map(r => ({ value: r.id, label: r.name }))
          ]}
          helpText={STRINGS.BASIC.REGION.helpText}
          disabled={disabled || dropdownLoading}
        />

        <SettingsTextInput
          name="s3-bucket"
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
          name="s3-prefix"
          label={STRINGS.BASIC.PREFIX.label}
          value={config.bucket?.prefix || ''}
          onChange={(value) => updateBucket('prefix', value)}
          helpText={STRINGS.BASIC.PREFIX.helpText}
          placeholder={STRINGS.BASIC.PREFIX.placeholder}
          disabled={disabled}
        />

        <Flex direction="column" gap="2">
          {!isLicensedEdition && <LicenseRequiredCallout feature="presignedUrl" />}
          <SettingsCheckbox
            name="s3-presigned"
            label={STRINGS.BASIC.PRESIGNED.label}
            description={STRINGS.BASIC.PRESIGNED.description}
            checked={config.preSignedUrlEnabled || false}
            onChange={(checked) => onChange('bucketConfiguration.preSignedUrlEnabled', checked)}
            disabled={disabled || !isLicensedEdition}
            badge={isLicensedEdition ? undefined : <EditionPill tier="pro" />}
          />
        </Flex>
      </SettingsFormSection>

      {/* Global Distribution */}
      {!isGlobalDistributionAvailable && <LicenseRequiredCallout feature="globalDistribution" mb="1" />}
      <SettingsFormSection
        title={STRINGS.GLOBAL_DISTRIBUTION.title}
        description={STRINGS.GLOBAL_DISTRIBUTION.description}
        icon={<Globe size={20} />}
        collapsible
        defaultExpanded={hasGlobalDistribution}
      >
        <SettingsSelect
          name="s3-global-distribution-type"
          label={STRINGS.GLOBAL_DISTRIBUTION.TYPE.label}
          value={data.globalDistribution?.type || ''}
          onChange={(value) => updateGlobalDistribution(value)}
          options={getGlobalDistributionTypeOptions(isGlobalDistributionAvailable)}
          helpText={STRINGS.GLOBAL_DISTRIBUTION.TYPE.helpText}
          disabled={disabled}
        />

        {data.globalDistribution?.type === 'CLOUDFRONT' && (
          <>
            <SettingsTextInput
              label={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.DISTRIBUTION_URL.label}
              value={(data.globalDistribution as S3CloudFrontConfig)?.distributionUrl || ''}
              onChange={(value) => updateCloudFront('distributionUrl', value)}
              helpText={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.DISTRIBUTION_URL.helpText}
              placeholder={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.DISTRIBUTION_URL.placeholder}
              required
              disabled={isCloudFrontLocked}
            />

            <SettingsTextInput
              label={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.KEY_ID.label}
              value={(data.globalDistribution as S3CloudFrontConfig)?.keyId || ''}
              onChange={(value) => updateCloudFront('keyId', value)}
              helpText={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.KEY_ID.helpText}
              required
              disabled={isCloudFrontLocked}
            />

            {shouldShowPrivateKeyFields ? (
              <>
                <SettingsTextArea
                  label={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.PRIVATE_KEY.label}
                  value={(data.globalDistribution as S3CloudFrontConfig)?.privateKey || ''}
                  onChange={(value) => updateCloudFront('privateKey', value)}
                  helpText={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.PRIVATE_KEY.helpText}
                  placeholder={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.PRIVATE_KEY.placeholder}
                  required
                  disabled={isCloudFrontLocked}
                  monospace
                  rows={6}
                />

                <SettingsPasswordInput
                  label={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.PRIVATE_KEY_PASSWORD.label}
                  value={(data.globalDistribution as S3CloudFrontConfig)?.privateKeyPassword || ''}
                  onChange={(value) => updateCloudFront('privateKeyPassword', value)}
                  helpText={STRINGS.GLOBAL_DISTRIBUTION.CLOUDFRONT.PRIVATE_KEY_PASSWORD.helpText}
                  disabled={isCloudFrontLocked}
                  autoComplete="new-password"
                />

                {isEditingExistingCloudFront && isEditingPrivateKey && (
                  <SettingsButton
                    variant="secondary"
                    onClick={handleCancelEditPrivateKey}
                    disabled={isCloudFrontLocked}
                    icon={X}
                  >
                    Cancel
                  </SettingsButton>
                )}
              </>
            ) : (
              <div className="s3-blob-store-settings__private-key-notice">
                <SettingsAlert variant="info">
                  A private key has been configured for this CloudFront distribution.
                </SettingsAlert>
                <SettingsButton
                  variant="secondary"
                  onClick={handleEditPrivateKey}
                  disabled={isCloudFrontLocked}
                  icon={Edit}
                >
                  Edit Private Key
                </SettingsButton>
              </div>
            )}
          </>
        )}
      </SettingsFormSection>

      {/* Authentication */}
      <SettingsFormSection
        title={STRINGS.AUTH.title}
        description={STRINGS.AUTH.description}
        collapsible
        defaultExpanded={hasAuth}
      >
        <SettingsTextInput
          name="s3-access-key"
          label={STRINGS.AUTH.ACCESS_KEY.label}
          value={config.bucketSecurity?.accessKeyId || ''}
          onChange={(value) => updateSecurity('accessKeyId', value)}
          placeholder={STRINGS.AUTH.ACCESS_KEY.placeholder}
          disabled={disabled}
        />

        <SettingsPasswordInput
          name="s3-secret-key"
          label={STRINGS.AUTH.SECRET_KEY.label}
          value={config.bucketSecurity?.secretAccessKey || ''}
          onChange={(value) => updateSecurity('secretAccessKey', value)}
          placeholder={STRINGS.AUTH.SECRET_KEY.placeholder}
          disabled={disabled}
          autoComplete="new-password"
        />

        <SettingsTextInput
          name="s3-role-arn"
          label={STRINGS.AUTH.ROLE_ARN.label}
          value={config.bucketSecurity?.role || ''}
          onChange={(value) => updateSecurity('role', value)}
          placeholder={STRINGS.AUTH.ROLE_ARN.placeholder}
          disabled={disabled}
        />

        <SettingsPasswordInput
          name="s3-session-token"
          label={STRINGS.AUTH.SESSION_TOKEN.label}
          value={config.bucketSecurity?.sessionToken || ''}
          onChange={(value) => updateSecurity('sessionToken', value)}
          placeholder={STRINGS.AUTH.SESSION_TOKEN.placeholder}
          disabled={disabled}
        />
      </SettingsFormSection>

      {/* Encryption */}
      <SettingsFormSection
        title={STRINGS.ENCRYPTION.title}
        description={STRINGS.ENCRYPTION.description}
        collapsible
        defaultExpanded={hasEncryption}
      >
        <SettingsSelect
          label={STRINGS.ENCRYPTION.TYPE.label}
          value={config.encryption?.encryptionType || ''}
          onChange={(value) => updateEncryption('encryptionType', value)}
          options={[
            { value: '', label: 'None' },
            ...encryptionTypes.map(e => ({ value: e.id, label: e.name }))
          ]}
          helpText={STRINGS.ENCRYPTION.TYPE.helpText}
          disabled={disabled || dropdownLoading}
        />

        <SettingsTextInput
          label={STRINGS.ENCRYPTION.KEY_ID.label}
          value={config.encryption?.encryptionKey || ''}
          onChange={(value) => updateEncryption('encryptionKey', value)}
          helpText={STRINGS.ENCRYPTION.KEY_ID.helpText}
          placeholder={STRINGS.ENCRYPTION.KEY_ID.placeholder}
          disabled={disabled}
          monospace
        />
      </SettingsFormSection>

      {/* Advanced Settings */}
      <SettingsFormSection
        title={STRINGS.ADVANCED.title}
        description={STRINGS.ADVANCED.description}
        collapsible
        defaultExpanded={hasAdvanced}
      >
        <SettingsTextInput
          label={STRINGS.ADVANCED.ENDPOINT.label}
          value={config.advancedBucketConnection?.endpoint || ''}
          onChange={(value) => updateAdvanced('endpoint', value)}
          helpText={STRINGS.ADVANCED.ENDPOINT.helpText}
          placeholder={STRINGS.ADVANCED.ENDPOINT.placeholder}
          disabled={disabled}
        />

        <SettingsTextInput
          label={STRINGS.ADVANCED.MAX_CONNECTIONS.label}
          value={config.advancedBucketConnection?.maxConnectionPoolSize?.toString() || ''}
          onChange={(value) => updateAdvanced('maxConnectionPoolSize', value ? parseInt(value, 10) : undefined)}
          helpText={STRINGS.ADVANCED.MAX_CONNECTIONS.helpText}
          placeholder={STRINGS.ADVANCED.MAX_CONNECTIONS.placeholder}
          type="number"
          disabled={disabled}
        />

        <SettingsCheckbox
          label={STRINGS.ADVANCED.PATH_STYLE.label}
          description={STRINGS.ADVANCED.PATH_STYLE.description}
          checked={config.advancedBucketConnection?.forcePathStyle || false}
          onChange={(checked) => updateAdvanced('forcePathStyle', checked)}
          disabled={disabled}
        />
      </SettingsFormSection>

      {/* Failover Buckets */}
      {isFailoverAvailable && (
        <SettingsFormSection
          title={STRINGS.FAILOVER.title}
          description={STRINGS.FAILOVER.description}
          collapsible
          defaultExpanded={hasFailover}
        >
          {config.failoverBuckets?.map((bucket, index) => (
            <div key={index} className="s3-blob-store-settings__failover-bucket">
              <div className="s3-blob-store-settings__failover-fields">
                <SettingsSelect
                  label={`${STRINGS.FAILOVER.REGION.label} ${index + 1}`}
                  value={bucket.region || ''}
                  onChange={(value) => updateFailoverBucket(index, 'region', value)}
                  options={[
                    { value: '', label: 'Select a region...' },
                    ...regions.map(r => ({ value: r.id, label: r.name }))
                  ]}
                  helpText={STRINGS.FAILOVER.REGION.helpText}
                  disabled={disabled || dropdownLoading}
                />

                <SettingsTextInput
                  label={`${STRINGS.FAILOVER.BUCKET.label} ${index + 1}`}
                  value={bucket.bucketName || ''}
                  onChange={(value) => updateFailoverBucket(index, 'bucketName', value)}
                  helpText={STRINGS.FAILOVER.BUCKET.helpText}
                  disabled={disabled}
                />
              </div>

              <SettingsButton
                variant="ghost"
                onClick={() => removeFailoverBucket(index)}
                disabled={disabled}
                icon={Trash2}
                className="s3-blob-store-settings__remove-btn"
              >
                {STRINGS.FAILOVER.REMOVE_BUTTON}
              </SettingsButton>
            </div>
          ))}

          {(config.failoverBuckets?.length || 0) < MAX_FAILOVER_BUCKETS ? (
            <SettingsButton
              variant="secondary"
              onClick={addFailoverBucket}
              disabled={disabled}
              icon={Plus}
            >
              {STRINGS.FAILOVER.ADD_BUTTON}
            </SettingsButton>
          ) : (
            <SettingsAlert variant="warning" icon={<AlertTriangle size={16} />}>
              {STRINGS.FAILOVER.MAX_WARNING}
            </SettingsAlert>
          )}
        </SettingsFormSection>
      )}
    </div>
  );
}

