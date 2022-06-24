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
import {useMachine} from '@xstate/react';

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import {buildMachine} from './NugetGroupMachine';

import {
  NxFormGroup,
  NxStatefulTransferList,
  NxLoadWrapper,
  NxFormSelect
} from '@sonatype/react-shared-components';

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
  const [stateParent, sendParent] = parentMachine;

  const {
    data: {
      name,
      group: {memberNames = []}
    }
  } = stateParent.context;

  const [state, send] = useMachine(buildMachine(name), {devTools: true});

  const retry = () => send('RETRY');
  const isLoading = state.matches('loading');

  const {repositories, error, groupVersion} = state.context;

  const availableRepositories =
    repositories
      ?.filter(
        (repo) => (repo.nugetVersion === groupVersion || !repo.nugetVersion) && repo.name !== name
      )
      ?.map((repo) => ({id: repo.id, displayName: repo.name})) || [];

  const updateNugetVersion = (event) => {
    const value = event.currentTarget.value;
    send({type: 'SET_GROUP_VERSION', groupVersion: value});
    sendParent({type: 'UPDATE', name: 'group.memberNames', value: []});
  };

  return (
    <>
      <h2 className="nx-h2">{EDITOR.GROUP_CAPTION}</h2>
      <NxLoadWrapper loading={isLoading} error={error} retryHandler={retry}>
        {availableRepositories.length > 0 && (
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
                allItems={availableRepositories}
                selectedItems={memberNames}
                onChange={FormUtils.handleUpdate('group.memberNames', sendParent)}
                allowReordering
              />
            </NxFormGroup>
          </>
        )}
      </NxLoadWrapper>
    </>
  );
}
