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

import AnonymousMachine from './AnonymousMachine';

export default function AnonymousSettingsReadOnly() {
  const [current, send] = useMachine(AnonymousMachine, {devTools: true});
  const {data, loadError, realms} = current.context;
  const isLoading = current.matches('loading');
  const realmName = realms?.find(realm => realm.id === data.realmName).name;
  const accessStatus = FormUtils.readOnlyCheckboxValueLabel(data.enabled);

  function retry() {
    send({type: 'RETRY'});
  }

  return <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
    <NxInfoAlert>{UIStrings.SETTINGS.READ_ONLY.WARNING}</NxInfoAlert>
    <NxReadOnly>
      <NxReadOnly.Label>{UIStrings.ANONYMOUS_SETTINGS.ENABLED_CHECKBOX_LABEL}</NxReadOnly.Label>
      <NxReadOnly.Data>{accessStatus}</NxReadOnly.Data>
      <NxReadOnly.Label>{UIStrings.ANONYMOUS_SETTINGS.USERNAME_TEXTFIELD_LABEL}</NxReadOnly.Label>
      <NxReadOnly.Data>{data.userId}</NxReadOnly.Data>
      <NxReadOnly.Label>{UIStrings.ANONYMOUS_SETTINGS.REALM_SELECT_LABEL}</NxReadOnly.Label>
      <NxReadOnly.Data>{realmName}</NxReadOnly.Data>
    </NxReadOnly>
  </NxLoadWrapper>;
}
