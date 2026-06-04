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
import { SettingsAlert, SettingsFormSection, SettingsSelect } from '../../../../../shared/form';
import { RepositoryFormData } from '../types';

interface RawFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(key: K, updates: Partial<RepositoryFormData[K]>) => void;
}

const CONTENT_DISPOSITION_OPTIONS = [
  { value: 'INLINE', label: 'Inline' },
  { value: 'ATTACHMENT', label: 'Attachment' },
];

/**
 * RawFacet - Raw format content disposition setting
 */
export function RawFacet({ formData, onNestedChange }: RawFacetProps) {
  return (
    <SettingsFormSection title="Raw Settings" description="Raw repository configuration">
      <SettingsSelect
        name="raw-contentDisposition"
        label="Content Disposition"
        value={formData.raw?.contentDisposition || 'ATTACHMENT'}
        onChange={(value) => onNestedChange('raw', { contentDisposition: value as 'INLINE' | 'ATTACHMENT' })}
        options={CONTENT_DISPOSITION_OPTIONS}
        helpText="Controls whether content is displayed inline in the browser or downloaded as an attachment"
      />

      {(formData.raw?.contentDisposition || 'ATTACHMENT') === 'INLINE' && (
        <SettingsAlert type="warning">
          Serving content inline allows uploaded HTML to render on a trusted Nexus URL, which
          can be exploited for phishing attacks against other users.
        </SettingsAlert>
      )}
    </SettingsFormSection>
  );
}

export default RawFacet;
