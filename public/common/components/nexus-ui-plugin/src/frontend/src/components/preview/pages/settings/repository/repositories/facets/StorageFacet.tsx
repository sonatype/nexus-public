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
    <SettingsFormSection title="Storage">
      {!isCloud && (
        <SettingsSelect
          name="storage-blobStoreName"
          label="Blob Store"
          value={formData.storage?.blobStoreName || ''}
          onChange={handleBlobStoreChange}
          options={[
            { value: '', label: 'Select a blob store...' },
            ...blobStoreOptions,
          ]}
          error={errors?.storage?.blobStoreName}
          disabled={isEdit}
          required
          helpText={isEdit ? 'Blob store cannot be changed after creation' : 'Select the blob store used to store repository contents'}
        />
      )}

      <SettingsCheckbox
        name="storage-strictContentTypeValidation"
        label="Strict Content Type Validation"
        checked={formData.storage?.strictContentTypeValidation ?? true}
        onChange={handleContentValidationChange}
        description="Validate that all content uploaded to this repository is of a MIME type appropriate for the repository format"
      />
    </SettingsFormSection>
  );
}

export default StorageFacet;

