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
import React, {useState, useEffect} from 'react';
import {useMachine} from '@xstate/react';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {useRouter} from '@uirouter/react';
import {faShieldAlt, faChevronRight} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';

import IqServerConnectedMachine from './IqServerConnectedMachine';

import firewallLogoDark from '../../../../../../art/logos/sonatype-firewall-logo-iq-dark.svg';
import firewallLogoLight from '../../../../../../art/logos/sonatype-firewall-logo-iq-light.svg';
import lifecycleLogoDark from '../../../../../../art/logos/sonatype-lifecycle-logo-iq-dark.svg';
import lifecycleLogoLight from '../../../../../../art/logos/sonatype-lifecycle-logo-iq-light.svg';

import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  PageActions,
  Section
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxTextLink,
  NxPositiveStatusIndicator,
  NxErrorStatusIndicator,
  NxErrorAlert,
  NxLoadWrapper
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import {ROUTE_NAMES} from '../../../../routerConfig/routeNames/routeNames';

import './IqServer.scss';

function useDarkMode() {
  const htmlEl = document.documentElement;
  const [isDarkMode, setDarkMode] = useState(htmlEl.classList.contains('nx-html--dark-mode'));

  useEffect(() => {
    const observer = new MutationObserver(() => {
      setDarkMode(htmlEl.classList.contains('nx-html--dark-mode'));
    });

    observer.observe(htmlEl, {attributes: true, attributeFilter: ['class']});

    return () => observer.disconnect();
  }, [htmlEl]);

  return isDarkMode;
}

