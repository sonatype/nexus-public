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
import { useService } from '@xstate/react';

import {
  ContentBody,
  ListMachineUtils,
  Page,
  PageHeader,
  PageTitle,
  HelpTile
} from '@sonatype/nexus-ui-plugin';

import {
  NxH2,
  NxFilterInput,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTile
} from '@sonatype/react-shared-components';

import { faTags } from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../constants/UIStrings';

const {TAGS} = UIStrings;
const {COLUMNS} = TAGS.LIST;

export default function TagsList({onEdit, service}) {
  const [state, send] = useService(service);
  const {data, error, filter: filterText} = state.context;
  const isLoading = state.matches('loading');

  const nameSortDir = ListMachineUtils.getSortDirection('name', state.context);
  const firstCreatedSortDir = ListMachineUtils.getSortDirection('firstCreated', state.context);
  const lastUpdatedSortDir = ListMachineUtils.getSortDirection('lastUpdated', state.context);
  const sortByName = () => send('SORT_BY_NAME');
  const sortByFirstCreated = () => send('SORT_BY_FIRST_CREATED');
  const sortByLastUpdated = () => send('SORT_BY_LAST_UPDATED');

  const filter = (value) => send({type: 'FILTER', filter: value});

  return <Page className="nxrm-tags">
    <PageHeader>
      <PageTitle icon={faTags} {...TAGS.MENU}/>
    </PageHeader>
    <ContentBody className="nxrm-tags-list">
      <NxTile>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>{TAGS.LIST.CAPTION}</NxH2>
          </NxTile.HeaderTitle>
          <NxTile.HeaderActions>
            <NxFilterInput
                id="filter"
                onChange={filter}
                value={filterText}
                placeholder={TAGS.LIST.FILTER_PLACEHOLDER}/>
          </NxTile.HeaderActions>
        </NxTile.Header>
        <NxTile.Content>
          <NxTable>
            <NxTableHead>
              <NxTableRow>
                <NxTableCell onClick={sortByName} isSortable sortDir={nameSortDir}>{COLUMNS.NAME}</NxTableCell>
                <NxTableCell onClick={sortByFirstCreated} isSortable sortDir={firstCreatedSortDir}>{COLUMNS.FIRST_CREATED}</NxTableCell>
                <NxTableCell onClick={sortByLastUpdated} isSortable sortDir={lastUpdatedSortDir}>{COLUMNS.LAST_UPDATED}</NxTableCell>
                <NxTableCell chevron/>
              </NxTableRow>
            </NxTableHead>
            <NxTableBody isLoading={isLoading} error={error} emptyMessage={TAGS.EMPTY_MESSAGE}>
              {data.map(
                ({name, firstCreated, lastUpdated}) => (
                  <NxTableRow key={name} isClickable onClick={() => onEdit(name)}>
                    <NxTableCell>{name}</NxTableCell>
                    <NxTableCell>{new Date(firstCreated).toLocaleString()}</NxTableCell>
                    <NxTableCell>{new Date(lastUpdated).toLocaleString()}</NxTableCell>
                    <NxTableCell chevron/>
                  </NxTableRow>
              ))}
            </NxTableBody>
          </NxTable>
        </NxTile.Content>
      </NxTile>
      <HelpTile header={TAGS.HELP_MESSAGE.TITLE} body={TAGS.HELP_MESSAGE.TEXT}/>
    </ContentBody>
  </Page>
}
