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
import React, {useEffect, useRef, useState} from 'react';
import {useMachine} from '@xstate/react';
import {ExtJS, toURIParams, getVersionMajorMinor} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxButtonBar,
  NxLoadWrapper,
  NxPageMain,
  NxPageTitle,
  NxH1,
  NxWarningAlert
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import welcomeMachine from './WelcomeMachine';
import OutreachActions from './OutreachActions';
import UsageMetrics from './UsageMetrics';
import MaliciousRiskOnDisk from '../../maliciousrisk/riskondisk/MaliciousRiskOnDisk';

import './Welcome.scss';

const iframeUrlPath = './service/outreach/';

function getUserType(user) {
  if (!user) {
    return 'anonymous';
  }
  else if (user.administrator) {
    return 'admin';
  }
  else {
    return 'normal';
  }
}

function getDatabaseType() {
  return ExtJS.state().getValue('datastore.isPostgresql') ? 'postgres' : 'h2';
}

const iframeDefaultHeight = 1000;
const iframePadding = 48;

export default function Welcome() {
  const [state, send] = useMachine(welcomeMachine, { devtools: true }),
      [iframeHeight, setIframeHeight] = useState(iframeDefaultHeight),
      ref = useRef(),
      loading = state.matches('loading'),
      error = state.matches('error') ? state.context.error : null,
      proxyDownloadNumberParams = state.context.data?.proxyDownloadNumberParams;

  const user = ExtJS.useUser(),
      status = ExtJS.useStatus(),
      iframeProps = {
        version: status.version,
        versionMm: getVersionMajorMinor(status.version),
        edition: status.edition,
        usertype: getUserType(user),
        daysToExpiry: ExtJS.useLicense().daysToExpiry,
        databaseType: getDatabaseType()
      },
      isAdmin = user?.administrator;

  function load() {
    send('LOAD');
  }

  async function navigateToFirewall() {
    window.location.href = '#admin/iq';
  }

  const onLoad = () => {
    if (ref.current?.contentWindow) {
      setIframeHeight(
        ref.current.contentWindow.document.body.scrollHeight + iframePadding
      )
    }
  };

  useEffect(load, [user]);

  useEffect(() => {
    let timeout;

    const debounce = () => {
      timeout = setTimeout(onLoad, 500);
    };

    window.addEventListener('resize', debounce);

    return () => {
      if(timeout) {
        clearTimeout(timeout)
      }

      window.removeEventListener('resize', debounce);
    };
  }, []);

  const shouldShowMaliciousRiskBanner = ExtJS.state().getValue('MaliciousRiskDashboard') && isAdmin;

  function navigateToMaliciousRiskDashboard() {
    window.location.href = '#browse/maliciousrisk';
  }

  return (
    <NxPageMain className="nx-viewport-sized nxrm-welcome">
      <NxPageTitle className="nxrm-welcome__page-title">
        <NxPageTitle.Headings>
          <NxH1>
            {/* Empty alt per WHATWG-HTML § 4.8.4.4.4 paragraph 6
              * https://html.spec.whatwg.org/multipage/images.html#a-short-phrase-or-label-with-an-alternative-graphical-representation:-icons,-logos
              * NOTE: the role here should be redundant per https://www.w3.org/TR/html-aria/#el-img-empty-alt but
              * the RTL queries don't appear to recognize that nuance
              */}
            <img className="nxrm-welcome__logo"
                 alt=""
                 role="presentation"
                 src="./static/rapture/resources/icons/x32/sonatype.png" />
            <span>{UIStrings.WELCOME.MENU.text}</span>
          </NxH1>
          <NxPageTitle.Subtitle>{UIStrings.WELCOME.MENU.description}</NxPageTitle.Subtitle>
        </NxPageTitle.Headings>
      </NxPageTitle>
      <NxLoadWrapper loading={loading} error={error} retryHandler={load}>
        <div className="nxrm-welcome__outreach nx-viewport-sized__scrollable">
          {user && <MaliciousRiskOnDisk/>}
          { shouldShowMaliciousRiskBanner &&
              <div className="nxrm-welcome__malicious-risk-banner">
                <div className="banner-text">
                  <p className="banner-first-line">
                      <span className="heavy-bold">{UIStrings.WELCOME.MALICIOUS_RISK_BANNER_CONTENT.FIRST_LINE_HEAVY_BOLD_TEXT}
                      </span> {UIStrings.WELCOME.MALICIOUS_RISK_BANNER_CONTENT.FIRST_LINE_NORMAL_BOLD_TEXT}
                  </p>
                  <p className="banner-second-line">{UIStrings.WELCOME.MALICIOUS_RISK_BANNER_CONTENT.SECOND_LINE_TEXT}</p>
                </div>
                <NxButtonBar>
                  <NxButton id="view-dashboard-banner-btn" variant="primary"
                            onClick={navigateToMaliciousRiskDashboard}>
                    {UIStrings.WELCOME.MALICIOUS_RISK_BANNER_BUTTON_CONTENT}
                  </NxButton>
                </NxButtonBar>
              </div>
          }
          { state.context.data?.showFirewallAlert &&
              <section id="nxrm-firewall-onboarding-nudge" className="nxrm-firewall-onboarding" aria-label="Firewall Capability Notice">
                <NxWarningAlert>
                  <p className="nxrm-firewall-alert-content">{UIStrings.WELCOME.FIREWALL_ALERT_CONTENT}</p>
                  <NxButtonBar>
                    <NxButton id="nxrm-welcome-firewall-enable-btn" variant="primary" onClick={navigateToFirewall}>
                      {UIStrings.WELCOME.FIREWALL_ENABLE_BUTTON_CONTENT}
                    </NxButton>
                  </NxButtonBar>
                </NxWarningAlert>
              </section>
          }
          {isAdmin && <UsageMetrics />}
          <OutreachActions />
          { state.context.data?.showOutreachIframe &&
              <iframe
                  id="nxrm-welcome-outreach-frame"
                  role="document"
                  height={iframeHeight}
                  ref={ref}
                  scrolling="no"
                  onLoad={onLoad}
                  aria-label="Outreach Frame"
                  src={`${iframeUrlPath}?${toURIParams(iframeProps)}${proxyDownloadNumberParams ?? ''}`}
                  className="nxrm-welcome__outreach-frame"
              />
          }
        </div>
      </NxLoadWrapper>
    </NxPageMain>
  );
}
