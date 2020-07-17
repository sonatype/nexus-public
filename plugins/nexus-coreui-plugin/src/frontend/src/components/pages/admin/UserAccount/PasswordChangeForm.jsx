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
import PasswordChangeMachine from './PasswordChangeMachine';
import {Button, FieldWrapper, SectionFooter, Section, Textfield, Utils} from 'nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

export default function PasswordChangeForm({userId}) {
  const [current, send] = useMachine(PasswordChangeMachine, {devTools: true});
  const {data, isPristine, validationErrors} = current.context;
  const {passwordCurrent, passwordNew, passwordNewConfirm} = data;
  const isInvalid = Utils.isInvalid(validationErrors);
  if (Object.keys(data).length === 0) {
    console.trace();
  }

  function update(event) {
    send('UPDATE', {data: {[event.target.name]: event.target.value}});
  }

  function handlePasswordSubmit() {
    send({type: 'SAVE', userId: userId});
  }

  function handlePasswordDiscard() {
    send('RESET');
  }

  return <Section>
    <FieldWrapper labelText={UIStrings.USER_ACCOUNT.PASSWORD_CURRENT_FIELD_LABEL}>
      <Textfield
          name='passwordCurrent'
          type='password'
          value={passwordCurrent}
          onChange={update}
          validationErrors={validationErrors.passwordCurrent}
      />
    </FieldWrapper>
    <FieldWrapper labelText={UIStrings.USER_ACCOUNT.PASSWORD_NEW_FIELD_LABEL}>
      <Textfield
          name='passwordNew'
          type='password'
          value={passwordNew}
          validationErrors={validationErrors.passwordNew}
          onChange={update}
      />
    </FieldWrapper>
    <FieldWrapper labelText={UIStrings.USER_ACCOUNT.PASSWORD_NEW_CONFIRM_FIELD_LABEL}>
      <Textfield
          name='passwordNewConfirm'
          type='password'
          value={passwordNewConfirm}
          validationErrors={validationErrors.passwordNewConfirm}
          onChange={update}
      />
    </FieldWrapper>
    <SectionFooter>
      <Button
          variant='primary'
          disabled={isInvalid}
          onClick={handlePasswordSubmit}
      >
        {UIStrings.USER_ACCOUNT.ACTIONS.changePassword}
      </Button>
      <Button
          disabled={isPristine}
          onClick={handlePasswordDiscard}
      >
        {UIStrings.USER_ACCOUNT.ACTIONS.discardChangePassword}
      </Button>
    </SectionFooter>
  </Section>;
}
