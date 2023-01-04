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
import './SslCertificates.scss';
import SslCertificatesDetails from './SslCertificatesDetails';
import SslCertificatesAddForm from './SslCertificatesAddForm';
import SslCertificatesFormMachine, {SOURCES} from './SslCertificatesFormMachine';
import {canDeleteCertificate} from './SslCertificatesHelper';

export default function SslCertificatesForm({itemId, onDone}) {
  const machine = useMachine(SslCertificatesFormMachine, {
    context: {
      id: decodeURIComponent(itemId),
      pristineData: {
        remoteHostUrl: '',
        pemContent: ''
      },
      source: SOURCES.REMOTE_HOST
    },
    actions: {
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone
    },
    guards: {
      canDelete: () => canDeleteCertificate()
    },
    devTools: true
  });

  const [state] = machine;

  const {loadError, data} = state.context;

  const {inTrustStore, fingerprint} = data?.certificate || {};

  const showAddForm = !itemId && (!fingerprint || inTrustStore || loadError);

  return showAddForm ? (
    <SslCertificatesAddForm machine={machine} onDone={onDone} />
  ) : (
    <SslCertificatesDetails itemId={itemId} machine={machine} onDone={onDone} />
  );
}
