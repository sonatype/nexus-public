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
  SectionToolbar,
  Permissions,
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
  NxInfoAlert,
} from '@sonatype/react-shared-components';

import {faUserTag} from '@fortawesome/free-solid-svg-icons';

import RolesListMachine from './RolesListMachine';
import UIStrings from '../../../../constants/UIStrings';

const {ROLES: {LIST: LABELS}} = UIStrings;
const {COLUMNS} = LABELS;

export default function RolesList({onCreate, onEdit}) {
  const [state, send] = useMachine(RolesListMachine, {devTools: true});
  const isLoading = state.matches('loading');
  const {data, error, filter: filterText, defaultRole: {roleId, roleName, capabilityId} = {}} = state.context;

  const idSortDir = ListMachineUtils.getSortDirection('id', state.context);
  const nameSortDir = ListMachineUtils.getSortDirection('name', state.context);
  const descriptionSortDir = ListMachineUtils.getSortDirection('description', state.context);

  const sortById = () => send({type: 'SORT_BY_ID'});
  const sortByName = () => send({type: 'SORT_BY_NAME'});
  const sortByDescription = () => send({type: 'SORT_BY_DESCRIPTION'});

  const filter = (value) => send({type: 'FILTER', filter: value});
  const canCreate = ExtJS.checkPermission(Permissions.ROLES.CREATE);

  function create() {
    if (canCreate) {
      onCreate();
    }
  }

  return <Page className="nxrm-roles">
    <PageHeader>
      <PageTitle icon={faUserTag} {...UIStrings.ROLES.MENU} />
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
    <ContentBody className="nxrm-roles-list">
      {roleId && roleName &&
          <NxInfoAlert>
            {LABELS.ALERT.DEFAULT_ROLE(roleId, roleName)}
            <br/>
            {capabilityId && LABELS.ALERT.CAPABILITY(capabilityId)}
          </NxInfoAlert>
      }
      <Section>
        <SectionToolbar>
          <div className="nxrm-spacer"/>
          <NxFilterInput
              id="filter"
              onChange={filter}
              value={filterText}
              placeholder={UIStrings.FILTER}/>
        </SectionToolbar>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell onClick={sortById} isSortable sortDir={idSortDir}>{COLUMNS.ID}</NxTableCell>
              <NxTableCell onClick={sortByName} isSortable sortDir={nameSortDir}>{COLUMNS.NAME}</NxTableCell>
              <NxTableCell onClick={sortByDescription} isSortable sortDir={descriptionSortDir}>{COLUMNS.DESCRIPTION}</NxTableCell>
              <NxTableCell chevron/>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoading} error={error} emptyMessage={LABELS.EMPTY_LIST}>
            {data.map(({id, name, description}) => (
                <NxTableRow key={id} onClick={() => onEdit(encodeURIComponent(id))} isClickable>
                  <NxTableCell>{id}</NxTableCell>
                  <NxTableCell>{name}</NxTableCell>
                  <NxTableCell>{description}</NxTableCell>
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
