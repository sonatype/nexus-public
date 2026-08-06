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
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { IqServerPage } from '../IqServerPage';
import * as useIqServerFormModule from '../useIqServerForm';

// Handle both old and new userEvent API
const getUser = () => (typeof (userEvent as any).setup === 'function' ? (userEvent as any).setup() : userEvent);

// Mock the form hook
jest.mock('../useIqServerForm');

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: { go: jest.fn() },
  }),
}));

const mockedUseIqServerForm = useIqServerFormModule.useIqServerForm as jest.MockedFunction<typeof useIqServerFormModule.useIqServerForm>;

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

// Factory function for creating mock form hook return values
function makeForm(overrides: Record<string, any> = {}) {
  const data = {
    enabled: true,
    url: 'https://iq.example.com',
    authenticationType: 'USER',
    username: 'admin',
    password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
    useTrustStoreForUrl: false,
    timeoutSeconds: null,
    properties: [],
    showLink: true,
    ...(overrides.data || {}),
  };

  const validationErrors = overrides.validationErrors || {};
  const touched = overrides.touched || {};

  // Start with overrides to allow caller-provided functions, then fall back to defaults
  const handleFieldChange = overrides.handleFieldChange ?? jest.fn();
  const handleUrlChange = overrides.handleUrlChange ?? jest.fn();

  // Build result object with defaults, then apply overrides (excluding already-processed keys)
  const result: Record<string, any> = {
    data,
    field: (name: string) => ({
      name,
      value: String((data as any)[name] ?? ''),
      error: touched[name] ? (validationErrors as any)[name] ?? undefined : undefined,
      onChange: (value: string) => handleFieldChange(name, value),
      onBlur: jest.fn(),
    }),
    checkbox: (name: string) => ({
      name,
      checked: Boolean((data as any)[name]),
      error: touched[name] ? (validationErrors as any)[name] ?? undefined : undefined,
      onChange: (checked: boolean) => handleFieldChange(name, checked),
    }),
    select: (name: string) => ({
      name,
      value: String((data as any)[name] ?? ''),
      error: touched[name] ? (validationErrors as any)[name] ?? undefined : undefined,
      onChange: (value: string) => handleFieldChange(name, value),
      onBlur: jest.fn(),
    }),
    validationErrors,
    touched,
    hasValidationErrors: false,
    isLoading: false,
    isSaving: false,
    isPristine: true,
    saveError: null,
    handleFieldChange,
    handleUrlChange,
    verify: jest.fn(),
    connectionStatus: 'idle',
    connectionMessage: undefined,
    verificationResult: null,
    capabilities: { hasFirewall: false, hasLifecycle: false, connected: false, url: null },
    isCloud: false,
    canUpdate: true,
    canOpenDashboard: true,
    dashboardUrl: data.url,
    submit: jest.fn(),
    reset: jest.fn(),
    clearSaveError: jest.fn(),
    setProperties: jest.fn(),
    propertyValidations: [],
    hasPropertyErrors: false,
    showAllValidation: false,
    showClearAllConfirm: false,
    requestClearAllProperties: jest.fn(),
    confirmClearAllProperties: jest.fn(),
    cancelClearAllProperties: jest.fn(),
    propertiesDroppedLineCount: 0,
    showPropertiesDroppedWarning: false,
    dismissPropertiesDroppedWarning: jest.fn(),
  };

  // Apply remaining overrides (skip keys we've already processed)
  const processedKeys = ['data', 'validationErrors', 'touched', 'handleFieldChange', 'handleUrlChange'];
  for (const key of Object.keys(overrides)) {
    if (!processedKeys.includes(key)) {
      result[key] = overrides[key];
    }
  }

  return result;
}

