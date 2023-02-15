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
import {render, screen, waitForElementToBeRemoved, within, act} from '@testing-library/react';
import {sort, prop, descend, ascend} from 'ramda';
import userEvent from '@testing-library/user-event';
import {ExtJS, APIConstants, ExtAPIUtils, Permissions} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {when} from 'jest-when';
import Axios from 'axios';

import LdapServersList from './LdapServersList';
import {URL} from './LdapServersHelper';
import UIStrings from '../../../../constants/UIStrings';
import {LDAP_SERVERS} from './LdapServers.testdata';

const {
  SORT_DIRECTIONS: {DESC, ASC},
  EXT: {
    URL: EXT_URL,
    LDAP: {
      ACTION,
      METHODS: {CLEAR_CACHE}
    }
  }
} = APIConstants;
const {ldapServersUrl, changeLdapServersOrderUrl} = URL;
const {LDAP_SERVERS: {LIST: LABELS}, SETTINGS} = UIStrings;
const XSS_STRING = TestUtils.XSS_STRING;

jest.mock('axios', () => ({
  get: jest.fn(),
  post: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      checkPermission: jest.fn(),
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn()
    }
  }
});

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.tableSelectors,
  emptyMessage: () => screen.getByText(LABELS.EMPTY_LIST),
  filter: () => screen.queryByPlaceholderText(UIStrings.FILTER),
  createButton: () => screen.getByText(LABELS.BUTTONS.CREATE),
  clearCacheButton: () => screen.getByText(LABELS.BUTTONS.CLEAR_CACHE).closest('button'),
  queryClearCacheButton: () => screen.queryByText(LABELS.BUTTONS.CLEAR_CACHE),
  changeOrderButton: () => screen.getByText(LABELS.BUTTONS.CHANGE_ORDER).closest('button'),
  queryChangeOrderButton: () => screen.queryByText(LABELS.BUTTONS.CHANGE_ORDER),
  modal: {
    title: () => screen.getByText(LABELS.MODAL.LABEL),
    queryTitle: () => screen.queryByText(LABELS.MODAL.LABEL),
    save: () => screen.getByText(SETTINGS.SAVE_BUTTON_LABEL),
    cancel: () => screen.getByText(SETTINGS.CANCEL_BUTTON_LABEL),
    list: () => screen.getByRole('group', { name: LABELS.MODAL.SUB_LABEL }),
    listItem: (text) => within(selectors.modal.container()).getByText(text).closest('.nx-transfer-list__item'),
    listItemDownButton: (text) => within(selectors.modal.listItem(text)).queryAllByRole('button')[1],
    input: () => within(selectors.modal.container()).queryByPlaceholderText(UIStrings.FILTER),
    container: () => screen.getByRole('dialog'),
  }
};

const FIELDS = {
  ORDER: 'order',
  NAME: 'name',
  URL: 'url',
};

const sortLdapServers = (field, order = ASC) => sort((order === ASC ? ascend : descend)(prop(field)), LDAP_SERVERS);

