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
import React, {useState} from 'react';

import {ExtJS, FormUtils} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxCheckbox,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxFormRow,
  NxInfoAlert,
  NxList,
  NxTextInput,
  NxWarningAlert
} from '@sonatype/react-shared-components';

import {faPlusCircle, faTrashAlt} from '@fortawesome/free-solid-svg-icons';

import UIStrings from '../../../../../constants/UIStrings';

const {RAW: {QUERY_PARAMS}} = UIStrings.REPOSITORIES.EDITOR;

export default function RawQueryParamsConfiguration({parentMachine}) {
  if (!ExtJS.state()?.getValue('rawQueryParamsForwardingEnabled')) {
    return null;
  }

  const [parentState, sendParent] = parentMachine;

  const forwardQueryParameters = parentState.context.data?.raw?.forwardQueryParameters ?? false;
  const excludedQueryParameters = parentState.context.data?.raw?.excludedQueryParameters ?? [];
  const [newExcludedParameter, setNewExcludedParameter] = useState('');

  const handleToggle = () => {
    sendParent({
      type: 'UPDATE',
      name: 'raw.forwardQueryParameters',
      value: !forwardQueryParameters
    });
    // Don't clear exclusions - let user preserve their list
  };

  const handleAddExcludedParameter = () => {
    const trimmed = newExcludedParameter.trim();

    if (trimmed === '') {
      return;
    }

    const isDuplicate = excludedQueryParameters.some(
      (param) => param.toLowerCase() === trimmed.toLowerCase()
    );

    if (isDuplicate) {
      return;
    }

    sendParent({
      type: 'UPDATE',
      name: 'raw.excludedQueryParameters',
      value: [...excludedQueryParameters, trimmed]
    });

    setNewExcludedParameter('');
  };

  const handleRemoveExcludedParameter = (index) => {
    const updated = excludedQueryParameters.filter((_, i) => i !== index);
    sendParent({
      type: 'UPDATE',
      name: 'raw.excludedQueryParameters',
      value: updated
    });
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      handleAddExcludedParameter();
    }
  };

  return (
    <NxFieldset
      label={QUERY_PARAMS.CAPTION}
      sublabel={QUERY_PARAMS.SUBLABEL}
      className="nxrm-form-group-query-params"
    >
      <NxCheckbox
        {...FormUtils.checkboxProps('raw.forwardQueryParameters', parentState)}
        onChange={handleToggle}
        isChecked={forwardQueryParameters}
      >
        {QUERY_PARAMS.CHECKBOX}
      </NxCheckbox>
      <div className="nx-sub-label nxrm-query-params-description">
        {forwardQueryParameters ? QUERY_PARAMS.DESCRIPTION_ENABLED : QUERY_PARAMS.DESCRIPTION}
      </div>

      {forwardQueryParameters && (
        <>
          <NxWarningAlert>
            <p><strong>{QUERY_PARAMS.CACHING_WARNING_TITLE}</strong></p>
            <p>{QUERY_PARAMS.CACHING_WARNING_CONTENT}</p>
          </NxWarningAlert>

          <NxInfoAlert>
            <p><strong>{QUERY_PARAMS.EXAMPLES_TITLE}</strong></p>
            <ul>
              {QUERY_PARAMS.EXAMPLES.map((example, index) => (
                <li key={index}>{example}</li>
              ))}
            </ul>

            <p><strong>{QUERY_PARAMS.USE_CASES_TITLE}</strong></p>
            <ul>
              {QUERY_PARAMS.USE_CASES.map((useCase, index) => (
                <li key={index}>{useCase}</li>
              ))}
            </ul>
          </NxInfoAlert>

          <NxFieldset
            label={QUERY_PARAMS.EXCLUSION_LABEL}
            sublabel={QUERY_PARAMS.EXCLUSION_SUBLABEL}
            className="nxrm-query-params-exclusions"
          >
            <NxFormRow>
              <>
                <NxFormGroup label="">
                  <NxTextInput
                    className="nx-text-input--long"
                    value={newExcludedParameter}
                    onChange={setNewExcludedParameter}
                    onKeyDown={handleKeyDown}
                    placeholder={QUERY_PARAMS.EXCLUSION_PLACEHOLDER}
                  />
                </NxFormGroup>
                <NxButton
                  variant="icon-only"
                  title={newExcludedParameter.trim() ? QUERY_PARAMS.ADD_EXCLUSION : undefined}
                  onClick={handleAddExcludedParameter}
                  disabled={!newExcludedParameter.trim()}
                  type="button"
                >
                  <NxFontAwesomeIcon icon={faPlusCircle} />
                </NxButton>
              </>
            </NxFormRow>

            {excludedQueryParameters.length > 0 && (
              <div className="nx-scrollable nxrm-query-params-exclusion-list">
                <NxList>
                  {excludedQueryParameters.map((param, index) => (
                    <NxList.Item key={index}>
                      <NxList.Text>{param}</NxList.Text>
                      <NxList.Actions>
                        <NxButton
                          variant="icon-only"
                          title={QUERY_PARAMS.REMOVE_EXCLUSION}
                          onClick={() => handleRemoveExcludedParameter(index)}
                          type="button"
                        >
                          <NxFontAwesomeIcon icon={faTrashAlt} />
                        </NxButton>
                      </NxList.Actions>
                    </NxList.Item>
                  ))}
                </NxList>
              </div>
            )}
          </NxFieldset>
        </>
      )}
    </NxFieldset>
  );
}
