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
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { useRouter } from '@uirouter/react';
import SsoLogin from './SsoLogin';

jest.mock('@sonatype/nexus-ui-plugin');
jest.mock('@uirouter/react');

describe('SsoLogin', () => {
  const mockUseState = jest.fn();
  const mockState = jest.fn();
  const mockRouterParams = {};

  beforeEach(() => {
    ExtJS.useState = mockUseState;
    ExtJS.state = mockState;
    mockState.mockReturnValue({
      getValue: jest.fn()
    });

    useRouter.mockReturnValue({
      globals: {
        params: mockRouterParams
      }
    });

    Object.defineProperty(window, 'location', {
      value: {
        assign: jest.fn()
      },
      writable: true
    });

    jest.clearAllMocks();
  });

  describe('visibility', () => {
    it('renders button when SAML is enabled', () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud

      render(<SsoLogin />);
      expect(screen.getByRole('button', { name: /continue with sso/i })).toBeInTheDocument();
    });

    it('renders button when OAuth2 is enabled', () => {
      mockUseState
        .mockReturnValueOnce(false) // samlEnabled
        .mockReturnValueOnce(true)  // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud

      render(<SsoLogin />);
      expect(screen.getByRole('button', { name: /continue with sso/i })).toBeInTheDocument();
    });

    it('renders button when both SAML and OAuth2 are enabled', () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(true)  // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud

      render(<SsoLogin />);
      expect(screen.getByRole('button', { name: /continue with sso/i })).toBeInTheDocument();
    });
  });

  describe('button properties', () => {
    beforeEach(() => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud
    });

    it('has correct attributes', () => {
      render(<SsoLogin />);
      const button = screen.getByRole('button');

      expect(button).toHaveAttribute('type', 'button');
      expect(button).toHaveAttribute('data-analytics-id', 'nxrm-login-sso');
      expect(button).toHaveClass('sso-login-button');
    });

    it('displays correct text when not loading (self-hosted)', () => {
      render(<SsoLogin />);
      expect(screen.getByText('Continue with SSO')).toBeInTheDocument();
    });

    it('displays "Login" text in cloud environment', () => {
      mockUseState.mockReset();
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(true); // isCloud

      render(<SsoLogin />);
      expect(screen.getByText('Login')).toBeInTheDocument();
    });
  });

  describe('SAML redirect', () => {
    it('redirects to /saml with empty hash parameter', async () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud
      mockRouterParams.returnTo = undefined;

      render(<SsoLogin />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml?');
      });
    });

    it('redirects to /saml with hash parameter', async () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud
      mockRouterParams.returnTo = '#admin/repository/repositories';

      render(<SsoLogin />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml?hash=%23admin%2Frepository%2Frepositories');
      });
    });

    it('redirects to context path + /saml when context path is set', async () => {
      mockUseState
        .mockReturnValueOnce(true)      // samlEnabled
        .mockReturnValueOnce(false)     // oauth2Enabled
        .mockReturnValueOnce('/nexus')  // contextPath
        .mockReturnValueOnce(false);    // isCloud
      mockRouterParams.returnTo = undefined;

      render(<SsoLogin />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/nexus/saml?');
      });
    });

    it('redirects to context path + /saml with hash parameter when context path is set', async () => {
      mockUseState
        .mockReturnValueOnce(true)      // samlEnabled
        .mockReturnValueOnce(false)     // oauth2Enabled
        .mockReturnValueOnce('/nexus')  // contextPath
        .mockReturnValueOnce(false);    // isCloud
      mockRouterParams.returnTo = '#admin/repository/repositories';

      render(<SsoLogin />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/nexus/saml?hash=%23admin%2Frepository%2Frepositories');
      });
    });
  });

  describe('OAuth2 redirect', () => {
    it('redirects to /oidc/login with empty hash parameter', async () => {
      mockUseState
        .mockReturnValueOnce(false) // samlEnabled
        .mockReturnValueOnce(true)  // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud
      mockRouterParams.returnTo = undefined;

      render(<SsoLogin />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/oidc/login?');
      });
    });

    it('redirects to /oidc/login with hash parameter', async () => {
      mockUseState
        .mockReturnValueOnce(false) // samlEnabled
        .mockReturnValueOnce(true)  // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud
      mockRouterParams.returnTo = '#admin/repository/repositories';

      render(<SsoLogin />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/oidc/login?hash=%23admin%2Frepository%2Frepositories');
      });
    });

    it('redirects to context path + /oidc/login when context path is set', async () => {
      mockUseState
        .mockReturnValueOnce(false)     // samlEnabled
        .mockReturnValueOnce(true)      // oauth2Enabled
        .mockReturnValueOnce('/nexus')  // contextPath
        .mockReturnValueOnce(false);    // isCloud
      mockRouterParams.returnTo = undefined;

      render(<SsoLogin />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/nexus/oidc/login?');
      });
    });

    it('redirects to context path + /oidc/login with hash parameter when context path is set', async () => {
      mockUseState
        .mockReturnValueOnce(false)     // samlEnabled
        .mockReturnValueOnce(true)      // oauth2Enabled
        .mockReturnValueOnce('/nexus')  // contextPath
        .mockReturnValueOnce(false);    // isCloud
      mockRouterParams.returnTo = '#admin/repository/repositories';

      render(<SsoLogin />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/nexus/oidc/login?hash=%23admin%2Frepository%2Frepositories');
      });
    });
  });

  describe('SAML priority', () => {
    it('prefers SAML when both SAML and OAuth2 are enabled', async () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(true)  // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud
      mockRouterParams.returnTo = undefined;

      render(<SsoLogin />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml?');
      });
    });
  });

  describe('accessibility', () => {
    beforeEach(() => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud
    });

    it('is keyboard accessible', () => {
      render(<SsoLogin />);
      const button = screen.getByRole('button');

      // Check that the button can receive focus and has proper attributes
      expect(button).toHaveAttribute('type', 'button');
      expect(button).not.toHaveAttribute('tabindex', '-1');
    });
  });

  describe('analytics', () => {
    beforeEach(() => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud
    });

    it('has analytics attribute', () => {
      render(<SsoLogin />);
      const button = screen.getByRole('button');

      expect(button).toHaveAttribute('data-analytics-id', 'nxrm-login-sso');
    });
  });


  describe('button behavior on click', () => {
    it('calls window.location.assign when clicked', () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud

      render(<SsoLogin />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      expect(window.location.assign).toHaveBeenCalledWith('/saml?');
      expect(window.location.assign).toHaveBeenCalledTimes(1);
    });

    it('prevents multiple redirects on rapid clicks', () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud

      render(<SsoLogin />);
      const button = screen.getByRole('button');

      // Simulate rapid clicks
      fireEvent.click(button);
      fireEvent.click(button);
      fireEvent.click(button);

      // Only the first click should trigger redirect (button gets disabled)
      expect(window.location.assign).toHaveBeenCalledTimes(1);
    });
  });

  describe('edge cases', () => {
    it('handles returnTo with special characters', async () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud

      useRouter.mockReturnValue({
        globals: {
          params: {
            returnTo: '%23browse%2Fsearch%3Fq%3Dtest%2520package'
          }
        }
      });

      render(<SsoLogin />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml?hash=%2523browse%252Fsearch%253Fq%253Dtest%252520package');
      });
    });
  });

  describe('autoFocus behavior', () => {
    beforeEach(() => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('/')   // contextPath
        .mockReturnValueOnce(false); // isCloud
    });

    it('always focuses button on mount', async () => {
      render(<SsoLogin />);
      const button = screen.getByRole('button');

      // Wait for autofocus to take effect
      await waitFor(() => {
        expect(document.activeElement).toBe(button);
      }, { timeout: 1000 });
    });
  });
});
