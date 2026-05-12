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
import { SettingsFormSection, SettingsCheckbox } from '../../../../shared/form';
import { RepositoryFormData } from '../types';

interface NpmFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(key: K, updates: Partial<RepositoryFormData[K]>) => void;
}

/**
 * NpmFacet - npm-specific proxy configuration (removeQuarantined)
 */
export function NpmFacet({ formData, onNestedChange }: NpmFacetProps) {
  return (
    <SettingsFormSection title="npm Settings" description="npm proxy repository configuration">
      <SettingsCheckbox
        name="npm-removeQuarantined"
        label="Remove Quarantined Versions"
        checked={formData.npm?.removeQuarantined ?? false}
        onChange={(checked) => onNestedChange('npm', { removeQuarantined: checked })}
        description="Remove quarantined versions from search and package metadata responses"
      />
    </SettingsFormSection>
  );
}

export default NpmFacet;
