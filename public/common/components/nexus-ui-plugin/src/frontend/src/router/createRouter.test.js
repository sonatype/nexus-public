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
    // Default: the client already knows whether a user is signed in, so no deferral
    ExtJS.isAuthStateResolved = jest.fn().mockReturnValue(true);
    ExtJS.whenAuthStateResolved = jest.fn().mockResolvedValue(undefined);
    ExtJS.onStateChange = jest.fn(() => () => {});

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
    ExtJS.isAuthStateResolved = jest.fn().mockReturnValue(true);
    ExtJS.whenAuthStateResolved = jest.fn().mockResolvedValue(undefined);
    ExtJS.onStateChange = jest.fn(() => () => {});
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

  it('redirects to login without returnTo when url is empty for protected path', async () => {
    global.NX.Security.hasUser.mockReturnValue(false);
    window.location.hash = '#admin/some/path';

    const goSpy = jest.spyOn(router.stateService, 'go');
    jest.spyOn(router.urlService, 'url').mockReturnValue('');

    await router.urlService.sync();
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(goSpy).toHaveBeenCalledWith('login');
  });

  it('defers to ExtJS for bookmark URLs containing = in hash outside preview routes', async () => {
    global.NX.Security.hasUser.mockReturnValue(false);
    window.location.hash = '#browse/search=format%3Dnpm:repository-id:path';

    const goSpy = jest.spyOn(router.stateService, 'go');

    await router.urlService.sync();
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(goSpy).not.toHaveBeenCalledWith('login', expect.anything());
    expect(goSpy).not.toHaveBeenCalledWith('missing.route', expect.anything(), expect.anything());
  });
});

describe('createRouter - preserve URL hash on browser refresh', () => {
  let router;

  beforeEach(() => {
    jest.clearAllMocks();
    global.NX.Permissions.check.mockReturnValue(false);
    if (!global.NX.Security) {
      global.NX.Security = { hasUser: jest.fn() };
    }
    ExtJS.hasUser = jest.fn().mockReturnValue(true);
    ExtJS.state = jest.fn().mockReturnValue({ getValue: jest.fn().mockReturnValue(false) });
    ExtJS.isAuthStateResolved = jest.fn().mockReturnValue(true);
    ExtJS.whenAuthStateResolved = jest.fn().mockResolvedValue(undefined);
    ExtJS.onStateChange = jest.fn(() => () => {});
  });

  afterEach(() => {
    window.location.hash = '';
  });

  it('honors the URL hash for a registered route on browser refresh, not overriding with initialRoute', async () => {
    // Uses #/login (not the initialRoute browse.welcome) — if the initial rule fired
    // it would override to browse.welcome; with the fix the hash wins and router lands on login.
    ExtJS.hasUser = jest.fn().mockReturnValue(false);
    window.location.hash = '#/login';
    router = getTestRouter();

    await router.urlService.sync();
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(router.stateService.current.name).toBe('login');
  });

  it('does not redirect to initialRoute when URL has an unrecognized settings hash', async () => {
    // Settings pages such as #admin/repository/repositories are ExtJS routes not
    // registered in the React router. Without the fix the initial rule fires for
    // unrecognized URLs and redirects to browse.welcome. With the fix the otherwise
    // handler runs instead, routing authenticated users to missing.route (404).
    global.NX.Security.hasUser.mockReturnValue(true);
    window.location.hash = '#admin/system/capabilities';
    router = getTestRouter();

    const goSpy = jest.spyOn(router.stateService, 'go');

    await router.urlService.sync();
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(goSpy).toHaveBeenCalledWith('missing.route', {}, { location: false });
    expect(goSpy).not.toHaveBeenCalledWith('browse.welcome');
  });

  it('applies initial route rule when URL has only #/', async () => {
    // #/ is treated as empty — initial rule should fire, so the otherwise handler (missing.route)
    // is NOT used; the router falls through to the initial route instead.
    global.NX.Security.hasUser.mockReturnValue(true);
    window.location.hash = '#/';
    router = getTestRouter();

    const goSpy = jest.spyOn(router.stateService, 'go');

    await router.urlService.sync();
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(goSpy).not.toHaveBeenCalledWith('missing.route', expect.anything(), expect.anything());
  });
});

