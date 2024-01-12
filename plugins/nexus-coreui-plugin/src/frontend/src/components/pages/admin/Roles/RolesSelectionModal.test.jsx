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

import RolesSelectionModal from "./RolesSelectionModal";
import { render, within, screen } from '@testing-library/react';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import userEvent from '@testing-library/user-event';

const ROLES = [
    {
        description: 'Administrator Role',
        id: 'nx-admin',
        name: 'nx-admin',
        privileges: ['nx-all'],
        roles: [],
        source: 'default'
    },
    {
        name: "nx-analytics-all",
        description: "All permissions for Analytics",
        privileges: ['nx-healthcheck-read'],
        roles: [],
    },
    {
        name: "nx-apikey-all",
        description: "All permissions for APIKey",
        privileges: ['nx-healthcheck-read'],
        roles: [],
    },
    {
        name: "nx-blobstores-all",
        description: "All permissions for Blobstores",
        privileges: ['nx-healthcheck-read'],
        roles: [],
    },
    {
        name: "nx-blobstores-create",
        description: "Create permission for Blobstores",
        privileges: ['nx-healthcheck-read'],
        roles: [],
    },
    {
        name: "nx-blobstores-update",
        description: "Update permission for Blobstores",
        privileges: ['nx-healthcheck-read'],
        roles: [],
    },
    {
        name: "nx-bundles-all",
        description: "All permissions for Bundles",
        privileges: ['nx-healthcheck-read'],
        roles: [],
    },
    {
        name: "nx-bundles-read",
        description: "Read permission for Bundles",
        privileges: ['nx-healthcheck-read'],
        roles: [],
    },
    {
        name: "nx-capabilities-all",
        description: "All permissions for Capabilities",
        privileges: ['nx-healthcheck-read'],
        roles: [],
    },
    {
        name: "nx-capabilities-create",
        description: "Create permission for Capabilities",
        privileges: ['nx-healthcheck-read'],
        roles: [],
    },
    {
        name: "nx-capabilities-delete",
        description: "Delete permission for Capabilities",
        privileges: ['nx-healthcheck-read'],
        roles: [],
    },
    {
        name: "nx-capabilities-read",
        description: "Read permission for Capabilities",
        privileges: ['nx-all'],
        roles: [],
    },
    {
        name: "nx-capabilities-update",
        description: "Update permission for Capabilities",
        privileges: ['nx-all'],
        roles: [],
    },
    {
        name: "nx-component-upload",
        description: "Upload component permission",
        privileges: ['nx-all'],
        roles: [],
    },
    {
        name: "nx-crowd-all",
        description: "All permissions for Crowd",
        privileges: ['nx-all'],
        roles: [],
    },
    {
        name: "nx-crowd-read",
        description: "Read permission for Crowd",
        privileges: ['nx-all'],
        roles: [],
    },
    {
        name: "nx-crowd-update",
        description: "Update permission for Crowd",
        privileges: ['nx-all'],
        roles: [],
    },
    {
        name: "nx-datastores-all",
        description: "All permissions for Datastores",
        privileges: ['nx-all'],
        roles: [],
    },
    {
        name: "nx-datastores-create",
        description: "Create permission for Datastores",
        privileges: ['nx-all'],
        roles: [],
    },
    {
        name: "nx-datastores-delete",
        description: "Delete permission for Datastores",
        privileges: ['nx-all'],
        roles: [],
    },
];

const selectors = {
    filter: () => screen.queryByPlaceholderText('Filter'),
    modal: () => screen.getByRole('dialog'),
};

describe('RolesSelectionModal', function () {

    function renderModal(roles) {
        return render(<RolesSelectionModal title={"Roles"} allRoles={roles} onModalClose={() => { }} selectedRoles={[]} />);
    }

    it('sorts the selected above the rest of the roles alphabetically when sorting select ascending', async function () {
        const { modal } = selectors;
        renderModal(ROLES);

        const tableRow = (index) => modal().querySelectorAll('tbody tr')[index];
        expect(tableRow(0).cells[1]).toHaveTextContent('nx-admin');
        expect(tableRow(0).cells[2]).toHaveTextContent('Administrator Role');
        expect(tableRow(1).cells[1]).toHaveTextContent('nx-analytics-all');
        expect(tableRow(2).cells[1]).toHaveTextContent('nx-apikey-all');

        const tableRowHeader = (index) => modal().querySelectorAll('thead tr')[index];
        expect(tableRowHeader(1).cells[2].textContent).toBe('0 Selected');

        const checkboxes = (index) => within(modal()).queryAllByRole('checkbox')[index];
        userEvent.click(checkboxes(2));

        userEvent.click(tableRowHeader(0).cells[0]);

        expect(tableRowHeader(1).cells[2].textContent).toBe('1 Selected');
        expect(tableRow(0).cells[1]).toHaveTextContent('nx-apikey-all');

        userEvent.click(checkboxes(0));
        userEvent.click(tableRowHeader(0).cells[2]);

        expect(tableRowHeader(1).cells[2].textContent).toBe('0 Selected');
        expect(tableRow(0).cells[1]).toHaveTextContent('nx-admin');
        expect(tableRow(2).cells[1]).toHaveTextContent('nx-apikey-all');
    });

    it('filters out roles based on the text the user inputs', async function () {
        const { filter, modal } = selectors;
        renderModal(ROLES);

        expect(modal()).toBeInTheDocument();

        const tableRow = (index) => modal().querySelectorAll('tbody tr')[index];
        expect(tableRow(0).cells[1]).toHaveTextContent('nx-admin');
        expect(tableRow(1).cells[1]).toHaveTextContent('nx-analytics-all');
        expect(tableRow(2).cells[1]).toHaveTextContent('nx-apikey-all');

        await TestUtils.changeField(filter, 'ana');
        expect(tableRow(0).cells[1]).toHaveTextContent('nx-analytics-all');

        await TestUtils.changeField(filter, 'z');
        expect(tableRow(0).cells[1]).not.toBe;

        await TestUtils.changeField(filter, '');
        expect(tableRow(0).cells[1]).toHaveTextContent('nx-admin');
    });

    it('changes to page 1 when filtering', async function () {
        const { filter, modal } = selectors;
        renderModal(ROLES);

        const navigation = within(modal()).getByRole('navigation', { name: 'pagination' });
        const page2 = within(navigation).getByText('2');
        userEvent.click(page2);
        expect(page2).toHaveAttribute('aria-current', 'page');

        await TestUtils.changeField(filter, 'n');
        const page1 = within(navigation).getByText('1');
        expect(page1).toHaveAttribute('aria-current', 'page');
    });

    it('pagination display 10 roles at a time', async function () {
        const { modal } = selectors;
        renderModal(ROLES);
        expect(modal()).toBeInTheDocument();

        const navigation = within(modal()).getByRole('navigation', { name: 'pagination' });
        const pages = (index) => within(navigation).getAllByRole('button')[index];

        const tableRow = (index) => modal().querySelectorAll('tbody tr')[index];
        expect(tableRow(0).cells[1]).toHaveTextContent('nx-admin');

        expect(pages(0)).toHaveAttribute('aria-current', 'page');
        expect(pages(0)).toHaveTextContent('1');

        const page1 = within(navigation).getByText('1');
        expect(page1).toHaveAttribute('aria-current', 'page');

        const page2 = within(navigation).getByText('2');
        userEvent.click(page2);

        expect(page2).toHaveAttribute('aria-current', 'page');
        expect(tableRow(0).cells[1]).toHaveTextContent('nx-capabilities-delete');
    });
})
