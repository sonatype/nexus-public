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
import {useMachine} from "@xstate/react";

import {
  NxButtonBar,
  NxErrorAlert,
  NxFontAwesomeIcon,
  NxGrid,
  NxH2,
  NxH3,
  useToggle
} from "@sonatype/react-shared-components";
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {faExclamationCircle, faExclamationTriangle, faCaretRight} from "@fortawesome/free-solid-svg-icons";

import MaliciousRiskOnDiskMachine from "./MaliciousRiskOnDiskMachine";
import UIStrings from '../../../constants/UIStrings';
import "./MaliciousRiskOnDisk.scss";
import FeatureFlags from '../../../constants/FeatureFlags';
import {helperFunctions} from '../CELimits/UsageHelper';
import {isPageHashIncluding} from '../../../interfaces/LocationUtils';

const {
  CLM,
  MALWARE_RISK_ENABLED,
  MALWARE_RISK_ON_DISK_ENABLED, 
  MALWARE_RISK_ON_DISK_NONADMIN_OVERRIDE_ENABLED
} = FeatureFlags;

const {
  TITLE_PLURAL,
  TITLE_SINGULAR,
  DESCRIPTION,
  CONTACT_SONATYPE,
  VIEW_MALWARE_RISK,
} = UIStrings.MALICIOUS_RISK.RISK_ON_DISK;

const {
  useThrottlingStatus
} = helperFunctions;

const EXTJS_EXPANDED_HEIGHT = 292;
const EXTJS_COLLAPSED_HEIGHT = 115;

function MalwareButton({isProEdition, isMalwareRiskEnabled, isIqServerEnabled}) {
  if (isProEdition && isMalwareRiskEnabled && isIqServerEnabled) {
    return <a className="nx-btn nx-btn--error" href="#browse/malwarerisk">{VIEW_MALWARE_RISK}</a>;
  } else if (isProEdition) {
    return <a className="nx-btn nx-btn--error" href={CONTACT_SONATYPE.URL.PRO} target="_blank">{CONTACT_SONATYPE.TEXT}</a>;
  } else {
    return <a className="nx-btn nx-btn--error" href={CONTACT_SONATYPE.URL.OSS} target="_blank">{CONTACT_SONATYPE.TEXT}</a>;
  }
}

