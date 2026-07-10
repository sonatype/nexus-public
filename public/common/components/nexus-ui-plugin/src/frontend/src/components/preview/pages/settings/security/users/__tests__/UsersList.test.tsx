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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { UsersList } from '../UsersList';
import * as useUsersApiModule from '../useUsersApi';

// Mock the API hook
jest.mock('../useUsersApi');

const mockedUseUsersApi = useUsersApiModule.useUsersApi as jest.MockedFunction<typeof useUsersApiModule.useUsersApi>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const mockUsers = [
  {
    userId: 'admin',
    firstName: 'Admin',
    lastName: 'User',
    emailAddress: 'admin@example.com',
    source: 'default',
    status: 'active',
    roles: ['nx-admin'],
  },
  {
    userId: 'testuser',
    firstName: 'Test',
    lastName: 'User',
    emailAddress: 'test@example.com',
    source: 'LDAP',
    status: 'active',
    roles: ['nx-anonymous'],
  },
];

const mockSources = [
  { id: 'default', name: 'Local' },
  { id: 'LDAP', name: 'LDAP Server' },
];

describe('UsersList', () => {
  const mockOnSelect = jest.fn();
  const mockOnCreate = jest.fn();
  const mockFetchUsers = jest.fn();
  const mockFetchSources = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchUsers.mockResolvedValue(mockUsers);
    mockFetchSources.mockResolvedValue(mockSources);
    mockedUseUsersApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchUser: jest.fn(),
      fetchUsers: mockFetchUsers,
      fetchSources: mockFetchSources,
      fetchRoles: jest.fn(),
      createUser: jest.fn(),
      updateUser: jest.fn(),
      deleteUser: jest.fn(),
      changePassword: jest.fn(),
      resetUserToken: jest.fn(),
      inviteUser: jest.fn(),
    });
  });

  it('loads users on mount', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(mockFetchUsers).toHaveBeenCalled();
    });
  });

  it('renders users after loading', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });
    
    expect(screen.getByText('testuser')).toBeInTheDocument();
  });

  it('calls onSelect when a user row is clicked', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });
    
    const adminRow = screen.getByText('admin').closest('tr');
    if (adminRow) {
      fireEvent.click(adminRow);
    }
    
    expect(mockOnSelect).toHaveBeenCalledWith('admin', 'default');
  });

  it('handles error state', async () => {
    mockedUseUsersApi.mockReturnValue({
      loading: false,
      error: 'Network error',
      setError: mockSetError,
      fetchUser: jest.fn(),
      fetchUsers: mockFetchUsers.mockRejectedValue(new Error('Network error')),
      fetchSources: mockFetchSources,
      fetchRoles: jest.fn(),
      createUser: jest.fn(),
      updateUser: jest.fn(),
      deleteUser: jest.fn(),
      changePassword: jest.fn(),
      resetUserToken: jest.fn(),
    });
    
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument();
    });
  });

  it('renders filter input', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });
    
    expect(screen.getByTestId('users-search')).toBeInTheDocument();
  });

  it('renders table headers', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );
    
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });
    
    expect(screen.getByRole('columnheader', { name: /user id/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /source/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /first name/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /last name/i })).toBeInTheDocument();
  });

  it('renders help section', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    expect(screen.getByText('About Users')).toBeInTheDocument();
  });

  it('should render documentation link in help section', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    const docLink = screen.getByText('View Documentation').closest('a');
    expect(docLink).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/docs/users');
  });

  it('should sort users by User ID descending on first click (toggle from default asc)', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    // Default is sorted by userId ascending, first click toggles to descending
    const userIdHeader = screen.getByRole('columnheader', { name: /user id/i });
    fireEvent.click(userIdHeader);

    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows.length).toBe(2);
      // Sorted by userId descending: testuser, admin
      expect(rows[0]).toHaveTextContent('testuser');
      expect(rows[1]).toHaveTextContent('admin');
    });
  });

  it('should clear sort on second click (after desc)', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    const userIdHeader = screen.getByRole('columnheader', { name: /user id/i });
    fireEvent.click(userIdHeader); // toggle to desc
    fireEvent.click(userIdHeader); // toggle to null (clear)

    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows.length).toBe(2);
      // Original order: admin, testuser
      expect(rows[0]).toHaveTextContent('admin');
      expect(rows[1]).toHaveTextContent('testuser');
    });
  });

  it('should return to ascending on third click', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    const userIdHeader = screen.getByRole('columnheader', { name: /user id/i });
    fireEvent.click(userIdHeader); // toggle to desc
    fireEvent.click(userIdHeader); // toggle to null
    fireEvent.click(userIdHeader); // toggle to asc

    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows.length).toBe(2);
      // Back to ascending: admin, testuser
      expect(rows[0]).toHaveTextContent('admin');
      expect(rows[1]).toHaveTextContent('testuser');
    });
  });

  it('should sort by First Name', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    const firstNameHeader = screen.getByRole('columnheader', { name: /first name/i });
    fireEvent.click(firstNameHeader);

    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows.length).toBe(2);
      // Sorted by firstName ascending: Admin, Test
      expect(rows[0]).toHaveTextContent('Admin');
      expect(rows[1]).toHaveTextContent('Test');
    });
  });

  it('should sort by Last Name', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    const lastNameHeader = screen.getByRole('columnheader', { name: /last name/i });
    fireEvent.click(lastNameHeader);

    await waitFor(() => {
      // Both have lastName "User", order should be stable
      expect(screen.getByText('admin')).toBeInTheDocument();
    });
  });

  it('should sort by Source', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    const sourceHeader = screen.getByRole('columnheader', { name: /source/i });
    fireEvent.click(sourceHeader);

    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows.length).toBe(2);
      // Sorted by source - all have 'Local', order should be stable
      expect(rows[0]).toHaveTextContent('admin');
    });
  });

  it('should change sort field and reset to ascending', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    const userIdHeader = screen.getByRole('columnheader', { name: /user id/i });
    const firstNameHeader = screen.getByRole('columnheader', { name: /first name/i });

    // Sort by userId - first click toggles to desc (from default asc)
    fireEvent.click(userIdHeader);

    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows[0]).toHaveTextContent('testuser');
    });

    // Change to firstName, should reset to ascending
    fireEvent.click(firstNameHeader);

    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows[0]).toHaveTextContent('Admin');
    });
  });

  it('should filter users client-side', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
      expect(screen.getByText('testuser')).toBeInTheDocument();
    });

    const filterInput = screen.getByTestId('users-search');
    fireEvent.change(filterInput, { target: { value: 'admin' } });

    // Client-side filtering should hide 'testuser' and show 'admin'
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
      expect(screen.queryByText('testuser')).not.toBeInTheDocument();
    });
  });

  it('should render source filter', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    // The source filter is in the filter sidebar
    const filterSidebar = screen.getByTestId('filter-sidebar');
    expect(filterSidebar).toBeInTheDocument();
  });

  it('should call onSelect with userId and source', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('testuser')).toBeInTheDocument();
    });

    const row = screen.getByText('testuser').closest('tr');
    fireEvent.click(row!);

    expect(mockOnSelect).toHaveBeenCalledWith('testuser', 'LDAP');
  });

  it('should display empty state when no users', async () => {
    mockFetchUsers.mockResolvedValue([]);

    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText(/no users/i)).toBeInTheDocument();
    });
  });

  it('should display status badges', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    // Both users have 'active' status, displayed as "Active"
    const statusLabels = screen.getAllByText('Active');
    expect(statusLabels.length).toBeGreaterThanOrEqual(1);
  });

  describe('cloud distribution (isCloud=true)', () => {
    it('fetches users with OAuth2 source', async () => {
      render(
        <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} isCloud />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(mockFetchUsers).toHaveBeenCalledWith('', 'OAuth2');
      });
    });

    it('hides the source filter section', async () => {
      render(
        <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} isCloud />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByTestId('filter-sidebar')).toBeInTheDocument();
      });

      const sidebar = screen.getByTestId('filter-sidebar');
      expect(sidebar).not.toHaveTextContent('Source');
    });

    it('still shows the status filter section', async () => {
      render(
        <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} isCloud />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('Status')).toBeInTheDocument();
      });
    });
  });

  describe('self-hosted distribution (isCloud=false)', () => {
    it('fetches users from all sources', async () => {
      render(
        <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(mockFetchUsers).toHaveBeenCalledWith('', undefined);
      });
    });

    it('shows the source filter section', async () => {
      render(
        <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        const sidebar = screen.getByTestId('filter-sidebar');
        expect(sidebar).toHaveTextContent('Source');
      });
    });
  });
});
