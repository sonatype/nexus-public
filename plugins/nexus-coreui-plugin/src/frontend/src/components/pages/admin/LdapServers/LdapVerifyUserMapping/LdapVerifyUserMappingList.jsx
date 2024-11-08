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
import {useActor} from '@xstate/react';
import {NxModal, NxH2, NxTable} from '@sonatype/react-shared-components';
import UIStrings from '../../../../../constants/UIStrings';

const {
  LDAP_SERVERS: {
    FORM: {
      MODAL_VERIFY_USER_MAPPING: {
        LIST: {
          HEADER,
          TABLE: {USER_ID, NAME, EMAIL, ROLES}
        }
      }
    }
  },
  EMPTY_MESSAGE
} = UIStrings;

export default function LdapVerifyUserMappingList({service}) {
  const [state, send] = useActor(service);

  const {data = [], error} = state.context;

  const isLoading = state.matches('loading');

  const showItem = (itemIndex) => send({type: 'SHOW_ITEM', itemIndex});
  const retryHandler = () => send({type: 'RETRY'});

  return (
    <>
      <NxModal.Header>
        <NxH2 id="modal-form-header">{HEADER}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxTable>
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell>{USER_ID}</NxTable.Cell>
              <NxTable.Cell>{NAME}</NxTable.Cell>
              <NxTable.Cell>{EMAIL}</NxTable.Cell>
              <NxTable.Cell>{ROLES}</NxTable.Cell>
              <NxTable.Cell chevron />
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body
            isLoading={isLoading}
            error={error}
            retryHandler={retryHandler}
            emptyMessage={EMPTY_MESSAGE}
          >
            {data.map(({username, realName, email, membership}, index) => (
              <NxTable.Row key={username} isClickable onClick={() => showItem(index)}>
                <NxTable.Cell>{username}</NxTable.Cell>
                <NxTable.Cell>{realName}</NxTable.Cell>
                <NxTable.Cell>{email}</NxTable.Cell>
                <NxTable.Cell>{membership}</NxTable.Cell>
                <NxTable.Cell chevron />
              </NxTable.Row>
            ))}
          </NxTable.Body>
        </NxTable>
      </NxModal.Content>
    </>
  );
}
