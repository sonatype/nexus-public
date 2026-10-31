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

import React, { useMemo } from 'react';
import { Box } from '@radix-ui/themes';

import type {
  UploadFieldRendererProps,
  UploadComponentField,
} from './upload.types';

import { GenericUploadFields } from './components/fields/GenericUploadFields';
import { MavenUploadFields } from './components/fields/MavenUploadFields';
import { NpmUploadFields } from './components/fields/NpmUploadFields';
import { RawUploadFields } from './components/fields/RawUploadFields';

/**
 * Groups component fields by their group property.
 */
function groupFieldsByGroup(
  fields: UploadComponentField[]
): Record<string, UploadComponentField[]> {
  return fields.reduce((acc, field) => {
    const group = field.group || 'Other';
    if (!acc[group]) {
      acc[group] = [];
    }
    acc[group].push(field);
    return acc;
  }, {} as Record<string, UploadComponentField[]>);
}

/**
 * Main upload field renderer component.


 *
 * Delegates to format-specific field components based on the repository format.
 * Supports Maven, npm, raw, and generic formats.
 *
 * @example
 * ```tsx
 * <UploadFieldRenderer
 *   format="maven2"
 *   definition={uploadDefinition}
 *   values={formValues}
 *   onChange={handleFieldChange}
 *   errors={fieldErrors}
 *   hasPomFile={hasPomFile}
 * />
 * ```
 */
export function UploadFieldRenderer({
  format,
  definition,
  values,
  onChange,
  errors,
  disabledFields = new Set(),
  hasPomFile = false,
  fieldsByGroup: preGroupedFields,
}: UploadFieldRendererProps): JSX.Element {
  // Group fields by their group property
  const fieldsByGroup = useMemo(() => {
    if (preGroupedFields) {
      return preGroupedFields;
    }
    const componentFields = (definition?.componentFields || []) as UploadComponentField[];
    return groupFieldsByGroup(componentFields);
  }, [definition, preGroupedFields]);

  // If no definition or no fields, return empty
  if (!definition || Object.keys(fieldsByGroup).length === 0) {
    return <Box data-testid="upload-field-renderer-empty" />;
  }

  // Common props for format-specific components
  const commonProps = {
    fieldsByGroup,
    values,
    onChange,
    errors,
    disabledFields,
    hasPomFile,
  };

  // Render format-specific component
  switch (format.toLowerCase()) {
    case 'maven2':
      return (
        <Box data-testid="upload-field-renderer">
          <MavenUploadFields {...commonProps} />
        </Box>
      );

    case 'npm':
      return (
        <Box data-testid="upload-field-renderer">
          <NpmUploadFields {...commonProps} />
        </Box>
      );

    case 'raw':
      return (
        <Box data-testid="upload-field-renderer">
          <RawUploadFields {...commonProps} />
        </Box>
      );

    default:
      // Use generic renderer for all other formats
      return (
        <Box data-testid="upload-field-renderer">
          <GenericUploadFields {...commonProps} />
        </Box>
      );
  }
}

export default UploadFieldRenderer;
