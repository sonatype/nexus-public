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
  SettingsTextArea,
  SettingsCheckbox,
  SettingsPasswordInput,
} from '../../../../../shared/form';

import { RepositoryFormData, RepositoryFormErrors } from '../types';

interface AptFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  repoType: 'hosted' | 'proxy' | 'group';
}

/**
 * AptFacet - APT/Debian-specific repository configuration
 *
 * Fields:
 * - Enforce Distribution (proxy only)
 * - Distribution (e.g., bionic, focal, jammy)
 * - Flat repository mode (proxy only)
 * - GPG Signing keypair and passphrase (hosted and proxy)
 */
export function AptFacet({
  formData,
  onNestedChange,
  errors,
  repoType,
}: AptFacetProps) {
  const isHosted = repoType === 'hosted';
  const isProxy = repoType === 'proxy';
  const showSigning = isHosted || isProxy;

  return (
    <>
      <SettingsFormSection
        title="APT Settings"
        description="Debian/APT repository configuration"
      >
        {isProxy && (
          <SettingsCheckbox
            name="apt-enforceDistribution"
            label="Enforce Distribution"
            checked={formData.apt?.enforceDistribution ?? false}
            onChange={(checked) => onNestedChange('apt', { enforceDistribution: checked })}
            description="Restrict the distribution field to the value configured below"
          />
        )}

        <SettingsTextInput
          name="apt-distribution"
          label="Distribution"
          value={formData.apt?.distribution || ''}
          onChange={(value) => onNestedChange('apt', { distribution: value })}
          helpText="Distribution to fetch (e.g., bionic, focal, jammy) or path for flat repositories"
          placeholder="e.g., bionic"
          required={isHosted || formData.apt?.enforceDistribution}
        />

        {isProxy && (
          <SettingsCheckbox
            name="apt-flat"
            label="Flat Repository"
            checked={formData.apt?.flat ?? false}
            onChange={(checked) => onNestedChange('apt', { flat: checked })}
            description="Is this repository flat (i.e., no distribution folder hierarchy)?"
          />
        )}
      </SettingsFormSection>

      {showSigning && (
        <SettingsFormSection
          title="APT Signing"
          description="GPG signing configuration for APT repositories"
        >
          <SettingsTextArea
            name="apt-keypair"
            label="GPG Signing Key"
            value={formData.aptSigning?.keypair || ''}
            onChange={(value) => onNestedChange('aptSigning', { keypair: value })}
            helpText="PEM encoded GPG signing key pair"
            required={isHosted}
            rows={8}
            monospace
          />

          <SettingsPasswordInput
            name="apt-passphrase"
            label="GPG Signing Key Passphrase"
            value={formData.aptSigning?.passphrase || ''}
            onChange={(value) => onNestedChange('aptSigning', { passphrase: value })}
            helpText="Passphrase for the GPG signing key (leave empty if key has no passphrase)"
            showToggle={false}
          />
        </SettingsFormSection>
      )}
    </>
  );
}

export default AptFacet;
