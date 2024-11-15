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

import {
  NxH2,
  NxFormGroup,
  NxModal,
  NxStatefulForm,
  NxStatefulWarningAlert,
  NxTextInput
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

const {
  USER_TOKEN_CONFIGURATION: {RESET_CONFIRMATION}
} = UIStrings;

export default function UserTokensResetModal({service}) {
  const [state, send] = useActor(service);

  const {confirmationString, confirmationStringValidationError} = state.context;

  const resetUserTokens = () => send({type: 'DELETE'});
  const cancelResetConfirmation = () => send({type: 'CANCEL_RESET_CONFIRMATION'});
  const setConfirmationString = (value) => send({type: 'SET_CONFIRMATION_STRING', value});

  return (
    <NxModal onCancel={cancelResetConfirmation} variant="narrow">
      <NxModal.Header>
        <NxH2>{RESET_CONFIRMATION.CAPTION}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxStatefulWarningAlert>{RESET_CONFIRMATION.WARNING}</NxStatefulWarningAlert>
        <NxStatefulForm
          onSubmit={resetUserTokens}
          submitBtnText={RESET_CONFIRMATION.BUTTON}
          onCancel={cancelResetConfirmation}
          validationErrors={confirmationStringValidationError}
        >
          <NxFormGroup
            label={RESET_CONFIRMATION.LABEL}
            sublabel={RESET_CONFIRMATION.SUBLABEL}
            isRequired
          >
            <NxTextInput
              id="confirmationString"
              name="confirmationString"
              value={confirmationString}
              onChange={setConfirmationString}
              validationErrors={confirmationStringValidationError}
              validatable
              isPristine={confirmationString === ''}
            />
          </NxFormGroup>
        </NxStatefulForm>
      </NxModal.Content>
    </NxModal>
  );
}
