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
import React, {useEffect, useMemo, useState} from 'react';

import Axios from 'axios';

import {FormUtils, ExtJS} from '@sonatype/nexus-ui-plugin';

import {useRepositoriesService} from '../RepositoriesContextProvider';

import {NxFormGroup, NxStatefulTransferList, NxReadOnly, NxWarningAlert} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {
  EDITOR,
  EDITOR: {NUGET}
} = UIStrings.REPOSITORIES;

export default function NugetGroupConfiguration({parentMachine}) {
  const [repositoriesState, repositoriesSend] = useRepositoriesService();

  const allRepositories = repositoriesState.context.data;

  useEffect(() => {
    !allRepositories?.length && repositoriesSend({type: 'LOAD'});
  }, []);

  const [parentState, sendParent] = parentMachine;

  const {
    data: {
      group: {memberNames = []},
      url
    },
    pristineData: {name}
  } = parentState.context;
  const isEdit = !!name;

  const chocolateyEnabled = useMemo(() => {
    try {
      return ExtJS.state().getValue('nugetChocolateyEnabled') === true;
    } catch {
      return false;
    }
  }, []);

  const [nugetProxyVersions, setNugetProxyVersions] = useState({});

  useEffect(() => {
    Axios.get('/service/rest/beta/repositories').then(({data}) => {
      const versions = {};
      data.forEach((repo) => {
        if (repo.format === 'nuget' && repo.type === 'proxy' && repo.nugetProxy?.nugetVersion) {
          versions[repo.name] = repo.nugetProxy.nugetVersion;
        }
      });
      setNugetProxyVersions(versions);
    }).catch((error) => {
      console.error('Failed to load repository list for NuGet version detection', error);
    });
  }, []);

  const availableGroupMembers = useMemo(
    () =>
      allRepositories
        ?.filter((repo) => repo.format === 'nuget' && repo.name !== name)
        ?.map((repo) => ({id: repo.name, displayName: repo.name})) || [],
    [allRepositories]
  );

  const mixedVersionConflict = useMemo(() => {
    let firstMemberName = null;
    let firstMemberVersion = null;
    for (const memberName of memberNames) {
      const version = nugetProxyVersions[memberName];
      if (!version) {
        continue;
      }
      if (!firstMemberName) {
        firstMemberName = memberName;
        firstMemberVersion = version;
      } else if (version !== firstMemberVersion) {
        return {
          conflictingName: memberName,
          conflictingVersion: version === 'V3' ? 'v3' : 'v2',
          firstMemberName,
          firstMemberVersion: firstMemberVersion === 'V3' ? 'v3' : 'v2'
        };
      }
    }
    return null;
  }, [memberNames, nugetProxyVersions]);

  return (
    <>
      {isEdit && url && (
        <NxReadOnly>
          <NxReadOnly.Label>{NUGET.SYMSRV_ENDPOINT.LABEL}</NxReadOnly.Label>
          <NxReadOnly.Data>{`${url}/symbols`}</NxReadOnly.Data>
        </NxReadOnly>
      )}
      <h2 className="nx-h2">{EDITOR.GROUP_CAPTION}</h2>
      <NxFormGroup label={EDITOR.MEMBERS_LABEL} isRequired>
        <NxStatefulTransferList
          allItems={availableGroupMembers}
          selectedItems={availableGroupMembers.length ? memberNames : []}
          onChange={FormUtils.handleUpdate('group.memberNames', sendParent)}
          allowReordering
        />
      </NxFormGroup>
      {mixedVersionConflict && !chocolateyEnabled && (
        <NxWarningAlert>
          {NUGET.MIXED_VERSION_WARNING(
            mixedVersionConflict.conflictingName,
            mixedVersionConflict.conflictingVersion,
            mixedVersionConflict.firstMemberName,
            mixedVersionConflict.firstMemberVersion
          )}
        </NxWarningAlert>
      )}
    </>
  );
}
