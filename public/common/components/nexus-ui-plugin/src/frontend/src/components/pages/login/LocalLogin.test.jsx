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
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import LoginPageStrings from '../../../constants/LoginPageStrings';
import LocalLogin from './LocalLogin';
import { RouteNames } from '../../../constants/RouteNames';

const mockRequestSession = jest.fn();
const mockWaitForNextPermissionChange = jest.fn().mockResolvedValue();

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    get waitForNextPermissionChange() {
      return mockWaitForNextPermissionChange;
    }
  }
}));

jest.mock('../../../interface/ExtJS', () => ({
  __esModule: true,
  default: {
    get requestSession() {
      return mockRequestSession;
    },
    setDirtyStatus: jest.fn(),
    fireEvent: jest.fn()
  }
}));

const mockRouter = {
  globals: { params: {} },
  urlService: { url: jest.fn() },
  stateService: { go: jest.fn() }
};

jest.mock('@uirouter/react', () => ({
  useRouter: () => mockRouter
}));

jest.setTimeout(15000);

describe('LocalLogin', () => {
  const selectors = {
    usernameInput: () => screen.getByPlaceholderText(LoginPageStrings.USERNAME_LABEL),
    passwordInput: () => screen.getByPlaceholderText(LoginPageStrings.PASSWORD_LABEL),
    loginButton: () => screen.getByTestId('login-primary-button'),
    loadingButton: () => screen.queryByText(LoginPageStrings.LOGIN_BUTTON_LOADING),
    serverErrorMessage: () => document.querySelector('.server-error-message')
  };

  beforeEach(() => {
    mockRequestSession.mockReset();
    mockWaitForNextPermissionChange.mockReset().mockResolvedValue();
    mockRouter.globals.params = {};
    mockRouter.urlService.url.mockReset();
    mockRouter.stateService.go.mockReset();
  });

  function renderComponent(props = {}) {
    const defaultProps = {
      primaryButton: true
    };

    return render(<LocalLogin {...defaultProps} {...props} />);
  }

  async function fillCredentials(username, password) {
    const usernameInput = selectors.usernameInput();
    const passwordInput = selectors.passwordInput();

    await userEvent.clear(usernameInput);
    await userEvent.type(usernameInput, username);
    await userEvent.clear(passwordInput);
    await userEvent.type(passwordInput, password);

    await waitFor(() => {
      expect(usernameInput).toHaveValue(username);
      expect(passwordInput).toHaveValue(password);
    });
  }

  describe('rendering', () => {
    it('renders the inputs and submit button', () => {
      renderComponent();

      expect(selectors.usernameInput()).toBeInTheDocument();
      expect(selectors.passwordInput()).toBeInTheDocument();
      expect(selectors.loginButton()).toBeInTheDocument();
      expect(selectors.serverErrorMessage()).not.toBeInTheDocument();
    });

    it('uses primary button styling by default', () => {
      renderComponent();

      expect(selectors.loginButton()).toHaveClass('rt-variant-solid');
    });

    it('switches to outline button styling when primary=false', () => {
      renderComponent({ primaryButton: false });

      const loginButton = selectors.loginButton();
      expect(loginButton).not.toHaveClass('rt-variant-solid');
      expect(loginButton).toHaveClass('rt-variant-outline');
    });

    it('adds analytics ids to form controls', () => {
      renderComponent();

      const usernameInput = document.querySelector('[data-analytics-id="login-username-input"]');
      const passwordInput = document.querySelector('[data-analytics-id="login-password-input"]');
      const submitButton = document.querySelector('[data-analytics-id="nxrm-login-local"]');

      expect(usernameInput).toBeInTheDocument();
      expect(passwordInput).toBeInTheDocument();
      expect(submitButton).toBe(selectors.loginButton());
    });

  });

  describe('validation', () => {
    it('shows username required error when submitting empty username', async () => {
      renderComponent();

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        const usernameInput = selectors.usernameInput();
        expect(usernameInput).toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('shows password required error when submitting empty password', async () => {
      renderComponent();
      const usernameInput = selectors.usernameInput();

      await userEvent.type(usernameInput, 'testuser');
      await waitFor(() => expect(usernameInput).toHaveValue('testuser'));

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        const passwordInput = selectors.passwordInput();
        expect(passwordInput).toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('shows both username and password required errors when both are empty', async () => {
      renderComponent();

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.usernameInput()).toHaveAttribute('aria-invalid', 'true');
        expect(selectors.passwordInput()).toHaveAttribute('aria-invalid', 'true');
      });
    });
  });

  describe('form submission', () => {
    it('invokes authenticate on submit and redirects on success', async () => {
      renderComponent();
      await fillCredentials('admin', 'admin123');

      mockRequestSession.mockResolvedValue({ response: { status: 204 } });

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(mockRequestSession).toHaveBeenCalledWith('admin', 'admin123');
        expect(mockWaitForNextPermissionChange).toHaveBeenCalled();
      });
    });

    it('clears password field on authentication error', async () => {
      renderComponent();
      await fillCredentials('admin', 'badpass');

      const authError = new Error('Forbidden');
      authError.response = { status: 403, data: {} };
      mockRequestSession.mockRejectedValue(authError);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).toHaveTextContent(LoginPageStrings.ERRORS.WRONG_CREDENTIALS);
        expect(selectors.passwordInput()).toHaveValue('');
      });

      expect(selectors.usernameInput()).toHaveValue('admin');

      expect(selectors.usernameInput()).toHaveAttribute('aria-invalid', 'true');
      expect(selectors.passwordInput()).toHaveAttribute('aria-invalid', 'true');
      expect(screen.queryByText(LoginPageStrings.ERRORS.USERNAME_REQUIRED)).not.toBeInTheDocument();
      expect(screen.queryByText(LoginPageStrings.ERRORS.PASSWORD_REQUIRED)).not.toBeInTheDocument();
    });

    it('displays connection errors returned by the server', async () => {
      const mockOnError = jest.fn();
      renderComponent({ onError: mockOnError });
      await fillCredentials('admin', 'oops');

      const failure = new Error('Server error');
      failure.response = { status: 500, data: { message: 'Authentication failed' } };
      mockRequestSession.mockRejectedValue(failure);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(mockOnError).toHaveBeenCalledWith('Authentication failed');
        expect(selectors.serverErrorMessage()).not.toBeInTheDocument();
      });
    });

    it('focuses username input after authentication error', async () => {
      renderComponent();
      await fillCredentials('admin', 'wrongpass');

      const authError = new Error('Forbidden');
      authError.response = { status: 403, data: {} };
      mockRequestSession.mockRejectedValue(authError);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).toBeInTheDocument();
      });

      expect(document.activeElement).toBe(document.getElementById('username'));
    });

    it('allows user to retry after authentication error', async () => {
      renderComponent();
      await fillCredentials('admin', 'wrongpass');

      const authError = new Error('Forbidden');
      authError.response = { status: 403, data: {} };
      mockRequestSession.mockRejectedValue(authError);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.usernameInput()).toHaveAttribute('aria-invalid', 'true');
        expect(selectors.passwordInput()).toHaveAttribute('aria-invalid', 'true');
        expect(selectors.serverErrorMessage()).toHaveTextContent(LoginPageStrings.ERRORS.WRONG_CREDENTIALS);
      });

      mockRequestSession.mockResolvedValue({ response: { status: 204 } });
      await userEvent.type(selectors.passwordInput(), 'correctpass');

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(mockWaitForNextPermissionChange).toHaveBeenCalled();
      });
    });

    it('clears error message when user modifies username after authentication error', async () => {
      renderComponent();
      await fillCredentials('admin', 'wrongpass');

      const authError = new Error('Forbidden');
      authError.response = { status: 403, data: {} };
      mockRequestSession.mockRejectedValue(authError);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).toHaveTextContent(LoginPageStrings.ERRORS.WRONG_CREDENTIALS);
        expect(selectors.usernameInput()).toHaveAttribute('aria-invalid', 'true');
        expect(selectors.passwordInput()).toHaveAttribute('aria-invalid', 'true');
      });

      await userEvent.type(selectors.usernameInput(), 'x');

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).not.toBeInTheDocument();
        expect(selectors.usernameInput()).not.toHaveAttribute('aria-invalid', 'true');
        expect(selectors.passwordInput()).not.toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('clears error message when user modifies password after authentication error', async () => {
      renderComponent();
      await fillCredentials('admin', 'wrongpass');

      const authError = new Error('Forbidden');
      authError.response = { status: 403, data: {} };
      mockRequestSession.mockRejectedValue(authError);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).toHaveTextContent(LoginPageStrings.ERRORS.WRONG_CREDENTIALS);
        expect(selectors.usernameInput()).toHaveAttribute('aria-invalid', 'true');
        expect(selectors.passwordInput()).toHaveAttribute('aria-invalid', 'true');
      });

      // User starts correcting password (which was cleared after error)
      await userEvent.type(selectors.passwordInput(), 'newpass');

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).not.toBeInTheDocument();
        expect(selectors.usernameInput()).not.toHaveAttribute('aria-invalid', 'true');
        expect(selectors.passwordInput()).not.toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('clears error message and allows resubmission after user corrects credentials', async () => {
      renderComponent();
      await fillCredentials('admin', 'wrongpass');

      const authError = new Error('Forbidden');
      authError.response = { status: 403, data: {} };
      mockRequestSession.mockRejectedValue(authError);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).toHaveTextContent(LoginPageStrings.ERRORS.WRONG_CREDENTIALS);
      });

      await userEvent.clear(selectors.usernameInput());
      await userEvent.type(selectors.usernameInput(), 'correctuser');

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).not.toBeInTheDocument();
      });

      await userEvent.type(selectors.passwordInput(), 'correctpass');

      mockRequestSession.mockResolvedValue({ response: { status: 204 } });

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(mockWaitForNextPermissionChange).toHaveBeenCalled();
      });
    });
  });

  describe('field interactions', () => {
    it('updates username and password values as the user types', async () => {
      renderComponent();

      await fillCredentials('my-user', 'my-pass');

      expect(selectors.usernameInput()).toHaveValue('my-user');
      expect(selectors.passwordInput()).toHaveValue('my-pass');
    });
  });

  describe('redirection after login', () => {
    beforeEach(() => {
      mockRouter.globals.params = {};
      mockRouter.urlService.url.mockClear();
      mockRouter.stateService.go.mockClear();
    });

    it('redirects to returnTo URL after successful login', async () => {
      const returnToUrl = '#admin/repository/repositories';
      mockRouter.globals.params.returnTo = btoa(returnToUrl);
      renderComponent();
      await fillCredentials('admin', 'admin123');

      mockRequestSession.mockResolvedValue({ response: { status: 204 } });

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(mockWaitForNextPermissionChange).toHaveBeenCalled();
        expect(mockRouter.urlService.url).toHaveBeenCalledWith(returnToUrl);
        expect(mockRouter.stateService.go).not.toHaveBeenCalled();
      });
    });

    it('redirects to welcome page when no returnTo is provided', async () => {
      renderComponent();
      await fillCredentials('admin', 'admin123');

      mockRequestSession.mockResolvedValue({ response: { status: 204 } });

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(mockWaitForNextPermissionChange).toHaveBeenCalled();
        expect(mockRouter.stateService.go).toHaveBeenCalledWith('browse.welcome');
        expect(mockRouter.urlService.url).not.toHaveBeenCalled();
      });
    });

    it('redirects to missing route page when redirection is unsuccessful', async () => {
      renderComponent();
      await fillCredentials('admin', 'admin123');

      mockRequestSession.mockResolvedValue({ response: { status: 204 } });
      mockWaitForNextPermissionChange.mockRejectedValueOnce(new Error('Permission check failed'));

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(mockWaitForNextPermissionChange).toHaveBeenCalled();
        expect(mockRouter.stateService.go).toHaveBeenCalledWith(RouteNames.MISSING_ROUTE);
      });
    });

    it('redirects to missing route page when redirection URL is not a valid base64', async () => {
      mockRouter.globals.params.returnTo = 'invalid@base64==';
      renderComponent();
      await fillCredentials('admin', 'admin123');

      mockRequestSession.mockResolvedValue({ response: { status: 204 } });

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(mockWaitForNextPermissionChange).toHaveBeenCalled();
        expect(mockRouter.stateService.go).toHaveBeenCalledWith(RouteNames.MISSING_ROUTE);
      });
    });
  });

  describe('autoFocus behavior', () => {
    it('focuses username input when primaryButton is true', async () => {
      renderComponent({ primaryButton: true });
      const usernameInput = document.getElementById('username');

      // Wait for autofocus to take effect
      await waitFor(() => {
        expect(document.activeElement).toBe(usernameInput);
      }, { timeout: 1000 });
    });

    it('does not focus username input when primaryButton is false', () => {
      renderComponent({ primaryButton: false });
      const usernameInput = document.getElementById('username');

      // Username input should not be focused
      expect(document.activeElement).not.toBe(usernameInput);
    });
  });
});
