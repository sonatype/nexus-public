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
import { SettingsTextInput, SettingsSelect, SettingsCheckbox } from '../../shared/form';
import type { UploadFieldDefinition } from './upload.types';

/**
 * Renders an individual upload field based on its definition.
 */
export function UploadField({
  field,
  value,
  onChange,
  error,
  onBlur,
}: {
  field: UploadFieldDefinition;
  value: string | boolean;
  onChange: (value: string | boolean) => void;
  error?: string | null;
  onBlur?: () => void;
}): JSX.Element {
  const commonProps = {
    name: field.name,
    label: field.displayName || field.name,
    helpText: field.helpText,
    required: !field.optional,
  };

  if (field.type === 'BOOLEAN') {
    return (
      <SettingsCheckbox
        {...commonProps}
        checked={value as boolean}
        onChange={onChange}
        description={field.helpText}
      />
    );
  }

  if (field.type === 'SELECT' && field.selectOptions) {
    return (
      <SettingsSelect
        {...commonProps}
        value={value as string}
        onChange={onChange}
        options={field.selectOptions.map((o) => ({ value: o, label: o }))}
        error={error || ''}
      />
    );
  }

  return (
    <SettingsTextInput
      {...commonProps}
      value={value as string}
      onChange={onChange}
      onBlur={onBlur}
      error={error || ''}
    />
  );
}

export default UploadField;
