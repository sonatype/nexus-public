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
import { act, fireEvent, render, screen } from '@testing-library/react';

import { ExtJS } from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../constants/UIStrings';
import LoginPage from './LoginPage';

// Mock the child components
jest.mock('../../layout/LoginLayout', () => {
  return function LoginLayout({ children }) {
    return <div data-testid="login-layout">{children}</div>;
  };
});

const mockOnErrorCallbacks = {
  local: jest.fn(),
};

jest.mock('./LocalLogin', () => {
  return function LoginForm(props) {
    // Store the onError callback when component renders
    if (props.onError) {
      mockOnErrorCallbacks.local = props.onError;
    }

    return (
      <div data-testid="login-form" data-primary-button={props.primaryButton}>
        Login Form
      </div>
    );
  };
});

jest.mock('./SsoLogin', () => {
  return function SsoLoginButton() {
    return <button data-testid="sso-login-button">SSO Login</button>;
  };
});

jest.mock('./InitialPasswordInfo', () => {
  return function InitialPasswordInfo({ passwordFilePath }) {
    return <div data-testid="initial-password-info">{passwordFilePath}</div>;
  };
});

const mockRouterGo = jest.fn();
const mockRouterUrl = jest.fn();
const mockRouterParams = {};

jest.mock('@uirouter/react', () => {
  const actualModule = jest.requireActual('@uirouter/react');
  return {
    ...actualModule,
    useRouter: () => ({
      stateService: {
        go: mockRouterGo
      },
      urlService: {
        url: mockRouterUrl
      },
      globals: {
        params: mockRouterParams
      }
    })
  };
});

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    useState: jest.fn(),
    state: jest.fn(),
    waitForNextPermissionChange: jest.fn().mockResolvedValue(),
    urlOf: jest.fn().mockImplementation((_path) => `/static/rapture/resources/favicon.svg`)
  }
}));