describe('createRouter - onBefore - state with no visibilityRequirements', () => {
  let router;

  beforeEach(() => {
    jest.clearAllMocks();
    global.NX.Permissions.check.mockReturnValue(true);
    ExtJS.hasUser = jest.fn().mockReturnValue(true);
    ExtJS.state = jest.fn().mockReturnValue({ getValue: jest.fn().mockReturnValue(false) });
    ExtJS.isAuthStateResolved = jest.fn().mockReturnValue(true);
    ExtJS.whenAuthStateResolved = jest.fn().mockResolvedValue(undefined);
    ExtJS.onStateChange = jest.fn(() => () => {});
    router = getTestRouter();
  });

  it('allows navigation to a state with no visibilityRequirements', async () => {
    router.stateRegistry.register({
      name: 'unrestricted',
      url: '/unrestricted',
      component: () => null,
    });
    await router.urlService.sync();

    await router.stateService.go('unrestricted');

    expect(router.stateService.current.name).toBe('unrestricted');
  });
});

/*
 * NEXUS-54290: under header/token authentication (rutauth-realm) the server-inlined
 * NX.app.state carries no user and there is no session cookie, so during bootstrap the client
 * cannot tell "no user" from "user not known yet". These tests cover both halves of the fix:
 * waiting for an authoritative answer before redirecting to login, and leaving login again if
 * the user turns up after the redirect already happened.
 */
describe('createRouter - onBefore - unresolved auth state (NEXUS-54290)', () => {
  let router;
  let originalGetValue;

  /** Models the real Welcome page, which is gated on anonymousAccessOrHasUser. */
  const GATED_ROUTE = {
    name: 'gated',
    url: '/gated',
    component: () => null,
    data: { visibilityRequirements: { anonymousAccessOrHasUser: true } }
  };

  const GATED_RETURN_TO = btoa('#/gated');

  beforeEach(() => {
    jest.clearAllMocks();
    global.NX.Permissions.check.mockReturnValue(true);
    // An earlier suite deletes NX.Security; NavigationUtils needs it for the visibility check.
    global.NX.Security = { hasUser: jest.fn().mockReturnValue(false) };
    originalGetValue = global.NX.State.getValue;
    // No anonymous access, so visibility hinges entirely on there being a user
    global.NX.State.getValue = jest.fn().mockReturnValue(undefined);

    ExtJS.hasUser = jest.fn(() => global.NX.Security.hasUser());
    ExtJS.state = jest.fn().mockReturnValue({ getValue: jest.fn().mockReturnValue(false) });
    ExtJS.isAuthStateResolved = jest.fn().mockReturnValue(false);
    ExtJS.whenAuthStateResolved = jest.fn().mockResolvedValue(undefined);
    ExtJS.onStateChange = jest.fn(() => () => {});

    router = getTestRouter();
    router.stateRegistry.register(GATED_ROUTE);
  });

  afterEach(() => {
    global.NX.State.getValue = originalGetValue;
  });

  /** Returns the listener createRouter subscribed to 'userchanged', or undefined. */
  function recoveryListener() {
    const call = ExtJS.onStateChange.mock.calls.find(([events]) => events.includes('userchanged'));
    return call?.[1];
  }

  it('waits for the auth state, then allows the route when a user turns up', async () => {
    ExtJS.whenAuthStateResolved.mockImplementation(() => {
      global.NX.Security.hasUser.mockReturnValue(true);
      return Promise.resolve();
    });

    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('gated').catch(() => {});

    expect(ExtJS.whenAuthStateResolved).toHaveBeenCalled();
    expect(goSpy).toHaveBeenCalledTimes(1);
    expect(goSpy).not.toHaveBeenCalledWith('login', expect.anything());
    expect(router.stateService.current.name).toBe('gated');
  });

  it('does not redirect to login while the auth state is still in flight', async () => {
    let resolveAuthState;
    ExtJS.whenAuthStateResolved.mockReturnValue(new Promise((resolve) => {
      resolveAuthState = resolve;
    }));

    const goSpy = jest.spyOn(router.stateService, 'go');
    jest.spyOn(router.urlService, 'url').mockReturnValue('/gated');
    const transition = router.stateService.go('gated').catch(() => {});

    // Let the hook run up to the await; nothing should have been decided yet
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(goSpy).not.toHaveBeenCalledWith('login', expect.anything());

    global.NX.Security.hasUser.mockReturnValue(true);
    resolveAuthState();
    await transition;

    expect(goSpy).not.toHaveBeenCalledWith('login', expect.anything());
    expect(router.stateService.current.name).toBe('gated');
  });

  it('redirects to login once the auth state resolves with no user', async () => {
    const goSpy = jest.spyOn(router.stateService, 'go');
    jest.spyOn(router.urlService, 'url').mockReturnValue('/gated');
    await router.stateService.go('gated').catch(() => {});

    expect(ExtJS.whenAuthStateResolved).toHaveBeenCalled();
    expect(goSpy).toHaveBeenCalledWith('login', { returnTo: GATED_RETURN_TO });
  });

  it('does not wait when the auth state is already resolved', async () => {
    ExtJS.isAuthStateResolved.mockReturnValue(true);

    const goSpy = jest.spyOn(router.stateService, 'go');
    jest.spyOn(router.urlService, 'url').mockReturnValue('/gated');
    await router.stateService.go('gated').catch(() => {});

    expect(ExtJS.whenAuthStateResolved).not.toHaveBeenCalled();
    expect(goSpy).toHaveBeenCalledWith('login', { returnTo: GATED_RETURN_TO });
  });

  it('resumes the original destination when a user arrives after the login redirect', async () => {
    jest.spyOn(router.urlService, 'url').mockReturnValue('/gated');
    await router.stateService.go('gated').catch(() => {});
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(router.stateService.current.name).toBe('login');

    const onUserChanged = recoveryListener();
    expect(onUserChanged).toBeDefined();

    const urlSpy = jest.spyOn(router.urlService, 'url');
    global.NX.Security.hasUser.mockReturnValue(true);
    onUserChanged();

    expect(urlSpy).toHaveBeenCalledWith('#/gated');
  });

  it('leaves login for the welcome page when a user arrives and there is no returnTo', async () => {
    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('login');
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(router.stateService.current.name).toBe('login');

    const onUserChanged = recoveryListener();
    expect(onUserChanged).toBeDefined();

    global.NX.Security.hasUser.mockReturnValue(true);
    onUserChanged();

    expect(goSpy).toHaveBeenCalledWith('browse.welcome');
  });

  it('stays on login when the state change brings no user', async () => {
    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('login');
    await new Promise((resolve) => setTimeout(resolve, 0));

    const onUserChanged = recoveryListener();
    expect(onUserChanged).toBeDefined();

    goSpy.mockClear();
    onUserChanged();

    expect(goSpy).not.toHaveBeenCalled();
    expect(router.stateService.current.name).toBe('login');
  });

  it('does not watch for a late user when the auth state is already resolved', async () => {
    ExtJS.isAuthStateResolved.mockReturnValue(true);

    await router.stateService.go('login');

    expect(ExtJS.onStateChange).not.toHaveBeenCalled();
  });
});

