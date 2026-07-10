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
import * as useHttpFormModule from '../useHttpForm';

jest.mock('../useHttpForm');

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
    httpAuthType: '',
    httpAuthUsername: '',
    httpAuthPassword: '',
    httpAuthNtlmHost: '',
    httpAuthNtlmDomain: '',
    httpsEnabled: false,
    httpsHost: '',
    httpsPort: null,
    httpsAuthType: '',
    httpsAuthUsername: '',
    httpsAuthPassword: '',
    httpsAuthNtlmHost: '',
    httpsAuthNtlmDomain: '',
    nonProxyHosts: [],
  };

  beforeEach(() => {
    jest.clearAllMocks();
    // Reset ExtJS.checkPermission to return true for editable form
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(true);

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
      expect(screen.getByRole('heading', { name: 'HTTP Settings' })).toBeInTheDocument();
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

  it('splits comma-separated input into multiple non-proxy hosts on Add', async () => {
    const sendSpy = jest.fn();
    mockedUseHttpForm.mockReturnValue(createHttpFormMock(
      {
        ...mockSettings,
        httpEnabled: true,
        httpHost: 'proxy.example.com',
        httpPort: 8080,
        nonProxyHosts: [],
      },
      { send: sendSpy },
    ));

    render(<HttpPage />, { wrapper: TestWrapper });

    const input = await screen.findByLabelText('Host Pattern');
    fireEvent.change(input, { target: { value: 'a.com, b.com,c.com' } });
    fireEvent.click(screen.getByRole('button', { name: /^Add$/ }));

    const updateCalls = sendSpy.mock.calls.filter(
      ([event]: any[]) => event?.type === 'UPDATE' && event?.name === 'nonProxyHosts',
    );
    expect(updateCalls.length).toBeGreaterThanOrEqual(1);
    const lastUpdate = updateCalls[updateCalls.length - 1][0];
    expect(lastUpdate.value).toEqual(['a.com', 'b.com', 'c.com']);
  });

  it('skips blank tokens and tokens with internal whitespace when splitting on comma', async () => {
    const sendSpy = jest.fn();
    mockedUseHttpForm.mockReturnValue(createHttpFormMock(
      {
        ...mockSettings,
        httpEnabled: true,
        httpHost: 'proxy.example.com',
        httpPort: 8080,
        nonProxyHosts: [],
      },
      { send: sendSpy },
    ));

    render(<HttpPage />, { wrapper: TestWrapper });

    const input = await screen.findByLabelText('Host Pattern');
    fireEvent.change(input, { target: { value: 'a.com, ,bad host , b.com' } });
    fireEvent.click(screen.getByRole('button', { name: /^Add$/ }));

    const updateCalls = sendSpy.mock.calls.filter(
      ([event]: any[]) => event?.type === 'UPDATE' && event?.name === 'nonProxyHosts',
    );
    const lastUpdate = updateCalls[updateCalls.length - 1][0];
    expect(lastUpdate.value).toEqual(['a.com', 'b.com']);
  });

  describe('read-only view (no update permission)', () => {
    beforeEach(() => {
      const { ExtJS } = require('../../../../../../../interface/ExtJS');
      ExtJS.checkPermission.mockReturnValue(false);
    });

    it('shows the page title and read-only banner', async () => {
      render(<HttpPage />, { wrapper: TestWrapper });
      await waitFor(() => {
        expect(screen.getByText('HTTP Settings')).toBeInTheDocument();
      });
      expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
      expect(screen.getByText(/read-only/i)).toBeInTheDocument();
    });

    it('shows configured connection settings', async () => {
      mockedUseHttpForm.mockReturnValue(createHttpFormMock({
        ...mockSettings,
        userAgentSuffix: 'UniqueAgent/99',
        timeout: 999,
        retries: 7,
      }));
      render(<HttpPage />, { wrapper: TestWrapper });
      await waitFor(() => {
        expect(screen.getByText('UniqueAgent/99')).toBeInTheDocument();
      });
      expect(screen.getByText('999')).toBeInTheDocument();
      expect(screen.getByText('7')).toBeInTheDocument();
    });

    it('shows HTTP proxy host, port, and auth username when configured', async () => {
      mockedUseHttpForm.mockReturnValue(createHttpFormMock({
        ...mockSettings,
        httpEnabled: true,
        httpHost: 'proxy.example.com',
        httpPort: 8080,
        httpAuthType: 'username',
        httpAuthUsername: 'svc',
      }));
      render(<HttpPage />, { wrapper: TestWrapper });
      await waitFor(() => {
        expect(screen.getByText('proxy.example.com')).toBeInTheDocument();
      });
      expect(screen.getByText('8080')).toBeInTheDocument();
      expect(screen.getByText('svc')).toBeInTheDocument();
    });

    it('hides HTTP auth fields when auth is disabled', async () => {
      mockedUseHttpForm.mockReturnValue(createHttpFormMock({
        ...mockSettings,
        httpEnabled: true,
        httpHost: 'proxy.example.com',
        httpPort: 8080,
        httpAuthType: '',
        httpAuthUsername: 'should-not-show',
      }));
      render(<HttpPage />, { wrapper: TestWrapper });
      await waitFor(() => {
        expect(screen.getByText('proxy.example.com')).toBeInTheDocument();
      });
      expect(screen.queryByText('should-not-show')).not.toBeInTheDocument();
    });

    it('shows non-proxy hosts when proxy is enabled', async () => {
      mockedUseHttpForm.mockReturnValue(createHttpFormMock({
        ...mockSettings,
        httpEnabled: true,
        httpHost: 'p',
        httpPort: 80,
        nonProxyHosts: ['*.internal.com', 'localhost'],
      }));
      render(<HttpPage />, { wrapper: TestWrapper });
      await waitFor(() => {
        expect(screen.getByText('*.internal.com')).toBeInTheDocument();
      });
      expect(screen.getByText('localhost')).toBeInTheDocument();
    });
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

  it('renders non-proxy hosts in alphabetical order regardless of input order', async () => {
    mockedUseHttpForm.mockReturnValue(createHttpFormMock({
      ...mockSettings,
      httpEnabled: true,
      httpHost: 'proxy.example.com',
      httpPort: 8080,
      nonProxyHosts: ['zeta.example.com', 'alpha.example.com', 'mike.example.com'],
    }));

    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('alpha.example.com')).toBeInTheDocument();
    });

    const buttons = screen.getAllByRole('button', { name: /^Remove / });
    expect(buttons.map((b) => b.getAttribute('aria-label'))).toEqual([
      'Remove alpha.example.com',
      'Remove mike.example.com',
      'Remove zeta.example.com',
    ]);
  });

  it('removes the correct host when sorted display order differs from data order', async () => {
    const send = jest.fn();
    mockedUseHttpForm.mockReturnValue(createHttpFormMock({
      ...mockSettings,
      httpEnabled: true,
      httpHost: 'proxy.example.com',
      httpPort: 8080,
      nonProxyHosts: ['zeta.example.com', 'alpha.example.com'],
    }, { send }));

    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('alpha.example.com')).toBeInTheDocument();
    });

    // Sorted, alpha is first on screen but at index 1 in the underlying data —
    // a regression test against the old index-based remove handler.
    fireEvent.click(screen.getByRole('button', { name: 'Remove alpha.example.com' }));

    expect(send).toHaveBeenCalledWith(expect.objectContaining({
      type: 'UPDATE',
      name: 'nonProxyHosts',
      value: ['zeta.example.com'],
    }));
  });

  it('displays help section', async () => {
    render(<HttpPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('About HTTP Settings')).toBeInTheDocument();
    });

    expect(screen.getByText('documentation')).toHaveAttribute(
      'href',
      'http://links.sonatype.com/products/nxrm3/docs/http-request-and-proxy-settings'
    );
  });

  it('renders form content', async () => {
    render(<HttpPage />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByTestId('http-form')).toBeInTheDocument();
    });
  });

  it('HTTP Auth section shows credentials when httpAuthType is username', async () => {
    mockedUseHttpForm.mockReturnValue(createHttpFormMock({
      ...mockSettings,
      httpEnabled: true,
      httpHost: 'proxy.example.com',
      httpPort: 8080,
      httpAuthType: 'username',
      httpAuthUsername: 'svc',
    }));
    render(<HttpPage />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByDisplayValue('svc')).toBeInTheDocument();
    });
  });

  it('loading state has aria-busy and aria-live attributes', () => {
    mockedUseHttpForm.mockReturnValue(createHttpFormMock(mockSettings, { isLoading: true }));
    const { container } = render(<HttpPage />, { wrapper: TestWrapper });
    const status = container.querySelector('[role="status"]');
    expect(status).toBeInTheDocument();
    expect(status).toHaveAttribute('aria-busy', 'true');
    expect(status).toHaveAttribute('aria-live', 'polite');
  });

  it('save button carries the nxrm-http-save analytics id', async () => {
    mockedUseHttpForm.mockReturnValue(createHttpFormMock(mockSettings, { isPristine: false }));
    render(<HttpPage />, { wrapper: TestWrapper });
    const saveButton = await screen.findByRole('button', { name: 'Save' });
    expect(saveButton).toHaveAttribute('data-analytics-id', 'nxrm-http-save');
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

  describe('breadcrumbs', () => {
    it('renders Settings breadcrumb that navigates to settings page', async () => {
      render(<HttpPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      });

      // Click Settings breadcrumb navigates to settings page
      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });

    it('renders HTTP as current page breadcrumb', async () => {
      render(<HttpPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        // The current page item is rendered as Text (not a button) with aria-current="page"
        const breadcrumb = screen.getByText('HTTP', { selector: '[aria-current="page"]' });
        expect(breadcrumb).toBeInTheDocument();
      });
    });
  });
});
