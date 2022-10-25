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
import {render, screen, waitForElementToBeRemoved} from '@testing-library/react';
import {sort, prop, descend, ascend} from 'ramda';
import userEvent from '@testing-library/user-event';
import {ExtJS, TestUtils, APIConstants} from '@sonatype/nexus-ui-plugin';
import {when} from 'jest-when';
import Axios from 'axios';

import LdapServersList from './LdapServersList';
import {URL} from './LdapServersHelper';
import UIStrings from '../../../../constants/UIStrings';
import {LDAP_SERVERS} from './LdapServers.testdata';

const {SORT_DIRECTIONS: {DESC, ASC}} = APIConstants;
const {ldapServersUrl} = URL;
const {LDAP_SERVERS: {LIST: LABELS}} = UIStrings;
const XSS_STRING = TestUtils.XSS_STRING;

jest.mock('axios', () => ({
  get: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      checkPermission: jest.fn().mockReturnValue(true),
    }
  }
});

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.tableSelectors,
  emptyMessage: () => screen.getByText(LABELS.EMPTY_LIST),
  filter: () => screen.queryByPlaceholderText(UIStrings.FILTER),
  createButton: () => screen.getByText(LABELS.BUTTONS.CREATE),
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
    ExtJS.checkPermission.mockReturnValue(false);

    await renderAndWaitForLoad();

    expect(selectors.createButton()).toHaveClass('disabled');
  });
});
