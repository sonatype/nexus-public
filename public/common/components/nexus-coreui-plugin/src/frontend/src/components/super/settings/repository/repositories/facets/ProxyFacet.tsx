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
} from '../../../../shared/form';

import {
  RepositoryFormData,
  RepositoryFormErrors,
} from '../types';

interface ProxyFacetProps {
  formData: RepositoryFormData;
  onChange: (updates: Partial<RepositoryFormData>) => void;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  format?: string;
}

// Remote URL examples by format
const REMOTE_URL_EXAMPLES: Record<string, string> = {
  maven2: 'e.g., https://repo1.maven.org/maven2/',
  npm: 'e.g., https://registry.npmjs.org/',
  nuget: 'e.g., https://api.nuget.org/v3/index.json',
  pypi: 'e.g., https://pypi.org/',
  docker: 'e.g., https://registry-1.docker.io',
  raw: 'e.g., https://example.com/files/',
  default: 'e.g., https://example.com/repository/',
};

/**
 * ProxyFacet - Remote URL and proxy settings
 */
export function ProxyFacet({
  formData,
  onChange,
  onNestedChange,
  errors,
  format,
}: ProxyFacetProps) {
  const handleRemoteUrlChange = (value: string) => {
    onNestedChange('proxy', { remoteUrl: value });
  };

  const handleContentMaxAgeChange = (value: string) => {
    const numValue = parseInt(value, 10);
    onNestedChange('proxy', { contentMaxAge: isNaN(numValue) ? -1 : numValue });
  };

  const handleMetadataMaxAgeChange = (value: string) => {
    const numValue = parseInt(value, 10);
    onNestedChange('proxy', { metadataMaxAge: isNaN(numValue) ? 1440 : numValue });
  };

  const urlExample = format ? REMOTE_URL_EXAMPLES[format] || REMOTE_URL_EXAMPLES.default : REMOTE_URL_EXAMPLES.default;

  return (
    <SettingsFormSection title="Proxy">
      <SettingsTextInput
        name="proxy-remoteUrl"
        label="Remote Storage"
        value={formData.proxy?.remoteUrl || ''}
        onChange={handleRemoteUrlChange}
        error={errors?.proxy?.remoteUrl}
        required
        placeholder="https://"
        helpText={`Location of the remote repository being proxied. ${urlExample}`}
      />

      <SettingsTextInput
        name="proxy-contentMaxAge"
        label="Maximum Component Age"
        value={String(formData.proxy?.contentMaxAge ?? -1)}
        onChange={handleContentMaxAgeChange}
        type="number"
        helpText="How long (in minutes) to cache artifacts before rechecking the remote repository. Set to -1 to disable caching."
      />

      <SettingsTextInput
        name="proxy-metadataMaxAge"
        label="Maximum Metadata Age"
        value={String(formData.proxy?.metadataMaxAge ?? 1440)}
        onChange={handleMetadataMaxAgeChange}
        type="number"
        helpText="How long (in minutes) to cache metadata before rechecking the remote repository."
      />
    </SettingsFormSection>
  );
}

export default ProxyFacet;

