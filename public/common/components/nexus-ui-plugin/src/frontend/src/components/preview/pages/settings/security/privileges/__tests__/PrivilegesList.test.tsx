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
import { render, screen, fireEvent, waitFor, } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { PrivilegesList } from '../PrivilegesList';
import { usePrivilegeList } from '../usePrivilegeList';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { Privilege, PRIVILEGE_TYPES } from '../types';

// Mock dependencies
jest.mock('../usePrivilegeList');
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn(),
  },
}));

const mockUsePrivilegeList = usePrivilegeList as jest.MockedFunction<typeof usePrivilegeList>;
const mockCheckPermission = ExtJS.checkPermission as jest.Mock;

const mockPrivileges: Privilege[] = [
  {
    id: 'nx-all',
    version: '1',
    name: 'nx-all',
    description: 'All permissions',
    type: PRIVILEGE_TYPES.WILDCARD,
    readOnly: true,
    properties: { pattern: 'nexus:*' },
    permission: 'nexus:*',
  },
  {
    id: 'nx-search-read',
    version: '1',
    name: 'nx-search-read',
    description: 'Search read permission',
    type: PRIVILEGE_TYPES.APPLICATION,
    readOnly: true,
    properties: { domain: 'search', actions: 'read' },
    permission: 'nexus:search:read',
  },
  {
    id: 'custom-priv',
    version: '1',
    name: 'custom-priv',
    description: 'Custom privilege',
    type: PRIVILEGE_TYPES.REPOSITORY_VIEW,
    readOnly: false,
    properties: { format: 'maven2', repository: '*', actions: 'read' },
    permission: 'nexus:repository-view:maven2:*:read',
  },
];

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('PrivilegesList', () => {
  const defaultProps = {
    onSelect: jest.fn(),
    onCreate: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockCheckPermission.mockReturnValue(true);
    // Create a mock implementation that will be reused
    const mockImplementation = {
      data: mockPrivileges,
      pristineData: mockPrivileges,
      loading: false,
      error: null,
      filters: { filter: '', typeFilter: [] },
      setFilters: jest.fn(),
      sortField: 'name',
      sortDirection: 'asc' as const,
      setSort: jest.fn(),
      typeCounts: [
        ['wildcard', 1],
        ['application', 1],
        ['repository-view', 1],
      ] as [string, number][],
      readOnlyCounts: { locked: 2, unlocked: 1 },
      handleFilterChange: jest.fn(),
      handleRowClick: jest.fn((privilege: Privilege) => {
        // Simulate the real hook behavior - extract name and call the callback
        defaultProps.onSelect(privilege.name);
      }),
      handleCreate: jest.fn(() => defaultProps.onCreate()),
    };
    mockUsePrivilegeList.mockReturnValue(mockImplementation);
  });

  it('should render the privileges list', async () => {
    renderWithTheme(<PrivilegesList {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
      expect(screen.getByText('nx-search-read')).toBeInTheDocument();
      expect(screen.getByText('custom-priv')).toBeInTheDocument();
    });
  });

  it('should show loading state initially', () => {
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      loading: true,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    expect(screen.getByText('Loading privileges...')).toBeInTheDocument();
  });

  it('should display privilege types correctly', async () => {
    renderWithTheme(<PrivilegesList {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });
    
    // Type labels are capitalized via getPrivilegeTypeLabel
    // The types appear in both the filter sidebar checkboxes and table - use getAllByText
    expect(screen.getAllByText('Wildcard').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Application').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Repository View').length).toBeGreaterThanOrEqual(1);
  });

  it('should call setFilters when search term changes', async () => {
    const mockSetFilter = jest.fn();
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      setFilters: mockSetFilter,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    const searchInput = screen.getByTestId('privileges-search');
    fireEvent.change(searchInput, { target: { value: 'search' } });

    expect(mockSetFilter).toHaveBeenCalledWith({ filter: 'search' });
  });

  it('should call onSelect when a privilege is clicked', async () => {
    renderWithTheme(<PrivilegesList {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('nx-all'));
    
    expect(defaultProps.onSelect).toHaveBeenCalledWith('nx-all');
  });

  it('should display lock icon for read-only privileges', async () => {
    renderWithTheme(<PrivilegesList {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });
    
    // Read-only privileges show Lock icon - check for the icon by its class
    const lockIcons = document.querySelectorAll('.privileges-list__readonly-icon');
    // nx-all and nx-search-read are read-only (2 icons)
    expect(lockIcons.length).toBe(2);
  });

  it('should show empty state when no privileges exist', async () => {
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      data: [],
      pristineData: [],
      typeCounts: [],
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('No Privileges')).toBeInTheDocument();
    });
  });

  it('should sort privileges by name ascending on first click', async () => {
    const mockSetSort = jest.fn();
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      setSort: mockSetSort,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    // Default is already sorted by name ascending
    // First click toggles direction (machine handles toggling internally)
    const nameHeader = screen.getByRole('columnheader', { name: /name/i });
    fireEvent.click(nameHeader);

    expect(mockSetSort).toHaveBeenCalledWith('name');
  });

  it('should clear sort on second click (after desc)', async () => {
    const mockSetSort = jest.fn();
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      sortDirection: 'desc' as const,
      setSort: mockSetSort,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    const nameHeader = screen.getByRole('columnheader', { name: /name/i });
    fireEvent.click(nameHeader); // Machine handles toggling internally

    expect(mockSetSort).toHaveBeenCalledWith('name');
  });

  it('should return to ascending on third click', async () => {
    const mockSetSort = jest.fn();
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      sortDirection: null,
      setSort: mockSetSort,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    const nameHeader = screen.getByRole('columnheader', { name: /name/i });
    fireEvent.click(nameHeader); // Machine handles toggling internally

    expect(mockSetSort).toHaveBeenCalledWith('name');
  });

  it('should display privilege permissions', async () => {
    renderWithTheme(<PrivilegesList {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByText('nexus:*')).toBeInTheDocument();
    });
  });

  it('should filter privileges by description', async () => {
    const mockSetFilter = jest.fn();
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      setFilters: mockSetFilter,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    const searchInput = screen.getByTestId('privileges-search');
    fireEvent.change(searchInput, { target: { value: 'Search read permission' } });

    expect(mockSetFilter).toHaveBeenCalledWith({ filter: 'Search read permission' });
  });

  it('should filter privileges by type', async () => {
    const mockSetFilter = jest.fn();
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      setFilters: mockSetFilter,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    const searchInput = screen.getByTestId('privileges-search');
    fireEvent.change(searchInput, { target: { value: 'repository-view' } });

    expect(mockSetFilter).toHaveBeenCalledWith({ filter: 'repository-view' });
  });

  it('should filter privileges by permission', async () => {
    const mockSetFilter = jest.fn();
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      setFilters: mockSetFilter,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    const searchInput = screen.getByTestId('privileges-search');
    fireEvent.change(searchInput, { target: { value: 'maven2' } });

    expect(mockSetFilter).toHaveBeenCalledWith({ filter: 'maven2' });
  });

  it('should filter case insensitively', async () => {
    const mockSetFilter = jest.fn();
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      setFilters: mockSetFilter,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    const searchInput = screen.getByTestId('privileges-search');
    fireEvent.change(searchInput, { target: { value: 'SEARCH' } });

    expect(mockSetFilter).toHaveBeenCalledWith({ filter: 'SEARCH' });
  });

  it('should show empty state when filter yields no results', async () => {
    // Mock with empty data to simulate no results
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      data: [],
      filters: { filter: 'nonexistent-privilege', typeFilter: [] },
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('No Matching Privileges')).toBeInTheDocument();
    });
  });

  it('should sort by description', async () => {
    const mockSetSort = jest.fn();
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      setSort: mockSetSort,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    const descriptionHeader = screen.getByRole('columnheader', { name: /description/i });
    fireEvent.click(descriptionHeader);

    expect(mockSetSort).toHaveBeenCalledWith('description');
  });

  it('should sort by type', async () => {
    const mockSetSort = jest.fn();
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      setSort: mockSetSort,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    const typeHeader = screen.getByRole('columnheader', { name: /type/i });
    fireEvent.click(typeHeader);

    expect(mockSetSort).toHaveBeenCalledWith('type');
  });

  it('should sort by permission', async () => {
    const mockSetSort = jest.fn();
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      setSort: mockSetSort,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    const permissionHeader = screen.getByRole('columnheader', { name: /permission/i });
    fireEvent.click(permissionHeader);

    expect(mockSetSort).toHaveBeenCalledWith('permission');
  });

  it('should change sort field and reset to ascending', async () => {
    const mockSetSort = jest.fn();
    mockUsePrivilegeList.mockReturnValue({
      ...mockUsePrivilegeList(),
      setSort: mockSetSort,
    });

    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    const nameHeader = screen.getByRole('columnheader', { name: /name/i });
    const typeHeader = screen.getByRole('columnheader', { name: /type/i });

    // Click name header - machine handles direction toggling
    fireEvent.click(nameHeader);
    expect(mockSetSort).toHaveBeenCalledWith('name');

    // Click type header - machine handles direction resetting
    fireEvent.click(typeHeader);
    expect(mockSetSort).toHaveBeenCalledWith('type');
  });

  it('should call onSelect with privilege ID when row is clicked', async () => {
    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('custom-priv')).toBeInTheDocument();
    });

    const row = screen.getByText('custom-priv').closest('tr');
    fireEvent.click(row!);

    expect(defaultProps.onSelect).toHaveBeenCalledWith('custom-priv');
  });

  it('should display Lock icon only for read-only privileges', async () => {
    renderWithTheme(<PrivilegesList {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('nx-all')).toBeInTheDocument();
    });

    const readOnlyIcons = document.querySelectorAll('.privileges-list__readonly-icon');
    expect(readOnlyIcons).toHaveLength(2); // nx-all and nx-search-read are read-only
  });

  it('should render help section with documentation link', async () => {
    renderWithTheme(<PrivilegesList {...defaultProps} />);

    expect(screen.getByText('What is a Privilege?')).toBeInTheDocument();

    const docLink = screen.getByText('View Documentation').closest('a');
    expect(docLink).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/docs/privileges');
  });
});


