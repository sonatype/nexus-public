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
  ExtJS,
  HelpTile,
  ListMachineUtils,
  Page,
  PageHeader,
  PageTitle,
  PageActions,
  Section,
} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxFilterInput,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTooltip,
  NxFormSelect,
} from '@sonatype/react-shared-components';

import {faUsers} from '@fortawesome/free-solid-svg-icons';

import './Users.scss';

import Machine from './UsersListMachine';
import UIStrings from '../../../../constants/UIStrings';

const {USERS: {LIST: LABELS}} = UIStrings;
const {COLUMNS} = LABELS;

export default function UsersList({onCreate, onEdit}) {
  const [current, send] = useMachine(Machine, {devTools: true});
  const isLoading = current.matches('loading') || current.matches('loadingSources');
  const {data, sources = {}, error, filter, sourceFilter} = current.context;

  const userIdSortDir = ListMachineUtils.getSortDirection('userId', current.context);
  const realmSortDir = ListMachineUtils.getSortDirection('realm', current.context);
  const firstNameSortDir = ListMachineUtils.getSortDirection('firstName', current.context);
  const lastNameSortDir = ListMachineUtils.getSortDirection('lastName', current.context);
  const emailSortDir = ListMachineUtils.getSortDirection('email', current.context);
  const statusSortDir = ListMachineUtils.getSortDirection('status', current.context);

  const sortById = () => send({type: 'SORT_BY_USER_ID'});
  const sortByRealms = () => send({type: 'SORT_BY_REALM'});
  const sortByFirstName = () => send({type: 'SORT_BY_FIRST_NAME'});
  const sortByLastName = () => send({type: 'SORT_BY_LAST_NAME'});
  const sortByEmail = () => send({type: 'SORT_BY_EMAIL'});
  const sortByStatus = () => send({type: 'SORT_BY_STATUS'});

  const onUserIdFilterChange = (value) => send({type: 'FILTER', filter: value});
  const onSourceFilterChange = (value) => send({type: 'FILTER_BY_SOURCE', filter: value});
  const canCreate = ExtJS.checkPermission('nexus:users:create');

  const create = () => {
    if (canCreate) {
      onCreate();
    }
  };

  const onClickRow = (realm, userId) => onEdit(`${encodeURIComponent(realm)}/${encodeURIComponent(userId)}`);

  return <Page className="nxrm-users">
    <PageHeader>
      <PageTitle icon={faUsers} {...UIStrings.USERS.MENU} />
      <PageActions>
        <NxTooltip
            title={!canCreate && UIStrings.PERMISSION_ERROR}
            placement="bottom"
        >
          <NxButton
              type="button"
              variant="primary"
              className={!canCreate && 'disabled'}
              onClick={create}
          >
            {LABELS.CREATE_BUTTON}
          </NxButton>
        </NxTooltip>
      </PageActions>
    </PageHeader>
    <ContentBody className="nxrm-users-list">
      <Section>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell onClick={sortById} isSortable sortDir={userIdSortDir}>{COLUMNS.USER_ID}</NxTableCell>
              <NxTableCell onClick={sortByRealms} isSortable sortDir={realmSortDir}>{COLUMNS.REALM}</NxTableCell>
              <NxTableCell onClick={sortByFirstName} isSortable sortDir={firstNameSortDir}>{COLUMNS.FIRST_NAME}</NxTableCell>
              <NxTableCell onClick={sortByLastName} isSortable sortDir={lastNameSortDir}>{COLUMNS.LAST_NAME}</NxTableCell>
              <NxTableCell onClick={sortByEmail} isSortable sortDir={emailSortDir}>{COLUMNS.EMAIL}</NxTableCell>
              <NxTableCell onClick={sortByStatus} isSortable sortDir={statusSortDir}>{COLUMNS.STATUS}</NxTableCell>
              <NxTableCell chevron/>
            </NxTableRow>
            <NxTableRow isFilterHeader>
              <NxTableCell>
                <NxFilterInput
                    id="userIdFilter"
                    onChange={onUserIdFilterChange}
                    value={filter}
                    placeholder={LABELS.FILTER_PLACEHOLDER}
                />
              </NxTableCell>
              <NxTableCell>
                <NxFormSelect
                    id="userSourceFilter"
                    name="userSourceFilter"
                    value={sourceFilter}
                    onChange={onSourceFilterChange}
                >
                  {Object.values(sources).map(({id, name}) => (
                      <option key={id} value={id}>{name}</option>
                  ))}
                </NxFormSelect>
              </NxTableCell>
              <NxTableCell />
              <NxTableCell />
              <NxTableCell />
              <NxTableCell />
              <NxTableCell />
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoading} error={error} emptyMessage={LABELS.EMPTY_LIST}>
            {data.map(({userId, realm, firstName, lastName, email, status}) => (
                <NxTableRow key={userId} onClick={() => onClickRow(realm, userId)} isClickable>
                  <NxTableCell>{userId}</NxTableCell>
                  <NxTableCell>{sources[realm].name}</NxTableCell>
                  <NxTableCell>{firstName}</NxTableCell>
                  <NxTableCell>{lastName}</NxTableCell>
                  <NxTableCell>{email}</NxTableCell>
                  <NxTableCell className="capitalize-status">{status}</NxTableCell>
                  <NxTableCell chevron/>
                </NxTableRow>
            ))}
          </NxTableBody>
        </NxTable>
      </Section>
      <HelpTile header={LABELS.HELP.TITLE} body={LABELS.HELP.TEXT}/>
    </ContentBody>
  </Page>;
}
