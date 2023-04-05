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
import {faGlobe} from '@fortawesome/free-solid-svg-icons';
import {
  ContentBody,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  ExtJS
} from '@sonatype/nexus-ui-plugin';

import {NxButton, NxLoadWrapper} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import SystemInformationBody from './SystemInformationBody';
import NodeSelector from './NodeSelector';
import SystemInformationHaMachine from './SystemInformationHaMachine';
import SystemInformationMachine from './SystemInformationMachine';

import './SystemInformation.scss';

const {MENU, ACTIONS, LOAD_ERROR} = UIStrings.SYSTEM_INFORMATION;

export default function SystemInformation() {
  const isCluster = ExtJS.state().getValue('nexus.datastore.clustered.enabled');

  const machine = isCluster ? SystemInformationHaMachine : SystemInformationMachine;

  const [state, send, service] = useMachine(machine, {devTools: true});

  const isLoading = state.matches('loading');
  const loadError = state.matches('loadError') ? LOAD_ERROR : null;

  const {selectedNodeId, systemInformation} = state.context;

  function retry() {
    send({type: 'RETRY'});
  }

  function downloadSystemInformation() {
    const fileName = `sysinfo${selectedNodeId ? '_' + selectedNodeId : ''}.json`;
    saveAsFile(systemInformation, fileName);
  }

  return (
    <Page>
      <PageHeader>
        <PageTitle icon={faGlobe} {...MENU} />
        <PageActions>
          <NxButton
            variant="primary"
            onClick={downloadSystemInformation}
            disabled={isLoading || loadError}
          >
            <span>{ACTIONS.download}</span>
          </NxButton>
        </PageActions>
      </PageHeader>

      {isCluster && <NodeSelector service={service} />}

      <ContentBody className="nxrm-system-information">
        <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
          <SystemInformationBody systemInformation={systemInformation} />
        </NxLoadWrapper>
      </ContentBody>
    </Page>
  );
}

const saveAsFile = (obj, filename) => {
  const content = JSON.stringify(obj, null, 2);
  const link = document.createElement('a');
  const blob = new Blob([content], {type: 'application/json;charset=UTF-8'});
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
};
