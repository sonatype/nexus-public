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
import { SettingsFormSection, SettingsTextInput, SettingsCheckbox } from '../../../../../shared/form';
import { RepositoryFormData } from '../types';

interface PyPiFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(key: K, updates: Partial<RepositoryFormData[K]>) => void;
  showFirewallFeatures?: boolean;
}

/**
 * PyPiFacet - PyPI proxy-specific configuration
 */
export function PyPiFacet({ formData, onNestedChange, showFirewallFeatures = false }: PyPiFacetProps) {
  return (
    <SettingsFormSection title="PyPI Settings">
      <SettingsTextInput
        name="pypi-indexPath"
        label="Remote Index Path"
        value={formData.pypi?.indexPath ?? '/simple'}
        onChange={(value) => onNestedChange('pypi', { indexPath: value })}
        helpText='Path appended to the remote URL for PyPI Simple API access. Use "/simple" (default) for standard PyPI repositories like PyPI.org, or leave empty for root-path repositories like pypi.nvidia.com or pypi.fury.io.'
        placeholder="/simple"
      />
      {showFirewallFeatures && (
        <div data-analytics-id="nxrm-repository-pypi-toggle-remove-quarantined">
          <SettingsCheckbox
            name="pypi-removeQuarantinedVersions"
            label="Filter component versions that fail Sonatype Repository Firewall policy"
            checked={formData.pypi?.removeQuarantinedVersions ?? false}
            onChange={(checked) => onNestedChange('pypi', { removeQuarantinedVersions: checked })}
            description="If enabled, automatically filter component versions from metadata that fail Sonatype Repository Firewall policy at the Proxy stage."
          />
        </div>
      )}
    </SettingsFormSection>
  );
}

export default PyPiFacet;
