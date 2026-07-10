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
import { getTestRouter } from './testRouter';
import { UIView } from '@uirouter/react';
import ExtJS from '../interface/ExtJS';

jest.mock('../interface/ExtJS');

describe('createRouter - onBefore - validate permissions and configuration on each route request', () => {
  let router;

  beforeEach(() => {
    jest.clearAllMocks();

    // Default: user has no permissions (NavigationUtils calls NX.Permissions.check directly)
    global.NX.Permissions.check.mockReturnValue(false);

    ExtJS.hasUser = jest.fn().mockReturnValue(false);
    // Default to anonymous access disabled
    ExtJS.state = jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(false)
    });

    router = getTestRouter();
  });

  it('authenticated user goes to login page, should redirect to welcome page', async () => {
    ExtJS.hasUser.mockReturnValue(true);

    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('login').catch(() => {});

    expect(goSpy).toHaveBeenCalledTimes(2);
    expect(goSpy).toHaveBeenNthCalledWith(1, 'login');
    expect(goSpy).toHaveBeenNthCalledWith(2, 'browse.welcome');
  });

  it('go to visible pages, should be allowed', async () => {
    await router.stateService.go('browse.welcome');
    expect(router.stateService.current.name).toBe('browse.welcome');

    await router.stateService.go('login');
    expect(router.stateService.current.name).toBe('login');
  });

  it('authenticated user goes to unauthorized page, should redirect to welcome', async () => {
    ExtJS.hasUser.mockReturnValue(true);
    // Navigate to a known state first to clear any residual state
    await router.stateService.go('browse.welcome');

    const protectedRoute = {
      name: 'protected',
      url: '/protected',
      component: () => null,
      data: { visibilityRequirements: { permissions: ['admin:all'] } }
    };
    router.stateRegistry.register(protectedRoute);
    await router.urlService.sync();

    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('protected').catch(() => {});

    expect(goSpy).toHaveBeenCalledTimes(2);
    expect(goSpy).toHaveBeenNthCalledWith(1, 'protected');
    expect(goSpy).toHaveBeenNthCalledWith(2, 'browse.welcome');
  });

  it('from login user goes to unauthorized page, should stay on login (transition aborted)', async () => {
    const protectedRoute = {
      name: 'protected',
      url: '/protected',
      component: () => null,
      data: { visibilityRequirements: { permissions: ['admin:all'] } }
    };
    router.stateRegistry.register(protectedRoute);
    await router.urlService.sync();

    // First go to login
    await router.stateService.go('login');

    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('protected').catch(() => {});

    // Transition is aborted; user stays on login — no additional go() calls
    expect(goSpy).toHaveBeenCalledTimes(1);
    expect(goSpy).toHaveBeenNthCalledWith(1, 'protected');
    expect(router.stateService.current.name).toBe('login');
  });

  it('unauthenticated user goes to unauthorized page, should redirect to login page with returnTo parameter', async () => {
    const protectedRoute = {
      name: 'protected',
      url: '/protected?filter&sort',
      component: () => null,
      data: { visibilityRequirements: { permissions: ['admin:all'] } }
    };
    router.stateRegistry.register(protectedRoute);
    await router.urlService.sync();

    const goSpy = jest.spyOn(router.stateService, 'go');
    const urlSpy = jest.spyOn(router.urlService, 'url').mockReturnValue('/protected?filter=maven&sort=name');
    await router.stateService.go('protected', { filter: 'maven', sort: 'name' }).catch(() => {});

    const expectedReturnTo = btoa('#/protected?filter=maven&sort=name');
    expect(goSpy).toHaveBeenCalledTimes(2);
    expect(goSpy).toHaveBeenNthCalledWith(1, 'protected', { filter: 'maven', sort: 'name' });
    expect(goSpy).toHaveBeenNthCalledWith(2, 'login', {returnTo: expectedReturnTo});
    expect(urlSpy).toHaveBeenCalled();
  });

  it('returnTo must decode to a plain hash URL without percent-encoding (OIDC server compat)', async () => {
    // OidcCallbackFilter.buildTargetUri only does Base64 decode on returnTo.
    // If the decoded value starts with %23 instead of #, the server treats it as a
    // literal path and the user gets a 404 after SSO login.
    const protectedRoute = {
      name: 'nodoubleencode',
      url: '/nodoubleencode',
      component: () => null,
      data: { visibilityRequirements: { permissions: ['admin:all'] } }
    };
    router.stateRegistry.register(protectedRoute);
    await router.urlService.sync();

    const goSpy = jest.spyOn(router.stateService, 'go');
    jest.spyOn(router.urlService, 'url').mockReturnValue('/nodoubleencode');
    await router.stateService.go('nodoubleencode').catch(() => {});

    const returnTo = goSpy.mock.calls[1][1].returnTo;
    const decoded = atob(returnTo);
    expect(decoded).toBe('#/nodoubleencode');
    expect(decoded).not.toMatch(/^%23/);
  });

  it('unauthenticated user with empty URL should redirect to login without returnTo', async () => {
    const protectedRoute = {
      name: 'protected',
      url: '/protected',
      component: () => null,
      data: { visibilityRequirements: { permissions: ['admin:all'] } }
    };
    router.stateRegistry.register(protectedRoute);
    await router.urlService.sync();

    const goSpy = jest.spyOn(router.stateService, 'go');
    jest.spyOn(router.urlService, 'url').mockReturnValue('');
    await router.stateService.go('protected').catch(() => {});

    expect(goSpy).toHaveBeenCalledTimes(2);
    expect(goSpy).toHaveBeenNthCalledWith(1, 'protected');
    expect(goSpy).toHaveBeenNthCalledWith(2, 'login');
  });

  it('unauthenticated user accessing child route with parent visibilityRequirements should redirect to login', async () => {

    router.stateRegistry.register({
      name: 'preview',
      url: '/preview',
      component: UIView,
    });
    router.stateRegistry.register({
      name: 'preview.admin',
      url: '/admin',
      abstract: true,
      component: UIView,
    });
    router.stateRegistry.register({
      name: 'preview.admin.security',
      url: '/security',
      component: UIView,
      data: {
        visibilityRequirements: { permissions: ['nexus:security:read'] },
      },
    });
    router.stateRegistry.register({
      name: 'preview.admin.security.roles',
      url: '/roles',
      component: () => null,
      data: { title: 'Roles' },
    });
    await router.urlService.sync();

    const goSpy = jest.spyOn(router.stateService, 'go');
    jest.spyOn(router.urlService, 'url').mockReturnValue('/preview/admin/security/roles');
    await router.stateService.go('preview.admin.security.roles').catch(() => {});

    expect(goSpy).toHaveBeenCalledWith('login', { returnTo: btoa('#/preview/admin/security/roles') });
  });

  it('authenticated user accessing child route with parent visibilityRequirements should redirect to preview welcome', async () => {
    ExtJS.hasUser.mockReturnValue(true);

    router.stateRegistry.register({
      name: 'preview',
      url: '/preview',
      component: UIView,
    });
    router.stateRegistry.register({
      name: 'preview.browse',
      url: '/browse',
      component: UIView,
    });
    router.stateRegistry.register({
      name: 'preview.browse.welcome',
      url: '/welcome',
      component: () => null,
      data: { visibilityRequirements: {} },
    });
    router.stateRegistry.register({
      name: 'preview.admin',
      url: '/admin',
      abstract: true,
      component: UIView,
    });
    router.stateRegistry.register({
      name: 'preview.admin.security',
      url: '/security',
      component: UIView,
      data: {
        visibilityRequirements: { permissions: ['nexus:security:read'] },
      },
    });
    router.stateRegistry.register({
      name: 'preview.admin.security.roles',
      url: '/roles',
      component: () => null,
      data: { title: 'Roles' },
    });
    await router.urlService.sync();

    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('preview.admin.security.roles').catch(() => {});

    expect(goSpy).toHaveBeenCalledWith('preview.browse.welcome');
  });

  it('unauthenticated user accessing preview child route with parent visibilityRequirements should redirect to login with returnTo', async () => {
    router.stateRegistry.register({
      name: 'preview',
      url: '/preview',
      component: UIView,
    });
    router.stateRegistry.register({
      name: 'preview.admin',
      url: '/admin',
      abstract: true,
      component: UIView,
    });
    router.stateRegistry.register({
      name: 'preview.admin.security',
      url: '/security',
      component: UIView,
      data: {
        visibilityRequirements: { permissions: ['nexus:security:read'] },
      },
    });
    router.stateRegistry.register({
      name: 'preview.admin.security.roles',
      url: '/roles',
      component: () => null,
      data: { title: 'Roles' },
    });
    router.stateRegistry.register({
      name: 'preview.admin.security.roles.profile',
      url: '/:roleId/profile',
      component: () => null,
      data: { title: 'Role Profile' },
    });
    await router.urlService.sync();

    const goSpy = jest.spyOn(router.stateService, 'go');
    const urlSpy = jest.spyOn(router.urlService, 'url').mockReturnValue('/preview/admin/security/roles/asd/profile');
    await router.stateService.go('preview.admin.security.roles.profile', { roleId: 'asd' }).catch(() => {});

    const expectedReturnTo = btoa('#/preview/admin/security/roles/asd/profile');
    expect(goSpy).toHaveBeenCalledWith('login', { returnTo: expectedReturnTo });
    expect(urlSpy).toHaveBeenCalled();
  });

  it('authenticated user accessing preview protected route should redirect to preview.browse.welcome (not classic welcome)', async () => {
    ExtJS.hasUser.mockReturnValue(true);

    router.stateRegistry.register({
      name: 'preview',
      url: '/preview',
      component: UIView,
    });
    router.stateRegistry.register({
      name: 'preview.browse',
      url: '/browse',
      component: UIView,
    });
    router.stateRegistry.register({
      name: 'preview.browse.welcome',
      url: '/welcome',
      component: () => null,
      data: { visibilityRequirements: {} },
    });
    router.stateRegistry.register({
      name: 'preview.admin',
      url: '/admin',
      abstract: true,
      component: UIView,
    });
    router.stateRegistry.register({
      name: 'preview.admin.system',
      url: '/system',
      component: () => null,
      data: {
        visibilityRequirements: { permissions: ['nexus:settings:read'] },
      },
    });
    await router.urlService.sync();

    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('preview.admin.system').catch(() => {});

    expect(goSpy).toHaveBeenCalledWith('preview.browse.welcome');
  });

  it('authenticated user accessing classic protected route should redirect to browse.welcome', async () => {
    ExtJS.hasUser.mockReturnValue(true);
    await router.stateService.go('browse.welcome');

    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('browse.search.generic').catch(() => {});

    expect(goSpy).toHaveBeenCalledWith('browse.welcome');
  });

  it('unauthenticated user can navigate to public routes without redirect', async () => {
    await router.stateService.go('browse.welcome');
    expect(router.stateService.current.name).toBe('browse.welcome');

    await router.stateService.go('login');
    expect(router.stateService.current.name).toBe('login');
  });

  it('transition from login to protected route multiple times should remain on login', async () => {
    const protectedRoute = {
      name: 'protected',
      url: '/protected',
      component: () => null,
      data: { visibilityRequirements: { permissions: ['admin:all'] } }
    };
    router.stateRegistry.register(protectedRoute);
    await router.urlService.sync();

    await router.stateService.go('login');

    await router.stateService.go('protected').catch(() => {});
    expect(router.stateService.current.name).toBe('login');

    await router.stateService.go('protected').catch(() => {});
    expect(router.stateService.current.name).toBe('login');

    await router.stateService.go('protected').catch(() => {});
    expect(router.stateService.current.name).toBe('login');
  });
});

