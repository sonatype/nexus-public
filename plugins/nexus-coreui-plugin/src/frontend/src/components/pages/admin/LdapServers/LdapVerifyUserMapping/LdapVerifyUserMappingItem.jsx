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
import {NxModal, NxH2} from '@sonatype/react-shared-components';
import {ReadOnlyField} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../../constants/UIStrings';
import BackButton from './BackButton/BackButton';

const {
  LDAP_SERVERS: {
    FORM: {
      MODAL_VERIFY_USER_MAPPING: {
        ITEM: {
          BACK_BUTTON,
          LABELS: {USER_ID, NAME, EMAIL, ROLES}
        }
      }
    }
  }
} = UIStrings;

export default function LdapVerifyUserMappingItem({service}) {
  const [state, send] = useActor(service);

  const backToList = () => send({type: 'SHOW_LIST'});

  const {item} = state.context;

  const {username, realName, email, membership} = item;

  return (
    <>
      <NxModal.Header>
        <NxH2 id="modal-form-header">{realName}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <BackButton onClick={backToList} text={BACK_BUTTON} />
        <ReadOnlyField label={USER_ID} value={username} />
        <ReadOnlyField label={NAME} value={realName} />
        <ReadOnlyField label={EMAIL} value={email} />
        <ReadOnlyField label={ROLES} value={membership.join(', ')} />
      </NxModal.Content>
    </>
  );
}
