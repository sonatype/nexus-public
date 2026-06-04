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
import React, {} from 'react';
import { useMachine } from '@xstate/react';
import { useRouter } from '@uirouter/react';
import classNames from 'classnames';

import {
  NxButton,
  NxFilterInput,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxFontAwesomeIcon
} from '@sonatype/react-shared-components';

import {
  faServer,
  faCheckCircle,
  faExclamationCircle,
  faMinusCircle
} from '@fortawesome/free-solid-svg-icons';

import { ContentBody, Page, PageActions, PageHeader, PageTitle, Section, SectionToolbar } from '../../layout';

import CapabilitiesListMachine from './CapabilitiesListMachine';
import UIStrings from '../../../constants/UIStrings';
import { AdminRouteNames } from '../../../constants/admin/AdminRouteNames';
import ListMachineUtils from '../../../interface/ListMachineUtils';
import './CapabilitiesList.scss';

import ExtJS from '../../../interface/ExtJS';
import Permissions from '../../../constants/Permissions';
import { RouteNames } from '../../../constants/RouteNames';

const ADMIN = RouteNames.ADMIN;

const { CAPABILITIES } = UIStrings;
const { COLUMNS } = CAPABILITIES.LIST;

export default function CapabilitiesList() {
  const router = useRouter();
  const onEdit = id => router.stateService.go(AdminRouteNames.SYSTEM.CAPABILITIES.EDIT, { id });
  const onCreate = () => router.stateService.go(ADMIN.SYSTEM.CAPABILITIES.CREATE);
  const canCreate = ExtJS.checkPermission(Permissions.CAPABILITIES.CREATE);

  const [current, send] = useMachine(CapabilitiesListMachine, { devTools: true });
  const isLoading = current.matches('loading');
  const { data, error, filter, sortField, sortDirection } = current.context;
  const retryHandler = () =>
      send({ type: 'RETRY' });

  const filterHandler = (value) =>
      send({ type: 'FILTER', filter: value });

  const sortByTypeName = () => send({ type: 'SORT_BY_TYPE_NAME' });
  const sortByState = () => send({ type: 'SORT_BY_STATE' });
  const sortByCategory = () => send({ type: 'SORT_BY_CATEGORY' });
  const sortByRepository = () => send({ type: 'SORT_BY_REPOSITORY' });
  const sortByDescription = () => send({ type: 'SORT_BY_DESCRIPTION' });
  const sortByNotes = () => send({ type: 'SORT_BY_NOTES' });

  const showsRepositoryHeader = !!data ?
      !!data.find(entry => entry.tags?.Repository)
      : false;

  return (
    <Page className='nxrm-capabilities'>
      <PageHeader>
        <PageTitle icon={faServer} {...CAPABILITIES.MENU} />
        <PageActions>
          <NxButton data-analytics-id="nxrm-create-capability" variant="primary" onClick={onCreate} disabled={!canCreate}>{CAPABILITIES.LIST.CREATE_BUTTON}</NxButton>
        </PageActions>
      </PageHeader>
      <ContentBody className='nxrm-capabilities-list'>
        <Section>
          <SectionToolbar>
            <div className="nxrm-spacer"/>
            <NxFilterInput placeholder={UIStrings.FILTER} value={filter} onChange={filterHandler} />
          </SectionToolbar>
          <NxTable>
            <NxTableHead>
              <NxTableRow>
                <NxTableCell
                  isSortable
                  sortDir={ListMachineUtils.getSortDirection('typeName', { sortField, sortDirection })}
                  onClick={sortByTypeName}
                >
                  {COLUMNS.TYPE}
                </NxTableCell>
                <NxTableCell
                  isSortable
                  sortDir={ListMachineUtils.getSortDirection('state', { sortField, sortDirection })}
                  onClick={sortByState}
                >
                  {COLUMNS.STATE}
                </NxTableCell>
                <NxTableCell
                  isSortable
                  sortDir={ListMachineUtils.getSortDirection('category', { sortField, sortDirection })}
                  onClick={sortByCategory}
                >
                  {COLUMNS.CATEGORY}
                </NxTableCell>
                { showsRepositoryHeader &&
                  <NxTableCell
                    isSortable
                    sortDir={ListMachineUtils.getSortDirection('repository', { sortField, sortDirection })}
                    onClick={sortByRepository}
                  >
                    {COLUMNS.REPOSITORY}
                  </NxTableCell>
                }
                <NxTableCell
                  isSortable
                  sortDir={ListMachineUtils.getSortDirection('description', { sortField, sortDirection })}
                  onClick={sortByDescription}
                >
                  {COLUMNS.DESCRIPTION}
                </NxTableCell>
                <NxTableCell
                  isSortable
                  sortDir={ListMachineUtils.getSortDirection('notes', { sortField, sortDirection })}
                  onClick={sortByNotes}
                >
                  {COLUMNS.NOTES}
                </NxTableCell>
                <NxTableCell chevron />
              </NxTableRow>
            </NxTableHead>
            <NxTableBody
              isLoading={isLoading}
              error={error}
              emptyMessage={CAPABILITIES.LIST.EMPTY_LIST}
              retryHandler={retryHandler}
            >
              {data.map(({ id, state, typeName, description, notes, tags }) => (
                <NxTableRow isClickable key={id} onClick={() => onEdit(id)}>
                  <NxTableCell>{typeName}</NxTableCell>
                  <StateCell state={state} />
                  <NxTableCell>{tagsToCategoryDisplayText(tags)}</NxTableCell>
                  { showsRepositoryHeader && <NxTableCell>{propertiesToRepositoryDisplayText(tags)}</NxTableCell>}
                  <NxTableCell>{toDisplayText(description)}</NxTableCell>
                  <NxTableCell>{toDisplayText(notes)}</NxTableCell>
                  <NxTableCell chevron />
                </NxTableRow>
              ))}
            </NxTableBody>
          </NxTable>
        </Section>
      </ContentBody>
    </Page>
  );

  function tagsToCategoryDisplayText(tags) {
    return toDisplayText(tags?.Category)
  }

  function propertiesToRepositoryDisplayText(tags) {
    return toDisplayText(tags?.Repository)
  }

  function toDisplayText(text) {
    return text || '-';
  }

}

function StateCell({state}) {
  let icon;

  switch (state) {
    case "active":
      icon = faCheckCircle;
      break;
    case "error":
    case "passive":
      icon = faExclamationCircle;
      break;
    case "disabled":
      icon = faMinusCircle;
      break;
    default:
      icon = null;
  }

  return (
      <NxTableCell className={classNames("capabilities-state", "capabilities-state__" + state)}>
        {icon && <NxFontAwesomeIcon icon={icon} />}
        {state}
      </NxTableCell>
  );
}
