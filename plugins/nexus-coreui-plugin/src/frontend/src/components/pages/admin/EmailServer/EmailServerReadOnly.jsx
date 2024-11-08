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
  NxInfoAlert,
  NxH2,
} from '@sonatype/react-shared-components';

import {FormUtils, ReadOnlyField} from '@sonatype/nexus-ui-plugin';

import Machine from './EmailServerMachine';

import UIStrings from '../../../../constants/UIStrings';

const {FORM: LABELS, READ_ONLY} = UIStrings.EMAIL_SERVER;
const {readOnlyCheckboxValueLabel} = FormUtils;

export default function EmailServerReadOnly() {
  const [current, send] = useMachine(Machine, {devTools: true});
  const {data, loadError} = current.context;
  const isLoading = current.matches('loading');
  const enabled = readOnlyCheckboxValueLabel(data.enabled);
  const nexusTrustStoreEnabled = readOnlyCheckboxValueLabel(
    data.nexusTrustStoreEnabled
  );
  const retry = () => send({type: 'RETRY'});

  const renderData = (label, value) => {
    const enable = value ? 'ENABLE' : 'NOT_ENABLE';
    return <NxReadOnly.Data>{READ_ONLY[enable][label]}</NxReadOnly.Data>;
  };

  return (
    <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
      <NxH2>{LABELS.SECTIONS.SETUP}</NxH2>
      <NxInfoAlert>{UIStrings.SETTINGS.READ_ONLY.WARNING}</NxInfoAlert>
      <NxReadOnly>
        <ReadOnlyField label={LABELS.ENABLED.LABEL} value={enabled} />
        {data.enabled && (
          <>
            <ReadOnlyField label={LABELS.HOST.LABEL} value={data.host} />
            <ReadOnlyField label={LABELS.PORT.LABEL} value={data.port} />
            <ReadOnlyField
              label={UIStrings.USE_TRUST_STORE.LABEL}
              value={nexusTrustStoreEnabled}
            />
            <ReadOnlyField
              label={LABELS.USERNAME.LABEL}
              value={data.username}
            />
            <ReadOnlyField
              label={LABELS.FROM_ADDRESS.LABEL}
              value={data.fromAddress}
            />
            <ReadOnlyField
              label={LABELS.SUBJECT_PREFIX.LABEL}
              value={data.subjectPrefix}
            />
            <NxReadOnly.Label>{LABELS.SSL_TLS_OPTIONS.LABEL}</NxReadOnly.Label>
            {renderData('ENABLE_STARTTLS', data.startTlsEnabled)}
            {renderData('REQUIRE_STARTTLS', data.startTlsRequired)}
            {renderData('ENABLE_SSL_TLS', data.sslOnConnectEnabled)}
            {renderData('IDENTITY_CHECK', data.sslServerIdentityCheckEnabled)}
          </>
        )}
      </NxReadOnly>
    </NxLoadWrapper>
  );
}
