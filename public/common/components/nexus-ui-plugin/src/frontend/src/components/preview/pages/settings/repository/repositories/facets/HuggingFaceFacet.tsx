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

import React, { useMemo } from 'react';

import { ExtJS } from '@sonatype/nexus-ui-plugin';

import {
  SettingsFormSection,
  SettingsCheckbox,
} from '../../../../../shared/form';

import { RepositoryFormData } from '../types';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';
import FeatureFlags from '../../../../../../../constants/FeatureFlags';

const { HUGGING_FACE_XET_ENABLED } = FeatureFlags;

interface HuggingFaceFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
}

/**
 * HuggingFaceFacet - HuggingFace proxy transfer-protocol (LFS/XET) toggle.
 *
 * Rendered only when the {@code nexus.format.huggingface.xet.enabled} feature
 * flag is on. When disabled, the legacy LFS path is used; when enabled, XET
 * is used for large file transfers.
 *
 * Defaults (NEXUS-48685):
 *   - Existing pre-feature repositories carry no persisted `xet` block; their
 *     GET responses omit the field entirely (Jackson NON_NULL). formData.xet
 *     is undefined and the checkbox renders UNCHECKED, preserving the legacy
 *     LFS behavior for repos upgraded across the feature boundary.
 *   - New repositories are seeded with `xet.enabled=true` in
 *     buildDefaultFormData so the checkbox renders CHECKED at create time.
 */
export function HuggingFaceFacet({ formData, onNestedChange }: HuggingFaceFacetProps) {
  const xetFeatureEnabled = useMemo(() => {
    try {
      return Boolean(ExtJS.state().getValue(HUGGING_FACE_XET_ENABLED));
    } catch {
      return false;
    }
  }, []);

  if (!xetFeatureEnabled) {
    return null;
  }

  const isXetEnabled = formData.xet?.enabled ?? false;

  const handleToggle = (checked: boolean) => {
    onNestedChange('xet', { enabled: checked });
  };

  return (
    <SettingsFormSection
      title={UIStrings.HUGGINGFACE.SECTION.title}
      description={UIStrings.HUGGINGFACE.SECTION.description}
    >
      <SettingsCheckbox
        name="xet-enabled"
        label={UIStrings.HUGGINGFACE.ENABLE_XET.label}
        checked={isXetEnabled}
        onChange={handleToggle}
        description={UIStrings.HUGGINGFACE.ENABLE_XET.description}
      />
    </SettingsFormSection>
  );
}

export default HuggingFaceFacet;
