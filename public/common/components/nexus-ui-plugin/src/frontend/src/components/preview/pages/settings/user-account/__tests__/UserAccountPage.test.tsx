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
import { ToastProvider } from '../../../../shared';

import { UserAccountPage } from '../UserAccountPage';

// Mock ExtJS - note: jest.mock is hoisted, so we use a factory function
jest.mock('../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    useUser: jest.fn(),
  },
}));

// Mock restClient
jest.mock('../../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
    put: jest.fn(),
  },
  ENDPOINTS: {
    USER_ACCOUNT: '/service/rest/internal/ui/user',
  },
}));

const mockApiUser = {
  userId: 'testuser',
  firstName: 'Test',
  lastName: 'User',
  email: 'test@example.com',
  external: false,
};

const mockExtJsUser = {
  id: 'testuser',
  authenticated: true,
  administrator: true,
  authenticatedRealms: ['NexusAuthenticatingRealm'],
};

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme><ToastProvider>{children}</ToastProvider></Theme>;
}

describe('UserAccountPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    const { ExtJS } = require('../../../../../../interface/ExtJS');
    ExtJS.useUser.mockReturnValue(mockExtJsUser);

    const { restClient } = require('../../../../../../interface/api');
    restClient.get.mockResolvedValue(mockApiUser);
  });

  it('renders the page header', async () => {
    render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('User Account')).toBeInTheDocument();
    });

    expect(screen.getByText('Manage your account settings')).toBeInTheDocument();
  });

  it('fetches user account data from the internal API on mount', async () => {
    const { restClient, ENDPOINTS } = require('../../../../../../interface/api');

    render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(restClient.get).toHaveBeenCalledWith(ENDPOINTS.USER_ACCOUNT);
    });
  });

  it('displays real Name from API response', async () => {
    render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Test User')).toBeInTheDocument();
    });
  });

  it('displays real Email from API response', async () => {
    render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('test@example.com')).toBeInTheDocument();
    });
  });

  it('does not show "Not set" when API returns real name and email', async () => {
    render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.queryByText('Not set')).not.toBeInTheDocument();
    });
  });

  it('shows "Not set" in the Name row when API returns empty firstName and lastName', async () => {
    const { restClient } = require('../../../../../../interface/api');
    restClient.get.mockResolvedValue({ ...mockApiUser, firstName: '', lastName: '' });

    const { container } = render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      const nameRow = container.querySelector('.user-account-page__info-row:nth-child(3)');
      expect(nameRow).toHaveTextContent('Not set');
    });
  });

  it('shows "Not set" in the Email row when API returns empty email', async () => {
    const { restClient } = require('../../../../../../interface/api');
    restClient.get.mockResolvedValue({ ...mockApiUser, email: '' });

    const { container } = render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      const emailRow = container.querySelector('.user-account-page__info-row:nth-child(2)');
      expect(emailRow).toHaveTextContent('Not set');
    });
  });

  it('hides password form when API fetch fails', async () => {
    const { restClient } = require('../../../../../../interface/api');
    restClient.get.mockRejectedValue(new Error('Network error'));

    render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText(/Failed to load account information/)).toBeInTheDocument();
    });

    expect(screen.queryByRole('heading', { name: 'Change Password' })).not.toBeInTheDocument();
  });

  it('shows password change form for internal (non-external) users', async () => {
    render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Change Password' })).toBeInTheDocument();
    });

    expect(screen.getByLabelText(/Current Password/)).toBeInTheDocument();
    expect(screen.getByTestId('password-newPassword')).toBeInTheDocument();
    expect(screen.getByTestId('password-confirmPassword')).toBeInTheDocument();
  });

  it('validates password length', async () => {
    render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('password-newPassword')).toBeInTheDocument();
    });

    const newPasswordInput = screen.getByTestId('password-newPassword');
    fireEvent.change(newPasswordInput, { target: { value: 'short' } });
    fireEvent.blur(newPasswordInput);

    await waitFor(() => {
      const errorEl = document.getElementById('settings-error-newPassword');
      expect(errorEl).toBeInTheDocument();
      expect(errorEl).toHaveTextContent(/at least.*characters/i);
    });
  });

  it('validates password confirmation', async () => {
    render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('password-newPassword')).toBeInTheDocument();
    });

    const newPasswordInput = screen.getByTestId('password-newPassword');
    const confirmPasswordInput = screen.getByTestId('password-confirmPassword');

    fireEvent.change(newPasswordInput, { target: { value: 'password123' } });
    fireEvent.change(confirmPasswordInput, { target: { value: 'different123' } });
    fireEvent.blur(confirmPasswordInput);

    await waitFor(() => {
      expect(screen.getByText(/passwords.*match/i)).toBeInTheDocument();
    });
  });

  it('shows external user message when API returns external=true', async () => {
    const { restClient } = require('../../../../../../interface/api');
    restClient.get.mockResolvedValue({ ...mockApiUser, external: true });

    render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText(/Your account is managed externally/)).toBeInTheDocument();
    });
  });

  it('shows warning when not logged in (no ExtJS user)', async () => {
    const { ExtJS } = require('../../../../../../interface/ExtJS');
    ExtJS.useUser.mockReturnValue(null);

    render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText(/must be logged in/i)).toBeInTheDocument();
    });
  });

  it('clears form on discard', async () => {
    render(<UserAccountPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByLabelText(/Current Password/)).toBeInTheDocument();
    });

    const currentPasswordInput = screen.getByLabelText(/Current Password/);
    fireEvent.change(currentPasswordInput, { target: { value: 'oldpassword' } });

    const discardButton = screen.getByRole('button', { name: /discard/i });
    fireEvent.click(discardButton);

    expect(currentPasswordInput).toHaveValue('');
  });

  describe('Security section (spec xhpg)', () => {
    it('renders Security section with User Token and NuGet API Key links', async () => {
      render(<UserAccountPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('security-section')).toBeInTheDocument();
      });

      expect(screen.getByTestId('user-token-link-card')).toBeInTheDocument();
      expect(screen.getByTestId('nuget-key-link-card')).toBeInTheDocument();
    });

    it('NuGet API Key shows Pro badge', async () => {
      render(<UserAccountPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('nuget-key-link-card')).toBeInTheDocument();
      });

      expect(screen.getByText('Pro')).toBeInTheDocument();
    });

    it('Manage User Token button navigates to user token hash', async () => {
      render(<UserAccountPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('manage-user-token-btn')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId('manage-user-token-btn'));
      expect(window.location.hash).toBe('#preview/user/usertoken');
    });

    it('Manage NuGet API Key button navigates to nuget api token hash', async () => {
      render(<UserAccountPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('manage-nuget-key-btn')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId('manage-nuget-key-btn'));
      expect(window.location.hash).toBe('#preview/user/nugetapitoken');
    });
  });
});
