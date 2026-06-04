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

  it('authenticated user goes to unauthorized page, should redirect to 404', async () => {
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
    expect(goSpy).toHaveBeenNthCalledWith(2, 'missing.route');
  });

  it('from login anonymous user goes to unauthorized page, should redirect to 404', async () => {
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue('anonymous')
    });

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

    expect(goSpy).toHaveBeenCalledTimes(2);
    expect(goSpy).toHaveBeenNthCalledWith(1, 'protected');
    expect(goSpy).toHaveBeenNthCalledWith(2, 'missing.route');
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
});
