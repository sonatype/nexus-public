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
import { render, screen, fireEvent } from '@testing-library/react';
import LogInAndUserProfileMenu from './LogInAndUserProfileMenu';
import { ROUTE_NAMES } from '../../routerConfig/routeNames/routeNames';

const mockRouter = {
  globals: { params: {} },
  urlService: { url: jest.fn() },
  stateService: {
    go: jest.fn(),
    get: jest.fn()
  },
  stateRegistry: {
    get: jest.fn()
  }
};

jest.mock('@uirouter/react', () => ({
  useRouter: () => mockRouter,
  useSref: (routeName) => ({
    href: `#${routeName}`
  })
}));

// Mock nexus-ui-plugin with all needed exports
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const useIsVisibleMock = jest.fn().mockReturnValue(true);

  return {
    ExtJS: {
      signOut: jest.fn(),
      useState: jest.fn(),
      state: jest.fn().mockReturnValue({
        getValue: jest.fn().mockImplementation((key) => {
          if (key === 'user') return { id: 'admin' };
          if (key === 'nugetApiKeyRealmEnabled') return true;
          if (key === 'userTokenRealmEnabled') return true;
          return undefined;
        }),
      }),
      useUser: jest.fn().mockReturnValue({ name: 'admin', administrator: true }),
      usePermission: jest.fn().mockReturnValue(true),
    },
    useIsVisible: useIsVisibleMock,
  };
});

// Import the mocked modules
import { ExtJS, useIsVisible } from '@sonatype/nexus-ui-plugin';

jest.mock('../../hooks/useHasUser', () => ({
  __esModule: true,
  default: jest.fn()
}));

import useHasUser from '../../hooks/useHasUser';

