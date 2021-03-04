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
  FieldWrapper,
  NxButton,
  NxFilterInput,
  NxLoadWrapper,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  Section, SectionToolbar,
  Select,
  Utils
} from '@sonatype/nexus-ui-plugin';
import UIStrings from "../../../../constants/UIStrings";

import CleanupPoliciesPreviewMachine from './CleanupPoliciesPreviewMachine';

export default function CleanupPoliciesPreview({policyData}) {
  const [current, send] = useMachine(CleanupPoliciesPreviewMachine, {
    devTools: true
  });

  const {repositories, data, filter : filterText, formError, previewError, repository} = current.context;
  const previewUnavailable = Utils.isBlank(repository) || (Utils.isBlank(policyData.criteriaLastBlobUpdated) &&
      Utils.isBlank(policyData.criteriaLastDownloaded) && Utils.isBlank(policyData.criteriaReleaseType) &&
      Utils.isBlank(policyData.criteriaAssetRegex));
  const isLoadingForm = current.matches('form.loading');
  const isLoadingPreview = current.matches('preview.loading');
  const hasData = data?.length > 0;

  const nameSortDir = Utils.getSortDirection('name', current.context);
  const groupSortDir = Utils.getSortDirection('group', current.context);
  const versionSortDir = Utils.getSortDirection('version', current.context);

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
        <FieldWrapper labelText={UIStrings.CLEANUP_POLICIES.PREVIEW.REPOSITORY_LABEL}
                      descriptionText={UIStrings.CLEANUP_POLICIES.PREVIEW.REPOSITORY_DESCRIPTION}>
          <Select name="repository" onChange={repositoryChangeHandler}>
            <option value="">{UIStrings.CLEANUP_POLICIES.REPOSITORY_SELECT}</option>
            {repositories.map(({id, name}) =>
                <option key={id} value={id}>{name}</option>
            )}
          </Select>
          <NxButton disabled={previewUnavailable} onClick={previewHandler}>{UIStrings.CLEANUP_POLICIES.PREVIEW.BUTTON}</NxButton>
        </FieldWrapper>
        <SectionToolbar>
          <div className="nxrm-spacer" />
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
          <NxTableBody isLoading={isLoadingPreview} error={previewError} retryHandler={retryPreview}>
            {!hasData && <NxTableRow><NxTableCell>{UIStrings.CLEANUP_POLICIES.PREVIEW.EMPTY}</NxTableCell></NxTableRow>}
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
