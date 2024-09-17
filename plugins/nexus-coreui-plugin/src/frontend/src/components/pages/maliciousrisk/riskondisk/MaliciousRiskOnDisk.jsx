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
  NxButton,
  NxButtonBar,
  NxErrorAlert,
  NxFontAwesomeIcon,
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
  TITLE,
  DESCRIPTION,
  CONTACT_SONATYPE,
  VIEW_OSS_MALWARE_RISK
} = UIStrings.MALICIOUS_RISK.RISK_ON_DISK;

function MaliciousRiskOnDiskContent({user}) {
  const [state, send] = useMachine(MaliciousRiskOnDiskMachine, {devtools: true});
  const {maliciousRiskOnDisk, loadError} = state.context;
  const isLoading = state.matches('loading');

  const isAdmin = user?.administrator;
  const isProEdition = ExtJS.isProEdition();

  const maliciousDashBoardHash = '#browse/maliciousrisk';
  const notMaliciousDashBoardPage = window.location.hash !== maliciousDashBoardHash;

  function retry() {
    send({type: 'RETRY'});
  }

  function navigateToMaliciousDashBoard() {
    window.location.href = maliciousDashBoardHash;
  }

  function redirectToFirewallContactSonatype() {
    window.open(isProEdition ? CONTACT_SONATYPE.URL.PRO : CONTACT_SONATYPE.URL.OSS, '_blank');
  }

  const WarningDescription = function() {
    const {NON_ADMIN_OSS, NON_ADMIN_PRO, ADMIN_OSS, ADMIN_PRO} = DESCRIPTION;

    if (isAdmin && isProEdition) {
      return ADMIN_PRO;
    }
    else if (!isAdmin && isProEdition) {
      return NON_ADMIN_PRO;
    }
    else if (isAdmin && !isProEdition) {
      return ADMIN_OSS;
    }
    else if (!isAdmin && !isProEdition) {
      return NON_ADMIN_OSS;
    }
    else {
      return '';
    }
  }

  return (
      <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
        <div className="risk-on-disk-container">
          <NxErrorAlert className="risk-on-disk-alert">
            <div className="risk-on-disk-content">
              <div>
                <NxFontAwesomeIcon icon={faExclamationTriangle}/>
                <NxH2>{maliciousRiskOnDisk?.totalCount}</NxH2>
                <NxH3>{TITLE}</NxH3>
              </div>
              <p><WarningDescription/></p>
            </div>
            <NxButtonBar>
              {notMaliciousDashBoardPage &&
                  <NxButton onClick={navigateToMaliciousDashBoard}>{VIEW_OSS_MALWARE_RISK}</NxButton>}
              {isAdmin &&
                  <NxButton
                      variant="error"
                      onClick={redirectToFirewallContactSonatype}>
                    {CONTACT_SONATYPE.TEXT}
                  </NxButton>}
            </NxButtonBar>
          </NxErrorAlert>
        </div>
      </NxLoadWrapper>
  );
}

export default function MaliciousRiskOnDisk() {
  const isRiskOnDiskEnabled = ExtJS.state().getValue('nexus.malicious.risk.on.disk.enabled');
  const maliciousRiskDashboardEnabled = ExtJS.state().getValue('MaliciousRiskDashboard');
  const user = ExtJS.useUser();
  const userIsLogged = user ?? false;
  const showMaliciousRiskOnDisk = userIsLogged && isRiskOnDiskEnabled && maliciousRiskDashboardEnabled;

  if (!showMaliciousRiskOnDisk) {
    return null;
  }
  return <MaliciousRiskOnDiskContent user={user}/>;
}
