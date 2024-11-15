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
  FormUtils
} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxCheckbox,
  NxFormGroup,
  NxFormSelect,
  NxStatefulForm,
  NxTextInput,
  NxTooltip,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

import AnonymousMachine from './AnonymousMachine';

export default function AnonymousSettingsForm() {
  const [current, send] = useMachine(AnonymousMachine, {devTools: true});
  const {data, isPristine, realms} = current.context;

  function discard() {
    send({type: 'RESET'});
  }

  return <NxStatefulForm
      {...FormUtils.formProps(current, send)}
      additionalFooterBtns={
        <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
          <NxButton type="button" className={isPristine && 'disabled'} onClick={discard}>
            {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
          </NxButton>
        </NxTooltip>
      }>
    {() => <>
      <NxFormGroup label={UIStrings.ANONYMOUS_SETTINGS.ENABLED_CHECKBOX_LABEL} isRequired>
        <NxCheckbox
            {...FormUtils.checkboxProps('enabled', current)}
            onChange={FormUtils.handleUpdate('enabled', send)}>
          {UIStrings.ANONYMOUS_SETTINGS.ENABLED_CHECKBOX_DESCRIPTION}
        </NxCheckbox>
      </NxFormGroup>
      <NxFormGroup label={UIStrings.ANONYMOUS_SETTINGS.USERNAME_TEXTFIELD_LABEL} isRequired>
        <NxTextInput
          {...FormUtils.fieldProps('userId', current)}
          onChange={FormUtils.handleUpdate('userId', send)}
          onBlur={FormUtils.trimOnBlur('userId', send)}
        />
      </NxFormGroup>
      <NxFormGroup label={UIStrings.ANONYMOUS_SETTINGS.REALM_SELECT_LABEL} isRequired>
        <NxFormSelect name='realmName' value={data.realmName} onChange={FormUtils.handleUpdate('realmName', send)} validatable>
          {realms?.map((realm) =>
              <option key={realm.id} value={realm.id}>{realm.name}</option>
          )}
        </NxFormSelect>
      </NxFormGroup>
    </>}
  </NxStatefulForm>;
}
