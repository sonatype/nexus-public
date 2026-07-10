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
} from '../../../../../shared/form';

import { RepositoryFormData, RepositoryFormErrors } from '../types';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

interface NugetFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
}

const NUGET_VERSION_OPTIONS = [
  { value: 'V2', label: UIStrings.NUGET.PROTOCOL_VERSION.V2 },
  { value: 'V3', label: UIStrings.NUGET.PROTOCOL_VERSION.V3 },
];

/**
 * NugetFacet - NuGet proxy-specific repository configuration
 *
 * Field order matches Classic UI:
 * 1. Protocol version
 * 2. Metadata query cache age
 */
export function NugetFacet({
  formData,
  onNestedChange,
  errors,
}: NugetFacetProps) {
  return (
    <SettingsFormSection
      title={UIStrings.NUGET.SECTION.title}
      description={UIStrings.NUGET.SECTION.description}
    >
      <div data-analytics-id="nxrm-repository-nuget-protocol-version">
        <SettingsSelect
          name="nuget-nugetVersion"
          label={UIStrings.NUGET.PROTOCOL_VERSION.label}
          value={formData.nugetProxy?.nugetVersion || 'V3'}
          onChange={(value) => onNestedChange('nugetProxy', { nugetVersion: value as 'V2' | 'V3' })}
          options={NUGET_VERSION_OPTIONS}
        />
      </div>

      <div data-analytics-id="nxrm-repository-nuget-query-cache-age">
        <SettingsTextInput
          name="nuget-queryCacheItemMaxAge"
          label={UIStrings.NUGET.QUERY_CACHE_AGE.label}
          value={formData.nugetProxy?.queryCacheItemMaxAge?.toString() ?? ''}
          onChange={(value) => {
            // Match the proven Maven max-age handler shape:
            // - empty / lone '-' -> undefined so the user can retype freely
            // - any parseable number is clamped to >= 0 (cache age can't be negative)
            // - non-numeric / NaN -> push undefined so the controlled input
            //   doesn't get stuck on a stale numeric default like 3600
            if (value === '' || value === '-') {
              onNestedChange('nugetProxy', { queryCacheItemMaxAge: undefined });
            } else {
              const parsed = parseInt(value, 10);
              if (Number.isNaN(parsed)) {
                onNestedChange('nugetProxy', { queryCacheItemMaxAge: undefined });
              } else {
                onNestedChange('nugetProxy', { queryCacheItemMaxAge: Math.max(0, parsed) });
              }
            }
          }}
          helpText={UIStrings.NUGET.QUERY_CACHE_AGE.helpText}
          type="number"
          error={errors?.nugetProxy?.queryCacheItemMaxAge}
        />
      </div>
    </SettingsFormSection>
  );
}

export default NugetFacet;
