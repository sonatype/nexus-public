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
import {
  Alert,
  Button,
  FieldWrapper,
  NxLoadWrapper,
  NxSubmitMask,
  Section,
  SectionFooter,
  Textfield,
  Utils
} from 'nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

export default function UserAccountSettings({service}) {
  const [current, send] = useService(service);
  const context = current.context;
  const data = context.data;
  const external = data?.external;
  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isPristine = context.isPristine;
  const validationErrors = context.validationErrors;
  const isInvalid = Utils.isInvalid(validationErrors);
  const hasData = data && data !== {};

  function handleSave(evt) {
    evt.preventDefault();
    send('SAVE');
  }

  function handleDiscard() {
    send('RESET');
  }

  function handleChange({target}) {
    send('UPDATE', {
      data: {
        [target.name]: target.value
      }
    });
  }

  let error = null;
  if (context.error instanceof Array) {
    error = (
        <Alert type="error">
          {UIStrings.USER_ACCOUNT.MESSAGES.UPDATE_ERROR}
          <ul>
            {context.error.map(e => <li key={e.id}>{JSON.stringify(e)}</li>)}
          </ul>
        </Alert>
    );
  }
  else if (context.error) {
    error = (
        <Alert type="error">
          {UIStrings.USER_ACCOUNT.MESSAGES.UPDATE_ERROR}<br/>
          {context.error}
        </Alert>
    );
  }

  return <Section>
    <NxLoadWrapper loading={isLoading}>
      {hasData && <>
        {isSaving && <NxSubmitMask message={UIStrings.SAVING}/>}
        {error}

        <FieldWrapper labelText={UIStrings.USER_ACCOUNT.ID_FIELD_LABEL}>
          <Textfield name="userId" readOnly disabled value={data.userId}/>
        </FieldWrapper>
        <FieldWrapper labelText={UIStrings.USER_ACCOUNT.FIRST_FIELD_LABEL}>
          <Textfield {...buildFieldProps('firstName', data, validationErrors, handleChange)}/>
        </FieldWrapper>
        <FieldWrapper labelText={UIStrings.USER_ACCOUNT.LAST_FIELD_LABEL}>
          <Textfield {...buildFieldProps('lastName', data, validationErrors, handleChange)}/>
        </FieldWrapper>
        <FieldWrapper labelText={UIStrings.USER_ACCOUNT.EMAIL_FIELD_LABEL}>
          <Textfield {...buildFieldProps('email', data, validationErrors, handleChange)}/>
        </FieldWrapper>
        <SectionFooter>
          <Button variant='primary' disabled={external || isPristine || isInvalid} onClick={handleSave}>
            {UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
          </Button>
          <Button disabled={external || isPristine} onClick={handleDiscard}>
            {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
          </Button>
        </SectionFooter>
      </>}
    </NxLoadWrapper>
  </Section>;
}

function buildFieldProps(name, data, validationErrors, handleChange) {
  const readOnly = data.external;
  return {
    name,
    value: data[name],
    disabled: readOnly,
    readOnly: readOnly,
    onChange: handleChange,
    required: !readOnly,
    validationErrors: validationErrors[name]
  };
}
