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

import { act, renderHook } from '@testing-library/react';

import { useDebounced } from '../useDebounced';

const DELAY = 300;

/** Advance timers inside `act` so the resulting state update is flushed before assertions. */
function advance(ms: number): void {
  act(() => {
    jest.advanceTimersByTime(ms);
  });
}

describe('useDebounced', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('exposes the initial value immediately, without waiting for the delay', () => {
    const { result } = renderHook(() => useDebounced('initial', DELAY));

    expect(result.current).toBe('initial');
  });

  it('withholds a new value until the delay has elapsed', () => {
    const { result, rerender } = renderHook(({ value }) => useDebounced(value, DELAY), {
      initialProps: { value: 'a' },
    });

    rerender({ value: 'b' });
    expect(result.current).toBe('a');

    advance(DELAY - 1);
    expect(result.current).toBe('a');

    advance(1);
    expect(result.current).toBe('b');
  });

  it('applies only the latest value after a rapid burst of changes', () => {
    const { result, rerender } = renderHook(({ value }) => useDebounced(value, DELAY), {
      initialProps: { value: '' },
    });

    // Simulate typing: each change lands well inside the debounce window.
    for (const value of ['n', 'ne', 'nex', 'nexu', 'nexus']) {
      rerender({ value });
      advance(50);
    }

    // Nothing intermediate was ever exposed — the window kept restarting.
    expect(result.current).toBe('');

    advance(DELAY);
    expect(result.current).toBe('nexus');
  });

  it('restarts the delay window on every change rather than applying on a fixed schedule', () => {
    const { result, rerender } = renderHook(({ value }) => useDebounced(value, DELAY), {
      initialProps: { value: 'a' },
    });

    rerender({ value: 'b' });
    advance(DELAY - 50);

    // A further change before the window closes discards the pending 'b'.
    rerender({ value: 'c' });
    advance(DELAY - 50);
    expect(result.current).toBe('a');

    advance(50);
    expect(result.current).toBe('c');
  });

  it('does not apply a pending value after unmount', () => {
    const { result, rerender, unmount } = renderHook(
      ({ value }) => useDebounced(value, DELAY),
      { initialProps: { value: 'a' } },
    );

    rerender({ value: 'b' });
    unmount();

    // The pending timer is cleared on unmount, so flushing it must not attempt a
    // state update on the unmounted hook.
    expect(() => advance(DELAY * 2)).not.toThrow();
    expect(result.current).toBe('a');
  });

  it('reschedules with the new delay when the delay changes', () => {
    const { result, rerender } = renderHook(({ value, delay }) => useDebounced(value, delay), {
      initialProps: { value: 'a', delay: DELAY },
    });

    rerender({ value: 'b', delay: 1000 });
    advance(DELAY);
    expect(result.current).toBe('a');

    advance(700);
    expect(result.current).toBe('b');
  });

  it('debounces non-string values', () => {
    const { result, rerender } = renderHook(({ value }) => useDebounced(value, DELAY), {
      initialProps: { value: 0 },
    });

    rerender({ value: 42 });
    expect(result.current).toBe(0);

    advance(DELAY);
    expect(result.current).toBe(42);
  });

  it('debounces a transition to an empty value, so clearing a filter also settles', () => {
    const { result, rerender } = renderHook(({ value }) => useDebounced(value, DELAY), {
      initialProps: { value: 'nexus' },
    });

    rerender({ value: '' });
    expect(result.current).toBe('nexus');

    advance(DELAY);
    expect(result.current).toBe('');
  });
});
