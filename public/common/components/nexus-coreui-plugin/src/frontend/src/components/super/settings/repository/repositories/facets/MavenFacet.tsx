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
} from '../../../../shared/form';

import { RepositoryFormData, RepositoryFormErrors } from '../types';

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
  { value: 'RELEASE', label: 'Release' },
  { value: 'SNAPSHOT', label: 'Snapshot' },
  { value: 'MIXED', label: 'Mixed' },
];

const LAYOUT_POLICY_OPTIONS = [
  { value: 'STRICT', label: 'Strict' },
  { value: 'PERMISSIVE', label: 'Permissive' },
];

const CONTENT_DISPOSITION_OPTIONS = [
  { value: 'INLINE', label: 'Inline' },
  { value: 'ATTACHMENT', label: 'Attachment' },
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
  return (
    <SettingsFormSection
      title="Maven 2"
      description="Maven-specific repository configuration"
    >
      <SettingsSelect
        name="maven-versionPolicy"
        label="Version Policy"
        value={formData.maven?.versionPolicy || 'RELEASE'}
        onChange={(value) => onNestedChange('maven', { versionPolicy: value as 'RELEASE' | 'SNAPSHOT' | 'MIXED' })}
        options={VERSION_POLICY_OPTIONS}
        helpText="Controls what type of artifacts can be deployed to this repository"
        disabled={isEdit}
        required
      />

      <SettingsSelect
        name="maven-layoutPolicy"
        label="Layout Policy"
        value={formData.maven?.layoutPolicy || 'STRICT'}
        onChange={(value) => onNestedChange('maven', { layoutPolicy: value as 'STRICT' | 'PERMISSIVE' })}
        options={LAYOUT_POLICY_OPTIONS}
        helpText="Validates that all paths are Maven artifact or metadata paths"
      />

      <SettingsSelect
        name="maven-contentDisposition"
        label="Content Disposition"
        value={formData.maven?.contentDisposition || 'ATTACHMENT'}
        onChange={(value) => onNestedChange('maven', { contentDisposition: value as 'INLINE' | 'ATTACHMENT' })}
        options={CONTENT_DISPOSITION_OPTIONS}
        helpText="Controls whether content is displayed inline or downloaded as an attachment"
      />

      {(formData.maven?.contentDisposition || 'ATTACHMENT') === 'INLINE' && (
        <SettingsAlert type="warning">
          Serving content inline allows uploaded HTML to render on a trusted Nexus URL, which
          can be exploited for phishing attacks against other users.
        </SettingsAlert>
      )}
    </SettingsFormSection>
  );
}

export default MavenFacet;
