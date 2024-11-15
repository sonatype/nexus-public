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
import PasswordChangeForm from './PasswordChangeForm';
import {faUser} from '@fortawesome/free-solid-svg-icons';
import {ContentBody, FormUtils, Page, PageHeader, PageTitle} from '@sonatype/nexus-ui-plugin';

import './UserAccount.scss';
import UserAccountMachine from './UserAccountMachine';
import UIStrings from '../../../../constants/UIStrings';
import {
  NxButton,
  NxFormGroup,
  NxStatefulForm,
  NxTextInput,
  NxTile,
  NxTooltip
} from '@sonatype/react-shared-components';

export default function UserAccount() {
  const [state, send] = useMachine(UserAccountMachine, {devTools: true});
  const {isPristine} = state.context;
  const isLoading = state.matches('loading');
  const isExternal = !isLoading && state.context.data.external === true;
  const isInternal = !isLoading && state.context.data.external === false;
  const userId = state.context.data?.userId;

  function discard() {
    send({type: 'RESET'});
  }

  return (
      <Page>
        <PageHeader><PageTitle icon={faUser} {...UIStrings.USER_ACCOUNT.MENU}/></PageHeader>
        <ContentBody className="nxrm-user-account">
          <NxTile className="user-account-settings">
            <NxStatefulForm
                {...FormUtils.formProps(state, send)}
                additionalFooterBtns={
                  <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
                    <NxButton type="button" className={(isExternal || isPristine) && 'disabled'} onClick={discard}>
                      {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
                    </NxButton>
                  </NxTooltip>
                }>
              <NxFormGroup label={UIStrings.USER_ACCOUNT.ID_FIELD_LABEL} isRequired>
                <NxTextInput
                    {...FormUtils.fieldProps('userId', state)}
                    disabled
                />
              </NxFormGroup>
              <NxFormGroup label={UIStrings.USER_ACCOUNT.FIRST_FIELD_LABEL} isRequired>
                <NxTextInput
                    {...FormUtils.fieldProps('firstName', state)}
                    onChange={FormUtils.handleUpdate('firstName', send)}
                    disabled={isExternal}
                />
              </NxFormGroup>
              <NxFormGroup label={UIStrings.USER_ACCOUNT.LAST_FIELD_LABEL} isRequired>
                <NxTextInput
                    {...FormUtils.fieldProps('lastName', state)}
                    onChange={FormUtils.handleUpdate('lastName', send)}
                    disabled={isExternal}
                />
              </NxFormGroup>
              <NxFormGroup label={UIStrings.USER_ACCOUNT.EMAIL_FIELD_LABEL} isRequired>
                <NxTextInput
                    {...FormUtils.fieldProps('email', state)}
                    onChange={FormUtils.handleUpdate('email', send)}
                    disabled={isExternal}
                />
              </NxFormGroup>
            </NxStatefulForm>
          </NxTile>

          {isInternal && <PasswordChangeForm userId={userId}/>}
        </ContentBody>
      </Page>
  );
}


