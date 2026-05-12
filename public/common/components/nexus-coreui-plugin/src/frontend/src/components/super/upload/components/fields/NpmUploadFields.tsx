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
import { Box, Text } from '@radix-ui/themes';
import { Package } from 'lucide-react';

import { SettingsTextInput, SettingsFormSection } from '../../../../../components/super/shared/form';
import { UploadField } from './GenericUploadFields';

import type { FormatFieldsProps, UploadComponentField } from '../../upload.types';
import { FORMAT_FIELD_STRINGS } from '../../upload.types';

import './NpmUploadFields.scss';

const NPM_FIELD_NAMES = {
  PACKAGE_NAME: 'name',
  VERSION: 'version',
} as const;

/**
 * Renders npm-specific upload fields using shared form components.
 */
export function NpmUploadFields({
  fieldsByGroup,
  values,
  onChange,
  errors,
  disabledFields = new Set(),
}: FormatFieldsProps): JSX.Element {
  const { npm } = FORMAT_FIELD_STRINGS;

  const allFields = Object.values(fieldsByGroup).flat();
  const packageNameField = allFields.find((f) => f.name === NPM_FIELD_NAMES.PACKAGE_NAME);
  const versionField = allFields.find((f) => f.name === NPM_FIELD_NAMES.VERSION);
  const otherFields = allFields.filter(
    (f) => f.name !== NPM_FIELD_NAMES.PACKAGE_NAME && f.name !== NPM_FIELD_NAMES.VERSION
  );

  if (allFields.length === 0) {
    return (
      <Box className="nxrm-npm-upload-fields" data-testid="npm-upload-fields">
        <SettingsFormSection title="npm Package" icon={<Package size={18} />}>
          <Text size="2" color="gray">
            Upload your npm package tarball (.tgz). Package metadata will be extracted from package.json.
          </Text>
        </SettingsFormSection>
      </Box>
    );
  }

  return (
    <Box className="nxrm-npm-upload-fields" data-testid="npm-upload-fields">
      <SettingsFormSection title="npm Package Details" icon={<Package size={18} />}>
        <Box className="nxrm-npm-upload-fields__grid">
          {packageNameField && (
            <SettingsTextInput
              name="npm-package-name"
              label={npm.packageNameLabel}
              value={(values[NPM_FIELD_NAMES.PACKAGE_NAME] as string) || ''}
              onChange={(val) => onChange(NPM_FIELD_NAMES.PACKAGE_NAME, val)}
              disabled={disabledFields.has(NPM_FIELD_NAMES.PACKAGE_NAME)}
              placeholder="@scope/package-name"
              helpText={npm.packageNameHelp}
              error={errors[NPM_FIELD_NAMES.PACKAGE_NAME] || ''}
              required={!packageNameField.optional}
            />
          )}

          {versionField && (
            <SettingsTextInput
              name="npm-version"
              label={npm.versionLabel}
              value={(values[NPM_FIELD_NAMES.VERSION] as string) || ''}
              onChange={(val) => onChange(NPM_FIELD_NAMES.VERSION, val)}
              disabled={disabledFields.has(NPM_FIELD_NAMES.VERSION)}
              placeholder="1.0.0"
              helpText={npm.versionHelp}
              error={errors[NPM_FIELD_NAMES.VERSION] || ''}
              required={!versionField.optional}
            />
          )}
        </Box>

        {otherFields.length > 0 && (
          <Box className="nxrm-npm-upload-fields__other-fields">
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

export default NpmUploadFields;
