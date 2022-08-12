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
  NxButton,
  NxTooltip,
} from '@sonatype/react-shared-components';
import {FormUtils} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

const {CONFIGURATION: LABELS} = UIStrings.HTTP;

import HttpMachine from './HttpMachine';

export default function HttpForm() {
  const [current, send] = useMachine(HttpMachine, {devTools: true});
  const {isPristine, loadError, saveError, validationErrors} = current.context;
  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isInvalid = FormUtils.isInvalid(validationErrors);

  const discard = () => send('RESET');

  const save = () => send('SAVE');

  const retry = () => send('RETRY');

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
        isRequired
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
    </NxForm>
  );
}
