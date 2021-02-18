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
  NxButton,
  NxLoadWrapper,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  Utils
} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import SystemInformationBody from './SystemInformationBody';
import SystemInformationMachine from './SystemInformationMachine';

import './SystemInformation.scss';

/**
 * @since 3.24
 */
export default function SystemInformation() {
  const [current, send] = useMachine(SystemInformationMachine);
  const isLoading = current.matches('loading');
  const loadError = current.matches('error') ? UIStrings.SYSTEM_INFORMATION.LOAD_ERROR : null;
  const systemInformation = current.context.systemInformation;

  function retry() {
    send({type: 'RETRY'});
  }

  function downloadSystemInformation() {
    window.open(Utils.urlFromPath('/service/rest/atlas/system-information'), '_blank');
  }

  return <Page>
    <PageHeader>
      <PageTitle icon={faGlobe} {...UIStrings.SYSTEM_INFORMATION.MENU}/>
      <PageActions>
        <NxButton variant="primary" onClick={downloadSystemInformation} disabled={isLoading}>
          <span>{UIStrings.SYSTEM_INFORMATION.ACTIONS.download}</span>
        </NxButton>
      </PageActions>
    </PageHeader>
    <ContentBody className="nxrm-system-information">
      <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
        <SystemInformationBody systemInformation={systemInformation} />
      </NxLoadWrapper>
    </ContentBody>
  </Page>;
}
