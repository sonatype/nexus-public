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
import React, {useEffect} from 'react';

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import {useRepositoriesService} from '../RepositoriesContextProvider';

import {NxFormGroup, NxStatefulTransferList} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function GenericGroupConfiguration({parentMachine}) {
  const [repositoriesState, repositoriesSend] = useRepositoriesService();

  const repositories = repositoriesState.context.data;

  useEffect(() => {
    !repositories?.length && repositoriesSend({type: 'LOAD'});
  }, []);

  const [parentState, sendParent] = parentMachine;

  const {
    data: {
      format,
      group: {memberNames = []}
    },
    pristineData: {name}
  } = parentState.context;

  const availableRepositories =
    repositories
      ?.filter((repo) => repo.name !== name && repo.format === format)
      ?.map((repo) => ({id: repo.name, displayName: repo.name})) || [];

  return (
    <>
      <h2 className="nx-h2">{EDITOR.GROUP_CAPTION}</h2>
      <NxFormGroup label={EDITOR.MEMBERS_LABEL} isRequired>
        <NxStatefulTransferList
          allItems={availableRepositories}
          selectedItems={availableRepositories.length ? memberNames : []}
          onChange={FormUtils.handleUpdate('group.memberNames', sendParent)}
          allowReordering
        />
      </NxFormGroup>
    </>
  );
}
