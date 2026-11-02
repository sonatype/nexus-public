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
  SettingsTextArea,
  SettingsPasswordInput,
} from '../../../../../shared/form';

import { RepositoryFormData } from '../types';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';
import { type FacetErrors, resolveError } from './resolveError';

// PKCS#8, as the help text instructs; RsaUtils also accepts PKCS#1.
const KEYPAIR_PLACEHOLDER =
  '-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----';

interface AlpineFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: FacetErrors;
}

/**
 * AlpineFacet - RSA signing keypair and passphrase, on all three repository types.
 *
 * Strings and the required-on-every-type rule mirror Classic's AlpineSigningFacet; the REST
 * model is looser for hosted (@Valid only), so relax this only together with Classic.
 */
export function AlpineFacet({ formData, onNestedChange, errors }: AlpineFacetProps) {

  return (
    <SettingsFormSection
      title={UIStrings.ALPINE.SIGNING.title}
      description={UIStrings.ALPINE.SIGNING.description}
    >
      <SettingsTextArea
        name="alpine-keypair"
        label={UIStrings.ALPINE.SIGNING.KEYPAIR.label}
        value={formData.alpineSigning?.keypair || ''}
        onChange={(value: string) => onNestedChange('alpineSigning', { keypair: value })}
        helpText={UIStrings.ALPINE.SIGNING.KEYPAIR.helpText}
        placeholder={KEYPAIR_PLACEHOLDER}
        error={resolveError(errors, 'alpineSigning.keypair', (e) => e.alpineSigning?.keypair) || ''}
        rows={8}
        monospace
        required
      />

      <SettingsPasswordInput
        name="alpine-passphrase"
        label={UIStrings.ALPINE.SIGNING.PASSPHRASE.label}
        value={formData.alpineSigning?.passphrase || ''}
        onChange={(value: string) => onNestedChange('alpineSigning', { passphrase: value })}
        helpText={UIStrings.ALPINE.SIGNING.PASSPHRASE.helpText}
      />
    </SettingsFormSection>
  );
}

export default AlpineFacet;
