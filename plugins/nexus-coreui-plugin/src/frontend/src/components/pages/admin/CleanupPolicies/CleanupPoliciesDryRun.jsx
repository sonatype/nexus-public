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
  NxButton,
  NxButtonBar,
  NxFormRow,
  NxFormSelect,
  NxH2,
  NxLoadWrapper,
  NxP
} from '@sonatype/react-shared-components';
import {FormUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import cleanupPoliciesDryRunMachine from './CleanupPoliciesDryRunMachine';

const {BUTTON, REPOSITORY_DESCRIPTION, REPOSITORY_SELECT, TITLE} = UIStrings.CLEANUP_POLICIES.DRY_RUN;

export default function CleanupPoliciesDryRun({policyData}) {
  const [state, send] = useMachine(cleanupPoliciesDryRunMachine, {devTools: true});
  const {loadError, repository, repositories} = state.context;
  const isLoading = state.matches('loading');

  useEffect(() => {
    send({type: 'LOAD_REPOSITORIES', format: policyData.format});
  }, [policyData.format]);

  function repositoryChangeHandler(event) {
    send({type: 'SET_REPOSITORY', repository: event.target.value});
  }

  function retry() {
    send({type: 'RETRY', format: policyData.format});
  }

  function createCSVReport() {
    send({type: 'CREATE_CSV_REPORT', policyData});
  }

  return <div className="nxrm-cleanup-policies-dry-run">
    <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
      {() =>
        <>
          <NxH2>{TITLE}</NxH2>
          <NxP id="dryRunDesc">{REPOSITORY_DESCRIPTION}</NxP>
          <NxFormRow>
            <NxFormSelect aria-describedby="dryRunDesc"
                          disabled={!policyData.format}
                          {...FormUtils.fieldProps('repository', state)}
                          onChange={repositoryChangeHandler}
                          value={repository}>
              <option value="">{REPOSITORY_SELECT}</option>
              {repositories?.map(({id, name}) =>
                <option key={id} value={id}>{name}</option>
              )}
            </NxFormSelect>
            <NxButtonBar>
              <NxButton disabled={!repository} onClick={createCSVReport} type="button">{BUTTON}</NxButton>
            </NxButtonBar>
          </NxFormRow>
        </>
      }
    </NxLoadWrapper>
  </div>
}
