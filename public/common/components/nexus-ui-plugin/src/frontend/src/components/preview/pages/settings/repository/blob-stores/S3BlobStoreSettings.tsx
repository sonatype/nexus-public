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

import React, { useState } from 'react';
import { Cloud, Plus, Trash2, AlertTriangle, Info } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';
import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsSelect,
  SettingsCheckbox,
  SettingsAlert,
  SettingsButton
} from '../../../../shared/form';
import { useS3DropdownValues } from './useBlobStores';
import type { BlobStoreFormData, S3BlobStoreConfig, S3FailoverBucket } from './types';
import './S3BlobStoreSettings.scss';

interface S3BlobStoreSettingsProps {
  data: BlobStoreFormData;
  onChange: (path: string, value: unknown) => void;
  disabled?: boolean;
  isEdit?: boolean;
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
  HELP: 'S3 blob stores require specific permissions to support full provisioning and functionality. Consult our documentation for the required set of permissions.'
};

const MAX_FAILOVER_BUCKETS = 5;

export default function S3BlobStoreSettings({
  data,
  onChange,
  disabled = false,
  isEdit = false
}: S3BlobStoreSettingsProps) {
  const { values: dropdownValues, loading: dropdownLoading } = useS3DropdownValues();
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set(['basic']));

  const isProEdition = ExtJS.isProEdition();
  const isFailoverAvailable = ExtJS.state().getValue('S3FailoverEnabled', false);

  const config: S3BlobStoreConfig = (data.bucketConfiguration as S3BlobStoreConfig) || {
    bucket: { region: '', name: '' },
    bucketSecurity: {},
    encryption: {},
    advancedBucketConnection: {},
    failoverBuckets: []
  };

  const toggleSection = (section: string) => {
    setExpandedSections(prev => {
      const next = new Set(prev);
      if (next.has(section)) {
        next.delete(section);
      } else {
        next.add(section);
      }
      return next;
    });
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

  const regions = dropdownValues?.regions || [];
  const encryptionTypes = dropdownValues?.encryptionTypes || [];
  const hasAuth = Boolean(config.bucketSecurity?.accessKeyId);
  const hasEncryption = Boolean(config.encryption?.encryptionType || config.encryption?.encryptionKey);
  const hasAdvanced = Boolean(config.advancedBucketConnection?.endpoint || config.advancedBucketConnection?.forcePathStyle);
  const hasFailover = (config.failoverBuckets?.length || 0) > 0;

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

        {isProEdition && (
          <SettingsCheckbox
            name="s3-presigned"
            label={STRINGS.BASIC.PRESIGNED.label}
            description={STRINGS.BASIC.PRESIGNED.description}
            checked={config.preSignedUrlEnabled || false}
            onChange={(checked) => onChange('bucketConfiguration.preSignedUrlEnabled', checked)}
            disabled={disabled}
          />
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

