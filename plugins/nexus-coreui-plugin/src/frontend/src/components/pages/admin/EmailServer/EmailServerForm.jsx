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
import {FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';
import {
  NxForm,
  NxFormGroup,
  NxButton,
  NxH2,
  NxTextInput,
  NxFormRow,
  NxFieldset,
  NxTooltip,
  NxCheckbox,
  NxTile,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

const {EMAIL_SERVER: {FORM: LABELS}} = UIStrings;

export default function EmailServerForm({ parentMachine }) {
  const [current, send] = parentMachine;
  const {
    isPristine,
    loadError,
    saveError,
    validationErrors,
  } = current.context;

  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isInvalid = ValidationUtils.isInvalid(validationErrors);

  const save = () => send('SAVE');
  const retry = () => send('RETRY');
  const discard = () => send('RESET');

  return <NxForm
      loading={isLoading}
      loadError={loadError}
      doLoad={retry}
      onSubmit={save}
      submitError={saveError}
      submitMaskState={isSaving ? false : null}
      submitBtnText={UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
      submitMaskMessage={UIStrings.SAVING}
      validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}
      additionalFooterBtns={<>
        <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
          <NxButton type="button" className={isPristine && 'disabled'} onClick={discard}>
            {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
          </NxButton>
        </NxTooltip>
      </>}
  >
    <NxTile.Content>
      <NxH2>{LABELS.SECTIONS.SETUP}</NxH2>
      <NxFormGroup label={LABELS.ENABLED.LABEL}>
        <NxCheckbox
            {...FormUtils.checkboxProps('enabled', current)}
            onChange={FormUtils.handleUpdate('enabled', send)}
        >
          {LABELS.ENABLED.SUB_LABEL}
        </NxCheckbox>
      </NxFormGroup>
      <NxFormGroup label={LABELS.HOST.LABEL} isRequired>
        <NxTextInput
            className="nx-text-input--long"
            {...FormUtils.fieldProps('host', current)}
            onChange={FormUtils.handleUpdate('host', send)}
        />
      </NxFormGroup>
      <NxFormGroup label={LABELS.PORT.LABEL} isRequired>
        <NxTextInput
            className="nx-text-input--short"
            {...FormUtils.fieldProps('port', current)}
            onChange={FormUtils.handleUpdate('port', send)}
        />
      </NxFormGroup>
      <NxFormRow>
        <NxFormGroup label={LABELS.USERNAME.LABEL}>
          <NxTextInput
              {...FormUtils.fieldProps('username', current)}
              onChange={FormUtils.handleUpdate('username', send)}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.PASSWORD.LABEL}>
          <NxTextInput
              type="password"
              autoComplete="new-password"
              {...FormUtils.fieldProps('password', current)}
              onChange={FormUtils.handleUpdate('password', send)}
          />
        </NxFormGroup>
      </NxFormRow>
      <NxFormGroup label={LABELS.FROM_ADDRESS.LABEL} isRequired>
        <NxTextInput
            className="nx-text-input--long"
            {...FormUtils.fieldProps('fromAddress', current)}
            onChange={FormUtils.handleUpdate('fromAddress', send)}
        />
      </NxFormGroup>
      <NxFormGroup label={LABELS.SUBJECT_PREFIX.LABEL}>
        <NxTextInput
            className="nx-text-input--long"
            {...FormUtils.fieldProps('subjectPrefix', current)}
            onChange={FormUtils.handleUpdate('subjectPrefix', send)}
        />
      </NxFormGroup>
      <NxFieldset label={LABELS.SSL_TLS_OPTIONS.LABEL}>
        <NxCheckbox
            {...FormUtils.checkboxProps('startTlsEnabled', current)}
            onChange={FormUtils.handleUpdate('startTlsEnabled', send)}
        >
          {LABELS.SSL_TLS_OPTIONS.OPTIONS.ENABLE_STARTTLS}
        </NxCheckbox>
        <NxCheckbox
            {...FormUtils.checkboxProps('startTlsRequired', current)}
            onChange={FormUtils.handleUpdate('startTlsRequired', send)}
        >
          {LABELS.SSL_TLS_OPTIONS.OPTIONS.REQUIRE_STARTTLS}
        </NxCheckbox>
        <NxCheckbox
            {...FormUtils.checkboxProps('sslOnConnectEnabled', current)}
            onChange={FormUtils.handleUpdate('sslOnConnectEnabled', send)}
        >
          {LABELS.SSL_TLS_OPTIONS.OPTIONS.ENABLE_SSL_TLS}
        </NxCheckbox>
        <NxCheckbox
            {...FormUtils.checkboxProps('sslServerIdentityCheckEnabled', current)}
            onChange={FormUtils.handleUpdate('sslServerIdentityCheckEnabled', send)}
        >
          {LABELS.SSL_TLS_OPTIONS.OPTIONS.IDENTITY_CHECK}
        </NxCheckbox>
      </NxFieldset>
    </NxTile.Content>
  </NxForm>;
}
