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
import { ToastProvider } from '../../../../../shared';

import { EmailPage } from '../EmailPage';
import * as useEmailApiModule from '../useEmailApi';
import * as useEmailFormModule from '../useEmailForm';
import { waitForFormToLoad } from '../../../../../../../__tests__/test-utils/form-helpers';

// Mock the API hook and form hook
jest.mock('../useEmailApi');
jest.mock('../useEmailForm');

const mockedUseEmailApi = useEmailApiModule.useEmailApi as jest.MockedFunction<typeof useEmailApiModule.useEmailApi>;
const mockedUseEmailForm = useEmailFormModule.useEmailForm as jest.MockedFunction<typeof useEmailFormModule.useEmailForm>;

function createEmailFormMock(data: Record<string, any>, overrides: Record<string, any> = {}) {
  return {
    field: jest.fn((name: string) => ({
      name,
      value: data[name] != null ? String(data[name]) : '',
      error: undefined,
      onChange: jest.fn(),
      onBlur: jest.fn(),
    })),
    checkbox: jest.fn((name: string) => ({
      name,
      checked: Boolean(data[name]),
      error: undefined,
      onChange: jest.fn(),
    })),
    submit: jest.fn(),
    reset: jest.fn(),
    isPristine: true,
    isSaving: false,
    isLoading: false,
    hasLoadError: false,
    hasValidationErrors: false,
    data,
    touched: {} as Record<string, boolean>,
    validationErrors: {} as Record<string, string>,
    saveError: null as string | null,
    loadError: null as string | null,
    state: { context: {}, matches: jest.fn(() => false) },
    send: jest.fn(),
    ...overrides,
  } as any;
}

// Mock ExtJS via local path (EmailPage imports from interface/ExtJS directly)
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
    state: jest.fn(() => ({ getValue: jest.fn().mockReturnValue(false) })),
  },
}));

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme><ToastProvider>{children}</ToastProvider></Theme>;
}

