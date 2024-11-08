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
  NxTooltip,
} from '@sonatype/react-shared-components';

import {faIdBadge} from '@fortawesome/free-solid-svg-icons';

import PrivilegesListMachine from './PrivilegesListMachine';
import UIStrings from '../../../../constants/UIStrings';

const {PRIVILEGES: {LIST: LABELS}} = UIStrings;
const {COLUMNS} = LABELS;

export default function PrivilegesList({onCreate, onEdit}) {
  const [current, send] = useMachine(PrivilegesListMachine, {devTools: true});
  const isLoading = current.matches('loading');
  const {data, error, filter: filterText} = current.context;

  const nameSortDir = ListMachineUtils.getSortDirection('name', current.context);
  const descriptionSortDir = ListMachineUtils.getSortDirection('description', current.context);
  const typeSortDir = ListMachineUtils.getSortDirection('type', current.context);
  const permissionSortDir = ListMachineUtils.getSortDirection('permission', current.context);

  const sortByName = () => send({type: 'SORT_BY_NAME'});
  const sortByDescription = () => send({type: 'SORT_BY_DESCRIPTION'});
  const sortByType = () => send({type: 'SORT_BY_TYPE'});
  const sortByPermission = () => send({type: 'SORT_BY_PERMISSION'});

  const filter = (value) => send({type: 'FILTER', filter: value});
  const canCreate = ExtJS.checkPermission('nexus:privileges:create');

  function create() {
    if (canCreate) {
      onCreate();
    }
  }

  return <Page className="nxrm-privileges">
    <PageHeader>
      <PageTitle icon={faIdBadge} {...UIStrings.PRIVILEGES.MENU} />
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
    <ContentBody className="nxrm-privileges-list">
      <Section>
        <SectionToolbar>
          <div className="nxrm-spacer"/>
          <NxFilterInput
              id="filter"
              onChange={filter}
              value={filterText}
              placeholder={UIStrings.FILTER}
          />
        </SectionToolbar>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell onClick={sortByName} isSortable sortDir={nameSortDir}>{COLUMNS.NAME}</NxTableCell>
              <NxTableCell onClick={sortByDescription} isSortable sortDir={descriptionSortDir}>{COLUMNS.DESCRIPTION}</NxTableCell>
              <NxTableCell onClick={sortByType} isSortable sortDir={typeSortDir}>{COLUMNS.TYPE}</NxTableCell>
              <NxTableCell onClick={sortByPermission} isSortable sortDir={permissionSortDir}>{COLUMNS.PERMISSION}</NxTableCell>
              <NxTableCell chevron/>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoading} error={error} emptyMessage={LABELS.EMPTY_LIST}>
            {data.map(({id, name, description, type, permission}) => (
                <NxTableRow key={id} onClick={() => onEdit(id)} isClickable>
                  <NxTableCell>{name}</NxTableCell>
                  <NxTableCell>{description}</NxTableCell>
                  <NxTableCell>{type}</NxTableCell>
                  <NxTableCell>{permission}</NxTableCell>
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
