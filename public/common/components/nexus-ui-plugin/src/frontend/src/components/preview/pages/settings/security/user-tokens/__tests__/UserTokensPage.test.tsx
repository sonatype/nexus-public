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

import { UserTokensPage } from '../UserTokensPage';
import * as useUserTokensApiModule from '../useUserTokensApi';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
  },
  Permissions: {
    USER_TOKENS_SETTINGS: { UPDATE: 'nexus:usertokens-settings:update' },
    USER_TOKENS_USERS: { DELETE: 'nexus:usertokens-users:delete' },
  },
  APIConstants: {
    REST: {
      PUBLIC: {
        USER_TOKENS: '/service/rest/v1/security/user-tokens',
      },
    },
  },
}));

// Mock the API hook
jest.mock('../useUserTokensApi');

const mockedUseUserTokensApi = useUserTokensApiModule.useUserTokensApi as jest.MockedFunction<typeof useUserTokensApiModule.useUserTokensApi>;

// Wrapper component for Radix Theme and Toast context
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

describe('UserTokensPage', () => {
  const mockFetchSettings = jest.fn();
  const mockSaveSettings = jest.fn();
  const mockResetAllTokens = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchSettings.mockResolvedValue({
      enabled: true,
      protectContent: false,
      expirationEnabled: false,
      expirationDays: 30,
    });
    mockedUseUserTokensApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchSettings: mockFetchSettings,
      saveSettings: mockSaveSettings.mockResolvedValue({}),
      resetAllTokens: mockResetAllTokens.mockResolvedValue({}),
    });
  });

  it('renders the page title and description', async () => {
    render(<UserTokensPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'User Tokens' })).toBeInTheDocument();
    });

    expect(screen.getByText(/Configure user token settings/i)).toBeInTheDocument();
  });

  it('loads settings on mount', async () => {
    render(<UserTokensPage />, { wrapper: TestWrapper });
    
    await waitFor(() => {
      expect(mockFetchSettings).toHaveBeenCalled();
    });
  });

  it('displays enable user tokens checkbox', async () => {
    render(<UserTokensPage />, { wrapper: TestWrapper });
    
    await waitFor(() => {
      expect(screen.getByLabelText(/Enable user tokens/i)).toBeInTheDocument();
    });
  });

  it('handles loading state', () => {
    mockFetchSettings.mockReturnValue(new Promise(() => {})); // Never resolves
    
    render(<UserTokensPage />, { wrapper: TestWrapper });
    
    expect(screen.getByText(/Loading/i)).toBeInTheDocument();
  });

  it('handles error state', async () => {
    mockedUseUserTokensApi.mockReturnValue({
      loading: false,
      error: 'Failed to load settings',
      setError: mockSetError,
      fetchSettings: mockFetchSettings,
      saveSettings: mockSaveSettings,
      resetAllTokens: mockResetAllTokens,
    });

    render(<UserTokensPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'User Tokens' })).toBeInTheDocument();
    });

    expect(screen.getByText('Failed to load settings')).toBeInTheDocument();
  });

  it('shows save button', async () => {
    render(<UserTokensPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'User Tokens' })).toBeInTheDocument();
    });

    expect(screen.getByText('Save')).toBeInTheDocument();
  });

  it('shows discard button', async () => {
    render(<UserTokensPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'User Tokens' })).toBeInTheDocument();
    });

    expect(screen.getByText('Discard')).toBeInTheDocument();
  });

  it('shows reset all tokens button when enabled', async () => {
    render(<UserTokensPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'User Tokens' })).toBeInTheDocument();
    });

    expect(screen.getByText(/Reset All User Tokens/i)).toBeInTheDocument();
  });

  it('shows help text about user tokens', async () => {
    render(<UserTokensPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'User Tokens' })).toBeInTheDocument();
    });

    expect(screen.getByText(/User tokens allow/i)).toBeInTheDocument();
  });

  it('shows repository authentication section', async () => {
    render(<UserTokensPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'User Tokens' })).toBeInTheDocument();
    });

    expect(screen.getByText('Repository Authentication')).toBeInTheDocument();
  });

  it('shows token expiration section', async () => {
    render(<UserTokensPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'User Tokens' })).toBeInTheDocument();
    });

    expect(screen.getByText('Token Expiration')).toBeInTheDocument();
  });

  describe('breadcrumbs', () => {
    it('renders Settings breadcrumb that navigates to settings page', async () => {
      render(<UserTokensPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
      });

      // Click Settings breadcrumb navigates to settings page
      screen.getByRole('button', { name: 'Settings' }).click();
      expect(window.location.hash).toBe('#preview/admin/settings');
    });

    it('renders User Tokens as current page breadcrumb', async () => {
      render(<UserTokensPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        // The current page item is rendered as Text (not a button) with aria-current="page"
        const breadcrumb = screen.getByText('User Tokens', { selector: '[aria-current="page"]' });
        expect(breadcrumb).toBeInTheDocument();
      });
    });
  });
});
