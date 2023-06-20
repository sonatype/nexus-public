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
import {render, screen, within, waitForElementToBeRemoved} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {sort, prop, descend, ascend} from 'ramda';
import {ExtJS, APIConstants, Permissions} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';
import RolesList from './RolesList';

const {ROLES: {LIST: LABELS}} = UIStrings;
const {EXT: {CAPABILITY: {ACTION, METHODS}, URL: EXT_URL}} = APIConstants;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  post: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      checkPermission: jest.fn(),
      state: jest.fn().mockReturnValue({
        getValue: jest.fn(),
      }),
    }
  }
});

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.tableSelectors,
  emptyMessage: () => screen.getByText(LABELS.EMPTY_LIST),
  tableBody: () => screen.getAllByRole('rowgroup')[1],
  tableHeader: (text) => screen.getByText(text, {selector: 'thead *'}),
  rows: () => within(this.tableBody()).getAllByRole('row'),
  filter: () => screen.queryByPlaceholderText(UIStrings.FILTER),
  createButton: () => screen.getByText(LABELS.CREATE_BUTTON),
  defaultRoleNameLink: (name) => screen.queryByRole('link', {name}),
  defaultRoleCapabilityLink: () => screen.queryByRole('link', {name: 'Default Role capability'}),
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
    description: 'Provides privileges',
    id: 'replication-role',
    name: 'replication role',
    privileges: ['nx-replication-update'],
    roles: [],
    source: 'default',
}];

const FIELDS = {
  ID: 'id',
  NAME: 'name',
  DESCRIPTION: 'description',
};

const sortRoles = (field, order = ascend) => sort(order(prop(field)), ROWS);

describe('RolesList', function() {

  const renderAndWaitForLoad = async () => {
    render(<RolesList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  beforeEach(() => {
    when(Axios.get).calledWith(expect.stringContaining(URL)).mockResolvedValue({
      data: ROWS
    });
    when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: ACTION, method: METHODS.READ}))
        .mockResolvedValue({data: TestUtils.makeExtResult([])});

    when(ExtJS.state().getValue).calledWith('capabilityActiveTypes').mockReturnValue([]);
    when(ExtJS.state().getValue).calledWith('defaultRole').mockReturnValue(null);
    when(ExtJS.checkPermission).calledWith(Permissions.ROLES.CREATE).mockReturnValue(true);
    when(ExtJS.checkPermission).calledWith(Permissions.CAPABILITIES.READ).mockReturnValue(false);
  });

  it('renders the resolved empty data', async function() {
    when(Axios.get).calledWith(expect.stringContaining(URL)).mockResolvedValue({
      data: []
    });
    await renderAndWaitForLoad();

    expect(selectors.createButton()).not.toHaveClass('disabled');
    expect(selectors.emptyMessage()).toBeInTheDocument();
  });

  it('renders the resolved data', async function() {
    await renderAndWaitForLoad();

    TestUtils.expectTableHeaders(Object.values(LABELS.COLUMNS));
    TestUtils.expectTableRows(ROWS, Object.values(FIELDS));
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

    TestUtils.expectProperRowsOrder(ROWS, FIELDS.ID);

    userEvent.click(headerCell(LABELS.COLUMNS.ID));
    let roles = sortRoles(FIELDS.ID, descend);
    TestUtils.expectProperRowsOrder(roles, FIELDS.ID);

    userEvent.click(headerCell(LABELS.COLUMNS.NAME));
    roles = sortRoles(FIELDS.NAME);
    TestUtils.expectProperRowsOrder(roles, FIELDS.ID);

    userEvent.click(headerCell(LABELS.COLUMNS.NAME));
    roles = sortRoles(FIELDS.NAME, descend);
    TestUtils.expectProperRowsOrder(roles, FIELDS.ID);

    userEvent.click(headerCell(LABELS.COLUMNS.DESCRIPTION));
    roles = sortRoles(FIELDS.DESCRIPTION);
    TestUtils.expectProperRowsOrder(roles, FIELDS.ID);
  });

  it('filters by name and description', async function() {
    const {filter} = selectors;

    await renderAndWaitForLoad();

    await TestUtils.expectProperFilteredItemsCount(filter, '', ROWS.length);
    await TestUtils.expectProperFilteredItemsCount(filter, 'nx-admin', 1);
    await TestUtils.expectProperFilteredItemsCount(filter, 'nx-anonymous-name', 1);
    await TestUtils.expectProperFilteredItemsCount(filter, 'Provides privileges', 1);
  });

  it('disables the create button when not enough permissions', async function() {
    Axios.get.mockResolvedValue({data: []});
    when(ExtJS.checkPermission).calledWith(Permissions.ROLES.CREATE).mockReturnValue(false);

    await renderAndWaitForLoad();

    expect(selectors.createButton()).toHaveClass('disabled');
  });

  describe('Default Role Alert', function() {
    const {defaultRoleNameLink, defaultRoleCapabilityLink} = selectors;

    const defaultRoleCapabilityTypeId = 'defaultrole';
    const roleId = 'nx-anonymous';
    const roleName = 'nx-anonymous-name';
    const capabilityId = 'test_id';

    it('shows the default role alert when the user has capabilities read permission', async function() {
      when(Axios.post).calledWith(EXT_URL, expect.objectContaining({action: ACTION, method: METHODS.READ}))
          .mockResolvedValue({
            data: TestUtils.makeExtResult([{
              enabled: true,
              id: capabilityId,
              properties: {role: roleId},
              typeId: defaultRoleCapabilityTypeId,
            }])
          });
      when(ExtJS.checkPermission).calledWith(Permissions.CAPABILITIES.READ).mockReturnValue(true);
      when(ExtJS.state().getValue).calledWith('capabilityActiveTypes').mockReturnValue([defaultRoleCapabilityTypeId]);
      when(ExtJS.state().getValue).calledWith('defaultRole').mockReturnValue({
        id: roleId,
        name: roleName,
      });

      await renderAndWaitForLoad();

      expect(defaultRoleNameLink(roleName)).toBeInTheDocument();
      expect(defaultRoleNameLink(roleName)).toHaveAttribute('href', `#admin/security/roles:${roleId}`);

      expect(defaultRoleCapabilityLink()).toBeInTheDocument();
      expect(defaultRoleCapabilityLink()).toHaveAttribute('href', `#admin/system/capabilities:${capabilityId}`);
    });

    it('shows the default role alert when the user doesn\'t have capabilities read permission', async function() {
      when(ExtJS.checkPermission).calledWith(Permissions.CAPABILITIES.READ).mockReturnValue(false);
      when(ExtJS.state().getValue).calledWith('defaultRole').mockReturnValue({
        id: roleId,
        name: roleName,
      });

      await renderAndWaitForLoad();

      expect(defaultRoleNameLink(roleName)).toBeInTheDocument();
      expect(defaultRoleNameLink(roleName)).toHaveAttribute('href', `#admin/security/roles:${roleId}`);

      expect(defaultRoleCapabilityLink()).not.toBeInTheDocument();
    });

    it('doesn\'t show the default role alert when the capability is not enabled', async function() {
      when(ExtJS.state().getValue).calledWith('capabilityActiveTypes').mockReturnValue([]);
      when(ExtJS.checkPermission).calledWith(Permissions.CAPABILITIES.READ).mockReturnValue(true);

      await renderAndWaitForLoad();

      expect(defaultRoleNameLink(roleName)).not.toBeInTheDocument();
      expect(defaultRoleCapabilityLink()).not.toBeInTheDocument();
    });
  });
});
