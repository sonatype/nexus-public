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
import {render, screen, fireEvent} from '@testing-library/react';
import '@testing-library/jest-dom';
import LoginPageRadix from './LoginPageRadix';
import useLoginPage from './useLoginPage';

jest.mock('./useLoginPage');
jest.mock('../../contexts/ThemeContext', () => ({
  useTheme: () => ({effectiveTheme: 'light', setTheme: jest.fn()}),
}));
jest.mock('../ThemeSwitcher/ThemeSwitcher', () => ({
  ThemeSwitcher: () => <button aria-label="Switch to dark mode">theme</button>,
}));
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    urlOf: (path) => `/${path}`,
  },
  UIStrings: {
    SIGNIN_TITLE: 'Sign in to Nexus Repository',
    SIGNIN_BUTTON: 'Sign in',
    SIGNIN_BUTTON_LOADING: 'Signing in...',
    SIGNIN_FOOTER_LEAD: 'New to Nexus Repository?',
    SSO_BUTTON: 'Continue with SSO',
    SSO_BUTTON_LOADING: 'Redirecting...',
    SSO_DIVIDER_LABEL: 'or',
    USERNAME_LABEL: 'Username',
    PASSWORD_LABEL: 'Password',
    CONTINUE_WITHOUT_LOGIN: 'Continue without login',
    INITIAL_PASSWORD_MESSAGE: 'To log in for the first time, use the generated admin password located at:',
  },
}));

