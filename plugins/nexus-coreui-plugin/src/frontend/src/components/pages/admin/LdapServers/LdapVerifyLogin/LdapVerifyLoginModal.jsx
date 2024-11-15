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
  NxModal,
  NxTextInput,
  NxStatefulForm,
  NxFormGroup,
  NxH2,
  NxP,
  NxSuccessAlert
} from '@sonatype/react-shared-components';
import {FormUtils} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../../constants/UIStrings';
import LdapVerifyLoginMachine from './LdapVerifyLoginMachine.js';

const {
  LDAP_SERVERS: {
    FORM: {
      MODAL_VERIFY_LOGIN: {
        TITLE,
        DESCRIPTION,
        USERNAME,
        PASSWORD,
        TEST_CONNECTION_BUTTON,
        SUCCESS_MESSAGE,
        ERROR_MESSAGE
      }
    }
  }
} = UIStrings;

export default function LdapVerifyUserMappingModal({ldapConfig, onCancel}) {
  const [state, send] = useMachine(LdapVerifyLoginMachine, {
    context: {
      ldapConfig
    },
    devTools: true
  });

  const {success, error} = state.context;

  const {protocol, host, port} = ldapConfig;
  const serverUrl = `${protocol?.toLowerCase()}://${host}:${port}`;

  const verifyLogin = () => send({type: 'VERIFY'});

  return (
    <NxModal aria-labelledby="modal-form-header">
      <NxStatefulForm
        onSubmit={verifyLogin}
        onCancel={onCancel}
        submitBtnText={TEST_CONNECTION_BUTTON}
        submitMaskState={FormUtils.submitMaskState(state, ['verifying'])}
        submitError={error}
        submitErrorTitleMessage={ERROR_MESSAGE}
        validationErrors={FormUtils.getValidationErrorsMessage(state)}
      >
        <NxModal.Header>
          <NxH2 id="modal-form-header">{TITLE}</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxP>{DESCRIPTION}</NxP>
          <NxFormGroup label={USERNAME.LABEL} isRequired>
            <NxTextInput
              {...FormUtils.fieldProps('username', state)}
              onChange={FormUtils.handleUpdate('username', send)}
              placeholder={USERNAME.PLACEHOLDER}
            />
          </NxFormGroup>
          <NxFormGroup label={PASSWORD.LABEL} isRequired>
            <NxTextInput
              {...FormUtils.fieldProps('password', state)}
              onChange={FormUtils.handleUpdate('password', send)}
              type="password"
              placeholder={PASSWORD.PLACEHOLDER}
            />
          </NxFormGroup>
          {success && <NxSuccessAlert>{SUCCESS_MESSAGE + serverUrl}</NxSuccessAlert>}
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  );
}
