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
  Select,
  Textfield,
  Utils
} from '@sonatype/nexus-ui-plugin';

import CleanupPoliciesFormMachine from './CleanupPoliciesFormMachine';

import UIStrings from '../../../../constants/UIStrings';
import {faBroom, faTrash} from '@fortawesome/free-solid-svg-icons';
import CleanupPoliciesPreview from './CleanupPoliciesPreview';

import './CleanupPolicies.scss';

export default function CleanupPoliciesForm({itemId, onDone}) {
  const [current, send] = useMachine(CleanupPoliciesFormMachine, {
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

  const {
    isPristine,
    pristineData,
    data,
    loadError,
    saveError,
    validationErrors,
    criteriaByFormat,
    criteriaLastDownloadedEnabled,
    criteriaLastBlobUpdatedEnabled,
    criteriaReleaseTypeEnabled,
    criteriaAssetRegexEnabled
  } = current.context;
  const isEdit = Boolean(itemId);
  const isLoading = current.matches('loading') || current.matches('loadingFormatCriteria');
  const isSaving = current.matches('saving');
  const isInvalid = Utils.isInvalid(validationErrors);
  const hasData = data && data !== {};

  function update(event) {
    send({type: 'UPDATE', data: {[event.target.name]: event.target.value}});
  }

  function setCriteriaLastBlobUpdatedEnabled(checked) {
    send({type: 'SET_CRITERIA_LAST_BLOB_UPDATED_ENABLED', checked: checked})
    if (!checked && data.criteriaLastBlobUpdated) {
      send({type: 'UPDATE', data: {criteriaLastBlobUpdated: null}});
    }
  }

  function setCriteriaLastDownloadedEnabled(checked) {
    send({type: 'SET_CRITERIA_LAST_DOWNLOADED_ENABLED', checked: checked})
    if (!checked && data.criteriaLastDownloaded) {
      send({type: 'UPDATE', data: {criteriaLastDownloaded: null}});
    }
  }

  function setCriteriaReleaseTypeEnabled(checked) {
    send({type: 'SET_CRITERIA_RELEASE_TYPE_ENABLED', checked: checked})
    if (!checked && data.criteriaReleaseType) {
      send({type: 'UPDATE', data: {criteriaReleaseType: null}});
    }
  }

  function setCriteriaAssetRegexEnabled(checked) {
    send({type: 'SET_CRITERIA_ASSET_REGEX_ENABLED', checked: checked})
    if (!checked && data.criteriaAssetRegex) {
      send({type: 'UPDATE', data: {criteriaAssetRegex: null}});
    }
  }

  function save(event) {
    event.preventDefault();
    send({type: 'SAVE'});
  }

  function handleEnter(event) {
    if (event.key === 'Enter') {
      save(event);
    }
  }

  function retry() {
    send({type: 'RETRY'});
  }

  function cancel() {
    onDone();
  }

  function confirmDelete() {
    send({type: 'CONFIRM_DELETE'});
  }

  function isAnyFieldApplicable() {
    return isFieldApplicable('lastBlobUpdated') || isFieldApplicable('lastDownloaded') ||
        isFieldApplicable('isPrerelease') || isFieldApplicable('regex');
  }

  function isFieldApplicable(fieldId) {
    return criteriaByFormat?.some(
        ({id, availableCriteria}) => id === data.format && availableCriteria.includes(fieldId));
  }

  return <Page className="nxrm-cleanup-policies">
    <PageHeader>
      <PageTitle text={isEdit ? UIStrings.CLEANUP_POLICIES.EDIT_TITLE : UIStrings.CLEANUP_POLICIES.CREATE_TITLE}/>
    </PageHeader>
    <ContentBody>
      <Section className="nxrm-cleanup-policies-form" onKeyPress={handleEnter}>
        <NxLoadWrapper loading={isLoading} error={loadError ? `${loadError}` : null} retryHandler={retry}>
          {hasData && <>
            {saveError && <NxErrorAlert>{UIStrings.CLEANUP_POLICIES.MESSAGES.SAVE_ERROR}</NxErrorAlert>}
            {isSaving && <NxSubmitMask message={UIStrings.SAVING}/>}

            <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.NAME_LABEL}
                          descriptionText={UIStrings.CLEANUP_POLICIES.NAME_DESCRIPTION}
                          id="cleanup-name-group">
              <Textfield
                  {...Utils.fieldProps('name', current)}
                  disabled={pristineData.name}
                  onChange={update}/>
            </FieldWrapper>
            <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.FORMAT_LABEL}
                          descriptionText={UIStrings.CLEANUP_POLICIES.FORMAT_DESCRIPTION}
                          id="cleanup-format-group">
              <Select
                  {...Utils.fieldProps('format', current)}
                  name="format"
                  onChange={update}
                  value={data.format}>
                <option value="">{UIStrings.CLEANUP_POLICIES.FORMAT_SELECT}</option>
                {criteriaByFormat?.map(formatCriteria =>
                    <option key={formatCriteria.id} value={formatCriteria.id}>{formatCriteria.name}</option>
                )}
              </Select>
            </FieldWrapper>
            <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.NOTES_LABEL}
                          id="cleanup-notes-group">
              <Textfield
                  {...Utils.fieldProps('notes', current)}
                  onChange={update}/>
            </FieldWrapper>
            {isAnyFieldApplicable() &&
            <fieldset className="nx-fieldset">
              <legend className="nx-label nx-legend__text">
                {UIStrings.CLEANUP_POLICIES.CRITERIA_LABEL}
              </legend>
              {isFieldApplicable('lastBlobUpdated') &&
              <CheckboxControlledWrapper isChecked={Boolean(data.criteriaLastBlobUpdated)}
                                         onChange={setCriteriaLastBlobUpdatedEnabled}
                                         id="criteria-last-blob-updated-group">
                <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.LAST_UPDATED_LABEL}
                              descriptionText={UIStrings.CLEANUP_POLICIES.LAST_UPDATED_DESCRIPTION}>
                  <Textfield
                      {...Utils.fieldProps('criteriaLastBlobUpdated', current)}
                      onChange={update}
                      disabled={!criteriaLastBlobUpdatedEnabled}/>
                  <div className="suffix">
                    {UIStrings.CLEANUP_POLICIES.LAST_UPDATED_SUFFIX}
                  </div>
                </FieldWrapper>
              </CheckboxControlledWrapper>
              }
              {isFieldApplicable('lastDownloaded') &&
              <CheckboxControlledWrapper isChecked={Boolean(data.criteriaLastDownloaded)}
                                         onChange={setCriteriaLastDownloadedEnabled}
                                         id="criteria-last-downloaded-group">
                <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.LAST_DOWNLOADED_LABEL}
                              descriptionText={UIStrings.CLEANUP_POLICIES.LAST_DOWNLOADED_DESCRIPTION}>
                  <Textfield
                      {...Utils.fieldProps('criteriaLastDownloaded', current)}
                      onChange={update}
                      disabled={!criteriaLastDownloadedEnabled}/>
                  <div className="suffix">
                    {UIStrings.CLEANUP_POLICIES.LAST_DOWNLOADED_SUFFIX}
                  </div>
                </FieldWrapper>
              </CheckboxControlledWrapper>
              }
              {isFieldApplicable('isPrerelease') &&
              <CheckboxControlledWrapper isChecked={Boolean(data.criteriaReleaseType)}
                                         onChange={setCriteriaReleaseTypeEnabled}
                                         id="criteria-release-type-group">
                <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_LABEL}
                              descriptionText={UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_DESCRIPTION}>
                  <Select
                      {...Utils.fieldProps('criteriaReleaseType', current)}
                      onChange={update}
                      disabled={!criteriaReleaseTypeEnabled}>
                    <option value="">{UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_SELECT}</option>
                    <option key="RELEASES" value="RELEASES">{UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_RELEASE}</option>
                    <option key="PRERELEASES"
                            value="PRERELEASES">{UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_PRERELEASE}</option>
                  </Select>
                </FieldWrapper>
              </CheckboxControlledWrapper>
              }
              {isFieldApplicable('regex') &&
              <CheckboxControlledWrapper isChecked={Boolean(data.criteriaAssetRegex)}
                                         onChange={setCriteriaAssetRegexEnabled}
                                         id="criteria-asset-name-group">
                <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.ASSET_NAME_LABEL}
                              descriptionText={UIStrings.CLEANUP_POLICIES.ASSET_NAME_DESCRIPTION}>
                  <Textfield
                      {...Utils.fieldProps('criteriaAssetRegex', current)}
                      onChange={update}
                      disabled={!criteriaAssetRegexEnabled}/>
                </FieldWrapper>
              </CheckboxControlledWrapper>
              }
            </fieldset>
            }
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
      {!isLoading && !loadError && hasData && <CleanupPoliciesPreview policyData={data}/>}
    </ContentBody>
  </Page>;
}
