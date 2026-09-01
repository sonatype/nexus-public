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

import { OAuth2Page } from '../OAuth2Page';
import * as oauth2Api from '../oauth2Api';
import { ToastProvider } from '../../../../../shared/Toast';
import UIStrings from '../../../../../../../constants/UIStrings';

const SAVE_SUCCESS_TOAST = 'OAuth2 configuration saved successfully';

// Mock the pure API module the form machine invokes for load/save.
jest.mock('../oauth2Api');

const mockedFetch = oauth2Api.fetchOAuth2Config as jest.MockedFunction<typeof oauth2Api.fetchOAuth2Config>;
const mockedSave = oauth2Api.saveOAuth2Config as jest.MockedFunction<typeof oauth2Api.saveOAuth2Config>;

// OAuth2Page imports ExtJS from interface/ExtJS directly, so permission gating
// must be driven through that module (same pattern as RolesPage.test.tsx).
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
  },
}));

const getInterfaceCheckPermission = () => {
  const { ExtJS } = require('../../../../../../../interface/ExtJS');
  return ExtJS.checkPermission;
};

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

const getMockCheckPermission = () => {
  const { ExtJS } = require('@sonatype/nexus-ui-plugin');
  return ExtJS.checkPermission;
};

// ToastProvider is mounted so save-success toasts actually render; useToast
// falls back to a silent no-op when the provider is absent, which would make a
// missing notification indistinguishable from a working one.
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

const mockSettings = {
  clientId: 'client-id',
  clientSecret: 'client-secret',
  idpAuthorizationUrl: 'https://example.com/auth',
  idpLogoutUrl: 'https://example.com/logout',
  idpTokenUrl: 'https://example.com/token',
  idpJwksUrl: 'https://example.com/jwks',
  idpJwsAlgorithm: 'RS256',
  idpJwks: '',
  usernameClaim: 'sub',
  firstNameClaim: 'given_name',
  lastNameClaim: 'family_name',
  emailClaim: 'email',
  groupsClaim: 'groups',
  exactMatchClaims: '',
  authorizationCustomParams: '',
  tokenRequestCustomParams: '',
  useTrustStore: false,
};

