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

import { useCallback, useEffect, useRef } from 'react';

/**
 * Default polling cadence for live task status.
 *
 * 5 seconds mirrors the responsiveness of the Classic UI auto-refresh while
 * keeping load light: a single detail page (one GET) or a visible task list
 * (one GET) issues at most one request per interval, and only while there is
 * something worth observing (see the `enabled` gating used by the callers).
 */
export const POLL_INTERVAL_MS = 5000;

/**
 * Number of polls to keep running after a Run/Stop action even when nothing is
 * actively RUNNING yet. The server takes a moment to flip WAITING → RUNNING (and,
 * at the end of a run, RUNNING → WAITING with a fresh Last Result), so an idle
 * task is otherwise not polled. 6 × {@link POLL_INTERVAL_MS} ≈ 30s bridges those
 * transitions, after which polling goes quiet again if the task never started.
 * Shared by the detail and list pages so the window is defined in one place.
 */
export const POST_ACTION_POLL_COUNT = 6;

export interface UsePollingOptions {
  /** Polling cadence in milliseconds. Defaults to {@link POLL_INTERVAL_MS}. */
  intervalMs?: number;
  /**
   * Whether polling is active. When `false` the interval is torn down (this is
   * how callers express "terminal-state stop" / "nothing to watch"). Defaults
   * to `true`.
   */
  enabled?: boolean;
  /** Run one poll immediately when polling becomes enabled. Defaults to `true`. */
  pollOnEnable?: boolean;
  /**
   * Run a single catch-up poll when the tab returns to the foreground.
   * Defaults to `true`. This is intentionally one poll, not a flood of the
   * ticks that were skipped while hidden. Respects {@link enabled}: when polling
   * is disabled the visibility listener is not attached, so a tab-return does
   * not trigger a catch-up.
   */
  pollOnVisible?: boolean;
}

export interface UsePollingResult {
  /**
   * Trigger an immediate poll out-of-band (e.g. right after Run/Stop). Honours
   * the in-flight guard so it can never stack a second concurrent request, but
   * bypasses the visibility gate since it is only ever called from a user
   * gesture (the tab is, by definition, visible).
   */
  pollNow: () => void;
}

/**
 * Generic polling primitive used by the Preview UI task pages.
 *
 * Co-located with tasks (rather than promoted to shared utilities) because
 * tasks are currently the only consumer; extract later if a second caller
 * appears. It encapsulates every concern the page components must NOT
 * reimplement as scattered `setInterval`s:
 *
 *  - interval setup + teardown keyed on `enabled`/`intervalMs`
 *  - `document.visibilityState` gating (skip ticks while the tab is hidden)
 *  - a single catch-up poll on return to visibility (no burst)
 *  - skip-when-busy: never overlap requests
 *  - stale-response safety: nothing runs after unmount
 *  - silent error swallowing so a transient failure does not surface a toast
 *    or break the UI; a later successful poll recovers state
 *  - clean unmount / disable teardown (no dangling intervals or listeners)
 *
 * The latest `callback` is always invoked without resetting the interval, so
 * callers can pass a fresh closure each render without restarting the timer.
 */
export function usePolling(
  callback: () => void | Promise<void>,
  {
    intervalMs = POLL_INTERVAL_MS,
    enabled = true,
    pollOnEnable = true,
    pollOnVisible = true,
  }: UsePollingOptions = {},
): UsePollingResult {
  const savedCallback = useRef(callback);
  const inFlightRef = useRef(false);
  const mountedRef = useRef(true);

  // Keep the ref pointing at the latest callback so the interval can call
  // through to current state/props without being torn down and recreated.
  useEffect(() => {
    savedCallback.current = callback;
  }, [callback]);

  // Re-arming `mountedRef.current = true` here is required, not redundant: under
  // React StrictMode the mount effect runs twice on the SAME ref instance
  // (mount → cleanup → mount), so the first (simulated) unmount sets it false and
  // the second mount must set it back to true — otherwise polling would never run.
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // One guarded execution. Stable identity (only refs are referenced) so the
  // interval effect below is not re-created when the caller's callback changes.
  // `skipVisibilityGate` lets an out-of-band poll (pollNow, from a user gesture)
  // run even when the tab's visibility would skip it. It does NOT bypass the
  // in-flight guard below — so pollNow can never start a second concurrent request.
  const runPoll = useCallback(async (skipVisibilityGate = false) => {
    if (!mountedRef.current) {
      return;
    }
    if (!skipVisibilityGate && typeof document !== 'undefined' && document.visibilityState !== 'visible') {
      return; // visibility gating: skip work while the tab is backgrounded
    }
    if (inFlightRef.current) {
      return; // skip-when-busy: never run two polls concurrently (applies to pollNow too)
    }
    inFlightRef.current = true;
    try {
      await savedCallback.current();
    }
    catch {
      // Background-poll failures are swallowed by design: no toast, no error
      // state. The next successful tick reconciles the displayed state.
    }
    finally {
      inFlightRef.current = false;
    }
  }, []);

  const pollNow = useCallback(() => {
    void runPoll(true); // skip the visibility gate; still honours the in-flight guard
  }, [runPoll]);

  // Interval lifecycle. Keyed only on enabled/interval/pollOnEnable so the
  // timer is stable across unrelated re-renders.
  useEffect(() => {
    if (!enabled) {
      return undefined;
    }
    if (pollOnEnable) {
      void runPoll();
    }
    const id = setInterval(() => {
      void runPoll();
    }, intervalMs);
    return () => clearInterval(id);
  }, [enabled, intervalMs, pollOnEnable, runPoll]);

  // Resume with a single catch-up poll when the tab becomes visible again.
  // Gated on `enabled`: when polling is disabled (idle/terminal task) the listener
  // is not attached, so returning to the tab does not fire a catch-up poll.
  useEffect(() => {
    if (!enabled || !pollOnVisible) {
      return undefined;
    }
    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        void runPoll();
      }
    };
    document.addEventListener('visibilitychange', onVisibilityChange);
    return () => document.removeEventListener('visibilitychange', onVisibilityChange);
  }, [enabled, pollOnVisible, runPoll]);

  return { pollNow };
}

export default usePolling;
