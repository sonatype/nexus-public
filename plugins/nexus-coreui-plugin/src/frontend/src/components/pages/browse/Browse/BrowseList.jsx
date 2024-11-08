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

import {
  ContentBody,
  ExtJS,
  ListMachineUtils,
  Page,
  PageHeader,
  PageTitle,
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxButtonBar,
  NxFilterInput,
  NxFontAwesomeIcon,
  NxTable,
  NxTile
} from '@sonatype/react-shared-components';

import {
  isIqServerEnabled,
  canReadFirewallStatus,
  canUpdateHealthCheck
} from '../../admin/Repositories/IQServerColumns/IQServerHelpers';

import HealthCheckCell from '../../admin/Repositories/IQServerColumns/HealthCheckCell';
import IqPolicyViolationsCell from '../../admin/Repositories/IQServerColumns/IqPolicyViolationsCell';
import RepositoryStatus from '../../admin/Repositories/RepositoryStatus';
import {faCopy, faDatabase} from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../constants/UIStrings';
import './Browse.scss';

import { useRepositoriesService } from '../../admin/Repositories/RepositoriesContextProvider';

const {BROWSE} = UIStrings;
const {
  COLUMNS,
  FILTER_PLACEHOLDER,
  EMPTY_MESSAGE,
  COPY_URL_TITLE,
  URL_COPIED_MESSAGE
} = BROWSE.LIST;

export default function BrowseList({onEdit, copyUrl = doCopyUrl}) {
  const [state, send] = useRepositoriesService();

  useEffect(() => {
    send({type: 'LOAD'});
  }, []);

  const {data, error, filter: filterText} = state.context;
  const isLoading = state.matches('loading');

  const nameSortDir = ListMachineUtils.getSortDirection('name', state.context);
  const typeSortDir = ListMachineUtils.getSortDirection('type', state.context);
  const formatSortDir = ListMachineUtils.getSortDirection('format', state.context);
  const statusSortDir = ListMachineUtils.getSortDirection('status', state.context);
  const sortByName = () => send({type: 'SORT_BY_NAME'});
  const sortByType = () => send({type: 'SORT_BY_TYPE'});
  const sortByFormat = () => send({type: 'SORT_BY_FORMAT'});
  const sortByStatus = () => send({type: 'SORT_BY_STATUS'});

  const filter = (value) => send({type: 'FILTER', filter: value});

  const showHealthCheckColumn = canUpdateHealthCheck();
  const showIqPolicyViolationsColumn = isIqServerEnabled() && canReadFirewallStatus();

  return <Page className="nxrm-browse">
    <PageHeader>
      <PageTitle icon={faDatabase} {...BROWSE.MENU}/>
    </PageHeader>
    <ContentBody className="nxrm-browse-list">
      <NxTile>
        <NxTile.Header>
          <NxTile.HeaderActions>
            <NxFilterInput
                    id="filter"
                    onChange={filter}
                    value={filterText}
                    placeholder={FILTER_PLACEHOLDER}/>
          </NxTile.HeaderActions>
        </NxTile.Header>
        <NxTile.Content>
          <NxTable>
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell onClick={sortByName} isSortable sortDir={nameSortDir}>{COLUMNS.NAME}</NxTable.Cell>
                <NxTable.Cell onClick={sortByType} isSortable sortDir={typeSortDir}>{COLUMNS.TYPE}</NxTable.Cell>
                <NxTable.Cell onClick={sortByFormat} isSortable sortDir={formatSortDir}>{COLUMNS.FORMAT}</NxTable.Cell>
                <NxTable.Cell onClick={sortByStatus} isSortable sortDir={statusSortDir}>{COLUMNS.STATUS}</NxTable.Cell>
                <NxTable.Cell>{COLUMNS.URL}</NxTable.Cell>
                {showHealthCheckColumn && (
                  <NxTable.Cell className="nxrm-table-cell-centered">
                    {COLUMNS.HEALTH_CHECK}
                  </NxTable.Cell>
                )}
                {showIqPolicyViolationsColumn && (
                  <NxTable.Cell className="nxrm-table-cell-centered">
                    {COLUMNS.IQ_POLICY_VIOLATIONS}
                  </NxTable.Cell>
                )}
                <NxTable.Cell chevron />
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body isLoading={isLoading} error={error} emptyMessage={EMPTY_MESSAGE}>
              {data.map(({name, type, format, status, url}) => (
                <NxTable.Row key={name} onClick={() => onEdit(name)} isClickable>
                  <NxTable.Cell>{name}</NxTable.Cell>
                  <NxTable.Cell>{type}</NxTable.Cell>
                  <NxTable.Cell>{format}</NxTable.Cell>
                  <NxTable.Cell>
                    <RepositoryStatus status={status} />
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxButtonBar>
                      <NxButton
                        variant="icon-only"
                        onClick={(e) => copyUrl(e, url)}
                        title={COPY_URL_TITLE}
                      >
                        <NxFontAwesomeIcon icon={faCopy} />
                      </NxButton>
                    </NxButtonBar>
                  </NxTable.Cell>
                  {showHealthCheckColumn && (
                    <NxTable.Cell className="nxrm-table-cell-centered">
                      <HealthCheckCell name={name} openModal={() => {}}/>
                    </NxTable.Cell>
                  )}
                  {showIqPolicyViolationsColumn && (
                    <NxTable.Cell className="nxrm-table-cell-centered">
                      <IqPolicyViolationsCell name={name}/>
                    </NxTable.Cell>
                  )}
                  <NxTable.Cell chevron />
                </NxTable.Row>
              ))}
            </NxTable.Body>
          </NxTable>
        </NxTile.Content>
      </NxTile>
    </ContentBody>
  </Page>
}

function doCopyUrl(event, url) {
  event.stopPropagation();
  navigator.clipboard.writeText(url);
  ExtJS.showSuccessMessage(URL_COPIED_MESSAGE);
}
