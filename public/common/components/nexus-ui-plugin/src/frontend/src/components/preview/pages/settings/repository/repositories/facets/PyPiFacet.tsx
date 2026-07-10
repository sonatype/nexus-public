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
import { SettingsFormSection, SettingsTextInput } from '../../../../../shared/form';
import { RepositoryFormData } from '../types';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

interface PyPiFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
}

/**
 * PyPiFacet — PyPI proxy-specific configuration.
 *
 * Currently exposes only the Remote Index Path. The PCCS-related
 * `removeQuarantinedVersions` toggle that previously lived here was removed post-migration
 * STL-381: PCCS is now expressed as `firewall.mode = "PCCS"` on the typed repository config
 * (set via the Firewall tab's protection-level selector), and the migration step explicitly
 * deletes the legacy `attributes.pypi.removeQuarantinedVersions` field from migrated repos.
 * The legacy ExtJS PyPiProxy.js form was cleaned up at the same time; this matches it.
 */
export function PyPiFacet({ formData, onNestedChange }: PyPiFacetProps) {
  return (
    <SettingsFormSection title={UIStrings.PYPI.SECTION.title}>
      <SettingsTextInput
        name="pypi-indexPath"
        label={UIStrings.PYPI.INDEX_PATH.label}
        value={formData.pypi?.indexPath ?? '/simple'}
        onChange={(value) => onNestedChange('pypi', { indexPath: value })}
        helpText={UIStrings.PYPI.INDEX_PATH.helpText}
        placeholder={UIStrings.PYPI.INDEX_PATH.placeholder}
      />
    </SettingsFormSection>
  );
}

export default PyPiFacet;
