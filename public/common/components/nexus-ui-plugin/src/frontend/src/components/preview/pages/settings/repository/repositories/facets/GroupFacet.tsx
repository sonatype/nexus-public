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

import React, { useEffect, useMemo, useState } from 'react';
import { Box, Text, Flex } from '@radix-ui/themes';
import { ChevronUp, ChevronDown, Trash2 } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import axios from 'axios';

import {
  SettingsAlert,
  SettingsFormSection,
  SettingsSelect,
} from '../../../../../shared/form';

import {
  RepositoryFormData,
  RepositoryFormErrors,
  RepositoryReference,
} from '../types';

import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

import './GroupFacet.scss';

interface GroupFacetProps {
  formData: RepositoryFormData;
  onChange: (updates: Partial<RepositoryFormData>) => void;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  memberOptions: RepositoryReference[];
  format?: string;
}

const FORMATS_WITH_GROUP_WRITE = ['npm', 'docker', 'conan'];

interface RepositoryListItem {
  format: string;
  type: string;
  name: string;
  nugetProxy?: { nugetVersion?: string };
}

/**
 * GroupFacet - Group member repository selection
 */
export function GroupFacet({
  formData,
  onChange,
  onNestedChange,
  errors,
  memberOptions,
  format,
}: GroupFacetProps) {
  const currentMembers = formData.group?.memberNames || [];

  const groupWritableEnabled = useMemo(() => {
    try {
      return ExtJS.state().getValue('groupWritableEnabled');
    } catch {
      return false;
    }
  }, []);

  const chocolateyEnabled = useMemo(() => {
    try {
      return ExtJS.state().getValue('nugetChocolateyEnabled') === true;
    } catch {
      return false;
    }
  }, []);

  const showWritableMember = groupWritableEnabled && format && FORMATS_WITH_GROUP_WRITE.includes(format);

  const [nugetProxyVersions, setNugetProxyVersions] = useState<Record<string, string>>({});
  const [nugetVersionFetchError, setNugetVersionFetchError] = useState<boolean>(false);

  useEffect(() => {
    if (format !== 'nuget') return;
    setNugetVersionFetchError(false);
    // /beta/repositories is used intentionally: it is the only endpoint that exposes
    // nugetProxy.nugetVersion. The stable /v1/repositories returns a summary schema
    // without format-specific attributes.
    axios
      .get('/service/rest/beta/repositories')
      .then(({ data }: { data: RepositoryListItem[] }) => {
        const versions: Record<string, string> = {};
        data.forEach((repo) => {
          if (repo.format === 'nuget' && repo.type === 'proxy' && repo.nugetProxy?.nugetVersion) {
            versions[repo.name] = repo.nugetProxy.nugetVersion;
          }
        });
        setNugetProxyVersions(versions);
      })
      .catch((error: unknown) => {
        console.error('Failed to load repository list for NuGet version detection', error);
        setNugetVersionFetchError(true);
      });
  }, [format]);

  const mixedVersionConflict = useMemo(() => {
    if (format !== 'nuget') return null;
    let firstMemberName: string | null = null;
    let firstMemberVersion: string | null = null;
    for (const memberName of currentMembers) {
      const version = nugetProxyVersions[memberName];
      if (!version) continue;
      if (!firstMemberName) {
        firstMemberName = memberName;
        firstMemberVersion = version;
      } else if (version !== firstMemberVersion) {
        return {
          conflictingName: memberName,
          conflictingVersion: version === 'V3' ? 'v3' : 'v2',
          firstMemberName,
          firstMemberVersion: firstMemberVersion === 'V3' ? 'v3' : 'v2',
        };
      }
    }
    return null;
  }, [format, currentMembers, nugetProxyVersions]);

  // Filter out already-selected members and sort alphabetically
  const availableOptions = useMemo(() => {
    return memberOptions
      .filter((opt) => !currentMembers.includes(opt.name))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [memberOptions, currentMembers]);

  const handleAddMember = (memberName: string) => {
    if (!memberName) return;
    
    onNestedChange('group', {
      memberNames: [...currentMembers, memberName],
    });
  };

  const handleRemoveMember = (memberName: string) => {
    onNestedChange('group', {
      memberNames: currentMembers.filter((m) => m !== memberName),
    });
  };

  const handleMoveMember = (index: number, direction: 'up' | 'down') => {
    const newMembers = [...currentMembers];
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    
    if (targetIndex < 0 || targetIndex >= newMembers.length) return;
    
    [newMembers[index], newMembers[targetIndex]] = [newMembers[targetIndex], newMembers[index]];
    
    onNestedChange('group', { memberNames: newMembers });
  };

  return (
    <SettingsFormSection title={UIStrings.GROUP.SECTION.title}>
      {/* Writable Member - shown FIRST for formats that support it */}
      {showWritableMember && (
        <SettingsSelect
          name="group-writableMember"
          label={UIStrings.GROUP.WRITABLE_MEMBER.label}
          value={formData.group?.writableMember || ''}
          onChange={(value) => onNestedChange('group', { writableMember: value || null })}
          helpText={UIStrings.GROUP.WRITABLE_MEMBER.helpText}
          options={[
            { value: '', label: UIStrings.GROUP.WRITABLE_MEMBER.noneOption },
            ...memberOptions
              .filter((opt) => currentMembers.includes(opt.name) && opt.type === 'hosted')
              .map((opt) => ({ value: opt.name, label: opt.name })),
          ]}
        />
      )}

      <Box className="group-facet">
        <Text size="2" className="group-facet__label">
          {UIStrings.GROUP.MEMBER_REPOSITORIES.label} <span className="group-facet__required">*</span>
        </Text>
        <Text size="1" className="group-facet__help">
          {UIStrings.GROUP.MEMBER_REPOSITORIES.helpText}
        </Text>

        {errors?.group?.memberNames && (
          <Text size="1" className="group-facet__error">
            {errors.group.memberNames}
          </Text>
        )}

        {/* Add member dropdown */}
        <SettingsSelect
          name="group-addMember"
          label=""
          value=""
          onChange={handleAddMember}
          options={[
            { value: '', label: UIStrings.GROUP.MEMBER_REPOSITORIES.addPlaceholder },
            ...availableOptions.map((opt) => ({
              value: opt.name,
              label: `${opt.name} (${opt.type || 'unknown'})`,
            })),
          ]}
        />

        {/* Selected members list */}
        {currentMembers.length > 0 && (
          <Box className="group-facet__members">
            {currentMembers.map((member, index) => (
              <Flex
                key={member}
                align="center"
                justify="between"
                className="group-facet__member"
              >
                <Flex align="center" gap="2">
                  <Text size="1" className="group-facet__member-index">
                    {index + 1}
                  </Text>
                  <Text size="2">{member}</Text>
                </Flex>
                <Flex gap="1" className="group-facet__member-actions">
                  <button
                    type="button"
                    onClick={() => handleMoveMember(index, 'up')}
                    disabled={index === 0}
                    className="group-facet__move-btn"
                    title={UIStrings.GROUP.BUTTONS.moveUp}
                    aria-label={UIStrings.GROUP.BUTTONS.moveUp}
                  >
                    <ChevronUp size={14} />
                  </button>
                  <button
                    type="button"
                    onClick={() => handleMoveMember(index, 'down')}
                    disabled={index === currentMembers.length - 1}
                    className="group-facet__move-btn"
                    title={UIStrings.GROUP.BUTTONS.moveDown}
                    aria-label={UIStrings.GROUP.BUTTONS.moveDown}
                  >
                    <ChevronDown size={14} />
                  </button>
                  <button
                    type="button"
                    onClick={() => handleRemoveMember(member)}
                    className="group-facet__remove-btn"
                    title={UIStrings.GROUP.BUTTONS.remove}
                    aria-label={UIStrings.GROUP.BUTTONS.remove}
                  >
                    <Trash2 size={14} />
                  </button>
                </Flex>
              </Flex>
            ))}
          </Box>
        )}

        {currentMembers.length === 0 && (
          <Box className="group-facet__empty">
            <Text size="2">{UIStrings.GROUP.MEMBER_REPOSITORIES.emptyMessage}</Text>
          </Box>
        )}
      </Box>

      {mixedVersionConflict && !chocolateyEnabled && (
        <SettingsAlert type="warning">
          {UIStrings.NUGET.MIXED_VERSION_WARNING(
            mixedVersionConflict.conflictingName,
            mixedVersionConflict.conflictingVersion,
            mixedVersionConflict.firstMemberName,
            mixedVersionConflict.firstMemberVersion,
          )}
        </SettingsAlert>
      )}

      {format === 'nuget' && nugetVersionFetchError && (
        <SettingsAlert type="error">
          Unable to load NuGet proxy versions. Mixed-version group detection is unavailable.
        </SettingsAlert>
      )}
    </SettingsFormSection>
  );
}

export default GroupFacet;

