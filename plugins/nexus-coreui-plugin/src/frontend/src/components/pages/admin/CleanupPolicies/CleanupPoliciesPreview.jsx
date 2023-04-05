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
import React, {useEffect} from 'react';
import {useMachine} from '@xstate/react';

import {
  ListMachineUtils,
  Section,
  SectionToolbar,
  ValidationUtils
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxFilterInput,
  NxFormSelect,
  NxLoadWrapper,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTooltip,
  NxWarningAlert
} from '@sonatype/react-shared-components';
import UIStrings from '../../../../constants/UIStrings';

import CleanupPoliciesPreviewFormMachine from './CleanupPoliciesPreviewFormMachine';
import CleanupPoliciesPreviewListMachine from './CleanupPoliciesPreviewListMachine';

export default function CleanupPoliciesPreview({policyData}) {
  const [formState, sendToForm] = useMachine(CleanupPoliciesPreviewFormMachine, {devTools: true});
  const [listState, sendToList] = useMachine(CleanupPoliciesPreviewListMachine, {devTools: true});

  useEffect(() => {
    sendToForm({type: 'LOAD_REPOSITORIES', format: policyData.format});
    sendToList({type: 'CLEAR_PREVIEW'});
  }, [policyData]);

  const {error: repositoryLoadError, repository, repositories} = formState.context;
  const {error: previewError, data, total, filter, isAlertShown} = listState.context;
  const previewUnavailable = ValidationUtils.isBlank(repository) || (
      ValidationUtils.isBlank(policyData.criteriaLastBlobUpdated) &&
      ValidationUtils.isBlank(policyData.criteriaLastDownloaded) &&
      ValidationUtils.isBlank(policyData.criteriaReleaseType) &&
      ValidationUtils.isBlank(policyData.criteriaAssetRegex)
  );
  const isLoadingRepositories = formState.matches('loading');
  const isLoadingPreview = listState.matches('loading');
  const hasData = data?.length > 0;

  const nameSortDir = ListMachineUtils.getSortDirection('name', listState.context);
  const groupSortDir = ListMachineUtils.getSortDirection('group', listState.context);
  const versionSortDir = ListMachineUtils.getSortDirection('version', listState.context);
  const sortByName = () => sendToList({type: 'SORT_BY_NAME'});
  const sortByGroup = () => sendToList({type: 'SORT_BY_GROUP'});
  const sortByVersion = () => sendToList({type: 'SORT_BY_VERSION'});

  function repositoryChangeHandler(event) {
    sendToForm({type: 'SET_REPOSITORY', repository: event.target.value});
  }

  function retryForm() {
    sendToForm({type: 'LOAD_REPOSITORIES', format: policyData.format});
  }

  function previewHandler() {
    sendToList({type: 'PREVIEW', repository, policyData});
  }

  function retryPreview() {
    sendToList({type: 'RETRY_PREVIEW'});
  }

  function filterPreview(filter) {
    sendToList({type: 'FILTER', filter});
  }

  return <Section className="nxrm-cleanup-policies-preview">
    <NxLoadWrapper loading={isLoadingRepositories} error={repositoryLoadError} retryHandler={retryForm}>
      {() => <>
        <div className="nx-form-group cleanup-preview-repository-group">
          <label id="preview-repository-label" className="nx-label nx-label--optional" htmlFor="repository">
            <span className="nx-label__text">{UIStrings.CLEANUP_POLICIES.PREVIEW.REPOSITORY_LABEL}</span>
          </label>
          <div id="preview-repository-sub-label" className="nx-sub-label">
            {UIStrings.CLEANUP_POLICIES.PREVIEW.REPOSITORY_DESCRIPTION}
          </div>
          <div className="nx-form-row">
            <NxFormSelect
                id="repository"
                name="repository"
                onChange={repositoryChangeHandler}
                value={repository}
                aria-describedby="preview-repository-sub-label"
                disabled={!policyData.format}
            >
              <option value="">{UIStrings.CLEANUP_POLICIES.REPOSITORY_SELECT}</option>
              {repositories.map(({id, name}) =>
                  <option key={id} value={id}>{name}</option>
              )}
            </NxFormSelect>
            <NxTooltip title={previewUnavailable ? UIStrings.CLEANUP_POLICIES.PREVIEW.BUTTON_TOOLTIP : undefined}>
              <NxButton className={previewUnavailable ? 'disabled' : ''} onClick={previewHandler}>
                {UIStrings.CLEANUP_POLICIES.PREVIEW.BUTTON}
              </NxButton>
            </NxTooltip>
          </div>
        </div>
        {isAlertShown &&
            <NxWarningAlert onClose={() => sendToForm({type: 'HIDE_ALERT'})}>
              <p className="nx-p">{UIStrings.CLEANUP_POLICIES.PREVIEW.SAMPLE_WARNING}</p>
              <p className="nx-p">{UIStrings.CLEANUP_POLICIES.PREVIEW.COMPONENT_COUNT(data.length, total)}</p>
            </NxWarningAlert>}
        <SectionToolbar>
          <div className="nxrm-spacer"/>
          <NxFilterInput
              id="filter"
              onChange={filterPreview}
              value={filter}
              placeholder={UIStrings.CLEANUP_POLICIES.FILTER_PLACEHOLDER}/>
        </SectionToolbar>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell onClick={sortByName} isSortable sortDir={nameSortDir}>
                {UIStrings.CLEANUP_POLICIES.PREVIEW.NAME_COLUMN}
              </NxTableCell>
              <NxTableCell onClick={sortByGroup} isSortable sortDir={groupSortDir}>
                {UIStrings.CLEANUP_POLICIES.PREVIEW.GROUP_COLUMN}
              </NxTableCell>
              <NxTableCell onClick={sortByVersion} isSortable sortDir={versionSortDir}>
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
