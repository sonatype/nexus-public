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

import { CrowdPage } from '../CrowdPage';
import * as useCrowdApiModule from '../useCrowdApi';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock the API hook
jest.mock('../useCrowdApi');

const mockedUseCrowdApi = useCrowdApiModule.useCrowdApi as jest.MockedFunction<
  typeof useCrowdApiModule.useCrowdApi
>;

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

function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

const mockSettings = {
  enabled: false,
  realmActive: false,
  applicationName: '',
  applicationPassword: '',
  serverUrl: '',
  timeout: 30,
  useTrustStoreForUrl: false,
};

describe('CrowdPage', () => {
  const mockFetchConfig = jest.fn();
  const mockSaveConfig = jest.fn();
  const mockVerifyConnection = jest.fn();
  const mockClearCache = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // Restore checkPermission to return true after clearAllMocks
    getMockCheckPermission().mockReturnValue(true);
    mockedUseCrowdApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchConfig: mockFetchConfig.mockResolvedValue(mockSettings),
      saveConfig: mockSaveConfig.mockResolvedValue({}),
      verifyConnection: mockVerifyConnection.mockResolvedValue({ success: true }),
      clearCache: mockClearCache.mockResolvedValue({}),
    });
  });

  it('renders the page with correct data-testid', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('crowd-page')).toBeInTheDocument();
    });
  });

  it('renders the form with correct data-testid and state attributes', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      const form = screen.getByTestId('crowd-form');
      expect(form).toBeInTheDocument();
      // SettingsForm provides: data-loading, data-dirty, data-pristine, data-submit-disabled
      expect(form).toHaveAttribute('data-loading');
      expect(form).toHaveAttribute('data-dirty');
      expect(form).toHaveAttribute('data-pristine');
      expect(form).toHaveAttribute('data-submit-disabled');
    });
  });

  it('renders the page header', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Atlassian Crowd')).toBeInTheDocument();
    });
  });

  it('renders the page description', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText(/Manage Atlassian Crowd configuration/i)).toBeInTheDocument();
    });
  });

  it('loads settings on mount', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(mockFetchConfig).toHaveBeenCalled();
    });
  });

  it('displays enabled toggle', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('checkbox-enabled')).toBeInTheDocument();
    });
  });

  it('displays realm active toggle', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('checkbox-realmActive')).toBeInTheDocument();
    });
  });

  it('displays application name field', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });
  });

  it('displays application password field', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('password-applicationPassword')).toBeInTheDocument();
    });
  });

  it('displays server URL field', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-url')).toBeInTheDocument();
    });
  });

  it('displays timeout field', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-timeout')).toBeInTheDocument();
    });
  });

  it('displays use trust store toggle', async () => {
    // Trust store toggle only shows when URL starts with https
    mockedUseCrowdApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchConfig: mockFetchConfig.mockResolvedValue({
        ...mockSettings,
        url: 'https://crowd.example.com',
      }),
      saveConfig: mockSaveConfig.mockResolvedValue({}),
      verifyConnection: mockVerifyConnection.mockResolvedValue({ success: true }),
      clearCache: mockClearCache.mockResolvedValue({}),
    });

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('checkbox-useTrustStoreForUrl')).toBeInTheDocument();
    });
  });

  it('displays save button', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /save/i })).toBeInTheDocument();
    });
  });

  it('displays discard button', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /discard/i })).toBeInTheDocument();
    });
  });

  it('displays verify connection button', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /verify.*connection/i })).toBeInTheDocument();
    });
  });

  it('displays clear cache button', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /clear.*cache/i })).toBeInTheDocument();
    });
  });

  it('saves settings when Save button is clicked', async () => {
    mockedUseCrowdApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchConfig: mockFetchConfig.mockResolvedValue({
        ...mockSettings,
        enabled: true,
        applicationName: 'nexus',
        applicationPassword: 'secret',
        url: 'https://crowd.example.com',
      }),
      saveConfig: mockSaveConfig.mockResolvedValue({}),
      verifyConnection: mockVerifyConnection,
      clearCache: mockClearCache,
    });

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    // Make a change to enable the Save button (form needs to be dirty)
    const appNameInput = screen.getByTestId('input-applicationName');
    fireEvent.change(appNameInput, { target: { value: 'nexus-modified' } });

    const saveButton = screen.getByRole('button', { name: /save/i });
    await waitFor(() => {
      expect(saveButton).not.toBeDisabled();
    });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockSaveConfig).toHaveBeenCalled();
    });
  });

  it('verifies connection when Verify Connection button is clicked', async () => {
    // Provide complete valid settings to avoid validation errors disabling the button
    mockedUseCrowdApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchConfig: mockFetchConfig.mockResolvedValue({
        enabled: true,
        realmActive: false,
        applicationName: 'nexus',
        applicationPassword: 'secret',
        url: 'https://crowd.example.com',
        timeout: 30,
        useTrustStoreForUrl: false,
      }),
      saveConfig: mockSaveConfig.mockResolvedValue({}),
      verifyConnection: mockVerifyConnection.mockResolvedValue({ success: true }),
      clearCache: mockClearCache.mockResolvedValue({}),
    });

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    const verifyButton = screen.getByRole('button', { name: /verify.*connection/i });
    fireEvent.click(verifyButton);

    await waitFor(() => {
      expect(mockVerifyConnection).toHaveBeenCalled();
    });
  });

  it('shows success message after successful connection verification', async () => {
    mockedUseCrowdApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchConfig: mockFetchConfig.mockResolvedValue({
        enabled: true,
        realmActive: false,
        applicationName: 'nexus',
        applicationPassword: 'secret',
        url: 'https://crowd.example.com',
        timeout: 30,
        useTrustStoreForUrl: false,
      }),
      saveConfig: mockSaveConfig.mockResolvedValue({}),
      verifyConnection: mockVerifyConnection.mockResolvedValue({ success: true }),
      clearCache: mockClearCache.mockResolvedValue({}),
    });

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    const verifyButton = screen.getByRole('button', { name: /verify.*connection/i });
    fireEvent.click(verifyButton);

    await waitFor(() => {
      expect(screen.getByText(/Connection to Crowd server verified/i)).toBeInTheDocument();
    });
  });

  it('handles failed connection verification', async () => {
    const mockVerifyRejected = jest.fn().mockRejectedValue(new Error('Connection refused'));
    mockedUseCrowdApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchConfig: mockFetchConfig.mockResolvedValue({
        enabled: true,
        realmActive: false,
        applicationName: 'nexus',
        applicationPassword: 'secret',
        url: 'https://crowd.example.com',
        timeout: 30,
        useTrustStoreForUrl: false,
      }),
      saveConfig: mockSaveConfig.mockResolvedValue({}),
      verifyConnection: mockVerifyRejected,
      clearCache: mockClearCache.mockResolvedValue({}),
    });

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    const verifyButton = screen.getByRole('button', { name: /verify.*connection/i });
    fireEvent.click(verifyButton);

    // Verify that the connection verification was attempted
    await waitFor(() => {
      expect(mockVerifyRejected).toHaveBeenCalled();
    });
  });

  it('clears cache when Clear Cache button is clicked', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    const clearCacheButton = screen.getByRole('button', { name: /clear.*cache/i });
    fireEvent.click(clearCacheButton);

    await waitFor(() => {
      expect(mockClearCache).toHaveBeenCalled();
    });
  });

  it('shows success message after clearing cache', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    const clearCacheButton = screen.getByRole('button', { name: /clear.*cache/i });
    fireEvent.click(clearCacheButton);

    await waitFor(() => {
      expect(screen.getByText(/cache.*cleared/i)).toBeInTheDocument();
    });
  });

  it('resets form when Discard button is clicked', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    // Modify a field
    const appNameInput = screen.getByTestId('input-applicationName');
    fireEvent.change(appNameInput, { target: { value: 'modified-app' } });

    expect(appNameInput).toHaveValue('modified-app');

    // Click discard
    const discardButton = screen.getByTestId('form-cancel');
    fireEvent.click(discardButton);

    // Confirmation dialog should appear because confirmDiscard is true by default in SettingsForm
    await waitFor(() => {
      expect(screen.getByText('Unsaved Changes')).toBeInTheDocument();
    });

    const leaveButton = screen.getByRole('button', { name: /leave/i });
    fireEvent.click(leaveButton);

    await waitFor(() => {
      expect(appNameInput).toHaveValue('');
    });
  });

  it('handles loading state', () => {
    // Component shows loading state during initial data fetch
    // The loading message appears while fetchConfig is pending
    render(<CrowdPage />, { wrapper: TestWrapper });

    // Initially shows loading state before data loads
    expect(screen.getByText(/Loading Crowd configuration/i)).toBeInTheDocument();
  });

  it('handles error state', async () => {
    mockedUseCrowdApi.mockReturnValue({
      loading: false,
      error: 'Failed to load Crowd settings',
      setError: mockSetError,
      fetchConfig: mockFetchConfig,
      saveConfig: mockSaveConfig,
      verifyConnection: mockVerifyConnection,
      clearCache: mockClearCache,
    });

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load Crowd settings')).toBeInTheDocument();
    });
  });

  it('enables save button when changes are made', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    // Initially the save button is disabled (form is pristine)
    const saveButton = screen.getByRole('button', { name: /save/i });
    expect(saveButton).toBeDisabled();

    // Enable Crowd - this makes the form dirty
    const enabledToggle = screen.getByTestId('checkbox-enabled');
    fireEvent.click(enabledToggle);

    // Save button should now be enabled (form is dirty)
    await waitFor(() => {
      expect(saveButton).not.toBeDisabled();
    });
  });

  it('shows read-only view when user lacks update permission', async () => {
    // Mock no update permission
    getMockCheckPermission().mockReturnValue(false);
    (global as any).NX.Permissions.check.mockReturnValue(false);

    render(<CrowdPage />, { wrapper: TestWrapper });

    // Wait for loading to complete
    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    // Form fields should be disabled
    const appNameInput = screen.getByTestId('input-applicationName');
    expect(appNameInput).toBeDisabled();

    // Action buttons should not be present
    expect(screen.queryByRole('button', { name: /save/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /discard/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /verify.*connection/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /clear.*cache/i })).not.toBeInTheDocument();
  });

  it('shows permission warning when user lacks update permission', async () => {
    // Mock no update permission
    getMockCheckPermission().mockReturnValue(false);
    (global as any).NX.Permissions.check.mockReturnValue(false);

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText(/don't have permission to edit/i)).toBeInTheDocument();
    });
  });

  it('renders help section with documentation link', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('About Atlassian Crowd')).toBeInTheDocument();
    });

    // Check for documentation link
    const docLink = screen.getByRole('link', { name: /view crowd documentation/i });
    expect(docLink).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/docs/crowd');
  });

  it('has data-mode attribute set to edit', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      const form = screen.getByTestId('crowd-form');
      expect(form).toHaveAttribute('data-mode', 'edit');
    });
  });

  it('shows form state as dirty when changes are made', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('crowd-form')).toBeInTheDocument();
    });

    const form = screen.getByTestId('crowd-form');
    
    // Initially form should not be dirty
    expect(form).toHaveAttribute('data-dirty', 'false');

    // Make a change
    const enabledToggle = screen.getByTestId('checkbox-enabled');
    fireEvent.click(enabledToggle);

    // Form should now be dirty
    await waitFor(() => {
      expect(form).toHaveAttribute('data-dirty', 'true');
    });
  });

  it('validates URL format', async () => {
    mockedUseCrowdApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchConfig: mockFetchConfig.mockResolvedValue({
        ...mockSettings,
        applicationName: 'nexus',
        applicationPassword: 'secret',
      }),
      saveConfig: mockSaveConfig.mockResolvedValue({}),
      verifyConnection: mockVerifyConnection.mockResolvedValue({ success: true }),
      clearCache: mockClearCache.mockResolvedValue({}),
    });

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-url')).toBeInTheDocument();
    });

    // Enter invalid URL
    const urlInput = screen.getByTestId('input-url');
    fireEvent.change(urlInput, { target: { value: 'not-a-valid-url' } });

    // Try to save
    const saveButton = screen.getByRole('button', { name: /save/i });
    fireEvent.click(saveButton);

    // Should show validation error
    await waitFor(() => {
      expect(screen.getByText('URL is not valid')).toBeInTheDocument();
    });
  });

  it('validates timeout range (1-3600)', async () => {
    mockedUseCrowdApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchConfig: mockFetchConfig.mockResolvedValue({
        ...mockSettings,
        applicationName: 'nexus',
        applicationPassword: 'secret',
        url: 'http://crowd.example.com',
      }),
      saveConfig: mockSaveConfig.mockResolvedValue({}),
      verifyConnection: mockVerifyConnection.mockResolvedValue({ success: true }),
      clearCache: mockClearCache.mockResolvedValue({}),
    });

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-timeout')).toBeInTheDocument();
    });

    // Enter invalid timeout (too high)
    const timeoutInput = screen.getByTestId('input-timeout');
    fireEvent.change(timeoutInput, { target: { value: '5000' } });

    // Try to save
    const saveButton = screen.getByRole('button', { name: /save/i });
    fireEvent.click(saveButton);

    // Should show validation error
    await waitFor(() => {
      expect(screen.getByText('Timeout must be between 1 and 3600 seconds')).toBeInTheDocument();
    });
  });
});


