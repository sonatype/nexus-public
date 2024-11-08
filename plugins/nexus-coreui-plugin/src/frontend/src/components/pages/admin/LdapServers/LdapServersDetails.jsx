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

import React, {useRef} from 'react';
import {useMachine} from '@xstate/react';
import {Page, PageHeader, PageTitle} from '@sonatype/nexus-ui-plugin';
import {
  NxTile,
  NxTabs,
  NxTabList,
  NxTab,
  NxTabPanel,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';
import {faBook} from '@fortawesome/free-solid-svg-icons';

import LdapServersConfigurationForm from './LdapServersConfigurationForm';
import LdapServersUserAndGroupForm from './LdapServersUserAndGroupForm';
import LdapServersConfigurationFormReadOnly from './LdapServersConfigurationFormReadOnly';
import LdapServersUserAndGroupFormReadOnly from './LdapServersUserAndGroupFormReadOnly';
import {canUpdate} from './LdapServersHelper';
import Machine from './LdapServersDetailsMachine';

import UIStrings from '../../../../constants/UIStrings';

const {
  LDAP_SERVERS: {MENU, FORM},
} = UIStrings;
import {TABS_INDEX} from './LdapServersHelper';
import {isNil} from 'ramda';

export default function LdapServersDetails({itemId, onDone}) {
  const readOnly = !canUpdate();
  const isEdit = canUpdate() && Boolean(itemId);
  const isCreate = !isEdit && canUpdate();

  const ref = useRef();
  const [state, send] = useMachine(Machine, {
    devTools: true,
    context: {
      itemId: itemId ? decodeURIComponent(itemId) : null,
      isEdit,
    },
    actions: {
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone,
    },
  });
  const connection = state.matches('loaded.creatingConnection');
  const userAndGroup = state.matches('loaded.creatingUserAndGroup');
  const {activeTab} = state.context;

  const onTabSelectedEditMode = (value) => {
    if (TABS_INDEX.CREATE_CONNECTION === value) {
      send({type: 'CREATE_CONNECTION'});
    }

    if (TABS_INDEX.USER_AND_GROUP === value && !isNil(ref.current)) {
      // We use the submit event instead of xstate events to be able use the same validation defined for the form.
      const submitButton = ref.current.querySelector('.nx-form__submit-btn');
      submitButton.click();
    }
  };

  const onTabSelectedReadOnlyMode = (value) => {
    if (TABS_INDEX.CREATE_CONNECTION === value) {
      send({type: 'CREATE_CONNECTION'});
    }

    if (TABS_INDEX.USER_AND_GROUP === value) {
      send({type: 'NEXT'});
    }
  };

  const onTabSelected = (value) => {
    if (isEdit) {
      onTabSelectedEditMode(value);
    }

    if (readOnly) {
      onTabSelectedReadOnlyMode(value);
    }
  };

  const retryHandler = () => send({type: 'RETRY'});
  const isLoading = state.matches('loading');
  const hasLoadError = state.matches('loadError');

  const configurationForm = () =>
    connection && (
      <LdapServersConfigurationForm
        actor={state.context.createConnection}
        onDone={onDone}
        ref={ref}
      />
    );

  const userAndGroupForm = () =>
    userAndGroup && (
      <LdapServersUserAndGroupForm
        actor={state.context.userAndGroup}
        onDone={onDone}
      />
    );

  return (
    <Page className="nxrm-ldap-servers">
      <PageHeader>
        <PageTitle icon={faBook} {...MENU} />
      </PageHeader>
      <NxTile>
        <NxLoadWrapper
          retryHandler={retryHandler}
          loading={isLoading}
          error={hasLoadError && error}
        >
          {(isEdit || readOnly) && (
            <NxTabs activeTab={activeTab} onTabSelect={onTabSelected}>
              <NxTabList>
                <NxTab>{FORM.TABS.CONNECTION}</NxTab>
                <NxTab>{FORM.TABS.USER_AND_GROUP}</NxTab>
              </NxTabList>
              <NxTabPanel>
                {isEdit && configurationForm()}
                {readOnly && (
                  <LdapServersConfigurationFormReadOnly
                    actor={state.context.createConnection}
                  />
                )}
              </NxTabPanel>
              <NxTabPanel>
                {isEdit && userAndGroupForm()}
                {readOnly && (
                  <LdapServersUserAndGroupFormReadOnly
                    actor={state.context.userAndGroup}
                  />
                )}
              </NxTabPanel>
            </NxTabs>
          )}
          {isCreate && (
            <>
              {configurationForm()}
              {userAndGroupForm()}
            </>
          )}
        </NxLoadWrapper>
      </NxTile>
    </Page>
  );
}