describe('createRouter - otherwise handler - unrecognized URLs', () => {
  let router;

  beforeEach(() => {
    jest.clearAllMocks();
    global.NX.Permissions.check.mockReturnValue(false);
    if (!global.NX.Security) {
      global.NX.Security = { hasUser: jest.fn() };
    }
    ExtJS.hasUser = jest.fn().mockReturnValue(false);
    ExtJS.state = jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(false)
    });
    router = getTestRouter();
  });

  it('unauthenticated user navigating to unrecognized admin URL should redirect to login with returnTo', async () => {
    global.NX.Security.hasUser.mockReturnValue(false);
    window.location.hash = '#admin/security/roles/asd/profile';

    const goSpy = jest.spyOn(router.stateService, 'go');
    jest.spyOn(router.urlService, 'url').mockReturnValue('/admin/security/roles/asd/profile');

    await router.urlService.sync();
    await new Promise(resolve => setTimeout(resolve, 0));

    const expectedReturnTo = btoa('#/admin/security/roles/asd/profile');
    expect(goSpy).toHaveBeenCalledWith('login', { returnTo: expectedReturnTo });
  });

  it('unauthenticated user navigating to unrecognized preview URL should redirect to login with returnTo', async () => {
    global.NX.Security.hasUser.mockReturnValue(false);
    window.location.hash = '#preview/admin/security/roles/asd/profile';

    const goSpy = jest.spyOn(router.stateService, 'go');
    jest.spyOn(router.urlService, 'url').mockReturnValue('/preview/admin/security/roles/asd/profile');

    await router.urlService.sync();
    await new Promise(resolve => setTimeout(resolve, 0));

    const expectedReturnTo = btoa('#/preview/admin/security/roles/asd/profile');
    expect(goSpy).toHaveBeenCalledWith('login', { returnTo: expectedReturnTo });
  });

  it('authenticated user navigating to unrecognized admin URL should go to 404', async () => {
    global.NX.Security.hasUser.mockReturnValue(true);
    window.location.hash = '#admin/nonexistent/page';

    const goSpy = jest.spyOn(router.stateService, 'go');

    await router.urlService.sync();
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(goSpy).toHaveBeenCalledWith('missing.route', {}, { location: false });
  });

  it('unauthenticated user navigating to unrecognized non-protected URL should go to 404', async () => {
    global.NX.Security.hasUser.mockReturnValue(false);
    window.location.hash = '#browse/nonexistent/page';

    const goSpy = jest.spyOn(router.stateService, 'go');

    await router.urlService.sync();
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(goSpy).toHaveBeenCalledWith('missing.route', {}, { location: false });
  });

  it('falls back to bootstrap data when NX.Security is not available', async () => {
    delete global.NX.Security;
    window.__nxRestBootstrap = { user: { authenticated: false } };
    window.location.hash = '#admin/some/path';

    const goSpy = jest.spyOn(router.stateService, 'go');
    jest.spyOn(router.urlService, 'url').mockReturnValue('/admin/some/path');

    await router.urlService.sync();
    await new Promise(resolve => setTimeout(resolve, 0));

    const expectedReturnTo = btoa('#/admin/some/path');
    expect(goSpy).toHaveBeenCalledWith('login', { returnTo: expectedReturnTo });
  });
});
