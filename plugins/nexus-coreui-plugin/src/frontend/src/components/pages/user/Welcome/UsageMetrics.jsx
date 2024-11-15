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
import {useMachine} from '@xstate/react';
import {
  NxCard,
  NxDivider,
  NxH2,
  NxH3,
  NxLoadWrapper} from '@sonatype/react-shared-components';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import UsageMetricsMachine from './UsageMetricsMachine';
import UsageMetricsWithCircuitB from './UsageMetricsWithCircuitB';

import './UsageMetrics.scss';

const {WELCOME: {
  USAGE: {
    MENU,
    TOTAL_COMPONENTS,
    UNIQUE_LOGINS,
    PEAK_REQUESTS_PER_MINUTE,
    PEAK_REQUESTS_PER_DAY}}} = UIStrings;

export default function UsageMetrics() {
  const [state, send] = useMachine(UsageMetricsMachine, {devtools: true}),
    {data: {totalComponents, uniqueLogins, peakRequestsPerMin, peakRequestsPerDay}, loadError} = state.context,
    isLoading = state.matches('loading');

  const isProEdition = ExtJS.isProEdition();
  const isHa = ExtJS.state().getValue('nexus.datastore.clustered.enabled');
  const isCircuitBEnabled = ExtJS.state().getValue('nexus.circuitb.enabled');

  function retry() {
    send({type: 'RETRY'});
  };

  return !isHa && <div className={`nxrm-usage-metrics${isCircuitBEnabled ? '-circuit-b' : ''} nxrm-metrics-section`}>
    <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
      {() =>
        <>
          <NxH2>{MENU.TEXT}</NxH2>
          <NxCard.Container>
            {isCircuitBEnabled ? <UsageMetricsWithCircuitB /> :
              <>
                <NxCard aria-label={TOTAL_COMPONENTS.TITLE}>
                  <NxCard.Header>
                    <NxH3>{TOTAL_COMPONENTS.TITLE}</NxH3>
                  </NxCard.Header>
                  <NxCard.Content>
                    <NxCard.Text>{totalComponents.toLocaleString()}</NxCard.Text>
                  </NxCard.Content>
                </NxCard>
                {!isProEdition &&
                  <NxCard aria-label={UNIQUE_LOGINS.TITLE}>
                    <NxCard.Header>
                      <NxH3>{UNIQUE_LOGINS.TITLE}</NxH3>
                    </NxCard.Header>
                    <NxCard.Content>
                      <NxCard.Text>{uniqueLogins.toLocaleString()}</NxCard.Text>
                      <NxCard.Text className="nxrm-usage-subtitle">{UNIQUE_LOGINS.SUB_TITLE}</NxCard.Text>
                    </NxCard.Content>
                  </NxCard>
                }
                <NxCard aria-label={PEAK_REQUESTS_PER_MINUTE.TITLE}>
                  <NxCard.Header>
                    <NxH3>{PEAK_REQUESTS_PER_MINUTE.TITLE}</NxH3>
                  </NxCard.Header>
                  <NxCard.Content>
                    <NxCard.Text>{peakRequestsPerMin.toLocaleString()}</NxCard.Text>
                    <NxCard.Text className="nxrm-usage-subtitle">{PEAK_REQUESTS_PER_MINUTE.SUB_TITLE}</NxCard.Text>
                  </NxCard.Content>
                </NxCard>
                <NxCard aria-label={PEAK_REQUESTS_PER_DAY.TITLE}>
                  <NxCard.Header>
                    <NxH3>{PEAK_REQUESTS_PER_DAY.TITLE}</NxH3>
                  </NxCard.Header>
                  <NxCard.Content>
                    <NxCard.Text>{peakRequestsPerDay.toLocaleString()}</NxCard.Text>
                    <NxCard.Text className="nxrm-usage-subtitle">{PEAK_REQUESTS_PER_DAY.SUB_TITLE}</NxCard.Text>
                  </NxCard.Content>
                </NxCard>
              </>
            }
          </NxCard.Container>
          <NxDivider />
        </>
      }
    </NxLoadWrapper>
  </div>
};
