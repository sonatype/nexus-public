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
import {isNil} from 'ramda';
import {
  NxReadOnly,
  NxLoadWrapper,
  NxAccordion,
  NxTile,
  NxInfoAlert,
  NxH2,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

const {CONFIGURATION: LABELS} = UIStrings.HTTP;

import Machine from './HttpMachine';

export default function HttpReadOnly() {
  const [current, send] = useMachine(Machine, {devTools: true});
  const {data, loadError} = current.context;
  const {httpEnabled, httpAuthEnabled, httpsEnabled, httpsAuthEnabled} = data;
  const isHttpsEnabled = httpsEnabled && httpEnabled;
  const isLoading = current.matches('loading');
  const nonProxyHosts = data.nonProxyHosts || [];

  const retry = () => send('RETRY');

  const renderField = (label, data) => {
    return (
      !isNil(data) && (
        <>
          <NxReadOnly.Label>{label}</NxReadOnly.Label>
          <NxReadOnly.Data>{data}</NxReadOnly.Data>
        </>
      )
    );
  };

  return (
    <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
      <NxH2>{LABELS.READ_ONLY.LABEL}</NxH2>
      <NxInfoAlert>{LABELS.READ_ONLY.WARNING}</NxInfoAlert>
      <NxReadOnly>
        {renderField(LABELS.USER_AGENT.LABEL, data.userAgentSuffix)}
        {renderField(LABELS.TIMEOUT.LABEL, data.timeout)}
        {renderField(LABELS.ATTEMPTS.LABEL, data.retries)}
        {httpEnabled && (
          <>
            {renderField(LABELS.PROXY.HTTP_HOST.LABEL, data.httpHost)}
            {renderField(LABELS.PROXY.HTTP_PORT, data.httpPort)}
          </>
        )}
        {httpEnabled && httpAuthEnabled && (
          <NxTile.Content className="nx-tile-content--accordion-container">
            <NxAccordion open={data.httpAuthEnabled}>
              <NxAccordion.Header>
                <NxAccordion.Title>
                  {LABELS.PROXY.HTTP_AUTHENTICATION}
                </NxAccordion.Title>
              </NxAccordion.Header>
              {renderField(LABELS.PROXY.USERNAME, data.httpAuthUsername)}
              {renderField(LABELS.PROXY.HOST_NAME, data.httpAuthNtlmHost)}
              {renderField(LABELS.PROXY.DOMAIN, data.httpAuthNtlmDomain)}
            </NxAccordion>
          </NxTile.Content>
        )}
        {isHttpsEnabled && (
          <>
            {renderField(LABELS.PROXY.HTTPS_HOST, data.httpsHost)}
            {renderField(LABELS.PROXY.HTTPS_PORT, data.httpsPort)}
          </>
        )}
        {isHttpsEnabled && httpsAuthEnabled && (
          <NxTile.Content className="nx-tile-content--accordion-container">
            <NxAccordion open={data.httpAuthEnabled}>
              <NxAccordion.Header>
                <NxAccordion.Title>
                  {LABELS.PROXY.HTTPS_AUTHENTICATION}
                </NxAccordion.Title>
              </NxAccordion.Header>
              {renderField(LABELS.PROXY.USERNAME, data.httpsAuthUsername)}
              {renderField(LABELS.PROXY.HOST_NAME, data.httpsAuthNtlmHost)}
              {renderField(LABELS.PROXY.DOMAIN, data.httpsAuthNtlmDomain)}
            </NxAccordion>
          </NxTile.Content>
        )}
        {nonProxyHosts.length > 0 && (
          <>
            <NxReadOnly.Label>{LABELS.EXCLUDE.LABEL}</NxReadOnly.Label>
            {nonProxyHosts.map((item) => (
              <NxReadOnly.Data key={item}>{item}</NxReadOnly.Data>
            ))}
          </>
        )}
      </NxReadOnly>
    </NxLoadWrapper>
  );
}
