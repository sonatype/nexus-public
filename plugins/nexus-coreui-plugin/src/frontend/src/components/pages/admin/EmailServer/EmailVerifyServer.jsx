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
import {isEmpty} from 'ramda';
import {
  NxFormGroup,
  NxTextInput,
  NxFormRow,
  NxButton,
  NxSuccessAlert,
  NxErrorAlert,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';
import {FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
const {EMAIL_SERVER: LABELS} = UIStrings;

export default function EmailVerifyServer({actor}) {
  const [state, send] = useActor(actor);

  const {validationErrors, data, saveError, testResult, isTouched} =
    state.context;
  const isSaving = state.matches('saving');
  const isInvalid = FormUtils.isInvalid(validationErrors);
  const disabled = isInvalid || isSaving || ValidationUtils.isBlank(data.email);

  const onTest = () => send({type: 'SAVE'});

  const handleEnter = (event) => {
    if (event.key === 'Enter') {
      onTest();
    }
  };

  const success = isEmpty(isTouched) && testResult === true;
  const error = isEmpty(isTouched) && testResult === false;

  return (
    <>
      <NxFormRow>
        <>
          <NxFormGroup
            label={LABELS.VERIFY.LABEL}
            sublabel={LABELS.VERIFY.SUB_LABEL}
          >
            <NxTextInput
              {...FormUtils.fieldProps('email', state)}
              onChange={FormUtils.handleUpdate('email', send)}
              onKeyDown={handleEnter}
            />
          </NxFormGroup>
          <NxButton
            variant="primary"
            type="button"
            onClick={onTest}
            disabled={disabled}
          >
            {LABELS.VERIFY.TEST}
          </NxButton>
        </>
      </NxFormRow>
      <NxLoadWrapper
        loading={isSaving}
        loadError={saveError}
        retryHandler={onTest}
      >
        {success && <NxSuccessAlert>{LABELS.VERIFY.SUCCESS}</NxSuccessAlert>}
        {error && <NxErrorAlert>{LABELS.VERIFY.ERROR}</NxErrorAlert>}
      </NxLoadWrapper>
    </>
  );
}
