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

import { RealmsPage } from '../RealmsPage';
import * as useRealmsApiModule from '../useRealmsApi';
import * as useRealmsFormModule from '../useRealmsForm';
import { ToastProvider } from '../../../../../shared/Toast';
import { Realm } from '../types';

// Mock the API hook and form hook
jest.mock('../useRealmsApi');
jest.mock('../useRealmsForm', () => ({
  useRealmsForm: jest.fn(),
}));

const mockedUseRealmsApi = useRealmsApiModule.useRealmsApi as jest.MockedFunction<typeof useRealmsApiModule.useRealmsApi>;
const mockedUseRealmsForm = useRealmsFormModule.useRealmsForm as jest.MockedFunction<typeof useRealmsFormModule.useRealmsForm>;

function createRealmsFormMock(availableRealms: Realm[], activeRealms: Realm[], overrides: Record<string, any> = {}) {
  const activeIds = new Set(activeRealms.map(r => r.id));
  const inactiveRealms = availableRealms.filter(r => !activeIds.has(r.id));
  return {
    availableRealms,
    inactiveRealms,
    activeRealms,
    isPristine: true,
    isLoading: false,
    isSaving: false,
    hasLoadError: false,
    validationError: null as string | null,
    saveError: null as string | null,
    loadError: null as string | null,
    addRealm: jest.fn(),
    removeRealm: jest.fn(),
    reorder: jest.fn(),
    moveUp: jest.fn(),
    moveDown: jest.fn(),
    submit: jest.fn(),
    discard: jest.fn(),
    retry: jest.fn(),
    ...overrides,
  } as any;
}

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

// Wrapper component for Radix Theme and Toast context
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

