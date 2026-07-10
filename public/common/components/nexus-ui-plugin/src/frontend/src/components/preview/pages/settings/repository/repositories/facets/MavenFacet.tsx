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
  SettingsAlert,
  SettingsFormSection,
  SettingsSelect,
} from '../../../../../shared/form';

import { RepositoryFormData, RepositoryFormErrors } from '../types';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

interface MavenFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  isEdit?: boolean;
}

const VERSION_POLICY_OPTIONS = [
  { value: 'RELEASE', label: UIStrings.MAVEN.VERSION_POLICY.RELEASE },
  { value: 'SNAPSHOT', label: UIStrings.MAVEN.VERSION_POLICY.SNAPSHOT },
  { value: 'MIXED', label: UIStrings.MAVEN.VERSION_POLICY.MIXED },
];

const LAYOUT_POLICY_OPTIONS = [
  { value: 'STRICT', label: UIStrings.MAVEN.LAYOUT_POLICY.STRICT },
  { value: 'PERMISSIVE', label: UIStrings.MAVEN.LAYOUT_POLICY.PERMISSIVE },
];

const CONTENT_DISPOSITION_OPTIONS = [
  { value: 'INLINE', label: UIStrings.MAVEN.CONTENT_DISPOSITION.INLINE },
  { value: 'ATTACHMENT', label: UIStrings.MAVEN.CONTENT_DISPOSITION.ATTACHMENT },
];

/**
 * MavenFacet - Maven-specific repository configuration
 *
 * Fields:
 * - Version Policy: Release, Snapshot, or Mixed
 * - Layout Policy: Strict or Permissive path validation
 * - Content Disposition: Inline or Attachment for downloads
 */
export function MavenFacet({
  formData,
  onNestedChange,
  errors,
  isEdit,
}: MavenFacetProps) {
  const isCloud = ExtJS.useState?.(() => ExtJS.state()?.getValue?.('isCloud'));

  return (
    <SettingsFormSection
      title={UIStrings.MAVEN.SECTION.title}
      description={UIStrings.MAVEN.SECTION.description}
    >
      <div data-analytics-id="nxrm-repository-maven-version-policy">
        <SettingsSelect
          name="maven-versionPolicy"
          label={UIStrings.MAVEN.VERSION_POLICY.label}
          value={formData.maven?.versionPolicy || 'RELEASE'}
          onChange={(value) => onNestedChange('maven', { versionPolicy: value as 'RELEASE' | 'SNAPSHOT' | 'MIXED' })}
          options={VERSION_POLICY_OPTIONS}
          helpText={UIStrings.MAVEN.VERSION_POLICY.helpText}
          disabled={isEdit}
          required
        />
      </div>

      <div data-analytics-id="nxrm-repository-maven-layout-policy">
        <SettingsSelect
          name="maven-layoutPolicy"
          label={UIStrings.MAVEN.LAYOUT_POLICY.label}
          value={formData.maven?.layoutPolicy || 'STRICT'}
          onChange={(value) => onNestedChange('maven', { layoutPolicy: value as 'STRICT' | 'PERMISSIVE' })}
          options={LAYOUT_POLICY_OPTIONS}
          helpText={UIStrings.MAVEN.LAYOUT_POLICY.helpText}
        />
      </div>

      {!isCloud && (
        <>
          <div data-analytics-id="nxrm-repository-maven-content-disposition">
            <SettingsSelect
              name="maven-contentDisposition"
              label={UIStrings.MAVEN.CONTENT_DISPOSITION.label}
              value={formData.maven?.contentDisposition || 'ATTACHMENT'}
              onChange={(value) => onNestedChange('maven', { contentDisposition: value as 'INLINE' | 'ATTACHMENT' })}
              options={CONTENT_DISPOSITION_OPTIONS}
              helpText={UIStrings.MAVEN.CONTENT_DISPOSITION.helpText}
            />
          </div>

          {(formData.maven?.contentDisposition || 'ATTACHMENT') === 'INLINE' && (
            <SettingsAlert type="warning" mt="2">
              {UIStrings.MAVEN.CONTENT_DISPOSITION.inlineWarning}
            </SettingsAlert>
          )}
        </>
      )}
    </SettingsFormSection>
  );
}

export default MavenFacet;
