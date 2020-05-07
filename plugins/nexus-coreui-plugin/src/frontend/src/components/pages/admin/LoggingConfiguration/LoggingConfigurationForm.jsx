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
  Button,
  FieldWrapper,
  NxErrorAlert,
  NxLoadWrapper,
  NxSubmitMask,
  Section,
  SectionFooter,
  Select,
  Textfield
} from "nexus-ui-plugin";

import LoggingConfigurationFormMachine from "./LoggingConfigurationFormMachine";

import UIStrings from "../../../../constants/UIStrings";

export default function LoggingConfigurationForm({itemId, onDone}) {
  const [current, send] = useMachine(LoggingConfigurationFormMachine, {
    context: {
      pristineData: {
        name: itemId
      }
    },

    actions: {
      onDone
    },

    devTools: true
  });
  const {isPristine, isInvalid, pristineData, data, loadError, saveError} = current.context;
  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isResetting = current.matches('resetting');

  function update(event) {
    send('UPDATE', {data: {[event.target.name]: event.target.value}});
  }

  function save(event) {
    event.preventDefault();
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

  return <Section className="nxrm-logging-configuration-form" onKeyPress={handleEnter}>
    <NxLoadWrapper loading={isLoading} error={loadError ? `${loadError}` : null}>
      {saveError && <NxErrorAlert>{UIStrings.LOGGING.MESSAGES.SAVE_ERROR} {saveError}</NxErrorAlert>}
      {isSaving && <NxSubmitMask message={UIStrings.SAVING}/>}
      {isResetting && <NxSubmitMask message={UIStrings.LOGGING.MESSAGES.RESETTING}/>}

      <FieldWrapper labelText={UIStrings.LOGGING.NAME_LABEL}>
        <Textfield isRequired disabled={pristineData.name} name="name" value={data.name} onChange={update}/>
      </FieldWrapper>
      <FieldWrapper labelText={UIStrings.LOGGING.LEVEL_LABEL}>
        <Select name="level" value={data.level} onChange={update}>
          {['OFF', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'].map(logLevel =>
              <option key={logLevel} value={logLevel}>{logLevel}</option>
          )}
        </Select>
      </FieldWrapper>

      <SectionFooter>
        <Button variant="primary" disabled={isPristine || isInvalid} onClick={save} type="submit">
          {UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
        </Button>
        <Button onClick={cancel}>{UIStrings.SETTINGS.CANCEL_BUTTON_LABEL}</Button>
        {itemId && <Button variant="error" onClick={reset}>{UIStrings.LOGGING.RESET_BUTTON}</Button>}
      </SectionFooter>
    </NxLoadWrapper>
  </Section>;
}
