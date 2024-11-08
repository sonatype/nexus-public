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
import {faTrash} from '@fortawesome/free-solid-svg-icons';

import {
  Section,
  FormUtils,
  ExtJS
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxP,
  NxReadOnly,
  NxStatefulForm,
  NxTextInput
} from '@sonatype/react-shared-components';

import ContentSelectorsPreview from './ContentSelectorsPreview';
import UIStrings from '../../../../constants/UIStrings';

export default function ContentSelectorsForm({service, onDone}) {
  const stateMachine = useActor(service);
  const [state, send] = stateMachine;

  const {pristineData, data, loadError} = state.context;
  const hasData = data && data !== {};
  const canDelete = ExtJS.checkPermission('nexus:selectors:delete');
  const isEdit = Boolean(pristineData.name);

  function cancel() {
    onDone();
  }

  function confirmDelete() {
    send({type: 'CONFIRM_DELETE'});
  }
  
  return <>
    <Section className="nxrm-content-selectors-form">
      <NxStatefulForm
          {...FormUtils.formProps(state, send)}
          onCancel={cancel}
          additionalFooterBtns={isEdit && canDelete &&
              <NxButton type="button" variant="tertiary" onClick={confirmDelete}>
                <NxFontAwesomeIcon icon={faTrash}/>
                <span>{UIStrings.SETTINGS.DELETE_BUTTON_LABEL}</span>
              </NxButton>
          }
      >
        {hasData && !Boolean(pristineData.name) &&
            <NxFormGroup label={UIStrings.CONTENT_SELECTORS.NAME_LABEL} isRequired={!pristineData.name}>
              <NxTextInput
                  className="nx-text-input--long"
                  {...FormUtils.fieldProps('name', state)}
                  disabled={pristineData.name}
                  onChange={FormUtils.handleUpdate('name', send)}/>
            </NxFormGroup>}
        {hasData && <>
          <NxReadOnly>
            <NxReadOnly.Label>{UIStrings.CONTENT_SELECTORS.TYPE_LABEL}</NxReadOnly.Label>
            <NxReadOnly.Data>{data.type?.toUpperCase()}</NxReadOnly.Data>
          </NxReadOnly>
          <NxFormGroup label={UIStrings.CONTENT_SELECTORS.DESCRIPTION_LABEL}>
            <NxTextInput
                className="nx-text-input--long"
                {...FormUtils.fieldProps('description', state)}
                onChange={FormUtils.handleUpdate('description', send)}/>
          </NxFormGroup>
          <NxFormGroup label={UIStrings.CONTENT_SELECTORS.EXPRESSION_LABEL}
                       sublabel={UIStrings.CONTENT_SELECTORS.EXPRESSION_DESCRIPTION}
                       isRequired>
            <NxTextInput
                type="textarea"
                className="nx-text-input--long"
                {...FormUtils.fieldProps('expression', state)}
                onChange={FormUtils.handleUpdate('expression', send)}
            />
          </NxFormGroup>

          <h4>{UIStrings.CONTENT_SELECTORS.EXPRESSION_EXAMPLES}</h4>
          <NxP>
            {UIStrings.CONTENT_SELECTORS.RAW_EXPRESSION_EXAMPLE_LABEL}:
            {' '}<code className="nx-code">format == "raw"</code>
          </NxP>
          <NxP>
            {UIStrings.CONTENT_SELECTORS.MULTI_EXPRESSIONS_EXAMPLE_LABEL}:
            {' '}<code className="nx-code">format == "maven2" and path =^ "/org"</code>
          </NxP>
        </>}
      </NxStatefulForm>
    </Section>
    {!loadError && <ContentSelectorsPreview type={data?.type} expression={data?.expression}/>}
  </>;
}
