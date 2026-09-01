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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { PrivilegeProfilePage } from '../PrivilegeProfilePage';

const PRIVILEGE = {
  id: 'nx-all',
  name: 'nx-all',
  type: 'wildcard',
  description: 'All permissions',
  readOnly: true,
  permission: 'nexus:*',
  properties: { pattern: 'nexus:*' },
};

const ROLE = {
  id: 'nx-admin',
  name: 'nx-admin',
  source: 'default',
  description: 'Administrator Role',
};

const USER = {
  userId: 'admin',
  firstName: 'Administrator',
  lastName: 'User',
  status: 'active',
  source: 'default',
};

const mockProfile: {
  privilege: typeof PRIVILEGE;
  rolesUsing: Array<typeof ROLE>;
  usersWithAccess: Array<typeof USER>;
  loading: boolean;
  error: string | null;
} = {
  privilege: PRIVILEGE,
  rolesUsing: [ROLE],
  usersWithAccess: [USER],
  loading: false,
  error: null,
};

jest.mock('../usePrivilegeProfile', () => ({
  usePrivilegeProfile: () => mockProfile,
}));

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: { checkPermission: () => false },
}));

/**
 * NEXUS-52167: role/user names looked like hyperlinks but did nothing, because
 * they called openEntity() from a context whose provider is never mounted. These
 * assert on `href` rather than on a click handler because only a real href gives
 * middle-click, Cmd-click and "open in new tab".
 */
describe('PrivilegeProfilePage cross-entity navigation (NEXUS-52167)', () => {
  // Tests that mutate mockProfile must not leak into later ones, including when
  // an assertion throws before any inline restore would run.
  afterEach(() => {
    mockProfile.rolesUsing = [ROLE];
    mockProfile.usersWithAccess = [USER];
  });

  it('renders the role name as a link to the role profile', async () => {
    render(
      <PrivilegeProfilePage privilegeId="nx-all" onBack={jest.fn()} />
    );

    await userEvent.click(screen.getByRole('tab', { name: /Roles Using This/i }));

    expect(
      screen.getByRole('link', { name: 'Open role profile: nx-admin' })
    ).toHaveAttribute('href', '#preview/admin/security/roles/nx-admin/profile');
  });

  it('renders the user id as a link to the user profile, including source', async () => {
    render(
      <PrivilegeProfilePage privilegeId="nx-all" onBack={jest.fn()} />
    );

    await userEvent.click(screen.getByRole('tab', { name: /Users With Access/i }));

    expect(
      screen.getByRole('link', { name: 'Open user profile: admin' })
    ).toHaveAttribute('href', '#preview/admin/security/users/admin/default/profile');
  });

  // The User ID column is the only navigation affordance for a user. The Name
  // column navigated to the identical destination, so it is plain text now,
  // rendered like the Status and Source columns.
  it('renders the user full name as plain text, not a link', async () => {
    render(
      <PrivilegeProfilePage privilegeId="nx-all" onBack={jest.fn()} />
    );

    await userEvent.click(screen.getByRole('tab', { name: /Users With Access/i }));

    expect(screen.getByText('Administrator User')).toBeInTheDocument();
    // Exactly one link in the row — the User ID. The name must not be one.
    expect(screen.getAllByRole('link')).toHaveLength(1);
    expect(
      screen.getByRole('link', { name: 'Open user profile: admin' })
    ).toBeInTheDocument();
  });

  it('renders names as plain text with no links in embedMode', async () => {
    render(
      <PrivilegeProfilePage privilegeId="nx-all" onBack={jest.fn()} embedMode />
    );

    await userEvent.click(screen.getByRole('tab', { name: /Roles Using This/i }));

    expect(screen.queryAllByRole('link')).toHaveLength(0);
    // Name and ID columns both render 'nx-admin'; both must be plain text.
    expect(screen.getAllByText('nx-admin').length).toBeGreaterThan(0);
  });

  // NEXUS-52167 follow-up: the active tab is owned by the URL so it survives
  // navigating to a role/user profile and pressing browser Back. Before this,
  // tab state was local useState and every return landed on Overview.
  describe('tab state is controllable by the parent (URL-owned)', () => {
    it('renders the tab supplied by the parent instead of defaulting to overview', () => {
      render(
        <PrivilegeProfilePage
          privilegeId="nx-all"
          onBack={jest.fn()}
          activeTab="users"
          onTabChange={jest.fn()}
        />
      );

      // Users tab content is live without any click.
      expect(
        screen.getByRole('link', { name: 'Open user profile: admin' })
      ).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Users With Access/i })).toHaveAttribute(
        'aria-selected',
        'true'
      );
    });

    it('reports tab changes to the parent rather than self-managing', async () => {
      const onTabChange = jest.fn();
      render(
        <PrivilegeProfilePage
          privilegeId="nx-all"
          onBack={jest.fn()}
          activeTab="overview"
          onTabChange={onTabChange}
        />
      );

      await userEvent.click(screen.getByRole('tab', { name: /Roles Using This/i }));

      expect(onTabChange).toHaveBeenCalledWith('roles');
    });

    it('still manages its own tab when the parent supplies none', async () => {
      render(<PrivilegeProfilePage privilegeId="nx-all" onBack={jest.fn()} />);

      expect(screen.getByRole('tab', { name: /Overview/i })).toHaveAttribute(
        'aria-selected',
        'true'
      );

      await userEvent.click(screen.getByRole('tab', { name: /Roles Using This/i }));

      expect(screen.getByRole('tab', { name: /Roles Using This/i })).toHaveAttribute(
        'aria-selected',
        'true'
      );
    });
  });

  it('percent-encodes ids so slashes cannot break the route', async () => {
    mockProfile.rolesUsing = [{ ...ROLE, id: 'ops/admin role', name: 'Ops Admin' }];

    render(
      <PrivilegeProfilePage privilegeId="nx-all" onBack={jest.fn()} />
    );

    await userEvent.click(screen.getByRole('tab', { name: /Roles Using This/i }));

    expect(
      screen.getByRole('link', { name: 'Open role profile: Ops Admin' })
    ).toHaveAttribute(
      'href',
      '#preview/admin/security/roles/ops%2Fadmin%20role/profile'
    );
  });
});
