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
  ValidationUtils,
  UseNexusTruststore
} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxCheckbox,
  NxErrorAlert,
  NxFormGroup,
  NxFormSelect,
  NxLoadWrapper,
  NxTooltip,
  NxTextInput,
  NxTextLink,
  NxStatefulForm,
  NxSuccessAlert
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

import Machine from './IqServerMachine';

import './IqServer.scss';

export default function IqServerForm() {
  const [state, send] = useMachine(Machine, {devTools: true});
  const {data, pristineData, isPristine, validationErrors, verifyConnectionError, verifyConnectionSuccessMessage} = state.context;
  const isInvalid = FormUtils.isInvalid(validationErrors);
  const canOpenIqServerDashboard = pristineData.enabled && ValidationUtils.isUrl(pristineData.url);

  function verifyConnection() {
    send({type: 'VERIFY_CONNECTION'});
  }

  function discard() {
    send({type: 'RESET'});
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
    send({type: 'DISMISS'});
  }

  function handleAuthTypeChange(value) {
    send({
      type: 'UPDATE',
      data: {
        authenticationType: value,
        username: '',
        password: ''
      }
    });
  }

  return <NxStatefulForm
      {...FormUtils.formProps(state, send)}
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
            {...FormUtils.checkboxProps('enabled', state)}
            onChange={FormUtils.handleUpdate('enabled', send)}>
          {UIStrings.IQ_SERVER.ENABLED.sublabel}
        </NxCheckbox>
      </NxFormGroup>
      <NxFormGroup {...UIStrings.IQ_SERVER.IQ_SERVER_URL} isRequired>
        <NxTextInput className="nx-text-input--long"
                     {...FormUtils.fieldProps('url', state)}
                     onChange={handleUrlChange}/>
      </NxFormGroup>
      <UseNexusTruststore
        remoteUrl={data.url}
        {...FormUtils.checkboxProps('useTrustStoreForUrl', state)}
        onChange={FormUtils.handleUpdate('useTrustStoreForUrl', send)}
      />
      <NxFormGroup label={UIStrings.IQ_SERVER.AUTHENTICATION_TYPE.label} isRequired>
        <NxFormSelect className="nx-form-select--long"
                {...FormUtils.fieldProps('authenticationType', state)}
                onChange={handleAuthTypeChange}
                validatable>
          <option value=""/>
          <option value="USER">{UIStrings.IQ_SERVER.AUTHENTICATION_TYPE.USER}</option>
          <option value="PKI">{UIStrings.IQ_SERVER.AUTHENTICATION_TYPE.PKI}</option>
        </NxFormSelect>
      </NxFormGroup>
      {data.authenticationType === 'USER' && <>
        <NxFormGroup {...UIStrings.IQ_SERVER.USERNAME} isRequired>
          <NxTextInput className="nx-text-input--long"
                       {...FormUtils.fieldProps('username', state)}
                       onChange={FormUtils.handleUpdate('username', send)}/>
        </NxFormGroup>
        <NxFormGroup {...UIStrings.IQ_SERVER.PASSWORD} isRequired>
          <NxTextInput className="nx-text-input--long"
                       type="password"
                       autoComplete="new-password"
                       {...FormUtils.fieldProps('password', state)}
                       onChange={FormUtils.handleUpdate('password', send)}/>
        </NxFormGroup>
      </>}
      <NxFormGroup {...UIStrings.IQ_SERVER.CONNECTION_TIMEOUT}>
        <NxTextInput className="nx-text-input--long"
                     {...FormUtils.fieldProps('timeoutSeconds', state)}
                     onChange={FormUtils.handleUpdate('timeoutSeconds', send)}/>
      </NxFormGroup>
      <NxFormGroup {...UIStrings.IQ_SERVER.PROPERTIES}>
        <NxTextInput className="nx-text-input--long"
                     type="textarea"
                     {...FormUtils.fieldProps('properties', state)}
                     onChange={FormUtils.handleUpdate('properties', send)}/>
      </NxFormGroup>
      <NxFormGroup label={UIStrings.IQ_SERVER.SHOW_LINK.label}>
        <NxCheckbox{...FormUtils.checkboxProps('showLink', state)}
                   onChange={FormUtils.handleUpdate('showLink', send)}>
          {UIStrings.IQ_SERVER.SHOW_LINK.sublabel}
        </NxCheckbox>
      </NxFormGroup>
      <div className="verify-connection-status">
        {!state.matches('loaded.idle') &&
        <NxLoadWrapper loading={state.matches('loaded.verifyingConnection')} retryHandler={()=>{}}>
          {state.matches('loaded.success') &&
          <NxSuccessAlert onClose={dismissValidationMessage}>
            {UIStrings.IQ_SERVER.VERIFY_CONNECTION_SUCCESSFUL(verifyConnectionSuccessMessage)}
          </NxSuccessAlert>}
          {state.matches('loaded.error') &&
          <NxErrorAlert onClose={dismissValidationMessage}>
            {UIStrings.IQ_SERVER.VERIFY_CONNECTION_ERROR(verifyConnectionError)}
          </NxErrorAlert>}
        </NxLoadWrapper>}
      </div>
    </>}
  </NxStatefulForm>;
}
