/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import {renderHook, act} from '@testing-library/react';
import useLoginPage from './useLoginPage';

let mockStateValues = {};
const mockAssign = jest.fn();
const mockRequestSession = jest.fn();

jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ...actual,
    ExtJS: {
      state: () => ({
        getValue: (key, defaultVal) => mockStateValues[key] ?? defaultVal,
        getEdition: () => mockStateValues['edition'] || 'PRO',
      }),
      useState: (getter) => getter(),
      requestSession: (...args) => mockRequestSession(...args),
    },
  };
});

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    globals: {
      params: mockStateValues['_routerParams'] || {},
    },
  }),
}));

describe('useLoginPage', () => {
  const originalLocation = window.location;

  beforeEach(() => {
    mockStateValues = {
      'localAuthRealmEnabled': true,
    };
    delete window.location;
    window.location = {
      ...originalLocation,
      assign: mockAssign,
      hash: '',
      hostname: 'localhost',
    };
    jest.clearAllMocks();
  });

  afterEach(() => {
    window.location = originalLocation;
  });

  function setupStates(overrides = {}) {
    mockStateValues = {
      'localAuthRealmEnabled': true,
      ...overrides,
    };
  }

  describe('form state from useForm', () => {
    it('provides field helper for username', () => {
      setupStates();
      const {result} = renderHook(() => useLoginPage());

      const usernameField = result.current.field('username');
      expect(usernameField.value).toBe('');
      expect(usernameField.name).toBe('username');
      expect(typeof usernameField.onChange).toBe('function');
    });

    it('provides field helper for password', () => {
      setupStates();
      const {result} = renderHook(() => useLoginPage());

      const passwordField = result.current.field('password');
      expect(passwordField.value).toBe('');
      expect(passwordField.name).toBe('password');
    });

    it('provides submit function', () => {
      setupStates();
      const {result} = renderHook(() => useLoginPage());
      expect(typeof result.current.submit).toBe('function');
    });

    it('provides isSaving flag', () => {
      setupStates();
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.isSaving).toBe(false);
    });

    it('provides saveError', () => {
      setupStates();
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.saveError).toBeNull();
    });
  });

  describe('login via form machine', () => {
    it('calls requestSession on submit with valid credentials', async () => {
      mockRequestSession.mockResolvedValue({response: {status: 200}});
      setupStates();
      const {result} = renderHook(() => useLoginPage());

      act(() => result.current.field('username').onChange('admin'));
      act(() => result.current.field('password').onChange('admin123'));

      await act(async () => {
        result.current.submit();
      });

      expect(mockRequestSession).toHaveBeenCalledWith('admin', 'admin123');
    });

    it('sets saveError on failed login', async () => {
      mockRequestSession.mockRejectedValue(new Error('Unauthorized'));
      setupStates();
      const {result} = renderHook(() => useLoginPage());

      act(() => result.current.field('username').onChange('bad'));
      act(() => result.current.field('password').onChange('bad'));

      await act(async () => {
        result.current.submit();
      });

      expect(result.current.saveError).toBe('Invalid username or password');
    });
  });

  describe('SSO state', () => {
    it('returns showSsoLogin false when neither SAML nor OAuth2 is enabled', () => {
      setupStates();
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.showSsoLogin).toBe(false);
    });

    it('returns showSsoLogin true when SAML is enabled', () => {
      setupStates({samlEnabled: true});
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.showSsoLogin).toBe(true);
    });

    it('returns showSsoLogin true when OAuth2 is enabled', () => {
      setupStates({oauth2Enabled: true});
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.showSsoLogin).toBe(true);
    });
  });

  describe('SSO redirect', () => {
    it('redirects to /saml when SAML is enabled', () => {
      setupStates({samlEnabled: true, 'nexus-context-path': '/'});
      const {result} = renderHook(() => useLoginPage());

      act(() => result.current.handleSsoLogin());

      expect(mockAssign).toHaveBeenCalledWith('/saml?');
    });

    it('redirects to /oidc/login when OAuth2 is enabled', () => {
      setupStates({oauth2Enabled: true, 'nexus-context-path': '/'});
      const {result} = renderHook(() => useLoginPage());

      act(() => result.current.handleSsoLogin());

      expect(mockAssign).toHaveBeenCalledWith('/oidc/login?');
    });

    it('prefers SAML over OAuth2 when both are enabled', () => {
      setupStates({samlEnabled: true, oauth2Enabled: true, 'nexus-context-path': '/'});
      const {result} = renderHook(() => useLoginPage());

      act(() => result.current.handleSsoLogin());

      expect(mockAssign).toHaveBeenCalledWith('/saml?');
    });

    it('includes returnTo hash parameter in redirect URL', () => {
      setupStates({
        samlEnabled: true,
        'nexus-context-path': '/',
        '_routerParams': {returnTo: '#admin/repository/repositories'},
      });
      const {result} = renderHook(() => useLoginPage());

      act(() => result.current.handleSsoLogin());

      expect(mockAssign).toHaveBeenCalledWith(
        '/saml?hash=%23admin%2Frepository%2Frepositories'
      );
    });

    it('includes context path in redirect URL', () => {
      setupStates({samlEnabled: true, 'nexus-context-path': '/nexus'});
      const {result} = renderHook(() => useLoginPage());

      act(() => result.current.handleSsoLogin());

      expect(mockAssign).toHaveBeenCalledWith('/nexus/saml?');
    });
  });

  describe('anonymous access', () => {
    it('returns showAnonymousAccess true when anonymousUsername is configured', () => {
      setupStates({anonymousUsername: 'anonymous'});
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.showAnonymousAccess).toBe(true);
    });

    it('returns showAnonymousAccess false when anonymousUsername is not configured', () => {
      setupStates({anonymousUsername: false});
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.showAnonymousAccess).toBe(false);
    });
  });

  describe('edition', () => {
    it('returns PRO edition by default', () => {
      setupStates();
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.edition).toBe('PRO');
    });

    it('returns the configured edition', () => {
      setupStates({edition: 'COMMUNITY'});
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.edition).toBe('COMMUNITY');
    });
  });

  describe('initial admin password state', () => {
    it('returns adminPasswordFilePath from state', () => {
      setupStates({'admin.password.file': '/opt/sonatype/nexus/admin.password'});
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.adminPasswordFilePath).toBe('/opt/sonatype/nexus/admin.password');
    });

    it('returns null when admin.password.file is not set', () => {
      setupStates();
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.adminPasswordFilePath).toBeNull();
    });

    it('returns onboardingRequired as true when set in state', () => {
      setupStates({'onboarding.required': true});
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.onboardingRequired).toBe(true);
    });

    it('returns onboardingRequired as false when not set in state', () => {
      setupStates();
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.onboardingRequired).toBe(false);
    });

    it('returns isCloudEnvironment as true when isCloud is set in state', () => {
      setupStates({'isCloud': true});
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.isCloudEnvironment).toBe(true);
    });

    it('returns isCloudEnvironment as false when isCloud is not set in state', () => {
      setupStates();
      const {result} = renderHook(() => useLoginPage());
      expect(result.current.isCloudEnvironment).toBe(false);
    });

    it('returns all three initial password state values correctly', () => {
      setupStates({
        'admin.password.file': '/path/to/admin.password',
        'onboarding.required': true,
        'isCloud': false
      });
      const {result} = renderHook(() => useLoginPage());

      expect(result.current.adminPasswordFilePath).toBe('/path/to/admin.password');
      expect(result.current.onboardingRequired).toBe(true);
      expect(result.current.isCloudEnvironment).toBe(false);
    });
  });
});
