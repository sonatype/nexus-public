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
  ContentBody,
  FormUtils,
  NxButton,
  NxForm,
  NxFormGroup,
  NxTextInput,
  Page,
  PageHeader,
  PageTitle,
  Section,
  Select,
  ValidationUtils
} from '@sonatype/nexus-ui-plugin';

import LoggingConfigurationFormMachine from './LoggingConfigurationFormMachine';

import UIStrings from '../../../../constants/UIStrings';
import {faScroll} from '@fortawesome/free-solid-svg-icons';

export default function LoggingConfigurationForm({itemId, onDone}) {
  const [current, send] = useMachine(LoggingConfigurationFormMachine, {
    context: {
      pristineData: {
        name: itemId
      }
    },

    actions: {
      onCancel: onDone,
      onReset: onDone,
      onSaveSuccess: onDone
    },

    devTools: true
  });

  const {isPristine, pristineData, data, loadError, saveError, resetError, validationErrors} = current.context;
  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isResetting = current.matches('resetting');
  const isInvalid = ValidationUtils.isInvalid(validationErrors);

  function update(field, value) {
    send({
      type: 'UPDATE',
      data: {
        [field]: value
      }
    });
  }

  function updateName(value) {
    update('name', value);
  }

  function updateLevel(event) {
    update('level', event.currentTarget.value);
  }

  function save() {
    send('SAVE');
  }

  function handleEnter(event) {
    if (event.key === 'Enter') {
      save(event);
    }
  }

  function cancel() {
    send('CANCEL');
  }

  function reset() {
    send('RESET');
  }

  function retry() {
    send('RETRY');
  }

  return <Page className="nxrm-logging-configuration">
    <PageHeader><PageTitle icon={faScroll} {...UIStrings.LOGGING.MENU}/></PageHeader>
    <ContentBody>
      <Section className="nxrm-logging-configuration-form" onKeyPress={handleEnter}>
        <NxForm
            loading={isLoading}
            loadError={loadError || resetError}
            doLoad={retry}
            onCancel={cancel}
            onSubmit={save}
            submitError={saveError}
            submitMaskState={(isSaving || isResetting) ? false : null}
            submitMaskMessage={isSaving ? UIStrings.SAVING : UIStrings.LOGGING.MESSAGES.RESETTING}
            submitBtnText={UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
            validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}
            additionalFooterBtns={
              itemId && <NxButton variant="error" onClick={reset}>{UIStrings.LOGGING.RESET_BUTTON}</NxButton>
            }
        >
          {() => <>
            <NxFormGroup label={UIStrings.LOGGING.NAME_LABEL} isRequired>
              <NxTextInput
                  className="nx-text-input--long"
                  {...FormUtils.fieldProps('name', current)}
                  disabled={pristineData.name}
                  onChange={updateName}/>
            </NxFormGroup>
            <NxFormGroup label={UIStrings.LOGGING.LEVEL_LABEL} isRequired>
              <Select name="level" value={data.level} onChange={updateLevel}>
                {['OFF', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'].map(logLevel =>
                    <option key={logLevel} value={logLevel}>{logLevel}</option>
                )}
              </Select>
            </NxFormGroup>
          </>}
        </NxForm>
      </Section>
    </ContentBody>
  </Page>;
}
