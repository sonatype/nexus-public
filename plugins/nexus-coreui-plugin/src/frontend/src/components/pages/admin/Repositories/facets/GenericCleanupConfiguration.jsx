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
import {assign} from 'xstate';

import {ExtAPIUtils, FormUtils} from '@sonatype/nexus-ui-plugin';

import {NxFormGroup, NxLoadWrapper, NxStatefulTransferList} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function GenericCleanupConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;
  const [cleanupState, cleanupSend] = ExtAPIUtils.useExtMachine('cleanup_CleanupPolicy', 'readByFormat', {
    actions: {
      setData: assign({
        data: (_, {data}) => data?.map(policy =>
            ({id: policy.name, displayName: policy.name}))
      })
    }
  });

  const {format, cleanup} = currentParent.context.data;
  const {data: availablePolicies = [], error} = cleanupState.context;
  const isLoading = cleanupState.matches('loading');

  const loadCleanupPolicies = () => cleanupSend({
    type: 'LOAD',
    options: {
      filterField: 'format',
      filterValue: format,
    },
  });

  useEffect(() => {
    loadCleanupPolicies();
  }, [format]);

  return (
    <>
      <h2 className="nx-h2">{EDITOR.CLEANUP_CAPTION}</h2>
      <NxLoadWrapper loading={isLoading} error={error} retryHandler={loadCleanupPolicies}>
        <NxFormGroup
          label={EDITOR.CLEANUP_POLICIES_LABEL}
          sublabel={EDITOR.CLEANUP_POLICIES_SUBLABEL}
        >
          <NxStatefulTransferList
            allItems={availablePolicies}
            selectedItems={new Set(cleanup?.policyNames)}
            onChange={FormUtils.handleUpdate('cleanup.policyNames', sendParent)}
          />
        </NxFormGroup>
      </NxLoadWrapper>
    </>
  );
}
