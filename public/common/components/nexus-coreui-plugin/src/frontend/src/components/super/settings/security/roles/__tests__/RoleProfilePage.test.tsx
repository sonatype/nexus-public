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
import { RoleProfilePage } from '../RoleProfilePage';
import { useRolesApi } from '../useRolesApi';
import { useUsersApi } from '../../users/useUsersApi';
import { useRoleTree } from '../useRoleTree';

// Mock the hooks
jest.mock('../useRolesApi');
jest.mock('../../users/useUsersApi');
jest.mock('../useRoleTree');

const mockUseRolesApi = useRolesApi as jest.MockedFunction<typeof useRolesApi>;
const mockUseUsersApi = useUsersApi as jest.MockedFunction<typeof useUsersApi>;
const mockUseRoleTree = useRoleTree as jest.MockedFunction<typeof useRoleTree>;

const mockRole = {
  id: 'test-role',
  name: 'Test Role',
  source: 'default',
  description: 'A test role description',
  privileges: [],
  roles: [],
  readOnly: false,
  version: '1',
};

const mockUsers = [
  { userId: 'user1', firstName: 'John', lastName: 'Doe', status: 'active', source: 'default', roles: ['test-role'] },
];

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('RoleProfilePage', () => {
  const mockOnBack = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    
    mockUseRolesApi.mockReturnValue({
      findRole: jest.fn().mockResolvedValue(mockRole),
    } as any);

    mockUseUsersApi.mockReturnValue({
      fetchUsers: jest.fn().mockResolvedValue(mockUsers),
    } as any);

    mockUseRoleTree.mockReturnValue({
      tree: [],
      effectivePrivileges: [],
      loading: false,
      error: null,
      toggleExpand: jest.fn(),
    } as any);
  });

  it('renders correctly', async () => {
    renderWithTheme(<RoleProfilePage roleName="test-role" onBack={mockOnBack} />);

    // Wait for data to load
    await waitFor(() => {
      expect(screen.queryByTestId('loading-state')).not.toBeInTheDocument();
    });

    // Check tabs are present (using getAllByText because Radix renders duplicate hidden spans)
    expect(screen.getAllByText(/Security Tree/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Effective Permissions/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Assigned Users/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Overview/i).length).toBeGreaterThan(0);
  });

  it('handles error state', async () => {
    mockUseRolesApi.mockReturnValue({
      findRole: jest.fn().mockRejectedValue(new Error('Failed to fetch role')),
    } as any);

    renderWithTheme(<RoleProfilePage roleName="test-role" onBack={mockOnBack} />);

    expect(await screen.findByText('Failed to fetch role')).toBeInTheDocument();
  });
});
