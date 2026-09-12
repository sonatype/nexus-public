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

const mockGetValue = jest.fn();
// Controls what the mocked ExtJS.state() returns; each test sets it (an object, null, or undefined).
let mockState;

jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual(
    '../../../../../nexus-ui-plugin/src/frontend/src/utils/loginUtils'
  );
  return {
    ExtJS: { state: () => mockState },
    resolveDefaultLandingRoute: actual.resolveDefaultLandingRoute,
    USER_REQUESTED_LEGACY_KEY: actual.USER_REQUESTED_LEGACY_KEY,
  };
});

jest.mock('./routeNames/routeNames', () => ({
  ROUTE_NAMES: {
    LOGIN: 'login',
    BROWSE: { WELCOME: { ROOT: 'browse.welcome' } },
  },
}));

import { getInitialRoute } from './getInitialRoute';

describe('getInitialRoute', () => {
  beforeEach(() => {
    mockGetValue.mockReset();
    mockState = { getValue: mockGetValue };
    sessionStorage.clear();
  });

  const stateWith = (values) => (key, def) => (key in values ? values[key] : def);

  describe('when ExtJS.state is not available', () => {
    it('returns classic welcome when ExtJS.state() returns null', () => {
      mockState = null;
      expect(getInitialRoute()).toBe('browse.welcome');
    });

    it('returns classic welcome when ExtJS.state() returns undefined', () => {
      mockState = undefined;
      expect(getInitialRoute()).toBe('browse.welcome');
    });
  });

  it('returns login when anonymous access is disabled (real but falsy anonUser)', () => {
    mockGetValue.mockImplementation(stateWith({ anonymousUsername: '' }));
    expect(getInitialRoute()).toBe('login');
  });

  it('falls back to classic welcome when state is not seeded (anonUser null)', () => {
    mockGetValue.mockImplementation(stateWith({ anonymousUsername: null }));
    expect(getInitialRoute()).toBe('browse.welcome');
  });

  it('returns classic welcome for anonymous when defaultToPreviewUi is off', () => {
    mockGetValue.mockImplementation(stateWith({
      anonymousUsername: 'anonymous',
      defaultToPreviewUi: false,
      anonymousEnabled: true,
    }));
    expect(getInitialRoute()).toBe('browse.welcome');
  });

  it('returns preview welcome for anonymous when defaultToPreviewUi and anonymousEnabled are on', () => {
    mockGetValue.mockImplementation(stateWith({
      anonymousUsername: 'anonymous',
      defaultToPreviewUi: true,
      anonymousEnabled: true,
    }));
    expect(getInitialRoute()).toBe('preview.browse.welcome');
  });

  it('returns classic welcome for anonymous when defaultToPreviewUi is on but anonymousEnabled is off', () => {
    mockGetValue.mockImplementation(stateWith({
      anonymousUsername: 'anonymous',
      defaultToPreviewUi: true,
      anonymousEnabled: false,
    }));
    expect(getInitialRoute()).toBe('browse.welcome');
  });

  it('respects user_requested_legacy for anonymous users', () => {
    sessionStorage.setItem('user_requested_legacy', 'true');
    mockGetValue.mockImplementation(stateWith({
      anonymousUsername: 'anonymous',
      defaultToPreviewUi: true,
      anonymousEnabled: true,
    }));
    expect(getInitialRoute()).toBe('browse.welcome');
  });
});
