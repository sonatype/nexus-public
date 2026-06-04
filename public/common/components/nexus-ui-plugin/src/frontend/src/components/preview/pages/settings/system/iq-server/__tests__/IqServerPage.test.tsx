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

import { IqServerPage } from '../IqServerPage';
import * as useIqServerApiModule from '../useIqServerApi';

// Mock the API hook
jest.mock('../useIqServerApi');

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: { go: jest.fn() },
  }),
}));

const mockedUseIqServerApi = useIqServerApiModule.useIqServerApi as jest.MockedFunction<typeof useIqServerApiModule.useIqServerApi>;

// Mock ExtJS via local path (the component imports from interface/ExtJS directly)
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(undefined),
    }),
  },
}));

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('IqServerPage', () => {
  const mockSettings = {
    enabled: true,
    url: 'https://iq.example.com',
    authenticationType: 'USER' as const,
    username: 'admin',
    password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
    useTrustStoreForUrl: false,
    timeoutSeconds: null,
    properties: '',
    showLink: true,
  };

  const mockFetchSettings = jest.fn();
  const mockSaveSettings = jest.fn();
  const mockVerifyConnection = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // Reset ExtJS.checkPermission to return true for editable form
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(true);
    ExtJS.state().getValue.mockImplementation((key: string) => {
      if (key === 'user') return { id: 'admin', name: 'admin', administrator: true };
      return undefined;
    });

    mockedUseIqServerApi.mockReturnValue({
      loading: false,
      verifying: false,
      error: null,
      setError: mockSetError,
      fetchSettings: mockFetchSettings.mockResolvedValue(mockSettings),
      fetchCapabilities: jest.fn().mockResolvedValue({ hasLifecycle: false, hasFirewall: false }),
      fetchCapabilitiesWithConfig: jest.fn().mockResolvedValue({ hasLifecycle: false, hasFirewall: false }),
      saveSettings: mockSaveSettings.mockResolvedValue(mockSettings),
      verifyConnection: mockVerifyConnection.mockResolvedValue({ success: true, reason: '5' }),
    });
  });

  it('renders loading state initially', () => {
    // Use a never-resolving promise so the component stays in loading state
    mockedUseIqServerApi.mockReturnValue({
      loading: false,
      verifying: false,
      error: null,
      setError: mockSetError,
      fetchSettings: jest.fn().mockReturnValue(new Promise(() => {})),
      fetchCapabilities: jest.fn().mockReturnValue(new Promise(() => {})),
      fetchCapabilitiesWithConfig: jest.fn().mockResolvedValue({ hasLifecycle: false, hasFirewall: false }),
      saveSettings: mockSaveSettings.mockResolvedValue(mockSettings),
      verifyConnection: mockVerifyConnection.mockResolvedValue({ success: true, reason: '5' }),
    });

    render(<IqServerPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading IQ Server settings...')).toBeInTheDocument();
  });

  it('renders the page header', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'IQ Server' })).toBeInTheDocument();
    });

    expect(screen.getByText('Manage Sonatype Repository Firewall and Lifecycle configuration')).toBeInTheDocument();
  });

  it('displays current settings', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Enable IQ Server/)).toBeInTheDocument();
    });

    expect(screen.getByLabelText(/Enable IQ Server/)).toBeChecked();
    expect(screen.getByDisplayValue('https://iq.example.com')).toBeInTheDocument();
    expect(screen.getByDisplayValue('admin')).toBeInTheDocument();
  });

  it('makes IQ Server URL read-only in cloud mode', async () => {
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.state().getValue.mockImplementation((key: string) => {
      if (key === 'isCloud') return true;
      if (key === 'user') return { id: 'admin', name: 'admin', administrator: true };
      return undefined;
    });

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-url')).toBeInTheDocument();
    });

    const urlInput = screen.getByTestId('input-url');
    expect(urlInput).toHaveAttribute('readOnly');
    expect(
      screen.getByText('The IQ Server URL is set for your cloud environment and cannot be changed here.')
    ).toBeInTheDocument();
  });

  it('shows dashboard link when enabled and configured', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Open IQ Server Dashboard')).toBeInTheDocument();
    });

    const link = screen.getByText('Open IQ Server Dashboard');
    expect(link.closest('a')).toHaveAttribute('href', 'https://iq.example.com');
  });

  it('shows authentication fields for USER type', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Username/)).toBeInTheDocument();
    });

    expect(screen.getByLabelText(/Password/)).toBeInTheDocument();
  });

  it('hides authentication fields for PKI type', async () => {
    mockFetchSettings.mockResolvedValue({
      ...mockSettings,
      authenticationType: 'PKI',
      username: '',
      password: '',
    });

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('select-authenticationType')).toBeInTheDocument();
    });

    expect(screen.queryByLabelText(/Username/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Password/)).not.toBeInTheDocument();
  });

  it('saves changes when Save button is clicked', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Username/)).toBeInTheDocument();
    });

    // Make a change
    const usernameInput = screen.getByLabelText(/Username/);
    fireEvent.change(usernameInput, { target: { value: 'newadmin' } });

    // Click save
    const saveButton = screen.getByRole('button', { name: 'Save' });
    await waitFor(() => {
      expect(saveButton).not.toBeDisabled();
    });

    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockSaveSettings).toHaveBeenCalled();
    });
  });

  it('discards changes when Discard button is clicked', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Username/)).toBeInTheDocument();
    });

    // Make a change
    const usernameInput = screen.getByLabelText(/Username/);
    fireEvent.change(usernameInput, { target: { value: 'newadmin' } });

    // Click discard
    const discardButton = screen.getByRole('button', { name: 'Discard' });
    fireEvent.click(discardButton);

    // SettingsForm has confirmDiscard=true by default, so click "Leave" in confirmation dialog
    const leaveButton = await screen.findByRole('button', { name: /leave/i });
    fireEvent.click(leaveButton);

    // Should be back to original value
    await waitFor(() => {
      expect(usernameInput).toHaveValue('admin');
    });
  });

  it('verifies connection successfully', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Test Connection' })).toBeInTheDocument();
    });

    const verifyButton = screen.getByRole('button', { name: 'Test Connection' });
    fireEvent.click(verifyButton);

    await waitFor(() => {
      expect(screen.getByText(/Connection successful/)).toBeInTheDocument();
    });
  });

  it('opens full-screen application list when View application list is clicked', async () => {
    mockVerifyConnection.mockResolvedValue({
      success: true,
      reason: 'MyApp, BackendService, FrontendApp',
    });

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Test Connection' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Test Connection' }));

    await waitFor(() => {
      expect(screen.getByText(/Connection successful/)).toBeInTheDocument();
    });

    const viewListButton = screen.getByRole('button', { name: /View application list/i });
    fireEvent.click(viewListButton);

    await waitFor(() => {
      expect(screen.getByRole('dialog', { name: /IQ Server Applications/i })).toBeInTheDocument();
      expect(screen.getByText('MyApp')).toBeInTheDocument();
      expect(screen.getByText('BackendService')).toBeInTheDocument();
      expect(screen.getByText('FrontendApp')).toBeInTheDocument();
    });

    // Fullscreen container is a fixed Box, not Dialog — uses CSS class for sizing
    const modal = screen.getByTestId('iq-application-list-modal');
    expect(modal).toBeInTheDocument();
    expect(modal).toHaveClass('iq-app-list-fullscreen');

    // Count badge shows item count
    expect(screen.getByTestId('app-count-badge')).toHaveTextContent('3 applications');

    // Close via Minimize button
    const closeButton = screen.getByRole('button', { name: 'Close application list' });
    fireEvent.click(closeButton);

    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: /IQ Server Applications/i })).not.toBeInTheDocument();
    });
  });

  it('modal search filters application list', async () => {
    mockVerifyConnection.mockResolvedValue({
      success: true,
      reason: 'MyApp, BackendService, FrontendApp',
    });

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Test Connection' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Test Connection' }));

    await waitFor(() => {
      expect(screen.getByText(/Connection successful/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /View application list/i }));

    await waitFor(() => {
      expect(screen.getByText('MyApp')).toBeInTheDocument();
    });

    // Type in search box
    const searchInput = screen.getByPlaceholderText('Search applications...');
    fireEvent.change(searchInput, { target: { value: 'backend' } });

    // Only BackendService should remain visible
    expect(screen.getByText('BackendService')).toBeInTheDocument();
    expect(screen.queryByText('MyApp')).not.toBeInTheDocument();
    expect(screen.queryByText('FrontendApp')).not.toBeInTheDocument();
  });

  it('shows connection failure message', async () => {
    mockVerifyConnection.mockResolvedValue({ success: false, reason: 'Authentication failed' });

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Test Connection' })).toBeInTheDocument();
    });

    const verifyButton = screen.getByRole('button', { name: 'Test Connection' });
    fireEvent.click(verifyButton);

    await waitFor(() => {
      expect(screen.getByText(/Connection failed: Authentication failed/)).toBeInTheDocument();
    });
  });

  it('shows validation error for invalid URL', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/IQ Server URL/)).toBeInTheDocument();
    });

    // Enter invalid URL
    const urlInput = screen.getByLabelText(/IQ Server URL/);
    fireEvent.change(urlInput, { target: { value: 'not-a-url' } });
    fireEvent.blur(urlInput);

    // Should show error
    await waitFor(() => {
      expect(screen.getByText('Please enter a valid URL')).toBeInTheDocument();
    });
  });

  it('shows read-only view when user lacks update permission', async () => {
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(false);

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('IQ Server')).toBeInTheDocument();
    });

    // Save button should not be present
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();

    // Should show read-only view
    expect(screen.getByText('Current Settings')).toBeInTheDocument();
  });

  it('shows trust store option for HTTPS URLs', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Use Nexus Repository Trust Store/)).toBeInTheDocument();
    });
  });

  it('hides trust store option for HTTP URLs', async () => {
    mockFetchSettings.mockResolvedValue({
      ...mockSettings,
      url: 'http://iq.example.com',
    });

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/IQ Server URL/)).toBeInTheDocument();
    });

    expect(screen.queryByLabelText(/Use Nexus Repository Trust Store/)).not.toBeInTheDocument();
  });

  it('displays help section', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('About IQ Server')).toBeInTheDocument();
    });

    expect(screen.getByText('documentation')).toHaveAttribute(
      'href',
      'http://links.sonatype.com/products/nxrm3/docs/iq'
    );
  });

  it('toggles enabled checkbox', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Enable IQ Server/)).toBeInTheDocument();
    });

    const checkbox = screen.getByLabelText(/Enable IQ Server/);
    expect(checkbox).toBeChecked();

    fireEvent.click(checkbox);
    expect(checkbox).not.toBeChecked();
  });

  it('changes authentication type and hides username/password fields', async () => {
    // Start with PKI type
    mockFetchSettings.mockResolvedValue({
      ...mockSettings,
      authenticationType: 'PKI',
      username: '',
      password: '',
    });

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('select-authenticationType')).toBeInTheDocument();
    });

    // Username and password fields should not be present for PKI
    expect(screen.queryByLabelText(/Username/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Password/)).not.toBeInTheDocument();
  });

  it('dispatches nx-sidebar-toggle event when app list opens and closes (bchz)', async () => {
    mockVerifyConnection.mockResolvedValue({
      success: true,
      reason: 'MyApp, BackendService',
    });

    const events: any[] = [];
    window.addEventListener('nx-sidebar-toggle', (e) => events.push((e as CustomEvent).detail));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => screen.getByRole('button', { name: 'Test Connection' }));
    fireEvent.click(screen.getByRole('button', { name: 'Test Connection' }));
    await waitFor(() => screen.getByText(/Connection successful/));

    fireEvent.click(screen.getByRole('button', { name: /View application list/i }));
    await waitFor(() => expect(screen.getByTestId('iq-application-list-modal')).toBeInTheDocument());

    // Sidebar collapses on open
    expect(events.some((d) => d.open === false)).toBe(true);

    fireEvent.click(screen.getByRole('button', { name: 'Close application list' }));
    await waitFor(() => expect(screen.queryByTestId('iq-application-list-modal')).not.toBeInTheDocument());
  });

  it('closes app list on Escape key (bchz)', async () => {
    mockVerifyConnection.mockResolvedValue({
      success: true,
      reason: 'MyApp, BackendService',
    });

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => screen.getByRole('button', { name: 'Test Connection' }));
    fireEvent.click(screen.getByRole('button', { name: 'Test Connection' }));
    await waitFor(() => screen.getByText(/Connection successful/));

    fireEvent.click(screen.getByRole('button', { name: /View application list/i }));
    await waitFor(() => expect(screen.getByTestId('iq-application-list-modal')).toBeInTheDocument());

    fireEvent.keyDown(window, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByTestId('iq-application-list-modal')).not.toBeInTheDocument());
  });
});


