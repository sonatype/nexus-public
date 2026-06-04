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
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { LdapPage } from '../LdapPage';
import * as useLdapApiModule from '../useLdapApi';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock the API hook
jest.mock('../useLdapApi');

const mockedUseLdapApi = useLdapApiModule.useLdapApi as jest.MockedFunction<typeof useLdapApiModule.useLdapApi>;

// Extend global mock with controllable checkPermission
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

// Get reference to the actual mock after jest.mock is hoisted
const getMockCheckPermission = () => {
  const { ExtJS } = require('@sonatype/nexus-ui-plugin');
  return ExtJS.checkPermission;
};

// Mock child components
jest.mock('../LdapList', () => ({
  LdapList: function MockLdapList({ servers, onSelect, onCreate, onReorder, onDelete, onClearCache }: any) {
    return (
      <div data-testid="ldap-list">
        <span>Servers: {servers.length}</span>
        {servers.map((s: any) => (
          <div key={s.id}>
            <button onClick={() => onSelect(s)}>
              {s.name}
            </button>
            <button onClick={() => onDelete(s)} data-testid={`delete-${s.id}`}>
              Delete {s.name}
            </button>
          </div>
        ))}
        <button onClick={onCreate}>Create LDAP Server</button>
        <button onClick={onClearCache}>Clear Cache</button>
      </div>
    );
  },
}));

jest.mock('../LdapForm', () => ({
  LdapForm: function MockLdapForm({ server, isCreate, onSave, onCancel }: any) {
    return (
      <div data-testid="ldap-form">
        <span>{isCreate ? 'Create LDAP Server' : 'Edit LDAP Server'}</span>
        <button data-testid="button-submit" onClick={() => onSave({ name: 'Test Server', host: 'ldap.example.com', port: 389, searchBase: 'dc=example,dc=com', authScheme: 'simple', protocol: 'ldap', userObjectClass: 'inetOrgPerson', userIdAttribute: 'uid', userRealNameAttribute: 'cn', userEmailAddressAttribute: 'mail', ldapGroupsAsRoles: false })}>
          Save
        </button>
        <button data-testid="form-cancel" onClick={onCancel}>Cancel</button>
      </div>
    );
  },
}));

// Wrapper component for Radix Theme and Toast context
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

// Helper to simulate URL navigation which the component uses for back navigation
function simulateHashChange(hash: string) {
  // Set the hash and dispatch the hashchange event
  act(() => {
    Object.defineProperty(window, 'location', {
      value: { ...window.location, hash },
      writable: true,
    });
    window.dispatchEvent(new HashChangeEvent('hashchange'));
  });
}

