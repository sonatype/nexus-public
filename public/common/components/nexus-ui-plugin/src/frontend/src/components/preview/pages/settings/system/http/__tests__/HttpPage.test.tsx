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

import { HttpPage } from '../HttpPage';
import * as useHttpApiModule from '../useHttpApi';
import * as useHttpFormModule from '../useHttpForm';

// Mock the API hook and form hook
jest.mock('../useHttpApi');
jest.mock('../useHttpForm');

const mockedUseHttpApi = useHttpApiModule.useHttpApi as jest.MockedFunction<typeof useHttpApiModule.useHttpApi>;
const mockedUseHttpForm = useHttpFormModule.useHttpForm as jest.MockedFunction<typeof useHttpFormModule.useHttpForm>;

function createHttpFormMock(data: Record<string, any>, overrides: Record<string, any> = {}) {
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
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
  },
}));

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('HttpPage', () => {
  const mockSettings = {
    userAgentSuffix: 'Nexus/3.x',
    timeout: 30,
    retries: 3,
    httpEnabled: false,
    httpHost: '',
    httpPort: null,
    httpAuthEnabled: false,
    httpAuthUsername: '',
    httpAuthPassword: '',
    httpAuthNtlmHost: '',
    httpAuthNtlmDomain: '',
    httpsEnabled: false,
    httpsHost: '',
    httpsPort: null,
    httpsAuthEnabled: false,
    httpsAuthUsername: '',
    httpsAuthPassword: '',
    httpsAuthNtlmHost: '',
    httpsAuthNtlmDomain: '',
    nonProxyHosts: [],
  };

  const mockFetchSettings = jest.fn();
  const mockSaveSettings = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // Reset ExtJS.checkPermission to return true for editable form
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(true);

    mockedUseHttpApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchSettings: mockFetchSettings.mockResolvedValue(mockSettings),
      saveSettings: mockSaveSettings.mockResolvedValue(mockSettings),
    });
    // Default form hook mock with loaded settings
    mockedUseHttpForm.mockReturnValue(createHttpFormMock(mockSettings));
  });

  it('renders loading state initially', () => {
    mockedUseHttpForm.mockReturnValue(createHttpFormMock(mockSettings, { isLoading: true }));
    render(<HttpPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading HTTP settings...')).toBeInTheDocument();
  });

  it('renders the page header', async () => {
    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('HTTP Settings')).toBeInTheDocument();
    });

    expect(screen.getByText('Configure HTTP proxy and connection settings')).toBeInTheDocument();
  });

  it('displays connection settings', async () => {
    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText('User-Agent Suffix')).toBeInTheDocument();
    });

    expect(screen.getByDisplayValue('Nexus/3.x')).toBeInTheDocument();
    expect(screen.getByDisplayValue('30')).toBeInTheDocument();
    expect(screen.getByDisplayValue('3')).toBeInTheDocument();
  });

  it('shows HTTP proxy fields when httpEnabled is true', async () => {
    mockedUseHttpForm.mockReturnValue(createHttpFormMock(
      { ...mockSettings, httpEnabled: true, httpHost: 'proxy.example.com', httpPort: 8080 }
    ));
    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Enable HTTP Proxy/)).toBeInTheDocument();
    });

    const httpCheckbox = screen.getByLabelText(/Enable HTTP Proxy/);
    expect(httpCheckbox).toBeChecked();
    expect(screen.getByDisplayValue('proxy.example.com')).toBeInTheDocument();
  });

  it('shows HTTPS proxy fields when httpsEnabled is true', async () => {
    mockedUseHttpForm.mockReturnValue(createHttpFormMock(
      { ...mockSettings, httpsEnabled: true, httpsHost: 'proxy.example.com', httpsPort: 8443 }
    ));
    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Enable HTTPS Proxy/)).toBeInTheDocument();
    });

    const httpsCheckbox = screen.getByLabelText(/Enable HTTPS Proxy/);
    expect(httpsCheckbox).toBeChecked();
    expect(screen.getByDisplayValue('proxy.example.com')).toBeInTheDocument();
  });

  it('saves changes when Save button is clicked', async () => {
    const mockSubmit = jest.fn();
    mockedUseHttpForm.mockReturnValue(createHttpFormMock(mockSettings, { isPristine: false, submit: mockSubmit }));
    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText('User-Agent Suffix')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: 'Save' });
    fireEvent.click(saveButton);

    expect(mockSubmit).toHaveBeenCalled();
  });

  it('discards changes when Discard button is clicked', async () => {
    mockedUseHttpForm.mockReturnValue(createHttpFormMock(mockSettings, { isPristine: false }));
    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText('User-Agent Suffix')).toBeInTheDocument();
    });

    // Make a change
    const userAgentInput = screen.getByLabelText('User-Agent Suffix');
    fireEvent.change(userAgentInput, { target: { value: 'Custom/1.0' } });

    // Click discard
    const discardButton = screen.getByRole('button', { name: 'Discard' });
    fireEvent.click(discardButton);

    // SettingsForm has confirmDiscard=true by default, so click "Leave" in confirmation dialog
    const leaveButton = await screen.findByRole('button', { name: /leave/i });
    fireEvent.click(leaveButton);

    // Should be back to original value
    await waitFor(() => {
      expect(userAgentInput).toHaveValue('Nexus/3.x');
    });
  });

  it('shows validation error for invalid timeout', async () => {
    mockedUseHttpForm.mockReturnValue(createHttpFormMock(
      { ...mockSettings, timeout: 9999 }, {
        isPristine: false,
        hasValidationErrors: true,
        touched: { timeout: true },
        validationErrors: { timeout: 'Timeout must be between 1 and 3600 seconds' },
      }
    ));
    render(<HttpPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Timeout must be between 1 and 3600 seconds')).toBeInTheDocument();
  });

  it('displays error state via saveError', async () => {
    mockedUseHttpForm.mockReturnValue(createHttpFormMock(mockSettings, {
      saveError: 'Failed to load settings',
    }));

    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      const matches = screen.getAllByText('Failed to load settings');
      expect(matches.length).toBeGreaterThanOrEqual(1);
    });
  });

  it('shows read-only view when user lacks update permission', async () => {
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(false);

    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('HTTP Settings')).toBeInTheDocument();
    });

    // Save button should not be present
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();

    // Should show read-only view
    expect(screen.getByText('Current Settings')).toBeInTheDocument();
  });

  it('shows non-proxy hosts section when proxy is enabled', async () => {
    mockedUseHttpForm.mockReturnValue(createHttpFormMock({
      ...mockSettings,
      httpEnabled: true,
      httpHost: 'proxy.example.com',
      httpPort: 8080,
    }));

    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Hosts to Exclude from Proxy')).toBeInTheDocument();
    });
  });

  it('shows existing non-proxy hosts when proxy is enabled', async () => {
    mockedUseHttpForm.mockReturnValue(createHttpFormMock({
      ...mockSettings,
      httpEnabled: true,
      httpHost: 'proxy.example.com',
      httpPort: 8080,
      nonProxyHosts: ['*.internal.com'],
    }));

    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('*.internal.com')).toBeInTheDocument();
    });
  });

  it('displays help section', async () => {
    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('About HTTP Settings')).toBeInTheDocument();
    });

    expect(screen.getByText('documentation')).toHaveAttribute(
      'href',
      'https://help.sonatype.com/en/http-configuration.html'
    );
  });

  it('renders form content', async () => {
    render(<HttpPage />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByTestId('http-form')).toBeInTheDocument();
    });
  });

  describe('number field key filtering', () => {
    it('prevents typing "e" in timeout field', async () => {
      render(<HttpPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('input-timeout')).toBeInTheDocument();
      });

      const timeoutInput = screen.getByTestId('input-timeout');
      const event = fireEvent.keyDown(timeoutInput, { key: 'e' });
      expect(event).toBe(false);
    });

    it('allows typing "5" in timeout field', async () => {
      render(<HttpPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('input-timeout')).toBeInTheDocument();
      });

      const timeoutInput = screen.getByTestId('input-timeout');
      const event = fireEvent.keyDown(timeoutInput, { key: '5' });
      expect(event).toBe(true);
    });

    it('prevents typing "+" in retries field', async () => {
      render(<HttpPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('input-retries')).toBeInTheDocument();
      });

      const retriesInput = screen.getByTestId('input-retries');
      const event = fireEvent.keyDown(retriesInput, { key: '+' });
      expect(event).toBe(false);
    });
  });
});


