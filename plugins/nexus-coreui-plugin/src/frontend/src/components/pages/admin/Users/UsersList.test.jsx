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
import {render, screen, waitFor, waitForElementToBeRemoved} from '@testing-library/react';
import {sort, prop, descend, ascend, clone, toLower, compose} from 'ramda';
import userEvent from '@testing-library/user-event';
import {when} from 'jest-when';
import Axios from 'axios';
import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';
import UsersList from './UsersList';
import {ROWS, SOURCES, DEFAULT_DATA, FIELDS, SOURCES_MAP} from './Users.testdata';

const {USERS: {LIST: LABELS}} = UIStrings;

const {EXT: {URL, USER: {ACTION, METHODS}}, SORT_DIRECTIONS: {DESC, ASC}} = APIConstants;

jest.mock('axios', () => {
  return {
    ...jest.requireActual('axios'),
    post: jest.fn(),
  };
});

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
  filter: () => screen.queryByPlaceholderText(LABELS.FILTER_PLACEHOLDER),
  sourceFilter: () => screen.queryByRole('combobox'),
  createButton: () => screen.getByText(LABELS.CREATE_BUTTON),
};

const USERS_REQUEST = expect.objectContaining({
  action: ACTION,
  method: METHODS.READ,
});

const SOURCES_REQUEST = expect.objectContaining({
  action: ACTION,
  method: METHODS.READ_SOURCES,
});

const sortUsers = (data, field, order = ASC) => sort((order === ASC ? ascend : descend)(compose(toLower, prop(field))), data);

describe('UsersList', function() {

  const renderAndWaitForLoad = async () => {
    render(<UsersList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  beforeEach(() => {
    when(Axios.post).calledWith(URL, USERS_REQUEST).mockResolvedValue({
      data: TestUtils.makeExtResult(ROWS.LOCAL)
    });
    when(Axios.post).calledWith(URL, SOURCES_REQUEST).mockResolvedValue({
      data: TestUtils.makeExtResult(SOURCES)
    });
  });

  it('renders the resolved empty data', async function() {
    when(Axios.post).calledWith(URL, USERS_REQUEST).mockResolvedValue({
      data: TestUtils.makeExtResult([])
    });

    const {createButton, emptyMessage} = selectors;

    await renderAndWaitForLoad();

    expect(createButton()).not.toHaveClass('disabled');
    expect(emptyMessage()).toBeInTheDocument();
  });

  it('renders the resolved data', async function() {
    await renderAndWaitForLoad();

    await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(URL, USERS_REQUEST));

    TestUtils.expectTableHeaders(Object.values(LABELS.COLUMNS));
    const users = clone(sortUsers(ROWS.LOCAL, FIELDS.USER_ID));
    users.forEach(user => user.realm = SOURCES_MAP[user.realm].name);
    TestUtils.expectTableRows(users, Object.values(FIELDS));
  });

  it('renders an error message', async function() {
    const message = 'Error Message!';
    const {tableAlert} = selectors;
    when(Axios.post).calledWith(URL, USERS_REQUEST).mockReturnValue(Promise.reject({message}));

    await renderAndWaitForLoad();

    expect(tableAlert()).toHaveTextContent(message);
  });

  it('sorts the rows by each columns', async function () {
    const {headerCell} = selectors;
    const DATA = ROWS.CROWD;
    const COLUMNS = LABELS.COLUMNS;

    when(Axios.post).calledWith(URL, USERS_REQUEST).mockResolvedValue({
      data: TestUtils.makeExtResult(DATA)
    });

    await renderAndWaitForLoad();

    let users = sortUsers(DATA, FIELDS.USER_ID);
    TestUtils.expectProperRowsOrder(users, FIELDS.USER_ID);

    userEvent.click(headerCell(COLUMNS.USER_ID));
    users = sortUsers(DATA, FIELDS.USER_ID, DESC);

    TestUtils.expectProperRowsOrder(users, FIELDS.USER_ID);

    userEvent.click(headerCell(COLUMNS.FIRST_NAME));
    users = sortUsers(DATA, FIELDS.FIRST_NAME);
    TestUtils.expectProperRowsOrder(users, FIELDS.USER_ID);

    userEvent.click(headerCell(COLUMNS.FIRST_NAME));
    users = sortUsers(DATA, FIELDS.FIRST_NAME, DESC);
    TestUtils.expectProperRowsOrder(users, FIELDS.FIRST_NAME, 2);

    userEvent.click(headerCell(COLUMNS.EMAIL));
    users = sortUsers(DATA, FIELDS.EMAIL);
    TestUtils.expectProperRowsOrder(users, FIELDS.EMAIL, 4);
  });

  it('filters by User ID', async function() {
    const {filter, rows} = selectors;
    const filterString = 'test';

    await renderAndWaitForLoad();

    when(Axios.post).calledWith(URL, USERS_REQUEST).mockResolvedValue({data: TestUtils.makeExtResult([ROWS.LOCAL[0]])});

    await TestUtils.changeField(filter, filterString);

    let newRequest = clone(DEFAULT_DATA);
    newRequest.data[0].filter[0] = {
      property: 'userId',
      value: filterString,
    };

    await waitFor(() => expect(Axios.post).toHaveBeenLastCalledWith(URL, newRequest));
    expect(rows()).toHaveLength(1);
  });

  it('filters by User Source', async function() {
    const {sourceFilter, filter, rows, queryLoadingMask} = selectors;
    const filterString = 'test';
    const source = 'Crowd';

    await renderAndWaitForLoad();

    when(Axios.post).calledWith(URL, USERS_REQUEST).mockResolvedValue({data: TestUtils.makeExtResult(ROWS.CROWD)});

    expect(rows()).toHaveLength(2);

    expect(sourceFilter()).toHaveValue('default');
    expect(sourceFilter().options).toHaveLength(5);

    userEvent.selectOptions(sourceFilter(), source);

    await waitForElementToBeRemoved(queryLoadingMask());

    expect(rows()).toHaveLength(ROWS.CROWD.length);

    await TestUtils.changeField(filter, filterString);

    let newRequest = clone(DEFAULT_DATA);
    newRequest.data[0].filter = [
        {property: 'userId', value: filterString},
        {property: 'source', value: source},
    ];

    await waitFor(() => expect(Axios.post).toHaveBeenLastCalledWith(URL, newRequest));
    expect(rows()).toHaveLength(ROWS.CROWD.length);
  });

  it('disables the create button when not enough permissions', async function() {
    const {createButton} = selectors;
    ExtJS.checkPermission.mockReturnValue(false);

    await renderAndWaitForLoad();

    expect(createButton()).toHaveClass('disabled');
  });
});
