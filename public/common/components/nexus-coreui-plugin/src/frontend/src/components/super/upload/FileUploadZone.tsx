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
import { Box, Button, Flex, IconButton, Separator, Text } from '@radix-ui/themes';
import { Plus, Trash2 } from 'lucide-react';

import { FileDropzone } from './components/FileDropzone';
import { UploadField } from './UploadField';
import type { AssetFieldData, UploadFieldDefinition, ValidationErrors } from './upload.types';
import { UPLOAD_FORM_STRINGS } from './upload.types';

interface FileUploadZoneProps {
  assets: AssetFieldData[];
  assetFields: UploadFieldDefinition[];
  validationErrors: ValidationErrors;
  multipleUpload: boolean;
  onFileChange: (assetIndex: number, files: File[]) => void;
  onAssetFieldChange: (assetIndex: number, fieldName: string, value: string | boolean) => void;
  onBlurAssetField?: (assetIndex: number, fieldName: string) => void;
  onAddAsset: () => void;
  onRemoveAsset: (assetIndex: number) => void;
}

/**
 * FileUploadZone component for managing multiple asset uploads.
 */
export function FileUploadZone({
  assets,
  assetFields,
  validationErrors,
  multipleUpload,
  onFileChange,
  onAssetFieldChange,
  onBlurAssetField,
  onAddAsset,
  onRemoveAsset,
}: FileUploadZoneProps): JSX.Element {
  return (
    <Box data-testid="file-upload-zone">
      {assets.map((asset, assetIndex) => (
        <Box key={assetIndex} className="upload-form-page__asset" mb="4">
          {assets.length > 1 && (
            <Flex justify="between" align="center" mb="3" className="upload-form-page__asset-header">
              <Flex align="center" gap="2">
                <div className="upload-form-page__asset-number">{assetIndex + 1}</div>
                <Text size="2" weight="medium">Asset {assetIndex + 1}</Text>
              </Flex>
              <IconButton
                type="button"
                variant="ghost"
                color="red"
                size="1"
                onClick={() => onRemoveAsset(assetIndex)}
                aria-label={UPLOAD_FORM_STRINGS.removeAsset}
              >
                <Trash2 size={14} />
              </IconButton>
            </Flex>
          )}

          <FileDropzone
            files={asset.file ? [asset.file] : []}
            onChange={(files) => onFileChange(assetIndex, files)}
            label="File"
            helpText={
              assetFields.find((f) => f.type === 'FILE')?.helpText ??
              'The artifact or asset file to upload. Drag and drop or click to browse. Accepted types vary by repository format (e.g., .jar and .pom for Maven).'
            }
            required
            error={validationErrors.assets?.[assetIndex]?.file ?? undefined}
            id={`asset-file-${assetIndex}`}
            testId={`input-asset-file-${assetIndex}`}
          />

          {assetFields.filter((f) => f.type !== 'FILE').length > 0 && (
            <Box mt="4">
              <Text as="p" size="1" color="gray" mb="3">
                Specify additional metadata for this asset. Hover over or focus each field for details.
              </Text>
              {assetFields
                .filter((field) => field.type !== 'FILE')
                .map((field) => (
                  <UploadField
                    key={field.name}
                    field={field}
                    value={asset[field.name] as string | boolean}
                    onChange={(val) => onAssetFieldChange(assetIndex, field.name, val)}
                    error={validationErrors.assets?.[assetIndex]?.[field.name]}
                    onBlur={onBlurAssetField ? () => onBlurAssetField(assetIndex, field.name) : undefined}
                  />
                ))}
            </Box>
          )}

          {assetIndex < assets.length - 1 && (
            <Separator my="4" size="4" className="upload-form-page__asset-separator" />
          )}
        </Box>
      ))}

      {multipleUpload && (
        <Button
          type="button"
          variant="soft"
          onClick={onAddAsset}
          mt="2"
          className="upload-form-page__add-asset"
          aria-label="Add another asset"
        >
          <Plus size={16} />
          Add Another Asset
        </Button>
      )}
    </Box>
  );
}

export default FileUploadZone;
