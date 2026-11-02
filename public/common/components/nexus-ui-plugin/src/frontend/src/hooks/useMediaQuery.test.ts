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
import {act, renderHook} from '@testing-library/react';
import {useMediaQuery} from './useMediaQuery';

const TEST_QUERY = '(min-width: 1024px)';

type ChangeHandler = (event: Partial<MediaQueryListEvent>) => void;

// Builds a controllable matchMedia whose change listeners can be fired on demand.
function installMatchMedia(initialMatches: boolean) {
  const listeners: ChangeHandler[] = [];
  const mql = {
    matches: initialMatches,
    media: TEST_QUERY,
    addEventListener: jest.fn((_event: string, cb: ChangeHandler) => listeners.push(cb)),
    removeEventListener: jest.fn((_event: string, cb: ChangeHandler) => {
      const i = listeners.indexOf(cb);
      if (i >= 0) listeners.splice(i, 1);
    }),
    addListener: jest.fn(),
    removeListener: jest.fn(),
    dispatchEvent: jest.fn(),
  };
  window.matchMedia = jest.fn(() => mql) as unknown as typeof window.matchMedia;
  const emit = (matches: boolean) => {
    mql.matches = matches;
    listeners.forEach((cb) => cb({matches}));
  };
  return {mql, emit};
}

describe('useMediaQuery', () => {
  const originalMatchMedia = window.matchMedia;

  afterEach(() => {
    window.matchMedia = originalMatchMedia;
  });

  it('shouldReturnTrueWhenQueryMatchesInitially', () => {
    installMatchMedia(true);
    const {result} = renderHook(() => useMediaQuery(TEST_QUERY));
    expect(result.current).toBe(true);
  });

  it('shouldReturnFalseWhenQueryDoesNotMatchInitially', () => {
    installMatchMedia(false);
    const {result} = renderHook(() => useMediaQuery(TEST_QUERY));
    expect(result.current).toBe(false);
  });

  it('shouldUpdateWhenTheQueryStartsMatching', () => {
    const {emit} = installMatchMedia(false);
    const {result} = renderHook(() => useMediaQuery(TEST_QUERY));
    expect(result.current).toBe(false);

    act(() => emit(true));

    expect(result.current).toBe(true);
  });

  it('shouldUpdateWhenTheQueryStopsMatching', () => {
    const {emit} = installMatchMedia(true);
    const {result} = renderHook(() => useMediaQuery(TEST_QUERY));

    act(() => emit(false));

    expect(result.current).toBe(false);
  });

  it('shouldRemoveItsListenerOnUnmount', () => {
    const {mql} = installMatchMedia(true);
    const {unmount} = renderHook(() => useMediaQuery(TEST_QUERY));

    unmount();

    expect(mql.removeEventListener).toHaveBeenCalledTimes(1);
  });

  it('shouldReturnFalseWhenMatchMediaIsUnavailable', () => {
    // SSR / non-browser guard: no matchMedia on window.
    window.matchMedia = undefined as unknown as typeof window.matchMedia;
    const {result} = renderHook(() => useMediaQuery(TEST_QUERY));
    expect(result.current).toBe(false);
  });
});
