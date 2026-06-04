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

import { SettingsCombobox, SettingsFormSection } from '../../shared/form';
import type { UploadableRepository } from './upload.types';

interface RepositorySelectorProps {
  repositoryName: string;
  onRepositoryChange: (repoName: string) => void;
  availableRepositories: UploadableRepository[];
  disabled?: boolean;
}

/**
 * Component for selecting a target repository for upload.
 *
 * @param {RepositorySelectorProps} props - The props for the component.
 * @returns {JSX.Element} The rendered component.
 */
export function RepositorySelector({
  repositoryName,
  onRepositoryChange,
  availableRepositories,
  disabled = false,
}: RepositorySelectorProps): JSX.Element {
  const repositoryOptions = useMemo(
    () =>
      availableRepositories.map((repo) => ({
        value: repo.name,
        label: repo.name,
        description: repo.format,
      })),
    [availableRepositories],
  );

  return (
    <SettingsFormSection title="Repository">
      <SettingsCombobox
        name="repository"
        label="Target Repository"
        value={repositoryName}
        onChange={(val) => onRepositoryChange(val)}
        options={repositoryOptions}
        helpText="The hosted repository where the component will be uploaded"
        required
        allowCustom={false}
        disabled={disabled}
        aria-label="Select target repository"
      />
    </SettingsFormSection>
  );
}

export default RepositorySelector;
