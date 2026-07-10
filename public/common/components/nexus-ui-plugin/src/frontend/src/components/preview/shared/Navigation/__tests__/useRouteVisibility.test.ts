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
import {renderHook} from '@testing-library/react';
import {useRouteVisibility} from '../useRouteVisibility';

jest.mock('@uirouter/react', () => ({
  useRouter: jest.fn(),
}));

// Source: '../../../../interface/NavigationUtils' (4 up from Navigation/).
// From __tests__/ needs 5 ups to reach the same src/ root.
jest.mock('../../../../../interface/NavigationUtils', () => ({
  useIsVisible: jest.fn(),
}));

jest.mock('../useContextAwareRouteName', () => ({
  useContextAwareRouteName: jest.fn((name: string) => name),
}));

const {useRouter} = require('@uirouter/react');
const {useIsVisible} = require('../../../../../interface/NavigationUtils');

function makeRouter(routeData: unknown = {data: {visibilityRequirements: 'nexus:search:read'}}) {
  return {
    stateRegistry: {
      get: jest.fn().mockReturnValue(routeData),
    },
  };
}

describe('useRouteVisibility', () => {
  it('returns false when the route does not exist', () => {
    useRouter.mockReturnValue({stateRegistry: {get: jest.fn().mockReturnValue(null)}});
    useIsVisible.mockReturnValue(false);

    const {result} = renderHook(() => useRouteVisibility('missing.route'));
    expect(result.current).toBe(false);
  });

  it('returns true when the route exists and is visible', () => {
    useRouter.mockReturnValue(makeRouter());
    useIsVisible.mockReturnValue(true);

    const {result} = renderHook(() => useRouteVisibility('browse.search'));
    expect(result.current).toBe(true);
  });

  it('returns false when the route exists but is not visible', () => {
    useRouter.mockReturnValue(makeRouter());
    useIsVisible.mockReturnValue(false);

    const {result} = renderHook(() => useRouteVisibility('browse.search'));
    expect(result.current).toBe(false);
  });

  it('returns false when stateRegistry.get throws', () => {
    useRouter.mockReturnValue({
      stateRegistry: {
        get: jest.fn().mockImplementation(() => {
          throw new Error('registry error');
        }),
      },
    });
    useIsVisible.mockReturnValue(false);

    const {result} = renderHook(() => useRouteVisibility('bad.route'));
    expect(result.current).toBe(false);
  });

  it('returns true for a route with no visibilityRequirements when useIsVisible returns true', () => {
    useRouter.mockReturnValue(makeRouter({data: {}}));
    useIsVisible.mockReturnValue(true);

    const {result} = renderHook(() => useRouteVisibility('open.route'));
    expect(result.current).toBe(true);
  });
});