describe('IqServerPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset ExtJS.checkPermission to return true for editable form
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(true);
    ExtJS.state().getValue.mockImplementation((key: string) => {
      if (key === 'user') return { id: 'admin', name: 'admin', administrator: true };
      return undefined;
    });

    mockedUseIqServerForm.mockReturnValue(makeForm());
  });

  it('renders loading state initially', () => {
    mockedUseIqServerForm.mockReturnValue(makeForm({ isLoading: true }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    expect(screen.getByText('Loading IQ Server settings...')).toBeInTheDocument();
  });

  it('renders the page header', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'IQ Server' })).toBeInTheDocument();
    });
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

    mockedUseIqServerForm.mockReturnValue(makeForm({ isCloud: true }));

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

  it('dismissing the save-error alert calls clearSaveError', async () => {
    const clearSaveError = jest.fn();
    mockedUseIqServerForm.mockReturnValue(makeForm({ saveError: 'Save failed', clearSaveError }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    const dismissButton = await waitFor(() => screen.getByRole('button', { name: 'Dismiss' }));
    fireEvent.click(dismissButton);

    expect(clearSaveError).toHaveBeenCalledTimes(1);
  });

  it('shows authentication fields for USER type', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Username/)).toBeInTheDocument();
    });

    expect(screen.getByLabelText(/Password/)).toBeInTheDocument();
  });

  it('hides authentication fields for PKI type', async () => {
    mockedUseIqServerForm.mockReturnValue(makeForm({
      data: {
        enabled: true,
        url: 'https://iq.example.com',
        authenticationType: 'PKI',
        username: '',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: [],
        showLink: true,
      },
    }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('select-authenticationType')).toBeInTheDocument();
    });

    expect(screen.queryByLabelText(/Username/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Password/)).not.toBeInTheDocument();
  });

  it('saves changes when Save button is clicked', async () => {
    const mockSubmit = jest.fn();
    mockedUseIqServerForm.mockReturnValue(makeForm({
      isPristine: false,
      submit: mockSubmit,
    }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Username/)).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: 'Save' });
    await waitFor(() => {
      expect(saveButton).not.toBeDisabled();
    });

    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockSubmit).toHaveBeenCalled();
    });
  });

  it('discards changes when Discard button is clicked', async () => {
    const mockReset = jest.fn();
    mockedUseIqServerForm.mockReturnValue(makeForm({
      isPristine: false,
      reset: mockReset,
    }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Username/)).toBeInTheDocument();
    });

    const discardButton = screen.getByRole('button', { name: 'Discard' });
    fireEvent.click(discardButton);

    // SettingsForm has confirmDiscard=true by default, so click "Leave" in confirmation dialog
    const leaveButton = await screen.findByRole('button', { name: /leave/i });
    fireEvent.click(leaveButton);

    await waitFor(() => {
      expect(mockReset).toHaveBeenCalled();
    });
  });

  it('verifies connection successfully', async () => {
    const mockVerify = jest.fn();
    mockedUseIqServerForm.mockReturnValue(makeForm({
      verify: mockVerify,
      connectionStatus: 'connected',
      connectionMessage: 'Connected to IQ Server (v1.142.0)',
      verificationResult: { success: true, reason: '5' },
    }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Test Connection' })).toBeInTheDocument();
    });

    const verifyButton = screen.getByRole('button', { name: 'Test Connection' });
    fireEvent.click(verifyButton);

    await waitFor(() => {
      expect(mockVerify).toHaveBeenCalled();
    });

    // Connected state shows the success message
    await waitFor(() => {
      expect(screen.getByText(/Connection successful/)).toBeInTheDocument();
    });
  });

  it('opens full-screen application list when View application list is clicked', async () => {
    mockedUseIqServerForm.mockReturnValue(makeForm({
      verificationResult: { success: true, reason: 'MyApp, BackendService, FrontendApp' },
      connectionStatus: 'connected',
      connectionMessage: 'Connected to IQ Server',
    }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Test Connection' })).toBeInTheDocument();
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
    mockedUseIqServerForm.mockReturnValue(makeForm({
      verificationResult: { success: true, reason: 'MyApp, BackendService, FrontendApp' },
      connectionStatus: 'connected',
      connectionMessage: 'Connected to IQ Server',
    }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Test Connection' })).toBeInTheDocument();
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
    mockedUseIqServerForm.mockReturnValue(makeForm({
      connectionStatus: 'failed',
      connectionMessage: 'Connection failed: Authentication failed',
      verificationResult: { success: false, reason: 'Authentication failed' },
    }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText(/Connection failed: Authentication failed/)).toBeInTheDocument();
    });
  });

  it('shows validation error for invalid URL', async () => {
    mockedUseIqServerForm.mockReturnValue(makeForm({
      validationErrors: { url: 'Please enter a valid URL' },
      touched: { url: true },
      hasValidationErrors: true,
    }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/IQ Server URL/)).toBeInTheDocument();
    });

    // Should show error
    expect(screen.getByText('Please enter a valid URL')).toBeInTheDocument();
  });

  it('shows read-only view when user lacks update permission', async () => {
    const { ExtJS } = require('../../../../../../../interface/ExtJS');
    ExtJS.checkPermission.mockReturnValue(false);

    mockedUseIqServerForm.mockReturnValue(makeForm({ canUpdate: false }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'IQ Server' })).toBeInTheDocument();
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
    expect(screen.getByRole('button', { name: /View Certificate/ })).toBeInTheDocument();
  });

  it('hides trust store option for HTTP URLs', async () => {
    mockedUseIqServerForm.mockReturnValue(makeForm({
      data: {
        enabled: true,
        url: 'http://iq.example.com',
        authenticationType: 'USER',
        username: 'admin',
        password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: [],
        showLink: true,
      },
    }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/IQ Server URL/)).toBeInTheDocument();
    });

    expect(screen.queryByLabelText(/Use Nexus Repository Trust Store/)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /View Certificate/ })).not.toBeInTheDocument();
  });

  it('displays help section', async () => {
    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('About IQ Server')).toBeInTheDocument();
    });

    expect(screen.getByText('documentation')).toHaveAttribute(
      'href',
      'http://links.sonatype.com/products/nxrm3/browse/lc-learn'
    );
  });

  it('toggles enabled checkbox', async () => {
    const mockHandleFieldChange = jest.fn();
    mockedUseIqServerForm.mockReturnValue(makeForm({
      handleFieldChange: mockHandleFieldChange,
    }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Enable IQ Server/)).toBeInTheDocument();
    });

    const checkbox = screen.getByLabelText(/Enable IQ Server/);
    expect(checkbox).toBeChecked();

    fireEvent.click(checkbox);
    expect(mockHandleFieldChange).toHaveBeenCalled();
  });

  it('changes authentication type and hides username/password fields', async () => {
    mockedUseIqServerForm.mockReturnValue(makeForm({
      data: {
        enabled: true,
        url: 'https://iq.example.com',
        authenticationType: 'PKI',
        username: '',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: [],
        showLink: true,
      },
    }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('select-authenticationType')).toBeInTheDocument();
    });

    // Username and password fields should not be present for PKI
    expect(screen.queryByLabelText(/Username/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Password/)).not.toBeInTheDocument();
  });

  it('dispatches nx-sidebar-toggle event when app list opens and closes (bchz)', async () => {
    mockedUseIqServerForm.mockReturnValue(makeForm({
      verificationResult: { success: true, reason: 'MyApp, BackendService' },
      connectionStatus: 'connected',
      connectionMessage: 'Connected to IQ Server',
    }));

    const events: any[] = [];
    window.addEventListener('nx-sidebar-toggle', (e) => events.push((e as CustomEvent).detail));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => screen.getByRole('button', { name: /View application list/i }));
    fireEvent.click(screen.getByRole('button', { name: /View application list/i }));
    await waitFor(() => expect(screen.getByTestId('iq-application-list-modal')).toBeInTheDocument());

    // Sidebar collapses on open
    expect(events.some((d) => d.open === false)).toBe(true);

    fireEvent.click(screen.getByRole('button', { name: 'Close application list' }));
    await waitFor(() => expect(screen.queryByTestId('iq-application-list-modal')).not.toBeInTheDocument());
  });

  it('closes app list on Escape key (bchz)', async () => {
    mockedUseIqServerForm.mockReturnValue(makeForm({
      verificationResult: { success: true, reason: 'MyApp, BackendService' },
      connectionStatus: 'connected',
      connectionMessage: 'Connected to IQ Server',
    }));

    render(<IqServerPage />, { wrapper: TestWrapper });

    await waitFor(() => screen.getByRole('button', { name: /View application list/i }));
    fireEvent.click(screen.getByRole('button', { name: /View application list/i }));
    await waitFor(() => expect(screen.getByTestId('iq-application-list-modal')).toBeInTheDocument());

    fireEvent.keyDown(window, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByTestId('iq-application-list-modal')).not.toBeInTheDocument());
  });

  describe('Breadcrumb navigation', () => {
    it('renders breadcrumbs with Settings link', async () => {
      render(<IqServerPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      });
    });

    it('renders IQ Server as current page in breadcrumbs', async () => {
      const { container } = render(<IqServerPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'IQ Server' })).toBeInTheDocument();
      });

      // IQ Server should be the current page (span with aria-current)
      const currentBreadcrumb = container.querySelector('[aria-current="page"]');
      expect(currentBreadcrumb).toBeInTheDocument();
      expect(currentBreadcrumb?.textContent).toBe('IQ Server');
    });

    it('navigates to Settings when Settings breadcrumb is clicked', async () => {
      render(<IqServerPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      });

      const originalHash = window.location.hash;
      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
      window.location.hash = originalHash;
    });
  });

  describe('Properties editor', () => {
    it('renders property rows from data.properties', async () => {
      const user = getUser();
      mockedUseIqServerForm.mockReturnValue(makeForm({
        data: { properties: [{ id: '1', name: 'proxy.host', value: 'proxy.example.com' }] },
      }));

      render(<IqServerPage />, { wrapper: TestWrapper });
      await waitFor(() => expect(screen.getByRole('heading', { name: 'IQ Server' })).toBeInTheDocument());
      await user.click(screen.getByRole('button', { name: /Advanced Settings/i }));

      expect(screen.getByDisplayValue('proxy.host')).toBeInTheDocument();
      expect(screen.getByDisplayValue('proxy.example.com')).toBeInTheDocument();
    });

    it('calls setProperties when Add Parameter is clicked', async () => {
      const user = getUser();
      const setProperties = jest.fn();
      mockedUseIqServerForm.mockReturnValue(makeForm({
        data: { properties: [{ id: '1', name: 'proxy.host', value: 'x' }] },
        setProperties,
      }));

      render(<IqServerPage />, { wrapper: TestWrapper });
      await waitFor(() => expect(screen.getByRole('heading', { name: 'IQ Server' })).toBeInTheDocument());
      await user.click(screen.getByRole('button', { name: /Advanced Settings/i }));
      await user.click(screen.getByRole('button', { name: /add parameter/i }));

      expect(setProperties).toHaveBeenCalled();
    });

    it('shows the dropped-lines warning and dismisses it', async () => {
      const dismissPropertiesDroppedWarning = jest.fn();
      mockedUseIqServerForm.mockReturnValue(makeForm({
        showPropertiesDroppedWarning: true,
        propertiesDroppedLineCount: 2,
        dismissPropertiesDroppedWarning,
      }));

      render(<IqServerPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByText(/2 lines in the existing Properties configuration/i)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }));
      expect(dismissPropertiesDroppedWarning).toHaveBeenCalledTimes(1);
    });

    it('does not show the dropped-lines warning when there is nothing dropped', async () => {
      render(<IqServerPage />, { wrapper: TestWrapper });

      await waitFor(() => expect(screen.getByRole('heading', { name: 'IQ Server' })).toBeInTheDocument());

      expect(screen.queryByText(/weren.t recognized/i)).not.toBeInTheDocument();
    });

    it('shows the clear-all confirm banner and wires cancel/confirm', async () => {
      const user = getUser();
      const cancelClearAllProperties = jest.fn();
      const confirmClearAllProperties = jest.fn();
      mockedUseIqServerForm.mockReturnValue(makeForm({
        data: { properties: [{ id: '1', name: 'proxy.host', value: 'x' }] },
        showClearAllConfirm: true,
        cancelClearAllProperties,
        confirmClearAllProperties,
      }));

      render(<IqServerPage />, { wrapper: TestWrapper });
      await waitFor(() => expect(screen.getByRole('heading', { name: 'IQ Server' })).toBeInTheDocument());
      await user.click(screen.getByRole('button', { name: /Advanced Settings/i }));

      expect(screen.getByText(/clear all properties/i)).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: 'Cancel' }));
      expect(cancelClearAllProperties).toHaveBeenCalled();

      // Click the "Clear All" button in the confirmation banner (danger variant)
      // We select by the danger variant which is unique to the confirm banner button
      const clearAllButtons = screen.getAllByRole('button', { name: 'Clear All' });
      // Find the button with the danger variant (confirmation banner)
      const confirmButton = clearAllButtons.find(btn => btn.className.includes('settings-button--danger'));
      await user.click(confirmButton!);
      expect(confirmClearAllProperties).toHaveBeenCalled();
    });

    it('disables the Save button when hasPropertyErrors is true', async () => {
      mockedUseIqServerForm.mockReturnValue(makeForm({ hasPropertyErrors: true }));

      render(<IqServerPage />, { wrapper: TestWrapper });

      const saveButton = await waitFor(() => screen.getByRole('button', { name: 'Save' }));
      expect(saveButton).toBeDisabled();
    });
  });

});
