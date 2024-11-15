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
import React, {forwardRef} from 'react';
import classNames from 'classnames';
import {
  FormUtils,
  ValidationUtils,
  UseNexusTruststore,
} from '@sonatype/nexus-ui-plugin';
import {useActor} from '@xstate/react';
import {
  NxButton,
  NxButtonBar,
  NxFormGroup,
  NxFormSelect,
  NxH2,
  NxP,
  NxStatefulForm,
  NxStatefulSubmitMask,
  NxTextInput,
  NxTile,
  NxInfoAlert,
  NxPageTitle,
  NxFontAwesomeIcon,
  NxTooltip,
  NxModal,
  NxFooter,
} from '@sonatype/react-shared-components';
import {faTrashAlt} from '@fortawesome/free-solid-svg-icons';

import LdapServersModalPassword from './LdapServersModalPassword';

import UIStrings from '../../../../constants/UIStrings';

import {
  isSimpleAuth,
  isDigestAuth,
  isCramtAuth,
  isAnonymousAuth,
  validateUrlValues,
  generateUrl,
  canDelete,
} from './LdapServersHelper';

const {
  LDAP_SERVERS: {FORM: LABELS},
  SETTINGS,
  PERMISSION_ERROR,
} = UIStrings;

export default forwardRef(({actor, onDone}, ref) => {
  const [state, send] = useActor(actor);
  const {
    isEdit,
    data: {protocol, authScheme, host, port, name},
    isPristine,
    validationErrors,
  } = state.context;
  const isProtocolSelected = ValidationUtils.notBlank(protocol);
  const isAuthMethodSelected = ValidationUtils.notBlank(authScheme);
  const showRealmAuth = isDigestAuth(authScheme) || isCramtAuth(authScheme);
  const showAuth = showRealmAuth || isSimpleAuth(authScheme);
  const isInvalid = FormUtils.isInvalid(validationErrors);
  const canVerify = !isInvalid;
  const url = generateUrl('https', host, port);
  const urlMessage = generateUrl(protocol, host, port);
  const showUseTrustStore = validateUrlValues(protocol, host, port);
  const verifyingConnection = state.matches('verifyingConnection');
  const changingPassword = state.matches('changingPassword');
  const changingPasswordMessage = state.matches(
    'changingPassword.verifyingConnection'
  );
  const askingPassword =
    state.matches('askingPassword') ||
    (changingPassword && !changingPasswordMessage);
  const confirmingDelete = state.matches('confirmingDelete');

  const next = () => send({type: 'NEXT'});

  const verify = () => {
    if (canVerify) {
      send({type: 'VERIFY_CONNECTION'});
    }
  };

  const onDelete = () => {
    if (canDelete()) {
      send({type: 'DELETE_CONNECTION'});
    }
  };

  const updateProtocol = (value) =>
    send({type: 'UPDATE_PROTOCOL', value});

  const onChangePassword = () => send({type: 'CHANGE_PASSWORD'});

  const validations = () => {
    if (isEdit && isPristine) {
      return undefined;
    }

    return (
      validationErrors?.active || FormUtils.saveTooltip({isPristine, isInvalid})
    );
  };

  return (
    <NxTile.Content>
      <NxStatefulForm
        {...FormUtils.formProps(state, send)}
        validationErrors={validations()}
        onSubmit={next}
        submitBtnText={LABELS.NEXT}
        onCancel={onDone}
        additionalFooterBtns={
          <NxButtonBar>
            <NxButton
              type="button"
              onClick={verify}
              className={classNames({disabled: !canVerify})}
            >
              {LABELS.VERIFY_CONNECTION}
            </NxButton>
          </NxButtonBar>
        }
        ref={ref}
      >
        <NxPageTitle>
          <NxH2>{LABELS.CONFIGURATION}</NxH2>
          {isEdit && (
            <NxButtonBar>
              <NxTooltip title={!canDelete() && PERMISSION_ERROR}>
                <NxButton
                  onClick={onDelete}
                  className={classNames({disabled: !canDelete()})}
                  variant="tertiary"
                  type="button"
                >
                  <NxFontAwesomeIcon icon={faTrashAlt} />
                  <span>{LABELS.DELETE_BUTTON}</span>
                </NxButton>
              </NxTooltip>
            </NxButtonBar>
          )}
        </NxPageTitle>
        <NxFormGroup label={LABELS.NAME} isRequired>
          <NxTextInput
            {...FormUtils.fieldProps('name', state)}
            onChange={FormUtils.handleUpdate('name', send)}
          />
        </NxFormGroup>
        <NxH2>{LABELS.SETTINGS.LABEL}</NxH2>
        <NxP>{LABELS.SETTINGS.SUB_LABEL}</NxP>
        <NxFormGroup label={LABELS.PROTOCOL.LABEL} isRequired>
          <NxFormSelect
            {...FormUtils.fieldProps('protocol', state)}
            onChange={updateProtocol}
            className="nx-form-select--short"
            validatable
          >
            <option value="" disabled={isProtocolSelected} />
            {Object.values(LABELS.PROTOCOL.OPTIONS).map((label) => (
              <option key={label} value={label}>
                {label}
              </option>
            ))}
          </NxFormSelect>
        </NxFormGroup>
        <NxFormGroup label={LABELS.HOSTNAME} isRequired>
          <NxTextInput
            className="nx-text-input--long"
            {...FormUtils.fieldProps('host', state)}
            onChange={FormUtils.handleUpdate('host', send)}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.PORT} isRequired>
          <NxTextInput
            className="nx-text-input--short"
            {...FormUtils.fieldProps('port', state)}
            onChange={FormUtils.handleUpdate('port', send)}
          />
        </NxFormGroup>
        {showUseTrustStore && (
          <UseNexusTruststore
            remoteUrl={url}
            {...FormUtils.checkboxProps('useTrustStore', state)}
            onChange={FormUtils.handleUpdate('useTrustStore', send)}
          />
        )}
        <NxFormGroup
          label={LABELS.SEARCH.LABEL}
          sublabel={LABELS.SEARCH.SUB_LABEL}
          isRequired
        >
          <NxTextInput
            className="nx-text-input--long"
            {...FormUtils.fieldProps('searchBase', state)}
            onChange={FormUtils.handleUpdate('searchBase', send)}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.AUTHENTICATION.LABEL} isRequired>
          <NxFormSelect
            {...FormUtils.fieldProps('authScheme', state)}
            onChange={FormUtils.handleUpdate('authScheme', send)}
          >
            <option value="" disabled={isAuthMethodSelected} />
            {Object.values(LABELS.AUTHENTICATION.OPTIONS).map((option) => (
              <option key={option.id} value={option.id}>
                {option.label}
              </option>
            ))}
          </NxFormSelect>
        </NxFormGroup>

        {showAuth && (
          <>
            {showRealmAuth && (
              <NxFormGroup
                label={LABELS.SASL_REALM.LABEL}
                sublabel={LABELS.SASL_REALM.SUB_LABEL}
              >
                <NxTextInput
                  {...FormUtils.fieldProps('authRealm', state)}
                  onChange={FormUtils.handleUpdate('authRealm', send)}
                />
              </NxFormGroup>
            )}
            <NxFormGroup
              label={LABELS.USERNAME.LABEL}
              sublabel={LABELS.USERNAME.SUB_LABEL}
              isRequired
            >
              <NxTextInput
                className="nx-text-input--long"
                {...FormUtils.fieldProps('authUsername', state)}
                onChange={FormUtils.handleUpdate('authUsername', send)}
              />
            </NxFormGroup>
            {!isEdit && (
              <NxFormGroup
                label={LABELS.PASSWORD.LABEL}
                sublabel={LABELS.PASSWORD.SUB_LABEL}
                isRequired
              >
                <NxTextInput
                  {...FormUtils.fieldProps('authPassword', state)}
                  onChange={FormUtils.handleUpdate('authPassword', send)}
                  type="password"
                />
              </NxFormGroup>
            )}
            {isEdit && !isAnonymousAuth(authScheme) && (
              <NxFormGroup label="">
                <NxButton
                  variant="tertiary"
                  className="change-password"
                  type="button"
                  onClick={onChangePassword}
                >
                  {LABELS.CHANGE_PASSWORD}
                </NxButton>
              </NxFormGroup>
            )}
          </>
        )}

        <NxH2>{LABELS.CONNECTION_RULES.LABEL}</NxH2>
        <NxP>{LABELS.CONNECTION_RULES.SUB_LABEL}</NxP>
        <NxFormGroup
          label={LABELS.WAIT_TIMEOUT.LABEL}
          sublabel={LABELS.WAIT_TIMEOUT.SUB_LABEL}
          isRequired
        >
          <NxTextInput
            className="nx-text-input--short"
            {...FormUtils.fieldProps('connectionTimeout', state)}
            onChange={FormUtils.handleUpdate('connectionTimeout', send)}
          />
        </NxFormGroup>
        <NxFormGroup
          label={LABELS.RETRY_TIMEOUT.LABEL}
          sublabel={LABELS.RETRY_TIMEOUT.SUB_LABEL}
          isRequired
        >
          <NxTextInput
            className="nx-text-input--short"
            {...FormUtils.fieldProps('connectionRetryDelay', state)}
            onChange={FormUtils.handleUpdate('connectionRetryDelay', send)}
          />
        </NxFormGroup>
        <NxFormGroup
          label={LABELS.MAX_RETRIES.LABEL}
          sublabel={LABELS.MAX_RETRIES.SUB_LABEL}
          isRequired
        >
          <NxTextInput
            className="nx-text-input--short"
            {...FormUtils.fieldProps('maxIncidentsCount', state)}
            onChange={FormUtils.handleUpdate('maxIncidentsCount', send)}
          />
        </NxFormGroup>
        {isEdit && <NxInfoAlert>{LABELS.ALERT}</NxInfoAlert>}
        {verifyingConnection && (
          <NxStatefulSubmitMask
            message={LABELS.VERIFYING_MESSAGE(urlMessage)}
          />
        )}
        {changingPasswordMessage && (
          <NxStatefulSubmitMask
            message={LABELS.CHANGING_PASSWORD_MESSAGE(urlMessage)}
          />
        )}
      </NxStatefulForm>
      {askingPassword && (
        <LdapServersModalPassword
          onCancel={() => send({type: 'CANCEL'})}
          onSubmit={({value}) =>
            send({type: 'DONE', name: 'authPassword', value})
          }
        />
      )}
      {confirmingDelete && (
        <NxModal aria-labelledby="modal-header-confirm-delete" variant="narrow">
          <NxModal.Header>
            <NxH2 id="modal-header-confirm-delete">
              {LABELS.MODAL_DELETE.LABEL}
            </NxH2>
          </NxModal.Header>
          <NxModal.Content>
            <NxP>{LABELS.VERIFY_DELETE_MESSAGE(name)}</NxP>
          </NxModal.Content>
          <NxFooter>
            <NxButtonBar>
              <NxButton onClick={() => send({type: 'CANCEL'})}>
                {SETTINGS.CANCEL_BUTTON_LABEL}
              </NxButton>
              <NxButton onClick={() => send({type: 'ACCEPT'})} variant="primary">
                {LABELS.MODAL_DELETE.CONFIRM}
              </NxButton>
            </NxButtonBar>
          </NxFooter>
        </NxModal>
      )}
    </NxTile.Content>
  );
});
