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
import { RoleAssignedUsers } from '../RoleAssignedUsers';
import { useUsersApi } from '../../users/useUsersApi';
import { fetchAllUsersAcrossSources } from '../../../system/api/hooks/securityDirectoryCache';

// Mock the hooks
jest.mock('../../users/useUsersApi');
jest.mock('../../../system/api/hooks/securityDirectoryCache');

const mockUseUsersApi = useUsersApi as jest.MockedFunction<typeof useUsersApi>;
const mockFetchAllUsersAcrossSources = fetchAllUsersAcrossSources as jest.MockedFunction<typeof fetchAllUsersAcrossSources>;

const mockLocalUsers = [
  { userId: 'local-user', firstName: 'Local', lastName: 'User', status: 'active', source: 'default', roles: ['test-role'] },
];

const mockLdapUsers = [
  { userId: 'ldap-user', firstName: 'LDAP', lastName: 'User', status: 'active', source: 'LDAP', roles: ['test-role'] },
];

const mockLdapUserWithExternalRole = [
  { userId: 'ldap-external-user', firstName: 'LDAP External', lastName: 'User', status: 'active', source: 'LDAP', roles: [], externalRoles: ['external-role'] },
];

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('RoleAssignedUsers', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches users from all sources and filters by role', async () => {
    mockUseUsersApi.mockReturnValue({
      fetchUsers: jest.fn(),
      fetchSources: jest.fn(),
    } as any);

    // Mock fetchAllUsersAcrossSources to return users from multiple sources
    mockFetchAllUsersAcrossSources.mockResolvedValue([...mockLocalUsers, ...mockLdapUsers]);

    renderWithTheme(<RoleAssignedUsers roleId="test-role" />);

    // Wait for loading to finish
    await waitFor(() => {
      expect(screen.queryByText('Checking assigned users...')).not.toBeInTheDocument();
    });

    // Both local and LDAP users should be displayed
    expect(screen.getByText('local-user')).toBeInTheDocument();
    expect(screen.getByText('ldap-user')).toBeInTheDocument();
  });

  it('shows empty state when no users are assigned', async () => {
    mockUseUsersApi.mockReturnValue({
      fetchUsers: jest.fn(),
      fetchSources: jest.fn(),
    } as any);

    mockFetchAllUsersAcrossSources.mockResolvedValue([]);

    renderWithTheme(<RoleAssignedUsers roleId="unused-role" />);

    await waitFor(() => {
      expect(screen.queryByText('Checking assigned users...')).not.toBeInTheDocument();
    });

    expect(screen.getByText('No users are currently assigned this role.')).toBeInTheDocument();
  });

  it('handles error state', async () => {
    mockUseUsersApi.mockReturnValue({
      fetchUsers: jest.fn(),
      fetchSources: jest.fn(),
    } as any);

    mockFetchAllUsersAcrossSources.mockRejectedValue(new Error('Failed to fetch users'));

    renderWithTheme(<RoleAssignedUsers roleId="test-role" />);

    expect(await screen.findByText('Failed to fetch users')).toBeInTheDocument();
  });

  it('includes users with role in externalRoles (LDAP-mapped roles)', async () => {
    mockUseUsersApi.mockReturnValue({
      fetchUsers: jest.fn(),
      fetchSources: jest.fn(),
    } as any);

    // User has the role in externalRoles, not in roles
    mockFetchAllUsersAcrossSources.mockResolvedValue(mockLdapUserWithExternalRole);

    renderWithTheme(<RoleAssignedUsers roleId="external-role" />);

    await waitFor(() => {
      expect(screen.queryByText('Checking assigned users...')).not.toBeInTheDocument();
    });

    // User should be displayed even though role is in externalRoles, not roles
    expect(screen.getByText('ldap-external-user')).toBeInTheDocument();
  });
});
