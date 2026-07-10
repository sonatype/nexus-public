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
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

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
        title={UIStrings.APT.SETTINGS.title}
        description={UIStrings.APT.SETTINGS.description}
      >
        {isProxy && (
          <SettingsCheckbox
            name="apt-enforceDistribution"
            label={UIStrings.APT.SETTINGS.ENFORCE_DISTRIBUTION.label}
            checked={formData.apt?.enforceDistribution ?? false}
            onChange={(checked) => onNestedChange('apt', { enforceDistribution: checked })}
            description={UIStrings.APT.SETTINGS.ENFORCE_DISTRIBUTION.description}
          />
        )}

        <SettingsTextInput
          name="apt-distribution"
          label={UIStrings.APT.SETTINGS.DISTRIBUTION.label}
          value={formData.apt?.distribution || ''}
          onChange={(value) => onNestedChange('apt', { distribution: value })}
          helpText={UIStrings.APT.SETTINGS.DISTRIBUTION.helpText}
          placeholder={UIStrings.APT.SETTINGS.DISTRIBUTION.placeholder}
          required={isHosted || formData.apt?.enforceDistribution}
        />

        {isProxy && (
          <SettingsCheckbox
            name="apt-flat"
            label={UIStrings.APT.SETTINGS.FLAT.label}
            checked={formData.apt?.flat ?? false}
            onChange={(checked) => onNestedChange('apt', { flat: checked })}
            description={UIStrings.APT.SETTINGS.FLAT.description}
          />
        )}
      </SettingsFormSection>

      {showSigning && (
        <SettingsFormSection
          title={UIStrings.APT.SIGNING.title}
          description={UIStrings.APT.SIGNING.description}
        >
          <SettingsTextArea
            name="apt-keypair"
            label={UIStrings.APT.SIGNING.KEYPAIR.label}
            value={formData.aptSigning?.keypair || ''}
            onChange={(value) => onNestedChange('aptSigning', { keypair: value })}
            helpText={UIStrings.APT.SIGNING.KEYPAIR.helpText}
            required={isHosted}
            rows={8}
            monospace
          />

          <SettingsPasswordInput
            name="apt-passphrase"
            label={UIStrings.APT.SIGNING.PASSPHRASE.label}
            value={formData.aptSigning?.passphrase || ''}
            onChange={(value) => onNestedChange('aptSigning', { passphrase: value })}
            helpText={UIStrings.APT.SIGNING.PASSPHRASE.helpText}
            showToggle={false}
          />
        </SettingsFormSection>
      )}
    </>
  );
}

export default AptFacet;
