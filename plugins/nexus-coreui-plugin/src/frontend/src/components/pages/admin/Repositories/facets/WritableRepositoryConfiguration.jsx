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
import React, {useMemo} from 'react';

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import {NxFormGroup, NxFormSelect} from '@sonatype/react-shared-components';

import {useRepositoriesService} from '../RepositoriesContextProvider';

import UIStrings from '../../../../../constants/UIStrings';

const {WRITABLE} = UIStrings.REPOSITORIES.EDITOR;

export default function WritableRepositoryConfiguration({parentMachine}) {
  const [parentState, sendParent] = parentMachine;

  const {
    group: {memberNames, writableMember}
  } = parentState.context.data;

  const [repositoriesState] = useRepositoriesService();

  const repositories = repositoriesState.context.data;

  const hostedDockerRepos = useMemo(() => getHostedDockerRepos(repositories), [repositories]);

  const availableWritableRepos = memberNames.filter((name) => hostedDockerRepos.includes(name));

  if (writableMember && !availableWritableRepos.includes(writableMember)) {
    availableWritableRepos.push(writableMember);
  }

  return (
    <NxFormGroup
      label={WRITABLE.LABEL}
      sublabel={WRITABLE.SUBLABEL}
    >
      <NxFormSelect
        {...FormUtils.selectProps('group.writableMember', parentState)}
        onChange={FormUtils.handleUpdate('group.writableMember', sendParent)}
        disabled={!availableWritableRepos.length}
      >
        <option value="">{WRITABLE.PLACEHOLDER}</option>
        {availableWritableRepos.map((repo) => (
          <option key={repo} value={repo}>
            {repo}
          </option>
        ))}
      </NxFormSelect>
    </NxFormGroup>
  );
}

const getHostedDockerRepos = (repositories) =>
  repositories
    .filter(({type, format}) => type === 'hosted' && format === 'docker')
    .map(({name}) => name);
