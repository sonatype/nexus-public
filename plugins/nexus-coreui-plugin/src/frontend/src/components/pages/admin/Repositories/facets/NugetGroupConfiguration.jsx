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
import {assign} from 'xstate';
import {FormUtils, useSimpleMachine} from '@sonatype/nexus-ui-plugin';

import {
  NxFormGroup,
  NxStatefulTransferList,
  NxLoadWrapper,
  NxRadio,
  NxFieldset
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {
  EDITOR,
  EDITOR: {NUGET}
} = UIStrings.REPOSITORIES;

export const repositoriesUrl = '/service/rest/internal/ui/repositories?format=nuget';

export default function NugetGroupConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  const {
    data: {
      name,
      group: {memberNames}
    }
  } = currentParent.context;

  const {current, send, retry, isLoading} = useSimpleMachine({
    id: 'NugetGroupConfigurationMachine',
    url: repositoriesUrl,
    loadOnMount: true,
    idleEvents: {
      SET_GROUP_VERSION: {
        target: 'idle',
        actions: ['setGroupVersion']
      }
    },
    options: {
      actions: {
        setData: assign((_, event) => {
          const repositories = event.data?.data;

          const nameToVersionMap = repositories.reduce((acc, repo) => {
            acc[repo.name] = repo.nugetVersion;
            return acc;
          }, {});

          const findFirstMemberWithVersion = (members) =>
            members.find((memberName) => !!nameToVersionMap[memberName]);

          // determine group version from the members if exist or set to 'V3' as default  
          const groupVersion = nameToVersionMap[findFirstMemberWithVersion(memberNames)] || 'V3';

          // add nugetVersion to each group repo based on members versions
          repositories.forEach((repo) => {
            if (repo.memberNames) {
              repo.nugetVersion = nameToVersionMap[findFirstMemberWithVersion(repo.memberNames)];
            }
          });

          return {
            repositories,
            groupVersion
          };
        }),

        setGroupVersion: assign({
          groupVersion: (_, {groupVersion}) => groupVersion
        })
      }
    }
  });

  const {repositories, error, groupVersion} = current.context;

  const availableRepositories =
    repositories
      ?.filter(
        (repo) => (repo.nugetVersion === groupVersion || !repo.nugetVersion) && repo.name !== name
      )
      ?.map((repo) => ({id: repo.id, displayName: repo.name})) || [];

  const updateNugetVersion = (value) => {
    send({type: 'SET_GROUP_VERSION', groupVersion: value});
    sendParent({type: 'UPDATE', name: 'group.memberNames', value: []});
  };

  return (
    <>
      <h2 className="nx-h2">{EDITOR.GROUP_CAPTION}</h2>
      <NxLoadWrapper loading={isLoading} error={error} retryHandler={retry}>
        {availableRepositories.length > 0 && (
          <>
            <NxFieldset
              label={NUGET.GROUP_VERSION.LABEL}
              sublabel={NUGET.GROUP_VERSION.SUBLABEL}
              className="nxrm-form-group-nuget-protocol-version"
              isRequired
            >
              <NxRadio
                name="nuget-protocol-version"
                value="V2"
                onChange={updateNugetVersion}
                isChecked={groupVersion === 'V2'}
              >
                {NUGET.PROTOCOL_VERSION.V2_RADIO_DESCR}
              </NxRadio>
              <NxRadio
                name="nuget-protocol-version"
                value="V3"
                onChange={updateNugetVersion}
                isChecked={groupVersion === 'V3'}
              >
                {NUGET.PROTOCOL_VERSION.V3_RADIO_DESCR}
              </NxRadio>
            </NxFieldset>
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
