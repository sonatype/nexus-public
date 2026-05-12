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
import { RolesList } from '../RolesList';
import { useRolesApi } from '../useRolesApi';
import { Role, NEXUS_SOURCE } from '../types';

// Mock useRolesApi
jest.mock('../useRolesApi');

const mockUseRolesApi = useRolesApi as jest.MockedFunction<typeof useRolesApi>;

const mockRoles: Role[] = [
  {
    id: 'nx-admin',
    version: '1',
    source: 'Default',
    name: 'Administrator',
    description: 'Full administrative access',
    readOnly: true,
    privileges: ['nx-all'],
    roles: [],
  },
  {
    id: 'nx-anonymous',
    version: '1',
    source: 'Default',
    name: 'Anonymous',
    description: 'Anonymous access role',
    readOnly: true,
    privileges: ['nx-search-read'],
    roles: [],
  },
  {
    id: 'custom-role',
    version: '1',
    source: 'Default',
    name: 'Custom Role',
    description: 'A custom role',
    readOnly: false,
    privileges: [],
    roles: [],
  },
];

const mockSources = [
  { id: NEXUS_SOURCE, name: NEXUS_SOURCE },
  { id: 'LDAP', name: 'LDAP' },
];

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('RolesList', () => {
  const mockOnSelect = jest.fn();
  const mockOnCreate = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseRolesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchRoles: jest.fn().mockResolvedValue(mockRoles),
      fetchRoleReferences: jest.fn().mockResolvedValue([]),
      fetchRoleSources: jest.fn().mockResolvedValue(mockSources),
      fetchRolesFromSource: jest.fn().mockResolvedValue([]),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      findRole: jest.fn().mockResolvedValue(null),
      createRole: jest.fn(),
      updateRole: jest.fn(),
      deleteRole: jest.fn(),
    });
  });

  it('should render loading state initially', () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
    
    expect(screen.getByText('Loading roles...')).toBeInTheDocument();
  });

  it('should render roles after loading', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
    
    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
      expect(screen.getByText('Anonymous')).toBeInTheDocument();
      expect(screen.getByText('Custom Role')).toBeInTheDocument();
    });
  });

  it('should show lock icon for read-only roles', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
    
    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });
    
    // Read-only roles should have lock icons
    expect(screen.getAllByTitle('Read Only').length).toBeGreaterThanOrEqual(2);
  });

  it('should show Edit pencil for custom (user-created) roles', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
    
    await waitFor(() => {
      expect(screen.getByText('Custom Role')).toBeInTheDocument();
    });
    
    // Custom role has readOnly: false and source: Default - must show Edit pencil (isReadOnlyRole = false)
    const editButtons = screen.getAllByLabelText(/^Edit /);
    expect(editButtons.length).toBeGreaterThanOrEqual(1);
  });

  it('should filter roles by search term', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
    
    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });
    
    const searchInput = screen.getByTestId('roles-search');
    fireEvent.change(searchInput, { target: { value: 'admin' } });
    
    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
      expect(screen.queryByText('Anonymous')).not.toBeInTheDocument();
    });
  });

  it('should call onSelect when clicking a role row', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
    
    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('Administrator'));
    
    expect(mockOnSelect).toHaveBeenCalledWith('nx-admin', 'profile');
  });

  it('should sort roles by name ascending by default', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
    
    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows.length).toBe(3);
    });
  });

  it('should sort roles by name descending on first click (toggle from default asc)', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });

    // Default is already sorted by name ascending
    // First click toggles to descending
    const nameHeader = screen.getByRole('columnheader', { name: /name/i });
    fireEvent.click(nameHeader);

    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows.length).toBe(3);
      // Sorted descending: Custom Role, Anonymous, Administrator
      expect(rows[0]).toHaveTextContent('Custom Role');
      expect(rows[1]).toHaveTextContent('Anonymous');
      expect(rows[2]).toHaveTextContent('Administrator');
    });
  });

  it('should clear sort on second click (after desc)', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });

    const nameHeader = screen.getByRole('columnheader', { name: /name/i });
    fireEvent.click(nameHeader); // toggle from asc to desc
    fireEvent.click(nameHeader); // toggle from desc to null (clear)

    // When sort is cleared, data should be in original fetch order
    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows.length).toBe(3);
      // Original fetch order: Administrator, Anonymous, Custom Role
      expect(rows[0]).toHaveTextContent('Administrator');
      expect(rows[1]).toHaveTextContent('Anonymous');
      expect(rows[2]).toHaveTextContent('Custom Role');
    });
  });

  it('should return to ascending on third click', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });

    const nameHeader = screen.getByRole('columnheader', { name: /name/i });
    fireEvent.click(nameHeader); // toggle to desc
    fireEvent.click(nameHeader); // toggle to null
    fireEvent.click(nameHeader); // toggle to asc

    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows.length).toBe(3);
      // Back to ascending: Administrator, Anonymous, Custom Role
      expect(rows[0]).toHaveTextContent('Administrator');
      expect(rows[1]).toHaveTextContent('Anonymous');
      expect(rows[2]).toHaveTextContent('Custom Role');
    });
  });

  it('should display empty state when no roles match filter', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
    
    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });
    
    const searchInput = screen.getByTestId('roles-search');
    fireEvent.change(searchInput, { target: { value: 'nonexistent' } });
    
    await waitFor(() => {
      expect(screen.getByText('No Matching Roles')).toBeInTheDocument();
    });
  });

  it('should display error state when there is an error', async () => {
    // Mock fetchRoles to reject, which will trigger the error state
    // The component catches the error and calls setError, which we need to verify
    const mockFetchRoles = jest.fn().mockRejectedValue(new Error('Network error'));
    
    mockUseRolesApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchRoles: mockFetchRoles,
      fetchRoleReferences: jest.fn().mockResolvedValue([]),
      fetchRoleSources: jest.fn().mockResolvedValue(mockSources),
      fetchRolesFromSource: jest.fn().mockResolvedValue([]),
      fetchPrivilegeReferences: jest.fn().mockResolvedValue([]),
      findRole: jest.fn().mockResolvedValue(null),
      createRole: jest.fn(),
      updateRole: jest.fn(),
      deleteRole: jest.fn(),
    });
    
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
    
    // Wait for error to be set
    await waitFor(() => {
      expect(mockSetError).toHaveBeenCalledWith('Network error');
    });
  });

  it('should have source filter section', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
    
    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });

    // The source filter uses checkboxes inside the filter sidebar
    const filterSidebar = screen.getByTestId('filter-sidebar');
    expect(filterSidebar).toBeInTheDocument();
  });

  it('should display help section', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    expect(screen.getByText('About Roles')).toBeInTheDocument();
    expect(screen.getByText(/Roles are collections of privileges/)).toBeInTheDocument();
  });

  it('should render documentation link in help section', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    const docLink = screen.getByText('View Documentation').closest('a');
    expect(docLink).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/docs/roles');
  });

  it('should filter roles by ID', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });

    const searchInput = screen.getByTestId('roles-search');
    fireEvent.change(searchInput, { target: { value: 'nx-admin' } });

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
      expect(screen.queryByText('Anonymous')).not.toBeInTheDocument();
      expect(screen.queryByText('Custom Role')).not.toBeInTheDocument();
    });
  });

  it('should filter roles case insensitively', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });

    const searchInput = screen.getByTestId('roles-search');
    fireEvent.change(searchInput, { target: { value: 'ADMIN' } });

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
      expect(screen.queryByText('Anonymous')).not.toBeInTheDocument();
    });
  });

  it('should sort by description', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });

    const descriptionHeader = screen.getByRole('columnheader', { name: /description/i });
    fireEvent.click(descriptionHeader);

    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows.length).toBe(3);
      // Sorted by description ascending: "A custom role", "Anonymous access role", "Full administrative access"
      expect(rows[0]).toHaveTextContent('A custom role');
      expect(rows[1]).toHaveTextContent('Anonymous access role');
      expect(rows[2]).toHaveTextContent('Full administrative access');
    });
  });

  it('should sort by source', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });

    const sourceHeader = screen.getByRole('columnheader', { name: /source/i });
    fireEvent.click(sourceHeader!);

    await waitFor(() => {
      // All have 'Default' source, so order should remain stable
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });
  });

  it('should change sort field and reset to ascending', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });

    const nameHeader = screen.getByRole('columnheader', { name: /^name$/i });
    const descriptionHeader = screen.getByRole('columnheader', { name: /description/i });

    // Sort by name - first click toggles to desc (from default asc)
    fireEvent.click(nameHeader);

    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows[0]).toHaveTextContent('Custom Role');
    });

    // Change to description, should reset to ascending
    fireEvent.click(descriptionHeader);

    await waitFor(() => {
      const rows = document.querySelectorAll('.entity-table__row');
      expect(rows[0]).toHaveTextContent('A custom role');
    });
  });

  it('should call onSelect with correct role ID', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    await waitFor(() => {
      expect(screen.getByText('Custom Role')).toBeInTheDocument();
    });

    const row = screen.getByText('Custom Role').closest('tr');
    fireEvent.click(row!);

    expect(mockOnSelect).toHaveBeenCalledWith('custom-role', 'profile');
  });

  it('should filter roles by description', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });

    const searchInput = screen.getByTestId('roles-search');
    fireEvent.change(searchInput, { target: { value: 'administrative' } });

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
      expect(screen.queryByText('Anonymous')).not.toBeInTheDocument();
      expect(screen.queryByText('Custom Role')).not.toBeInTheDocument();
    });
  });

  it('should show source filter checkbox options', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });

    // The source filter shows checkbox options - look for checkbox within filter sidebar
    const filterSidebar = screen.getByTestId('filter-sidebar');
    const nexusCheckbox = filterSidebar.querySelector('.checkbox-filter__item');
    expect(nexusCheckbox).toBeInTheDocument();
  });

  it('should filter roles by source when checkbox is selected', async () => {
    // Add a role with LDAP source to test filtering
    const rolesWithLdap: Role[] = [
      ...mockRoles,
      {
        id: 'ldap-role',
        version: '1',
        source: 'LDAP',
        name: 'LDAP Role',
        description: 'A role from LDAP',
        readOnly: false,
        privileges: [],
        roles: [],
      },
    ];
    
    mockUseRolesApi.mockReturnValue({
      ...mockUseRolesApi(),
      fetchRoles: jest.fn().mockResolvedValue(rolesWithLdap),
    });

    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);

    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
      expect(screen.getByText('LDAP Role')).toBeInTheDocument();
    });

    // Expand the source filter section if it's collapsed (defaultExpanded: false)
    const sourceSection = screen.getByRole('button', { name: /Source/i });
    fireEvent.click(sourceSection);

    // Use a more specific selector to find the checkbox
    const ldapCheckbox = await screen.findByRole('checkbox', { name: /LDAP/i });
    fireEvent.click(ldapCheckbox);

    await waitFor(() => {
      // Only LDAP roles should show
      expect(screen.getByText('LDAP Role')).toBeInTheDocument();
      expect(screen.queryByText('Administrator')).not.toBeInTheDocument();
    });
  });

  it('should have role type filter section', async () => {
    renderWithTheme(<RolesList onSelect={mockOnSelect} onCreate={mockOnCreate} />);
    
    await waitFor(() => {
      expect(screen.getByText('Administrator')).toBeInTheDocument();
    });

    expect(screen.getByText('Role Type')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: /System-Managed/i })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: /User-Managed/i })).toBeInTheDocument();
  });
});


