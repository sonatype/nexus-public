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
  NxLoadWrapper,
  NxInfoAlert,
  NxReadOnly,
} from '@sonatype/react-shared-components';

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import Machine from './IqServerMachine';

export default function IqServerReadOnly() {
  const [current, send] = useMachine(Machine, {devTools: true});
  const {data, loadError} = current.context;
  const isLoading = current.matches('loading');
  const enabled = FormUtils.readOnlyCheckboxValueLabel(data.enabled);
  const useTrustStoreForUrl = FormUtils.readOnlyCheckboxValueLabel(data.useTrustStoreForUrl);
  const authenticationTypeLabel = data.authenticationType === 'USER'
      ? UIStrings.IQ_SERVER.AUTHENTICATION_TYPE.USER
      : UIStrings.IQ_SERVER.AUTHENTICATION_TYPE.PKI;
  const timeoutText = data.timeoutSeconds || UIStrings.IQ_SERVER.CONNECTION_TIMEOUT_DEFAULT_VALUE_LABEL;
  const showLink = FormUtils.readOnlyCheckboxValueLabel(data.showLink);

  function retry() {
    send({type: 'RETRY'});
  }

  return <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
    <NxInfoAlert>{UIStrings.SETTINGS.READ_ONLY.WARNING}</NxInfoAlert>
    <NxReadOnly>
      <NxReadOnly.Label>{UIStrings.IQ_SERVER.ENABLED.label}</NxReadOnly.Label>
      <NxReadOnly.Data>{enabled}</NxReadOnly.Data>
      {data.url && <>
        <NxReadOnly.Label>{UIStrings.IQ_SERVER.IQ_SERVER_URL.label}</NxReadOnly.Label>
        <NxReadOnly.Data>{data.url}</NxReadOnly.Data>
        <NxReadOnly.Label>{UIStrings.IQ_SERVER.TRUST_STORE.label}</NxReadOnly.Label>
        <NxReadOnly.Data>{useTrustStoreForUrl}</NxReadOnly.Data>
        <NxReadOnly.Label>{UIStrings.IQ_SERVER.AUTHENTICATION_TYPE.label}</NxReadOnly.Label>
        <NxReadOnly.Data>{authenticationTypeLabel}</NxReadOnly.Data>
        {data.authenticationType === 'USER' && <>
          <NxReadOnly.Label>{UIStrings.IQ_SERVER.USERNAME.label}</NxReadOnly.Label>
          <NxReadOnly.Data>{data.username}</NxReadOnly.Data>
        </>}
        <NxReadOnly.Label>{UIStrings.IQ_SERVER.CONNECTION_TIMEOUT.label}</NxReadOnly.Label>
        <NxReadOnly.Data>{timeoutText}</NxReadOnly.Data>
        {data.properties && <>
          <NxReadOnly.Label>{UIStrings.IQ_SERVER.PROPERTIES.label}</NxReadOnly.Label>
          <NxReadOnly.Data>{data.properties}</NxReadOnly.Data>
        </>}
        <NxReadOnly.Label>{UIStrings.IQ_SERVER.SHOW_LINK.label}</NxReadOnly.Label>
        <NxReadOnly.Data>{showLink}</NxReadOnly.Data>
      </>}
    </NxReadOnly>
  </NxLoadWrapper>;
}
