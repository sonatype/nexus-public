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
  NxForm,
  NxFormGroup,
  NxTextInput,
  NxH2,
  NxCheckbox,
  NxAccordion,
  NxButton,
  NxFontAwesomeIcon,
  NxFormRow,
  NxList,
  NxTooltip,
  NxFieldset,
  NxTile,
} from '@sonatype/react-shared-components';
import {FormUtils} from '@sonatype/nexus-ui-plugin';
import {faTrashAlt, faPlusCircle} from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../constants/UIStrings';

const {CONFIGURATION: LABELS} = UIStrings.HTTP;

import HttpMachine from './HttpMachine';

export default function HttpForm() {
  const [current, send] = useMachine(HttpMachine, {devTools: true});
  const {
    data: {
      nonProxyHosts,
      httpsEnabled,
      httpEnabled,
      httpAuthEnabled,
      httpsAuthEnabled,
    },
    isPristine,
    loadError,
    saveError,
    validationErrors,
  } = current.context;
  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isInvalid = FormUtils.isInvalid(validationErrors);
  const isHttpsEnabled = httpsEnabled && httpEnabled;

  const list = nonProxyHosts || [];

  const discard = () => send('RESET');

  const save = () => send('SAVE');

  const retry = () => send('RETRY');

  const removeNonProxyHost = (index) =>
    send({type: 'REMOVE_NON_PROXY_HOST', index});

  const addNonProxyHost = () => send('ADD_NON_PROXY_HOST');

  const toggleAuthentication = (name, value) =>
    send({type: 'TOGGLE_AUTHENTICATION', name, value});

  const handleHttpCheckbox = () => send('TOGGLE_HTTP_PROXY');

  const handleHttpsCheckbox = () => send('TOGGLE_HTTPS_PROXY');

  const handleEnter = (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      addNonProxyHost();
    }
  };

  return (
    <NxForm
      loading={isLoading}
      loadError={loadError}
      doLoad={retry}
      onSubmit={save}
      submitError={saveError}
      submitMaskState={isSaving ? false : null}
      submitBtnText={UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
      validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}
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
        isRequired
      >
        <NxTextInput
          className="nx-text-input--short"
          {...FormUtils.fieldProps('timeout', current)}
          onChange={FormUtils.handleUpdate('timeout', send)}
        />
      </NxFormGroup>
      <NxFormGroup
        label={LABELS.ATTEMPTS.LABEL}
        sublabel={LABELS.ATTEMPTS.SUB_LABEL}
        isRequired
      >
        <NxTextInput
          className="nx-text-input--short"
          {...FormUtils.fieldProps('retries', current)}
          onChange={FormUtils.handleUpdate('retries', send)}
        />
      </NxFormGroup>
      <NxH2>{LABELS.PROXY.LABEL}</NxH2>
      <NxFieldset label="" isRequired>
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
        isRequired
      >
        <NxTextInput
          className="nx-text-input--long"
          {...FormUtils.fieldProps('httpHost', current)}
          onChange={FormUtils.handleUpdate('httpHost', send)}
          disabled={!httpEnabled}
        />
      </NxFormGroup>
      <NxFormGroup label={LABELS.PROXY.HTTP_PORT} isRequired>
        <NxTextInput
          className="nx-text-input--short"
          {...FormUtils.fieldProps('httpPort', current)}
          onChange={FormUtils.handleUpdate('httpPort', send)}
          disabled={!httpEnabled}
        />
      </NxFormGroup>
      <NxTile.Content className="nx-tile-content--accordion-container">
        <NxAccordion
          title="http-authentication"
          open={httpAuthEnabled}
          onToggle={() =>
            httpEnabled && toggleAuthentication('httpAuthEnabled')
          }
        >
          <NxAccordion.Header>
            <NxAccordion.Title>
              {LABELS.PROXY.HTTP_AUTHENTICATION}
            </NxAccordion.Title>
          </NxAccordion.Header>
          <NxFormRow>
            <>
              <NxFormGroup label={LABELS.PROXY.USERNAME} isRequired>
                <NxTextInput
                  className="nx-text-input"
                  {...FormUtils.fieldProps('httpAuthUsername', current)}
                  onChange={FormUtils.handleUpdate('httpAuthUsername', send)}
                  disabled={!httpEnabled}
                />
              </NxFormGroup>
              <NxFormGroup label={LABELS.PROXY.PASSWORD}>
                <NxTextInput
                  className="nx-text-input"
                  {...FormUtils.fieldProps('httpAuthPassword', current)}
                  onChange={FormUtils.handleUpdate('httpAuthPassword', send)}
                  type="password"
                  disabled={!httpEnabled}
                />
              </NxFormGroup>
            </>
          </NxFormRow>
          <NxFormGroup label={LABELS.PROXY.HOST_NAME}>
            <NxTextInput
              className="nx-text-input--long"
              {...FormUtils.fieldProps('httpAuthNtlmHost', current)}
              onChange={FormUtils.handleUpdate('httpAuthNtlmHost', send)}
              disabled={!httpEnabled}
            />
          </NxFormGroup>
          <NxFormGroup label={LABELS.PROXY.DOMAIN}>
            <NxTextInput
              className="nx-text-input--long"
              {...FormUtils.fieldProps('httpAuthNtlmDomain', current)}
              onChange={FormUtils.handleUpdate('httpAuthNtlmDomain', send)}
              disabled={!httpEnabled}
            />
          </NxFormGroup>
        </NxAccordion>
      </NxTile.Content>
      <NxFieldset label="" isRequired>
        <NxCheckbox
          {...FormUtils.checkboxProps('httpsEnabled', current)}
          onChange={handleHttpsCheckbox}
          disabled={!httpEnabled}
        >
          {LABELS.PROXY.HTTPS_CHECKBOX}
        </NxCheckbox>
      </NxFieldset>
      <NxFormGroup
        label={LABELS.PROXY.HTTPS_HOST}
        sublabel={LABELS.PROXY.SUB_LABEL}
        isRequired
      >
        <NxTextInput
          className="nx-text-input--long"
          {...FormUtils.fieldProps('httpsHost', current)}
          onChange={FormUtils.handleUpdate('httpsHost', send)}
          disabled={!isHttpsEnabled}
        />
      </NxFormGroup>
      <NxFormGroup label={LABELS.PROXY.HTTPS_PORT} isRequired>
        <NxTextInput
          className="nx-text-input--short"
          {...FormUtils.fieldProps('httpsPort', current)}
          onChange={FormUtils.handleUpdate('httpsPort', send)}
          disabled={!isHttpsEnabled}
        />
      </NxFormGroup>
      <NxTile.Content className="nx-tile-content--accordion-container">
        <NxAccordion
          title="https-authentication"
          open={httpsAuthEnabled}
          onToggle={() =>
            isHttpsEnabled && toggleAuthentication('httpsAuthEnabled')
          }
        >
          <NxAccordion.Header>
            <NxAccordion.Title>
              {LABELS.PROXY.HTTPS_AUTHENTICATION}
            </NxAccordion.Title>
          </NxAccordion.Header>
          <NxFormRow>
            <>
              <NxFormGroup label={LABELS.PROXY.USERNAME} isRequired>
                <NxTextInput
                  className="nx-text-input"
                  {...FormUtils.fieldProps('httpsAuthUsername', current)}
                  onChange={FormUtils.handleUpdate('httpsAuthUsername', send)}
                  disabled={!isHttpsEnabled}
                />
              </NxFormGroup>
              <NxFormGroup label={LABELS.PROXY.PASSWORD}>
                <NxTextInput
                  className="nx-text-input"
                  {...FormUtils.fieldProps('httpsAuthPassword', current)}
                  onChange={FormUtils.handleUpdate('httpsAuthPassword', send)}
                  type="password"
                  disabled={!isHttpsEnabled}
                />
              </NxFormGroup>
            </>
          </NxFormRow>
          <NxFormGroup label={LABELS.PROXY.HOST_NAME}>
            <NxTextInput
              className="nx-text-input--long"
              {...FormUtils.fieldProps('httpsAuthNtlmHost', current)}
              onChange={FormUtils.handleUpdate('httpsAuthNtlmHost', send)}
              disabled={!isHttpsEnabled}
            />
          </NxFormGroup>
          <NxFormGroup label={LABELS.PROXY.DOMAIN}>
            <NxTextInput
              className="nx-text-input--long"
              {...FormUtils.fieldProps('httpsAuthNtlmDomain', current)}
              onChange={FormUtils.handleUpdate('httpsAuthNtlmDomain', send)}
              disabled={!isHttpsEnabled}
            />
          </NxFormGroup>
        </NxAccordion>
      </NxTile.Content>
      {httpEnabled && (
        <NxFormRow>
          <>
            <NxFormGroup
              label={LABELS.EXCLUDE.LABEL}
              sublabel={LABELS.EXCLUDE.SUB_LABEL}
              isRequired
            >
              <NxTextInput
                className="nx-text-input--long"
                {...FormUtils.fieldProps('nonProxyHost', current)}
                onChange={FormUtils.handleUpdate('nonProxyHost', send)}
                onKeyDown={handleEnter}
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
      {httpEnabled && list.length > 0 && (
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
    </NxForm>
  );
}
