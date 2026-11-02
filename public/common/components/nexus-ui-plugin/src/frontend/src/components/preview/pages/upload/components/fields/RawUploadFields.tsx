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
import { Box } from '@radix-ui/themes';
import { File } from 'lucide-react';

import { SettingsTextInput, SettingsFormSection } from '../../../../shared/form';
import { UploadField } from './GenericUploadFields';

import type { FormatFieldsProps, UploadComponentField } from '../../upload.types';
import { FORMAT_FIELD_STRINGS } from '../../upload.types';

import './RawUploadFields.scss';

const RAW_FIELD_NAMES = {
  DIRECTORY: 'directory',
} as const;

/**
 * Renders raw format upload fields using shared form components.
 */
export function RawUploadFields({
  fieldsByGroup,
  values,
  onChange,
  errors,
  disabledFields = new Set(),
}: FormatFieldsProps): JSX.Element {
  const { raw } = FORMAT_FIELD_STRINGS;

  const allFields = Object.values(fieldsByGroup).flat();
  const directoryField = allFields.find((f) => f.name === RAW_FIELD_NAMES.DIRECTORY);
  const otherFields = allFields.filter((f) => f.name !== RAW_FIELD_NAMES.DIRECTORY);

  return (
    <Box className="nxrm-raw-upload-fields" data-testid="raw-upload-fields">
      <SettingsFormSection title="Raw File Details" icon={<File size={18} />}>
        <Box className="nxrm-raw-upload-fields__grid">
          <SettingsTextInput
            name="raw-directory"
            label={directoryField?.displayName || raw.directoryLabel}
            value={(values[RAW_FIELD_NAMES.DIRECTORY] as string) || ''}
            onChange={(val) => onChange(RAW_FIELD_NAMES.DIRECTORY, val)}
            disabled={disabledFields.has(RAW_FIELD_NAMES.DIRECTORY)}
            placeholder={raw.directoryPlaceholder}
            helpText={directoryField?.helpText || raw.directoryHelp}
            error={errors[RAW_FIELD_NAMES.DIRECTORY] || ''}
            required={directoryField ? !directoryField.optional : false}
          />
        </Box>

        {otherFields.length > 0 && (
          <Box className="nxrm-raw-upload-fields__other-fields">
            {otherFields.map((field: UploadComponentField) => (
              <UploadField
                key={field.name}
                field={field}
                value={values[field.name] ?? (field.type === 'BOOLEAN' ? false : '')}
                onChange={(value) => onChange(field.name, value)}
                error={errors[field.name]}
                disabled={disabledFields.has(field.name)}
              />
            ))}
          </Box>
        )}
      </SettingsFormSection>
    </Box>
  );
}

export default RawUploadFields;
