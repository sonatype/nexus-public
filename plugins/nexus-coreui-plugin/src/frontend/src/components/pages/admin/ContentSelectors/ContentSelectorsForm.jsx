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
import {faTrash} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  Section,
  Textarea,
  Textfield,
  FormUtils,
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxP,
  NxReadOnly,
  NxStatefulForm
} from '@sonatype/react-shared-components';

import ContentSelectorsFormMachine from './ContentSelectorsFormMachine';
import ContentSelectorsPreview from './ContentSelectorsPreview';
import UIStrings from '../../../../constants/UIStrings';

export default function ContentSelectorsForm({itemId, onDone}) {
  const [current, send] = useMachine(ContentSelectorsFormMachine, {
    context: {
      pristineData: {
        name: itemId
      }
    },

    actions: {
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone
    },

    devTools: true
  });

  const {pristineData, data, loadError} = current.context;
  const hasData = data && data !== {};

  function update(event) {
    send('UPDATE', {data: {[event.target.name]: event.target.value}});
  }

  function cancel() {
    onDone();
  }

  function confirmDelete() {
    send('CONFIRM_DELETE');
  }

  return <Page className="nxrm-content-selectors">
    <PageHeader>
      <PageTitle text={Boolean(pristineData.name) ?
          UIStrings.CONTENT_SELECTORS.EDIT_TITLE(pristineData.name) :
          UIStrings.CONTENT_SELECTORS.MENU.text}/>
    </PageHeader>
    <ContentBody>
      <Section className="nxrm-content-selectors-form">
        <NxStatefulForm
            {...FormUtils.formProps(current, send)}
          onCancel={cancel}
          additionalFooterBtns={itemId &&
            <NxButton variant="tertiary" onClick={confirmDelete}>
              <NxFontAwesomeIcon icon={faTrash}/>
              <span>{UIStrings.SETTINGS.DELETE_BUTTON_LABEL}</span>
            </NxButton>
          }
        >
          {hasData && !Boolean(pristineData.name) &&
              <NxFormGroup label={UIStrings.CONTENT_SELECTORS.NAME_LABEL} isRequired={!pristineData.name}>
                <Textfield
                    className="nx-text-input--long"
                    {...FormUtils.fieldProps('name', current)}
                    disabled={pristineData.name}
                    onChange={update}/>
              </NxFormGroup>}
          {hasData && <>
            <NxReadOnly>
              <NxReadOnly.Label>{UIStrings.CONTENT_SELECTORS.TYPE_LABEL}</NxReadOnly.Label>
              <NxReadOnly.Data>{data.type?.toUpperCase()}</NxReadOnly.Data>
            </NxReadOnly>
            <NxFormGroup label={UIStrings.CONTENT_SELECTORS.DESCRIPTION_LABEL}>
              <Textfield
                  className="nx-text-input--long"
                  {...FormUtils.fieldProps('description', current)}
                  onChange={update}/>
            </NxFormGroup>
            <NxFormGroup label={UIStrings.CONTENT_SELECTORS.EXPRESSION_LABEL}
                         sublable={UIStrings.CONTENT_SELECTORS.EXPRESSION_DESCRIPTION}
                         isRequired>
              <Textarea
                  className="nx-text-input--long"
                  {...FormUtils.fieldProps('expression', current)}
                  onChange={update}
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
    </ContentBody>
  </Page>;
}
