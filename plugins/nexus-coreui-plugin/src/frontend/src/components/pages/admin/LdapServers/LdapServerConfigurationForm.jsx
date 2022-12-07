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
import classNames from 'classnames';
import {
  FormUtils,
  ValidationUtils,
  UseNexusTruststore,
} from '@sonatype/nexus-ui-plugin';
import {
  NxTile,
  NxTextInput,
  NxFormGroup,
  NxH2,
  NxP,
  NxFormSelect,
  NxForm,
  NxButton,
  NxButtonBar,
  NxStatefulSubmitMask,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

import {
  isSimpleAuth,
  isDigestAuth,
  isCramtAuth,
  validateUrlValues,
  generateUrl,
} from './LdapServersHelper';

const {
  LDAP_SERVERS: {FORM: LABELS},
} = UIStrings;

export default function LdapServerConfigurationForm({
  parentState,
  parentSend,
  onDone,
}) {
  const {
    data: {protocol, authScheme, host, port},
    validationErrors,
  } = parentState.context;
  const isProtocolSelected = ValidationUtils.notBlank(protocol);
  const isAuthMethodSelected = ValidationUtils.notBlank(authScheme);
  const showRealmAuth = isDigestAuth(authScheme) || isCramtAuth(authScheme);
  const showAuth = showRealmAuth || isSimpleAuth(authScheme);
  const isInvalid = FormUtils.isInvalid(validationErrors);
  const canVerify = !isInvalid;
  const url = generateUrl('https', host, port);
  const urlMessage = generateUrl(protocol, host, port);
  const showUseTrustStore = validateUrlValues(protocol, host, port);
  const verifyingConnection = parentState.matches('loaded.verifyingConnection');

  const cancel = () => onDone();

  const next = () => parentSend('NEXT');

  const verify = () => {
    if (canVerify) {
      parentSend('VERIFY_CONNECTION');
    }
  };

  const updateProtocol = (e) => {
    parentSend({type: 'UPDATE_PROTOCOL', value: e.target.value});
  };

  return (
    <NxTile.Content>
      <NxH2>{LABELS.CONFIGURATION}</NxH2>
      <NxForm
        {...FormUtils.formProps(parentState, parentSend)}
        onSubmit={next}
        submitBtnText={LABELS.NEXT}
        onCancel={cancel}
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
      >
        <NxFormGroup label={LABELS.NAME} isRequired>
          <NxTextInput
            {...FormUtils.fieldProps('name', parentState)}
            onChange={FormUtils.handleUpdate('name', parentSend)}
          />
        </NxFormGroup>
        <NxH2>{LABELS.SETTINGS.LABEL}</NxH2>
        <NxP>{LABELS.SETTINGS.SUB_LABEL}</NxP>
        <NxFormGroup label={LABELS.PROTOCOL.LABEL} isRequired>
          <NxFormSelect
            {...FormUtils.selectProps('protocol', parentState)}
            onChange={updateProtocol}
            className="nx-form-select--short"
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
            {...FormUtils.fieldProps('host', parentState)}
            onChange={FormUtils.handleUpdate('host', parentSend)}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.PORT} isRequired>
          <NxTextInput
            className="nx-text-input--short"
            {...FormUtils.fieldProps('port', parentState)}
            onChange={FormUtils.handleUpdate('port', parentSend)}
          />
        </NxFormGroup>
        {showUseTrustStore && (
          <UseNexusTruststore
            remoteUrl={url}
            {...FormUtils.checkboxProps('useTrustStore', parentState)}
            onChange={FormUtils.handleUpdate('useTrustStore', parentSend)}
          />
        )}
        <NxFormGroup
          label={LABELS.SEARCH.LABEL}
          sublabel={LABELS.SEARCH.SUB_LABEL}
          isRequired
        >
          <NxTextInput
            className="nx-text-input--long"
            {...FormUtils.fieldProps('searchBase', parentState)}
            onChange={FormUtils.handleUpdate('searchBase', parentSend)}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.AUTHENTICATION.LABEL} isRequired>
          <NxFormSelect
            {...FormUtils.selectProps('authScheme', parentState)}
            onChange={FormUtils.handleUpdate('authScheme', parentSend)}
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
                  {...FormUtils.fieldProps('authRealm', parentState)}
                  onChange={FormUtils.handleUpdate('authRealm', parentSend)}
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
                {...FormUtils.fieldProps('authUsername', parentState)}
                onChange={FormUtils.handleUpdate('authUsername', parentSend)}
              />
            </NxFormGroup>
            <NxFormGroup
              className="nx-text-input--long"
              label={LABELS.PASSWORD.LABEL}
              sublabel={LABELS.PASSWORD.SUB_LABEL}
              isRequired
            >
              <NxTextInput
                {...FormUtils.fieldProps('authPassword', parentState)}
                onChange={FormUtils.handleUpdate('authPassword', parentSend)}
                type="password"
              />
            </NxFormGroup>
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
            {...FormUtils.fieldProps('connectionTimeout', parentState)}
            onChange={FormUtils.handleUpdate('connectionTimeout', parentSend)}
          />
        </NxFormGroup>
        <NxFormGroup
          label={LABELS.RETRY_TIMEOUT.LABEL}
          sublabel={LABELS.RETRY_TIMEOUT.SUB_LABEL}
          isRequired
        >
          <NxTextInput
            className="nx-text-input--short"
            {...FormUtils.fieldProps('connectionRetryDelay', parentState)}
            onChange={FormUtils.handleUpdate(
              'connectionRetryDelay',
              parentSend
            )}
          />
        </NxFormGroup>
        <NxFormGroup
          label={LABELS.MAX_RETRIES.LABEL}
          sublabel={LABELS.MAX_RETRIES.SUB_LABEL}
          isRequired
        >
          <NxTextInput
            className="nx-text-input--short"
            {...FormUtils.fieldProps('maxIncidentsCount', parentState)}
            onChange={FormUtils.handleUpdate('maxIncidentsCount', parentSend)}
          />
        </NxFormGroup>
        {verifyingConnection && (
          <NxStatefulSubmitMask
            message={LABELS.VERIFYING_MESSAGE(urlMessage)}
          />
        )}
      </NxForm>
    </NxTile.Content>
  );
}