describe('LdapPage', () => {
  const originalLocation = window.location;

  beforeAll(() => {
    // Mock window.location.hash for URL-based navigation
    Object.defineProperty(window, 'location', {
      value: {
        ...originalLocation,
        hash: '#preview/admin/security/ldap',
      },
      writable: true,
    });
  });

  afterAll(() => {
    window.location = originalLocation;
  });

  const mockServers = [
    {
      id: 'server1',
      name: 'LDAP Server 1',
      host: 'ldap1.example.com',
      port: 389,
      protocol: 'ldap' as const,
      searchBase: 'dc=example,dc=com',
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
      name: 'LDAP Server 2',
      host: 'ldap2.example.com',
      port: 636,
      protocol: 'ldaps' as const,
      searchBase: 'dc=example2,dc=com',
      authScheme: 'simple',
      order: 2,
      userObjectClass: 'inetOrgPerson',
      userIdAttribute: 'uid',
      userRealNameAttribute: 'cn',
      userEmailAddressAttribute: 'mail',
      ldapGroupsAsRoles: true,
      groupType: 'static' as const,
    },
  ];

  const mockFetchServers = jest.fn();
  const mockCreateServer = jest.fn();
  const mockUpdateServer = jest.fn();
  const mockDeleteServer = jest.fn();
  const mockChangeOrder = jest.fn();
  const mockClearCache = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // Reset hash to base path before each test
    (window as any).location.hash = '#preview/admin/security/ldap';
    getMockCheckPermission().mockReturnValue(true);
    mockedUseLdapApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchServers: mockFetchServers.mockResolvedValue(mockServers),
      fetchServer: jest.fn(),
      fetchTemplates: jest.fn().mockResolvedValue([]),
      createServer: mockCreateServer.mockResolvedValue(mockServers[0]),
      updateServer: mockUpdateServer.mockResolvedValue(mockServers[0]),
      deleteServer: mockDeleteServer.mockResolvedValue(undefined),
      changeOrder: mockChangeOrder.mockResolvedValue(undefined),
      clearCache: mockClearCache.mockResolvedValue(undefined),
      verifyConnection: jest.fn().mockResolvedValue(undefined),
      verifyUserMapping: jest.fn().mockResolvedValue([]),
      verifyLogin: jest.fn().mockResolvedValue(undefined),
    });
  });

  it('renders loading state initially', () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading LDAP servers...')).toBeInTheDocument();
  });

  it('renders the page header', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP')).toBeInTheDocument();
    });

    expect(screen.getByText('Manage LDAP server connections for user authentication')).toBeInTheDocument();
  });

  it('displays LDAP server list', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });

    expect(screen.getByText('Servers: 2')).toBeInTheDocument();
  });

  it('shows create button in list view', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getAllByText('Create LDAP Server').length).toBeGreaterThan(0);
    });
  });

  it('navigates to create form when Create button is clicked', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });

    const createButtons = screen.getAllByText('Create LDAP Server');
    fireEvent.click(createButtons[0]);

    simulateHashChange('#preview/admin/security/ldap/create');

    await waitFor(() => {
      expect(screen.getByTestId('ldap-form')).toBeInTheDocument();
    });
  });

  it('navigates to edit form when server is selected', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Server 1')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('LDAP Server 1'));

    simulateHashChange('#preview/admin/security/ldap/LDAP%20Server%201');

    await waitFor(() => {
      expect(screen.getByTestId('ldap-form')).toBeInTheDocument();
    });
  });

  it('returns to list view when Cancel is clicked', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });

    // Go to create form
    const createButtons = screen.getAllByText('Create LDAP Server');
    fireEvent.click(createButtons[0]);
    simulateHashChange('#preview/admin/security/ldap/create');

    await waitFor(() => {
      expect(screen.getByTestId('ldap-form')).toBeInTheDocument();
    });

    // Click cancel - this triggers URL navigation which fires hashchange
    fireEvent.click(screen.getByText('Cancel'));

    // Simulate the hashchange event that the browser would fire
    simulateHashChange('#preview/admin/security/ldap');

    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });
  });

  it('navigates back to list after saving new server', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });

    // Go to create form
    const createButtons = screen.getAllByText('Create LDAP Server');
    fireEvent.click(createButtons[0]);
    simulateHashChange('#preview/admin/security/ldap/create');

    await waitFor(() => {
      expect(screen.getByTestId('ldap-form')).toBeInTheDocument();
    });

    // Click save - the mock LdapForm calls onSave which triggers navigation
    // Note: The actual API call happens in useLdapForm, not in LdapPage.handleSave
    fireEvent.click(screen.getByText('Save'));

    // Simulate the hashchange event that the browser would fire
    simulateHashChange('#preview/admin/security/ldap');

    // Should return to list view
    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });
  });

  it('navigates back to list after saving existing server', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Server 1')).toBeInTheDocument();
    });

    // Select server for editing
    fireEvent.click(screen.getByText('LDAP Server 1'));
    simulateHashChange('#preview/admin/security/ldap/LDAP%20Server%201');

    await waitFor(() => {
      expect(screen.getByTestId('ldap-form')).toBeInTheDocument();
    });

    // Click save
    fireEvent.click(screen.getByText('Save'));
    simulateHashChange('#preview/admin/security/ldap');

    // Should return to list view
    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });
  });

  it('calls clearCache when Clear Cache is clicked', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Clear Cache')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Clear Cache'));

    await waitFor(() => {
      expect(mockClearCache).toHaveBeenCalled();
    });
  });

  it('displays error message when error occurs', async () => {
    mockedUseLdapApi.mockReturnValue({
      loading: false,
      error: 'Failed to load LDAP servers',
      setError: mockSetError,
      fetchServers: mockFetchServers.mockResolvedValue([]),
      fetchServer: jest.fn(),
      fetchTemplates: jest.fn().mockResolvedValue([]),
      createServer: mockCreateServer,
      updateServer: mockUpdateServer,
      deleteServer: mockDeleteServer,
      changeOrder: mockChangeOrder,
      clearCache: mockClearCache,
      verifyConnection: jest.fn(),
      verifyUserMapping: jest.fn(),
      verifyLogin: jest.fn(),
    });

    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load LDAP servers')).toBeInTheDocument();
    });
  });

  // Note: Success toast messages are now shown by useLdapForm's save service (inside LdapForm),
  // not by LdapPage.handleSave. Toast tests should be in LdapForm.test.tsx or useLdapForm.test.ts.

  it('refreshes the list after creating a new server', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });

    // Initial load should have called fetchServers once
    expect(mockFetchServers).toHaveBeenCalledTimes(1);

    // Go to create form
    const createButtons = screen.getAllByText('Create LDAP Server');
    fireEvent.click(createButtons[0]);
    simulateHashChange('#preview/admin/security/ldap/create');

    await waitFor(() => {
      expect(screen.getByTestId('ldap-form')).toBeInTheDocument();
    });

    // Click save to create the server - the mock LdapForm's onSave triggers handleBack navigation
    // Note: The actual createServer API call happens in useLdapForm (inside LdapForm), not in LdapPage
    fireEvent.click(screen.getByText('Save'));

    // Simulate the hashchange event that the browser would fire when handleBack sets the hash
    simulateHashChange('#preview/admin/security/ldap');

    // Wait for list refresh triggered by hashchange
    await waitFor(() => {
      // fetchServers should be called again to refresh the list
      expect(mockFetchServers).toHaveBeenCalledTimes(2);
    });

    // Should return to list view
    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });
  });

  it('refreshes the list after updating a server', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByText('LDAP Server 1')).toBeInTheDocument();
    });

    // Initial load should have called fetchServers once
    expect(mockFetchServers).toHaveBeenCalledTimes(1);

    // Select server for editing
    fireEvent.click(screen.getByText('LDAP Server 1'));
    simulateHashChange('#preview/admin/security/ldap/LDAP%20Server%201');

    await waitFor(() => {
      expect(screen.getByTestId('ldap-form')).toBeInTheDocument();
    });

    // Edit mode triggers an additional fetchServers call to load fresh server data
    const callsBeforeSave = mockFetchServers.mock.calls.length;

    // Click save to update the server
    fireEvent.click(screen.getByText('Save'));

    simulateHashChange('#preview/admin/security/ldap');

    // Save triggers refreshKey increment which causes loadServers to re-fetch
    await waitFor(() => {
      expect(mockFetchServers.mock.calls.length).toBeGreaterThan(callsBeforeSave);
    });

    // Should return to list view
    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });
  });

  it('maintains list view when navigating to same LDAP route via browser', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });

    // Initial load should have called fetchServers once
    expect(mockFetchServers).toHaveBeenCalledTimes(1);

    // Simulate hash change to same route (e.g., clicking sidebar link while already on page)
    simulateHashChange('#preview/admin/security/ldap');

    // List should remain visible and functional
    expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    expect(screen.getByText('Servers: 2')).toBeInTheDocument();
  });

  it('calls deleteServer and shows success toast when deleting a server', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByText('LDAP Server 1')).toBeInTheDocument();
    });

    // Click delete button for the first server
    fireEvent.click(screen.getByTestId('delete-server1'));

    // Wait for delete to complete and toast to show
    await waitFor(() => {
      expect(mockDeleteServer).toHaveBeenCalledWith('LDAP Server 1');
    });

    // Success toast should be shown
    await waitFor(() => {
      expect(screen.getByText('LDAP server "LDAP Server 1" deleted successfully')).toBeInTheDocument();
    });

    // List should be refreshed
    await waitFor(() => {
      expect(mockFetchServers).toHaveBeenCalledTimes(2);
    });
  });
});