describe('createRouter - onBefore - unresolved permissions state (NEXUS-54422)', () => {
  let router;

  /**
   * Models a permission-gated preview route (e.g. the unified search page, gated
   * on nexus:search:read). On a hard reload, NEXUS-52583's fast-render bootstrap
   * lets the router evaluate this before NX.Permissions has loaded, so the
   * visibility check below fails not because the user lacks the permission, but
   * because the answer has not arrived yet.
   */
  const GATED_ROUTE = {
    name: 'preview.gated',
    url: '/gated',
    component: () => null,
    data: { visibilityRequirements: { permissions: ['nexus:search:read'] } }
  };

  beforeEach(() => {
    jest.clearAllMocks();

    // A session cookie resolves hasUser()/isAuthStateResolved() immediately, well
    // before the separate permissions poll lands — the two are not the same signal.
    ExtJS.hasUser = jest.fn().mockReturnValue(true);
    ExtJS.isAuthStateResolved = jest.fn().mockReturnValue(true);
    ExtJS.whenAuthStateResolved = jest.fn().mockResolvedValue(undefined);
    ExtJS.onStateChange = jest.fn(() => () => {});
    ExtJS.state = jest.fn().mockReturnValue({ getValue: jest.fn().mockReturnValue(false) });

    // Permissions have not loaded yet: NX.Permissions.permissions is undefined
    // (see ExtJS.arePermissionsReady), so every check reads as denied.
    ExtJS.arePermissionsReady = jest.fn().mockReturnValue(false);
    global.NX.Permissions.check.mockReturnValue(false);

    router = getTestRouter();
    // 'preview.browse.welcome' must exist so the isPreviewUIRouteName redirect target
    // resolves, and 'preview'/'preview.browse' must exist as ancestors before a dotted
    // state name can be registered at all.
    router.stateRegistry.register({ name: 'preview', url: '/preview', component: UIView });
    router.stateRegistry.register({ name: 'preview.browse', url: '/browse', component: UIView });
    router.stateRegistry.register({
      name: 'preview.browse.welcome',
      url: '/welcome',
      component: () => null,
      data: { visibilityRequirements: {} },
    });
    router.stateRegistry.register(GATED_ROUTE);
  });

  it('waits for permissions, then allows the route once they grant it', async () => {
    ExtJS.waitForPermissions = jest.fn().mockImplementation(() => {
      ExtJS.arePermissionsReady.mockReturnValue(true);
      global.NX.Permissions.check.mockReturnValue(true);
      return Promise.resolve();
    });

    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('preview.gated').catch(() => {});

    // Bounded to a short settle window, NOT waitForPermissions()'s 30s default. This
    // wait only avoids a welcome-page flash; correctness comes from the recovery
    // subscription. Blocking for 30s here stalled navigation on a slow permissions
    // endpoint and timed out cloudui's App tests (NEXUS-54422).
    expect(ExtJS.waitForPermissions).toHaveBeenCalledWith(500);
    expect(goSpy).not.toHaveBeenCalledWith('preview.browse.welcome');
    expect(router.stateService.current.name).toBe('preview.gated');
  });

  it('never blocks navigation for longer than the settle window', async () => {
    // Pins the regression directly: any caller passing no argument (or a large one)
    // inherits the 30s default and stalls the transition.
    ExtJS.waitForPermissions = jest.fn().mockResolvedValue(undefined);

    await router.stateService.go('preview.gated').catch(() => {});

    const [timeout] = ExtJS.waitForPermissions.mock.calls[0];
    expect(timeout).toBeLessThanOrEqual(1000);
  });

  it('falls back to the welcome redirect if permissions resolve without granting access', async () => {
    ExtJS.waitForPermissions = jest.fn().mockImplementation(() => {
      ExtJS.arePermissionsReady.mockReturnValue(true);
      // permissions loaded, but genuinely does not include nexus:search:read
      return Promise.resolve();
    });

    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('preview.gated').catch(() => {});

    expect(ExtJS.waitForPermissions).toHaveBeenCalled();
    expect(goSpy).toHaveBeenCalledWith('preview.browse.welcome');
  });

  it('falls back to the welcome redirect if the permissions wait times out', async () => {
    ExtJS.waitForPermissions = jest.fn().mockRejectedValue(new Error('Permissions load timed out'));

    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('preview.gated').catch(() => {});

    expect(goSpy).toHaveBeenCalledWith('preview.browse.welcome');
  });

  it('does not wait when permissions are already loaded', async () => {
    ExtJS.arePermissionsReady.mockReturnValue(true);
    ExtJS.waitForPermissions = jest.fn().mockResolvedValue(undefined);

    const goSpy = jest.spyOn(router.stateService, 'go');
    await router.stateService.go('preview.gated').catch(() => {});

    expect(ExtJS.waitForPermissions).not.toHaveBeenCalled();
    expect(goSpy).toHaveBeenCalledWith('preview.browse.welcome');
  });
});

