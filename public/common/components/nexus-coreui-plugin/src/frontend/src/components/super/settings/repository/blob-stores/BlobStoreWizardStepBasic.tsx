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
import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsSelect,
  SettingsCheckbox,
} from '../../../shared/form';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { useS3DropdownValues, useGroupableBlobStores } from './useBlobStores';
import { SettingsTransferList } from '../../../shared/form';
import type { BlobStoreFormData, S3BlobStoreConfig } from './types';
import { BLOB_STORE_TYPE_IDS } from './blobStoreFormMachine';
import type { BlobStoreTypeId } from './BlobStoreTypeSelector';

const FILL_POLICY_OPTIONS = [
  { value: '', label: 'Select a fill policy...' },
  { value: 'writeToFirst', label: 'Write to First' },
  { value: 'roundRobin', label: 'Round Robin' },
];

export interface BlobStoreWizardStepBasicProps {
  data: BlobStoreFormData;
  selectedType: BlobStoreTypeId;
  onChange: (path: string, value: unknown) => void;
  updateField: (field: string, value: unknown) => void;
  validationErrors: Record<string, string>;
}

export function BlobStoreWizardStepBasic({
  data,
  selectedType,
  onChange,
  updateField,
  validationErrors,
}: BlobStoreWizardStepBasicProps) {
  const { values: s3Dropdown, loading: s3Loading } = useS3DropdownValues();
  const { blobStores, loading: groupLoading } = useGroupableBlobStores();
  const isProEdition = ExtJS.isProEdition();

  const config = (data.bucketConfiguration || {}) as S3BlobStoreConfig;
  const bucket = config.bucket || { region: '', name: '', prefix: '' };
  const regions = s3Dropdown?.regions || [];

  const members: string[] = data.members || [];
  const availableOptions = blobStores
    .filter((s) => !members.includes(s))
    .map((s) => ({ value: s, label: s }));
  const selectedOptions = members.map((s) => ({ value: s, label: s }));

  return (
    <>
      <SettingsFormSection title="Basic Configuration" description="Name and required settings for your blob store">
        <SettingsTextInput
          name="blobstore-name"
          label="Name"
          value={data.name || ''}
          onChange={(v) => updateField('name', v)}
          placeholder="my-blob-store"
          helpText="Unique identifier. Use letters, numbers, underscores, and hyphens (e.g., maven-releases)"
          required
          error={validationErrors.name}
        />

        {selectedType === BLOB_STORE_TYPE_IDS.FILE && (
          <SettingsTextInput
            name="file-path"
            label="Path"
            value={data.path || ''}
            onChange={(v) => onChange('path', v)}
            placeholder="/path/to/blob/storage"
            helpText="Absolute path or relative to &lt;data-directory&gt;/blobs"
            required
            error={validationErrors.path}
            monospace
          />
        )}

        {selectedType === BLOB_STORE_TYPE_IDS.S3 && (
          <>
            <SettingsSelect
              name="s3-region"
              label="Region"
              value={bucket.region || ''}
              onChange={(v) => onChange('bucketConfiguration.bucket.region', v)}
              options={[
                { value: '', label: 'Select a region...' },
                ...regions.map((r) => ({ value: r.id, label: r.name })),
              ]}
              helpText="AWS region where the bucket resides"
              disabled={s3Loading}
            />
            <SettingsTextInput
              name="s3-bucket"
              label="Bucket"
              value={bucket.name || ''}
              onChange={(v) => onChange('bucketConfiguration.bucket.name', v)}
              placeholder="my-blob-store-bucket"
              helpText="S3 bucket name (3–63 characters)"
              required
              error={validationErrors['bucketConfiguration.bucket.name']}
            />
            <SettingsTextInput
              name="s3-prefix"
              label="Prefix"
              value={bucket.prefix || ''}
              onChange={(v) => onChange('bucketConfiguration.bucket.prefix', v)}
              placeholder="nexus/"
              helpText="Optional path prefix within the bucket"
            />
            {isProEdition && (
              <SettingsCheckbox
                name="s3-presigned"
                label="Pre-Signed URL"
                description="Redirect downloads to S3 for better performance"
                checked={config.preSignedUrlEnabled || false}
                onChange={(v) => onChange('bucketConfiguration.preSignedUrlEnabled', v)}
              />
            )}
          </>
        )}

        {selectedType === BLOB_STORE_TYPE_IDS.AZURE && (
          <>
            <SettingsTextInput
              name="azure-account"
              label="Account Name"
              value={(config as Record<string, string>).accountName || ''}
              onChange={(v) => onChange('bucketConfiguration.accountName', v)}
              placeholder="mystorageaccount"
              helpText="Azure Storage account name"
              required
              error={validationErrors['bucketConfiguration.accountName']}
            />
            <SettingsTextInput
              name="azure-container"
              label="Container Name"
              value={(config as Record<string, string>).containerName || ''}
              onChange={(v) => onChange('bucketConfiguration.containerName', v)}
              placeholder="nexus-blobs"
              helpText="Container for blobs; created if it does not exist"
              required
              error={validationErrors['bucketConfiguration.containerName']}
            />
          </>
        )}

        {selectedType === BLOB_STORE_TYPE_IDS.GOOGLE && (
          <>
            <SettingsTextInput
              name="google-project"
              label="Project ID"
              value={((config as Record<string, unknown>)?.bucket as Record<string, string>)?.projectId || ''}
              onChange={(v) => onChange('bucketConfiguration.bucket.projectId', v)}
              placeholder="my-gcp-project"
              helpText="Optional GCP project ID"
            />
            <SettingsTextInput
              name="google-bucket"
              label="Bucket"
              value={((config as Record<string, unknown>)?.bucket as Record<string, string>)?.name || ''}
              onChange={(v) => onChange('bucketConfiguration.bucket.name', v)}
              placeholder="my-nexus-bucket"
              helpText="GCP bucket name (3–63 characters)"
              required
              error={validationErrors['bucketConfiguration.bucket.name']}
            />
            <SettingsTextInput
              name="google-prefix"
              label="Prefix"
              value={((config as Record<string, unknown>)?.bucket as Record<string, string>)?.prefix || ''}
              onChange={(v) => onChange('bucketConfiguration.bucket.prefix', v)}
              placeholder="nexus/"
              helpText="Optional path prefix"
            />
          </>
        )}

        {selectedType === BLOB_STORE_TYPE_IDS.GROUP && (
          <>
            <SettingsTransferList
              name="members"
              label="Members"
              helpText="Select blob stores to include in this group"
              availableItems={availableOptions}
              selectedItems={selectedOptions}
              availableLabel="Available"
              selectedLabel="Selected"
              getItemId={(item) => (item as { value: string }).value}
              getItemLabel={(item) => (item as { label: string }).label}
              onChange={(items) => onChange('members', items.map((i) => (i as { value: string }).value))}
              disabled={groupLoading}
            />
            <SettingsSelect
              name="fill-policy"
              label="Fill Policy"
              value={data.fillPolicy || ''}
              onChange={(v) => onChange('fillPolicy', v)}
              options={FILL_POLICY_OPTIONS}
              helpText="How blobs are distributed across members"
              required
              error={validationErrors.fillPolicy}
              disabled={groupLoading}
            />
          </>
        )}
      </SettingsFormSection>
    </>
  );
}
