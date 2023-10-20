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
  CheckboxControlledWrapper,
  ContentBody,
  ExtJS,
  FormUtils,
  Page,
  PageHeader,
  PageTitle,
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxFontAwesomeIcon,
  NxFormSelect,
  NxStatefulForm,
  NxH2,
  NxP,
  NxFormGroup,
  NxTile,
  NxTextInput,
  NxFieldset
} from '@sonatype/react-shared-components';

import CleanupPoliciesFormMachine from './CleanupPoliciesFormMachine';
import CleanupPoliciesPreview from './CleanupPoliciesPreview';
import CleanupPoliciesDryRun from './CleanupPoliciesDryRun';
import CleanupExclusionCriteria from './CleanupExclusionCriteria';

import UIStrings from '../../../../constants/UIStrings';
import {faTrash} from '@fortawesome/free-solid-svg-icons';

import './CleanupPolicies.scss';
import {isEmpty} from 'ramda';

const {CLEANUP_POLICIES: LABELS} = UIStrings;

export default function CleanupPoliciesForm({itemId, onDone}) {
  const [state, send, actor] = useMachine(CleanupPoliciesFormMachine, {
    context: {
      pristineData: {
        name: itemId,
      },
    },

    actions: {
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone,
    },

    devTools: true,
  });

  const {
    pristineData,
    data,
    isTouched,
    loadError,
    criteriaByFormat,
    criteriaLastDownloadedEnabled,
    criteriaLastBlobUpdatedEnabled,
    criteriaAssetRegexEnabled,
    validationErrors: {criteriaSelected}
  } = state.context;

  const retainSupportedFormats = ['maven2' , 'docker'];

  const isEdit = Boolean(itemId);
  const isLoading = state.matches('loading');
  const hasData = !isEmpty(data);
  const isCriteriaFieldSetPristine = !isTouched.criteriaLastBlobUpdated &&
      !isTouched.criteriaLastDownloaded &&
      !isTouched.criteriaAssetRegex &&
      !isTouched.retain;
  const isPreviewEnabled =
    ExtJS.state().getValue('nexus.datastore.enabled') &&
    ExtJS.state().getValue('nexus.cleanup.preview.enabled');

  const showRetainN = isRetainSupportedFormat(data.format) && isRetainEnabled(data.format);

  function isRetainEnabled(format) {
    return ExtJS.state()
        .getValue(`nexus.cleanup.${format}Retain`);
  }

  function isRetainSupportedFormat(format) {
    return retainSupportedFormats.includes(format);
  }

  function setCriteriaLastBlobUpdatedEnabled(checked) {
    send({type: 'SET_CRITERIA_LAST_BLOB_UPDATED_ENABLED', checked});
  }

  function setCriteriaLastDownloadedEnabled(checked) {
    send({type: 'SET_CRITERIA_LAST_DOWNLOADED_ENABLED', checked});
  }

  function setCriteriaAssetRegexEnabled(checked) {
    send({type: 'SET_CRITERIA_ASSET_REGEX_ENABLED', checked});
  }

  function confirmDelete() {
    send({type: 'CONFIRM_DELETE'});
  }

  function isAnyFieldApplicable() {
    return (
      isFieldApplicable('lastBlobUpdated') ||
      isFieldApplicable('lastDownloaded') ||
      isFieldApplicable('regex')
    );
  }

  function isFieldApplicable(fieldId) {
    return criteriaByFormat?.some(
      ({id, availableCriteria}) =>
        id === data.format && availableCriteria.includes(fieldId)
    );
  }

  function setReleaseType(event) {
    send({type: 'UPDATE_RELEASE_TYPE', value: event.target.value});
  }

  return (
    <Page className="nxrm-cleanup-policies">
      <PageHeader>
        <PageTitle text={isEdit ? LABELS.EDIT_TITLE : LABELS.CREATE_TITLE} />
      </PageHeader>
      <ContentBody>
        <NxTile className="nxrm-cleanup-policies-form">
          <NxH2>{LABELS.SUB_TITLE}</NxH2>
          <NxP>{LABELS.DESCRIPTION}</NxP>
          <NxStatefulForm
            {...FormUtils.formProps(state, send)}
            onCancel={onDone}
            additionalFooterBtns={
              itemId && (
                <NxButton
                  type="button"
                  variant="tertiary"
                  onClick={confirmDelete}
                >
                  <NxFontAwesomeIcon icon={faTrash} />
                  <span>{UIStrings.SETTINGS.DELETE_BUTTON_LABEL}</span>
                </NxButton>
              )
            }
          >
            {hasData && (
              <>
                <NxFormGroup
                  label={LABELS.NAME_LABEL}
                  sublabel={LABELS.NAME_DESCRIPTION}
                  isRequired
                >
                  <NxTextInput
                    {...FormUtils.fieldProps('name', state)}
                    disabled={pristineData.name}
                    onChange={FormUtils.handleUpdate('name', send)}
                    className="nx-text-input--long"
                  />
                </NxFormGroup>
                <NxFormGroup
                  label={LABELS.FORMAT_LABEL}
                  sublabel={LABELS.FORMAT_DESCRIPTION}
                  isRequired
                >
                  <NxFormSelect
                    {...FormUtils.fieldProps('format', state)}
                    onChange={FormUtils.handleUpdate('format', send)}
                  >
                    <option value="">{LABELS.FORMAT_SELECT}</option>
                    {criteriaByFormat?.map((formatCriteria) => (
                      <option key={formatCriteria.id} value={formatCriteria.id}>
                        {formatCriteria.name}
                      </option>
                    ))}
                  </NxFormSelect>
                </NxFormGroup>
                <NxFormGroup label={LABELS.DESCRIPTION_LABEL}>
                  <NxTextInput
                    {...FormUtils.fieldProps('notes', state)}
                    onChange={FormUtils.handleUpdate('notes', send)}
                    className="nx-text-input--long"
                    type="textarea"
                  />
                </NxFormGroup>
                {isFieldApplicable('isPrerelease') && (
                    <NxFormGroup
                        label={LABELS.RELEASE_TYPE_LABEL}
                        sublabel={LABELS.RELEASE_TYPE_SELECT}
                    >
                      <NxFormSelect
                          {...FormUtils.fieldProps(
                              'criteriaReleaseType',
                              state
                          )}
                          onChange={setReleaseType}
                          validatable
                          className="nx-form-select--long"
                      >
                        {Object.keys(LABELS.RELEASE_TYPE).map((type) => {
                          const item = LABELS.RELEASE_TYPE[type];
                          return (
                              <option key={item.id} value={item.id}>
                                {item.label}
                              </option>
                          );
                        })}
                      </NxFormSelect>
                    </NxFormGroup>
                )}
                {isAnyFieldApplicable() && (
                  <>
                    <NxFieldset
                        className="criteria-fieldset"
                        label={LABELS.CRITERIA_LABEL}
                        sublabel={`${LABELS.CRITERIA_DESCRIPTION}. ${LABELS.MESSAGES.NO_CRITERIA_ERROR}`}
                        isPristine={isCriteriaFieldSetPristine}
                        validationErrors={criteriaSelected}
                        isRequired
                    >
                      {isFieldApplicable('lastBlobUpdated') && (
                          <CheckboxControlledWrapper
                              isChecked={Boolean(criteriaLastBlobUpdatedEnabled)}
                              onChange={setCriteriaLastBlobUpdatedEnabled}
                              id="criteria-last-blob-updated-group"
                              title={LABELS.LAST_UPDATED_CHECKBOX_TITLE(
                                  criteriaLastBlobUpdatedEnabled
                              )}
                          >
                            <NxFormGroup
                                label={LABELS.LAST_UPDATED_LABEL}
                                sublabel={LABELS.LAST_UPDATED_SUB_LABEL}
                                isRequired={criteriaLastBlobUpdatedEnabled}
                            >
                              <NxTextInput
                                  {...FormUtils.fieldProps(
                                      'criteriaLastBlobUpdated',
                                      state
                                  )}
                                  onChange={FormUtils.handleUpdate(
                                      'criteriaLastBlobUpdated',
                                      send
                                  )}
                                  placeholder={LABELS.PLACEHOLDER}
                                  disabled={!criteriaLastBlobUpdatedEnabled}
                                  className="nx-text-input--short"
                              />
                            </NxFormGroup>
                          </CheckboxControlledWrapper>
                      )}
                      {isFieldApplicable('lastDownloaded') && (
                          <CheckboxControlledWrapper
                              isChecked={Boolean(criteriaLastDownloadedEnabled)}
                              onChange={setCriteriaLastDownloadedEnabled}
                              id="criteria-last-downloaded-group"
                              title={LABELS.LAST_DOWNLOADED_CHECKBOX_TITLE(
                                  criteriaLastDownloadedEnabled
                              )}
                          >
                            <NxFormGroup
                                label={LABELS.LAST_DOWNLOADED_LABEL}
                                isRequired={criteriaLastDownloadedEnabled}
                                sublabel={LABELS.LAST_DOWNLOADED_SUB_LABEL}
                            >
                              <NxTextInput
                                  {...FormUtils.fieldProps(
                                      'criteriaLastDownloaded',
                                      state
                                  )}
                                  onChange={FormUtils.handleUpdate(
                                      'criteriaLastDownloaded',
                                      send
                                  )}
                                  placeholder={LABELS.PLACEHOLDER}
                                  disabled={!criteriaLastDownloadedEnabled}
                                  className="nx-text-input--short"
                              />
                            </NxFormGroup>
                          </CheckboxControlledWrapper>
                      )}
                      {isFieldApplicable('regex') && (
                          <CheckboxControlledWrapper
                              isChecked={Boolean(criteriaAssetRegexEnabled)}
                              onChange={setCriteriaAssetRegexEnabled}
                              id="criteria-asset-name-group"
                              title={LABELS.ASSET_NAME_CHECKBOX_TITLE(
                                  criteriaAssetRegexEnabled
                              )}
                          >
                            <NxFormGroup
                                label={LABELS.ASSET_NAME_LABEL}
                                sublabel={LABELS.ASSET_NAME_DESCRIPTION}
                                isRequired={criteriaAssetRegexEnabled}
                            >
                              <NxTextInput
                                  {...FormUtils.fieldProps(
                                      'criteriaAssetRegex',
                                      state
                                  )}
                                  onChange={FormUtils.handleUpdate(
                                      'criteriaAssetRegex',
                                      send
                                  )}
                                  disabled={!criteriaAssetRegexEnabled}
                                  className="nx-text-input--long"
                              />
                            </NxFormGroup>
                          </CheckboxControlledWrapper>
                      )}
                      {showRetainN && (
                          <CleanupExclusionCriteria actor={actor}/>
                      )}
                    </NxFieldset>
                    {isPreviewEnabled && (
                      <CleanupPoliciesDryRun policyData={data} />
                    )}
                  </>
                )}
              </>
            )}
          </NxStatefulForm>
        </NxTile>

        {!isLoading && !loadError && hasData && !isPreviewEnabled && (
          <CleanupPoliciesPreview policyData={data} />
        )}
      </ContentBody>
    </Page>
  );
}