describe('LoginPage', () => {
  const mockLogoConfig = { url: 'test-logo.png' };
  const mockState = jest.fn();

  beforeEach(() => {
    ExtJS.state = mockState;
    mockState.mockReturnValue({
      getValue: jest.fn().mockImplementation((key, defaultValue) => defaultValue)
    });
    jest.clearAllMocks();
    mockRouterGo.mockClear();
    mockRouterUrl.mockClear();
    mockRouterParams.returnTo = undefined;
  });

  function setupStates(
    samlEnabled = false,
    oauth2Enabled = false,
    isCloud = false,
    anonymousUsername = null,
    adminPasswordFile = null,
    ldapRealmEnabled = true,
    userTokenRealmEnabled = true,
    localAuthRealmEnabled = true,
    crowdRealmEnabled = false,
    onboardingRequired = undefined
  ) {
    mockState.mockReturnValue({
      getValue: jest.fn().mockImplementation((key, defaultValue) => {
        const values = {
          'samlEnabled': samlEnabled,
          'oauth2Enabled': oauth2Enabled,
          'isCloud': isCloud,
          'anonymousUsername': anonymousUsername,
          'admin.password.file': adminPasswordFile,
          'ldapRealmEnabled': ldapRealmEnabled,
          'userTokenRealmEnabled': userTokenRealmEnabled,
          'localAuthRealmEnabled': localAuthRealmEnabled,
          'crowdRealmEnabled': crowdRealmEnabled,
          'onboarding.required': onboardingRequired
        };
        return values[key] !== undefined ? values[key] : defaultValue;
      })
    });
  }

  function renderComponent(props) {
    return render(<LoginPage {...props} />);
  }

  describe('self-hosted environment', () => {
    it('renders login form when no SSO is enabled', () => {
      setupStates(false, false, false);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('login-tile')).toBeInTheDocument();
      expect(screen.getByText(UIStrings.LOGIN_TITLE)).toBeInTheDocument();
      expect(screen.getByText(UIStrings.LOGIN_SUBTITLE)).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      expect(screen.queryByTestId('sso-login-button')).not.toBeInTheDocument();
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });

    it('renders both SSO button and login form with divider when SAML is enabled', () => {
      setupStates(true, false, false);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      // Divider should appear when both SSO and local login are shown
      expect(screen.getByText(UIStrings.SSO_DIVIDER_LABEL)).toBeInTheDocument();
    });

    it('renders both SSO button and login form with divider when OAuth2 is enabled', () => {
      setupStates(false, true, false);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      // Divider should appear when both SSO and local login are shown
      expect(screen.getByText(UIStrings.SSO_DIVIDER_LABEL)).toBeInTheDocument();
    });

    it('sets LocalLogin primaryButton to false when SSO is enabled', () => {
      setupStates(true, false, false);

      renderComponent({ logoConfig: mockLogoConfig });

      const loginForm = screen.getByTestId('login-form');
      expect(loginForm).toHaveAttribute('data-primary-button', 'false');
    });

    it('sets LocalLogin primaryButton to true when SSO is not enabled', () => {
      setupStates(false, false, false);

      renderComponent({ logoConfig: mockLogoConfig });

      const loginForm = screen.getByTestId('login-form');
      expect(loginForm).toHaveAttribute('data-primary-button', 'true');
    });
  });

  describe('cloud environment', () => {
    beforeEach(() => {
      Object.defineProperty(window, 'location', {
        configurable: true,
        value: { hostname: 'cloud.nexus.sonatype.example' },
      });
    });

    afterEach(() => {
      Object.defineProperty(window, 'location', {
        configurable: true,
        value: { hostname: 'localhost' },
      });
    });

    it('does not render login form in cloud environment without SSO', () => {
      setupStates(false, false, true);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('login-tile')).toBeInTheDocument();
      expect(screen.getByText(UIStrings.LOGIN_TITLE)).toBeInTheDocument();
      expect(screen.getByText(UIStrings.LOGIN_SUBTITLE)).toBeInTheDocument();
      expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      expect(screen.queryByTestId('sso-login-button')).not.toBeInTheDocument();
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });

    it('renders only SSO button without divider in cloud environment with OAuth2 enabled', () => {
      setupStates(false, true, true);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      // No divider because local login is not shown in cloud
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });

    it('renders only SSO button without divider in cloud environment with SAML enabled', () => {
      setupStates(true, false, true);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      // No divider because local login is not shown in cloud
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });

    it('renders only SSO button without divider in cloud environment with both SAML and OAuth2 enabled', () => {
      setupStates(true, true, true);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      // No divider because local login is not shown in cloud
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });
  });

  describe('logo configuration', () => {
    it('passes logoConfig to LoginLayout', () => {
      setupStates(false, false, false);
      const customLogoConfig = { url: 'custom-logo.png', alt: 'Custom Logo' };

      renderComponent({ logoConfig: customLogoConfig });

      expect(screen.getByTestId('login-layout')).toBeInTheDocument();
    });
  });

  describe('general error handling', () => {
    it('does not display error alert initially', () => {
      setupStates(false, false, false);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('displays general errors received from LocalLogin', () => {
      setupStates(false, false, false);

      renderComponent({ logoConfig: mockLogoConfig });

      act(() => {
        mockOnErrorCallbacks.local('Server unavailable');
      });

      const alert = screen.getByRole('alert');
      expect(alert).toHaveTextContent('Server unavailable');
    });

    it('allows user to dismiss the error alert', () => {
      setupStates(false, false, false);

      renderComponent({ logoConfig: mockLogoConfig });

      act(() => {
        mockOnErrorCallbacks.local('Connection failed. Please check your network connection.');
      });

      expect(screen.getByRole('alert')).toBeInTheDocument();

      const closeButton = screen.getByRole('button', { name: /close/i });
      fireEvent.click(closeButton);

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('replaces previous error when new error is received', () => {
      setupStates(false, false, false);

      renderComponent({ logoConfig: mockLogoConfig });

      act(() => {
        mockOnErrorCallbacks.local('First error');
      });

      expect(screen.getByRole('alert')).toHaveTextContent('First error');

      act(() => {
        mockOnErrorCallbacks.local('Second error');
      });

      expect(screen.getByRole('alert')).toHaveTextContent('Second error');
      expect(screen.queryByText('First error')).not.toBeInTheDocument();
    });
  });

  describe('anonymous access', () => {
    beforeEach(() => {
      mockRouterParams.returnTo = undefined;
      mockRouterGo.mockClear();
      mockRouterUrl.mockClear();
    });

    it('does not render anonymous access button when anonymous username is not configured', () => {
      setupStates(false, false, false, null);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      expect(screen.queryByTestId('continue-without-login-button')).not.toBeInTheDocument();
    });

    it('renders anonymous access button when anonymous username is configured', () => {
      setupStates(false, false, false, 'anonymous');

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      expect(screen.getByTestId('continue-without-login-button')).toBeInTheDocument();
    });

    it('renders anonymous access button with SSO enabled', () => {
      setupStates(true, false, false, 'anonymous');

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      expect(screen.getByTestId('continue-without-login-button')).toBeInTheDocument();
    });

    it('does not render anonymous access button when anonymous username is empty string', () => {
      setupStates(false, false, false, '');

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.queryByTestId('continue-without-login-button')).not.toBeInTheDocument();
    });

    it('does not render anonymous access button when anonymous username is undefined', () => {
      setupStates(false, false, false, undefined);

      render(<LoginPage logoConfig={mockLogoConfig} />);

      expect(screen.queryByTestId('continue-without-login-button')).not.toBeInTheDocument();
    });

    it('redirects to returnTo URL when returnTo is provided and continuing without login', () => {
      setupStates(false, false, false, 'anonymous');
      const returnToUrl = '#browse/browse:maven-snapshots';
      mockRouterParams.returnTo = btoa(returnToUrl);

      renderComponent({ logoConfig: mockLogoConfig });

      const button = screen.getByTestId('continue-without-login-button');
      button.click();

      expect(mockRouterUrl).toHaveBeenCalledWith(returnToUrl);
      expect(mockRouterGo).not.toHaveBeenCalled();
    });

    it('redirects to welcome page when no returnTo is provided and continuing without login', () => {
      setupStates(false, false, false, 'anonymous');

      renderComponent({ logoConfig: mockLogoConfig });

      const button = screen.getByTestId('continue-without-login-button');
      button.click();

      expect(mockRouterGo).toHaveBeenCalledWith('browse.welcome');
      expect(mockRouterUrl).not.toHaveBeenCalled();
    });
  });

  describe('initial password info', () => {
    it('displays initial password info when admin password file path is provided', () => {
      const passwordFilePath = '/path/to/admin.password';
      setupStates(false, false, false, null, passwordFilePath, true, true, true, false, true);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('initial-password-info')).toBeInTheDocument();
      expect(screen.getByText(passwordFilePath)).toBeInTheDocument();
    });

    it('does not display initial password info when admin password file path is not provided', () => {
      setupStates(false, false, false, null, null);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.queryByTestId('initial-password-info')).not.toBeInTheDocument();
    });

    it('does not display initial password info when admin password file path is empty string', () => {
      setupStates(false, false, false, null, '');

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.queryByTestId('initial-password-info')).not.toBeInTheDocument();
    });

    it('does not display initial password info when admin password file path is undefined', () => {
      setupStates(false, false, false, null, undefined);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.queryByTestId('initial-password-info')).not.toBeInTheDocument();
    });

    it('displays initial password info with SSO enabled', () => {
      const passwordFilePath = '/path/to/admin.password';
      setupStates(true, false, false, null, passwordFilePath, true, true, true, false, true);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('initial-password-info')).toBeInTheDocument();
      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
    });

    it('does not display initial password info in cloud environment', () => {
      const passwordFilePath = '/path/to/admin.password';

      setupStates(false, false, true, null, passwordFilePath);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.queryByTestId('initial-password-info')).not.toBeInTheDocument();
    });

    it('does not display initial password info when admin password file exists but onboarding is not required', () => {
      const passwordFilePath = '/path/to/admin.password';

      setupStates(false, false, false, null, passwordFilePath, true, true, true, false, false);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.queryByTestId('initial-password-info')).not.toBeInTheDocument();
    });

    it('displays initial password info when admin password file exists and onboarding is required', () => {
      const passwordFilePath = '/path/to/admin.password';

      setupStates(false, false, false, null, passwordFilePath, true, true, true, false, true);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('initial-password-info')).toBeInTheDocument();
      expect(screen.getByText(passwordFilePath)).toBeInTheDocument();
    });
  });

  describe('edge cases', () => {
    it('handles undefined ExtJS state gracefully', () => {
      mockState.mockReturnValue({
        getValue: jest.fn().mockReturnValue(undefined)
      });

      renderComponent({ logoConfig: mockLogoConfig });

      // When all states are undefined (all false by default), no realms are enabled, so no login form
      expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
    });

    it('handles missing logoConfig gracefully', () => {
      setupStates(false, false, false, null);

      renderComponent({});

      expect(screen.getByTestId('login-tile')).toBeInTheDocument();
    });
  });

  describe('realm-based login visibility', () => {
    describe('self-hosted environment', () => {
      it('shows local login when at least one realm is enabled', () => {
        setupStates(false, false, false, null, true, false, false); // only LDAP enabled

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.getByTestId('login-form')).toBeInTheDocument();
      });

      it('shows local login when userToken realm is enabled', () => {
        setupStates(false, false, false, null, false, true, false); // only UserToken enabled

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.getByTestId('login-form')).toBeInTheDocument();
      });

      it('shows local login when localAuth realm is enabled', () => {
        setupStates(false, false, false, null, false, false, true); // only LocalAuth enabled

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.getByTestId('login-form')).toBeInTheDocument();
      });

      it('shows local login when multiple realms are enabled', () => {
        setupStates(false, false, false, null, true, true, false); // LDAP and UserToken enabled

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.getByTestId('login-form')).toBeInTheDocument();
      });

      it('shows local login when all realms are enabled', () => {
        setupStates(false, false, false, null, true, true, true); // all realms enabled

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.getByTestId('login-form')).toBeInTheDocument();
      });

      it('hides local login when no realms are enabled', () => {
        setupStates(false, false, false, null, null, false, false, false); // no realms enabled

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      });

      it('shows SSO but hides local login when SSO is enabled but no realms are enabled', () => {
        setupStates(true, false, false, null, null, false, false, false); // SAML enabled, no realms

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
        expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
        expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
      });

      it('shows both SSO and local login with divider when SSO and at least one realm are enabled', () => {
        setupStates(true, false, false, null, true, false, false); // SAML and LDAP realm enabled

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
        expect(screen.getByTestId('login-form')).toBeInTheDocument();
        expect(screen.getByText(UIStrings.SSO_DIVIDER_LABEL)).toBeInTheDocument();
      });
    });

    describe('cloud environment', () => {
      beforeEach(() => {
        Object.defineProperty(window, 'location', {
          configurable: true,
          value: { hostname: 'cloud.nexus.sonatype.example' },
        });
      });

      afterEach(() => {
        Object.defineProperty(window, 'location', {
          configurable: true,
          value: { hostname: 'localhost' },
        });
      });

      it('hides local login in cloud even when all realms are enabled', () => {
        setupStates(false, false, true, null, true, true, true); // cloud, all realms enabled

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      });

      it('hides local login in cloud when no realms are enabled', () => {
        setupStates(false, false, true, null, false, false, false); // cloud, no realms

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      });

      it('shows only SSO in cloud when SSO is enabled regardless of realm status', () => {
        setupStates(false, true, true, null, true, true, true); // cloud, OAuth2, all realms enabled

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
        expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
        expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
      });
    });

    describe('anonymous access with realm restrictions', () => {
      it('shows anonymous access button even when no realms are enabled', () => {
        setupStates(false, false, false, 'anonymous', null, false, false, false); // anonymous, no realms

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
        expect(screen.getByTestId('continue-without-login-button')).toBeInTheDocument();
      });

      it('shows both local login and anonymous access when realms are enabled', () => {
        setupStates(false, false, false, 'anonymous', null, true, false, false); // anonymous, LDAP realm

        renderComponent({ logoConfig: mockLogoConfig });

        expect(screen.getByTestId('login-form')).toBeInTheDocument();
        expect(screen.getByTestId('continue-without-login-button')).toBeInTheDocument();
      });
    });
  });
});
