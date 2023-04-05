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
import Axios from 'axios';
import {when} from 'jest-when';
import {render, screen, waitFor, waitForElementToBeRemoved} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {sort, prop, descend, ascend, clone} from 'ramda';

import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';
import PrivilegesList from './PrivilegesList';

const XSS_STRING = TestUtils.XSS_STRING;
const {PRIVILEGES: {LIST: LABELS}} = UIStrings;
const {EXT: {URL, PRIVILEGE: {ACTION, METHODS}}, SORT_DIRECTIONS: {DESC, ASC}} = APIConstants;

jest.mock('axios', () => {
  return {
    ...jest.requireActual('axios'),
    post: jest.fn()
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
  filter: () => screen.queryByPlaceholderText(UIStrings.FILTER),
  createButton: () => screen.getByText(LABELS.CREATE_BUTTON),
};

const ROWS = [{
  description: 'All permissions for Logging',
  id: 'nx-logging-all',
  name: 'nx-logging-all',
  permission: 'nexus:logging:*',
  readOnly: true,
  type: 'application',
}, {
  description: 'Browse permissions for Scripts',
  id: 'nx-script-*-browse',
  name: 'nx-script-*-browse',
  permission: 'nexus:script:*:browse,read',
  readOnly: true,
  type: 'script',
}, {
  description: 'All permissions for Settings',
  id: 'nx-settings-all',
  name: 'nx-settings-all',
  permission: 'nexus:settings:*',
  readOnly: true,
  type: 'application',
}];

const REQUEST = expect.objectContaining({
  action: ACTION,
  method: METHODS.READ.NAME,
});

const FIELDS = {
  NAME: 'name',
  DESCRIPTION: 'description',
  TYPE: 'type',
  PERMISSION: 'permission',
};

const DEFAULT_DATA = {
  action: ACTION,
  data: [
    {
      limit: 300,
      page: 1,
      sort: [
        {
          direction: ASC.toUpperCase(),
          property: "name"
        }
      ],
      start: 0
    }
  ],
  method: METHODS.READ.NAME,
  tid: 1,
  type: 'rpc'
};

const sortPrivileges = (field, order = ASC) => sort((order === ASC ? ascend : descend)(prop(field)), ROWS);

describe('PrivilegesList', function() {

  const renderAndWaitForLoad = async () => {
    render(<PrivilegesList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  beforeEach(() => {
    when(Axios.post).calledWith(URL, REQUEST).mockResolvedValue({
      data: TestUtils.makeExtResult(ROWS)
    });
  });

  it('renders the resolved empty data', async function() {
    when(Axios.post).calledWith(URL, REQUEST).mockResolvedValue({
      data: TestUtils.makeExtResult([])
    });
    const {createButton, emptyMessage} = selectors;

    await renderAndWaitForLoad();

    expect(createButton()).not.toHaveClass('disabled');
    expect(emptyMessage()).toBeInTheDocument();
  });

  it('renders the resolved data', async function() {
    await renderAndWaitForLoad();

    await waitFor(() => expect(Axios.post).toHaveBeenCalledWith(URL, REQUEST));

    TestUtils.expectTableHeaders(Object.values(LABELS.COLUMNS));
    TestUtils.expectTableRows(ROWS, Object.values(FIELDS));
  });

  it('renders the resolved data with XSS', async function() {
    const XSS_ROWS = [{
      ...ROWS[0],
      description: XSS_STRING,
      name: XSS_STRING,
      permission: XSS_STRING,
    }];

    when(Axios.post).calledWith(URL, REQUEST).mockResolvedValue({
      data: TestUtils.makeExtResult(XSS_ROWS)
    });

    await renderAndWaitForLoad();

    TestUtils.expectTableHeaders(Object.values(LABELS.COLUMNS));
    TestUtils.expectTableRows(XSS_ROWS, Object.values(FIELDS));
  });

  it('renders an error message', async function() {
    const message = 'Error Message!';
    const {tableAlert} = selectors;
    when(Axios.post).calledWith(URL, REQUEST).mockRejectedValue({message});

    await renderAndWaitForLoad();

    expect(tableAlert()).toHaveTextContent(message);
  });

  describe('Sorting', function() {
    const expectProperOrder = async (fieldName, columnName, direction) => {
      const {headerCell} = selectors;

      let privileges = sortPrivileges(fieldName, direction);
      when(Axios.post).calledWith(URL, REQUEST).mockResolvedValue({data: TestUtils.makeExtResult(privileges)});

      userEvent.click(headerCell(columnName));
      let newRequest = clone(DEFAULT_DATA);
      newRequest.data[0].sort[0] = {
        direction: direction.toUpperCase(),
        property: fieldName,
      };
      await waitFor(() => expect(Axios.post).toHaveBeenLastCalledWith(URL, newRequest));

      TestUtils.expectProperRowsOrder(privileges);
    }

    it('sorts the rows by each columns', async function () {
      await renderAndWaitForLoad();
      await waitFor(() => expect(Axios.post).toHaveBeenLastCalledWith(URL, REQUEST));
      TestUtils.expectProperRowsOrder(ROWS);

      await expectProperOrder(FIELDS.NAME, LABELS.COLUMNS.NAME, DESC);
      await expectProperOrder(FIELDS.DESCRIPTION, LABELS.COLUMNS.DESCRIPTION, ASC);
      await expectProperOrder(FIELDS.DESCRIPTION, LABELS.COLUMNS.DESCRIPTION, DESC);
      await expectProperOrder(FIELDS.TYPE, LABELS.COLUMNS.TYPE, ASC);
      await expectProperOrder(FIELDS.PERMISSION, LABELS.COLUMNS.PERMISSION, ASC);
      await expectProperOrder(FIELDS.PERMISSION, LABELS.COLUMNS.PERMISSION, DESC);
    });
  });

  it('filters by each columns', async function() {
    const {filter, rows} = selectors;
    const filterString = 'test';

    await renderAndWaitForLoad();

    when(Axios.post).calledWith(URL, REQUEST).mockResolvedValue({data: TestUtils.makeExtResult([ROWS[0]])});

    await TestUtils.changeField(filter, filterString);

    let newRequest = clone(DEFAULT_DATA);
    newRequest.data = [{
      ...newRequest.data[0],
      filter: [{
        value: filterString,
        property: 'filter',
      }],
    }];

    await waitFor(() => expect(Axios.post).toHaveBeenLastCalledWith(URL, newRequest));
    expect(rows()).toHaveLength(1);
  });

  it('disables the create button when not enough permissions', async function() {
    const {createButton} = selectors;
    ExtJS.checkPermission.mockReturnValue(false);

    await renderAndWaitForLoad();

    expect(createButton()).toHaveClass('disabled');
  });
});
