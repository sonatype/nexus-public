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
import { usePolling, POLL_INTERVAL_MS } from '../usePolling';

/**
 * Override document.visibilityState (a read-only getter in jsdom) so we can
 * exercise the hook's visibility gating. Restored in afterEach.
 */
function setVisibility(state: 'visible' | 'hidden') {
  Object.defineProperty(document, 'visibilityState', {
    configurable: true,
    get: () => state,
  });
  document.dispatchEvent(new Event('visibilitychange'));
}

// Flush pending microtasks (the hook's in-flight guard resets in a `finally`
// that runs as a microtask after the awaited callback settles).
const flush = () => act(async () => { await Promise.resolve(); });

// Advance fake timers by one or more intervals, flushing microtasks on either
// side. The leading flush settles the *previous* poll so its in-flight guard
// clears before the next tick fires; the trailing flush settles the poll the
// tick just started. Without this the next interval would (correctly) be
// skipped as "busy" because the prior poll's `finally` had not yet run. With
// real 5s timers this never happens — the microtask settles long before the
// next tick.
async function advance(ms: number) {
  await act(async () => {
    await Promise.resolve();
    jest.advanceTimersByTime(ms);
    await Promise.resolve();
  });
}

describe('usePolling', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    Object.defineProperty(document, 'visibilityState', {
      configurable: true,
      get: () => 'visible',
    });
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it('exposes a sensible default interval', () => {
    expect(POLL_INTERVAL_MS).toBe(5000);
  });

  it('polls immediately on enable, then once per interval', async () => {
    const cb = jest.fn();
    renderHook(() => usePolling(cb, { intervalMs: 5000, enabled: true }));

    // pollOnEnable fires one immediate poll.
    expect(cb).toHaveBeenCalledTimes(1);

    await advance(5000);
    expect(cb).toHaveBeenCalledTimes(2);

    await advance(5000);
    expect(cb).toHaveBeenCalledTimes(3);
  });

  it('does not poll while disabled', async () => {
    const cb = jest.fn();
    renderHook(() => usePolling(cb, { enabled: false }));

    expect(cb).not.toHaveBeenCalled();
    await act(async () => { jest.advanceTimersByTime(5000 * 3); });
    expect(cb).not.toHaveBeenCalled();
  });

  it('stops polling when enabled flips to false (terminal-state stop)', async () => {
    const cb = jest.fn();
    const { rerender } = renderHook(
      ({ enabled }) => usePolling(cb, { intervalMs: 5000, enabled }),
      { initialProps: { enabled: true } },
    );

    expect(cb).toHaveBeenCalledTimes(1);
    await advance(5000);
    expect(cb).toHaveBeenCalledTimes(2);

    rerender({ enabled: false });
    await advance(5000 * 5);
    expect(cb).toHaveBeenCalledTimes(2); // no further polls after disable
  });

  it('skips ticks while the tab is hidden and resumes on visibility change', async () => {
    setVisibility('hidden');
    const cb = jest.fn();
    renderHook(() => usePolling(cb, { intervalMs: 5000, enabled: true }));

    // Immediate poll is skipped because the tab is hidden.
    expect(cb).not.toHaveBeenCalled();
    await advance(5000 * 2);
    expect(cb).not.toHaveBeenCalled();

    // Returning to visible triggers exactly ONE catch-up poll (no burst).
    act(() => setVisibility('visible'));
    expect(cb).toHaveBeenCalledTimes(1);

    await advance(5000);
    expect(cb).toHaveBeenCalledTimes(2);
  });

  it('never overlaps requests (skip-when-busy)', async () => {
    let resolvePending: () => void = () => {};
    const cb = jest.fn(() => new Promise<void>((resolve) => { resolvePending = resolve; }));
    renderHook(() => usePolling(cb, { intervalMs: 5000, enabled: true }));

    // First poll is in flight (promise unresolved).
    expect(cb).toHaveBeenCalledTimes(1);

    // A tick while the previous request is still pending is skipped.
    await act(async () => { jest.advanceTimersByTime(5000); });
    expect(cb).toHaveBeenCalledTimes(1);

    // Once the in-flight request settles, the next tick polls again.
    await act(async () => { resolvePending(); await Promise.resolve(); });
    await act(async () => { jest.advanceTimersByTime(5000); });
    expect(cb).toHaveBeenCalledTimes(2);
  });

  it('stops polling after unmount', async () => {
    const cb = jest.fn();
    const { unmount } = renderHook(() => usePolling(cb, { intervalMs: 5000, enabled: true }));

    expect(cb).toHaveBeenCalledTimes(1);
    unmount();
    await act(async () => { jest.advanceTimersByTime(5000 * 3); });
    expect(cb).toHaveBeenCalledTimes(1); // no polls after unmount
  });

  it('swallows callback errors and recovers on a later poll', async () => {
    let calls = 0;
    const cb = jest.fn(() => {
      calls += 1;
      if (calls === 1) throw new Error('transient background failure');
    });
    renderHook(() => usePolling(cb, { intervalMs: 5000, enabled: true }));

    // First (immediate) poll throws but the error is swallowed.
    expect(cb).toHaveBeenCalledTimes(1);
    await flush();

    // Polling keeps going; a later poll succeeds.
    await act(async () => { jest.advanceTimersByTime(5000); });
    expect(cb).toHaveBeenCalledTimes(2);
  });

  it('pollNow triggers an immediate poll even when the interval is disabled', async () => {
    const cb = jest.fn();
    const { result } = renderHook(() => usePolling(cb, { enabled: false }));

    expect(cb).not.toHaveBeenCalled();
    act(() => { result.current.pollNow(); });
    expect(cb).toHaveBeenCalledTimes(1);
  });

  it('pollNow is a no-op while a poll is already in-flight', async () => {
    let resolvePending: () => void = () => {};
    const cb = jest.fn(() => new Promise<void>((resolve) => { resolvePending = resolve; }));
    const { result } = renderHook(() => usePolling(cb, { enabled: false }));

    // First pollNow starts a poll that stays in-flight (promise unresolved).
    act(() => { result.current.pollNow(); });
    expect(cb).toHaveBeenCalledTimes(1);

    // A second pollNow while the first is still in-flight must be a no-op — pollNow
    // skips the visibility gate but still honours the in-flight guard.
    act(() => { result.current.pollNow(); });
    expect(cb).toHaveBeenCalledTimes(1);

    // Once the in-flight poll settles, pollNow runs again.
    await act(async () => { resolvePending(); await Promise.resolve(); });
    act(() => { result.current.pollNow(); });
    expect(cb).toHaveBeenCalledTimes(2);
  });
});
