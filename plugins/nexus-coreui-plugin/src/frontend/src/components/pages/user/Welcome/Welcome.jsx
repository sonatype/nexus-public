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
import React, { useEffect } from 'react';
import { useMachine } from '@xstate/react';
import { ExtAPIUtils , ExtJS, toURIParams, getVersionMajorMinor } from '@sonatype/nexus-ui-plugin';
import { NxButton, NxButtonBar, NxLoadWrapper, NxPageMain, NxPageTitle, NxH1, NxWarningAlert, NxLoadError }
  from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import welcomeMachine from './WelcomeMachine';

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

export default function Welcome() {
  const [state, send] = useMachine(welcomeMachine, { devtools: true }),
      loading = state.value === 'loading',
      error = state.value === 'error' ? state.context.error : null,
      proxyDownloadNumberParams = state.context.data?.proxyDownloadNumberParams;

  const user = ExtJS.useUser(),
      status = ExtJS.useStatus(),
      iframeProps = {
        version: status.version,
        versionMm: getVersionMajorMinor(status.version),
        edition: status.edition,
        usertype: getUserType(user),
        daysToExpiry: ExtJS.useLicense().daysToExpiry
      };

  function load() {
    send('LOAD');
  }

  async function enableLog4j() {
    send('ENABLE_LOG4J');
  }

  useEffect(load, [user]);

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
        { state.context.data?.showLog4jAlert &&
          <section aria-label="Log4j Capability Notice">
            <NxWarningAlert>
              <p className="nxrm-log4j-alert-content">{UIStrings.WELCOME.LOG4J_ALERT_CONTENT}</p>
              <NxButtonBar>
                <NxButton variant="primary" onClick={enableLog4j}>
                  {UIStrings.WELCOME.LOG4J_ENABLE_BUTTON_CONTENT}
                </NxButton>
              </NxButtonBar>
            </NxWarningAlert>
          </section>
        }
        { state.context.log4jError &&
          <NxLoadError titleMessage="An error occurred while enabling the Log4j capability."
                       error={state.context.log4jError}
                       retryHandler={enableLog4j} />
        }
        { state.context.data?.showOutreachIframe &&
          <iframe role="document"
                  aria-label="Outreach Frame"
                  src={`${iframeUrlPath}?${toURIParams(iframeProps)}${proxyDownloadNumberParams ?? ''}`}
                  className="nx-viewport-sized__scrollable nxrm-welcome__outreach-frame" />
        }
      </NxLoadWrapper>
    </NxPageMain>
  );
}
