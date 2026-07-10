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
import { render, screen, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

import { UserProfilePage } from '../UserProfilePage';

const mockUser = {
  userId: 'deployer',
  realm: 'default',
  source: 'default',
  firstName: 'Deploy',
  lastName: 'User',
  emailAddress: 'deploy@example.com',
  status: 'active',
  roles: ['nx-anonymous', 'nx-deploy'],
};

// Stable mock reference - returning new jest.fn() each call caused useEffect to re-run infinitely
// because fetchUser was in the dependency array and changed every render (jest.mock is hoisted,
// so we define the stable fn inside the factory)
const mockUserData = {
  userId: 'deployer',
  realm: 'default',
  source: 'default',
  firstName: 'Deploy',
  lastName: 'User',
  emailAddress: 'deploy@example.com',
  status: 'active',
  roles: ['nx-anonymous', 'nx-deploy'],
};

jest.mock('../useUsersApi', () => {
  const mockFetchUser = jest.fn();
  return {
    useUsersApi: () => ({
      fetchUser: mockFetchUser,
    }),
    __mockFetchUser: mockFetchUser,
  };
});

jest.mock('../useUserTree', () => ({
  useUserTree: () => ({
    tree: [],
    loading: false,
    error: null,
    toggleExpand: jest.fn(),
    expandAll: jest.fn(),
    collapseAll: jest.fn(),
    setSearchTerm: jest.fn(),
  }),
}));

jest.mock('../useUserEffectivePrivileges', () => ({
  useUserEffectivePrivileges: () => ({
    privileges: [],
    roleMap: new Map(),
    loading: false,
    error: null,
  }),
}));

describe('UserProfilePage', () => {
  const defaultProps = {
    userId: 'deployer',
    userSource: 'default',
    onBack: jest.fn(),
    canEdit: false,
  };

  beforeEach(() => {
    const { __mockFetchUser } = require('../useUsersApi') as { __mockFetchUser: jest.Mock };
    __mockFetchUser.mockResolvedValue(mockUserData);
  });

  it('renders Overview tab with user metadata', async () => {
    render(
      <Theme>
        <UserProfilePage {...defaultProps} />
      </Theme>
    );

    expect(await screen.findByRole('heading', { name: 'deployer', level: 1 })).toBeInTheDocument();

    expect(screen.getByText('Deploy')).toBeInTheDocument();
    expect(screen.getByText('User')).toBeInTheDocument();
    expect(screen.getByText('deploy@example.com')).toBeInTheDocument();
  });

  it('shows Overview, Roles, Privileges, Security Tree tabs', async () => {
    render(
      <Theme>
        <UserProfilePage {...defaultProps} />
      </Theme>
    );

    await waitFor(() => {
      expect(screen.getByRole('tab', { name: /overview/i })).toBeInTheDocument();
    });

    expect(screen.getByRole('tab', { name: /roles/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /privileges/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /security tree/i })).toBeInTheDocument();
  });

  it('calls onBack when Users breadcrumb is clicked', async () => {
    const onBack = jest.fn();
    render(
      <Theme>
        <UserProfilePage {...defaultProps} onBack={onBack} />
      </Theme>
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Users' })).toBeInTheDocument();
    });

    screen.getByRole('button', { name: 'Users' }).click();
    expect(onBack).toHaveBeenCalled();
  });
});