describe('OAuth2Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    getMockCheckPermission().mockReturnValue(true);
    getInterfaceCheckPermission().mockReturnValue(true);
    mockedFetch.mockResolvedValue({ ...mockSettings });
    mockedSave.mockResolvedValue(undefined);
  });

  it('renders the page header', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'OAuth2' })).toBeInTheDocument();
    });
  });

  it('renders the page description', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByText(/configure openid connect/i)).toBeInTheDocument();
    });
  });

  it('loads settings on mount', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(mockedFetch).toHaveBeenCalled();
    });
  });

  it.each([
    'Client ID',
    'Client Secret',
    'IDP Authorization URL',
    'IDP Token URL',
    'Username Claim',
    'Groups Claim',
    'JWS Algorithm',
  ])('displays the %s field', async (label) => {
    render(<OAuth2Page />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByText(label)).toBeInTheDocument();
    });
  });

  it('displays save and discard buttons', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /save/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /discard/i })).toBeInTheDocument();
    });
  });

  it('saves settings when Save button is clicked', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Client ID')).toBeInTheDocument();
    });

    const clientIdInput = document.getElementById('settings-input-clientId') as HTMLInputElement;
    fireEvent.change(clientIdInput, { target: { value: 'modified-client' } });

    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(mockedSave).toHaveBeenCalled();
    });
  });

  it('clears dirty state after successful save', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Client ID')).toBeInTheDocument();
    });

    const clientIdInput = document.getElementById('settings-input-clientId') as HTMLInputElement;
    fireEvent.change(clientIdInput, { target: { value: 'modified-client' } });

    const saveButton = screen.getByRole('button', { name: /save/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockedSave).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(saveButton).toBeDisabled();
    });
  });

  it('shows a success notification and stays pristine after a successful save', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Client ID')).toBeInTheDocument();
    });

    const clientIdInput = document.getElementById('settings-input-clientId') as HTMLInputElement;
    fireEvent.change(clientIdInput, { target: { value: 'modified-client' } });

    const saveButton = screen.getByRole('button', { name: /save/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(screen.getByText(SAVE_SUCCESS_TOAST)).toBeInTheDocument();
    });
    // The toast must not come at the cost of the pristine transition: both the
    // notification and the disabled Save button are part of the success signal.
    expect(saveButton).toBeDisabled();
  });

  it('resets form when Discard is confirmed', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Client ID')).toBeInTheDocument();
    });

    const clientIdInput = document.getElementById('settings-input-clientId') as HTMLInputElement;
    fireEvent.change(clientIdInput, { target: { value: 'modified-client' } });
    expect(clientIdInput).toHaveValue('modified-client');

    fireEvent.click(screen.getByRole('button', { name: /discard/i }));

    await waitFor(() => {
      expect(screen.getByText('Unsaved Changes')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /leave/i }));

    await waitFor(() => {
      expect(clientIdInput).toHaveValue('client-id');
    });
  });

  it('handles loading state', () => {
    // Never-resolving load keeps the machine in the loading state.
    mockedFetch.mockReturnValue(new Promise(() => {}));
    render(<OAuth2Page />, { wrapper: TestWrapper });
    expect(screen.getByText(/loading oauth2 configuration/i)).toBeInTheDocument();
  });

  it('surfaces an initial load failure to the user', async () => {
    mockedFetch.mockRejectedValue(new Error('Failed to load OAuth2 settings'));
    render(<OAuth2Page />, { wrapper: TestWrapper });

    // Load failure leaves the form rendered (not stuck on the spinner) with the
    // error surfaced via the shared SettingsForm error handling.
    await waitFor(() => {
      expect(screen.getByText('Failed to load OAuth2 settings')).toBeInTheDocument();
    });
  });

  it('shows an error when save fails', async () => {
    mockedSave.mockRejectedValue(new Error('Failed to save OAuth2 settings'));
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Client ID')).toBeInTheDocument();
    });

    const clientIdInput = document.getElementById('settings-input-clientId') as HTMLInputElement;
    fireEvent.change(clientIdInput, { target: { value: 'modified-client' } });
    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(screen.getByText('Failed to save OAuth2 settings')).toBeInTheDocument();
    });
    // A failed save must not raise the success toast.
    expect(screen.queryByText(SAVE_SUCCESS_TOAST)).not.toBeInTheDocument();
  });

  it('validates required fields before save', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Client ID')).toBeInTheDocument();
    });

    const clientIdInput = document.getElementById('settings-input-clientId') as HTMLInputElement;
    fireEvent.change(clientIdInput, { target: { value: '' } });
    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(screen.getByText(UIStrings.ERROR.FIELD_REQUIRED)).toBeInTheDocument();
    });
  });

  describe('breadcrumbs', () => {
    it('renders Settings breadcrumb that navigates to settings page', async () => {
      render(<OAuth2Page />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      });

      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });

    it('renders OAuth2 as current page breadcrumb', async () => {
      render(<OAuth2Page />, { wrapper: TestWrapper });

      await waitFor(() => {
        const breadcrumb = screen.getByText('OAuth2', { selector: '[aria-current="page"]' });
        expect(breadcrumb).toBeInTheDocument();
      });
    });
  });
});

// NEXUS-54266: truststore control + permission gating.
describe('truststore control', () => {
  it('renders the checkbox checked when the loaded config has useTrustStore=true', async () => {
    mockedFetch.mockResolvedValue({ ...mockSettings, useTrustStore: true });
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(document.querySelector('input[name="useTrustStore"]')).toBeInTheDocument();
    });
    expect((document.querySelector('input[name="useTrustStore"]') as HTMLInputElement).checked).toBe(true);
  });

  it('enables the checkbox and View Certificate when the token URL is https', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(document.querySelector('input[name="useTrustStore"]')).toBeInTheDocument();
    });
    const cb = document.querySelector('input[name="useTrustStore"]') as HTMLInputElement;
    expect(cb.disabled).toBe(false);
    expect(screen.getByRole('button', { name: /view certificate/i })).toBeEnabled();
  });

  it('disables the checkbox and View Certificate when the token URL is not secure', async () => {
    mockedFetch.mockResolvedValue({ ...mockSettings, idpTokenUrl: 'http://example.com/token' });
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(document.querySelector('input[name="useTrustStore"]')).toBeInTheDocument();
    });
    const cb = document.querySelector('input[name="useTrustStore"]') as HTMLInputElement;
    expect(cb.disabled).toBe(true);
    expect(screen.getByRole('button', { name: /view certificate/i })).toBeDisabled();
  });

  it('disables View Certificate without the ssl-truststore:read permission', async () => {
    getInterfaceCheckPermission().mockImplementation((p: string) => p !== 'nexus:ssl-truststore:read');
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /view certificate/i })).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /view certificate/i })).toBeDisabled();
  });

  it('hides Save/Discard and disables the checkbox for a read-only user', async () => {
    getInterfaceCheckPermission().mockImplementation((p: string) => p !== 'nexus:settings:update');
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(document.querySelector('input[name="useTrustStore"]')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('form-submit')).not.toBeInTheDocument();
    expect(screen.queryByTestId('form-cancel')).not.toBeInTheDocument();
    expect((document.querySelector('input[name="useTrustStore"]') as HTMLInputElement).disabled).toBe(true);
  });
});
