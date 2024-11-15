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
  Section,
  FormUtils,
} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxFormGroup,
  NxStatefulForm,
  NxTooltip,
  NxTextInput
} from '@sonatype/react-shared-components';

import PasswordChangeMachine from './PasswordChangeMachine';
import UIStrings from '../../../../constants/UIStrings';

export default function PasswordChangeForm({userId}) {
  const [current, send] = useMachine(PasswordChangeMachine, {devTools: true});
  const {isPristine} = current.context;

  function save() {
    send({type: 'SAVE', userId: userId});
  }

  function discard() {
    send({type: 'RESET'});
  }

  return <Section className="user-account-settings">
    <NxStatefulForm
        {...FormUtils.formProps(current, send)}
        onSubmit={save}
        submitBtnText={UIStrings.USER_ACCOUNT.ACTIONS.changePassword}
        additionalFooterBtns={
          <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
            <NxButton type="button" className={isPristine && 'disabled'} onClick={discard}>
              {UIStrings.USER_ACCOUNT.ACTIONS.discardChangePassword}
            </NxButton>
          </NxTooltip>
        }
    >
      <NxFormGroup label={UIStrings.USER_ACCOUNT.PASSWORD_CURRENT_FIELD_LABEL} isRequired>
        <NxTextInput
            {...FormUtils.fieldProps('passwordCurrent', current)}
            onChange={FormUtils.handleUpdate('passwordCurrent', send)}
            type="password"
        />
      </NxFormGroup>
      <NxFormGroup label={UIStrings.USER_ACCOUNT.PASSWORD_NEW_FIELD_LABEL} isRequired>
        <NxTextInput
            {...FormUtils.fieldProps('passwordNew', current)}
            onChange={FormUtils.handleUpdate('passwordNew', send)}
            type="password"
        />
      </NxFormGroup>
      <NxFormGroup label={UIStrings.USER_ACCOUNT.PASSWORD_NEW_CONFIRM_FIELD_LABEL} isRequired>
        <NxTextInput
            {...FormUtils.fieldProps('passwordNewConfirm', current)}
            onChange={FormUtils.handleUpdate('passwordNewConfirm', send)}
            type="password"
        />
      </NxFormGroup>
    </NxStatefulForm>
  </Section>;
}
