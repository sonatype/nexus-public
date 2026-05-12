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

interface YumFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  repoType: 'hosted' | 'proxy' | 'group';
}

const DEPLOY_POLICY_OPTIONS = [
  { value: 'STRICT', label: 'Strict' },
  { value: 'PERMISSIVE', label: 'Permissive' },
];

/**
 * YumFacet - Yum/RPM-specific repository configuration
 *
 * Fields:
 * - Repodata Depth (0-5)
 * - Deploy Policy (Strict/Permissive) for hosted repos
 */
export function YumFacet({
  formData,
  onNestedChange,
  errors,
  repoType,
}: YumFacetProps) {
  const isHosted = repoType === 'hosted';

  return (
    <SettingsFormSection
      title="Yum Settings"
      description="Yum/RPM repository configuration"
    >
      <SettingsTextInput
        name="yum-repodataDepth"
        label="Repodata Depth"
        value={formData.yum?.repodataDepth?.toString() || '0'}
        onChange={(value) => onNestedChange('yum', {
          repodataDepth: parseInt(value, 10) || 0,
        })}
        helpText="Specifies the repository depth where repodata folder(s) are created (0-5)"
        type="number"
        required
      />

      {isHosted && (
        <SettingsSelect
          name="yum-deployPolicy"
          label="Deploy Policy"
          value={formData.yum?.deployPolicy || 'STRICT'}
          onChange={(value) => onNestedChange('yum', { deployPolicy: value as 'STRICT' | 'PERMISSIVE' })}
          options={DEPLOY_POLICY_OPTIONS}
          helpText="Validate that RPM deployments comply with the deployed version"
        />
      )}
    </SettingsFormSection>
  );
}

export default YumFacet;