describe('LoginPageRadix', () => {
  const mockField = (name, value = '') => ({
    name,
    value,
    error: undefined,
    onChange: jest.fn(),
    onBlur: jest.fn(),
  });

  const defaultHookReturn = {
    field: jest.fn((name) => mockField(name)),
    submit: jest.fn(),
    isSaving: false,
    saveError: null,
    data: {username: '', password: ''},
    isSsoRedirecting: false,
    edition: 'PRO',
    showSsoLogin: false,
    showAnonymousAccess: false,
    handleSsoLogin: jest.fn(),
    handleContinueWithoutLogin: jest.fn(),
    adminPasswordFilePath: null,
    onboardingRequired: false,
    isCloudEnvironment: false,
  };

  beforeEach(() => {
    useLoginPage.mockReturnValue(defaultHookReturn);
    jest.clearAllMocks();
  });

  it('renders the sign-in heading', () => {
    render(<LoginPageRadix />);
    expect(screen.getByRole('heading', {name: /sign in to nexus repository/i})).toBeInTheDocument();
  });

  it('renders username and password fields', () => {
    render(<LoginPageRadix />);
    expect(screen.getByPlaceholderText('Username')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Password')).toBeInTheDocument();
  });

  it('renders sign in button', () => {
    render(<LoginPageRadix />);
    expect(screen.getByRole('button', {name: /^sign in$/i})).toBeInTheDocument();
  });

  it('renders the mark-only logo in the card', () => {
    render(<LoginPageRadix />);
    const markLogo = document.querySelector('.nxrm-login-card__mark');
    expect(markLogo).toBeInTheDocument();
    expect(markLogo).toHaveAttribute('src', '/static/rapture/resources/favicon.svg');
  });

  it('renders ThemeSwitcher in the header', () => {
    render(<LoginPageRadix />);
    expect(screen.getByRole('button', {name: /switch to dark mode/i})).toBeInTheDocument();
  });

  it('renders semantic header with role="banner"', () => {
    render(<LoginPageRadix />);
    expect(screen.getByRole('banner')).toBeInTheDocument();
  });

  it('renders accessible form fields with autoComplete', () => {
    render(<LoginPageRadix />);
    expect(screen.getByPlaceholderText('Username')).toHaveAttribute('autocomplete', 'username');
    expect(screen.getByPlaceholderText('Password')).toHaveAttribute('autocomplete', 'current-password');
  });

  it('calls submit on form submission', () => {
    const submit = jest.fn();
    useLoginPage.mockReturnValue({...defaultHookReturn, submit, data: {username: 'a', password: 'b'}});
    render(<LoginPageRadix />);

    fireEvent.submit(screen.getByRole('button', {name: /^sign in$/i}).closest('form'));

    expect(submit).toHaveBeenCalledTimes(1);
  });

  describe('SSO rendering', () => {
    it('does not show SSO button when showSsoLogin is false', () => {
      render(<LoginPageRadix />);
      expect(screen.queryByRole('button', {name: /sso/i})).not.toBeInTheDocument();
    });

    it('shows SSO button when showSsoLogin is true', () => {
      useLoginPage.mockReturnValue({...defaultHookReturn, showSsoLogin: true});
      render(<LoginPageRadix />);
      expect(screen.getByRole('button', {name: /sso/i})).toBeInTheDocument();
    });

    it('calls handleSsoLogin when SSO button is clicked', () => {
      const handleSsoLogin = jest.fn();
      useLoginPage.mockReturnValue({...defaultHookReturn, showSsoLogin: true, handleSsoLogin});
      render(<LoginPageRadix />);

      fireEvent.click(screen.getByRole('button', {name: /sso/i}));
      expect(handleSsoLogin).toHaveBeenCalledTimes(1);
    });

    it('shows divider when showSsoLogin is true', () => {
      useLoginPage.mockReturnValue({...defaultHookReturn, showSsoLogin: true});
      render(<LoginPageRadix />);
      expect(screen.getByText('or')).toBeInTheDocument();
    });

    it('does not show divider when showSsoLogin is false', () => {
      render(<LoginPageRadix />);
      expect(screen.queryByText('or')).not.toBeInTheDocument();
    });
  });

  describe('anonymous access rendering', () => {
    it('shows footer with "New to Nexus Repository?" and continue link when showAnonymousAccess is true', () => {
      useLoginPage.mockReturnValue({...defaultHookReturn, showAnonymousAccess: true});
      render(<LoginPageRadix />);

      expect(screen.getByText(/new to nexus repository\?/i)).toBeInTheDocument();
      expect(screen.getByText(/continue without login/i)).toBeInTheDocument();
    });

    it('does not show continue without login when showAnonymousAccess is false', () => {
      render(<LoginPageRadix />);
      expect(screen.queryByText(/continue without login/i)).not.toBeInTheDocument();
    });

    it('continue without login links to #browse/welcome', () => {
      useLoginPage.mockReturnValue({...defaultHookReturn, showAnonymousAccess: true});
      render(<LoginPageRadix />);

      const link = screen.getByText(/continue without login/i);
      expect(link).toHaveAttribute('href', '#browse/welcome');
    });
  });

  describe('error display', () => {
    it('shows saveError when set', () => {
      useLoginPage.mockReturnValue({...defaultHookReturn, saveError: 'Invalid username or password'});
      render(<LoginPageRadix />);
      expect(screen.getByText('Invalid username or password')).toBeInTheDocument();
    });

    it('does not show error when saveError is null', () => {
      render(<LoginPageRadix />);
      expect(screen.queryByText('Invalid username or password')).not.toBeInTheDocument();
    });
  });

  describe('loading state', () => {
    it('shows "Signing in..." when isSaving is true', () => {
      useLoginPage.mockReturnValue({...defaultHookReturn, isSaving: true, data: {username: 'a', password: 'b'}});
      render(<LoginPageRadix />);
      expect(screen.getByRole('button', {name: /signing in/i})).toBeInTheDocument();
    });
  });

  describe('initial admin password info', () => {
    const testFilePath = '/opt/sonatype/nexus/admin.password';

    it('shows admin password info when all conditions are met', () => {
      useLoginPage.mockReturnValue({
        ...defaultHookReturn,
        adminPasswordFilePath: testFilePath,
        onboardingRequired: true,
        isCloudEnvironment: false,
      });
      render(<LoginPageRadix />);

      expect(screen.getByText('To log in for the first time, use the generated admin password located at:')).toBeInTheDocument();
      expect(screen.getByText(testFilePath)).toBeInTheDocument();
    });

    it('hides admin password info when file path is missing', () => {
      useLoginPage.mockReturnValue({
        ...defaultHookReturn,
        adminPasswordFilePath: null,
        onboardingRequired: true,
        isCloudEnvironment: false,
      });
      render(<LoginPageRadix />);

      expect(screen.queryByText('To log in for the first time, use the generated admin password located at:')).not.toBeInTheDocument();
    });

    it('hides admin password info when file path is empty string', () => {
      useLoginPage.mockReturnValue({
        ...defaultHookReturn,
        adminPasswordFilePath: '',
        onboardingRequired: true,
        isCloudEnvironment: false,
      });
      render(<LoginPageRadix />);

      expect(screen.queryByText('To log in for the first time, use the generated admin password located at:')).not.toBeInTheDocument();
    });

    it('hides admin password info in cloud environment', () => {
      useLoginPage.mockReturnValue({
        ...defaultHookReturn,
        adminPasswordFilePath: testFilePath,
        onboardingRequired: true,
        isCloudEnvironment: true,
      });
      render(<LoginPageRadix />);

      expect(screen.queryByText('To log in for the first time, use the generated admin password located at:')).not.toBeInTheDocument();
    });

    it('hides admin password info when onboarding is not required', () => {
      useLoginPage.mockReturnValue({
        ...defaultHookReturn,
        adminPasswordFilePath: testFilePath,
        onboardingRequired: false,
        isCloudEnvironment: false,
      });
      render(<LoginPageRadix />);

      expect(screen.queryByText('To log in for the first time, use the generated admin password located at:')).not.toBeInTheDocument();
    });

    it('displays the correct file path', () => {
      const customPath = '/custom/path/to/admin.password';
      useLoginPage.mockReturnValue({
        ...defaultHookReturn,
        adminPasswordFilePath: customPath,
        onboardingRequired: true,
        isCloudEnvironment: false,
      });
      render(<LoginPageRadix />);

      expect(screen.getByText(customPath)).toBeInTheDocument();
    });

    it('renders file path in monospace font', () => {
      useLoginPage.mockReturnValue({
        ...defaultHookReturn,
        adminPasswordFilePath: testFilePath,
        onboardingRequired: true,
        isCloudEnvironment: false,
      });
      render(<LoginPageRadix />);

      const filePathElement = screen.getByText(testFilePath);
      expect(filePathElement).toHaveClass('nxrm-login-filepath');
    });
  });
});
