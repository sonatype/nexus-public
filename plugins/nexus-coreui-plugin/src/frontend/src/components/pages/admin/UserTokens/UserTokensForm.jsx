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
import {faKey} from '@fortawesome/free-solid-svg-icons';

import {ExtJS, Permissions, FormUtils} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxCheckbox,
  NxFieldset,
  NxFontAwesomeIcon,
  NxH2,
  NxP,
  NxStatefulForm,
  NxTooltip
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import UserTokensResetModal from './UserTokensResetModal';

const {
  USER_TOKEN_CONFIGURATION: {
    CAPTION,
    HELP_TEXT,
    USER_TOKENS_CHECKBOX,
    REPOSITORY_AUTHENTICATION_CHECKBOX,
    RESET_ALL_TOKENS_BUTTON
  },
  SETTINGS: {DISCARD_BUTTON_LABEL}
} = UIStrings;

export default function UserTokensForm({service}) {
  const canDelete = ExtJS.checkPermission(Permissions.USER_TOKENS_USERS.DELETE);

  const [state, send] = useService(service);

  const resetConfirmation = state.matches('resetConfirmation');

  const {
    isPristine,
    data: {enabled},
    pristineData: {enabled: enabledSaved}
  } = state.context;

  const discard = () => send('RESET');
  const showResetConfirmation = () => send('RESET_CONFIRMATION');
  const setEnabled = (e) => send({type: 'SET_ENABLED', value: e.currentTarget.checked});

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

        <NxFieldset
          label={USER_TOKENS_CHECKBOX.LABEL}
          sublabel={USER_TOKENS_CHECKBOX.SUBLABEL}
          isRequired
        >
          <NxCheckbox {...FormUtils.checkboxProps('enabled', state)} onChange={setEnabled}>
            {USER_TOKENS_CHECKBOX.DESCRIPTION}
          </NxCheckbox>
        </NxFieldset>

        <NxFieldset
          label={REPOSITORY_AUTHENTICATION_CHECKBOX.LABEL}
          sublabel={REPOSITORY_AUTHENTICATION_CHECKBOX.SUBLABEL}
          isRequired
        >
          <NxCheckbox
            {...FormUtils.checkboxProps('protectContent', state)}
            onChange={FormUtils.handleUpdate('protectContent', send)}
            disabled={!enabled}
          >
            {REPOSITORY_AUTHENTICATION_CHECKBOX.DESCRIPTION}
          </NxCheckbox>
        </NxFieldset>
      </NxStatefulForm>

      {resetConfirmation && <UserTokensResetModal service={service} />}
    </>
  );
}
