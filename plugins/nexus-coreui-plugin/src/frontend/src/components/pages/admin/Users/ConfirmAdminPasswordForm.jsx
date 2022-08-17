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
  NxForm,
  NxFormGroup,
  NxTextInput,
  NxModal,
  NxH2,
  NxP,
} from '@sonatype/react-shared-components';
import {FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {MODAL} = UIStrings.USERS;

export default function ConfirmAdminPasswordForm({actor}) {
  const [state, send] = useActor(actor);
  const {isPristine, loadError, saveError, validationErrors} = state.context;
  const isLoading = state.matches('loading');
  const isSaving = state.matches('saving');
  const isInvalid = ValidationUtils.isInvalid(validationErrors);

  const next = () =>
    send({
      type: 'SAVE',
    });

  const cancel = () =>
    send({
      type: 'CANCEL',
    });

  return (
    <NxForm
      loading={isLoading}
      loadError={loadError}
      submitBtnText={MODAL.NEXT}
      onSubmit={next}
      submitError={saveError}
      submitMaskState={isSaving ? false : null}
      submitMaskMessage={MODAL.NEXT}
      onCancel={cancel}
      validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}
    >
      <NxModal.Header>
        <NxH2 id="modal-form-header">{MODAL.CHANGE_PASSWORD}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxP>{MODAL.TEXT}</NxP>
        <NxFormGroup label={MODAL.ADMIN_PASSWORD} isRequired>
          <NxTextInput
            {...FormUtils.fieldProps('adminPassword', state)}
            onChange={FormUtils.handleUpdate('adminPassword', send)}
            type="password"
            data-testid="adminPassword"
          />
        </NxFormGroup>
      </NxModal.Content>
    </NxForm>
  );
}
