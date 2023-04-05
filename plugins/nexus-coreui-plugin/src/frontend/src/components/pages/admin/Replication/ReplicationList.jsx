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

import {faCopy} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
  HelpTile,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  Utils,
  Section,
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxWarningAlert
} from '@sonatype/react-shared-components';

import ReplicationListMachine from './ReplicationListMachine';

import UIStrings from '../../../../constants/UIStrings';

const {REPLICATION} = UIStrings;

export default function ReplicationList({onCreate, onEdit, response, setResponse}) {
  const [current, send] = useMachine(ReplicationListMachine, {devTools: true});
  const isLoading = current.matches('loading');
  const data = current.context.data;
  const error = current.context.error;

  function closeWarning() {
    setResponse(null);
  }

  function getSortDirection(fieldName, {sortField, sortDirection}) {
    if (sortField !== fieldName) {
      return null;
    }
    return sortDirection === Utils.ASC ? 'asc' : 'desc';
  }

  const nameSortDir = getSortDirection('name', current.context);
  const sourceRepoSortDir = getSortDirection('sourceRepositoryName', current.context);
  const targetInstanceSortDir = getSortDirection('destinationInstanceUrl', current.context);
  const destinationRepoSortDir = getSortDirection('destinationRepositoryName', current.context);

  return <Page className="nxrm-replication">
    <PageHeader>
      <PageTitle icon={faCopy} {...REPLICATION.MENU}/>
      <PageActions>
        <NxButton variant="primary" onClick={onCreate}>New Replication</NxButton>
      </PageActions>
    </PageHeader>
    <ContentBody className="nxrm-replication-list">
      <Section>
        {response !== null &&
            <NxWarningAlert onClose={closeWarning}>
              {response}
            </NxWarningAlert>}
        <h2 className="nx-h2">
          {REPLICATION.LIST.HEADING}
        </h2>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell
                  onClick={() => send('SORT_BY_NAME')}
                  isSortable
                  sortDir={nameSortDir}
              >
                {REPLICATION.LIST.NAME_LABEL}
              </NxTableCell>
              <NxTableCell
                  onClick={() => send('SORT_BY_SOURCE_REPOSITORY_NAME')}
                  isSortable
                  sortDir={sourceRepoSortDir}
              >
                {REPLICATION.LIST.SOURCE_REPO_LABEL}
              </NxTableCell>
              <NxTableCell
                  onClick={() => send('SORT_BY_DESTINATION_INSTANCE_URL')}
                  isSortable
                  sortDir={targetInstanceSortDir}
              >
                {REPLICATION.LIST.TARGET_INSTANCE_LABEL}
              </NxTableCell>
              <NxTableCell
                  onClick={() => send('SORT_BY_DESTINATION_REPOSITORY_NAME')}
                  isSortable
                  sortDir={destinationRepoSortDir}
              >
                {REPLICATION.LIST.DESTINATION_REPO_LABEL}
              </NxTableCell>
              <NxTableCell chevron/>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody
              isLoading={isLoading}
              error={error}
              emptyMessage={REPLICATION.LIST.EMPTY_LIST}>
            {data.map(({name, sourceRepositoryName, destinationInstanceUrl, destinationRepositoryName}) => (
                <NxTableRow key={sourceRepositoryName} isClickable onClick={() => onEdit(name)}>
                  <NxTableCell>{name}</NxTableCell>
                  <NxTableCell>{sourceRepositoryName}</NxTableCell>
                  <NxTableCell>{destinationInstanceUrl}</NxTableCell>
                  <NxTableCell>{destinationRepositoryName}</NxTableCell>
                  <NxTableCell chevron/>
                </NxTableRow>
            ))}
          </NxTableBody>
        </NxTable>
      </Section>

      <HelpTile header={REPLICATION.LIST.HELP_TITLE} body={REPLICATION.LIST.HELP_TEXT}/>
    </ContentBody>
  </Page>;
}