describe('RealmsPage', () => {
  const mockAvailableRealms = [
    { id: 'NexusAuthenticatingRealm', name: 'Local Authenticating Realm' },
    { id: 'NexusAuthorizingRealm', name: 'Local Authorizing Realm' },
    { id: 'LdapRealm', name: 'LDAP Realm' },
    { id: 'SamlRealm', name: 'SAML Realm' },
    { id: 'DockerToken', name: 'Docker Bearer Token Realm' },
  ];

  const mockActiveRealmIds = ['NexusAuthenticatingRealm', 'NexusAuthorizingRealm'];

  const mockFetchAvailableRealms = jest.fn();
  const mockFetchActiveRealmIds = jest.fn();
  const mockUpdateActiveRealms = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    getMockCheckPermission().mockReturnValue(true);
    mockedUseRealmsApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchAvailableRealms: mockFetchAvailableRealms.mockResolvedValue(mockAvailableRealms),
      fetchActiveRealmIds: mockFetchActiveRealmIds.mockResolvedValue(mockActiveRealmIds),
      updateActiveRealms: mockUpdateActiveRealms.mockResolvedValue(undefined),
    });
    // Default form hook mock with loaded realms
    const activeRealms = mockActiveRealmIds
      .map(id => mockAvailableRealms.find(r => r.id === id))
      .filter((r): r is Realm => r !== undefined);
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock(mockAvailableRealms, activeRealms));
  });

  it('renders loading state initially', () => {
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock([], [], { isLoading: true }));
    render(<RealmsPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading realm configuration...')).toBeInTheDocument();
  });

  it('renders the page header', async () => {
    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getAllByText('Realms').length).toBeGreaterThanOrEqual(1);
    });

    expect(screen.getByText('Configure the active security realms and their order')).toBeInTheDocument();
  });

  it('displays available and active realms lists', async () => {
    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Local Authenticating Realm')).toBeInTheDocument();
    });

    // Active realms
    expect(screen.getByText('Local Authorizing Realm')).toBeInTheDocument();

    // Available (inactive) realms
    expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    expect(screen.getByText('SAML Realm')).toBeInTheDocument();
    expect(screen.getByText('Docker Bearer Token Realm')).toBeInTheDocument();
  });

  it('shows panel headers with counts', async () => {
    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Available')).toBeInTheDocument();
    });

    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByText('3 realms')).toBeInTheDocument(); // Inactive count
    expect(screen.getByText('2 realms')).toBeInTheDocument(); // Active count
  });

  it('allows selecting available realms', async () => {
    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    });

    const ldapItem = screen.getByText('LDAP Realm').closest('[role="option"]');
    fireEvent.click(ldapItem!);

    expect(ldapItem).toHaveAttribute('aria-selected', 'true');
  });

  it('calls addRealm on double click of available realm', async () => {
    const mockAddRealm = jest.fn();
    const activeRealms = mockActiveRealmIds
      .map(id => mockAvailableRealms.find(r => r.id === id))
      .filter((r): r is Realm => r !== undefined);
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock(mockAvailableRealms, activeRealms, { addRealm: mockAddRealm }));

    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    });

    const ldapItem = screen.getByText('LDAP Realm').closest('[role="option"]');
    fireEvent.doubleClick(ldapItem!);

    expect(mockAddRealm).toHaveBeenCalledWith({ id: 'LdapRealm', name: 'LDAP Realm' });
  });

  it('enables save button when form is dirty', async () => {
    const activeRealms = mockActiveRealmIds
      .map(id => mockAvailableRealms.find(r => r.id === id))
      .filter((r): r is Realm => r !== undefined);
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock(mockAvailableRealms, activeRealms, { isPristine: false }));

    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: 'Save' });
    expect(saveButton).not.toBeDisabled();
  });

  it('calls submit when Save button is clicked', async () => {
    const mockSubmit = jest.fn();
    const activeRealms = mockActiveRealmIds
      .map(id => mockAvailableRealms.find(r => r.id === id))
      .filter((r): r is Realm => r !== undefined);
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock(mockAvailableRealms, activeRealms, { isPristine: false, submit: mockSubmit }));

    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: 'Save' });
    fireEvent.click(saveButton);

    expect(mockSubmit).toHaveBeenCalled();
  });

  it('discards changes when Discard button is clicked', async () => {
    const activeRealms = mockActiveRealmIds
      .map(id => mockAvailableRealms.find(r => r.id === id))
      .filter((r): r is Realm => r !== undefined);
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock(mockAvailableRealms, activeRealms, { isPristine: false }));
    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    });

    // Make a change
    const ldapItem = screen.getByText('LDAP Realm').closest('[role="option"]');
    fireEvent.doubleClick(ldapItem!);

    // Click discard
    const discardButton = screen.getByRole('button', { name: 'Discard' });
    fireEvent.click(discardButton);

    // SettingsForm has confirmDiscard=true by default, so click "Leave" in confirmation dialog
    const leaveButton = await screen.findByRole('button', { name: /leave/i });
    fireEvent.click(leaveButton);

    // LDAP should be back in available list
    await waitFor(() => {
      const availableList = screen.getByLabelText('Available realms');
      expect(availableList).toContainElement(screen.getByText('LDAP Realm'));
    });
  });

  it('filters available realms when searching', async () => {
    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    });

    const searchInputs = screen.getAllByPlaceholderText('Filter...');
    const availableSearchInput = searchInputs[0]; // First one is for available

    fireEvent.change(availableSearchInput, { target: { value: 'LDAP' } });

    // Only LDAP should be visible
    expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    expect(screen.queryByText('SAML Realm')).not.toBeInTheDocument();
    expect(screen.queryByText('Docker Bearer Token Realm')).not.toBeInTheDocument();
  });

  it('displays error state', async () => {
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock([], [], { loadError: 'Failed to load realms', hasLoadError: true }));

    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load realms')).toBeInTheDocument();
    });
  });

  it('calls submit when Save is clicked with dirty form', async () => {
    const mockSubmit = jest.fn();
    const activeRealms = mockActiveRealmIds
      .map(id => mockAvailableRealms.find(r => r.id === id))
      .filter((r): r is Realm => r !== undefined);
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock(mockAvailableRealms, activeRealms, { isPristine: false, submit: mockSubmit }));

    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: 'Save' });
    fireEvent.click(saveButton);

    expect(mockSubmit).toHaveBeenCalled();
  });

  it('displays help section with documentation link', async () => {
    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('About Realms')).toBeInTheDocument();
    });

    expect(screen.getByText('View Documentation')).toHaveAttribute('href', 'https://help.sonatype.com/en/realms.html');
  });

  it('shows read-only view when user lacks update permission', async () => {
    const { ExtJS } = require('@sonatype/nexus-ui-plugin');
    ExtJS.checkPermission.mockReturnValue(false);
    (global as any).NX.Permissions.check.mockReturnValue(false);

    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getAllByText('Realms').length).toBeGreaterThanOrEqual(1);
    });

    // Save button should not be present
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
  });

  it('calls reorder when transfer button is clicked', async () => {
    const mockReorder = jest.fn();
    const activeRealms = mockActiveRealmIds
      .map(id => mockAvailableRealms.find(r => r.id === id))
      .filter((r): r is Realm => r !== undefined);
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock(mockAvailableRealms, activeRealms, { reorder: mockReorder }));

    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    });

    // Select LDAP realm
    const ldapItem = screen.getByText('LDAP Realm').closest('[role="option"]');
    fireEvent.click(ldapItem!);

    // Click move to active button
    const addSelectedButton = screen.getByLabelText('Add selected realms');
    fireEvent.click(addSelectedButton);

    // reorder is called with the combined active + selected realms
    expect(mockReorder).toHaveBeenCalled();
  });

  it('calls moveUp when up button is clicked', async () => {
    const mockMoveUp = jest.fn();
    const activeRealms = mockActiveRealmIds
      .map(id => mockAvailableRealms.find(r => r.id === id))
      .filter((r): r is Realm => r !== undefined);
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock(mockAvailableRealms, activeRealms, { moveUp: mockMoveUp }));

    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Local Authenticating Realm')).toBeInTheDocument();
    });

    // Select second active realm
    const authorizingItem = screen.getByText('Local Authorizing Realm').closest('[role="option"]');
    fireEvent.click(authorizingItem!);

    // Click move up
    const moveUpButton = screen.getByLabelText('Move up');
    fireEvent.click(moveUpButton);

    expect(mockMoveUp).toHaveBeenCalledWith('NexusAuthorizingRealm');
  });

  it('calls reorder with all realms when Add All is clicked', async () => {
    const mockReorder = jest.fn();
    const activeRealms = mockActiveRealmIds
      .map(id => mockAvailableRealms.find(r => r.id === id))
      .filter((r): r is Realm => r !== undefined);
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock(mockAvailableRealms, activeRealms, { reorder: mockReorder }));

    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    });

    const addAllButton = screen.getByLabelText('Add all realms');
    fireEvent.click(addAllButton);

    // reorder is called with all available realms
    expect(mockReorder).toHaveBeenCalledWith(mockAvailableRealms);
  });

  it('allows multi-select with ctrl/meta key', async () => {
    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    });

    // Select LDAP realm
    const ldapItem = screen.getByText('LDAP Realm').closest('[role="option"]');
    fireEvent.click(ldapItem!);

    // Ctrl-click SAML realm
    const samlItem = screen.getByText('SAML Realm').closest('[role="option"]');
    fireEvent.click(samlItem!, { ctrlKey: true });

    // Both should be selected
    expect(ldapItem).toHaveAttribute('aria-selected', 'true');
    expect(samlItem).toHaveAttribute('aria-selected', 'true');
  });

  it('calls moveDown when down button is clicked', async () => {
    const mockMoveDown = jest.fn();
    const activeRealms = mockActiveRealmIds
      .map(id => mockAvailableRealms.find(r => r.id === id))
      .filter((r): r is Realm => r !== undefined);
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock(mockAvailableRealms, activeRealms, { moveDown: mockMoveDown }));

    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Local Authenticating Realm')).toBeInTheDocument();
    });

    // Select first active realm
    const authItem = screen.getByText('Local Authenticating Realm').closest('[role="option"]');
    fireEvent.click(authItem!);

    // Click move down
    const moveDownButton = screen.getByLabelText('Move down');
    fireEvent.click(moveDownButton);

    expect(mockMoveDown).toHaveBeenCalledWith('NexusAuthenticatingRealm');
  });

  it('calls addRealm on keyboard Enter in available realm', async () => {
    const mockAddRealm = jest.fn();
    const activeRealms = mockActiveRealmIds
      .map(id => mockAvailableRealms.find(r => r.id === id))
      .filter((r): r is Realm => r !== undefined);
    mockedUseRealmsForm.mockReturnValue(createRealmsFormMock(mockAvailableRealms, activeRealms, { addRealm: mockAddRealm }));

    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('LDAP Realm')).toBeInTheDocument();
    });

    const ldapItem = screen.getByText('LDAP Realm').closest('[role="option"]');
    fireEvent.keyDown(ldapItem!, { key: 'Enter' });

    expect(mockAddRealm).toHaveBeenCalledWith({ id: 'LdapRealm', name: 'LDAP Realm' });
  });

  it('filters active realms when searching', async () => {
    render(<RealmsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Local Authenticating Realm')).toBeInTheDocument();
    });

    const searchInputs = screen.getAllByPlaceholderText('Filter...');
    const activeSearchInput = searchInputs[1]; // Second one is for active

    fireEvent.change(activeSearchInput, { target: { value: 'Authorizing' } });

    // Only Authorizing realm should be visible in active list
    const activeList = screen.getByLabelText('Active realms');
    expect(activeList).toContainElement(screen.getByText('Local Authorizing Realm'));
    expect(activeList).not.toContainElement(screen.queryByText('Local Authenticating Realm'));
  });
});


