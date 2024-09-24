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
  NxButtonBar,
  NxErrorAlert,
  NxFontAwesomeIcon, NxGrid,
  NxH2,
  NxH3,
  NxLoadWrapper
} from "@sonatype/react-shared-components";
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {faExclamationTriangle} from "@fortawesome/free-solid-svg-icons";

import MaliciousRiskOnDiskMachine from "./MaliciousRiskOnDiskMachine";
import UIStrings from "../../../../constants/UIStrings";
import "./MaliciousRiskOnDisk.scss";

const {
  TITLE_PLURAL,
  TITLE_SINGULAR,
  DESCRIPTION,
  CONTACT_SONATYPE,
} = UIStrings.MALICIOUS_RISK.RISK_ON_DISK;

function MaliciousRiskOnDiskContent({user, props}) {
  const [state, send] = useMachine(MaliciousRiskOnDiskMachine, {devtools: true});
  const {maliciousRiskOnDisk, loadError} = state.context;
  const isLoading = state.matches('loading');

  const isAdmin = user?.administrator;
  const isProEdition = ExtJS.isProEdition();

  const riskOnDiskCount = maliciousRiskOnDisk?.totalCount ?? 0;
  const showWarningAlert = riskOnDiskCount > 0;

  setTimeout(() => {
    if (window.location.hash.includes('#browse/browse') || window.location.hash.includes('#browse/search')) {
      props.rerender(riskOnDiskCount);
    }
  }, 100);

  function retry() {
    send({type: 'RETRY'});
  }

  function getFirewallContactSonatype() {
    return isProEdition ? CONTACT_SONATYPE.URL.PRO : CONTACT_SONATYPE.URL.OSS;
  }

  return (
      <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
        {showWarningAlert && <div className="risk-on-disk-container">
          <NxErrorAlert className="risk-on-disk-alert">
            <div className="risk-on-disk-content">
              <div className="risk-on-disk-alert-title">
                <NxFontAwesomeIcon icon={faExclamationTriangle}/>
                <NxH2>{riskOnDiskCount.toLocaleString()}</NxH2>
                <NxH3>{riskOnDiskCount > 1 ? TITLE_PLURAL : TITLE_SINGULAR}</NxH3>
              </div>
              <NxGrid.Column className='risk-on-disk-alert-description'>
                <NxGrid.ColumnSection>
                  <NxGrid.Header>
                    <NxH3>{DESCRIPTION.TITLE}</NxH3>
                  </NxGrid.Header>
                  <p>{DESCRIPTION.CONTENT}</p>
                </NxGrid.ColumnSection>
                {!isAdmin &&
                    <NxGrid.ColumnSection>
                      <p>{DESCRIPTION.ADDITIONAL_NON_ADMIN_CONTENT}</p>
                    </NxGrid.ColumnSection>
                }
              </NxGrid.Column>
            </div>
            <NxButtonBar>
              {isAdmin &&
                  <a className="nx-btn nx-btn--error"
                     href={getFirewallContactSonatype()}
                     target="_blank">
                    {CONTACT_SONATYPE.TEXT}
                  </a>}
            </NxButtonBar>
          </NxErrorAlert>
        </div>}
      </NxLoadWrapper>
  );
}

export default function MaliciousRiskOnDisk(props) {
  const isRiskOnDiskEnabled = ExtJS.state().getValue('nexus.malicious.risk.on.disk.enabled');
  const maliciousRiskDashboardEnabled = ExtJS.state().getValue('MaliciousRiskDashboard');
  const user = ExtJS.useUser();
  const userIsLogged = user ?? false;
  const showMaliciousRiskOnDisk = userIsLogged && isRiskOnDiskEnabled && maliciousRiskDashboardEnabled;

  if (!showMaliciousRiskOnDisk) {
    return null;
  }
  return <MaliciousRiskOnDiskContent user={user} props={props}/>;
}
