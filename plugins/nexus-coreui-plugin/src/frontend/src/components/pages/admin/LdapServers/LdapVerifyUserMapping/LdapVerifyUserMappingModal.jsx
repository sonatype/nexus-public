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
import {NxModal, NxFooter, NxButton, NxButtonBar} from '@sonatype/react-shared-components';
import UIStrings from '../../../../../constants/UIStrings';
import LdapVerifyUserMappingMachine from './LdapVerifyUserMappingMachine';
import LdapVerifyUserMappingItem from './LdapVerifyUserMappingItem';
import LdapVerifyUserMappingList from './LdapVerifyUserMappingList';

const {CLOSE} = UIStrings;

export default function LdapVerifyUserMappingModal({ldapConfig, onCancel}) {
  const [state, _, service] = useMachine(LdapVerifyUserMappingMachine, {
    context: {
      ldapConfig
    },
    devTools: true
  });

  const showingItem = state.matches('showingItem');

  return (
    <NxModal
      variant="wide"
      aria-labelledby="modal-form-header"
      className="nxrm-ldap-user-mapping-modal"
    >
      {showingItem ? (
        <LdapVerifyUserMappingItem service={service} />
      ) : (
        <LdapVerifyUserMappingList service={service} />
      )}
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={onCancel}>{CLOSE}</NxButton>
        </NxButtonBar>
      </NxFooter>
    </NxModal>
  );
}
