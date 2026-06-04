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
  onChange,
  onNestedChange,
  errors,
}: NegativeCacheFacetProps) {
  const handleEnabledChange = (checked: boolean) => {
    onNestedChange('negativeCache', { enabled: checked });
  };

  const handleTimeToLiveChange = (value: string) => {
    const numValue = parseInt(value, 10);
    onNestedChange('negativeCache', { timeToLive: isNaN(numValue) ? 1440 : numValue });
  };

  return (
    <SettingsFormSection title="Negative Cache">
      <SettingsCheckbox
        name="negativeCache-enabled"
        label="Negative Cache"
        checked={formData.negativeCache?.enabled ?? true}
        onChange={handleEnabledChange}
        description="Cache responses for content not present in the remote repository"
      />

      <SettingsTextInput
        name="negativeCache-timeToLive"
        label="Negative Cache TTL"
        value={String(formData.negativeCache?.timeToLive ?? 1440)}
        onChange={handleTimeToLiveChange}
        type="number"
        disabled={!formData.negativeCache?.enabled}
        helpText="How long (in minutes) to cache that a file was not found in the remote repository"
      />
    </SettingsFormSection>
  );
}

export default NegativeCacheFacet;

