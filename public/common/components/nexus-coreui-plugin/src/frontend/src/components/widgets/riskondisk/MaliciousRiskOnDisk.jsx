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
import React, {useEffect, useRef} from 'react';
import {useMachine} from "@xstate/react";

import {
  NxButtonBar,
  NxErrorAlert,
  NxFontAwesomeIcon,
  NxGrid,
  NxH3,
  useToggle
} from "@sonatype/react-shared-components";
import {ExtJS, isPageHashIncluding} from '@sonatype/nexus-ui-plugin';
import {faExclamationCircle, faCaretRight} from "@fortawesome/free-solid-svg-icons";

import MaliciousRiskOnDiskMachine from "./MaliciousRiskOnDiskMachine";
import UIStrings from '../../../constants/UIStrings';
import "./MaliciousRiskOnDisk.scss";
import FeatureFlags from '../../../constants/FeatureFlags';
import {helperFunctions} from '../SystemStatusAlerts/CELimits/UsageHelper';
import { useSize } from '../../../hooks/useSize';
import classNames from 'classnames';

const {
  CLM,
  MALWARE_RISK_ENABLED,
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

export default function MaliciousRiskOnDisk({ toggle, onSizeChanged, className }) {
  const isRiskOnDiskEnabled = ExtJS.state().getValue(MALWARE_RISK_ENABLED);
  const malwareData = ExtJS.useState(() => ExtJS.state().getValue('nexus.malware.count'));
  const testOverride = parseInt(localStorage.getItem('SONATYPE_TEST_MALWARE_BANNER') || '0', 10) || 0;
  const malwareCount = testOverride || (malwareData?.totalCount ?? 0);
  const user = ExtJS.useUser();
  const userIsLogged = user ?? false;
  const showMaliciousRiskOnDisk = userIsLogged && (isRiskOnDiskEnabled || malwareCount > 0);

  const isRiskOnDiskNoneAdminOverrideEnabled = ExtJS.state().getValue(MALWARE_RISK_ON_DISK_NONADMIN_OVERRIDE_ENABLED);
  const isAdmin = user && user.administrator;
  const shouldHideForNonAdmin = isRiskOnDiskNoneAdminOverrideEnabled && !isAdmin;

  if (!userIsLogged) {
    document.cookie = 'MALWARE_BANNER=; expires=Thu, 26 Feb 1950 00:00:00 UTC; path=/';
  }

  if (!showMaliciousRiskOnDisk || shouldHideForNonAdmin) {
    return null;
  }

  return <MaliciousRiskOnDiskContent user={user} toggle={toggle} onSizeChanged={onSizeChanged} />;
}

function MaliciousRiskOnDiskContent({ toggle, onSizeChanged, className }) {
  const maliciousRiskRef = useRef(null);
  const maliciousRiskSize = useSize(maliciousRiskRef);

  useEffect(() => {
    if (!!maliciousRiskSize && typeof onSizeChanged === 'function') {
      onSizeChanged(maliciousRiskSize.width, maliciousRiskSize.height);
    }
  }, [maliciousRiskSize.width, maliciousRiskSize.height]);

  const [state, send] = useMachine(MaliciousRiskOnDiskMachine, {
    devTools: true
  });
  const maliciousRiskOnDisk = ExtJS.useState(() => ExtJS.state().getValue('nexus.malware.count'));
  const closeMalwareBanner = state.matches('close');
  const user = ExtJS.useUser();

  const isAdmin = user?.administrator;
  const isProEdition = ExtJS.isProEdition();
  const isIqServerEnabled = ExtJS.state().getValue(CLM)?.enabled;
  const isMalwareRiskEnabled = ExtJS.state().getValue(MALWARE_RISK_ENABLED);
  const isOnBrowseOrSearch = isPageHashIncluding(['#browse/browse', '#browse/search']);

  const isWelcomePage = !window.location.hash || isPageHashIncluding(['#browse/welcome']);

  const testOverrideContent = parseInt(localStorage.getItem('SONATYPE_TEST_MALWARE_BANNER') || '0', 10) || 0;
  const riskOnDiskCount = testOverrideContent || (maliciousRiskOnDisk?.totalCount ?? 0);
  // Show banner if malware count > 0 (and not dismissed)
  const showWarningAlert = riskOnDiskCount > 0 && !closeMalwareBanner;
  const throttlingStatus = useThrottlingStatus();

  const startsExpanded = shouldStartExpanded();
  const [isExpanded, onToggleCollapse] = useToggle(startsExpanded);

  useEffect(() => {
    if (isOnBrowseOrSearch && toggle) {
      toggle(isExpanded ? EXTJS_EXPANDED_HEIGHT : EXTJS_COLLAPSED_HEIGHT);
    }
  }, [isExpanded]);

  if (!showWarningAlert) {
    return null;
  }

  return (<div className={`${classNames('risk-on-disk-container', className)}`} ref={maliciousRiskRef}>
    <NxErrorAlert
        aria-label="Malicious Components Found"
        className="risk-on-disk-alert"
        onClose={isExpanded ? dismiss : undefined}
    >
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
            <NxH3>
              <span className="nxrm-malware-components-count-value">{riskOnDiskCount.toLocaleString()}</span>
              {getTitle()}
            </NxH3>
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
                <MalwareButton />
              </NxButtonBar>}
            </>
        )}
      </div>
    </NxErrorAlert>
  </div>);

  function dismiss() {
    send({type: 'DISMISS'});
  }

  function hasUsageBanner() {
    if (throttlingStatus === 'BELOW_LIMITS_GRACE_PERIOD_ENDED' &&
        document.cookie.includes('under_end_grace=dismissed')) {
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

  function getTitle() {
    return `${riskOnDiskCount > 1 ? TITLE_PLURAL : TITLE_SINGULAR}`
  }
}

function MalwareButton() {
  const isPreview = typeof window !== 'undefined' && window.location.hash.startsWith('#preview');
  const href = isPreview ? '#preview/browse/malwarerisk' : '#browse/malwarerisk';
  return <a className="nx-btn nx-btn--error" href={href}>{VIEW_MALWARE_RISK}</a>;
}
