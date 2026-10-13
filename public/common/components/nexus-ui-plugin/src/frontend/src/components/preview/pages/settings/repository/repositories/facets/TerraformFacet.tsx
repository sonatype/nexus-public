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
  SettingsTextArea,
  SettingsPasswordInput,
} from '../../../../../shared/form';

import { RepositoryFormData } from '../types';
import type { ValidationErrors } from '../../../../../../../interface/form';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

const KEYPAIR_PLACEHOLDER =
  '-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----';

interface TerraformFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  // Flat, dotted-key error map as produced by validateRepository / the form
  // machine (ValidationErrors), e.g. `errors['terraformSigning.keypair']`.
  errors?: ValidationErrors;
  repoType: 'hosted' | 'proxy' | 'group';
}

/**
 * TerraformFacet - Terraform repository configuration
 *
 * Fields:
 * - GPG signing keypair and passphrase (hosted only)
 *
 * GPG signing is mandatory for Terraform hosted repositories: the REST model
 * (TerraformHostedRepositoryApiRequest) marks `terraformSigning` @NotNull and
 * its `keypair` @NotBlank, so a create that omits it is rejected server-side.
 * Proxy and group Terraform repositories have no signing configuration.
 */
export function TerraformFacet({
  formData,
  onNestedChange,
  errors,
  repoType,
}: TerraformFacetProps) {
  // Signing applies to hosted repositories only.
  if (repoType !== 'hosted') {
    return null;
  }

  return (
    <SettingsFormSection
      title={UIStrings.TERRAFORM.SIGNING.title}
      description={UIStrings.TERRAFORM.SIGNING.description}
    >
      <SettingsTextArea
        name="terraform-keypair"
        label={UIStrings.TERRAFORM.SIGNING.KEYPAIR.label}
        value={formData.terraformSigning?.keypair || ''}
        onChange={(value: string) => onNestedChange('terraformSigning', { keypair: value })}
        helpText={UIStrings.TERRAFORM.SIGNING.KEYPAIR.helpText}
        placeholder={KEYPAIR_PLACEHOLDER}
        error={errors?.['terraformSigning.keypair'] || ''}
        rows={6}
        required
      />

      <SettingsPasswordInput
        name="terraform-passphrase"
        label={UIStrings.TERRAFORM.SIGNING.PASSPHRASE.label}
        value={formData.terraformSigning?.passphrase || ''}
        onChange={(value: string) => onNestedChange('terraformSigning', { passphrase: value })}
        helpText={UIStrings.TERRAFORM.SIGNING.PASSPHRASE.helpText}
      />
    </SettingsFormSection>
  );
}

export default TerraformFacet;