describe('EmailPage', () => {
  const mockSettings = {
    enabled: true,
    host: 'smtp.example.com',
    port: 587,
    useAuthentication: true,
    username: 'user@example.com',
    password: '',
    fromAddress: 'noreply@example.com',
    subjectPrefix: '[Nexus]',
    startTlsEnabled: true,
    startTlsRequired: false,
    sslOnConnectEnabled: false,
    sslCheckServerIdentityEnabled: false,
    nexusTrustStoreEnabled: false,
  };

  const mockFetchSettings = jest.fn();
  const mockSaveSettings = jest.fn();
  const mockSendVerificationEmail = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // Reset ExtJS mocks to default (self-hosted, update allowed) before each test
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(true);
    ExtJS.state.mockReturnValue({ getValue: jest.fn().mockReturnValue(false) });
    
    mockedUseEmailApi.mockReturnValue({
      loading: false,
      verifying: false,
      error: null,
      setError: mockSetError,
      fetchSettings: mockFetchSettings.mockResolvedValue(mockSettings),
      saveSettings: mockSaveSettings.mockResolvedValue(mockSettings),
      sendVerificationEmail: mockSendVerificationEmail.mockResolvedValue({ success: true }),
    });
    // Default form hook mock with loaded settings
    mockedUseEmailForm.mockReturnValue(createEmailFormMock(mockSettings));
  });

  it('renders loading state initially', () => {
    mockedUseEmailForm.mockReturnValue(createEmailFormMock(mockSettings, { isLoading: true }));
    render(<EmailPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading email server settings...')).toBeInTheDocument();
  });

  it('renders the page header', async () => {
    render(<EmailPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Email Server' })).toBeInTheDocument();
    });

    expect(screen.getByText('Configure outgoing email server settings')).toBeInTheDocument();
  });

  it('displays current settings', async () => {
    render(<EmailPage />, { wrapper: TestWrapper });

    await waitForFormToLoad();

    expect(screen.getByLabelText(/Enable email server/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Enable email server/)).toBeChecked();
    expect(screen.getByDisplayValue('smtp.example.com')).toBeInTheDocument();
    expect(screen.getByDisplayValue('587')).toBeInTheDocument();
    expect(screen.getByDisplayValue('noreply@example.com')).toBeInTheDocument();
  });

  it('enables save button when changes are made', async () => {
    mockedUseEmailForm.mockReturnValue(createEmailFormMock(mockSettings, { isPristine: false }));
    render(<EmailPage />, { wrapper: TestWrapper });

    await waitForFormToLoad();

    const saveButton = screen.getByRole('button', { name: 'Save' });
    expect(saveButton).not.toBeDisabled();
  });

  it('calls reset when Discard is confirmed', async () => {
    const mockReset = jest.fn();
    mockedUseEmailForm.mockReturnValue(createEmailFormMock(mockSettings, { isPristine: false, reset: mockReset }));
    render(<EmailPage />, { wrapper: TestWrapper });

    await waitForFormToLoad();

    const discardButton = screen.getByRole('button', { name: 'Discard' });
    fireEvent.click(discardButton);

    // SettingsForm has confirmDiscard=true by default, so click "Leave" in confirmation dialog
    const leaveButton = await screen.findByRole('button', { name: /leave/i });
    fireEvent.click(leaveButton);

    expect(mockReset).toHaveBeenCalled();
  });

  it('shows validation error when host is empty', async () => {
    mockedUseEmailForm.mockReturnValue(createEmailFormMock(
      { ...mockSettings, host: '' }, {
        isPristine: false,
        hasValidationErrors: true,
        field: jest.fn((name: string) => ({
          name,
          value: name === 'host' ? '' : String(mockSettings[name as keyof typeof mockSettings] ?? ''),
          error: name === 'host' ? 'SMTP host is required' : undefined,
          onChange: jest.fn(),
          onBlur: jest.fn(),
        })),
      }
    ));
    render(<EmailPage />, { wrapper: TestWrapper });

    expect(screen.getByText('SMTP host is required')).toBeInTheDocument();
  });

  it('shows validation error for invalid email address', async () => {
    mockedUseEmailForm.mockReturnValue(createEmailFormMock(
      { ...mockSettings, fromAddress: 'invalid-email' }, {
        isPristine: false,
        hasValidationErrors: true,
        field: jest.fn((name: string) => ({
          name,
          value: name === 'fromAddress' ? 'invalid-email' : String(mockSettings[name as keyof typeof mockSettings] ?? ''),
          error: name === 'fromAddress' ? 'Invalid email address format' : undefined,
          onChange: jest.fn(),
          onBlur: jest.fn(),
        })),
      }
    ));
    render(<EmailPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Invalid email address format')).toBeInTheDocument();
  });

  it('renders enabled checkbox as checked', async () => {
    render(<EmailPage />, { wrapper: TestWrapper });

    await waitForFormToLoad();

    const checkbox = screen.getByLabelText(/Enable email server/);
    expect(checkbox).toBeChecked();
    // Verify the checkbox hook was called
    const formMock = mockedUseEmailForm.mock.results[0]?.value;
    expect(formMock?.checkbox).toHaveBeenCalledWith('enabled');
  });

  it('handles error state gracefully', async () => {
    mockedUseEmailApi.mockReturnValue({
      loading: false,
      verifying: false,
      error: 'Failed to load settings',
      setError: mockSetError,
      fetchSettings: mockFetchSettings.mockResolvedValue(mockSettings),
      saveSettings: mockSaveSettings,
      sendVerificationEmail: mockSendVerificationEmail,
    });

    render(<EmailPage />, { wrapper: TestWrapper });

    // Error is now shown via Toast notifications (Sprint 15)
    // Page should still render
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Email Server' })).toBeInTheDocument();
    });
  });

  it('calls submit when Save is clicked with dirty form', async () => {
    const mockSubmit = jest.fn();
    mockedUseEmailForm.mockReturnValue(createEmailFormMock(mockSettings, { isPristine: false, submit: mockSubmit }));
    render(<EmailPage />, { wrapper: TestWrapper });

    await waitForFormToLoad();

    const saveButton = screen.getByRole('button', { name: 'Save' });
    fireEvent.click(saveButton);

    expect(mockSubmit).toHaveBeenCalled();
  });

  it('displays help section with documentation link', async () => {
    render(<EmailPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('About Email Server')).toBeInTheDocument();
    });

    expect(screen.getByText('documentation')).toHaveAttribute(
      'href',
      'https://help.sonatype.com/en/email-server-configuration.html'
    );
  });

  it('renders nothing on cloud deployments', () => {
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.state.mockReturnValue({ getValue: jest.fn().mockReturnValue(true) });

    render(<EmailPage />, { wrapper: TestWrapper });

    expect(screen.queryByText('Email Server')).not.toBeInTheDocument();
    expect(screen.queryByTestId('email-form')).not.toBeInTheDocument();
  });

  it('shows read-only view when user lacks update permission', async () => {
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(false);

    render(<EmailPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Email Server' })).toBeInTheDocument();
    });

    // Save button should not be present
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();

    // Should show read-only view
    expect(screen.getByText('Current Settings')).toBeInTheDocument();
  });

  it('displays port value from settings', async () => {
    render(<EmailPage />, { wrapper: TestWrapper });

    await waitForFormToLoad();

    const portInput = screen.getByDisplayValue('587');
    expect(portInput).toBeInTheDocument();
  });

  it('displays SSL/TLS checkbox state from settings', async () => {
    render(<EmailPage />, { wrapper: TestWrapper });

    await waitForFormToLoad();

    // Verify the form hook was used for SSL/TLS fields
    const formMock = mockedUseEmailForm.mock.results[0]?.value;
    expect(formMock?.checkbox).toHaveBeenCalledWith('startTlsEnabled');
  });

  it('shows validation error for invalid port', async () => {
    mockedUseEmailForm.mockReturnValue(createEmailFormMock(
      { ...mockSettings, port: 99999 }, {
        isPristine: false,
        hasValidationErrors: true,
        touched: { port: true },
        validationErrors: { port: 'Port must be between 1 and 65535' },
      }
    ));
    render(<EmailPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Port must be between 1 and 65535')).toBeInTheDocument();
  });

  it('renders form content', async () => {
    render(<EmailPage />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByTestId('email-form')).toBeInTheDocument();
    });
  });

  it('has nxrm-email-save analytics ID on the Save button', async () => {
    mockedUseEmailForm.mockReturnValue(
      createEmailFormMock(mockSettings, { isPristine: false })
    );
    render(<EmailPage />, { wrapper: TestWrapper });
    await waitForFormToLoad();

    const saveButton = screen.getByRole('button', { name: 'Save' });
    expect(saveButton).toHaveAttribute('data-analytics-id', 'nxrm-email-save');
  });

  it('renders EmailVerify outside the SettingsForm element', async () => {
    mockedUseEmailForm.mockReturnValue(createEmailFormMock(mockSettings));
    render(<EmailPage />, { wrapper: TestWrapper });
    await waitForFormToLoad();

    const form = screen.getByTestId('email-form');
    const verifySection = screen.getByTestId('email-verify-section');

    expect(form).not.toContainElement(verifySection);
  });

  describe('Authentication toggle', () => {
    it('shows auth toggle checkbox', async () => {
      mockedUseEmailForm.mockReturnValue(createEmailFormMock(mockSettings));
      render(<EmailPage />, { wrapper: TestWrapper });
      await waitForFormToLoad();

      expect(screen.getByLabelText(/Enable authentication/i)).toBeInTheDocument();
    });

    it('shows username and password fields when auth toggle is checked', async () => {
      mockedUseEmailForm.mockReturnValue(
        createEmailFormMock({ ...mockSettings, useAuthentication: true, username: 'user@example.com' })
      );
      render(<EmailPage />, { wrapper: TestWrapper });
      await waitForFormToLoad();

      await waitFor(() => {
        expect(screen.getByLabelText(/^Username/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/^Password/i)).toBeInTheDocument();
      });
    });

    it('hides username and password fields when auth toggle is unchecked', async () => {
      mockedUseEmailForm.mockReturnValue(
        createEmailFormMock({ ...mockSettings, useAuthentication: false, username: '' })
      );
      render(<EmailPage />, { wrapper: TestWrapper });
      await waitForFormToLoad();

      await waitFor(() => {
        expect(screen.queryByLabelText(/^Username/i)).not.toBeInTheDocument();
        expect(screen.queryByLabelText(/^Password/i)).not.toBeInTheDocument();
      });
    });

    it('preserves username/password in form data when auth toggle is unchecked', async () => {
      // Credentials remain in form state when auth is disabled; only useAuthentication changes.
      // The save layer strips credentials from the REST payload when useAuthentication is false.
      mockedUseEmailForm.mockReturnValue(
        createEmailFormMock({ ...mockSettings, useAuthentication: false, username: 'user@example.com' })
      );
      render(<EmailPage />, { wrapper: TestWrapper });
      await waitForFormToLoad();

      // Fields are hidden when useAuthentication is false
      expect(screen.queryByLabelText(/^Username/i)).not.toBeInTheDocument();
      expect(screen.queryByLabelText(/^Password/i)).not.toBeInTheDocument();

      // But username value is still in form data (not cleared)
      const form = mockedUseEmailForm.mock.results[0].value;
      expect(form.data.username).toBe('user@example.com');
    });

    it('initialises toggle to false when useAuthentication is false', async () => {
      mockedUseEmailForm.mockReturnValue(
        createEmailFormMock({ ...mockSettings, useAuthentication: false, username: '' })
      );
      render(<EmailPage />, { wrapper: TestWrapper });
      await waitForFormToLoad();

      const authToggle = await screen.findByLabelText(/Enable authentication/i);
      await waitFor(() => expect(authToggle).not.toBeChecked());
    });

    it('initialises toggle to true when useAuthentication is true', async () => {
      mockedUseEmailForm.mockReturnValue(
        createEmailFormMock({ ...mockSettings, useAuthentication: true, username: 'user@example.com' })
      );
      render(<EmailPage />, { wrapper: TestWrapper });
      await waitForFormToLoad();

      const authToggle = await screen.findByLabelText(/Enable authentication/i);
      await waitFor(() => expect(authToggle).toBeChecked());
    });
  });

  describe('View Certificate button', () => {
    it('is disabled when SMTP host is empty', async () => {
      mockedUseEmailForm.mockReturnValue(
        createEmailFormMock({ ...mockSettings, host: '', port: 587 })
      );
      render(<EmailPage />, { wrapper: TestWrapper });
      await waitForFormToLoad();

      expect(screen.getByRole('button', { name: /View Certificate/i })).toBeDisabled();
    });

    it('is disabled when user lacks nexus:ssl-truststore:read permission', async () => {
      const { ExtJS } = require('../../../../../../../interface/ExtJS');
      ExtJS.checkPermission.mockImplementation((perm: string) => perm !== 'nexus:ssl-truststore:read');

      render(<EmailPage />, { wrapper: TestWrapper });
      await waitForFormToLoad();

      expect(screen.getByRole('button', { name: /View Certificate/i })).toBeDisabled();
    });

    it('is enabled when host and port are set and user has permission', async () => {
      render(<EmailPage />, { wrapper: TestWrapper });
      await waitForFormToLoad();

      expect(screen.getByRole('button', { name: /View Certificate/i })).not.toBeDisabled();
    });
  });

  describe('breadcrumbs', () => {
    it('renders Settings breadcrumb that navigates to settings page', async () => {
      render(<EmailPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      });

      // Click Settings breadcrumb navigates to settings page
      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });

    it('renders Email Server as current page breadcrumb', async () => {
      render(<EmailPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        // The current page item is rendered as Text (not a button) with aria-current="page"
        const breadcrumb = screen.getByText('Email Server', { selector: '[aria-current="page"]' });
        expect(breadcrumb).toBeInTheDocument();
      });
    });
  });
});

