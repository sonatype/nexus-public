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
  ListMachineUtils,
  NxButton,
  NxFilterInput,
  NxFormGroup,
  NxLoadWrapper,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxWarningAlert,
  Section, SectionToolbar,
  Select,
  ValidationUtils
} from '@sonatype/nexus-ui-plugin';
import UIStrings from "../../../../constants/UIStrings";

import CleanupPoliciesPreviewMachine from './CleanupPoliciesPreviewMachine';

export default function CleanupPoliciesPreview({policyData}) {
  const [current, send] = useMachine(CleanupPoliciesPreviewMachine, {
    devTools: true
  });

  const {
    repositories, 
    data, 
    filter : filterText, 
    formError, 
    previewError, 
    repository, 
    total,
    isAlertShown
  } = current.context;
  const previewUnavailable = ValidationUtils.isBlank(repository) || (
      ValidationUtils.isBlank(policyData.criteriaLastBlobUpdated) &&
      ValidationUtils.isBlank(policyData.criteriaLastDownloaded) &&
      ValidationUtils.isBlank(policyData.criteriaReleaseType) &&
      ValidationUtils.isBlank(policyData.criteriaAssetRegex)
  );
  const isLoadingForm = current.matches('form.loading');
  const isLoadingPreview = current.matches('preview.loading');
  const hasData = data?.length > 0;

  const nameSortDir = ListMachineUtils.getSortDirection('name', current.context);
  const groupSortDir = ListMachineUtils.getSortDirection('group', current.context);
  const versionSortDir = ListMachineUtils.getSortDirection('version', current.context);

  const repositoryChangeHandler = (event) => send({type: 'SET_REPOSITORY', repository: event.target.value});
  const previewHandler = () => send({type: 'PREVIEW', policyData});

  function filter(value) {
    send({type: 'FILTER', filter: value, policyData});
  }

  function retryForm() {
    send({type: 'RETRY_FORM'});
  }

  function retryPreview() {
    send({type: 'RETRY_PREVIEW'});
  }

  return <Section className="nxrm-cleanup-policies-preview">
    <h2>{UIStrings.CLEANUP_POLICIES.TITLE}</h2>
    <NxLoadWrapper loading={isLoadingForm} error={formError} retryHandler={retryForm}>
      {() => <>
        <div className="nx-form-group">
          <label id="preview-repository-label" className="nx-label" htmlFor="repository">
            <span class="nx-label__text">{UIStrings.CLEANUP_POLICIES.PREVIEW.REPOSITORY_LABEL}</span>
          </label>
          <div id="preview-repository-sub-label" className="nx-sub-label">
            {UIStrings.CLEANUP_POLICIES.PREVIEW.REPOSITORY_DESCRIPTION}
          </div>
          <div className="nx-form-row">
            <Select name="repository" onChange={repositoryChangeHandler} aria-describedby="preview-repository-sub-label">
              <option value="">{UIStrings.CLEANUP_POLICIES.REPOSITORY_SELECT}</option>
              {repositories.map(({id, name}) =>
                  <option key={id} value={id}>{name}</option>
              )}
            </Select>
            <NxButton disabled={previewUnavailable} onClick={previewHandler}>{UIStrings.CLEANUP_POLICIES.PREVIEW.BUTTON}</NxButton>
          </div>
        </div>
        {isAlertShown && <NxWarningAlert onClose={() => send({type: 'HIDE_ALERT'})}>
              <p className="nx-p">{UIStrings.CLEANUP_POLICIES.PREVIEW.SAMPLE_WARNING}</p>
              <p className="nx-p">{UIStrings.CLEANUP_POLICIES.PREVIEW.COMPONENT_COUNT(data.length, total)}</p>  
        </NxWarningAlert>}
        <SectionToolbar>
          <div className="nxrm-spacer"/>
          <NxFilterInput
              id="filter"
              onChange={filter}
              value={filterText}
              placeholder={UIStrings.CLEANUP_POLICIES.FILTER_PLACEHOLDER}/>
        </SectionToolbar>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell onClick={() => send({type: 'SORT_BY_NAME'})} isSortable sortDir={nameSortDir}>
                {UIStrings.CLEANUP_POLICIES.PREVIEW.NAME_COLUMN}
              </NxTableCell>
              <NxTableCell onClick={() => send({type: 'SORT_BY_GROUP'})} isSortable sortDir={groupSortDir}>
                {UIStrings.CLEANUP_POLICIES.PREVIEW.GROUP_COLUMN}
              </NxTableCell>
              <NxTableCell onClick={() => send({type: 'SORT_BY_VERSION'})} isSortable sortDir={versionSortDir}>
                {UIStrings.CLEANUP_POLICIES.PREVIEW.VERSION_COLUMN}
              </NxTableCell>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoadingPreview}
                       error={previewError}
                       retryHandler={retryPreview}
                       emptyMessage={UIStrings.CLEANUP_POLICIES.PREVIEW.EMPTY}>
            {hasData && data?.map(({name, group, version, repository}) =>
                <NxTableRow key={`${name}${group}${version}${repository}`}>
                  <NxTableCell>{name}</NxTableCell>
                  <NxTableCell>{group}</NxTableCell>
                  <NxTableCell>{version}</NxTableCell>
                </NxTableRow>
            )}
          </NxTableBody>
        </NxTable>
      </>}
    </NxLoadWrapper>
  </Section>;
}
