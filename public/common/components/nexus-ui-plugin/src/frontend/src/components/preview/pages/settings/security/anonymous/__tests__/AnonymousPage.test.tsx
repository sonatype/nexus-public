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

import { AnonymousPage } from '../AnonymousPage';
import * as useAnonymousApiModule from '../useAnonymousApi';
import * as useAnonymousFormModule from '../useAnonymousForm';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock the API hook and form hook
jest.mock('../useAnonymousApi');
jest.mock('../useAnonymousForm');

const mockedUseAnonymousApi = useAnonymousApiModule.useAnonymousApi as jest.MockedFunction<typeof useAnonymousApiModule.useAnonymousApi>;
const mockedUseAnonymousForm = useAnonymousFormModule.useAnonymousForm as jest.MockedFunction<typeof useAnonymousFormModule.useAnonymousForm>;

function createAnonymousFormMock(formData: Record<string, any>, realmTypes: Array<{id: string; name: string}>, overrides: Record<string, any> = {}) {
  return {
    formData,
    errors: {} as Record<string, string>,
    touched: {} as Record<string, boolean>,
    isPristine: true,
    isSaving: false,
    isLoading: false,
    hasValidationErrors: false,
    loadError: null as string | null,
    realmTypes,
    field: jest.fn((name: string) => ({
      name,
      value: formData[name] != null ? String(formData[name]) : '',
      error: undefined,
      onChange: jest.fn(),
      onBlur: jest.fn(),
    })),
    checkbox: jest.fn((name: string) => ({
      name,
      checked: Boolean(formData[name]),
      error: undefined,
      onChange: jest.fn(),
    })),
    handleChange: jest.fn(),
    handleBlur: jest.fn(),
    handleSubmit: jest.fn(),
    handleDiscard: jest.fn(),
    handleRetry: jest.fn(),
    showDisableConfirm: false,
    handleConfirmDisable: jest.fn(),
    handleCancelDisable: jest.fn(),
    cancelDialogOpen: false,
    handleCancelConfirm: jest.fn(),
    handleStay: jest.fn(),
    ...overrides,
  } as any;
}

// Create controllable mock for checkPermission
const mockCheckPermission = jest.fn().mockReturnValue(true);

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

