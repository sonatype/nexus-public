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
  NxButton,
  NxFilterInput,
  NxFontAwesomeIcon,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  Page,
  PageHeader,
  PageTitle,
  PageActions,
  Section,
  SectionToolbar,
  Utils
} from '@sonatype/nexus-ui-plugin';
import {
  faCheckCircle,
  faChevronRight,
  faExclamationCircle,
  faInfoCircle,
  faServer
} from '@fortawesome/free-solid-svg-icons';

import BlobStoresListMachine from './BlobStoresListMachine';
import UIStrings from '../../../../constants/UIStrings';

const {BLOB_STORES} = UIStrings;
const {COLUMNS} = BLOB_STORES.LIST;

export default function BlobStoresList({onCreate, onEdit}) {
  const [current, send] = useMachine(BlobStoresListMachine, {devTools: true});
  const isLoading = current.matches('loading');
  const {data, error, filter: filterText} = current.context;

  const nameSortDir = Utils.getSortDirection('name', current.context);
  const typeSortDir = Utils.getSortDirection('typeName', current.context);
  const stateSortDir = Utils.getSortDirection('available', current.context);
  const countSortDir = Utils.getSortDirection('blobCount', current.context);
  const sizeSortDir = Utils.getSortDirection('totalSizeInBytes', current.context);
  const spaceSortDir = Utils.getSortDirection('availableSpaceInBytes', current.context);
  const sortByName = () => send('SORT_BY_NAME');
  const sortByType = () => send('SORT_BY_TYPE_NAME');
  const sortByState = () => send('SORT_BY_STATE');
  const sortByCount = () => send('SORT_BY_COUNT');
  const sortBySize = () => send('SORT_BY_SIZE');
  const sortBySpace = () => send('SORT_BY_SPACE');

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
              <NxTableCell onClick={sortByType} isSortable sortDir={typeSortDir}>{COLUMNS.TYPE}</NxTableCell>
              <NxTableCell onClick={sortByState} isSortable sortDir={stateSortDir}>{COLUMNS.STATE}</NxTableCell>
              <NxTableCell onClick={sortByCount} isSortable sortDir={countSortDir} isNumeric>
                {COLUMNS.COUNT}
              </NxTableCell>
              <NxTableCell onClick={sortBySize} isSortable sortDir={sizeSortDir} isNumeric>{COLUMNS.SIZE}</NxTableCell>
              <NxTableCell onClick={sortBySpace} isSortable sortDir={spaceSortDir} isNumeric>
                {COLUMNS.SPACE}
              </NxTableCell>
              <NxTableCell hasIcon/>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoading} error={error} emptyMessage={BLOB_STORES.LIST.EMPTY_LIST}>
            {data.map(
                ({name, typeId, typeName, available, unavailable, blobCount, totalSizeInBytes, availableSpaceInBytes, unlimited}) => (
                    <NxTableRow key={name} onClick={() => onEdit(`${typeId}/${name}`)} isClickable>
                      <NxTableCell>{name}</NxTableCell>
                      <NxTableCell>{typeName}</NxTableCell>
                      <NxTableCell>
                        <NxFontAwesomeIcon className={available ? 'available' : 'unavailable'}
                                           icon={available ? faCheckCircle : faExclamationCircle}/>
                        <span>{available ? BLOB_STORES.LIST.AVAILABLE : BLOB_STORES.LIST.UNAVAILABLE}</span>
                      </NxTableCell>
                      <NxTableCell isNumeric>{unavailable ? BLOB_STORES.LIST.UNKNOWN : blobCount}</NxTableCell>
                      <NxTableCell isNumeric>{unavailable ? BLOB_STORES.LIST.UNKNOWN : Utils.bytesToString(
                          totalSizeInBytes)} ({totalSizeInBytes})</NxTableCell>
                      <NxTableCell isNumeric>
                        {
                          (unavailable && BLOB_STORES.LIST.UNKNOWN) ||
                          (unlimited && BLOB_STORES.LIST.UNLIMITED) ||
                          Utils.bytesToString(availableSpaceInBytes)
                        }
                      </NxTableCell>
                      <NxTableCell hasIcon><NxFontAwesomeIcon icon={faChevronRight}/></NxTableCell>
                    </NxTableRow>
                ))}
          </NxTableBody>
        </NxTable>
      </Section>

      <HelpTile>
        <h3><NxFontAwesomeIcon icon={faInfoCircle}/><span>{BLOB_STORES.LIST.HELP.TITLE}</span></h3>
        <p dangerouslySetInnerHTML={{__html: BLOB_STORES.LIST.HELP.TEXT}}/>
      </HelpTile>
    </ContentBody>
  </Page>;
}