function MaliciousRiskOnDiskContent({rerender, toggle}) {
  const [state, send] = useMachine(MaliciousRiskOnDiskMachine, {
    devTools: true
  });
  const maliciousRiskOnDisk = ExtJS.state().getValue('nexus.malware.count');
  const closeMalwareBanner = state.matches('close');
  const user = ExtJS.useUser();

  const isAdmin = user?.administrator;
  const isProEdition = ExtJS.isProEdition();
  const isIqServerEnabled = ExtJS.state().getValue(CLM)?.enabled;
  const isMalwareRiskEnabled = ExtJS.state().getValue(MALWARE_RISK_ENABLED);
  const isMalwareRemediationPage = isPageHashIncluding(['#browse/malwarerisk']);
  const isOnBrowseOrSearch = isPageHashIncluding(['#browse/browse', '#browse/search']);
  const isWelcomePage = !window.location.hash || isPageHashIncluding(['#browse/welcome']);

  const riskOnDiskCount = maliciousRiskOnDisk?.totalCount ?? 0;
  const showWarningAlert = riskOnDiskCount > 0 && !closeMalwareBanner && !isMalwareRemediationPage;
  const throttlingStatus = useThrottlingStatus();

  function hasUsageBanner() {
    if (throttlingStatus === 'BELOW_LIMITS_GRACE_PERIOD_ENDED' && document.cookie.includes('under_end_grace=dismissed')) {
      return false;
    }
    else if (throttlingStatus === 'NO_THROTTLING') {
      return false;
    }
    return true;
  }

  function shouldStartExpanded() {
    if (isOnBrowseOrSearch) {
      return true;
    }
    else if (!isAdmin) {
      return true; 
    }
    else if (isWelcomePage && !hasUsageBanner()) {
      return true; 
    } 
    return false;
  }

  const startsExpanded = shouldStartExpanded();
  const [isExpanded, onToggleCollapse] = useToggle(startsExpanded);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (isOnBrowseOrSearch && rerender) {
        rerender(riskOnDiskCount, closeMalwareBanner);
      }
    }, 100);

    return () => clearTimeout(timer);
  }, [riskOnDiskCount, closeMalwareBanner]);

  useEffect(() => {
    if (isOnBrowseOrSearch && toggle) {
      toggle(isExpanded ? EXTJS_EXPANDED_HEIGHT : EXTJS_COLLAPSED_HEIGHT);
    }
  }, [isExpanded]);

  function retry() {
    send({type: 'RETRY'});
  }

  function dismiss() {
    send({type: 'DISMISS'});
  }

  return (
      <>
        {showWarningAlert && (
          <>
            <div className={isOnBrowseOrSearch ? "risk-on-disk-container-browse-search" : "risk-on-disk-container"}>
              <NxErrorAlert className="risk-on-disk-alert" onClose={isExpanded ? dismiss : undefined}>
                <div className="risk-on-disk-content">
                  <button 
                    type="button"
                    className="nx-collapsible-items__trigger"
                    onClick={onToggleCollapse || undefined}
                    aria-expanded={isExpanded}
                  > 
                    <div className="risk-on-disk-toggle">
                      <NxFontAwesomeIcon icon={faCaretRight} className={isExpanded ? 'expanded' : ''}/>
                    </div>
                    <div className="risk-on-disk-alert-title">
                      <NxFontAwesomeIcon icon={faExclamationCircle}/>
                      <NxH3>{riskOnDiskCount.toLocaleString()}</NxH3>
                      <NxH3>{riskOnDiskCount > 1 ? TITLE_PLURAL : TITLE_SINGULAR}</NxH3>
                    </div>
                  </button> 
                  {isExpanded && (
                    <>
                      <NxGrid.Column className='risk-on-disk-alert-description'>
                        <NxGrid.ColumnSection>
                          <NxGrid.Header>
                            <p className='risk-on-disk-alert-description-title'>{DESCRIPTION.TITLE}</p>
                          </NxGrid.Header>
                          <p className='risk-on-disk-alert-description-content'>{DESCRIPTION.CONTENT}</p>
                        </NxGrid.ColumnSection>
                        {!isAdmin && 
                          <NxGrid.ColumnSection>
                            <p>{DESCRIPTION.ADDITIONAL_NON_ADMIN_CONTENT}</p>
                          </NxGrid.ColumnSection>
                        }
                      </NxGrid.Column>
                      {isAdmin && <NxButtonBar>
                        <MalwareButton isProEdition={isProEdition}
                                       isMalwareRiskEnabled={isMalwareRiskEnabled}
                                       isIqServerEnabled={isIqServerEnabled}/>
                      </NxButtonBar>}
                    </>
                  )}
                </div>
              </NxErrorAlert>
          </div>
        </>
      )}
      {isMalwareRemediationPage && <div className={`malware-components-count ${riskOnDiskCount === 0 ? 'zero' : ''}`}>
        {riskOnDiskCount > 0 && <NxFontAwesomeIcon icon={faExclamationTriangle}/>}
        <NxH2>{riskOnDiskCount.toLocaleString()}</NxH2>
        <NxH3>{riskOnDiskCount > 1 ? TITLE_PLURAL : TITLE_SINGULAR}</NxH3>
      </div>}
    </>
  );
}

export default function MaliciousRiskOnDisk({rerender, toggle}) {
  const isRiskOnDiskEnabled = ExtJS.state().getValue(MALWARE_RISK_ON_DISK_ENABLED);
  const user = ExtJS.useUser();
  const userIsLogged = user ?? false;
  const showMaliciousRiskOnDisk = userIsLogged && isRiskOnDiskEnabled;

  const isRiskOnDiskNoneAdminOverrideEnabled = ExtJS.state().getValue(MALWARE_RISK_ON_DISK_NONADMIN_OVERRIDE_ENABLED);
  const isAdmin = user && user.administrator;
  const shouldHideForNonAdmin = isRiskOnDiskNoneAdminOverrideEnabled && !isAdmin;

  if (!showMaliciousRiskOnDisk || shouldHideForNonAdmin) {
    return null;
  }
  return <MaliciousRiskOnDiskContent user={user} rerender={rerender} toggle={toggle} />;
}
