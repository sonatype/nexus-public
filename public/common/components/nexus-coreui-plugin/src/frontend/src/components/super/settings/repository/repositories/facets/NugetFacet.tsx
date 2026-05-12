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
} from '../../../../shared/form';

import { RepositoryFormData, RepositoryFormErrors } from '../types';

interface NugetFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
}

const NUGET_VERSION_OPTIONS = [
  { value: 'V2', label: 'NuGet V2' },
  { value: 'V3', label: 'NuGet V3' },
];

/**
 * NugetFacet - NuGet proxy-specific repository configuration
 *
 * Fields:
 * - Query Cache Item Max Age
 * - NuGet Protocol Version (V2/V3)
 */
export function NugetFacet({
  formData,
  onNestedChange,
  errors,
}: NugetFacetProps) {
  return (
    <SettingsFormSection
      title="NuGet Settings"
      description="NuGet proxy repository configuration"
    >
      <SettingsTextInput
        name="nuget-queryCacheItemMaxAge"
        label="Query Cache Item Max Age"
        value={formData.nugetProxy?.queryCacheItemMaxAge?.toString() || '3600'}
        onChange={(value) => onNestedChange('nugetProxy', {
          queryCacheItemMaxAge: parseInt(value, 10) || 3600,
        })}
        helpText="How long to cache query results from the proxied repository (in seconds)"
        type="number"
      />

      <SettingsSelect
        name="nuget-nugetVersion"
        label="NuGet Protocol Version"
        value={formData.nugetProxy?.nugetVersion || 'V3'}
        onChange={(value) => onNestedChange('nugetProxy', { nugetVersion: value as 'V2' | 'V3' })}
        options={NUGET_VERSION_OPTIONS}
        helpText="NuGet protocol version to use for proxy communication"
      />
    </SettingsFormSection>
  );
}

export default NugetFacet;
