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
import React, {useState, useEffect, useMemo} from 'react';
import {indexBy, prop} from 'ramda';

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import {useRepositoriesService} from '../RepositoriesContextProvider';

import {NxFormGroup, NxStatefulTransferList, NxFormSelect} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {
  EDITOR,
  EDITOR: {NUGET}
} = UIStrings.REPOSITORIES;

const NUGET_VERSIONS = {
  V2: 'NuGet V2',
  V3: 'NuGet V3'
};

export default function NugetGroupConfiguration({parentMachine}) {
  const [repositoriesState, repositoriesSend] = useRepositoriesService();

  const allRepositories = repositoriesState.context.data;

  const [groupVersion, setGroupVersion] = useState('');

  useEffect(() => {
    !allRepositories?.length && repositoriesSend({type: 'LOAD'});
  }, []);

  const [parentState, sendParent] = parentMachine;

  const {
    data: {
      group: {memberNames = []}
    },
    pristineData: {name}
  } = parentState.context;

  const nugetRepositories = useMemo(
    () => calculateNugetRepositoriesData(allRepositories, name, setGroupVersion),
    [allRepositories]
  );

  const availableGroupMembers = useMemo(
    () => getAvailableGroupMembers(nugetRepositories, groupVersion, name),
    [nugetRepositories, groupVersion]
  );

  const updateNugetVersion = (event) => {
    const value = event.currentTarget.value;
    setGroupVersion(value);
    sendParent({type: 'UPDATE', name: 'group.memberNames', value: []});
  };

  return (
    <>
      <h2 className="nx-h2">{EDITOR.GROUP_CAPTION}</h2>
      <>
        <NxFormGroup
          label={NUGET.GROUP_VERSION.LABEL}
          sublabel={NUGET.GROUP_VERSION.SUBLABEL}
          className="nxrm-form-group-nuget-protocol-version"
          isRequired
        >
          <NxFormSelect
            id="nuget-version"
            name="nuget-version"
            value={groupVersion}
            onChange={updateNugetVersion}
          >
            {Object.entries(NUGET_VERSIONS)?.map(([k, v]) => (
              <option key={v} value={k}>
                {v}
              </option>
            ))}
          </NxFormSelect>
        </NxFormGroup>

        <NxFormGroup label={EDITOR.MEMBERS_LABEL} isRequired>
          <NxStatefulTransferList
            allItems={availableGroupMembers}
            selectedItems={availableGroupMembers.length ? memberNames : []}
            onChange={FormUtils.handleUpdate('group.memberNames', sendParent)}
            allowReordering
          />
        </NxFormGroup>
      </>
    </>
  );
}

const getVersionFromMembers = (memberNames, repositoriesByName) => {
  if (!repositoriesByName) return;
  for (let memberName of memberNames) {
    const member = repositoriesByName[memberName];
    if (member.nugetVersion) {
      return member.nugetVersion;
    } else if (member.memberNames) {
      member.nugetVersion = getVersionFromMembers(member.memberNames, repositoriesByName);
      return member.nugetVersion;
    }
  }
};

const calculateNugetRepositoriesData = (allRepositories, repoName, setGroupVersion) => {
  const nugetRepositories = allRepositories.filter((repo) => repo.format === 'nuget');

  const nugetRepositoriesByName = indexBy(prop('name'), nugetRepositories);

  nugetRepositories.forEach((repo) => {
    if (repo.memberNames) {
      repo.nugetVersion = getVersionFromMembers(repo.memberNames, nugetRepositoriesByName);
    }
  });

  const initialGroupVersion = nugetRepositoriesByName[repoName]?.nugetVersion || 'V3';

  setGroupVersion(initialGroupVersion);

  return nugetRepositories;
};

const getAvailableGroupMembers = (nugetRepositories, groupVersion, repoName) => {
  return (
    nugetRepositories
      ?.filter(
        (repo) =>
          (repo.nugetVersion === groupVersion || !repo.nugetVersion) && repo.name !== repoName
      )
      ?.map((repo) => ({id: repo.name, displayName: repo.name})) || []
  );
};
