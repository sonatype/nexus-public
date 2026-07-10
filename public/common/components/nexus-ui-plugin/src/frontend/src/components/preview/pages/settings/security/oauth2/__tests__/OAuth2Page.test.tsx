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
import * as useOAuth2ApiModule from '../useOAuth2Api';

// Mock the API hook
jest.mock('../useOAuth2Api');

const mockedUseOAuth2Api = useOAuth2ApiModule.useOAuth2Api as jest.MockedFunction<
  typeof useOAuth2ApiModule.useOAuth2Api
>;

const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

// Mock the REST API from @/utils/api directly
jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
    post: (...args: unknown[]) => mockRestClient.post(...args),
    put: (...args: unknown[]) => mockRestClient.put(...args),
    delete: (...args: unknown[]) => mockRestClient.delete(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
    status: err?.response?.status,
  })),
}));

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
  return <Theme>{children}</Theme>;
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
};

describe('OAuth2Page', () => {
  const mockFetchConfig = jest.fn();
  const mockSaveConfig = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    getMockCheckPermission().mockReturnValue(true);
    mockedUseOAuth2Api.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchConfig: mockFetchConfig.mockResolvedValue(mockSettings),
      saveConfig: mockSaveConfig.mockResolvedValue({}),
    });
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
      expect(mockFetchConfig).toHaveBeenCalled();
    });
  });

  it('displays client ID field', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Client ID')).toBeInTheDocument();
    });
  });

  it('displays client secret field', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Client Secret')).toBeInTheDocument();
    });
  });

  it('displays IDP authorization URL field', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('IDP Authorization URL')).toBeInTheDocument();
    });
  });

  it('displays IDP token URL field', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('IDP Token URL')).toBeInTheDocument();
    });
  });

  it('displays username claim field', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Username Claim')).toBeInTheDocument();
    });
  });

  it('displays groups claim field', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Groups Claim')).toBeInTheDocument();
    });
  });

  it('displays save button', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /save/i })).toBeInTheDocument();
    });
  });

  it('displays discard button', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /discard/i })).toBeInTheDocument();
    });
  });

  it('saves settings when Save button is clicked', async () => {
    mockedUseOAuth2Api.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchConfig: mockFetchConfig.mockResolvedValue({
        ...mockSettings,
        clientId: 'test-client',
        clientSecret: 'test-secret',
        idpAuthorizationUrl: 'https://example.com/auth',
        idpLogoutUrl: 'https://example.com/logout',
        idpTokenUrl: 'https://example.com/token',
        idpJwksUrl: 'https://example.com/jwks',
        idpJwsAlgorithm: 'RS256',
        usernameClaim: 'sub',
        firstNameClaim: 'given_name',
        lastNameClaim: 'family_name',
        emailClaim: 'email',
        groupsClaim: 'groups',
      }),
      saveConfig: mockSaveConfig,
    });

    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Client ID')).toBeInTheDocument();
    });

    // Make a change to enable Save button (form is pristine until modified)
    const clientIdInput = document.getElementById('settings-input-clientId') as HTMLInputElement;
    fireEvent.change(clientIdInput, { target: { value: 'modified-client' } });

    const saveButton = screen.getByRole('button', { name: /save/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockSaveConfig).toHaveBeenCalled();
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
      expect(mockSaveConfig).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(saveButton).toBeDisabled();
    });
  });

  it('resets form when Discard is confirmed', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Client ID')).toBeInTheDocument();
    });

    const clientIdInput = document.getElementById('settings-input-clientId') as HTMLInputElement;
    fireEvent.change(clientIdInput, { target: { value: 'modified-client' } });

    expect(clientIdInput).toHaveValue('modified-client');

    const discardButton = screen.getByRole('button', { name: /discard/i });
    fireEvent.click(discardButton);

    await waitFor(() => {
      expect(screen.getByText('Unsaved Changes')).toBeInTheDocument();
    });

    const leaveButton = screen.getByRole('button', { name: /leave/i });
    fireEvent.click(leaveButton);

    await waitFor(() => {
      expect(clientIdInput).toHaveValue('client-id');
    });
  });

  it('handles loading state', () => {
    mockedUseOAuth2Api.mockReturnValue({
      loading: true,
      error: null,
      setError: mockSetError,
      fetchConfig: mockFetchConfig,
      saveConfig: mockSaveConfig,
    });

    render(<OAuth2Page />, { wrapper: TestWrapper });

    expect(screen.getByText(/loading oauth2 configuration/i)).toBeInTheDocument();
  });

  it('handles error state', async () => {
    mockedUseOAuth2Api.mockReturnValue({
      loading: false,
      error: 'Failed to load OAuth2 settings',
      setError: mockSetError,
      fetchConfig: mockFetchConfig,
      saveConfig: mockSaveConfig,
    });

    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load OAuth2 settings')).toBeInTheDocument();
    });
  });

  it('validates required fields before save', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Client ID')).toBeInTheDocument();
    });

    const clientIdInput = document.getElementById('settings-input-clientId') as HTMLInputElement;
    fireEvent.change(clientIdInput, { target: { value: '' } });

    const saveButton = screen.getByRole('button', { name: /save/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(screen.getByText('Client ID is required')).toBeInTheDocument();
    });
  });

  it('shows ID Token Signing Algorithm dropdown', async () => {
    render(<OAuth2Page />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('JWS Algorithm')).toBeInTheDocument();
    });
  });

  describe('breadcrumbs', () => {
    it('renders Settings breadcrumb that navigates to settings page', async () => {
      render(<OAuth2Page />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      });

      // Click Settings breadcrumb navigates to settings page
      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });

    it('renders OAuth2 as current page breadcrumb', async () => {
      render(<OAuth2Page />, { wrapper: TestWrapper });

      await waitFor(() => {
        // The current page item is rendered as Text (not a button) with aria-current="page"
        const breadcrumb = screen.getByText('OAuth2', { selector: '[aria-current="page"]' });
        expect(breadcrumb).toBeInTheDocument();
      });
    });
  });
});

