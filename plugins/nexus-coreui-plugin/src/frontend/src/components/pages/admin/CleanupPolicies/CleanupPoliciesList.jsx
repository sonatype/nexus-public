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

import {faBroom} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
  HelpTile,
  ListMachineUtils,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  Section,
  SectionToolbar
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxFilterInput,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';

import CleanupPoliciesListMachine from './CleanupPoliciesListMachine';

import UIStrings from '../../../../constants/UIStrings';

const {CLEANUP_POLICIES: LABELS} = UIStrings;

export default function CleanupPoliciesList({onCreate, onEdit}) {
  const [current, send] = useMachine(CleanupPoliciesListMachine, {devTools: true});
  const isLoading = current.matches('loading');
  const data = current.context.data;
  const filterText = current.context.filter;
  const error = current.context.error;

  const nameSortDir = ListMachineUtils.getSortDirection('name', current.context);
  const formatSortDir = ListMachineUtils.getSortDirection('format', current.context);
  const notesSortDir = ListMachineUtils.getSortDirection('notes', current.context);

  function filter(value) {
    send({type: 'FILTER', filter: value});
  }

  return <Page className="nxrm-cleanup-policies">
    <PageHeader>
      <PageTitle icon={faBroom} {...LABELS.MENU}/>
      <PageActions>
        <NxButton variant="primary" onClick={onCreate}>
          <span>{LABELS.CREATE_BUTTON}</span>
        </NxButton>
      </PageActions>
    </PageHeader>
    <ContentBody>
      <Section className="nxrm-cleanup-policies-list">
        <SectionToolbar>
          <div className="nxrm-spacer" />
          <NxFilterInput
              id="filter"
              onChange={filter}
              value={filterText}
              placeholder={LABELS.FILTER_PLACEHOLDER}/>
        </SectionToolbar>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell onClick={() => send({type: 'SORT_BY_NAME'})} isSortable sortDir={nameSortDir}>
                {LABELS.NAME_LABEL}
              </NxTableCell>
              <NxTableCell onClick={() => send({type: 'SORT_BY_FORMAT'})} isSortable sortDir={formatSortDir}>
                {LABELS.FORMAT_LABEL}
              </NxTableCell>
              <NxTableCell onClick={() => send({type: 'SORT_BY_NOTES'})} isSortable sortDir={notesSortDir}>
                {LABELS.NOTES_LABEL}
              </NxTableCell>
              <NxTableCell chevron/>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoading} error={error} emptyMessage={LABELS.EMPTY_MESSAGE}>
            {data.map(({name, format, notes}) => (
                <NxTableRow key={name} onClick={() => onEdit(name)} isClickable>
                  <NxTableCell>{name}</NxTableCell>
                  <NxTableCell>{format}</NxTableCell>
                  <NxTableCell>{notes}</NxTableCell>
                  <NxTableCell chevron/>
                </NxTableRow>
            ))}
          </NxTableBody>
        </NxTable>
      </Section>

      <HelpTile header={LABELS.HELP_TITLE} body={LABELS.HELP_TEXT}/>
    </ContentBody>
  </Page>;
}
