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
  SettingsSelect,
  SettingsCheckbox,
} from '../../../../../shared/form';
import { useIsEnterpriseEdition } from '../../../../../shared/hooks';
import { ExtJS } from '../../../../../../../interface/ExtJS';
import { Permissions } from '../../../../../../../constants/Permissions';

import {
  RepositoryFormData,
  RepositoryFormErrors,
  BlobStore,
  StorageConfig,
} from '../types';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';
import { getCdnDefaultForNewRepositories } from '../repositoryFormMachine';
import { RepositoryGlobalDistribution, type RepositoryGlobalDistributionMode } from './RepositoryGlobalDistribution';

interface StorageFacetProps {
  formData: RepositoryFormData;
  onChange: (updates: Partial<RepositoryFormData>) => void;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  isEdit?: boolean;
  isCloud?: boolean;
  blobStores: BlobStore[];
}

/**
 * StorageFacet - Blob store and content validation settings
 */
export function StorageFacet({
  formData,
  onChange,
  onNestedChange,
  errors,
  isEdit,
  isCloud = false,
  blobStores,
}: StorageFacetProps) {
  const isEnterprise = useIsEnterpriseEdition();
  const hasGlobalDistribution = formData.storage?.globalDistribution !== undefined;
  const blobStoreName = formData.storage?.blobStoreName || '';
  const selectedBlobStore = blobStores.find((store) => store.name === blobStoreName);

  const isCdnBlobStore = (name?: string) =>
    Boolean(blobStores.find((store) => store.name === name)?.globalDistributionEnabled);

  // Cloud provisions a blob store per repository at save time, so there is none to inspect here.
  const globalDistributionAvailable = isCloud || isCdnBlobStore(blobStoreName);

  let globalDistributionMode: RepositoryGlobalDistributionMode | undefined;
  if (!isEnterprise) {
    globalDistributionMode = 'unlicensed';
  } else if (hasGlobalDistribution) {
    if (isCloud) {
      globalDistributionMode = 'available';
    } else if (blobStoreName) {
      // A blob store missing from the list (e.g. the list failed to load) can't be checked, so
      // nothing is shown rather than a false "not enabled" state.
      if (selectedBlobStore) {
        globalDistributionMode = globalDistributionAvailable ? 'available' : 'blobStoreOff';
      }
    } else if (blobStores.length > 0) {
      globalDistributionMode = 'blobStoreOff';
    }
  }

  const hasUser = ExtJS.useUser() ?? false;
  const canReadBlobStores = ExtJS.usePermission(() => ExtJS.checkPermission(Permissions.BLOB_STORES.READ), [hasUser]);
  // Only S3 blob stores can serve Global Distribution, so only they get a Configure Blob Store link.
  const isS3BlobStore = selectedBlobStore?.type?.toLowerCase() === 's3';
  const blobStoreHref =
    !isCloud && canReadBlobStores && isS3BlobStore
      ? `#preview/admin/repository/blobstores/s3/${encodeURIComponent(blobStoreName)}`
      : undefined;

  // Re-seeds from the CDN default when a different blob store is picked, clearing it when the new
  // blob store cannot serve global distribution. Without an Enterprise license the value is left as is.
  const handleBlobStoreChange = (value: string) => {
    const updates: Partial<StorageConfig> = { blobStoreName: value };
    if (isEnterprise && hasGlobalDistribution) {
      updates.globalDistribution = isCdnBlobStore(value) && getCdnDefaultForNewRepositories();
    }
    onNestedChange('storage', updates);
  };

  const handleContentValidationChange = (checked: boolean) => {
    onNestedChange('storage', { strictContentTypeValidation: checked });
  };

  const handleGlobalDistributionChange = (checked: boolean) => {
    onNestedChange('storage', { globalDistribution: checked });
  };

  // Clears a value the selected blob store cannot serve, for the loads that bypass
  // handleBlobStoreChange. Without an Enterprise license the value is left as is.
  React.useEffect(() => {
    if (
      isEnterprise &&
      hasGlobalDistribution &&
      blobStores.length > 0 &&
      !globalDistributionAvailable &&
      formData.storage?.globalDistribution
    ) {
      onNestedChange('storage', { globalDistribution: false });
    }
  }, [
    isEnterprise,
    hasGlobalDistribution,
    blobStores.length,
    globalDistributionAvailable,
    formData.storage?.globalDistribution,
    onNestedChange,
  ]);

  const blobStoreOptions = blobStores.map((store) => ({
    value: store.name,
    label: store.name,
  }));

  return (
    <SettingsFormSection title={UIStrings.STORAGE.SECTION.title}>
      {!isCloud && (
        <SettingsSelect
          name="storage-blobStoreName"
          label={UIStrings.STORAGE.BLOB_STORE.label}
          value={formData.storage?.blobStoreName || ''}
          onChange={handleBlobStoreChange}
          options={[
            { value: '', label: UIStrings.STORAGE.BLOB_STORE.selectPlaceholder },
            ...blobStoreOptions,
          ]}
          error={errors?.storage?.blobStoreName}
          disabled={isEdit}
          required
          helpText={isEdit ? UIStrings.STORAGE.BLOB_STORE.editHelpText : UIStrings.STORAGE.BLOB_STORE.helpText}
        />
      )}

      {globalDistributionMode && (
        <RepositoryGlobalDistribution
          mode={globalDistributionMode}
          checked={formData.storage?.globalDistribution}
          onChange={handleGlobalDistributionChange}
          blobStoreName={isCloud ? undefined : blobStoreName || undefined}
          blobStoreHref={blobStoreHref}
          isS3BlobStore={isS3BlobStore}
        />
      )}

      <SettingsCheckbox
        name="storage-strictContentTypeValidation"
        label={UIStrings.STORAGE.STRICT_CONTENT_VALIDATION.label}
        checked={formData.storage?.strictContentTypeValidation ?? true}
        onChange={handleContentValidationChange}
        description={UIStrings.STORAGE.STRICT_CONTENT_VALIDATION.description}
      />
    </SettingsFormSection>
  );
}

export default StorageFacet;