describe('createRouter - onBefore - permissions arriving after the redirect (NEXUS-54422)', () => {
  let router;
  let originalHash;

  /**
   * Models the empty-map state observed in the browser: arePermissionsReady() reads
   * true, so the wait is skipped, yet every check() is false. This is the case no
   * amount of waiting or timeout tuning can fix, and it is why recovery exists.
   */
  const GATED_ROUTE = {
    name: 'preview.gated',
    url: '/gated',
    component: () => null,
    data: { visibilityRequirements: { permissions: ['nexus:search:read'] } }
  };

  const SEARCH_HASH = '#/preview/gated?format=maven&nameOrVersion=commons';

  beforeEach(() => {
    jest.clearAllMocks();

    ExtJS.hasUser = jest.fn().mockReturnValue(true);
    ExtJS.isAuthStateResolved = jest.fn().mockReturnValue(true);
    ExtJS.whenAuthStateResolved = jest.fn().mockResolvedValue(undefined);
    ExtJS.onStateChange = jest.fn(() => () => {});
    ExtJS.state = jest.fn().mockReturnValue({ getValue: jest.fn().mockReturnValue(false) });

    // The empty-install state: "ready", but granting nothing.
    ExtJS.arePermissionsReady = jest.fn().mockReturnValue(true);
    ExtJS.waitForPermissions = jest.fn().mockResolvedValue(undefined);
    global.NX.Permissions.check.mockReturnValue(false);

    ExtJS.onPermissionsChange = jest.fn(() => () => {});

    originalHash = window.location.hash;
    window.location.hash = SEARCH_HASH;

    router = getTestRouter();
    router.stateRegistry.register({ name: 'preview', url: '/preview', component: UIView });
    router.stateRegistry.register({ name: 'preview.browse', url: '/browse', component: UIView });
    router.stateRegistry.register({
      name: 'preview.browse.welcome',
      url: '/welcome',
      component: () => null,
      data: { visibilityRequirements: {} },
    });
    router.stateRegistry.register(GATED_ROUTE);
  });

  afterEach(() => {
    window.location.hash = originalHash;
  });

  /** Returns the listener createRouter subscribed to permission changes. */
  function permissionsListener() {
    return ExtJS.onPermissionsChange.mock.calls[0]?.[0];
  }

  it('subscribes to permission changes when it redirects away', async () => {
    await router.stateService.go('preview.gated').catch(() => {});

    expect(ExtJS.onPermissionsChange).toHaveBeenCalled();
    expect(permissionsListener()).toBeDefined();
  });

  it('restores the original destination once permissions grant it', async () => {
    await router.stateService.go('preview.gated').catch(() => {});
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(router.stateService.current.name).toBe('preview.browse.welcome');

    // The real permission set lands.
    global.NX.Permissions.check.mockReturnValue(true);
    permissionsListener()();

    // Restored by hash assignment, not stateService.go, so undeclared params survive.
    expect(window.location.hash).toContain('nameOrVersion=commons');
    expect(window.location.hash).toContain('format=maven');
  });

  it('keeps waiting when a permission change still does not grant the route', async () => {
    await router.stateService.go('preview.gated').catch(() => {});
    await new Promise((resolve) => setTimeout(resolve, 0));

    // An empty install fires 'changed' too — it must not consume the subscription.
    permissionsListener()();
    expect(router.stateService.current.name).toBe('preview.browse.welcome');

    global.NX.Permissions.check.mockReturnValue(true);
    permissionsListener()();
    expect(window.location.hash).toContain('nameOrVersion=commons');
  });

  it('does not hijack navigation if the user has moved on', async () => {
    await router.stateService.go('preview.gated').catch(() => {});
    await new Promise((resolve) => setTimeout(resolve, 0));

    // User navigates somewhere else before permissions land.
    await router.stateService.go('browse.welcome').catch(() => {});
    const hashAfterUserNavigation = window.location.hash;

    global.NX.Permissions.check.mockReturnValue(true);
    permissionsListener()();

    expect(window.location.hash).toBe(hashAfterUserNavigation);
  });

  it('stops watching after the recovery timeout', async () => {
    jest.useFakeTimers();
    try {
      const unsubscribe = jest.fn();
      ExtJS.onPermissionsChange = jest.fn(() => unsubscribe);

      router = getTestRouter();
      router.stateRegistry.register({ name: 'preview', url: '/preview', component: UIView });
      router.stateRegistry.register({ name: 'preview.browse', url: '/browse', component: UIView });
      router.stateRegistry.register({
        name: 'preview.browse.welcome',
        url: '/welcome',
        component: () => null,
        data: { visibilityRequirements: {} },
      });
      router.stateRegistry.register(GATED_ROUTE);

      router.stateService.go('preview.gated').catch(() => {});
      // The transition resolves across a promise chain; alternate microtask flushes
      // with zero-length timer advances until the redirect branch has run.
      for (let i = 0; i < 50; i++) {
        jest.advanceTimersByTime(0);
        await Promise.resolve();
      }
      expect(ExtJS.onPermissionsChange).toHaveBeenCalled();

      jest.advanceTimersByTime(30000);
      expect(unsubscribe).toHaveBeenCalled();
    } finally {
      jest.useRealTimers();
    }
  });
});

