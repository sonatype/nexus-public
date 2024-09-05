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
import {useMachine} from "@xstate/react";

import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
} from '@sonatype/nexus-ui-plugin';

import {
  NxGrid,
  NxH2,
  NxLoadWrapper,
  NxTile,
  NxWarningAlert
} from '@sonatype/react-shared-components';

import UIStrings from "../../../constants/UIStrings";
import MaliciousRiskMachine from "./MaliciousRiskMachine";
import MaliciousComponents from "./MaliciousComponents";
import MaliciousHighRiskEcosystems from "./MaliciousHighRiskEcosystems";
import MaliciousEvents from "./MaliciousEvents";
import {faExclamationTriangle} from "@fortawesome/free-solid-svg-icons";
import "./MaliciousRisk.scss";
import MaliciousRiskOnDisk from "./riskondisk/MaliciousRiskOnDisk";

const {
  TITLE,
  OPEN_SOURCE_MALWARE_PROTECTION_STATUS,
  LOAD_ERROR,
  HDS_CONNECTION_WARNING} = UIStrings.MALICIOUS_RISK;


export default function MaliciousRisk() {
  const [state, send, service] = useMachine(MaliciousRiskMachine, {devtools: true});
  const isLoading = state.matches('loading');
  const loadError = state.matches('loadError') ? LOAD_ERROR : null;
  const {
    maliciousRisk: {
      countByEcosystem,
      totalMaliciousRiskCount,
      totalProxyRepositoryCount,
      quarantineEnabledRepositoryCount,
      hdsError
    }
  } = state.context;

  function retry() {
    send({type: 'RETRY'});
  }

  return (
      <Page className="nxrm-malicious-risk">
        <PageHeader>
          <PageTitle icon={faExclamationTriangle} text={TITLE}/>
        </PageHeader>
        <ContentBody>
          <MaliciousRiskOnDisk/>
          <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
            {hdsError && <NxWarningAlert role="alert">{HDS_CONNECTION_WARNING}</NxWarningAlert>}
            {!hdsError && <>
              <NxH2>{OPEN_SOURCE_MALWARE_PROTECTION_STATUS}</NxH2>
              <NxTile>
                <NxTile.Content>
                  <NxGrid.Row>
                    <MaliciousComponents/>
                    <MaliciousEvents totalMaliciousRiskCount={totalMaliciousRiskCount}
                                     totalProxyRepositoryCount={totalProxyRepositoryCount}
                                     quarantineEnabledRepositoryCount={quarantineEnabledRepositoryCount}/>
                  </NxGrid.Row>
                </NxTile.Content>
              </NxTile>
              <MaliciousHighRiskEcosystems countByEcosystem={countByEcosystem}
                                           enabledCount={quarantineEnabledRepositoryCount === 0}/>
            </>}
          </NxLoadWrapper>
        </ContentBody>
      </Page>
  )
}
