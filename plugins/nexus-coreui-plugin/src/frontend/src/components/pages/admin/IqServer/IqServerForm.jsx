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
  FormUtils,
  Select,
  ValidationUtils,
  SslCertificateDetailsModal
} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxCheckbox,
  NxErrorAlert,
  NxForm,
  NxFormGroup,
  NxLoadWrapper,
  NxTooltip,
  NxTextInput,
  NxTextLink,
  NxSuccessAlert,
  NxFormRow,
  NxFontAwesomeIcon
} from '@sonatype/react-shared-components';
import { faCertificate } from '@fortawesome/free-solid-svg-icons';

import UIStrings from '../../../../constants/UIStrings';

import Machine from './IqServerMachine';

import './IqServer.scss';

export default function IqServerForm() {
  const [current, send] = useMachine(Machine, {devTools: true});
  const {data, pristineData, isPristine, loadError, saveError, validationErrors, verifyConnectionError, verifyConnectionSuccessMessage} = current.context;
  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving')
  const isInvalid = FormUtils.isInvalid(validationErrors);
  const viewingCertificate = current.matches('loaded.viewingCertificate');
  const canOpenIqServerDashboard = pristineData.enabled && ValidationUtils.isUrl(pristineData.url);
  const hasSecureUrl = ValidationUtils.isSecureUrl(data.url);

  function verifyConnection() {
    send('VERIFY_CONNECTION');
  }

  function discard() {
    send('RESET');
  }

  function save() {
    send('SAVE');
  }

  function retry() {
    send('RETRY');
  }

  function handleUrlChange(url) {
    send({
      type: 'UPDATE_URL',
      data: {
        url
      }
    });
  }

  function dismissValidationMessage() {
    send('DISMISS');
  }

  function handleAuthTypeChange(event) {
    send({
      type: 'UPDATE',
      data: {
        authenticationType: event.currentTarget.value,
        username: '',
        password: ''
      }
    });
  }

  function viewCertificate() {
    send('VIEW_CERTIFICATE');
  }

  function closeCertificate() {
    send('CLOSE_CERTIFICATE');
  }

  return <NxForm
      loading={isLoading}
      loadError={loadError}
      doLoad={retry}
      onSubmit={save}
      submitError={saveError}
      submitMaskState={isSaving ? false : null}
      submitBtnText={UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
      validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}
      additionalFooterBtns={<>
        <NxButton type="button" variant="tertiary" disabled={isInvalid} onClick={verifyConnection}>
          {UIStrings.IQ_SERVER.VERIFY_CONNECTION_BUTTON_LABEL}
        </NxButton>
        <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
          <NxButton type="button" className={isPristine && 'disabled'} onClick={discard}>
            {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
          </NxButton>
        </NxTooltip>
      </>}>
    {() => <>
      {canOpenIqServerDashboard &&
      <NxTextLink className="open-dashboard-link" external href={pristineData.url}>
        {UIStrings.IQ_SERVER.OPEN_DASHBOARD}
      </NxTextLink>}
      <p>
        <NxTextLink external href="http://www.sonatype.com/nexus/product-overview/nexus-lifecycle">
          {UIStrings.IQ_SERVER.MENU.text}
        </NxTextLink>
        {' '}{UIStrings.IQ_SERVER.FORM_NOTES}
      </p>
      <div className="nx-sub-label help-text">{UIStrings.IQ_SERVER.HELP_TEXT}</div>
      <NxFormGroup label={UIStrings.IQ_SERVER.ENABLED.label} isRequired>
        <NxCheckbox
            {...FormUtils.checkboxProps('enabled', current)}
            onChange={FormUtils.handleUpdate('enabled', send)}>
          {UIStrings.IQ_SERVER.ENABLED.sublabel}
        </NxCheckbox>
      </NxFormGroup>
      <NxFormGroup {...UIStrings.IQ_SERVER.IQ_SERVER_URL} isRequired>
        <NxTextInput className="nx-text-input--long"
                     {...FormUtils.fieldProps('url', current)}
                     onChange={handleUrlChange}/>
      </NxFormGroup>
      <NxFormRow>
        <>
          <NxFormGroup label={UIStrings.IQ_SERVER.TRUST_STORE.label} isRequired>
            <NxCheckbox
              {...FormUtils.checkboxProps('useTrustStoreForUrl', current)}
              onChange={FormUtils.handleUpdate('useTrustStoreForUrl', send)}
              disabled={!hasSecureUrl}>
                {UIStrings.IQ_SERVER.TRUST_STORE.sublabel}
            </NxCheckbox>
          </NxFormGroup>
          <NxButton variant="tertiary" disabled={!hasSecureUrl} onClick={viewCertificate} type="button">
            <NxFontAwesomeIcon icon={faCertificate}/>
            <span>{UIStrings.IQ_SERVER.CERTIFICATE}</span>
          </NxButton>
        </>
      </NxFormRow>
      {viewingCertificate &&
            <SslCertificateDetailsModal remoteUrl={data.url} onCancel={closeCertificate}/>}
      <NxFormGroup label={UIStrings.IQ_SERVER.AUTHENTICATION_TYPE.label} isRequired>
        <Select className="nx-form-select--long"
                {...FormUtils.fieldProps('authenticationType', current)}
                onChange={handleAuthTypeChange}>
          <option value=""/>
          <option value="USER">{UIStrings.IQ_SERVER.AUTHENTICATION_TYPE.USER}</option>
          <option value="PKI">{UIStrings.IQ_SERVER.AUTHENTICATION_TYPE.PKI}</option>
        </Select>
      </NxFormGroup>
      {data.authenticationType === 'USER' && <>
        <NxFormGroup {...UIStrings.IQ_SERVER.USERNAME} isRequired>
          <NxTextInput className="nx-text-input--long"
                       {...FormUtils.fieldProps('username', current)}
                       onChange={FormUtils.handleUpdate('username', send)}/>
        </NxFormGroup>
        <NxFormGroup {...UIStrings.IQ_SERVER.PASSWORD} isRequired>
          <NxTextInput className="nx-text-input--long"
                       type="password"
                       autoComplete="new-password"
                       {...FormUtils.fieldProps('password', current)}
                       onChange={FormUtils.handleUpdate('password', send)}/>
        </NxFormGroup>
      </>}
      <NxFormGroup {...UIStrings.IQ_SERVER.CONNECTION_TIMEOUT}>
        <NxTextInput className="nx-text-input--long"
                     {...FormUtils.fieldProps('timeoutSeconds', current)}
                     onChange={FormUtils.handleUpdate('timeoutSeconds', send)}/>
      </NxFormGroup>
      <NxFormGroup {...UIStrings.IQ_SERVER.PROPERTIES}>
        <NxTextInput className="nx-text-input--long"
                     type="textarea"
                     {...FormUtils.fieldProps('properties', current)}
                     onChange={FormUtils.handleUpdate('properties', send)}/>
      </NxFormGroup>
      <NxFormGroup label={UIStrings.IQ_SERVER.SHOW_LINK.label} isRequired>
        <NxCheckbox{...FormUtils.checkboxProps('showLink', current)}
                   onChange={FormUtils.handleUpdate('showLink', send)}>
          {UIStrings.IQ_SERVER.SHOW_LINK.sublabel}
        </NxCheckbox>
      </NxFormGroup>
      <div className="verify-connection-status">
        {!current.matches('loaded.idle') &&
        <NxLoadWrapper loading={current.matches('loaded.verifyingConnection')} retryHandler={()=>{}}>
          {current.matches('loaded.success') &&
          <NxSuccessAlert onClose={dismissValidationMessage}>
            {UIStrings.IQ_SERVER.VERIFY_CONNECTION_SUCCESSFUL(verifyConnectionSuccessMessage)}
          </NxSuccessAlert>}
          {current.matches('loaded.error') &&
          <NxErrorAlert onClose={dismissValidationMessage}>
            {UIStrings.IQ_SERVER.VERIFY_CONNECTION_ERROR(verifyConnectionError)}
          </NxErrorAlert>}
        </NxLoadWrapper>}
      </div>
    </>}
  </NxForm>;
}
