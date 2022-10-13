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
import {FormUtils, ValidationUtils, UseNexusTruststore} from '@sonatype/nexus-ui-plugin';
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
import {isEmpty} from 'ramda';

import UIStrings from '../../../../constants/UIStrings';

const {EMAIL_SERVER: {FORM: LABELS}} = UIStrings;

export default function EmailServerForm({ parentMachine }) {
  const [state, send] = parentMachine;
  const {
    data,
    isPristine,
    loadError,
    saveError,
    validationErrors,
  } = state.context;

  const isLoading = state.matches('loading');
  const isSaving = state.matches('saving');
  const isInvalid = ValidationUtils.isInvalid(validationErrors);

  const save = () => send('SAVE');
  const retry = () => send('RETRY');
  const discard = () => send('RESET');

  const remoteUrl = !isEmpty(data) ? `https://${data.host}:${data.port}` : '';

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
            {...FormUtils.checkboxProps('enabled', state)}
            onChange={FormUtils.handleUpdate('enabled', send)}
        >
          {LABELS.ENABLED.SUB_LABEL}
        </NxCheckbox>
      </NxFormGroup>
      <NxFormGroup label={LABELS.HOST.LABEL} isRequired>
        <NxTextInput
            className="nx-text-input--long"
            {...FormUtils.fieldProps('host', state)}
            onChange={FormUtils.handleUpdate('host', send)}
        />
      </NxFormGroup>
      <NxFormGroup label={LABELS.PORT.LABEL} isRequired>
        <NxTextInput
            className="nx-text-input--short"
            {...FormUtils.fieldProps('port', state)}
            onChange={FormUtils.handleUpdate('port', send)}
        />
      </NxFormGroup>
      <UseNexusTruststore
        remoteUrl={remoteUrl}
        {...FormUtils.checkboxProps('nexusTrustStoreEnabled', state)}
        onChange={FormUtils.handleUpdate('nexusTrustStoreEnabled', send)}
      />
      <NxFormRow>
        <NxFormGroup label={LABELS.USERNAME.LABEL}>
          <NxTextInput
              {...FormUtils.fieldProps('username', state)}
              onChange={FormUtils.handleUpdate('username', send)}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.PASSWORD.LABEL}>
          <NxTextInput
              type="password"
              autoComplete="new-password"
              {...FormUtils.fieldProps('password', state)}
              onChange={FormUtils.handleUpdate('password', send)}
          />
        </NxFormGroup>
      </NxFormRow>
      <NxFormGroup label={LABELS.FROM_ADDRESS.LABEL} isRequired>
        <NxTextInput
            className="nx-text-input--long"
            {...FormUtils.fieldProps('fromAddress', state)}
            onChange={FormUtils.handleUpdate('fromAddress', send)}
        />
      </NxFormGroup>
      <NxFormGroup label={LABELS.SUBJECT_PREFIX.LABEL}>
        <NxTextInput
            className="nx-text-input--long"
            {...FormUtils.fieldProps('subjectPrefix', state)}
            onChange={FormUtils.handleUpdate('subjectPrefix', send)}
        />
      </NxFormGroup>
      <NxFieldset label={LABELS.SSL_TLS_OPTIONS.LABEL}>
        <NxCheckbox
            {...FormUtils.checkboxProps('startTlsEnabled', state)}
            onChange={FormUtils.handleUpdate('startTlsEnabled', send)}
        >
          {LABELS.SSL_TLS_OPTIONS.OPTIONS.ENABLE_STARTTLS}
        </NxCheckbox>
        <NxCheckbox
            {...FormUtils.checkboxProps('startTlsRequired', state)}
            onChange={FormUtils.handleUpdate('startTlsRequired', send)}
        >
          {LABELS.SSL_TLS_OPTIONS.OPTIONS.REQUIRE_STARTTLS}
        </NxCheckbox>
        <NxCheckbox
            {...FormUtils.checkboxProps('sslOnConnectEnabled', state)}
            onChange={FormUtils.handleUpdate('sslOnConnectEnabled', send)}
        >
          {LABELS.SSL_TLS_OPTIONS.OPTIONS.ENABLE_SSL_TLS}
        </NxCheckbox>
        <NxCheckbox
            {...FormUtils.checkboxProps('sslServerIdentityCheckEnabled', state)}
            onChange={FormUtils.handleUpdate('sslServerIdentityCheckEnabled', send)}
        >
          {LABELS.SSL_TLS_OPTIONS.OPTIONS.IDENTITY_CHECK}
        </NxCheckbox>
      </NxFieldset>
    </NxTile.Content>
  </NxForm>;
}
