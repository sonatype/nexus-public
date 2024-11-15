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
import {faKey} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  ExtJS,
  Permissions
} from '@sonatype/nexus-ui-plugin';

import {NxTile, NxLoadWrapper} from '@sonatype/react-shared-components';

import UserTokensReadOnly from './UserTokensReadOnly';
import UserTokensMachine from './UserTokensMachine';
import UIStrings from '../../../../constants/UIStrings';
import UserTokensForm from './UserTokensForm';

import './UserTokens.scss';

const {
  USER_TOKEN_CONFIGURATION: {MENU}
} = UIStrings;

export default function UserTokens() {
  const canUpdate = ExtJS.checkPermission(Permissions.USER_TOKENS_SETTINGS.UPDATE);
  const canDelete = ExtJS.checkPermission(Permissions.USER_TOKENS_USERS.DELETE);

  const [state, send, service] = useMachine(UserTokensMachine, {
    actions: {
      onSaveSuccess: () => {},
      onDeleteSuccess: () => {}
    },
    guards: {
      canDelete: () => canDelete
    },
    devTools: true
  });

  const isLoading = state.matches('loading') || state.matches('delete');

  const {loadError, data} = state.context;

  const retry = () => send({type: 'RETRY'});

  return (
    <Page>
      <PageHeader>
        <PageTitle icon={faKey} {...MENU} />
      </PageHeader>
      <ContentBody className="nxrm-user-tokens">
        <NxTile>
          <NxTile.Content>
            <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
              {canUpdate ? (
                <UserTokensForm service={service} />
              ) : (
                <UserTokensReadOnly data={data} />
              )}
            </NxLoadWrapper>
          </NxTile.Content>
        </NxTile>
      </ContentBody>
    </Page>
  );
}
