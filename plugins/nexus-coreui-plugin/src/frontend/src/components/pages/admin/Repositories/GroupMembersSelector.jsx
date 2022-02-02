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
import {useMachine} from '@xstate/react';

import {
  NxFormGroup,
  NxStatefulTransferList,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import GroupMembersSelectorMachine from './GroupMembersSelectorMachine';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function GroupMembersSelector({
  format,
  selectedMembers,
  onChange,
}) {
  const [current, send] = useMachine(GroupMembersSelectorMachine, {
    devTools: true,
  });

  useEffect(() => {
    send({type: 'LOAD_REPOSITORIES', format});
  }, [format]);

  const {repositories, error} = current.context;

  const isLoading = current.matches('loading');

  const retry = () => send('RETRY');

  return (
    <>
      <h2 className="nx-h2">{EDITOR.GROUP_CAPTION}</h2>
      <NxLoadWrapper loading={isLoading} error={error} retryHandler={retry}>
        <NxFormGroup label={EDITOR.MEMBERS_LABEL}>
          <NxStatefulTransferList
            allItems={repositories}
            selectedItems={selectedMembers}
            onChange={onChange}
            allowReordering
          />
        </NxFormGroup>
      </NxLoadWrapper>
    </>
  );
}
