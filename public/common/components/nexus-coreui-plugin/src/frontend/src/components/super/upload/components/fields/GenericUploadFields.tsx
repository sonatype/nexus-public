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

import {
  SettingsTextInput,
  SettingsSelect,
  SettingsCheckbox,
  SettingsFormSection,
} from '../../../../../components/super/shared/form';

import type {
  UploadFieldProps,
  FormatFieldsProps,
  UploadComponentField,
} from '../../upload.types';

import './GenericUploadFields.scss';

const FIELD_HELP_TEXT: Record<string, string> = {
  groupId: 'The Maven group identifier (e.g., com.example.myapp)',
  artifactId: 'The Maven artifact name (e.g., my-library)',
  version: 'Artifact version (e.g., 1.0.0, 1.0-SNAPSHOT)',
  packaging: 'Packaging type (e.g., jar, war, pom)',
  classifier: 'Optional classifier for variants (e.g., sources, javadoc)',
  extension: 'File extension for this asset (e.g., jar, pom)',
  'generate-pom': 'Automatically creates a POM file with the specified coordinates',
  tag: 'Optional tag to categorize components',
  directory: 'Target directory path (e.g., /path/to/files)',
  id: 'Unique package identifier (e.g., MyCompany.MyPackage)',
  name: 'Package name using lowercase with hyphens (e.g., my-package)',
  filename: 'Name of the file being uploaded',
  path: 'Storage path within the repository',
  'docker.imageName': 'Docker image name (e.g., nginx, mycompany/myimage)',
  'docker.imageTag': 'Docker image tag (e.g., latest, 1.0.0)',
};

const FIELD_PLACEHOLDERS: Record<string, string> = {
  groupId: 'com.example.myapp',
  artifactId: 'my-library',
  version: '1.0.0',
  packaging: 'jar',
  classifier: 'sources',
  extension: 'jar',
  tag: 'release-candidate',
  directory: '/path/to/files',
  filename: 'my-file.txt',
  id: 'MyCompany.MyPackage',
  name: 'my-package',
  path: '/path/to/asset',
  'docker.imageName': 'mycompany/myimage',
  'docker.imageTag': 'latest',
};

function getHelpText(fieldName: string, backendHelp?: string): string {
  return backendHelp || FIELD_HELP_TEXT[fieldName] || '';
}

/**
 * Renders a single form field using the shared Settings* components.
 */
export function UploadField({
  field,
  value,
  onChange,
  error,
  disabled = false,
}: UploadFieldProps): JSX.Element {
  const label = field.displayName || field.name;
  const helpText = getHelpText(field.name, field.helpText);
  const placeholder = FIELD_PLACEHOLDERS[field.name] || '';

  if (field.type === 'BOOLEAN') {
    return (
      <SettingsCheckbox
        name={field.name}
        label={label}
        checked={value as boolean}
        onChange={(checked: boolean) => onChange(checked)}
        description={helpText}
        disabled={disabled}
      />
    );
  }

  if (field.type === 'SELECT' && field.selectOptions) {
    return (
      <SettingsSelect
        name={field.name}
        label={label}
        value={value as string}
        onChange={(val: string) => onChange(val)}
        options={field.selectOptions.map((o) => ({ value: o, label: o }))}
        helpText={helpText}
        error={error || ''}
        required={!field.optional}
        disabled={disabled}
      />
    );
  }

  return (
    <SettingsTextInput
      name={field.name}
      label={label}
      value={value as string}
      onChange={(val: string) => onChange(val)}
      helpText={helpText}
      error={error || ''}
      required={!field.optional}
      disabled={disabled}
      placeholder={placeholder}
    />
  );
}

/**
 * Renders all fields for a generic/unknown format using shared form components.
 */
export function GenericUploadFields({
  fieldsByGroup,
  values,
  onChange,
  errors,
  disabledFields = new Set(),
}: FormatFieldsProps): JSX.Element {
  const groupNames = Object.keys(fieldsByGroup);

  if (groupNames.length === 0) {
    return <></>;
  }

  return (
    <Box className="nxrm-generic-upload-fields" data-testid="generic-upload-fields">
      {groupNames.map((groupName) => {
        const fields = fieldsByGroup[groupName];

        return (
          <SettingsFormSection
            key={groupName}
            title={groupName}
          >
            {fields.map((field: UploadComponentField) => (
              <UploadField
                key={field.name}
                field={field}
                value={values[field.name] ?? (field.type === 'BOOLEAN' ? false : '')}
                onChange={(value) => onChange(field.name, value)}
                error={errors[field.name]}
                disabled={disabledFields.has(field.name)}
              />
            ))}
          </SettingsFormSection>
        );
      })}
    </Box>
  );
}

export default GenericUploadFields;
