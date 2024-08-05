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
import MaliciousRiskMachine from "./MaliciousRiskMachine";
import UIStrings from "../../../constants/UIStrings";
import {ContentBody, Page, PageHeader, PageTitle} from "@sonatype/nexus-ui-plugin/src/frontend/src";
import {faGlobe} from "@fortawesome/free-solid-svg-icons";
import {NxLoadWrapper} from "@sonatype/react-shared-components";

const {MENU, TITLE, LOAD_ERROR} = UIStrings.MALICIOUS_RISK;

export default function MaliciousRisk() {

  const [state, send, service] = useMachine(MaliciousRiskMachine);

  const isLoading = state.matches('loading');
  const loadError = state.matches('loadError') ? LOAD_ERROR : null;

  const {maliciousRisk} = state.context;

  function retry() {
    send({type: 'RETRY'});
  }

  return (
      <Page>
        <PageHeader>
          <PageTitle icon={faGlobe} text={TITLE} description={MENU.description}/>
        </PageHeader>

        <ContentBody>
          <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
            <p>Data: {maliciousRisk.totalMaliciousRiskCount}</p>
          </NxLoadWrapper>
        </ContentBody>
      </Page>
  )
}