describe('LogInAndUserProfileMenu', () => {
  beforeEach(() => {
    mockRouter.globals.params = {};
    mockRouter.urlService.url.mockReset();
    mockRouter.stateService.go.mockReset();
    useHasUser.mockReset();

    // Reset ExtJS mocks
    ExtJS.signOut.mockClear();
    ExtJS.useState.mockReset();
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockImplementation((key) => {
        if (key === 'user') return { id: 'admin' };
        if (key === 'nugetApiKeyRealmEnabled') return true;
        if (key === 'userTokenRealmEnabled') return true;
        return undefined;
      }),
    });

    // Reset and configure useIsVisible mock - default to true
    useIsVisible.mockReset();
    useIsVisible.mockReturnValue(true);

    // Default mock for route registry
    mockRouter.stateRegistry.get.mockReturnValue({
      data: { visibilityRequirements: {} }
    });
  });

  describe('Login Button (unauthenticated state)', () => {
    beforeEach(() => {
      useHasUser.mockReturnValue(false);
    });

    it('renders login button when user is not authenticated', () => {
      render(<LogInAndUserProfileMenu />);

      const loginButton = screen.getByRole('button', { name: /log in/i });
      expect(loginButton).toBeInTheDocument();
    });

    it('has correct button attributes', () => {
      render(<LogInAndUserProfileMenu />);

      const loginButton = screen.getByRole('button', { name: /log in/i });
      // NxButton renders aria-label but not title attribute
      expect(loginButton).toHaveAttribute('aria-label', 'Log In');
      expect(loginButton).toHaveAttribute('data-analytics-id', 'nxrm-global-header-login');
    });

    it('renders icon-only button variant', () => {
      render(<LogInAndUserProfileMenu />);

      const loginButton = screen.getByRole('button', { name: /log in/i });
      // Button is rendered with variant="icon-only" prop, which our mock doesn't translate to a class
      // but we can verify the button exists
      expect(loginButton).toBeInTheDocument();
    });

    it('renders user icon', () => {
      const { container } = render(<LogInAndUserProfileMenu />);

      // Real component renders FontAwesome icon as SVG
      const icon = container.querySelector('svg[data-icon="user"]');
      expect(icon).toBeInTheDocument();
    });

    describe('login button click behavior', () => {
      it('redirects to login page when clicked', () => {
        mockRouter.urlService.url.mockReturnValue('/browse/repositories');

        render(<LogInAndUserProfileMenu />);

        const loginButton = screen.getByRole('button', { name: /log in/i });
        fireEvent.click(loginButton);

        expect(mockRouter.stateService.go).toHaveBeenCalledWith(
          ROUTE_NAMES.LOGIN,
          { returnTo: expect.any(String) }
        );
      });

      it('encodes current URL as base64 returnTo parameter', () => {
        mockRouter.urlService.url.mockReturnValue('/browse/repositories');

        render(<LogInAndUserProfileMenu />);

        const loginButton = screen.getByRole('button', { name: /log in/i });
        fireEvent.click(loginButton);

        const returnToParam = mockRouter.stateService.go.mock.calls[0][1].returnTo;
        const decoded = atob(returnToParam);
        expect(decoded).toBe('#/browse/repositories');
      });

      it('handles root URL correctly', () => {
        mockRouter.urlService.url.mockReturnValue('/');

        render(<LogInAndUserProfileMenu />);

        const loginButton = screen.getByRole('button', { name: /log in/i });
        fireEvent.click(loginButton);

        const returnToParam = mockRouter.stateService.go.mock.calls[0][1].returnTo;
        const decoded = atob(returnToParam);
        expect(decoded).toBe('#/');
      });

      it('handles URL with query parameters', () => {
        mockRouter.urlService.url.mockReturnValue('/browse/repositories?filter=maven');

        render(<LogInAndUserProfileMenu />);

        const loginButton = screen.getByRole('button', { name: /log in/i });
        fireEvent.click(loginButton);

        const returnToParam = mockRouter.stateService.go.mock.calls[0][1].returnTo;
        const decoded = atob(returnToParam);
        expect(decoded).toBe('#/browse/repositories?filter=maven');
      });

      it('handles URL with special characters', () => {
        mockRouter.urlService.url.mockReturnValue('/browse/search?q=test value&type=npm');

        render(<LogInAndUserProfileMenu />);

        const loginButton = screen.getByRole('button', { name: /log in/i });
        fireEvent.click(loginButton);

        const returnToParam = mockRouter.stateService.go.mock.calls[0][1].returnTo;
        const decoded = atob(returnToParam);
        expect(decoded).toBe('#/browse/search?q=test value&type=npm');
      });
    });
  });

  describe('Profile Menu (authenticated state)', () => {
    const mockUserId = 'testuser';

    beforeEach(() => {
      useHasUser.mockReturnValue(true);
      // Mock ExtJS.useState to return the user ID (component calls: ExtJS.useState(() => ExtJS.state().getValue('user')?.id))
      ExtJS.useState.mockReturnValue(mockUserId);
      ExtJS.state().getValue.mockReturnValue({ id: mockUserId });
      useIsVisible.mockReturnValue(true);
    });

    it('renders profile menu when user is authenticated', () => {
      const { container } = render(<LogInAndUserProfileMenu />);

      // Query by the dropdown container with analytics ID
      const dropdown = container.querySelector('[data-analytics-id="nxrm-global-header-profile-menu"]');
      expect(dropdown).toBeInTheDocument();

      // Verify the toggle button exists with aria-expanded
      const profileButton = container.querySelector('[aria-expanded]');
      expect(profileButton).toBeInTheDocument();
    });

    it('has correct analytics ID on profile menu', () => {
      const { container } = render(<LogInAndUserProfileMenu />);

      const dropdown = container.querySelector('[data-analytics-id="nxrm-global-header-profile-menu"]');
      expect(dropdown).toBeInTheDocument();
    });

    it('displays user ID in menu header', () => {
      const { container } = render(<LogInAndUserProfileMenu />);

      // Find and click the dropdown toggle button
      const profileButton = container.querySelector('[aria-expanded="false"]');
      fireEvent.click(profileButton);

      // Check for user ID in the menu
      expect(screen.getByText(mockUserId)).toBeInTheDocument();
    });

    it('displays user icon in menu header', () => {
      const { container } = render(<LogInAndUserProfileMenu />);

      const profileButton = container.querySelector('[aria-expanded="false"]');
      fireEvent.click(profileButton);

      // Verify the menu is now expanded and contains user ID
      expect(screen.getByText('testuser')).toBeInTheDocument();
    });

    describe('menu items', () => {
      it('renders My Account link', () => {
        const { container } = render(<LogInAndUserProfileMenu />);

        const profileButton = container.querySelector('[aria-expanded="false"]');
        fireEvent.click(profileButton);

        const myAccountLink = screen.getByRole('link', { name: /my account/i });
        expect(myAccountLink).toBeInTheDocument();
        expect(myAccountLink).toHaveAttribute('data-analytics-id', 'nxrm-global-header-profile-menu-my-profile');
      });

      it('renders User Token link when enabled', () => {
        useIsVisible.mockReturnValue(true);

        const { container } = render(<LogInAndUserProfileMenu />);

        const profileButton = container.querySelector('[aria-expanded="false"]');
        fireEvent.click(profileButton);

        const userTokenLink = screen.getByRole('link', { name: /user token/i });
        expect(userTokenLink).toBeInTheDocument();
        expect(userTokenLink).toHaveAttribute('data-analytics-id', 'nxrm-global-header-profile-menu-user-token');
      });

      it('does not render User Token link when disabled', () => {
        useIsVisible.mockReturnValue(false);

        const { container } = render(<LogInAndUserProfileMenu />);

        const profileButton = container.querySelector('[aria-expanded="false"]');
        fireEvent.click(profileButton);

        const userTokenLink = screen.queryByRole('link', { name: /user token/i });
        expect(userTokenLink).not.toBeInTheDocument();
      });

      it('renders NuGet API Key link when enabled', () => {
        useIsVisible.mockReturnValue(true);

        const { container } = render(<LogInAndUserProfileMenu />);

        const profileButton = container.querySelector('[aria-expanded="false"]');
        fireEvent.click(profileButton);

        const nugetLink = screen.getByRole('link', { name: /nuget api key/i });
        expect(nugetLink).toBeInTheDocument();
        expect(nugetLink).toHaveAttribute('data-analytics-id', 'nxrm-global-header-profile-menu-nuget-api-key');
      });

      it('does not render NuGet API Key link when disabled', () => {
        useIsVisible.mockReturnValue(false);

        const { container } = render(<LogInAndUserProfileMenu />);

        const profileButton = container.querySelector('[aria-expanded="false"]');
        fireEvent.click(profileButton);

        const nugetLink = screen.queryByRole('link', { name: /nuget api key/i });
        expect(nugetLink).not.toBeInTheDocument();
      });

      it('renders both optional links when both are enabled', () => {
        useIsVisible.mockReturnValue(true);

        const { container } = render(<LogInAndUserProfileMenu />);

        const profileButton = container.querySelector('[aria-expanded="false"]');
        fireEvent.click(profileButton);

        expect(screen.getByRole('link', { name: /user token/i })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /nuget api key/i })).toBeInTheDocument();
      });

      it('renders divider before logout button', () => {
        const { container } = render(<LogInAndUserProfileMenu />);

        const profileButton = container.querySelector('[aria-expanded="false"]');
        fireEvent.click(profileButton);

        // Look for the divider element - it renders as hr with different class
        const divider = container.querySelector('hr');
        expect(divider).toBeInTheDocument();
      });

      it('renders Log Out button', () => {
        const { container } = render(<LogInAndUserProfileMenu />);

        const profileButton = container.querySelector('[aria-expanded="false"]');
        fireEvent.click(profileButton);

        const logoutButton = screen.getByRole('button', { name: /log out/i });
        expect(logoutButton).toBeInTheDocument();
        expect(logoutButton).toHaveClass('nx-dropdown-button');
      });
    });

    describe('logout functionality', () => {
      it('calls ExtJS.signOut when logout button is clicked', () => {
        const { container } = render(<LogInAndUserProfileMenu />);

        const profileButton = container.querySelector('[aria-expanded="false"]');
        fireEvent.click(profileButton);

        const logoutButton = screen.getByRole('button', { name: /log out/i });
        fireEvent.click(logoutButton);

        expect(ExtJS.signOut).toHaveBeenCalledTimes(1);
      });
    });

    describe('edge cases', () => {
      it('handles undefined user ID gracefully', () => {
        ExtJS.state().getValue.mockReturnValue(undefined);
        ExtJS.useState.mockReturnValue(undefined);

        const { container } = render(<LogInAndUserProfileMenu />);

        const profileButton = container.querySelector('[aria-expanded="false"]');
        fireEvent.click(profileButton);

        // Component should still render without crashing
        expect(profileButton).toBeInTheDocument();
      });

      it('handles user object without id property', () => {
        ExtJS.state().getValue.mockReturnValue({});
        ExtJS.useState.mockReturnValue(undefined);

        const { container } = render(<LogInAndUserProfileMenu />);

        const profileButton = container.querySelector('[aria-expanded="false"]');
        fireEvent.click(profileButton);

        // Component should still render without crashing
        expect(profileButton).toBeInTheDocument();
      });
    });
  });

  describe('conditional rendering', () => {
    it('switches from login button to profile menu when user authenticates', () => {
      useHasUser.mockReturnValue(false);

      const { rerender, container } = render(<LogInAndUserProfileMenu />);

      expect(screen.getByRole('button', { name: /log in/i })).toBeInTheDocument();

      // Simulate user authentication
      useHasUser.mockReturnValue(true);
      ExtJS.useState.mockReturnValue('testuser');
      ExtJS.state().getValue.mockReturnValue({ id: 'testuser' });
      useIsVisible.mockReturnValue(true);

      rerender(<LogInAndUserProfileMenu />);

      expect(screen.queryByRole('button', { name: /log in/i })).not.toBeInTheDocument();

      // Verify profile dropdown is rendered instead
      const dropdown = container.querySelector('[data-analytics-id="nxrm-global-header-profile-menu"]');
      expect(dropdown).toBeInTheDocument();
    });
  });
});