export default function IqServerConnected() {
  const router = useRouter();
  const [current] = useMachine(IqServerConnectedMachine, {devTools: true});
  const {data, iqServerUrl, connectionStatus, error} = current.context;
  const isLoading = current.matches('loading') || current.matches('verifyingConnection');
  const isDarkMode = useDarkMode();
  const firewallLogo = isDarkMode ? firewallLogoDark : firewallLogoLight;
  const lifecycleLogo = isDarkMode ? lifecycleLogoDark : lifecycleLogoLight;

  const hasLifecycle = data?.lifecycle || false;
  const hasFirewall = data?.firewall || false;
  const isConnected = connectionStatus === 'connected';

  if (!ExtJS.useUser()) {
    return null;
  }

  function navigateToConnectionSettings() {
    router.stateService.go(ROUTE_NAMES.ADMIN.IQ.ROOT, {fromConnected: true});
  }

  function navigateToLifecycle() {
    if (hasLifecycle) {
      router.stateService.go(ROUTE_NAMES.ADMIN.IQ.SONATYPE_LIFECYCLE.ROOT);
    }
  }

  return <Page>
    <PageHeader>
      <div className="nxrm-iq-header-layout">
        <div className="nxrm-iq-page-title-wrapper">
          <PageTitle
            icon={faShieldAlt}
            text={UIStrings.IQ_SERVER.CONNECTED.TITLE}
          />
          {!isLoading && (
            <div className="nxrm-iq-page-description">
              {isConnected ? (
                <NxPositiveStatusIndicator>{UIStrings.IQ_SERVER.CONNECTED.STATUS}</NxPositiveStatusIndicator>
              ) : (
                <NxErrorStatusIndicator>{UIStrings.IQ_SERVER.CONNECTED.CONNECTION_ERROR}</NxErrorStatusIndicator>
              )}
              {iqServerUrl && (
                <span className="nxrm-iq-subtitle">{iqServerUrl}</span>
              )}
            </div>
          )}
        </div>
        <PageActions>
          <NxButton variant="tertiary" onClick={navigateToConnectionSettings}>
            {UIStrings.IQ_SERVER.CONNECTED.CONNECTION_SETTINGS_BUTTON}
          </NxButton>
        </PageActions>
      </div>
    </PageHeader>
    <ContentBody className="nxrm-iq-server-connected">
      <Section>
        <NxLoadWrapper loading={isLoading} error={error} retryHandler={() => window.location.reload()}>
          <div className="nxrm-iq-tiles-container" role="list">
          <div
            className={`nxrm-iq-tile ${hasLifecycle ? 'enabled' : 'disabled'}`}
            role="listitem"
            aria-label={`Sonatype Lifecycle - ${hasLifecycle ? 'Enabled' : 'Not Available'}`}
            tabIndex={hasLifecycle ? 0 : -1}
            onClick={navigateToLifecycle}
            onKeyDown={(e) => {
              if (hasLifecycle && (e.key === 'Enter' || e.key === ' ')) {
                e.preventDefault();
                navigateToLifecycle();
              }
            }}
            style={hasLifecycle ? {cursor: 'pointer'} : {cursor: 'default'}}
          >
            <div className="nxrm-iq-tile-content">
              <div className="nxrm-iq-tile-header">
                <img src={lifecycleLogo} alt="Sonatype Lifecycle" className="nxrm-iq-tile-logo-image" />
              </div>
              <div className={`nxrm-iq-tile-status ${hasLifecycle ? 'enabled' : 'disabled'}`}>
                <span className="status-indicator" aria-hidden="true"></span>
                {hasLifecycle ? UIStrings.IQ_SERVER.CONNECTED.LIFECYCLE.ENABLED : UIStrings.IQ_SERVER.CONNECTED.LIFECYCLE.NOT_AVAILABLE}
              </div>
              {!hasLifecycle && (
                <>
                  <div className="nxrm-iq-tile-error-message">
                    {UIStrings.IQ_SERVER.CONNECTED.LIFECYCLE.LICENSE_NOT_AVAILABLE}
                  </div>
                  <NxTextLink className="nxrm-iq-explore-link" external href="https://links.sonatype.com/products/nxrm3/browse/lc-learn" rel="noopener noreferrer">
                    {UIStrings.IQ_SERVER.CONNECTED.LIFECYCLE.EXPLORE_LINK}
                  </NxTextLink>
                </>
              )}
            </div>
            {hasLifecycle && (
              <div className="nxrm-iq-tile-arrow" aria-hidden="true">
                <FontAwesomeIcon icon={faChevronRight} />
              </div>
            )}
          </div>

          <div
            className={`nxrm-iq-tile ${hasFirewall ? 'enabled' : 'disabled'}`}
            role="listitem"
            aria-label={`Repository Firewall - ${hasFirewall ? 'Enabled' : 'Not Available'}`}
            tabIndex={-1}
            style={{cursor: 'default'}}
          >
            <div className="nxrm-iq-tile-content">
              <div className="nxrm-iq-tile-header">
                <img src={firewallLogo} alt="Sonatype Repository Firewall" className="nxrm-iq-tile-logo-image" />
              </div>
              <div className="nxrm-iq-tile-description">
                {UIStrings.IQ_SERVER.CONNECTED.FIREWALL.DESCRIPTION}
              </div>
              <div className={`nxrm-iq-tile-status ${hasFirewall ? 'enabled' : 'disabled'}`}>
                <span className="status-indicator" aria-hidden="true"></span>
                {hasFirewall ? UIStrings.IQ_SERVER.CONNECTED.FIREWALL.ENABLED : UIStrings.IQ_SERVER.CONNECTED.FIREWALL.NOT_AVAILABLE}
              </div>
              {!hasFirewall && (
                <>
                  <div className="nxrm-iq-tile-error-message">
                    {UIStrings.IQ_SERVER.CONNECTED.FIREWALL.LICENSE_NOT_AVAILABLE}
                  </div>
                  <NxTextLink className="nxrm-iq-explore-link" external href="https://links.sonatype.com/nexus-repository-firewall" rel="noopener noreferrer">
                    {UIStrings.IQ_SERVER.CONNECTED.FIREWALL.EXPLORE_LINK}
                  </NxTextLink>
                </>
              )}
            </div>
          </div>
        </div>
        </NxLoadWrapper>
      </Section>
    </ContentBody>
  </Page>;
}
