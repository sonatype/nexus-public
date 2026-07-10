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
  SettingsCheckbox,
} from '../../../../../shared/form';

import {
  RepositoryFormData,
  RepositoryFormErrors,
} from '../types';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

interface NegativeCacheFacetProps {
  formData: RepositoryFormData;
  onChange: (updates: Partial<RepositoryFormData>) => void;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
}

/**
 * NegativeCacheFacet - Negative cache settings for proxy repositories
 */
export function NegativeCacheFacet({
  formData,
  onNestedChange,
  errors,
}: NegativeCacheFacetProps) {
  const handleEnabledChange = (checked: boolean) => {
    onNestedChange('negativeCache', { enabled: checked });
  };

  const handleTimeToLiveChange = (value: string) => {
    // Pass value through to machine - validation will handle errors
    if (value === '' || value === '-') {
      onNestedChange('negativeCache', { timeToLive: undefined });
    } else {
      const numValue = parseInt(value, 10);
      // Only update if it's a valid number - otherwise let validation catch it
      if (!Number.isNaN(numValue)) {
        onNestedChange('negativeCache', { timeToLive: numValue });
      }
      // For invalid input like "abc", don't update - let current value persist
      // and validation will show error when form is submitted
    }
  };

  return (
    <SettingsFormSection title={UIStrings.NEGATIVE_CACHE.SECTION.title}>
      <SettingsCheckbox
        name="negativeCache-enabled"
        label={UIStrings.NEGATIVE_CACHE.ENABLED.label}
        checked={formData.negativeCache?.enabled ?? true}
        onChange={handleEnabledChange}
        description={UIStrings.NEGATIVE_CACHE.ENABLED.description}
      />

      <SettingsTextInput
        name="negativeCache-timeToLive"
        label={UIStrings.NEGATIVE_CACHE.TTL.label}
        value={formData.negativeCache?.timeToLive?.toString() ?? ''}
        onChange={handleTimeToLiveChange}
        type="number"
        helpText={UIStrings.NEGATIVE_CACHE.TTL.helpText}
        error={errors?.negativeCache?.timeToLive}
        disabled={!formData.negativeCache?.enabled}
      />
    </SettingsFormSection>
  );
}

export default NegativeCacheFacet;
