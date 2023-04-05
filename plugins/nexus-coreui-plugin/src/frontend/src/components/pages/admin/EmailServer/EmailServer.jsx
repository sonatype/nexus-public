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
import {NxTile} from '@sonatype/react-shared-components';
import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  ExtJS,
  Permissions,
} from '@sonatype/nexus-ui-plugin';

import {faEnvelope} from '@fortawesome/free-solid-svg-icons';

import EmailServerForm from './EmailServerForm';
import EmailServerReadOnly from './EmailServerReadOnly';
import EmailVerifyServer from './EmailVerifyServer';

import UIStrings from '../../../../constants/UIStrings';

import Machine from './EmailServerMachine';

export default function EmailServer() {
  const canEdit = ExtJS.checkPermission(Permissions.SETTINGS.UPDATE);
  const stateMachine = useMachine(Machine, {devTools: true});
  const [state] = stateMachine;
  const actor = state.context.emailVerifyServer;

  return (
    <Page>
      <PageHeader>
        <PageTitle icon={faEnvelope} {...UIStrings.EMAIL_SERVER.MENU} />
      </PageHeader>
      <ContentBody className="nxrm-email-server">
        <NxTile>
          {canEdit ? (
            <EmailServerForm parentMachine={stateMachine} />
          ) : (
            <EmailServerReadOnly />
          )}
        </NxTile>
        {canEdit && actor && (
          <NxTile>
            <EmailVerifyServer actor={actor} />
          </NxTile>
        )}
      </ContentBody>
    </Page>
  );
}
