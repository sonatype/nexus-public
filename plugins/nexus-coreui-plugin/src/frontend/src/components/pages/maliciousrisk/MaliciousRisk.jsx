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
  NxLoadWrapper,
  NxTile
} from '@sonatype/react-shared-components';

import UIStrings from "../../../constants/UIStrings";
import MaliciousRiskMachine from "./MaliciousRiskMachine";
import MaliciousComponents from "./MaliciousComponents";
import "./MaliciousRisk.scss";
import MaliciousEvents from "./MaliciousEvents";
import {faExclamationTriangle} from "@fortawesome/free-solid-svg-icons";

const {TITLE, LOAD_ERROR} = UIStrings.MALICIOUS_RISK;

export default function MaliciousRisk() {
  const [state, send, service] = useMachine(MaliciousRiskMachine, {devtools: true});
  const isLoading = state.matches('loading');
  const loadError = state.matches('loadError') ? LOAD_ERROR : null;
  const {maliciousRisk: {totalMaliciousRiskCount, totalProxyRepositoryCount}} = state.context;

  function retry() {
    send({type: 'RETRY'});
  }

  return (
      <Page className="nxrm-malicious-risk">
        <PageHeader>
          <PageTitle icon={faExclamationTriangle} text={TITLE}/>
        </PageHeader>
        <ContentBody>
          <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
            <NxTile>
              <NxTile.Content>
                <NxGrid.Row>
                  <MaliciousComponents/>
                  <MaliciousEvents totalMaliciousRiskCount={totalMaliciousRiskCount}
                                   totalProxyRepositoryCount={totalProxyRepositoryCount}/>
                </NxGrid.Row>
              </NxTile.Content>
            </NxTile>
          </NxLoadWrapper>
        </ContentBody>
      </Page>
  )
}
