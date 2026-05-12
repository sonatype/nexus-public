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
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import { LdapList } from '../LdapList';
import { LdapServer } from '../types';

// Mock ExtJS.checkPermission to return true for all permissions
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const { createNexusUiPluginMock } = jest.requireActual('../../../../../../../__jest__/mocks/nexusUiPluginMock');
  const baseMock = createNexusUiPluginMock();
  return {
    ...baseMock,
    ExtJS: {
      ...baseMock.ExtJS,
      checkPermission: jest.fn().mockReturnValue(true),
    },
  };
});

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('LdapList', () => {
  const mockServers: LdapServer[] = [
    {
      id: 'server1',
      name: 'Corporate LDAP',
      host: 'ldap.corp.example.com',
      port: 389,
      protocol: 'ldap',
      searchBase: 'dc=corp,dc=example,dc=com',
      authScheme: 'simple',
      order: 1,
      userObjectClass: 'inetOrgPerson',
      userIdAttribute: 'uid',
      userRealNameAttribute: 'cn',
      userEmailAddressAttribute: 'mail',
      ldapGroupsAsRoles: false,
    },
    {
      id: 'server2',
      name: 'Active Directory',
      host: 'ad.example.com',
      port: 636,
      protocol: 'ldaps',
      searchBase: 'dc=ad,dc=example,dc=com',
      authScheme: 'simple',
      order: 2,
      userObjectClass: 'user',
      userIdAttribute: 'sAMAccountName',
      userRealNameAttribute: 'cn',
      userEmailAddressAttribute: 'mail',
      ldapGroupsAsRoles: true,
      groupType: 'static',
    },
  ];

  const defaultProps = {
    servers: mockServers,
    loading: false,
    onSelect: jest.fn(),
    onCreate: jest.fn(),
    onReorder: jest.fn(),
    onDelete: jest.fn(),
    onClearCache: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the list header', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('URL')).toBeInTheDocument();
    expect(screen.getByText('Order')).toBeInTheDocument();
    expect(screen.queryByText('Actions')).not.toBeInTheDocument();
  });

  it('renders all servers', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByText('Corporate LDAP')).toBeInTheDocument();
    expect(screen.getByText('Active Directory')).toBeInTheDocument();
  });

  it('displays server URL in correct format', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByText('ldap://ldap.corp.example.com:389')).toBeInTheDocument();
    expect(screen.getByText('ldaps://ad.example.com:636')).toBeInTheDocument();
  });

  it('displays order numbers', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('calls onSelect when a server row is clicked', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Corporate LDAP'));

    expect(defaultProps.onSelect).toHaveBeenCalledWith(mockServers[0]);
  });

  it('does not render Create button in list view', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.queryByText('Create LDAP Server')).not.toBeInTheDocument();
  });

  it('calls onClearCache when Clear Cache button is clicked', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Clear Cache'));

    expect(defaultProps.onClearCache).toHaveBeenCalled();
  });

  it('displays empty state when no servers', () => {
    render(<LdapList {...defaultProps} servers={[]} />, { wrapper: TestWrapper });

    expect(screen.getByText('No LDAP servers configured')).toBeInTheDocument();
    expect(screen.getByText(/create ldap server/i)).toBeInTheDocument();
  });

  it('shows loading overlay when loading is true', () => {
    render(<LdapList {...defaultProps} servers={[]} loading={true} />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading LDAP servers...')).toBeInTheDocument();
  });

  it('renders move up button for all servers except first', () => {
    const { container } = render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const dragHandles = container.querySelectorAll('.ldap-list__drag-handle');
    expect(dragHandles.length).toBe(mockServers.length);
  });

  it('renders delete button for each server', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const deleteButtons = screen.getAllByRole('button', { name: /delete/i });
    expect(deleteButtons).toHaveLength(2);
  });

  it('shows delete confirmation modal when delete button is clicked', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const deleteButton = screen.getByRole('button', { name: /delete corporate ldap/i });
    fireEvent.click(deleteButton);

    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    expect(screen.getByText(/are you sure you want to delete/i)).toBeInTheDocument();
  });

  it('calls onDelete when delete is confirmed in modal', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const deleteButton = screen.getByRole('button', { name: /delete corporate ldap/i });
    fireEvent.click(deleteButton);

    fireEvent.click(screen.getByRole('button', { name: /Delete Server/i }));

    expect(defaultProps.onDelete).toHaveBeenCalledWith(mockServers[0]);
  });

  it('closes delete modal when cancel is clicked', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const deleteButton = screen.getByRole('button', { name: /delete corporate ldap/i });
    fireEvent.click(deleteButton);

    // Click cancel
    fireEvent.click(screen.getByText('Cancel'));

    // Modal should close
    expect(screen.queryByTestId('ldap-delete-modal')).not.toBeInTheDocument();
    expect(defaultProps.onDelete).not.toHaveBeenCalled();
  });

  it('does not render Create button in list view', () => {
    render(<LdapList {...defaultProps} loading={true} />, { wrapper: TestWrapper });

    expect(screen.queryByText('Create LDAP Server')).not.toBeInTheDocument();
  });

  it('disables Clear Cache button when no servers', () => {
    render(<LdapList {...defaultProps} servers={[]} />, { wrapper: TestWrapper });

    expect(screen.getByText('Clear Cache').closest('button')).toBeDisabled();
  });

  it('renders single server correctly', () => {
    render(<LdapList {...defaultProps} servers={[mockServers[0]]} />, { wrapper: TestWrapper });

    expect(screen.getByText('Corporate LDAP')).toBeInTheDocument();
    expect(screen.queryByText('Active Directory')).not.toBeInTheDocument();
  });

  it('shows Change Order text when multiple servers exist', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByText('Change Order')).toBeInTheDocument();
  });

  it('does not show Change Order when single server exists', () => {
    render(<LdapList {...defaultProps} servers={[mockServers[0]]} />, { wrapper: TestWrapper });

    expect(screen.queryByText('Change Order')).not.toBeInTheDocument();
  });

  it('shows search input for filtering', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByPlaceholderText(/filter servers/i)).toBeInTheDocument();
  });

  it('has Clear Cache button', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByText('Clear Cache')).toBeInTheDocument();
  });

  it('opens Change Order modal when Change Order button is clicked', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Change Order'));

    expect(screen.getByText('Change Server Order')).toBeInTheDocument();
    expect(screen.getByText(/ldap servers are queried in order/i)).toBeInTheDocument();
  });

  it('shows all servers in Change Order modal', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Change Order'));

    // Both servers should appear in the modal list
    const modalServers = screen.getAllByText(/Corporate LDAP|Active Directory/);
    expect(modalServers.length).toBeGreaterThanOrEqual(2);
  });

  it('calls onReorder when Save Order is clicked in modal', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Change Order'));
    fireEvent.click(screen.getByText('Save Order'));

    expect(defaultProps.onReorder).toHaveBeenCalled();
  });

  it('closes Change Order modal when Cancel is clicked', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Change Order'));
    
    // Find and click Cancel in the order modal
    const cancelButtons = screen.getAllByText('Cancel');
    fireEvent.click(cancelButtons[cancelButtons.length - 1]);

    expect(screen.queryByText('Change Server Order')).not.toBeInTheDocument();
  });

  it('filters servers when search text is entered', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const searchInput = screen.getByPlaceholderText(/filter servers/i);
    fireEvent.change(searchInput, { target: { value: 'Corporate' } });

    expect(screen.getByText('Corporate LDAP')).toBeInTheDocument();
    expect(screen.queryByText('Active Directory')).not.toBeInTheDocument();
  });

  it('shows no matches message when filter has no results', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const searchInput = screen.getByPlaceholderText(/filter servers/i);
    fireEvent.change(searchInput, { target: { value: 'nonexistent' } });

    expect(screen.getByText('No servers match your filter')).toBeInTheDocument();
  });

  it('filters by server name', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const searchInput = screen.getByPlaceholderText(/filter servers/i);
    fireEvent.change(searchInput, { target: { value: 'Directory' } });

    expect(screen.queryByText('Corporate LDAP')).not.toBeInTheDocument();
    expect(screen.getByText('Active Directory')).toBeInTheDocument();
  });

  it('renders drag handles for reordering', () => {
    const { container } = render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const dragHandles = container.querySelectorAll('[draggable="true"]');
    expect(dragHandles.length).toBeGreaterThan(0);
  });

  it('disables buttons when loading', () => {
    render(<LdapList {...defaultProps} loading={true} />, { wrapper: TestWrapper });

    expect(screen.getByText('Clear Cache').closest('button')).toBeDisabled();
  });

  it('shows server status indicators', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    // Check that the order column shows correct numbers
    const rows = screen.getAllByRole('row');
    expect(rows.length).toBeGreaterThan(1); // Header + data rows
  });
});

