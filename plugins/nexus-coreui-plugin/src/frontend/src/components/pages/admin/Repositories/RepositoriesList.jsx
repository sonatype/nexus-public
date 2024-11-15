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
import React, {useState, useEffect} from 'react';

import {
  ContentBody,
  ExtJS,
  HelpTile,
  ListMachineUtils,
  Page,
  PageHeader,
  PageTitle,
  PageActions,
  Section,
  SectionToolbar
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxButtonBar,
  NxFilterInput,
  NxFontAwesomeIcon,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTooltip
} from '@sonatype/react-shared-components';

import {faCopy, faDatabase} from '@fortawesome/free-solid-svg-icons';

import {useRepositoriesService} from './RepositoriesContextProvider';

import UIStrings from '../../../../constants/UIStrings';

import {
  isIqServerEnabled,
  canReadFirewallStatus,
  canUpdateHealthCheck
} from './IQServerColumns/IQServerHelpers';

import RepositoryStatus from './RepositoryStatus';
import HealthCheckCell from './IQServerColumns/HealthCheckCell';
import AnalyzeConfirmationModal from './IQServerColumns/AnalyzeConfirmationModal';
import IqPolicyViolationsCell from './IQServerColumns/IqPolicyViolationsCell';

const {REPOSITORIES} = UIStrings;
const {COLUMNS} = REPOSITORIES.LIST;

export default function RepositoriesList({onCreate, onEdit, copyUrl = doCopyUrl}) {
  const [state, send] = useRepositoriesService();

  useEffect(() => {
    send({type: 'LOAD'});
  }, []);

  const isLoading = state.matches('loading');
  const {data, error, filter: filterText} = state.context;

  const nameSortDir = ListMachineUtils.getSortDirection('name', state.context);
  const typeSortDir = ListMachineUtils.getSortDirection('type', state.context);
  const formatSortDir = ListMachineUtils.getSortDirection('format', state.context);
  const statusSortDir = ListMachineUtils.getSortDirection('status', state.context);
  const sortByName = () => send({type: 'SORT_BY_NAME'});
  const sortByType = () => send({type: 'SORT_BY_TYPE'});
  const sortByFormat = () => send({type: 'SORT_BY_FORMAT'});
  const sortByStatus = () => send({type: 'SORT_BY_STATUS'});

  const filter = (value) => send({type: 'FILTER', filter: value});

  const canCreate = ExtJS.checkPermission('nexus:repository-admin:*:*:add');

  const showHealthCheckColumn = canUpdateHealthCheck();
  const showIqPolicyViolationsColumn = isIqServerEnabled() && canReadFirewallStatus();

  function create() {
    if (canCreate) {
      onCreate();
    }
  }

  const [healthCheckModalState, setHealthCheckModalState] = useState({
    isOpen: false,
    repoName: ''
  });
  const openHealthCheckModal = (repoName) =>
    setHealthCheckModalState({
      isOpen: true,
      repoName
    });
  const closeHealthCheckModal = () =>
    setHealthCheckModalState({
      isOpen: false,
      repoName: ''
    });

  return (
    <Page className="nxrm-repositories">
      <PageHeader>
        <PageTitle icon={faDatabase} {...REPOSITORIES.MENU} />
        <PageActions>
          <NxTooltip title={!canCreate && UIStrings.PERMISSION_ERROR} placement="bottom">
            <NxButton
              type="button"
              variant="primary"
              className={!canCreate && 'disabled'}
              onClick={create}
            >
              {REPOSITORIES.LIST.CREATE_BUTTON}
            </NxButton>
          </NxTooltip>
        </PageActions>
      </PageHeader>
      <ContentBody className="nxrm-repositories-list">
        <Section>
          <SectionToolbar>
            <div className="nxrm-spacer" />
            <NxFilterInput
              id="filter"
              onChange={filter}
              value={filterText}
              placeholder={REPOSITORIES.LIST.FILTER_PLACEHOLDER}
            />
          </SectionToolbar>
          <NxTable>
            <NxTableHead>
              <NxTableRow>
                <NxTableCell onClick={sortByName} isSortable sortDir={nameSortDir}>
                  {COLUMNS.NAME}
                </NxTableCell>
                <NxTableCell onClick={sortByType} isSortable sortDir={typeSortDir}>
                  {COLUMNS.TYPE}
                </NxTableCell>
                <NxTableCell onClick={sortByFormat} isSortable sortDir={formatSortDir}>
                  {COLUMNS.FORMAT}
                </NxTableCell>
                <NxTableCell onClick={sortByStatus} isSortable sortDir={statusSortDir}>
                  {COLUMNS.STATUS}
                </NxTableCell>
                <NxTableCell className="nxrm-table-cell-centered">{COLUMNS.URL}</NxTableCell>
                {showHealthCheckColumn && (
                  <NxTableCell className="nxrm-table-cell-centered">
                    {COLUMNS.HEALTH_CHECK}
                  </NxTableCell>
                )}
                {showIqPolicyViolationsColumn && (
                  <NxTableCell className="nxrm-table-cell-centered">{COLUMNS.IQ}</NxTableCell>
                )}
                <NxTableCell chevron />
              </NxTableRow>
            </NxTableHead>
            <NxTableBody
              isLoading={isLoading}
              error={error}
              emptyMessage={REPOSITORIES.LIST.EMPTY_LIST}
            >
              {data.map(({name, type, format, url, status, firewallStatus}) => (
                <NxTableRow key={name} onClick={() => onEdit(name)} isClickable>
                  <NxTableCell>{name}</NxTableCell>
                  <NxTableCell>{type}</NxTableCell>
                  <NxTableCell>{format}</NxTableCell>
                  <NxTableCell>
                    <RepositoryStatus status={status} />
                  </NxTableCell>
                  <NxTableCell className="nxrm-table-cell-centered">
                    <NxButtonBar>
                      <NxButton
                        variant="icon-only"
                        onClick={(e) => copyUrl(e, url)}
                        title={REPOSITORIES.LIST.COPY_URL_TITLE}
                      >
                        <NxFontAwesomeIcon icon={faCopy} />
                      </NxButton>
                    </NxButtonBar>
                  </NxTableCell>
                  {showHealthCheckColumn && (
                    <NxTableCell className="nxrm-table-cell-centered">
                      <HealthCheckCell name={name} openModal={() => openHealthCheckModal(name)} />
                    </NxTableCell>
                  )}
                  {showIqPolicyViolationsColumn && (
                    <NxTableCell className="nxrm-table-cell-centered">
                      <IqPolicyViolationsCell name={name} />
                    </NxTableCell>
                  )}
                  <NxTableCell chevron />
                </NxTableRow>
              ))}
            </NxTableBody>
          </NxTable>
        </Section>

        <HelpTile header={REPOSITORIES.LIST.HELP.TITLE} body={REPOSITORIES.LIST.HELP.TEXT} />
      </ContentBody>
      {healthCheckModalState.isOpen && (
        <AnalyzeConfirmationModal
          close={closeHealthCheckModal}
          name={healthCheckModalState.repoName}
        />
      )}
    </Page>
  );
}

function doCopyUrl(event, url) {
  event.stopPropagation();
  navigator.clipboard.writeText(url);
  ExtJS.showSuccessMessage('URL Copied to Clipboard');
}
