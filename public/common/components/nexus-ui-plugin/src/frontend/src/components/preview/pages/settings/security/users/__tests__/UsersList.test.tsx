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

  it('should re-fetch users from the backend when the search filter changes (debounced)', async () => {
    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(mockFetchUsers).toHaveBeenCalledWith('', undefined);
    });

    const filterInput = screen.getByTestId('users-search');
    fireEvent.change(filterInput, { target: { value: 'jane' } });

    await waitFor(
      () => {
        expect(mockFetchUsers).toHaveBeenCalledWith('jane', undefined);
      },
      { timeout: 1500 }
    );
  });

  it('should find users beyond the initial page via server-side filter', async () => {
    const beyondPageUser = {
      userId: 'user101',
      firstName: 'Beyond',
      lastName: 'Page',
      emailAddress: 'beyond@example.com',
      source: 'LDAP',
      status: 'active',
      roles: [],
    };
    mockFetchUsers.mockImplementation(async (filter: string) => {
      if (filter === 'user101') {
        return [beyondPageUser];
      }
      return mockUsers;
    });

    render(
      <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
      { wrapper: TestWrapper }
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });
    expect(screen.queryByText('user101')).not.toBeInTheDocument();

    const filterInput = screen.getByTestId('users-search');
    fireEvent.change(filterInput, { target: { value: 'user101' } });

    await waitFor(
      () => {
        expect(screen.getByText('user101')).toBeInTheDocument();
      },
      { timeout: 1500 }
    );
    expect(screen.queryByText('admin')).not.toBeInTheDocument();
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

  describe('NEXUS-54435: no per-row actions column', () => {
    it('renders no icon-button controls inside any row', async () => {
      // The rows themselves are <tr role="button"> click affordances. Icon-column
      // controls, if present, would render as real <button> children inside the
      // row's <td> cells — NEXUS-54435 removes all of them.
      const { container } = render(
        <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('admin')).toBeInTheDocument();
      });

      const inCellButtons = container.querySelectorAll('td button');
      expect(inCellButtons.length).toBe(0);
    });

    it('renders no actions column header', async () => {
      render(
        <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('admin')).toBeInTheDocument();
      });

      // Data column headers exist; actions header must not.
      expect(screen.queryByRole('columnheader', { name: /actions/i })).not.toBeInTheDocument();
    });

    it('forwards getRowAriaLabel to each rendered row', async () => {
      const rowAriaLabel = (u: { userId: string }) => `Row for ${u.userId}`;

      render(
        <UsersList
          onSelect={mockOnSelect}
          onCreate={mockOnCreate}
          getRowAriaLabel={rowAriaLabel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('admin')).toBeInTheDocument();
      });

      // Both rows should adopt the parent-provided aria-label.
      const adminRow = screen.getByRole('button', { name: 'Row for admin' });
      const testuserRow = screen.getByRole('button', { name: 'Row for testuser' });
      expect(adminRow.tagName).toBe('TR');
      expect(testuserRow.tagName).toBe('TR');
    });

    it('falls back to the default aria-label when getRowAriaLabel is not provided', async () => {
      render(
        <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('admin')).toBeInTheDocument();
      });

      // EntityTable's default clickable-row label is `View ${rowKey}`, and the
      // Users list keys rows as `${userId}-${source}`.
      expect(screen.getByRole('button', { name: 'View admin-default' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'View testuser-LDAP' })).toBeInTheDocument();
    });

    it('still delegates row clicks to onSelect after the actions column removal', async () => {
      render(
        <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('testuser')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('testuser').closest('tr')!);
      expect(mockOnSelect).toHaveBeenCalledWith('testuser', 'LDAP');
    });
  });

  describe('NEXUS-54435 piggyback: Status column pill treatment', () => {
    // Pre-existing bug in the Status column surfaced during manual testing of the
    // row-click work: the cell rendered "Online Active" (dot+"Online" from
    // StatusBadge plus a sibling "Active" span). One signal, two labels.
    // Fix: single Radix Badge with variant="soft" and green/gray color mapping.

    const INACTIVE_USER = {
      userId: 'inactive-user',
      firstName: 'Inactive',
      lastName: 'User',
      emailAddress: 'inactive@example.com',
      source: 'default',
      status: 'disabled',
      roles: [],
    };

    it('renders Active users as a green soft Badge and Inactive users as a gray soft Badge', async () => {
      mockFetchUsers.mockResolvedValueOnce([mockUsers[0], INACTIVE_USER]);

      render(
        <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('admin')).toBeInTheDocument();
      });

      const activeRow = screen.getByRole('button', {name: 'View admin-default'});
      const inactiveRow = screen.getByRole('button', {name: 'View inactive-user-default'});
      const activeBadge = activeRow.querySelector('.rt-Badge');
      const inactiveBadge = inactiveRow.querySelector('.rt-Badge');
      expect(activeBadge).toHaveTextContent('Active');
      expect(activeBadge).toHaveAttribute('data-accent-color', 'green');
      expect(inactiveBadge).toHaveTextContent('Inactive');
      expect(inactiveBadge).toHaveAttribute('data-accent-color', 'gray');
    });

    it('does not render the pre-fix "Online" or "Offline" labels anywhere', async () => {
      mockFetchUsers.mockResolvedValueOnce([mockUsers[0], INACTIVE_USER]);

      render(
        <UsersList onSelect={mockOnSelect} onCreate={mockOnCreate} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('admin')).toBeInTheDocument();
      });

      expect(screen.queryByText('Online')).not.toBeInTheDocument();
      expect(screen.queryByText('Offline')).not.toBeInTheDocument();
    });
  });
});
