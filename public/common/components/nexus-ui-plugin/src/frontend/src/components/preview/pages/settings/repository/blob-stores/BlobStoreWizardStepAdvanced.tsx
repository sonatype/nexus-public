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
import { Flex, Text } from '@radix-ui/themes';
import { Plus, Trash2 } from 'lucide-react';
import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsSelect,
  SettingsCheckbox,
  SettingsButton,
} from '../../../../shared/form';
import { ExtJS } from '../../../../../../interface/ExtJS';
import type { BlobStoreFormData, S3BlobStoreConfig, S3FailoverBucket } from './types';
import { BLOB_STORE_TYPE_IDS } from './blobStoreFormMachine';
import type { BlobStoreTypeId } from './BlobStoreTypeSelector';

const SPACE_USED_QUOTA_ID = 'spaceUsedQuota';
const MAX_FAILOVER = 5;

export interface BlobStoreWizardStepAdvancedProps {
  data: BlobStoreFormData;
  selectedType: BlobStoreTypeId;
  onChange: (path: string, value: unknown) => void;
  updateField: (field: string, value: unknown) => void;
  quotaTypes: Array<{ id: string; name: string }>;
  validationErrors: Record<string, string>;
}

export function BlobStoreWizardStepAdvanced({
  data,
  selectedType,
  onChange,
  updateField,
  quotaTypes,
  validationErrors,
}: BlobStoreWizardStepAdvancedProps) {
  const isProEdition = ExtJS.isProEdition();
  const isFailoverAvailable = ExtJS.state().getValue('S3FailoverEnabled', false);

  const config = (data.bucketConfiguration || {}) as S3BlobStoreConfig;
  const adv = config.advancedBucketConnection || {};
  const failoverBuckets: S3FailoverBucket[] = config.failoverBuckets || [];
  const softQuota = data.softQuota || { enabled: false };

  const addFailover = () => {
    onChange('bucketConfiguration.failoverBuckets', [
      ...failoverBuckets,
      { region: '', bucketName: '' },
    ]);
  };

  const removeFailover = (i: number) => {
    onChange(
      'bucketConfiguration.failoverBuckets',
      failoverBuckets.filter((_, idx) => idx !== i)
    );
  };

  const updateFailover = (i: number, field: keyof S3FailoverBucket, value: string) => {
    const next = failoverBuckets.map((b, idx) =>
      idx === i ? { ...b, [field]: value } : b
    );
    onChange('bucketConfiguration.failoverBuckets', next);
  };

  return (
    <>
      {selectedType === BLOB_STORE_TYPE_IDS.S3 && (
        <SettingsFormSection
          title="Advanced Connection"
          description="Custom endpoints and connection settings"
        >
          <SettingsTextInput
            name="s3-endpoint"
            label="Endpoint URL"
            value={adv.endpoint || ''}
            onChange={(v) => onChange('bucketConfiguration.advancedBucketConnection.endpoint', v)}
            placeholder="https://s3.example.com"
            helpText="Custom endpoint for S3-compatible stores"
          />
          <SettingsTextInput
            name="s3-max-pool"
            label="Max Connection Pool Size"
            value={adv.maxConnectionPoolSize?.toString() || ''}
            onChange={(v) => {
              const n = parseInt(String(v), 10);
              onChange('bucketConfiguration.advancedBucketConnection.maxConnectionPoolSize', Number.isNaN(n) ? undefined : n);
            }}
            placeholder="100"
            type="number"
          />
          <SettingsCheckbox
            name="s3-path-style"
            label="Use path-style access"
            description="Use path-style URLs for all requests"
            checked={adv.forcePathStyle}
            onChange={(v) => onChange('bucketConfiguration.advancedBucketConnection.forcePathStyle', v)}
          />
          {isFailoverAvailable && isProEdition && (
            <>
              <Text weight="medium" size="2" mb="2" as="p">
                Replication Buckets
              </Text>
              {failoverBuckets.map((fb, i) => (
                <Flex key={i} gap="2" mb="2" align="center">
                  <SettingsTextInput
                    name={`failover-region-${i}`}
                    label="Region"
                    value={fb.region}
                    onChange={(v) => updateFailover(i, 'region', String(v))}
                    placeholder="us-east-1"
                  />
                  <SettingsTextInput
                    name={`failover-bucket-${i}`}
                    label="Bucket"
                    value={fb.bucketName}
                    onChange={(v) => updateFailover(i, 'bucketName', String(v))}
                    placeholder="replica-bucket"
                  />
                  <SettingsButton
                    variant="ghost"
                    size="1"
                    onClick={() => removeFailover(i)}
                    icon={Trash2}
                    title="Remove"
                  />
                </Flex>
              ))}
              {failoverBuckets.length < MAX_FAILOVER && (
                <SettingsButton variant="secondary" size="1" onClick={addFailover} icon={Plus}>
                  Add Replication Bucket
                </SettingsButton>
              )}
            </>
          )}
        </SettingsFormSection>
      )}

      <SettingsFormSection
        title="Soft Quota"
        description="Alert when storage exceeds the configured limit"
      >
        <SettingsCheckbox
          name="soft-quota-enabled"
          label="Enable soft quota"
          checked={softQuota.enabled}
          onChange={(v) =>
            updateField('softQuota', {
              ...softQuota,
              enabled: v,
              type: v ? softQuota.type || SPACE_USED_QUOTA_ID : undefined,
              limit: v ? (softQuota.limit ?? 1024) : undefined,
            })
          }
        />
        {softQuota.enabled && (
          <>
            <SettingsSelect
              name="soft-quota-type"
              label="Constraint Type"
              value={softQuota.type || ''}
              onChange={(v) => updateField('softQuota', { ...softQuota, type: v })}
              options={[
                { value: '', label: 'Select type...' },
                ...quotaTypes.map((q) => ({ value: q.id, label: q.name })),
              ]}
              required
              error={validationErrors['softQuota.type']}
            />
            <SettingsTextInput
              name="soft-quota-limit"
              label="Constraint Limit (MB)"
              value={softQuota.limit?.toString() || ''}
              onChange={(v) => {
                const n = parseInt(String(v), 10);
                updateField('softQuota', {
                  ...softQuota,
                  limit: Number.isNaN(n) ? undefined : n,
                });
              }}
              placeholder="1024"
              type="number"
              required
              error={validationErrors['softQuota.limit']}
            />
          </>
        )}
      </SettingsFormSection>
    </>
  );
}
