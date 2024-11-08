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
import {faKey} from '@fortawesome/free-solid-svg-icons';

import {ExtJS, Permissions, FormUtils} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxCheckbox,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxH2,
  NxP,
  NxStatefulForm,
  NxTextInput,
  NxTooltip
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import UserTokensResetModal from './UserTokensResetModal';
import UserTokenExpiryChangesModal from './UserTokenExpiryChangesModal';

const {
  USER_TOKEN_CONFIGURATION: {
    CAPTION,
    HELP_TEXT,
    USER_TOKENS_CHECKBOX,
    REPOSITORY_AUTHENTICATION_CHECKBOX,
    EXPIRATION_CHECKBOX,
    USER_TOKEN_EXPIRY,
    RESET_ALL_TOKENS_BUTTON
  },
  SETTINGS: {DISCARD_BUTTON_LABEL}
} = UIStrings;

export default function UserTokensForm({service}) {
  const canDelete = ExtJS.checkPermission(Permissions.USER_TOKENS_USERS.DELETE);

  const [state, send] = useActor(service);

  const resetConfirmation = state.matches('resetConfirmation');

  const {
    isPristine,
    data: {enabled, expirationEnabled},
    pristineData: {enabled: enabledSaved}
  } = state.context;

  const discard = () => send({type: 'RESET'});
  const showResetConfirmation = () => send({type: 'RESET_CONFIRMATION'});
  const setEnabled = (value) => send({type: 'SET_ENABLED', value});
  const showUserTokenExpiryChangesModal = state.matches('showUserTokenExpiryChangesModal');
  const save = () => send('SAVE');
  const closeModal = () => send('CLOSE');

  return (
    <>
      <NxStatefulForm
        {...FormUtils.formProps(state, send)}
        additionalFooterBtns={
          <>
            {canDelete && enabledSaved && (
              <NxButton type="button" variant="tertiary" onClick={showResetConfirmation}>
                <NxFontAwesomeIcon icon={faKey} />
                <span>{RESET_ALL_TOKENS_BUTTON}</span>
              </NxButton>
            )}
            <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
              <NxButton
                type="button"
                variant="secondary"
                className={isPristine ? 'disabled' : ''}
                onClick={discard}
              >
                {DISCARD_BUTTON_LABEL}
              </NxButton>
            </NxTooltip>
          </>
        }
      >
        <NxH2>{CAPTION}</NxH2>
        <NxP>{HELP_TEXT}</NxP>

        <NxFieldset label={USER_TOKENS_CHECKBOX.LABEL}>
          <NxCheckbox {...FormUtils.checkboxProps('enabled', state)} onChange={setEnabled}>
            {USER_TOKENS_CHECKBOX.DESCRIPTION}
          </NxCheckbox>
        </NxFieldset>

        <NxFieldset label={REPOSITORY_AUTHENTICATION_CHECKBOX.LABEL}>
          <NxCheckbox
            {...FormUtils.checkboxProps('protectContent', state)}
            onChange={FormUtils.handleUpdate('protectContent', send)}
            disabled={!enabled}
          >
            {REPOSITORY_AUTHENTICATION_CHECKBOX.DESCRIPTION}
          </NxCheckbox>
        </NxFieldset>

        <NxFieldset label={EXPIRATION_CHECKBOX.LABEL}>
          <NxCheckbox
              {...FormUtils.checkboxProps('expirationEnabled', state)}
              onChange={FormUtils.handleUpdate('expirationEnabled', send)}
              disabled={!enabled}
          >
            {EXPIRATION_CHECKBOX.DESCRIPTION}
          </NxCheckbox>
        </NxFieldset>

        <NxFormGroup
          label={USER_TOKEN_EXPIRY.LABEL}
          sublabel={USER_TOKEN_EXPIRY.SUBLABEL}
          isRequired={expirationEnabled}
        >
          <NxTextInput
            {...FormUtils.fieldProps('expirationDays', state)}
            className="nx-text-input--short nxrm-user-tokens-expiry-field"
            onChange={FormUtils.handleUpdate('expirationDays', send)}
            disabled={!enabled || !expirationEnabled}
          />
        </NxFormGroup>
      </NxStatefulForm>

      {resetConfirmation && <UserTokensResetModal service={service} />}
      {showUserTokenExpiryChangesModal && <UserTokenExpiryChangesModal onClose={closeModal}
                                                                       onConfirm={save}
                                                                       expirationEnabled={expirationEnabled}/>}
    </>
  );
}
