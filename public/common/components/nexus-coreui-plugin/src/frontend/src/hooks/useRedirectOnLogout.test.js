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
import { renderHook } from '@testing-library/react-hooks';
import { ExtJS, isVisible } from '@sonatype/nexus-ui-plugin';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { isExtJSLoaded, onExtJSLoad } from '../utils/extJsLoader';
import { useRedirectOnLogout } from './useRedirectOnLogout';

jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: jest.fn(),
  useRouter: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    useUser: jest.fn(),
  },
  isVisible: jest.fn(() => false),
}));

jest.mock('../utils/extJsLoader', () => ({
  isExtJSLoaded: jest.fn(() => true),
  onExtJSLoad: jest.fn()
}));

describe('useRedirectOnLogout', () => {
  let goMock, onMock, offMock, urlMock;

  beforeEach(() => {
    jest.useFakeTimers();

    // Reset mocks
    goMock = jest.fn();
    onMock = jest.fn();
    offMock = jest.fn();
    urlMock = jest.fn().mockReturnValue('/admin/repository/repositories');

    isExtJSLoaded.mockReturnValue(true);
    onExtJSLoad.mockClear();

    useRouter.mockReturnValue({
      stateService: {
        go: goMock,
      },
      urlService: {
        url: urlMock,
      },
      globals: {
        $current: {
          name: 'admin.repository.repositories',
          data: {
            visibilityRequirements: {
              permissions: ['nexus:repository-admin:*:*:read'],
            },
          },
          parent: { name: '' },
        },
      },
    });

    useCurrentStateAndParams.mockReturnValue({
      state: {
        name: 'admin.repository.repositories',
        data: {
          visibilityRequirements: {
            permissions: ['nexus:repository-admin:*:*:read'],
          },
        },
      },
    });

    global.Ext = {
      getApplication: () => ({
        getController: () => ({
          on: onMock,
          un: offMock,
        }),
      }),
    };

    window.dirty = [];
  });

  afterEach(() => {
    jest.clearAllTimers();
    jest.clearAllMocks();
  });

  it('should redirect to login with returnTo parameter if user is not authenticated and route has permission requirements', () => {
    ExtJS.useUser.mockReturnValue(null); // no autenticado

    renderHook(() => useRedirectOnLogout());

    const [event, permissionsChangedHandler] = onMock.mock.calls[0];
    expect(event).toBe('changed');
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    const expectedReturnTo = btoa('#/admin/repository/repositories');
    expect(goMock).toHaveBeenCalledWith('login', { returnTo: expectedReturnTo });
  });

  it('should clear unsaved changes before redirecting', () => {
    ExtJS.useUser.mockReturnValue(null);
    window.dirty = ['some unsaved changes'];

    renderHook(() => useRedirectOnLogout());

    const [, permissionsChangedHandler] = onMock.mock.calls[0];
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    expect(window.dirty).toEqual([]);
    const expectedReturnTo = btoa('#/admin/repository/repositories');
    expect(goMock).toHaveBeenCalledWith('login', { returnTo: expectedReturnTo });
  });

  it('should not redirect if user is authenticated', () => {
    ExtJS.useUser.mockReturnValue({ id: 'mockUser' }); // autenticado

    renderHook(() => useRedirectOnLogout());

    const [event, permissionsChangedHandler] = onMock.mock.calls[0];
    expect(event).toBe('changed');
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    expect(goMock).not.toHaveBeenCalled();
  });

  it('should not redirect if route has no permission requirements', () => {
    ExtJS.useUser.mockReturnValue(null);

    // Override with state that has no permission requirements
    useRouter.mockReturnValue({
      stateService: { go: goMock },
      urlService: { url: urlMock },
      globals: {
        $current: {
          name: 'browse.welcome',
          data: { visibilityRequirements: {} },
          parent: { name: '' },
        },
      },
    });
    useCurrentStateAndParams.mockReturnValue({
      state: { name: 'browse.welcome', data: { visibilityRequirements: {} } },
    });

    renderHook(() => useRedirectOnLogout());

    const [event, permissionsChangedHandler] = onMock.mock.calls[0];
    expect(event).toBe('changed');
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    expect(goMock).not.toHaveBeenCalled();
  });

  it('should clean up event listener on unmount', () => {
    ExtJS.useUser.mockReturnValue(null);

    const { unmount } = renderHook(() => useRedirectOnLogout());

    unmount();

    const handler = onMock.mock.calls[0][1];
    expect(offMock).toHaveBeenCalledWith('changed', handler);
  });

  it('should redirect to login when route has parent with permission requirements (ancestor check)', () => {
    ExtJS.useUser.mockReturnValue(null);

    useRouter.mockReturnValue({
      stateService: { go: goMock },
      urlService: { url: urlMock },
      globals: {
        $current: {
          name: 'preview.admin.security.roles.profile',
          data: { title: 'Role Profile' },
          parent: {
            name: 'preview.admin.security.roles',
            data: { title: 'Roles' },
            parent: {
              name: 'preview.admin.security',
              data: {
                visibilityRequirements: { permissions: ['nexus:security:read'] },
              },
              parent: { name: 'preview.admin', parent: { name: '' } },
            },
          },
        },
      },
    });
    useCurrentStateAndParams.mockReturnValue({
      state: { name: 'preview.admin.security.roles.profile', data: { title: 'Role Profile' } },
    });

    renderHook(() => useRedirectOnLogout());

    const [, permissionsChangedHandler] = onMock.mock.calls[0];
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    const expectedReturnTo = btoa('#/admin/repository/repositories');
    expect(goMock).toHaveBeenCalledWith('login', { returnTo: expectedReturnTo });
  });

  it('should redirect to login when route has requiresUser in requirements', () => {
    ExtJS.useUser.mockReturnValue(null);

    useRouter.mockReturnValue({
      stateService: { go: goMock },
      urlService: { url: urlMock },
      globals: {
        $current: {
          name: 'user.account',
          data: {
            visibilityRequirements: { requiresUser: true },
          },
          parent: { name: '' },
        },
      },
    });
    useCurrentStateAndParams.mockReturnValue({
      state: { name: 'user.account', data: { visibilityRequirements: { requiresUser: true } } },
    });

    renderHook(() => useRedirectOnLogout());

    const [, permissionsChangedHandler] = onMock.mock.calls[0];
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    expect(goMock).toHaveBeenCalledWith('login', expect.objectContaining({ returnTo: expect.any(String) }));
  });

  it('should redirect to login when route has permissionPrefix in requirements', () => {
    ExtJS.useUser.mockReturnValue(null);

    useRouter.mockReturnValue({
      stateService: { go: goMock },
      urlService: { url: urlMock },
      globals: {
        $current: {
          name: 'admin.repository.repositories',
          data: {
            visibilityRequirements: { permissionPrefix: 'nexus:repository-admin' },
          },
          parent: { name: '' },
        },
      },
    });
    useCurrentStateAndParams.mockReturnValue({
      state: { name: 'admin.repository.repositories', data: { visibilityRequirements: { permissionPrefix: 'nexus:repository-admin' } } },
    });

    renderHook(() => useRedirectOnLogout());

    const [, permissionsChangedHandler] = onMock.mock.calls[0];
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    expect(goMock).toHaveBeenCalledWith('login', expect.objectContaining({ returnTo: expect.any(String) }));
  });

  it('should redirect to login when route has requiresAnyPermission in requirements', () => {
    ExtJS.useUser.mockReturnValue(null);

    useRouter.mockReturnValue({
      stateService: { go: goMock },
      urlService: { url: urlMock },
      globals: {
        $current: {
          name: 'admin.security.realms',
          data: {
            visibilityRequirements: { requiresAnyPermission: ['nexus:settings:read', 'nexus:security:read'] },
          },
          parent: { name: '' },
        },
      },
    });
    useCurrentStateAndParams.mockReturnValue({
      state: { name: 'admin.security.realms', data: { visibilityRequirements: { requiresAnyPermission: ['nexus:settings:read', 'nexus:security:read'] } } },
    });

    renderHook(() => useRedirectOnLogout());

    const [, permissionsChangedHandler] = onMock.mock.calls[0];
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    expect(goMock).toHaveBeenCalledWith('login', expect.objectContaining({ returnTo: expect.any(String) }));
  });

  it('should not redirect if route only has non-permission visibilityRequirements (e.g., ignoreForMenuVisibilityCheck)', () => {
    ExtJS.useUser.mockReturnValue(null);

    useRouter.mockReturnValue({
      stateService: { go: goMock },
      urlService: { url: urlMock },
      globals: {
        $current: {
          name: 'browse',
          data: {
            visibilityRequirements: { ignoreForMenuVisibilityCheck: true },
          },
          parent: { name: '' },
        },
      },
    });
    useCurrentStateAndParams.mockReturnValue({
      state: { name: 'browse', data: { visibilityRequirements: { ignoreForMenuVisibilityCheck: true } } },
    });

    renderHook(() => useRedirectOnLogout());

    const [, permissionsChangedHandler] = onMock.mock.calls[0];
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    expect(goMock).not.toHaveBeenCalled();
  });

  it('should encode returnTo correctly with query parameters', () => {
    ExtJS.useUser.mockReturnValue(null);
    urlMock.mockReturnValue('/admin/security/roles?filter=maven&sort=name');

    renderHook(() => useRedirectOnLogout());

    const [, permissionsChangedHandler] = onMock.mock.calls[0];
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    const expectedReturnTo = btoa('#/admin/security/roles?filter=maven&sort=name');
    expect(goMock).toHaveBeenCalledWith('login', { returnTo: expectedReturnTo });
  });

  it('should produce a returnTo that decodes to a plain hash URL (no double-encoding)', () => {
    // The server-side OIDC callback (OidcCallbackFilter) only does Base64 decode on returnTo.
    // If encodeURIComponent is applied before btoa, the server gets "%23" instead of "#",
    // treats it as a literal path, and the user hits a 404 after SSO login.
    ExtJS.useUser.mockReturnValue(null);

    renderHook(() => useRedirectOnLogout());

    const [, permissionsChangedHandler] = onMock.mock.calls[0];
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    const returnTo = goMock.mock.calls[0][1].returnTo;
    const decoded = atob(returnTo);
    expect(decoded).toMatch(/^#/);
    expect(decoded).not.toMatch(/^%23/);
  });

  it('should not redirect when anonymous access grants sufficient permissions (browse/search)', () => {
    ExtJS.useUser.mockReturnValue(null);
    isVisible.mockReturnValue(true);

    useRouter.mockReturnValue({
      stateService: { go: goMock },
      urlService: { url: urlMock },
      globals: {
        $current: {
          name: 'browse.search.generic',
          data: {
            visibilityRequirements: { permissions: ['nexus:search:read'] },
          },
          parent: {
            name: 'browse.search',
            data: { visibilityRequirements: {} },
            parent: { name: 'browse', data: { visibilityRequirements: { ignoreForMenuVisibilityCheck: true } }, parent: { name: '' } },
          },
        },
      },
    });
    useCurrentStateAndParams.mockReturnValue({
      state: { name: 'browse.search.generic', data: { visibilityRequirements: { permissions: ['nexus:search:read'] } } },
    });

    renderHook(() => useRedirectOnLogout());

    const [, permissionsChangedHandler] = onMock.mock.calls[0];
    permissionsChangedHandler();

    jest.advanceTimersByTime(150);

    expect(goMock).not.toHaveBeenCalled();
  });
});