describe('AnonymousPage', () => {
  const mockRealmTypes = [
    { id: 'NexusAuthorizingRealm', name: 'Local Authorizing Realm' },
    { id: 'LdapRealm', name: 'LDAP Realm' },
  ];

  const mockSettings = {
    enabled: true,
    userId: 'anonymous',
    realmName: 'NexusAuthorizingRealm',
  };

  const mockFetchRealmTypes = jest.fn();
  const mockFetchSettings = jest.fn();
  const mockSaveSettings = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // Restore checkPermission to return true after clearAllMocks
    getMockCheckPermission().mockReturnValue(true);
    mockedUseAnonymousApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchRealmTypes: mockFetchRealmTypes.mockResolvedValue(mockRealmTypes),
      fetchSettings: mockFetchSettings.mockResolvedValue(mockSettings),
      saveSettings: mockSaveSettings.mockResolvedValue(mockSettings),
    });
    // Default form hook mock with loaded settings
    mockedUseAnonymousForm.mockReturnValue(createAnonymousFormMock(mockSettings, mockRealmTypes));
  });

  it('renders loading state initially', () => {
    mockedUseAnonymousForm.mockReturnValue(createAnonymousFormMock(mockSettings, mockRealmTypes, { isLoading: true }));
    render(<AnonymousPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading anonymous access settings...')).toBeInTheDocument();
  });

  it('renders the page header', async () => {
    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Anonymous Access' })).toBeInTheDocument();
    });

    expect(screen.getByText('Configure anonymous user access settings')).toBeInTheDocument();
  });

  it('displays current settings', async () => {
    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('checkbox-enabled')).toBeInTheDocument();
    });

    expect(screen.getByTestId('checkbox-enabled')).toBeChecked();
    expect(screen.getByTestId('input-userId')).toHaveValue('anonymous');
  });

  it('shows realm dropdown', async () => {
    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('select-realmName')).toBeInTheDocument();
    });

    // SettingsSelect uses Radix UI Select - verify the trigger is rendered
    const realmSelect = screen.getByTestId('select-realmName');
    expect(realmSelect).toBeInTheDocument();
    // Verify the label is rendered
    expect(screen.getByText('Realm')).toBeInTheDocument();
  });

  it('enables save button when changes are made', async () => {
    // Mock the form as dirty (isPristine: false)
    mockedUseAnonymousForm.mockReturnValue(createAnonymousFormMock(mockSettings, mockRealmTypes, { isPristine: false }));
    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-userId')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: 'Save' });
    expect(saveButton).not.toBeDisabled();
  });

  it('saves changes when Save button is clicked', async () => {
    const mockHandleSubmit = jest.fn();
    mockedUseAnonymousForm.mockReturnValue(createAnonymousFormMock(
      { ...mockSettings, userId: 'guest' }, mockRealmTypes, { isPristine: false, handleSubmit: mockHandleSubmit }
    ));
    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-userId')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: 'Save' });
    fireEvent.click(saveButton);

    expect(mockHandleSubmit).toHaveBeenCalled();
  });

  it('discards changes when Discard button is clicked', async () => {
    const handleDiscard = jest.fn();
    mockedUseAnonymousForm.mockReturnValue(createAnonymousFormMock(mockSettings, mockRealmTypes, { isPristine: false, handleDiscard }));
    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-userId')).toBeInTheDocument();
    });

    // Click discard
    const discardButton = screen.getByRole('button', { name: 'Discard' });
    fireEvent.click(discardButton);

    // SettingsForm shows a confirmation dialog by default; click "Leave" to confirm
    const leaveButton = await screen.findByRole('button', { name: /leave/i });
    fireEvent.click(leaveButton);

    expect(handleDiscard).toHaveBeenCalled();
  });

  it('shows validation error when username is empty', async () => {
    // Mock form with validation error on userId field
    mockedUseAnonymousForm.mockReturnValue(createAnonymousFormMock(
      { ...mockSettings, userId: '' }, mockRealmTypes, {
        isPristine: false,
        errors: { userId: 'Username is required' },
        hasValidationErrors: true,
        field: jest.fn((name: string) => ({
          name,
          value: name === 'userId' ? '' : String(mockSettings[name as keyof typeof mockSettings] ?? ''),
          error: name === 'userId' ? 'Username is required' : undefined,
          onChange: jest.fn(),
          onBlur: jest.fn(),
        })),
      }
    ));
    render(<AnonymousPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Username is required')).toBeInTheDocument();
  });

  it('renders anonymous access checkbox as checked', async () => {
    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('checkbox-enabled')).toBeInTheDocument();
    });

    const checkbox = screen.getByTestId('checkbox-enabled');
    expect(checkbox).toBeChecked();
    // Verify checkbox hook was called with 'enabled'
    const formMock = mockedUseAnonymousForm.mock.results[0]?.value;
    expect(formMock?.checkbox).toHaveBeenCalledWith('enabled');
  });

  it('displays error state', async () => {
    // Mock form with loadError
    mockedUseAnonymousForm.mockReturnValue(createAnonymousFormMock(
      mockSettings, mockRealmTypes, { loadError: 'Failed to load settings', hasLoadError: true }
    ));

    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load settings')).toBeInTheDocument();
    });
  });

  it('triggers submit when Save button is clicked', async () => {
    const mockHandleSubmit = jest.fn();
    mockedUseAnonymousForm.mockReturnValue(createAnonymousFormMock(
      { ...mockSettings, userId: 'guest' }, mockRealmTypes, { isPristine: false, handleSubmit: mockHandleSubmit }
    ));
    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-userId')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: 'Save' });
    fireEvent.click(saveButton);

    expect(mockHandleSubmit).toHaveBeenCalled();
  });

  it('displays help section with documentation link', async () => {
    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('About Anonymous Access')).toBeInTheDocument();
    });

    expect(screen.getByText('View Documentation')).toHaveAttribute('href', 'https://help.sonatype.com/en/anonymous-access.html');
  });

  it('shows read-only view when user lacks update permission', async () => {
    getMockCheckPermission().mockReturnValue(false);
    (global as any).NX.Permissions.check.mockReturnValue(false);

    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Anonymous Access' })).toBeInTheDocument();
    });

    // Save button should not be present
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();

    // Should show read-only view
    expect(screen.getByText('Current Settings')).toBeInTheDocument();
  });

  it('changes realm selection', async () => {
    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('select-realmName')).toBeInTheDocument();
    });

    const realmSelect = screen.getByTestId('select-realmName');
    fireEvent.change(realmSelect, { target: { value: 'LdapRealm' } });

    expect(realmSelect).toHaveValue('LdapRealm');
  });

  it('renders userId field with current value', async () => {
    render(<AnonymousPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-userId')).toBeInTheDocument();
    });

    // Verify the userId field shows the current value from the hook
    const usernameInput = screen.getByTestId('input-userId');
    expect(usernameInput).toHaveValue('anonymous');
  });
});
