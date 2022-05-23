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
import {render, screen, within, fireEvent, waitForElementToBeRemoved} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {when} from 'jest-when';
import Axios from 'axios';

import RolesList from './RolesList';

import UIStrings from "../../../../constants/UIStrings";

const {ROLES: {LIST: LABELS}} = UIStrings;

jest.mock('axios', () => ({
  get: jest.fn()
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
  emptyMessage: () => screen.getByText(LABELS.EMPTY_LIST),
  tableBody: () => screen.getAllByRole('rowgroup')[1],
  tableHeader: (text) => screen.getByText(text, {selector: 'thead *'}),
  rows: () => within(this.tableBody()).getAllByRole('row'),
  filter: () => screen.queryByPlaceholderText(UIStrings.FILTER),
  createButton: () => screen.getByText(LABELS.CREATE_BUTTON),
};

const URL = '/security/roles';

const ROWS = [{
    description: 'Administrator Role',
    id: 'nx-admin',
    name: 'nx-admin-name',
    privileges: ['nx-all'],
    roles: [],
    source: 'default'
  }, {
    description: 'Anonymous Role',
    id: 'nx-anonymous',
    name: 'nx-anonymous-name',
    privileges: ['nx-healthcheck-read'],
    roles: [],
    source: 'default',
}, {
    description: 'Provides privileges required to administer replication connections',
    id: 'replication-role',
    name: 'Replication role',
    privileges: ['nx-replication-update'],
    roles: [],
    source: 'default',
}];

describe('RolesList', function() {
  it('renders the resolved empty data', async function() {
    when(Axios.get).calledWith(expect.stringContaining(URL)).mockResolvedValue({
      data: []
    });

    render(<RolesList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.createButton()).not.toHaveClass('disabled');

    expect(selectors.emptyMessage()).toBeInTheDocument();
  });

  it('renders the resolved data', async function() {
    when(Axios.get).calledWith(expect.stringContaining(URL)).mockResolvedValue({
      data: ROWS
    });

    render(<RolesList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    const table = within(selectors.tableBody());

    ROWS.forEach((role) => {
      expect(table.getByText(role.id)).toBeInTheDocument();
      expect(table.getByText(role.name)).toBeInTheDocument();
      expect(table.getByText(role.description)).toBeInTheDocument();
    });

    expect(table.getAllByRole('row')).toHaveLength(3);
  });

  it('renders an error message', async function() {
    const message = 'Error Message !';
    Axios.get.mockReturnValue(Promise.reject({message}));

    render(<RolesList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    const table = within(selectors.tableBody());

    expect(table.getByRole('alert')).toHaveTextContent(message);
  });

  it('sorts the rows by name or description', async function () {
    when(Axios.get).calledWith(expect.stringContaining(URL)).mockResolvedValue({
      data: ROWS
    });

    render(<RolesList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    let rows = within(selectors.tableBody()).getAllByRole('row');

    expect(rows[0].cells).toHaveLength(4);

    expect(rows[0].cells[0]).toHaveTextContent(ROWS[0].id);
    expect(rows[1].cells[0]).toHaveTextContent(ROWS[1].id);
    expect(rows[2].cells[0]).toHaveTextContent(ROWS[2].id);

    fireEvent.click(selectors.tableHeader(LABELS.COLUMNS.NAME));
    rows = within(selectors.tableBody()).getAllByRole('row');

    expect(rows[0].cells[0]).toHaveTextContent(ROWS[2].id);
    expect(rows[1].cells[0]).toHaveTextContent(ROWS[1].id);
    expect(rows[2].cells[0]).toHaveTextContent(ROWS[0].id);

    fireEvent.click(selectors.tableHeader(LABELS.COLUMNS.DESCRIPTION));
    rows = within(selectors.tableBody()).getAllByRole('row');

    expect(rows[0].cells[0]).toHaveTextContent(ROWS[0].id);
    expect(rows[1].cells[0]).toHaveTextContent(ROWS[1].id);
    expect(rows[2].cells[0]).toHaveTextContent(ROWS[2].id);
  });

  it('filters by name and description', async function() {
    when(Axios.get).calledWith(expect.stringContaining(URL)).mockResolvedValue({
      data: ROWS
    });

    render(<RolesList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    let rows = within(selectors.tableBody()).getAllByRole('row');

    expect(rows).toHaveLength(3);

    await TestUtils.changeField(selectors.filter, 'nx-an');
    rows = within(selectors.tableBody()).getAllByRole('row');
    expect(rows).toHaveLength(1);
    expect(rows[0].cells[0]).toHaveTextContent(ROWS[1].id);

    await TestUtils.changeField(selectors.filter, 'privileges');
    rows = within(selectors.tableBody()).getAllByRole('row');
    expect(rows).toHaveLength(1);
    expect(rows[0].cells[0]).toHaveTextContent(ROWS[2].id);

    await TestUtils.changeField(selectors.filter, 'admin');
    rows = within(selectors.tableBody()).getAllByRole('row');
    expect(rows).toHaveLength(2);
  });

  it('disables the create button when not enough permissions', async function() {
    Axios.get.mockResolvedValue({data: []});
    ExtJS.checkPermission.mockReturnValue(false);

    render(<RolesList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.createButton()).toHaveClass('disabled');
  });
});