describe('LdapServersList', function() {

  const renderAndWaitForLoad = async () => {
    render(<LdapServersList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  beforeEach(() => {
    when(Axios.get).calledWith(ldapServersUrl).mockResolvedValue({
      data: LDAP_SERVERS
    });
    when(ExtJS.checkPermission)
      .calledWith(Permissions.LDAP.CREATE)
      .mockReturnValue(true);

    when(ExtJS.checkPermission)
      .calledWith(Permissions.LDAP.UPDATE)
      .mockReturnValue(true);

    when(ExtJS.checkPermission)
      .calledWith(Permissions.LDAP.DELETE)
      .mockReturnValue(true);
  });

  it('renders the resolved empty data', async function() {
    when(Axios.get).calledWith(ldapServersUrl).mockResolvedValue({
      data: []
    });
    await renderAndWaitForLoad();

    expect(selectors.createButton()).not.toHaveClass('disabled');
    expect(selectors.emptyMessage()).toBeInTheDocument();
  });

  it('renders the resolved data', async function() {
    await renderAndWaitForLoad();

    TestUtils.expectTableHeaders(Object.values(LABELS.COLUMNS));
    const servers = sortLdapServers(FIELDS.ORDER);

    TestUtils.expectTableRows(servers, Object.values(FIELDS));
  });

  it('renders the resolved data with XSS', async function() {
    const XSS_ROWS = [{
      ...LDAP_SERVERS[0],
      order: XSS_STRING,
      name: XSS_STRING,
      url: XSS_STRING,
    }];

    when(Axios.get).calledWith(ldapServersUrl).mockResolvedValue({
      data: XSS_ROWS
    });

    await renderAndWaitForLoad();

    TestUtils.expectTableHeaders(Object.values(LABELS.COLUMNS));
    TestUtils.expectTableRows(XSS_ROWS, Object.values(FIELDS));
  });

  it('renders an error message', async function() {
    const message = 'Error Message !';
    const {tableAlert} = selectors;
    Axios.get.mockReturnValue(Promise.reject({message}));

    await renderAndWaitForLoad();

    expect(tableAlert()).toHaveTextContent(message);
  });

  it('sorts the rows by each columns', async function () {
    const {headerCell} = selectors;
    await renderAndWaitForLoad();

    let servers = sortLdapServers(FIELDS.ORDER);
    TestUtils.expectProperRowsOrder(servers, FIELDS.ORDER);

    userEvent.click(headerCell(LABELS.COLUMNS.ORDER));
    servers = sortLdapServers(FIELDS.ORDER, DESC);
    TestUtils.expectProperRowsOrder(servers, FIELDS.ORDER);

    userEvent.click(headerCell(LABELS.COLUMNS.NAME));
    servers = sortLdapServers(FIELDS.NAME);
    TestUtils.expectProperRowsOrder(servers, FIELDS.ORDER);

    userEvent.click(headerCell(LABELS.COLUMNS.NAME));
    servers = sortLdapServers(FIELDS.NAME, DESC);
    TestUtils.expectProperRowsOrder(servers, FIELDS.ORDER);

    userEvent.click(headerCell(LABELS.COLUMNS.URL));
    servers = sortLdapServers(FIELDS.URL);
    TestUtils.expectProperRowsOrder(servers, FIELDS.ORDER);

    userEvent.click(headerCell(LABELS.COLUMNS.URL));
    servers = sortLdapServers(FIELDS.URL, DESC);
    TestUtils.expectProperRowsOrder(servers, FIELDS.ORDER);
  });

  it('filters by each columns', async function() {
    const {filter} = selectors;

    await renderAndWaitForLoad();

    await TestUtils.expectProperFilteredItemsCount(filter, '', LDAP_SERVERS.length);
    await TestUtils.expectProperFilteredItemsCount(filter, 'test', 1);
    await TestUtils.expectProperFilteredItemsCount(filter, 'ads', 1);
    await TestUtils.expectProperFilteredItemsCount(filter, 'dev', 1);
    await TestUtils.expectProperFilteredItemsCount(filter, 'dc=local', 2);
  });

  it('disables the create button when not enough permissions', async function() {
    when(ExtJS.checkPermission)
      .calledWith(Permissions.LDAP.CREATE)
      .mockReturnValue(false);

    await renderAndWaitForLoad();

    expect(selectors.createButton()).toHaveClass('disabled');
  });

  describe('Clear Cache button', () => {
    it('clears the cache', async () => {
      const {clearCacheButton} = selectors;

      await renderAndWaitForLoad();

      const REQUEST = ExtAPIUtils.createRequestBody(ACTION, CLEAR_CACHE);

      const response = {data: TestUtils.makeExtResult({})};

      when(Axios.post)
        .calledWith(EXT_URL, REQUEST)
        .mockResolvedValue(response);

      await act(async () => userEvent.click(clearCacheButton()));

      expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(LABELS.MESSAGES.CACHE_CLEARED);
    });

    it('do not show the button if the user has not delete permission', async () => {
      const {queryClearCacheButton} = selectors;

      when(ExtJS.checkPermission)
        .calledWith(Permissions.LDAP.DELETE)
        .mockReturnValue(false);

      await renderAndWaitForLoad();

      expect(queryClearCacheButton()).not.toBeInTheDocument();
    });
  })

  describe('Change order button', () => {
    it('users can cancel and close the modal', async () => {
      const {changeOrderButton, modal: {title, save, cancel, queryTitle}} = selectors;

      await renderAndWaitForLoad();

      userEvent.click(changeOrderButton());

      expect(title()).toBeInTheDocument();
      expect(save()).toBeInTheDocument();
      expect(cancel()).toBeInTheDocument();

      userEvent.click(cancel());

      expect(queryTitle()).not.toBeInTheDocument();
    });

    it('users can reorder and save the changes', async () => {
      const {
        changeOrderButton,
        modal: {
          list,
          listItemDownButton,
          save,
          queryTitle
        }
      } = selectors;

      await renderAndWaitForLoad();

      await userEvent.click(changeOrderButton());
      const servers = sortLdapServers(FIELDS.ORDER);
      const names = servers.map(server => server.name);
      const [firstItem] = names;

      expect(names).toHaveLength(3);

      names.forEach(name => {
        expect(list()).toHaveTextContent(name);
      });

      when(Axios.post)
        .calledWith(changeLdapServersOrderUrl)
        .mockResolvedValue({});

      const downButton = listItemDownButton(firstItem);

      userEvent.click(downButton);

      await act(async () => userEvent.click(save()));

      const expectedOrder = [...names].reverse();

      expect(Axios.post).toHaveBeenCalledWith(changeLdapServersOrderUrl, expectedOrder);

      expect(queryTitle()).not.toBeInTheDocument();
      expect(ExtJS.showSuccessMessage).toHaveBeenCalledWith(LABELS.MESSAGES.LIST_CHANGED);
    });

    it('users can filter the list in the modal when reordering the ldap servers', async () => {
      const {changeOrderButton, modal: {input, list}} = selectors;

      await renderAndWaitForLoad();

      userEvent.click(changeOrderButton());

      expect(list().querySelectorAll('.nx-transfer-list__item')).toHaveLength(LDAP_SERVERS.length)

      await TestUtils.changeField(input, LDAP_SERVERS[0].name);

      expect(list().querySelectorAll('.nx-transfer-list__item')).toHaveLength(1)
    });

    it('do not show the button if the user has not update permission', async () => {
      const {queryChangeOrderButton} = selectors;

      when(ExtJS.checkPermission)
        .calledWith(Permissions.LDAP.UPDATE)
        .mockReturnValue(false);

      await renderAndWaitForLoad();

      expect(queryChangeOrderButton()).not.toBeInTheDocument();
    });
  });
});
