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
  NxTile,
  NxTransferListHalf,
  NxModal,
  NxH2,
  NxFooter,
  NxButtonBar,
  NxFontAwesomeIcon,
  NxPageTitle,
} from '@sonatype/react-shared-components';
import {faSortNumericUp, faTrashAlt} from '@fortawesome/free-solid-svg-icons';

import {canDelete, canUpdate, canCreate} from './LdapServersHelper';

import {faBook} from '@fortawesome/free-solid-svg-icons';

import Machine from './LdapServersListMachine';
import UIStrings from '../../../../constants/UIStrings';

const {
  LDAP_SERVERS: {LIST: LABELS, MENU},
  SETTINGS,
} = UIStrings;
const {COLUMNS} = LABELS;

import './LdapServersList.scss';

export default function LdapServersList({onCreate, onEdit}) {
  const [state, send] = useMachine(Machine, {
    context: {transferListData: [], filterTransferList: '', modal: false},
    devTools: true,
  });
  const isLoading = state.matches('loading');
  const {
    data,
    modal,
    transferListData,
    filterTransferList,
    error,
    filter: filterText,
  } = state.context;
  const hasCreatePermission = canCreate();

  const orderSortDir = ListMachineUtils.getSortDirection(
    'order',
    state.context
  );
  const nameSortDir = ListMachineUtils.getSortDirection('name', state.context);
  const urlSortDir = ListMachineUtils.getSortDirection('url', state.context);

  const sortByOrder = () => send({type: 'SORT_BY_ORDER'});
  const sortByName = () => send({type: 'SORT_BY_NAME'});
  const sortByUrl = () => send({type: 'SORT_BY_URL'});

  const filter = (value) => send({type: 'FILTER', filter: value});

  const create = () => {
    if (hasCreatePermission) {
      onCreate();
    }
  };

  const change = () => send({type: 'TOGGLE_ORDER_MODAL', value: true});
  const clear = () => send({type: 'CLEAR_CACHE'});

  const onReorder = (id, direction) => send({type: 'REORDER', id, direction});
  const onCancelOrder = () => send({type: 'TOGGLE_ORDER_MODAL', value: false});
  const onSaveOrder = () => send({type: 'SAVE_ORDER'});
  const filterOrder = (value) =>
    send({
      type: 'FILTER_ORDER_LIST',
      value,
    });

  const transferListItems = transferListData.map(({order, name}) => ({
    id: order,
    displayName: name,
  }));

  return (
    <Page className="nxrm-ldap-servers">
      <PageHeader>
        <PageTitle
          icon={faBook}
          text={MENU.text}
          description={MENU.description}
        />
        <PageActions>
          <NxTooltip
            title={!hasCreatePermission && UIStrings.PERMISSION_ERROR}
            placement="bottom"
          >
            <NxButton
              type="button"
              variant="primary"
              className={!hasCreatePermission && 'disabled'}
              onClick={create}
            >
              {LABELS.BUTTONS.CREATE}
            </NxButton>
          </NxTooltip>
        </PageActions>
      </PageHeader>
      <ContentBody className="nxrm-ldap-servers-list">
        <NxTile>
          <NxPageTitle>
            <NxH2>{LABELS.LABEL}</NxH2>
            <NxButtonBar>
              {canUpdate() && (
                <NxButton type="button" onClick={change} variant="tertiary">
                  <NxFontAwesomeIcon icon={faSortNumericUp} />
                  <span>{LABELS.BUTTONS.CHANGE_ORDER}</span>
                </NxButton>
              )}
              {canDelete() && (
                <NxButton type="button" onClick={clear} variant="tertiary">
                  <NxFontAwesomeIcon icon={faTrashAlt} />
                  <span>{LABELS.BUTTONS.CLEAR_CACHE}</span>
                </NxButton>
              )}
            </NxButtonBar>
          </NxPageTitle>
          <SectionToolbar>
            <div className="nxrm-spacer" />
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
                <NxTableCell
                  onClick={sortByOrder}
                  isSortable
                  sortDir={orderSortDir}
                >
                  {COLUMNS.ORDER}
                </NxTableCell>
                <NxTableCell
                  onClick={sortByName}
                  isSortable
                  sortDir={nameSortDir}
                >
                  {COLUMNS.NAME}
                </NxTableCell>
                <NxTableCell
                  onClick={sortByUrl}
                  isSortable
                  sortDir={urlSortDir}
                >
                  {COLUMNS.URL}
                </NxTableCell>
                <NxTableCell chevron />
              </NxTableRow>
            </NxTableHead>
            <NxTableBody
              isLoading={isLoading}
              error={error}
              emptyMessage={LABELS.EMPTY_LIST}
            >
              {data.map(({id, order, name, url}) => (
                <NxTableRow
                  key={id}
                  onClick={() => onEdit(encodeURIComponent(name))}
                  isClickable
                >
                  <NxTableCell>{order}</NxTableCell>
                  <NxTableCell>{name}</NxTableCell>
                  <NxTableCell>{url}</NxTableCell>
                  <NxTableCell chevron />
                </NxTableRow>
              ))}
            </NxTableBody>
          </NxTable>
          {modal && (
            <NxModal
              onCancel={() => {}}
              aria-labelledby="modal-header-text"
              variant="narrow"
            >
              <NxModal.Header>
                <NxH2 id="modal-header-text">{LABELS.MODAL.LABEL}</NxH2>
              </NxModal.Header>
              <NxModal.Content>
                <NxTransferListHalf
                  onReorderItem={onReorder}
                  label={LABELS.MODAL.SUB_LABEL}
                  filterValue={filterTransferList}
                  onFilterChange={filterOrder}
                  items={transferListItems}
                  allowReordering
                  isSelected={false}
                  showMoveAll={false}
                  onItemChange={() => {}}
                  footerContent={LABELS.MODAL.FOOTER(transferListItems.length)}
                />
              </NxModal.Content>
              <NxFooter>
                <NxButtonBar>
                  <NxButton onClick={onCancelOrder}>
                    {SETTINGS.CANCEL_BUTTON_LABEL}
                  </NxButton>
                  <NxButton onClick={onSaveOrder} variant="primary">
                    {SETTINGS.SAVE_BUTTON_LABEL}
                  </NxButton>
                </NxButtonBar>
              </NxFooter>
            </NxModal>
          )}
        </NxTile>
        <HelpTile header={LABELS.HELP.TITLE} body={LABELS.HELP.TEXT} />
      </ContentBody>
    </Page>
  );
}
