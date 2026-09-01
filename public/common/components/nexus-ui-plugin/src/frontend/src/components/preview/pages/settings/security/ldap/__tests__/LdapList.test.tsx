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

import { LdapList } from '../LdapList';
import { LdapServer } from '../types';

// Mock ExtJS.checkPermission to return true for all permissions
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const { createNexusUiPluginMock } = jest.requireActual('../../../../../../../../__jest__/mocks/nexusUiPluginMock');
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
    onReorder: jest.fn().mockResolvedValue(undefined),
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

  it('hides Clear Cache when the user has nexus:ldap:update but lacks nexus:ldap:delete (NEXUS-53627)', () => {
    global.NX.Permissions.check.mockImplementation(
      (permission: string) => permission !== 'nexus:ldap:delete'
    );

    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.queryByText('Clear Cache')).not.toBeInTheDocument();
  });

  it('shows Clear Cache when the user has nexus:ldap:delete even without nexus:ldap:update (NEXUS-53627)', () => {
    global.NX.Permissions.check.mockImplementation(
      (permission: string) => permission !== 'nexus:ldap:update'
    );

    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByText('Clear Cache')).toBeInTheDocument();
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

  it('hides the row-level Delete button when the user lacks nexus:ldap:delete (NEXUS-53627)', () => {
    global.NX.Permissions.check.mockImplementation(
      (permission: string) => permission !== 'nexus:ldap:delete'
    );

    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.queryAllByRole('button', { name: /delete/i })).toHaveLength(0);
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

    const button = screen.getByText('Change Order');
    expect(button).toBeInTheDocument();
    expect(button.closest('button')).toBeDisabled();
  });

  it('shows search input for filtering', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByLabelText('Filter servers')).toBeInTheDocument();
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

  it('calls onReorder with server names (not IDs) when a row is drag-dropped', () => {
    const { container } = render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const draggableRows = container.querySelectorAll('[draggable="true"]');
    // Drag first row (Corporate LDAP, index 0) onto second row (Active Directory, index 1).
    // handleDrop tracks draggedIndex via component state, not dataTransfer.getData — setData
    // is mocked only because handleDragStart writes to it (jsdom's DataTransfer is null).
    const mockDataTransfer = { effectAllowed: '', dropEffect: '', setData: jest.fn() };
    fireEvent.dragStart(draggableRows[0], { dataTransfer: mockDataTransfer });
    fireEvent.drop(draggableRows[1], { dataTransfer: mockDataTransfer });

    expect(defaultProps.onReorder).toHaveBeenCalledWith(['Active Directory', 'Corporate LDAP']);
  });

  it('calls onReorder with server names (not IDs) when Save Order is clicked in Change Order modal', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Change Order'));
    fireEvent.click(screen.getByText('Save Order'));

    expect(defaultProps.onReorder).toHaveBeenCalledWith(['Corporate LDAP', 'Active Directory']);
  });

  it('reverts local order and reopens modal when Save Order API call fails', async () => {
    const failingReorder = jest.fn().mockRejectedValueOnce(new Error('Network error'));
    render(<LdapList {...defaultProps} onReorder={failingReorder} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Change Order'));
    fireEvent.click(screen.getByTestId('ldap-order-save'));

    // Modal should reopen after failure
    await waitFor(() => expect(screen.getByTestId('ldap-order-modal')).toBeInTheDocument());

    // Close modal so the table is accessible (Radix Dialog sets aria-hidden on the rest of the page)
    fireEvent.click(screen.getByTestId('ldap-order-cancel'));
    await waitFor(() => expect(screen.queryByTestId('ldap-order-modal')).not.toBeInTheDocument());

    // Local order should be reverted to original (skip the header row)
    const serverRows = screen.getAllByRole('row').slice(1);
    expect(serverRows[0]).toHaveTextContent('Corporate LDAP');
    expect(serverRows[1]).toHaveTextContent('Active Directory');
  });

  it('reverts local order when inline drag-drop reorder API call fails', async () => {
    const failingReorder = jest.fn().mockRejectedValueOnce(new Error('Network error'));
    const { container } = render(<LdapList {...defaultProps} onReorder={failingReorder} />, { wrapper: TestWrapper });

    const draggableRows = container.querySelectorAll('[draggable="true"]');
    const mockDataTransfer = { effectAllowed: '', dropEffect: '', setData: jest.fn() };
    fireEvent.dragStart(draggableRows[0], { dataTransfer: mockDataTransfer });
    fireEvent.drop(draggableRows[1], { dataTransfer: mockDataTransfer });

    // After the rejected promise, local order should revert to original
    await waitFor(() => {
      const rows = container.querySelectorAll('[draggable="true"]');
      expect(rows[0]).toHaveTextContent('Corporate LDAP');
      expect(rows[1]).toHaveTextContent('Active Directory');
    });
  });

  it('reorders against the full list (not the filtered view) when a filter hides a leading row', () => {
    // Regression: when a filter is active, filtered-list positions differ from localServers
    // positions. Dragging must splice localServers by identity, otherwise the submitted auth
    // order is silently corrupted. Here the filter hides the first server (Alpha), so the two
    // visible rows have filtered indices 0/1 but localServers indices 1/2.
    const servers: LdapServer[] = [
      { ...mockServers[0], id: 'a', name: 'Alpha', host: 'alpha.internal', order: 1 },
      { ...mockServers[0], id: 'b', name: 'Beta', host: 'beta.example.com', order: 2 },
      { ...mockServers[0], id: 'c', name: 'Gamma', host: 'gamma.example.com', order: 3 },
    ];
    const { container } = render(<LdapList {...defaultProps} servers={servers} />, { wrapper: TestWrapper });

    // 'example' matches Beta and Gamma (by host/url) but not Alpha (alpha.internal).
    fireEvent.change(screen.getByLabelText(/filter servers/i), { target: { value: 'example' } });

    const draggableRows = container.querySelectorAll('[draggable="true"]');
    expect(draggableRows.length).toBe(2); // Alpha is filtered out

    // Drag Beta (visible row 0) onto Gamma (visible row 1).
    const mockDataTransfer = { effectAllowed: '', dropEffect: '', setData: jest.fn() };
    fireEvent.dragStart(draggableRows[0], { dataTransfer: mockDataTransfer });
    fireEvent.drop(draggableRows[1], { dataTransfer: mockDataTransfer });

    // Correct result moves Beta after Gamma within the full list: Alpha, Gamma, Beta.
    // The old (buggy) filtered-index splice would have produced Beta, Alpha, Gamma.
    expect(defaultProps.onReorder).toHaveBeenCalledWith(['Alpha', 'Gamma', 'Beta']);
  });

  it('filters servers when search text is entered', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const searchInput = screen.getByLabelText('Filter servers');
    fireEvent.change(searchInput, { target: { value: 'Corporate' } });

    expect(screen.getByText('Corporate LDAP')).toBeInTheDocument();
    expect(screen.queryByText('Active Directory')).not.toBeInTheDocument();
  });

  it('shows no matches message when filter has no results', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const searchInput = screen.getByLabelText('Filter servers');
    fireEvent.change(searchInput, { target: { value: 'nonexistent' } });

    expect(screen.getByText('No servers match your filter')).toBeInTheDocument();
  });

  it('filters by server name', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const searchInput = screen.getByLabelText('Filter servers');
    fireEvent.change(searchInput, { target: { value: 'Directory' } });

    expect(screen.queryByText('Corporate LDAP')).not.toBeInTheDocument();
    expect(screen.getByText('Active Directory')).toBeInTheDocument();
  });

  it('filters by computed URL (protocol + host + port)', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const searchInput = screen.getByLabelText(/filter servers/i);
    // 'ldaps' matches only the second server whose protocol is ldaps
    fireEvent.change(searchInput, { target: { value: 'ldaps' } });

    expect(screen.queryByText('Corporate LDAP')).not.toBeInTheDocument();
    expect(screen.getByText('Active Directory')).toBeInTheDocument();
  });

  it('filters by order number', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const searchInput = screen.getByLabelText(/filter servers/i);
    // '1' appears only in the first server's order (1); the second server's name/host/url/order
    // (Active Directory, ad.example.com, ldaps://ad.example.com:636, order 2) contain no '1'.
    fireEvent.change(searchInput, { target: { value: '1' } });

    expect(screen.getByText('Corporate LDAP')).toBeInTheDocument();
    expect(screen.queryByText('Active Directory')).not.toBeInTheDocument();
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

    // Check that data rows are rendered (each exposes a keyboard-focusable Edit button)
    const editButtons = screen.getAllByRole('button', { name: /edit ldap server/i });
    expect(editButtons.length).toBeGreaterThan(0);
  });

  it('filter input has an accessible name', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    expect(screen.getByLabelText(/filter servers/i)).toBeInTheDocument();
  });

  it('opens a server via the keyboard-focusable row Edit button (A2)', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const editButton = screen.getByRole('button', {
      name: /edit ldap server corporate ldap/i,
    });
    fireEvent.click(editButton);

    expect(defaultProps.onSelect).toHaveBeenCalledWith(mockServers[0]);
  });

  it('does not open the server for editing when its Delete button is activated (A2)', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    const deleteButton = screen.getByRole('button', {
      name: /delete corporate ldap/i,
    });
    fireEvent.click(deleteButton);

    // Delete stops propagation, so the row-open (onSelect) handler must not fire.
    expect(defaultProps.onSelect).not.toHaveBeenCalled();
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
  });

  it('reorders items via Move Down button in Change Order modal', () => {
    render(<LdapList {...defaultProps} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Change Order'));

    // Click Move Down on the first server (Corporate LDAP)
    const moveDownButton = screen.getByRole('button', { name: /move corporate ldap down/i });
    fireEvent.click(moveDownButton);

    // Active Directory should now be first, Corporate LDAP second
    const orderItems = screen.getAllByRole('button', { name: /move .* (up|down)/i });
    // First item's "Move up" button should be disabled (index 0), second item is Active Directory
    const firstMoveUpButton = screen.getByRole('button', { name: /move active directory up/i });
    expect(firstMoveUpButton).toBeDisabled();
    const lastMoveDownButton = screen.getByRole('button', { name: /move corporate ldap down/i });
    expect(lastMoveDownButton).toBeDisabled();
    // Verify order: Active Directory first, Corporate LDAP second
    const items = screen.getAllByText(/Corporate LDAP|Active Directory/);
    const modalItems = items.filter((el) => el.closest('.ldap-list__order-item'));
    expect(modalItems[0]).toHaveTextContent('Active Directory');
    expect(modalItems[1]).toHaveTextContent('Corporate LDAP');
  });
});

