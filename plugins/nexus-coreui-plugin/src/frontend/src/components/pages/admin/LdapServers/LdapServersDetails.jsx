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
import {Page, PageHeader, PageTitle} from '@sonatype/nexus-ui-plugin';
import {NxTile} from '@sonatype/react-shared-components';
import {faBook} from '@fortawesome/free-solid-svg-icons';

import LdapServerConfigurationForm from './LdapServerConfigurationForm';
import LdapServerUserAndGroupForm from './LdapServerUserAndGroupForm';

import Machine from './LdapServersDetailsMachine';

import UIStrings from '../../../../constants/UIStrings';

const {
  LDAP_SERVERS: {MENU},
} = UIStrings;

export default function LdapServersDetails({onDone}) {
  const [state, send] = useMachine(Machine, {
    devTools: true,
    actions: {
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone,
    },
  });
  const userAndGroup = state.matches('loaded.creatingUserAndGroup');

  return (
    <Page className="nxrm-ldap-servers">
      <PageHeader>
        <PageTitle icon={faBook} {...MENU} />
      </PageHeader>
      <NxTile>
        {userAndGroup ? (
          <LdapServerUserAndGroupForm
            actor={state.context.userAndGroup}
            onDone={onDone}
          />
        ) : (
          <LdapServerConfigurationForm
            parentState={state}
            parentSend={send}
            onDone={onDone}
          />
        )}
      </NxTile>
    </Page>
  );
}
