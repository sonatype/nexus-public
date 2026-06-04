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
  SettingsSelect,
  SettingsCheckbox,
} from '../../../../../shared/form';

import {
  RepositoryFormData,
  RepositoryFormErrors,
  WRITE_POLICY_OPTIONS,
  WritePolicy,
} from '../types';

interface HostedFacetProps {
  formData: RepositoryFormData;
  onChange: (updates: Partial<RepositoryFormData>) => void;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
}

/**
 * HostedFacet - Hosted repository specific settings
 */
export function HostedFacet({
  formData,
  onChange,
  onNestedChange,
  errors,
}: HostedFacetProps) {
  const handleWritePolicyChange = (value: string) => {
    onNestedChange('storage', { writePolicy: value as WritePolicy });
  };

  const handleProprietaryChange = (checked: boolean) => {
    onNestedChange('component', { proprietaryComponents: checked });
  };

  return (
    <SettingsFormSection title="Hosted">
      <SettingsSelect
        name="storage-writePolicy"
        label="Deployment Policy"
        value={formData.storage?.writePolicy || 'ALLOW_ONCE'}
        onChange={handleWritePolicyChange}
        options={WRITE_POLICY_OPTIONS}
        helpText="Controls if deployments of and updates to artifacts are allowed"
      />

      <SettingsCheckbox
        name="component-proprietaryComponents"
        label="Proprietary Components"
        checked={formData.component?.proprietaryComponents ?? false}
        onChange={handleProprietaryChange}
        description="Components in this repository count as proprietary for firewall"
      />
    </SettingsFormSection>
  );
}

export default HostedFacet;

