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

// Mock the formatIcons module which imports @icons-pack/react-simple-icons — a package
// that cannot be resolved in this test environment (missing package.json in node_modules).
// Mocking the intermediate module avoids the unresolvable transitive import.
jest.mock('../../../../../shared/Badges/formatIcons', () => ({}));

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
        <button onClick={() => onReorder(['LDAP Server 2', 'LDAP Server 1']).catch(() => {})} data-testid="reorder-servers">
          Reorder
        </button>
      </div>
    );
  },
}));

jest.mock('../LdapForm', () => ({
  LdapForm: function MockLdapForm({ server, isCreate, existingNames, onSave, onCancel, onDelete }: any) {
    return (
      <div data-testid="ldap-form">
        <span>{isCreate ? 'Create LDAP Server' : 'Edit LDAP Server'}</span>
        <span data-testid="ldap-form-server-name">{server?.name ?? 'NONE'}</span>
        <span data-testid="ldap-form-existing-names">{JSON.stringify(existingNames ?? [])}</span>
        <button data-testid="button-submit" onClick={() => onSave({ name: 'Test Server', host: 'ldap.example.com', port: 389, searchBase: 'dc=example,dc=com', authScheme: 'simple', protocol: 'ldap', userObjectClass: 'inetOrgPerson', userIdAttribute: 'uid', userRealNameAttribute: 'cn', userEmailAddressAttribute: 'mail', ldapGroupsAsRoles: false })}>
          Save
        </button>
        <button data-testid="form-cancel" onClick={onCancel}>Cancel</button>
        {onDelete && <button data-testid="form-delete" onClick={onDelete}>Delete</button>}
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

  it('renders loading state initially', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading LDAP servers...')).toBeInTheDocument();

    // Drain the mount-time loadServers() fetch so its resolution doesn't
    // trigger an "update not wrapped in act" warning during a later test.
    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });
  });

  it('renders the page header', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'LDAP' })).toBeInTheDocument();
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

  it('passes loaded server names as existingNames to LdapForm in create mode (NEXUS-54082)', async () => {
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

    const existingNamesEl = screen.getByTestId('ldap-form-existing-names');
    const passed = JSON.parse(existingNamesEl.textContent ?? '[]');
    expect(passed).toEqual(['LDAP Server 1', 'LDAP Server 2']);
  });

  it('passes other server names as existingNames to LdapForm in edit mode, excluding its own name (NEXUS-54125)', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Server 1')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('LDAP Server 1'));
    simulateHashChange('#preview/admin/security/ldap/LDAP%20Server%201');

    await waitFor(() => {
      expect(screen.getByTestId('ldap-form')).toBeInTheDocument();
    });

    const existingNamesEl = screen.getByTestId('ldap-form-existing-names');
    const passed = JSON.parse(existingNamesEl.textContent ?? '[]');
    expect(passed).toEqual(['LDAP Server 2']);
  });

  it('redirects to the list with an error toast when a user without nexus:ldap:create lands directly on the create URL (NEXUS-53627 review fix)', async () => {
    // The Create button itself is already hidden via canCreate, but a direct
    // URL visit (or browser back/forward) bypasses that and goes straight
    // through syncViewWithHash — which must apply the same guard.
    global.NX.Permissions.check.mockImplementation(
      (permission: string) => permission !== 'nexus:ldap:create'
    );
    (window as any).location.hash = '#preview/admin/security/ldap/create';

    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('You do not have permission to create LDAP servers')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('ldap-form')).not.toBeInTheDocument();
  });

  it('navigates to edit form when server is selected', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Server 1')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('LDAP Server 1'));

    simulateHashChange('#preview/admin/security/ldap/LDAP%20Server%201');

    // Assert synchronously, immediately after the click + hashchange (both of
    // which are wrapped in synchronous `act()` calls above) and BEFORE any
    // `await`/`waitFor`. This is deliberate: LdapPage's edit-mode effect
    // re-fetches the server list asynchronously and calls setSelectedServer
    // once that promise resolves, which would otherwise mask the bug by the
    // time a `waitFor` polling loop lets that microtask flush.
    //
    // NEXUS-53672 regression guard: the selected server must be passed to
    // LdapForm synchronously on mount (i.e. still true at this synchronous
    // checkpoint), not left null until the async refetch resolves — XState's
    // useMachine ignores machine updates after first mount, so a
    // null-then-populated server never reaches the real form.
    expect(screen.getByTestId('ldap-form')).toBeInTheDocument();
    expect(screen.getByTestId('ldap-form-server-name')).toHaveTextContent('LDAP Server 1');

    // Let the edit-mode re-fetch effect's promise settle and its resulting
    // setSelectedServer/setServers state updates flush, so the test doesn't
    // leave a dangling update outside of act(). mockFetchServers having been
    // called is already true synchronously above (it fires on mount too), so
    // polling on that alone would resolve before the re-fetch's *resolution*
    // is flushed — assert on the post-flush call count instead, which can
    // only be reached once that promise has actually resolved.
    await waitFor(() => {
      expect(mockFetchServers).toHaveBeenCalledTimes(2);
    });
    expect(screen.getByTestId('ldap-form-server-name')).toHaveTextContent('LDAP Server 1');
  });

  it('passes onDelete to LdapForm when the user has nexus:ldap:delete permission', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Server 1')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('LDAP Server 1'));
    simulateHashChange('#preview/admin/security/ldap/LDAP%20Server%201');

    expect(screen.getByTestId('form-delete')).toBeInTheDocument();
  });

  it('does not pass onDelete to LdapForm when the user lacks nexus:ldap:delete permission (NEXUS-53627)', async () => {
    global.NX.Permissions.check.mockImplementation(
      (permission: string) => permission !== 'nexus:ldap:delete'
    );

    render(<LdapPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Server 1')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('LDAP Server 1'));
    simulateHashChange('#preview/admin/security/ldap/LDAP%20Server%201');

    expect(screen.getByTestId('ldap-form')).toBeInTheDocument();
    expect(screen.queryByTestId('form-delete')).not.toBeInTheDocument();
  });

  it('does not render LdapForm until the server is loaded when landing directly on an edit URL', async () => {
    // Simulate a full page load / refresh where the browser is already on the
    // edit URL, as opposed to navigating there via a click in the list (which
    // is handled by handleEdit setting selectedServer synchronously before
    // the hash changes). In this path, `viewMode` flips to 'edit' via the
    // mount-time syncViewWithHash effect while `selectedServer` is still
    // null; it's only populated later by an async re-fetch effect.
    (window as any).location.hash = '#preview/admin/security/ldap/LDAP%20Server%201';

    render(<LdapPage />, { wrapper: TestWrapper });

    // NEXUS-53672 regression guard: LdapForm must not mount with a null
    // server. XState's useMachine only binds to the machine on the first
    // render and ignores later machine changes, so a null-then-populated
    // server never reaches the real form — it must not render at all until
    // selectedServer is resolved (show a loading state instead).
    //
    // Assert the loading state is actually shown, not just that the form is
    // absent — a future refactor that rendered nothing at all instead of the
    // spinner would still pass a form-absence-only check.
    expect(screen.queryByTestId('ldap-form')).not.toBeInTheDocument();
    expect(screen.getByText('Loading LDAP server...')).toBeInTheDocument();

    // Once the async re-fetch resolves, the form must mount already
    // populated with the real server.
    await waitFor(() => {
      expect(screen.getByTestId('ldap-form-server-name')).toHaveTextContent('LDAP Server 1');
    });
  });

  it('navigates back to list with a toast when the edit-mode re-fetch fails on a direct edit URL load', async () => {
    // Simulate landing directly on an edit URL (e.g. page refresh), same setup
    // as the "does not render LdapForm..." test above, but this time every
    // fetchServers() call rejects instead of succeeding. Using
    // mockRejectedValue (not "Once") deliberately: the initial mount fires
    // both the list-loading fetch (loadServers effect) and the edit-mode
    // re-fetch, in that order, so an "Once" rejection would only fail the
    // former and let the latter succeed with real data, masking the bug this
    // test targets.
    mockFetchServers.mockRejectedValue(new Error('network error'));
    (window as any).location.hash = '#preview/admin/security/ldap/LDAP%20Server%201';

    render(<LdapPage />, { wrapper: TestWrapper });

    // Loading state is shown synchronously while the re-fetch is in flight.
    expect(screen.getByText('Loading LDAP server...')).toBeInTheDocument();

    // NEXUS-53672 catch-path regression guard: without the .catch() handler,
    // a rejected re-fetch here would leave selectedServer unresolved forever
    // and the loading state would spin indefinitely. Instead it must surface
    // an error toast and return to the list view.
    //
    // The redirect drives `viewMode` directly (via redirectToListWithError)
    // rather than only assigning window.location.hash and waiting for a
    // 'hashchange' event to flip it — so the list view is expected to
    // reappear without this test needing to simulate that event itself.
    await waitFor(() => {
      expect(screen.getByText('Failed to load LDAP server "LDAP Server 1"')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });
  });

  it('does not show a spurious error toast when navigating back to the list while the edit-mode re-fetch is still in flight', async () => {
    // Hold the edit-mode re-fetch open with a promise we control, so we can
    // navigate away (back to the list) before it settles. Reject it (rather
    // than resolving with a server list) so that, absent the effect's
    // cancellation guard, it would definitely take the redirectToListWithError
    // path (toast.error + redirect) rather than possibly matching a real
    // server and succeeding silently.
    let rejectFetch!: (error: Error) => void;
    const pendingFetch = new Promise<typeof mockServers>((_resolve, reject) => {
      rejectFetch = reject;
    });
    mockFetchServers.mockResolvedValueOnce(mockServers); // initial list-load fetch
    mockFetchServers.mockImplementationOnce(() => pendingFetch); // edit-mode re-fetch
    (window as any).location.hash = '#preview/admin/security/ldap/LDAP%20Server%201';

    render(<LdapPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading LDAP server...')).toBeInTheDocument();

    // Navigate back to the list while the edit-mode re-fetch is still pending.
    simulateHashChange('#preview/admin/security/ldap');

    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });

    // Now let the stale re-fetch reject. Without the effect's cancellation
    // guard, this would call redirectToListWithError, firing a spurious error
    // toast and a redundant redirect to a list the user is already on.
    await act(async () => {
      rejectFetch(new Error('network error'));
      await pendingFetch.catch(() => {});
    });

    expect(screen.queryByText(/Failed to load LDAP server/)).not.toBeInTheDocument();
    expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
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

  it('refreshes the list after a successful reorder', async () => {
    render(<LdapPage />, { wrapper: TestWrapper });

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });

    // Initial load should have called fetchServers once
    expect(mockFetchServers).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByTestId('reorder-servers'));

    await waitFor(() => {
      expect(mockChangeOrder).toHaveBeenCalledWith(['LDAP Server 2', 'LDAP Server 1']);
    });

    // Success toast should be shown
    await waitFor(() => {
      expect(screen.getByText('Server order updated')).toBeInTheDocument();
    });

    // List should be refreshed so the stale `order` values get updated
    await waitFor(() => {
      expect(mockFetchServers).toHaveBeenCalledTimes(2);
    });
  });

  it('does not refresh the list or show a success toast when reorder fails', async () => {
    mockChangeOrder.mockRejectedValueOnce(new Error('Reorder failed'));

    render(<LdapPage />, { wrapper: TestWrapper });

    // Wait for initial load
    await waitFor(() => {
      expect(screen.getByTestId('ldap-list')).toBeInTheDocument();
    });

    // Initial load should have called fetchServers once
    expect(mockFetchServers).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByTestId('reorder-servers'));

    await waitFor(() => {
      expect(mockChangeOrder).toHaveBeenCalledWith(['LDAP Server 2', 'LDAP Server 1']);
    });

    // No success toast on failure
    expect(screen.queryByText('Server order updated')).not.toBeInTheDocument();

    // List should not be re-fetched since the reorder didn't succeed
    expect(mockFetchServers).toHaveBeenCalledTimes(1);
  });
});
