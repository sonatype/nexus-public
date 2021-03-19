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
  FieldWrapper,
  NxErrorAlert,
  NxButton,
  NxFontAwesomeIcon,
  NxLoadWrapper,
  NxSubmitMask,
  NxTooltip,
  Page,
  PageHeader,
  PageTitle,
  Section,
  SectionFooter,
  Textarea,
  Textfield,
  Utils
} from '@sonatype/nexus-ui-plugin';

import ContentSelectorsFormMachine from './ContentSelectorsFormMachine';

import UIStrings from '../../../../constants/UIStrings';
import {faScroll, faTrash} from '@fortawesome/free-solid-svg-icons';
import ContentSelectorsPreview from './ContentSelectorsPreview';

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

  const {isPristine, pristineData, data, loadError, saveError, validationErrors} = current.context;
  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isInvalid = Utils.isInvalid(validationErrors);
  const hasData = data && data !== {};

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
    onDone();
  }

  function confirmDelete() {
    send('CONFIRM_DELETE');
  }

  function retry() {
    send('RETRY');
  }

  return <Page className="nxrm-content-selectors">
    <PageHeader><PageTitle icon={faScroll} {...UIStrings.CONTENT_SELECTORS.MENU}/></PageHeader>
    <ContentBody>
      <Section className="nxrm-content-selectors-form" onKeyPress={handleEnter}>
        <NxLoadWrapper loading={isLoading} error={loadError ? `${loadError}` : null} retryHandler={retry}>
          {hasData && <>
            {saveError && <NxErrorAlert>{UIStrings.CONTENT_SELECTORS.MESSAGES.SAVE_ERROR}</NxErrorAlert>}
            {isSaving && <NxSubmitMask message={UIStrings.SAVING}/>}

            <FieldWrapper labelText={UIStrings.CONTENT_SELECTORS.NAME_LABEL}>
              <Textfield
                  className="nx-text-input--long"
                  {...Utils.fieldProps('name', current)}
                  disabled={pristineData.name}
                  onChange={update}/>
            </FieldWrapper>
            <div className="field-wrapper">
              <label id="type" className="nx-label"><span className="nx-label__text">{UIStrings.CONTENT_SELECTORS.TYPE_LABEL}</span></label>
              <p aria-labelledby="type">{data.type?.toUpperCase()}</p>
              <div className="error-spacing"></div>
            </div>
            <FieldWrapper labelText={UIStrings.CONTENT_SELECTORS.DESCRIPTION_LABEL} isOptional>
              <Textfield
                  className="nx-text-input--long"
                  {...Utils.fieldProps('description', current)}
                  onChange={update}/>
            </FieldWrapper>
            <FieldWrapper labelText={UIStrings.CONTENT_SELECTORS.EXPRESSION_LABEL}
                          descriptionText={UIStrings.CONTENT_SELECTORS.EXPRESSION_DESCRIPTION}>
              <Textarea
                  className="nx-text-input--long"
                  {...Utils.fieldProps('expression', current)}
                  onChange={update}
              />
            </FieldWrapper>

            <h4>Example Content Selector Expressions</h4>
            <p>
              Select "raw" format content:
              {' '}<code className="nx-code">format == "raw"</code>
            </p>
            <p>
              Select "maven2" content along a path that starts with "/org":
              {' '}<code className="nx-code">format == "maven2" and path =^ "/org"</code>
            </p>

            <SectionFooter>
              <NxTooltip title={Utils.saveTooltip({isPristine, isInvalid})}>
                <NxButton variant="primary" className={(isPristine || isInvalid) && 'disabled'} onClick={save}
                          type="submit">
                  {UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
                </NxButton>
              </NxTooltip>
              <NxButton onClick={cancel}>{UIStrings.SETTINGS.CANCEL_BUTTON_LABEL}</NxButton>
              {itemId &&
              <NxButton variant="tertiary" onClick={confirmDelete}>
                <NxFontAwesomeIcon icon={faTrash}/>
                <span>{UIStrings.SETTINGS.DELETE_BUTTON_LABEL}</span>
              </NxButton>}
            </SectionFooter>
          </>}
        </NxLoadWrapper>
      </Section>
      {!loadError && <ContentSelectorsPreview type={data?.type} expression={data?.expression}/>}
    </ContentBody>
  </Page>;
}
