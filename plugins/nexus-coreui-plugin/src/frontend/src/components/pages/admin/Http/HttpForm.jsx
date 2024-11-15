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
  NxStatefulAccordion,
  NxAccordion,
  NxButton,
  NxCheckbox,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxFormRow,
  NxH2,
  NxList,
  NxStatefulForm,
  NxTextInput,
  NxTile,
  NxTooltip
} from '@sonatype/react-shared-components';
import {FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';
import {faTrashAlt, faPlusCircle} from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../constants/UIStrings';

const {CONFIGURATION: LABELS} = UIStrings.HTTP;

import HttpMachine from './HttpMachine';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

export default function HttpForm() {
  const [current, send] = useMachine(HttpMachine, {devTools: true});
  const {
    data: {
      nonProxyHosts,
      httpsEnabled,
      httpEnabled,
      httpAuthEnabled,
      httpsAuthEnabled,
      httpHost,
      httpsHost
    },
    isPristine,
    nonProxyHost = ''
  } = current.context;

  const list = nonProxyHosts || [];

  const isProxyEnabled = httpEnabled || httpsEnabled;
  const isHttpAuthEnabled =  httpEnabled && httpAuthEnabled;
  const isHttpsAuthEnabled =  httpsEnabled && httpsAuthEnabled;

  const discard = () => send({type: 'RESET'});

  const removeNonProxyHost = (index) =>
    send({type: 'REMOVE_NON_PROXY_HOST', index});

  const addNonProxyHost = () => send({type: 'ADD_NON_PROXY_HOST'});

  const toggleAuthentication = (name, value) =>
    send({type: 'TOGGLE_AUTHENTICATION', name, value});

  const handleHttpCheckbox = () => send({type: 'TOGGLE_HTTP_PROXY'});

  const handleHttpsCheckbox = () => send({type: 'TOGGLE_HTTPS_PROXY'});

  const handleEnter = (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      addNonProxyHost();
    }
  };

  const handleSetNonProxyHost = (value) => send({type: 'SET_NON_PROXY_HOST', value});

  const validateNonProxyHost = () =>
    ValidationUtils.validateWhiteSpace(nonProxyHost) || 
      (nonProxyHosts.includes(nonProxyHost) ? LABELS.EXCLUDE.ALREADY_ADDED : null)

  const timeoutPlaceholder = ExtJS.state().getValue('requestTimeout');
  const retryCountPlaceholder = ExtJS.state().getValue('retryCount');

  return (
    <NxStatefulForm
      {...FormUtils.formProps(current, send)}
      additionalFooterBtns={
        <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
          <NxButton
            type="button"
            className={isPristine && 'disabled'}
            onClick={discard}
          >
            {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
          </NxButton>
        </NxTooltip>
      }
    >
      <NxFormGroup
        label={LABELS.USER_AGENT.LABEL}
        sublabel={LABELS.USER_AGENT.SUB_LABEL}
      >
        <NxTextInput
          className="nx-text-input--long"
          {...FormUtils.fieldProps('userAgentSuffix', current)}
          onChange={FormUtils.handleUpdate('userAgentSuffix', send)}
        />
      </NxFormGroup>
      <NxFormGroup
        label={LABELS.TIMEOUT.LABEL}
        sublabel={LABELS.TIMEOUT.SUB_LABEL}
      >
        <NxTextInput
          className="nx-text-input--short"
          {...FormUtils.fieldProps('timeout', current)}
          onChange={FormUtils.handleUpdate('timeout', send)}
          placeholder={timeoutPlaceholder.value}
        />
      </NxFormGroup>
      <NxFormGroup
        label={LABELS.ATTEMPTS.LABEL}
        sublabel={LABELS.ATTEMPTS.SUB_LABEL}
      >
        <NxTextInput
          className="nx-text-input--short"
          {...FormUtils.fieldProps('retries', current)}
          onChange={FormUtils.handleUpdate('retries', send)}
          placeholder={retryCountPlaceholder}
        />
      </NxFormGroup>
      <NxH2>{LABELS.PROXY.LABEL}</NxH2>
      <NxFieldset label="">
        <NxCheckbox
          {...FormUtils.checkboxProps('httpEnabled', current)}
          onChange={handleHttpCheckbox}
        >
          {LABELS.PROXY.HTTP_CHECKBOX}
        </NxCheckbox>
      </NxFieldset>
      <NxFormGroup
        label={LABELS.PROXY.HTTP_HOST}
        sublabel={LABELS.PROXY.SUB_LABEL}
        isRequired={httpEnabled}
      >
        <NxTextInput
          className="nx-text-input--long"
          {...FormUtils.fieldProps('httpHost', current)}
          onChange={FormUtils.handleUpdate('httpHost', send)}
          disabled={!httpEnabled}
        />
      </NxFormGroup>
      <NxFormGroup 
        label={LABELS.PROXY.HTTP_PORT} 
        isRequired={!!httpHost}
      >
        <NxTextInput
          className="nx-text-input--short"
          {...FormUtils.fieldProps('httpPort', current)}
          onChange={FormUtils.handleUpdate('httpPort', send)}
          disabled={!httpEnabled || !httpHost}
        />
      </NxFormGroup>
      <NxTile.Content className="nx-tile-content--accordion-container">
        <NxStatefulAccordion 
          title="http-authentication"
          defaultOpen={httpAuthEnabled}
        >
          <NxAccordion.Header>
            <NxAccordion.Title>
              {LABELS.PROXY.HTTP_AUTHENTICATION}
            </NxAccordion.Title>
          </NxAccordion.Header>
          <NxFieldset label="">
            <NxCheckbox
              {...FormUtils.checkboxProps('httpAuthEnabled', current)}
              onChange={() => toggleAuthentication('httpAuthEnabled')}
              disabled={!httpEnabled}
            >
              {LABELS.PROXY.HTTP_AUTH_CHECKBOX}
            </NxCheckbox>
          </NxFieldset>
          <NxFormRow>
            <>
              <NxFormGroup 
                label={LABELS.PROXY.USERNAME} 
                isRequired={httpAuthEnabled}
              >
                <NxTextInput
                  className="nx-text-input"
                  {...FormUtils.fieldProps('httpAuthUsername', current)}
                  onChange={FormUtils.handleUpdate('httpAuthUsername', send)}
                  disabled={!isHttpAuthEnabled}
                />
              </NxFormGroup>
              <NxFormGroup label={LABELS.PROXY.PASSWORD}>
                <NxTextInput
                  className="nx-text-input"
                  {...FormUtils.fieldProps('httpAuthPassword', current)}
                  onChange={FormUtils.handleUpdate('httpAuthPassword', send)}
                  type="password"
                  disabled={!isHttpAuthEnabled}
                />
              </NxFormGroup>
            </>
          </NxFormRow>
          <NxFormGroup label={LABELS.PROXY.HOST_NAME}>
            <NxTextInput
              className="nx-text-input--long"
              {...FormUtils.fieldProps('httpAuthNtlmHost', current)}
              onChange={FormUtils.handleUpdate('httpAuthNtlmHost', send)}
              disabled={!isHttpAuthEnabled}
            />
          </NxFormGroup>
          <NxFormGroup label={LABELS.PROXY.DOMAIN}>
            <NxTextInput
              className="nx-text-input--long"
              {...FormUtils.fieldProps('httpAuthNtlmDomain', current)}
              onChange={FormUtils.handleUpdate('httpAuthNtlmDomain', send)}
              disabled={!isHttpAuthEnabled}
            />
          </NxFormGroup>
        </NxStatefulAccordion>
      </NxTile.Content>
      <NxFieldset label="">
        <NxCheckbox
          {...FormUtils.checkboxProps('httpsEnabled', current)}
          onChange={handleHttpsCheckbox}
        >
          {LABELS.PROXY.HTTPS_CHECKBOX}
        </NxCheckbox>
      </NxFieldset>
      <NxFormGroup
        label={LABELS.PROXY.HTTPS_HOST}
        sublabel={LABELS.PROXY.SUB_LABEL}
        isRequired={httpsEnabled}
      >
        <NxTextInput
          className="nx-text-input--long"
          {...FormUtils.fieldProps('httpsHost', current)}
          onChange={FormUtils.handleUpdate('httpsHost', send)}
          disabled={!httpsEnabled}
        />
      </NxFormGroup>
      <NxFormGroup 
        label={LABELS.PROXY.HTTPS_PORT} 
        isRequired={!!httpsHost}
      >
        <NxTextInput
          className="nx-text-input--short"
          {...FormUtils.fieldProps('httpsPort', current)}
          onChange={FormUtils.handleUpdate('httpsPort', send)}
          disabled={!httpsEnabled || !httpsHost}
        />
      </NxFormGroup>
      <NxTile.Content className="nx-tile-content--accordion-container">
        <NxStatefulAccordion
          title="https-authentication"
          defaultOpen={httpsAuthEnabled}
        >
          <NxAccordion.Header>
            <NxAccordion.Title>
              {LABELS.PROXY.HTTPS_AUTHENTICATION}
            </NxAccordion.Title>
          </NxAccordion.Header>
          <NxFieldset label="">
            <NxCheckbox
              {...FormUtils.checkboxProps('httpsAuthEnabled', current)}
              onChange={() => toggleAuthentication('httpsAuthEnabled')}
              disabled={!httpsEnabled}
            >
              {LABELS.PROXY.HTTPS_AUTH_CHECKBOX}
            </NxCheckbox>
          </NxFieldset>
          <NxFormRow>
            <>
              <NxFormGroup 
                label={LABELS.PROXY.USERNAME} 
                isRequired={httpsAuthEnabled}
              >
                <NxTextInput
                  className="nx-text-input"
                  {...FormUtils.fieldProps('httpsAuthUsername', current)}
                  onChange={FormUtils.handleUpdate('httpsAuthUsername', send)}
                  disabled={!isHttpsAuthEnabled}
                />
              </NxFormGroup>
              <NxFormGroup label={LABELS.PROXY.PASSWORD}>
                <NxTextInput
                  className="nx-text-input"
                  {...FormUtils.fieldProps('httpsAuthPassword', current)}
                  onChange={FormUtils.handleUpdate('httpsAuthPassword', send)}
                  type="password"
                  disabled={!isHttpsAuthEnabled}
                />
              </NxFormGroup>
            </>
          </NxFormRow>
          <NxFormGroup label={LABELS.PROXY.HOST_NAME}>
            <NxTextInput
              className="nx-text-input--long"
              {...FormUtils.fieldProps('httpsAuthNtlmHost', current)}
              onChange={FormUtils.handleUpdate('httpsAuthNtlmHost', send)}
              disabled={!isHttpsAuthEnabled}
            />
          </NxFormGroup>
          <NxFormGroup label={LABELS.PROXY.DOMAIN}>
            <NxTextInput
              className="nx-text-input--long"
              {...FormUtils.fieldProps('httpsAuthNtlmDomain', current)}
              onChange={FormUtils.handleUpdate('httpsAuthNtlmDomain', send)}
              disabled={!isHttpsAuthEnabled}
            />
          </NxFormGroup>
        </NxStatefulAccordion>
      </NxTile.Content>
      {isProxyEnabled && (
        <NxFormRow>
          <>
            <NxFormGroup
              label={LABELS.EXCLUDE.LABEL}
              sublabel={LABELS.EXCLUDE.SUB_LABEL}
            >
              <NxTextInput
                className="nx-text-input--long"
                id="nonProxyHost"
                name="nonProxyHost"
                value={nonProxyHost}
                isPristine={nonProxyHost === ''}
                onChange={handleSetNonProxyHost}
                onKeyDown={handleEnter}
                validatable
                validationErrors={validateNonProxyHost()}
              />
            </NxFormGroup>
            <NxButton
              variant="icon-only"
              title={LABELS.EXCLUDE.ADD}
              onClick={addNonProxyHost}
              type="button"
            >
              <NxFontAwesomeIcon icon={faPlusCircle} />
            </NxButton>
          </>
        </NxFormRow>
      )}
      {isProxyEnabled && list.length > 0 && (
        <NxList>
          {list.map((item, index) => (
            <NxList.Item key={item}>
              <NxList.Text>{item}</NxList.Text>
              <NxList.Actions>
                <NxButton
                  title={LABELS.EXCLUDE.REMOVE}
                  variant="icon-only"
                  onClick={() => removeNonProxyHost(index)}
                >
                  <NxFontAwesomeIcon icon={faTrashAlt} />
                </NxButton>
              </NxList.Actions>
            </NxList.Item>
          ))}
        </NxList>
      )}
    </NxStatefulForm>
  );
}
