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
import { waitForFormToLoad } from '../../../../../../__tests__/test-utils/form-helpers';

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

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
  },
  ExtAPIUtils: {
    extAPIRequest: jest.fn(),
    checkForError: jest.fn(),
    extractResult: jest.fn(),
  },
  APIConstants: {
    EXT: {
      EMAIL_SERVER: {
        ACTION: 'coreui_Email',
        METHODS: {
          READ: 'read',
          UPDATE: 'update',
          VERIFY: 'sendVerification',
        },
      },
    },
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
    // Reset ExtJS permission mock to default (true) before each test
    // This ensures tests that modify it don't affect subsequent tests
    const { ExtJS } = require('@sonatype/nexus-ui-plugin');
    ExtJS.checkPermission.mockReturnValue(true);
    
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
      expect(screen.getByText('Email Server')).toBeInTheDocument();
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

  it('saves changes when Save button is clicked', async () => {
    const mockSubmit = jest.fn();
    mockedUseEmailForm.mockReturnValue(createEmailFormMock(
      { ...mockSettings, host: 'new-smtp.example.com' }, { isPristine: false, submit: mockSubmit }
    ));
    render(<EmailPage />, { wrapper: TestWrapper });

    await waitForFormToLoad();

    const saveButton = screen.getByRole('button', { name: 'Save' });
    fireEvent.click(saveButton);

    expect(mockSubmit).toHaveBeenCalled();
  });

  it('discards changes when Discard button is clicked', async () => {
    mockedUseEmailForm.mockReturnValue(createEmailFormMock(mockSettings, { isPristine: false }));
    render(<EmailPage />, { wrapper: TestWrapper });

    await waitForFormToLoad();

    // Make a change (use regex to handle required asterisk)
    const hostInput = screen.getByLabelText(/SMTP Host/);
    fireEvent.change(hostInput, { target: { value: 'new-smtp.example.com' } });

    // Click discard
    const discardButton = screen.getByRole('button', { name: 'Discard' });
    fireEvent.click(discardButton);

    // SettingsForm has confirmDiscard=true by default, so click "Leave" in confirmation dialog
    const leaveButton = await screen.findByRole('button', { name: /leave/i });
    fireEvent.click(leaveButton);

    // Should be back to original value
    await waitFor(() => {
      expect(hostInput).toHaveValue('smtp.example.com');
    });
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
      expect(screen.getByText('Email Server')).toBeInTheDocument();
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

  it('shows read-only view when user lacks update permission', async () => {
    const { ExtJS } = require('@sonatype/nexus-ui-plugin');
    ExtJS.checkPermission.mockReturnValue(false);

    render(<EmailPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Email Server')).toBeInTheDocument();
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
});

