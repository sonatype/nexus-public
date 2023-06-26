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
  FieldWrapper,
  FormUtils,
  Page,
  PageHeader,
  PageTitle,
  Section,
  Textfield,
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFormSelect,
  NxStatefulForm
} from '@sonatype/react-shared-components';

import CleanupPoliciesFormMachine from './CleanupPoliciesFormMachine';
import CleanupPoliciesPreview from './CleanupPoliciesPreview';

import UIStrings from '../../../../constants/UIStrings';
import {faTrash} from '@fortawesome/free-solid-svg-icons';

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
    pristineData,
    data,
    loadError,
    criteriaByFormat,
    criteriaLastDownloadedEnabled,
    criteriaLastBlobUpdatedEnabled,
    criteriaReleaseTypeEnabled,
    criteriaAssetRegexEnabled
  } = current.context;
  const isEdit = Boolean(itemId);
  const isLoading = current.matches('loading');
  const hasData = data && data !== {};
  const isPreviewEnabled = ExtJS.state().getValue('nexus.cleanup.preview.enabled');

  function update(event) {
    send({type: 'UPDATE', data: {[event.target.name]: event.target.value}});
  }

  function setCriteriaLastBlobUpdatedEnabled(checked) {
    send({type: 'SET_CRITERIA_LAST_BLOB_UPDATED_ENABLED', checked: checked})
    if (!checked && data.criteriaLastBlobUpdated != null) {
      send({type: 'UPDATE', data: {criteriaLastBlobUpdated: null}});
    }
  }

  function setCriteriaLastDownloadedEnabled(checked) {
    send({type: 'SET_CRITERIA_LAST_DOWNLOADED_ENABLED', checked: checked})
    if (!checked && data.criteriaLastDownloaded != null) {
      send({type: 'UPDATE', data: {criteriaLastDownloaded: null}});
    }
  }

  function setCriteriaReleaseTypeEnabled(checked) {
    send({type: 'SET_CRITERIA_RELEASE_TYPE_ENABLED', checked: checked})
    if (!checked && data.criteriaReleaseType != null) {
      send({type: 'UPDATE', data: {criteriaReleaseType: null}});
    }
  }

  function setCriteriaAssetRegexEnabled(checked) {
    send({type: 'SET_CRITERIA_ASSET_REGEX_ENABLED', checked: checked})
    if (!checked && data.criteriaAssetRegex != null) {
      send({type: 'UPDATE', data: {criteriaAssetRegex: null}});
    }
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
      <Section className="nxrm-cleanup-policies-form">
        <NxStatefulForm
            {...FormUtils.formProps(current, send)}
            onCancel={cancel}
            additionalFooterBtns={itemId &&
              <NxButton type="button" variant="tertiary" onClick={confirmDelete}>
                <NxFontAwesomeIcon icon={faTrash}/>
                <span>{UIStrings.SETTINGS.DELETE_BUTTON_LABEL}</span>
              </NxButton>
            }
        >
          {hasData && <>
            <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.NAME_LABEL}
                          descriptionText={UIStrings.CLEANUP_POLICIES.NAME_DESCRIPTION}
                          id="cleanup-name-group">
              <Textfield
                  {...FormUtils.fieldProps('name', current)}
                  disabled={pristineData.name}
                  onChange={update}/>
            </FieldWrapper>
            <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.FORMAT_LABEL}
                          descriptionText={UIStrings.CLEANUP_POLICIES.FORMAT_DESCRIPTION}
                          id="cleanup-format-group">
              <NxFormSelect
                  {...FormUtils.fieldProps('format', current)}
                  name="format"
                  onChange={update}
                  value={data.format}>
                <option value="">{UIStrings.CLEANUP_POLICIES.FORMAT_SELECT}</option>
                {criteriaByFormat?.map(formatCriteria =>
                    <option key={formatCriteria.id} value={formatCriteria.id}>{formatCriteria.name}</option>
                )}
              </NxFormSelect>
            </FieldWrapper>
            <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.NOTES_LABEL}
                          id="cleanup-notes-group" isOptional>
              <Textfield
                  {...FormUtils.fieldProps('notes', current)}
                  onChange={update}/>
            </FieldWrapper>
            {isAnyFieldApplicable() &&

            <NxFieldset label={UIStrings.CLEANUP_POLICIES.CRITERIA_LABEL} isRequired>
              {isFieldApplicable('lastBlobUpdated') &&
              <CheckboxControlledWrapper isChecked={Boolean(criteriaLastBlobUpdatedEnabled)}
                                         onChange={setCriteriaLastBlobUpdatedEnabled}
                                         id="criteria-last-blob-updated-group"
                                         title={UIStrings.CLEANUP_POLICIES.LAST_UPDATED_CHECKBOX_TITLE(
                                             criteriaLastBlobUpdatedEnabled
                                         )}>
                <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.LAST_UPDATED_LABEL}
                              descriptionText={UIStrings.CLEANUP_POLICIES.LAST_UPDATED_DESCRIPTION}
                              isOptional={!criteriaLastBlobUpdatedEnabled}>
                  <Textfield
                      {...FormUtils.fieldProps('criteriaLastBlobUpdated', current)}
                      onChange={update}
                      disabled={!criteriaLastBlobUpdatedEnabled}/>
                  <div className="suffix">
                    {UIStrings.CLEANUP_POLICIES.LAST_UPDATED_SUFFIX}
                  </div>
                </FieldWrapper>
              </CheckboxControlledWrapper>
              }
              {isFieldApplicable('lastDownloaded') &&
              <CheckboxControlledWrapper isChecked={Boolean(criteriaLastDownloadedEnabled)}
                                         onChange={setCriteriaLastDownloadedEnabled}
                                         id="criteria-last-downloaded-group"
                                         title={UIStrings.CLEANUP_POLICIES.LAST_DOWNLOADED_CHECKBOX_TITLE(
                                             criteriaLastDownloadedEnabled
                                         )}>
                <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.LAST_DOWNLOADED_LABEL}
                              descriptionText={UIStrings.CLEANUP_POLICIES.LAST_DOWNLOADED_DESCRIPTION}
                              isOptional={!criteriaLastDownloadedEnabled}>
                  <Textfield
                      {...FormUtils.fieldProps('criteriaLastDownloaded', current)}
                      onChange={update}
                      disabled={!criteriaLastDownloadedEnabled}/>
                  <div className="suffix">
                    {UIStrings.CLEANUP_POLICIES.LAST_DOWNLOADED_SUFFIX}
                  </div>
                </FieldWrapper>
              </CheckboxControlledWrapper>
              }
              {isFieldApplicable('isPrerelease') &&
              <CheckboxControlledWrapper isChecked={Boolean(criteriaReleaseTypeEnabled)}
                                         onChange={setCriteriaReleaseTypeEnabled}
                                         id="criteria-release-type-group"
                                         title={UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_CHECKBOX_TITLE(
                                             criteriaReleaseTypeEnabled
                                         )}>
                <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_LABEL}
                              descriptionText={UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_DESCRIPTION}
                              isOptional={!criteriaReleaseTypeEnabled}>
                  <NxFormSelect
                      {...FormUtils.fieldProps('criteriaReleaseType', current)}
                      onChange={update}
                      disabled={!criteriaReleaseTypeEnabled}>
                    <option value="">{UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_SELECT}</option>
                    <option key="RELEASES" value="RELEASES">{UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_RELEASE}</option>
                    <option key="PRERELEASES"
                            value="PRERELEASES">{UIStrings.CLEANUP_POLICIES.RELEASE_TYPE_PRERELEASE}</option>
                  </NxFormSelect>
                </FieldWrapper>
              </CheckboxControlledWrapper>
              }
              {isFieldApplicable('regex') &&
              <CheckboxControlledWrapper isChecked={Boolean(criteriaAssetRegexEnabled)}
                                         onChange={setCriteriaAssetRegexEnabled}
                                         id="criteria-asset-name-group"
                                         title={UIStrings.CLEANUP_POLICIES.ASSET_NAME_CHECKBOX_TITLE(
                                             criteriaAssetRegexEnabled
                                         )}>
                <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.ASSET_NAME_LABEL}
                              descriptionText={UIStrings.CLEANUP_POLICIES.ASSET_NAME_DESCRIPTION}
                              isOptional={!criteriaAssetRegexEnabled}>
                  <Textfield
                      {...FormUtils.fieldProps('criteriaAssetRegex', current)}
                      onChange={update}
                      disabled={!criteriaAssetRegexEnabled}/>
                </FieldWrapper>
              </CheckboxControlledWrapper>
              }
            </NxFieldset>
            }
          </>}
        </NxStatefulForm>
      </Section>

      {!isPreviewEnabled && !isLoading && !loadError && hasData && <CleanupPoliciesPreview policyData={data}/>}
    </ContentBody>
  </Page>;
}
