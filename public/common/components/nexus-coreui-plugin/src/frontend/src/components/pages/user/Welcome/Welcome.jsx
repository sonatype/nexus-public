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
import {Box} from '@radix-ui/themes';
import {useMachine} from '@xstate/react';
import {ExtJS, toURIParams, getVersionMajorMinor} from '@sonatype/nexus-ui-plugin';
import {
  NxLoadWrapper, NxPageMain,
} from '@sonatype/react-shared-components';

import welcomeMachine from './WelcomeMachine';
import OutreachActions from './OutreachActions';
import UsageCenter from './UsageCenter/UsageCenter';
import MaliciousRiskOnDisk from '../../../widgets/riskondisk/MaliciousRiskOnDisk';
import CEHardLimitAlerts from './CEHardLimitAlerts/CEHardLimitAlerts';
import TelemetryWarningBanner from '../../../shared/telemetry/TelemetryWarningBanner';

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
  const [state, send] = useMachine(welcomeMachine, {devtools: false}),
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
    send({type: 'LOAD'});
  }

  const onLoad = () => {
    if (ref.current?.contentWindow) {
      try {
        setIframeHeight(
            ref.current.contentWindow.document.body.scrollHeight + iframePadding * 4
        )
      } catch (e) {
        // Cross-origin iframe - cannot access contentWindow.document.
        // Use a reasonable default height instead.
        setIframeHeight(800);
      }
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
      if (timeout) {
        clearTimeout(timeout)
      }

      window.removeEventListener('resize', debounce);
    };
  }, []);

  return (
      <NxPageMain>
        <div className="nxrm-welcome">
          <NxLoadWrapper loading={loading} error={error} retryHandler={load}>
            <div className="nxrm-welcome__outreach nx-viewport-sized__scrollable">
              <CEHardLimitAlerts />
              <TelemetryWarningBanner />
              <MaliciousRiskOnDisk />
              {isAdmin && <UsageCenter />}
              <OutreachActions/>
              {state.context.data?.showOutreachIframe &&
                  <Box style={{ width: '100%', minHeight: '400px' }}>
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
                        style={{
                          width: '100%',
                          height: iframeHeight,
                          border: 'none',
                          display: 'block',
                          backgroundColor: 'var(--nx-background-default, #fff)'
                        }}
                    />
                  </Box>
              }
            </div>
          </NxLoadWrapper>
        </div>
      </NxPageMain>
  );
}
