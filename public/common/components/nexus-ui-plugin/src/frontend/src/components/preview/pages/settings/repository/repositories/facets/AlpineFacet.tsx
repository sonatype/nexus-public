/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/oss/attributions.
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
  SettingsPasswordInput,
} from '../../../../../shared/form';

import { RepositoryFormData, RepositoryFormErrors } from '../types';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

interface AlpineFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  repoType: 'hosted' | 'proxy' | 'group';
}

/**
 * AlpineFacet - Alpine Linux package repository configuration
 *
 * Fields:
 * - RSA Signing keypair and passphrase (hosted and group)
 */
export function AlpineFacet({
  formData,
  onNestedChange,
  errors,
  repoType,
}: AlpineFacetProps) {
  // Show signing for hosted and group repositories
  const showSigning = repoType === 'hosted' || repoType === 'group';

  return (
    <>
      {showSigning && (
        <SettingsFormSection
          title={UIStrings.ALPINE.SIGNING.title}
          description={UIStrings.ALPINE.SIGNING.description}
        >
          <SettingsTextInput
            name="alpine-keypair"
            label={UIStrings.ALPINE.SIGNING.KEYPAIR.label}
            value={formData.alpineSigning?.keypair || ''}
            onChange={(value) => onNestedChange('alpineSigning', { keypair: value })}
            helpText={UIStrings.ALPINE.SIGNING.KEYPAIR.helpText}
            required
          />

          <SettingsPasswordInput
            name="alpine-passphrase"
            label={UIStrings.ALPINE.SIGNING.PASSPHRASE.label}
            value={formData.alpineSigning?.passphrase || ''}
            onChange={(value) => onNestedChange('alpineSigning', { passphrase: value })}
            helpText={UIStrings.ALPINE.SIGNING.PASSPHRASE.helpText}
          />
        </SettingsFormSection>
      )}
    </>
  );
}

export default AlpineFacet;
