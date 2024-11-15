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

import {faScroll} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
  ExtJS,
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

import ContentSelectorsListMachine from './ContentSelectorsListMachine';

import UIStrings from '../../../../constants/UIStrings';

const {CONTENT_SELECTORS: LABELS} = UIStrings;

export default function ContentSelectorsList({onCreate, onEdit}) {
  const [state, send] = useMachine(ContentSelectorsListMachine, {devTools: true});
  const isLoading = state.matches('loading');
  const data = state.context.data;
  const filterText = state.context.filter;
  const error = state.context.error;

  const nameSortDir = ListMachineUtils.getSortDirection('name', state.context);
  const typeSortDir = ListMachineUtils.getSortDirection('type', state.context);
  const descriptionSortDir = ListMachineUtils.getSortDirection('description', state.context);

  function filter(value) {
    send({type: 'FILTER', filter: value});
  }

  const canCreate = ExtJS.checkPermission('nexus:selectors:create');

  return <Page className="nxrm-content-selectors">
    <PageHeader>
      <PageTitle icon={faScroll} {...LABELS.MENU}/>
      <PageActions>
        <NxButton variant="primary" disabled={!canCreate} onClick={onCreate}>
          <span>{LABELS.CREATE_BUTTON}</span>
        </NxButton>
      </PageActions>
    </PageHeader>
    <ContentBody>
      <Section className="nxrm-content-selectors-list">
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
              <NxTableCell onClick={() => send({type: 'SORT_BY_TYPE'})} isSortable sortDir={typeSortDir}>
                {LABELS.TYPE_LABEL}
              </NxTableCell>
              <NxTableCell onClick={() => send({type: 'SORT_BY_DESCRIPTION'})} isSortable sortDir={descriptionSortDir}>
                {LABELS.DESCRIPTION_LABEL}
              </NxTableCell>
              <NxTableCell chevron/>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoading} error={error} emptyMessage={LABELS.EMPTY_MESSAGE}>
            {data.map(({name, type, description}) => (
                <NxTableRow key={name} onClick={() => onEdit(name)} isClickable>
                  <NxTableCell>{name}</NxTableCell>
                  <NxTableCell>{type?.toUpperCase()}</NxTableCell>
                  <NxTableCell>{description}</NxTableCell>
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
