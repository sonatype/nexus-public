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
import {DateTime} from 'luxon';
import {faScroll} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
  HumanReadableUtils,
  ListMachineUtils,
  Page,
  PageHeader,
  PageTitle,
  Section, SectionToolbar
} from '@sonatype/nexus-ui-plugin';

import {
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxFilterInput
} from '@sonatype/react-shared-components';

import LogsListMachine from './LogsListMachine';

import UIStrings from '../../../../constants/UIStrings';

export default function LogsList({onEdit}) {
  const [current, send] = useMachine(LogsListMachine, {devTools: true});
  const isLoading = current.matches('loading');
  const data = current.context.data;
  const error = current.context.error;
  const filter = current.context.filter;

  const fileNameSortDir = ListMachineUtils.getSortDirection('fileName', current.context);
  const sizeSortDir = ListMachineUtils.getSortDirection('size', current.context);
  const lastModifiedSortDir = ListMachineUtils.getSortDirection('lastModified', current.context);

  return <Page>
    <PageHeader>
      <PageTitle icon={faScroll} {...UIStrings.LOGS.MENU}/>
    </PageHeader>
    <ContentBody>
      <Section className="nxrm-logs-list">
        <SectionToolbar>
          <NxFilterInput
              className="nxrm-logs-filter"
              id="filter"
              onChange={(value) => send({type: 'FILTER', filter: value})}
              value={filter}
              placeholder={UIStrings.LOGS.LIST.FILTER_PLACEHOLDER}/>
        </SectionToolbar>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell onClick={() => send({type: 'SORT_BY_FILE_NAME'})} isSortable sortDir={fileNameSortDir}>
                {UIStrings.LOGS.LIST.FILE_NAME_LABEL}
              </NxTableCell>
              <NxTableCell onClick={() => send({type: 'SORT_BY_SIZE'})} isSortable sortDir={sizeSortDir}>
                {UIStrings.LOGS.LIST.SIZE_LABEL}
              </NxTableCell>
              <NxTableCell onClick={() => send({type: 'SORT_BY_LAST_MODIFIED'})} isSortable sortDir={lastModifiedSortDir}>
                {UIStrings.LOGS.LIST.LAST_MODIFIED_LABEL}
              </NxTableCell>
              <NxTableCell chevron/>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoading} error={error}>
            {data.map(({fileName, size, lastModified}) => (
                <NxTableRow key={fileName} onClick={() => onEdit(fileName)} isClickable>
                  <NxTableCell>{fileName}</NxTableCell>
                  <NxTableCell>{HumanReadableUtils.bytesToString(size)}</NxTableCell>
                  <NxTableCell>
                    {DateTime.fromMillis(lastModified).toLocaleString(DateTime.DATETIME_SHORT_WITH_SECONDS)}
                  </NxTableCell>
                  <NxTableCell chevron/>
                </NxTableRow>
            ))}
          </NxTableBody>
        </NxTable>
      </Section>
    </ContentBody>
  </Page>;
}
