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
import {useService} from '@xstate/react';

import {
  Section,
  FormUtils,
} from '@sonatype/nexus-ui-plugin';
import {
  NxForm,
  NxButton,
  NxTextInput,
  NxTooltip,
  NxFormGroup,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

export default function UserAccountSettings({service}) {
  const [current, send] = useService(service);
  const {data, isPristine, loadError, saveError, validationErrors} = current.context;

  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isInvalid = FormUtils.isInvalid(validationErrors);
  const readOnly = data?.external;

  function save() {
    if (!readOnly) {
      send('SAVE');
    }
  }

  function discard() {
    if (!readOnly) {
      send('RESET');
    }
  }

  function retry() {
    send('RETRY');
  }

  return <Section className="user-account-settings">
    <NxForm
        loading={isLoading}
        loadError={loadError}
        doLoad={retry}
        onSubmit={save}
        submitError={saveError}
        submitMaskState={isSaving ? false : null}
        submitBtnText={UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
        submitBtnClasses={readOnly ? 'disabled' : null}
        submitMaskMessage={UIStrings.SAVING}
        validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}
        additionalFooterBtns={
          <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
            <NxButton type="button" className={(readOnly || isPristine) && 'disabled'} onClick={discard}>
              {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
            </NxButton>
          </NxTooltip>
        }
    >
      <NxFormGroup label={UIStrings.USER_ACCOUNT.ID_FIELD_LABEL} isRequired>
        <NxTextInput
            {...FormUtils.fieldProps('userId', current)}
            disabled
        />
      </NxFormGroup>
      <NxFormGroup label={UIStrings.USER_ACCOUNT.FIRST_FIELD_LABEL} isRequired>
        <NxTextInput
            {...FormUtils.fieldProps('firstName', current)}
            onChange={FormUtils.handleUpdate('firstName', send)}
            disabled={readOnly}
        />
      </NxFormGroup>
      <NxFormGroup label={UIStrings.USER_ACCOUNT.LAST_FIELD_LABEL} isRequired>
        <NxTextInput
            {...FormUtils.fieldProps('lastName', current)}
            onChange={FormUtils.handleUpdate('lastName', send)}
            disabled={readOnly}
        />
      </NxFormGroup>
      <NxFormGroup label={UIStrings.USER_ACCOUNT.EMAIL_FIELD_LABEL} isRequired>
        <NxTextInput
            {...FormUtils.fieldProps('email', current)}
            onChange={FormUtils.handleUpdate('email', send)}
            disabled={readOnly}
        />
      </NxFormGroup>
    </NxForm>
  </Section>;
}
