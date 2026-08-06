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
import * as crowdApi from '../crowdApi';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock the pure API module the settings machine invokes.
jest.mock('../crowdApi');

const mockedFetch = crowdApi.fetchCrowdConfig as jest.MockedFunction<typeof crowdApi.fetchCrowdConfig>;
const mockedSave = crowdApi.saveCrowdConfig as jest.MockedFunction<typeof crowdApi.saveCrowdConfig>;
const mockedVerify = crowdApi.verifyCrowdConnection as jest.MockedFunction<typeof crowdApi.verifyCrowdConnection>;
const mockedClear = crowdApi.clearCrowdCache as jest.MockedFunction<typeof crowdApi.clearCrowdCache>;

jest.mock('@sonatype/nexus-ui-plugin', () => {
  const { createNexusUiPluginMock } = jest.requireActual('../../../../../../../../__jest__/mocks/nexusUiPluginMock');
  return createNexusUiPluginMock();
});

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
  url: '',
  timeout: 30,
  useTrustStoreForUrl: false,
};

const validSettings = {
  enabled: true,
  realmActive: false,
  applicationName: 'nexus',
  applicationPassword: 'secret',
  url: 'http://crowd.example.com',
  timeout: 30,
  useTrustStoreForUrl: false,
};

describe('CrowdPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (global as any).NX.Permissions.check.mockReturnValue(true);
    mockedFetch.mockResolvedValue({ ...mockSettings });
    mockedSave.mockResolvedValue(undefined);
    mockedVerify.mockResolvedValue(undefined);
    mockedClear.mockResolvedValue(undefined);
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
      expect(mockedFetch).toHaveBeenCalled();
    });
  });

  it.each([
    ['checkbox-enabled'],
    ['checkbox-realmActive'],
    ['input-applicationName'],
    ['password-applicationPassword'],
    ['input-url'],
    ['input-timeout'],
  ])('displays the %s field', async (testId) => {
    render(<CrowdPage />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByTestId(testId)).toBeInTheDocument();
    });
  });

  it('displays use trust store toggle when URL is https', async () => {
    mockedFetch.mockResolvedValue({ ...mockSettings, url: 'https://crowd.example.com' });
    render(<CrowdPage />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByTestId('checkbox-useTrustStoreForUrl')).toBeInTheDocument();
    });
  });

  it('displays save, discard, verify and clear-cache buttons', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /save/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /discard/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /verify.*connection/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /clear.*cache/i })).toBeInTheDocument();
    });
  });

  it('saves settings when Save button is clicked', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('input-applicationName'), { target: { value: 'nexus-modified' } });

    const saveButton = screen.getByRole('button', { name: /save/i });
    await waitFor(() => {
      expect(saveButton).not.toBeDisabled();
    });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockedSave).toHaveBeenCalled();
    });
  });

  it('verifies connection when Verify Connection button is clicked', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /verify.*connection/i }));

    await waitFor(() => {
      expect(mockedVerify).toHaveBeenCalled();
    });
  });

  it('shows success message after successful connection verification', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /verify.*connection/i }));

    await waitFor(() => {
      expect(screen.getByText(/Connection to Crowd server verified/i)).toBeInTheDocument();
    });
  });

  it('handles failed connection verification', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings });
    mockedVerify.mockRejectedValue(new Error('Connection refused'));
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /verify.*connection/i }));

    await waitFor(() => {
      expect(mockedVerify).toHaveBeenCalled();
      expect(screen.getByText('Connection refused')).toBeInTheDocument();
    });
  });

  it('clears cache when Clear Cache button is clicked', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /clear.*cache/i }));

    await waitFor(() => {
      expect(mockedClear).toHaveBeenCalled();
    });
  });

  it('shows success message after clearing cache', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /clear.*cache/i }));

    await waitFor(() => {
      expect(screen.getByText(/cache.*cleared/i)).toBeInTheDocument();
    });
  });

  it('resets form to the loaded configuration when Discard is confirmed', async () => {
    // Load a non-empty configuration so the assertion proves Discard restores
    // the loaded value (not merely that it clears the field).
    mockedFetch.mockResolvedValue({ ...validSettings, applicationName: 'nexus' });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toHaveValue('nexus');
    });

    const appNameInput = screen.getByTestId('input-applicationName');
    fireEvent.change(appNameInput, { target: { value: 'modified-app' } });
    expect(appNameInput).toHaveValue('modified-app');

    fireEvent.click(screen.getByTestId('form-cancel'));

    await waitFor(() => {
      expect(screen.getByText('Unsaved Changes')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /leave/i }));

    await waitFor(() => {
      // Restored to the loaded pristine value, not blanked.
      expect(appNameInput).toHaveValue('nexus');
    });
  });

  it('handles loading state', () => {
    mockedFetch.mockReturnValue(new Promise(() => {}));
    render(<CrowdPage />, { wrapper: TestWrapper });
    expect(screen.getByText(/Loading Crowd configuration/i)).toBeInTheDocument();
    const container = screen.getByTestId('crowd-page');
    expect(container).toHaveAttribute('data-loading', 'true');
    expect(container).toHaveAttribute('aria-busy', 'true');
    expect(container).toHaveAttribute('aria-live', 'polite');
  });

  it('shows an error banner when an operation fails', async () => {
    mockedClear.mockRejectedValue(new Error('Failed to clear Crowd cache'));
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /clear.*cache/i }));

    await waitFor(() => {
      expect(screen.getByText('Failed to clear Crowd cache')).toBeInTheDocument();
    });
  });

  it('enables save button when form is dirty AND valid', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings, enabled: false });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    const saveButton = screen.getByRole('button', { name: /save/i });
    expect(saveButton).toBeDisabled();

    fireEvent.click(screen.getByTestId('checkbox-enabled'));

    await waitFor(() => {
      expect(saveButton).not.toBeDisabled();
    });
  });

  it('disables Save and Verify buttons immediately when URL is blank on load', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-url')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /save/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /verify.*connection/i })).toBeDisabled();
  });

  it('does not show field errors on fresh load before user interaction', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-url')).toBeInTheDocument();
    });

    // Save/Verify are still disabled (raw validity), but no inline errors yet.
    expect(screen.queryByText('Crowd server URL is required')).not.toBeInTheDocument();
    expect(screen.queryByText('Application name is required')).not.toBeInTheDocument();
    expect(screen.queryByText('Application password is required')).not.toBeInTheDocument();
  });

  it('shows a field error after the field is blurred', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-url')).toBeInTheDocument();
    });

    fireEvent.blur(screen.getByTestId('input-url'));

    await waitFor(() => {
      expect(screen.getByText('Crowd server URL is required')).toBeInTheDocument();
    });
    // Other fields not yet touched — still silent.
    expect(screen.queryByText('Application name is required')).not.toBeInTheDocument();
  });

  it('reveals all field errors after a save attempt on a dirty but invalid form', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-url')).toBeInTheDocument();
    });

    // Make the form dirty without fixing the invalid fields (toggle a checkbox).
    fireEvent.click(screen.getByTestId('checkbox-enabled'));

    const saveButton = screen.getByRole('button', { name: /save/i });
    await waitFor(() => expect(saveButton).not.toBeDisabled());
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(screen.getByText('Crowd server URL is required')).toBeInTheDocument();
      expect(screen.getByText('Application name is required')).toBeInTheDocument();
      expect(screen.getByText('Application password is required')).toBeInTheDocument();
    });
    // The invalid form is never saved.
    expect(mockedSave).not.toHaveBeenCalled();
  });

  it('shows URL required error and disables Verify when URL is cleared', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-url')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('input-url'), { target: { value: '' } });

    await waitFor(() => {
      // URL field was changed (touched), so its error is visible.
      expect(screen.getByText('Crowd server URL is required')).toBeInTheDocument();
      // Verify is disabled because the form is invalid.
      expect(screen.getByRole('button', { name: /verify.*connection/i })).toBeDisabled();
      // Save is enabled (form is dirty) — the user can click it to reveal all errors.
      expect(screen.getByRole('button', { name: /save/i })).not.toBeDisabled();
    });
  });

  it('discard resets touched state so field errors are hidden again', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-url')).toBeInTheDocument();
    });

    // Touch the URL field so its error becomes visible.
    fireEvent.blur(screen.getByTestId('input-url'));
    await waitFor(() => {
      expect(screen.getByText('Crowd server URL is required')).toBeInTheDocument();
    });

    // Make a dirty change so the discard button is active.
    fireEvent.click(screen.getByTestId('checkbox-enabled'));
    fireEvent.click(screen.getByTestId('form-cancel'));

    await waitFor(() => {
      expect(screen.getByText('Unsaved Changes')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /leave/i }));

    await waitFor(() => {
      // Touched state was reset — error hidden again even though URL is still blank.
      expect(screen.queryByText('Crowd server URL is required')).not.toBeInTheDocument();
    });
    // Save still disabled because the form is still invalid (and now pristine).
    expect(screen.getByRole('button', { name: /save/i })).toBeDisabled();
  });

  it('shows read-only view when user lacks all permissions', async () => {
    (global as any).NX.Permissions.check.mockReturnValue(false);

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    expect(screen.getByTestId('input-applicationName')).toBeDisabled();
    expect(screen.queryByRole('button', { name: /save/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /discard/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /verify.*connection/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /clear.*cache/i })).not.toBeInTheDocument();
  });

  it('shows verify connection but not clear cache when user has read but not update permission', async () => {
    (global as any).NX.Permissions.check.mockImplementation((permission: string) => {
      return permission === 'nexus:crowd:read';
    });
    mockedFetch.mockResolvedValue({ ...validSettings });

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    expect(screen.queryByRole('button', { name: /save/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /discard/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /verify.*connection/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /clear.*cache/i })).not.toBeInTheDocument();
  });

  it('shows permission warning when user lacks update permission', async () => {
    (global as any).NX.Permissions.check.mockReturnValue(false);

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText(/don't have permission to edit/i)).toBeInTheDocument();
    });
  });

  it('renders analytics IDs on all actionable elements', async () => {
    mockedFetch.mockResolvedValue({ ...mockSettings, url: 'https://crowd.example.com' });

    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    expect(document.querySelector('[data-analytics-id="nxrm-crowd-save"]')).toBeInTheDocument();
    expect(document.querySelector('[data-analytics-id="nxrm-crowd-discard"]')).toBeInTheDocument();
    expect(document.querySelector('[data-analytics-id="nxrm-crowd-verify-connection"]')).toBeInTheDocument();
    expect(document.querySelector('[data-analytics-id="nxrm-crowd-clear-cache"]')).toBeInTheDocument();
    expect(document.querySelector('[data-analytics-id="nxrm-crowd-toggle-enabled"]')).toBeInTheDocument();
    expect(document.querySelector('[data-analytics-id="nxrm-crowd-toggle-realm-active"]')).toBeInTheDocument();
    expect(document.querySelector('[data-analytics-id="nxrm-crowd-toggle-truststore"]')).toBeInTheDocument();
  });

  it('renders help section with documentation link', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('About Atlassian Crowd')).toBeInTheDocument();
    });

    const docLink = screen.getByRole('link', { name: /view crowd documentation/i });
    expect(docLink).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm3/docs/crowd');
  });

  it('has data-mode attribute set to edit', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('crowd-form')).toHaveAttribute('data-mode', 'edit');
    });
  });

  it('shows form state as dirty when changes are made', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('crowd-form')).toBeInTheDocument();
    });

    const form = screen.getByTestId('crowd-form');
    expect(form).toHaveAttribute('data-dirty', 'false');

    fireEvent.click(screen.getByTestId('checkbox-enabled'));

    await waitFor(() => {
      expect(form).toHaveAttribute('data-dirty', 'true');
    });
  });

  it('validates URL format', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings, url: '' });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-url')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('input-url'), { target: { value: 'not-a-valid-url' } });

    await waitFor(() => {
      expect(screen.getByText('URL is not valid')).toBeInTheDocument();
    });
  });

  it('validates timeout range (1-3600)', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-timeout')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('input-timeout'), { target: { value: '5000' } });

    await waitFor(() => {
      expect(screen.getByText('Timeout must be between 1 and 3600 seconds')).toBeInTheDocument();
    });
  });

  it('disables Save on pristine load with invalid config, enables it once the form is dirty', async () => {
    mockedFetch.mockResolvedValue({ ...mockSettings, applicationName: '', applicationPassword: '' });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    // Pristine: Save is disabled.
    const saveButton = screen.getByRole('button', { name: /save/i });
    expect(saveButton).toBeDisabled();

    // After a change the form is dirty — Save enables so the user can click to reveal all errors.
    fireEvent.click(screen.getByTestId('checkbox-enabled'));
    await waitFor(() => expect(saveButton).not.toBeDisabled());
  });

  it('timeout input renders empty when the API returns null', async () => {
    mockedFetch.mockResolvedValue({ ...mockSettings, timeout: null as unknown as undefined });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-timeout')).toBeInTheDocument();
    });

    // Blank, not the string "null".
    expect(screen.getByTestId('input-timeout')).toHaveDisplayValue('');
  });

  it('timeout input shows non-numeric text and a validation error (not NaN)', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-timeout')).toBeInTheDocument();
    });

    const timeoutInput = screen.getByTestId('input-timeout');
    fireEvent.change(timeoutInput, { target: { value: 'abc' } });

    // Input shows what the user typed (not "NaN").
    expect(timeoutInput).toHaveDisplayValue('abc');
    await waitFor(() => {
      expect(screen.getByText('Timeout must be a number')).toBeInTheDocument();
    });
  });

  it('timeout input can be cleared after non-numeric entry', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-timeout')).toBeInTheDocument();
    });

    const timeoutInput = screen.getByTestId('input-timeout');
    fireEvent.change(timeoutInput, { target: { value: 'abc' } });
    fireEvent.change(timeoutInput, { target: { value: '' } });

    // Clearable — not stuck at "abc" or "NaN".
    expect(timeoutInput).toHaveDisplayValue('');
  });

  it('form is dirty when non-numeric text replaces a previously loaded timeout value', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings, timeout: 30 });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-timeout')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('input-timeout'), { target: { value: 'abc' } });

    await waitFor(() => {
      expect(screen.getByTestId('crowd-form')).toHaveAttribute('data-dirty', 'true');
    });
  });

  it('disables Verify Connection button immediately on load when required fields are empty', async () => {
    mockedFetch.mockResolvedValue({ ...mockSettings, applicationName: '', applicationPassword: '' });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /verify.*connection/i })).toBeDisabled();
  });

  it('disables Verify Connection button reactively when a required field is cleared', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /verify.*connection/i })).not.toBeDisabled();
    });

    fireEvent.change(screen.getByTestId('input-applicationName'), { target: { value: '' } });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /verify.*connection/i })).toBeDisabled();
    });
  });

  it('shows a distinct "not a number" message for non-numeric timeout input', async () => {
    mockedFetch.mockResolvedValue({ ...validSettings });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-timeout')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('input-timeout'), { target: { value: 'abc' } });

    await waitFor(() => {
      expect(screen.getByText('Timeout must be a number')).toBeInTheDocument();
      expect(screen.queryByText('Timeout must be between 1 and 3600 seconds')).not.toBeInTheDocument();
    });
  });

  it('renders realmActive checkbox description with a link to the Realms page', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('checkbox-realmActive')).toBeInTheDocument();
    });

    expect(screen.getByRole('link', { name: /realms/i })).toHaveAttribute(
      'href',
      '#preview/admin/security/realms'
    );
  });

  it('renders truststore checkbox description with a link to the SSL Certificates page', async () => {
    mockedFetch.mockResolvedValue({ ...mockSettings, url: 'https://crowd.example.com' });
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('checkbox-useTrustStoreForUrl')).toBeInTheDocument();
    });

    expect(screen.getByRole('link', { name: /configure.*truststore/i })).toHaveAttribute(
      'href',
      '#preview/admin/security/sslcertificates'
    );
  });

  it('shows an aggregate validation error banner when the form is dirty with validation errors', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('checkbox-enabled'));

    await waitFor(() => {
      expect(screen.getByText(/validation error/i)).toBeInTheDocument();
    });
  });

  it('hides the aggregate validation error banner once all errors are resolved', async () => {
    render(<CrowdPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('input-applicationName')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('checkbox-enabled'));

    await waitFor(() => {
      expect(screen.getByText(/validation error/i)).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('input-url'), { target: { value: 'http://crowd.example.com' } });
    fireEvent.change(screen.getByTestId('input-applicationName'), { target: { value: 'nexus' } });
    fireEvent.change(screen.getByTestId('password-applicationPassword'), { target: { value: 'secret' } });

    await waitFor(() => {
      expect(screen.queryByText(/validation error/i)).not.toBeInTheDocument();
    });
  });
});
