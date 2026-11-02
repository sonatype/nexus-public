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

import React, { useEffect } from 'react';

import {
  SettingsFormSection,
  SettingsCheckbox,
} from '../../../../../shared/form';

import { RepositoryFormData } from '../types';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

interface CargoFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  isCloud?: boolean;
}

/**
 * CargoFacet - controls whether a Cargo repository advertises `auth-required`
 * in the /config.json document it serves to Cargo clients.
 *
 * This is metadata advertising, not enforcement: it tells the client to attach
 * an Authorization header to index and download requests. It does not change who
 * may read the repository — that stays with Anonymous Access and repository
 * privileges.
 *
 * Applies to all three repository types. The backend has supported
 * cargo.requireAuthentication for proxy and group since NEXUS-44559 and for
 * hosted since NEXUS-55006; NEXUS-55104 exposes it in the Nexus One UI.
 *
 * Defaults: unchecked. NEXUS-44559 specified the API field as optional with a
 * default of false, and buildDefaultFormData seeds `cargo.requireAuthentication`
 * to false on create so the save payload always carries an explicit value.
 * Repositories created before the setting was exposed carry no persisted `cargo`
 * block, so formData.cargo is undefined on edit and the checkbox renders
 * unchecked.
 *
 * Not offered on Repo Cloud (NEXUS-47331 parity): Anonymous Access does not
 * exist there, so authentication is always required and the server forces the
 * value via the nexus.cargo.requireAuthentication.override property. Rendering a
 * control the server ignores would mislead the administrator.
 *
 * Hiding the control is not enough on its own, though: buildRepositoryConfig
 * still unconditionally emits config.cargo from whatever formData.cargo holds,
 * so a seeded/persisted `false` would silently be sent to the server even
 * though no checkbox is shown. The effect below keeps local form state in sync
 * with what the server actually forces, so the hidden control never implies a
 * value that contradicts what gets saved.
 */
export function CargoFacet({ formData, onNestedChange, isCloud = false }: CargoFacetProps) {
  useEffect(() => {
    if (isCloud && formData.cargo?.requireAuthentication !== true) {
      onNestedChange('cargo', { requireAuthentication: true });
    }
  }, [isCloud, formData.cargo?.requireAuthentication, onNestedChange]);

  if (isCloud) {
    return null;
  }

  const requireAuthentication = formData.cargo?.requireAuthentication ?? false;

  const handleToggle = (checked: boolean) => {
    onNestedChange('cargo', { requireAuthentication: checked });
  };

  return (
    <SettingsFormSection
      title={UIStrings.CARGO.SECTION.title}
      description={UIStrings.CARGO.SECTION.description}
    >
      <SettingsCheckbox
        name="cargo-require-authentication"
        label={UIStrings.CARGO.REQUIRE_AUTHENTICATION.label}
        checked={requireAuthentication}
        onChange={handleToggle}
      />
    </SettingsFormSection>
  );
}

export default CargoFacet;
