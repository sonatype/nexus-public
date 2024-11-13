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
  ContentBody,
  HelpTile,
  HumanReadableUtils,
  ListMachineUtils,
  Page,
  PageHeader,
  PageTitle,
  PageActions,
  Section,
  SectionToolbar,
} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxFilterInput,
  NxFontAwesomeIcon,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';
import {
  faCheckCircle,
  faExclamationCircle,
  faServer,
} from '@fortawesome/free-solid-svg-icons';

import BlobStoresListMachine from './BlobStoresListMachine';
import UIStrings from '../../../../constants/UIStrings';

const {BLOB_STORES} = UIStrings;
const {COLUMNS} = BLOB_STORES.LIST;

export default function BlobStoresList({onCreate, onEdit}) {
  const [current, send] = useMachine(BlobStoresListMachine, {devTools: true});
  const isLoading = current.matches('loading');
  const {data, error, filter: filterText} = current.context;

  const nameSortDir = ListMachineUtils.getSortDirection('name', current.context);
  const pathSortDir = ListMachineUtils.getSortDirection('path', current.context);
  const typeSortDir = ListMachineUtils.getSortDirection('typeName', current.context);
  const stateSortDir = ListMachineUtils.getSortDirection('available', current.context);
  const countSortDir = ListMachineUtils.getSortDirection('blobCount', current.context);
  const sizeSortDir = ListMachineUtils.getSortDirection('totalSizeInBytes', current.context);
  const spaceSortDir = ListMachineUtils.getSortDirection('availableSpaceInBytes', current.context);
  const sortByName = () => send('SORT_BY_NAME');
  const sortByPath = () => send('SORT_BY_PATH');
  const sortByType = () => send('SORT_BY_TYPE_NAME');
  const sortByState = () => send('SORT_BY_AVAILABLE');
  const sortByCount = () => send('SORT_BY_BLOB_COUNT');
  const sortBySize = () => send('SORT_BY_TOTAL_SIZE_IN_BYTES');
  const sortBySpace = () => send('SORT_BY_AVAILABLE_SPACE_IN_BYTES');

  const filter = (value) => send({type: 'FILTER', filter: value});

  return <Page className="nxrm-blob-stores">
    <PageHeader>
      <PageTitle icon={faServer} {...BLOB_STORES.MENU} />
      <PageActions>
        <NxButton variant="primary" onClick={onCreate}>{BLOB_STORES.LIST.CREATE_BUTTON}</NxButton>
      </PageActions>
    </PageHeader>
    <ContentBody className="nxrm-blob-stores-list">
      <Section>
        <SectionToolbar>
          <div className="nxrm-spacer"/>
          <NxFilterInput
              id="filter"
              onChange={filter}
              value={filterText}
              placeholder={BLOB_STORES.LIST.FILTER_PLACEHOLDER}/>
        </SectionToolbar>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell onClick={sortByName} isSortable sortDir={nameSortDir}>{COLUMNS.NAME}</NxTableCell>
              <NxTableCell onClick={sortByPath} isSortable sortDir={pathSortDir}>{COLUMNS.PATH}</NxTableCell>
              <NxTableCell onClick={sortByType} isSortable sortDir={typeSortDir}>{COLUMNS.TYPE}</NxTableCell>
              <NxTableCell onClick={sortByState} isSortable sortDir={stateSortDir}>{COLUMNS.STATE}</NxTableCell>
              <NxTableCell onClick={sortByCount} isSortable sortDir={countSortDir} isNumeric>
                {COLUMNS.COUNT}
              </NxTableCell>
              <NxTableCell onClick={sortBySize} isSortable sortDir={sizeSortDir} isNumeric>{COLUMNS.SIZE}</NxTableCell>
              <NxTableCell onClick={sortBySpace} isSortable sortDir={spaceSortDir} isNumeric>
                {COLUMNS.SPACE}
              </NxTableCell>
              <NxTableCell chevron/>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoading} error={error} emptyMessage={BLOB_STORES.LIST.EMPTY_LIST}>
            {data.map(
                ({name, path, typeId, typeName, available, unavailable, blobCount, totalSizeInBytes, availableSpaceInBytes, unlimited}) => (
                    <NxTableRow key={name} onClick={() => onEdit(`${encodeURIComponent(typeId)}/${encodeURIComponent(name)}`)} isClickable>
                      <NxTableCell>{name}</NxTableCell>
                      <NxTableCell className="blob-store-path">{path}</NxTableCell>
                      <NxTableCell>{typeName}</NxTableCell>
                      <NxTableCell>
                        <NxFontAwesomeIcon className={available ? 'available' : 'unavailable'}
                                           icon={available ? faCheckCircle : faExclamationCircle}/>
                        <span>{available ? BLOB_STORES.LIST.AVAILABLE : BLOB_STORES.LIST.UNAVAILABLE}</span>
                      </NxTableCell>
                      <NxTableCell isNumeric>{unavailable ? BLOB_STORES.LIST.UNKNOWN : blobCount}</NxTableCell>
                      <NxTableCell isNumeric>{unavailable ? BLOB_STORES.LIST.UNKNOWN : HumanReadableUtils.bytesToString(
                          totalSizeInBytes)} ({totalSizeInBytes})</NxTableCell>
                      <NxTableCell isNumeric>
                        {
                          (unavailable && BLOB_STORES.LIST.UNKNOWN) ||
                          (unlimited && BLOB_STORES.LIST.UNLIMITED) ||
                          HumanReadableUtils.bytesToString(availableSpaceInBytes)
                        }
                      </NxTableCell>
                      <NxTableCell chevron/>
                    </NxTableRow>
                ))}
          </NxTableBody>
        </NxTable>
      </Section>

      <HelpTile header={BLOB_STORES.LIST.HELP.TITLE} body={BLOB_STORES.LIST.HELP.TEXT}/>
    </ContentBody>
  </Page>;
}
