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

import {
  RepositoryFormData,
  RepositoryFormErrors,
  BlobStore,
} from '../types';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

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
  const handleBlobStoreChange = (value: string) => {
    onNestedChange('storage', { blobStoreName: value });
  };

  const handleContentValidationChange = (checked: boolean) => {
    onNestedChange('storage', { strictContentTypeValidation: checked });
  };

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

