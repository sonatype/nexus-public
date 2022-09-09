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
  NxReadOnly,
  NxLoadWrapper,
  NxAccordion,
  NxTile,
  NxInfoAlert,
  NxH2
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

  return (
    <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
      <NxH2>{LABELS.READ_ONLY.LABEL}</NxH2>
      <NxInfoAlert>{LABELS.READ_ONLY.WARNING}</NxInfoAlert>
      <NxReadOnly>
        <NxReadOnly.Label>{LABELS.USER_AGENT.LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{data.userAgentSuffix}</NxReadOnly.Data>
        <NxReadOnly.Label>{LABELS.TIMEOUT.LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{data.timeout}</NxReadOnly.Data>
        <NxReadOnly.Label>{LABELS.ATTEMPTS.LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{data.retries}</NxReadOnly.Data>
        {httpEnabled && (
          <>
            <NxReadOnly.Label>{LABELS.PROXY.HTTP_HOST}</NxReadOnly.Label>
            <NxReadOnly.Data>{data.httpHost}</NxReadOnly.Data>
            <NxReadOnly.Label>{LABELS.PROXY.HTTP_PORT}</NxReadOnly.Label>
            <NxReadOnly.Data>{data.httpPort}</NxReadOnly.Data>
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
              <NxReadOnly.Label>{LABELS.PROXY.USERNAME}</NxReadOnly.Label>
              <NxReadOnly.Data>{data.httpAuthUsername}</NxReadOnly.Data>
              <NxReadOnly.Label>{LABELS.PROXY.HOST_NAME}</NxReadOnly.Label>
              <NxReadOnly.Data>{data.httpAuthNtlmHost}</NxReadOnly.Data>
              <NxReadOnly.Label>{LABELS.PROXY.DOMAIN}</NxReadOnly.Label>
              <NxReadOnly.Data>{data.httpAuthNtlmDomain}</NxReadOnly.Data>
            </NxAccordion>
          </NxTile.Content>
        )}
        {isHttpsEnabled && (
          <>
            <NxReadOnly.Label>{LABELS.PROXY.HTTPS_HOST}</NxReadOnly.Label>
            <NxReadOnly.Data>{data.httpsHost}</NxReadOnly.Data>
            <NxReadOnly.Label>{LABELS.PROXY.HTTPS_PORT}</NxReadOnly.Label>
            <NxReadOnly.Data>{data.httpsPort}</NxReadOnly.Data>
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
              <NxReadOnly.Label>{LABELS.PROXY.USERNAME}</NxReadOnly.Label>
              <NxReadOnly.Data>{data.httpsAuthUsername}</NxReadOnly.Data>
              <NxReadOnly.Label>{LABELS.PROXY.HOST_NAME}</NxReadOnly.Label>
              <NxReadOnly.Data>{data.httpsAuthNtlmHost}</NxReadOnly.Data>
              <NxReadOnly.Label>{LABELS.PROXY.DOMAIN}</NxReadOnly.Label>
              <NxReadOnly.Data>{data.httpsAuthNtlmDomain}</NxReadOnly.Data>
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
