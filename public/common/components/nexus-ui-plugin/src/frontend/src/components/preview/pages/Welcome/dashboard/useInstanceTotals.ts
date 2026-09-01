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

import { useCallback, useRef, useState, useEffect } from 'react';

import { ExtJS } from '../../../../../interface/ExtJS';
import type { InstanceTotals } from './simplified.types';

/**
 * Upper bound on how long the card may stay in the loading state.
 *
 * The metrics come from the ExtJS `contentUsageEvaluationResult` state, which
 * the backend only populates after the first content-usage aggregation runs
 * (see ContentUsageEvaluatorManager#evaluateUsage). Before that first run the
 * state is an empty list, so without a bound the card would spin forever when
 * a page load races ahead of the aggregation. After this window the hook
 * reports an error/timeout instead of loading, so the UI can surface it and
 * offer a retry rather than hanging.
 */
const LOADING_TIMEOUT_MS = 15000;

/**
 * How often the hook re-reads ExtJS state. Polling (rather than a
 * datachanged listener) is required because the ExtJS application may not be
 * ready at mount time in Preview UI context.
 */
const POLL_INTERVAL_MS = 500;

/**
 * Metric names from the contentUsageEvaluationResult ExtJS state.
 *
 * Some metrics use different names on PostgreSQL vs H2; the hook reads
 * datastore.isPostgresql to pick the correct one.
 */
const METRIC_NAMES = {
  totalComponents: 'component_total_count',
  peakRequestsPerDay: {
    h2: 'peak_requests_per_day',
    postgresql: 'peak_requests_per_day_30d',
  },
} as const;

interface UseInstanceTotalsResult {
  /** Full metrics, present only once every required metric has been seen. */
  data: InstanceTotals | null;
  /** True until all required metrics arrive or the loading window elapses. */
  loading: boolean;
  /**
   * Decoupled view for the compact Components stat card. That card only needs
   * component_total_count and must not be blocked by the peak-requests metric
   * (which it never displays); these fields resolve independently of `data`.
   */
  componentCount: number | null;
  componentLimit: number;
  componentsLoading: boolean;
  componentsError: boolean;
  /** Restart the loading window and re-read state immediately. */
  retry: () => void;
}

interface UsageEntry {
  metricName: string;
  metricValue?: number | null;
  thresholds?: Array<{ thresholdName: string; thresholdValue: number }>;
}

function findThreshold(
  usage: UsageEntry[] | null | undefined,
  metricName: string
): number {
  if (!Array.isArray(usage)) {
    return 0;
  }
  const entry = usage.find((m) => m?.metricName === metricName);
  if (!entry || !Array.isArray(entry.thresholds)) {
    return 0;
  }
  const hard = entry.thresholds.find((t) => t?.thresholdName === 'HARD_THRESHOLD');
  return hard?.thresholdValue ?? 0;
}

/**
 * Returns the metricValue for the named entry, or undefined if the entry is
 * missing or its value is null/undefined. This distinguishes "metric is not
 * yet present in the state snapshot" from "metric is genuinely 0".
 */
function findMetric(
  usage: UsageEntry[] | null | undefined,
  metricName: string
): number | undefined {
  if (!Array.isArray(usage)) {
    return undefined;
  }
  const entry = usage.find((m) => m?.metricName === metricName);
  if (!entry || entry.metricValue == null) {
    return undefined;
  }
  return entry.metricValue;
}

function readUsageFromState(): UsageEntry[] | null | undefined {
  try {
    return ExtJS.state().getValue('contentUsageEvaluationResult', []) as UsageEntry[] | null | undefined;
  } catch {
    return undefined;
  }
}

function readIsPostgresFromState(): boolean {
  try {
    return Boolean(ExtJS.state().getValue('datastore.isPostgresql'));
  } catch {
    return false;
  }
}

/**
 * Hook that exposes instance-wide usage metrics from ExtJS state.
 *
 * Polls ExtJS.state() directly on a 500 ms interval so it works even when
 * the ExtJS application is not yet ready at mount time (which would cause
 * ExtJS.useState datachanged listeners to never fire in Preview UI context).
 *
 * Loading is sticky: once all required metrics have been seen at least once,
 * loading stays false even if the state snapshot transiently empties.
 */
export function useInstanceTotals(): UseInstanceTotalsResult {
  const [usage, setUsage] = useState<UsageEntry[] | null | undefined>(() => readUsageFromState());
  const [isPostgres, setIsPostgres] = useState<boolean>(() => readIsPostgresFromState());
  const [timedOut, setTimedOut] = useState(false);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const poll = () => {
      const newUsage = readUsageFromState();
      const newIsPostgres = readIsPostgresFromState();
      setUsage(newUsage);
      setIsPostgres(newIsPostgres);
    };

    const id = setInterval(poll, POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, []);

  // Bound the loading state so the card can never spin forever when the
  // backend has not populated contentUsageEvaluationResult yet. The window
  // restarts whenever retry() bumps `attempt`. Polling keeps running, so if
  // the metrics do arrive after the timeout the card recovers on its own.
  useEffect(() => {
    setTimedOut(false);
    const id = setTimeout(() => setTimedOut(true), LOADING_TIMEOUT_MS);
    return () => clearTimeout(id);
  }, [attempt]);

  const retry = useCallback(() => {
    setUsage(readUsageFromState());
    setIsPostgres(readIsPostgresFromState());
    setAttempt((a) => a + 1);
  }, []);

  const hasReceivedAllRef = useRef(false);
  const hasReceivedComponentsRef = useRef(false);
  // Last non-empty component count. The state snapshot can transiently empty on
  // a poll after the metric has been seen; without this cache componentCount
  // would momentarily report 0 (totalComponents ?? 0) instead of the last known
  // value, flashing "0" in the card.
  const lastComponentCountRef = useRef<number | null>(null);

  // Try the isPostgres-preferred name first, then fall back to the other.
  // This handles cases where isPostgres state hasn't loaded yet, or the
  // backend sends only one variant regardless of what isPostgresql reports.
  const peakRequestsPerDayMetricName = isPostgres
    ? METRIC_NAMES.peakRequestsPerDay.postgresql
    : METRIC_NAMES.peakRequestsPerDay.h2;
  const peakRequestsPerDayFallbackName = isPostgres
    ? METRIC_NAMES.peakRequestsPerDay.h2
    : METRIC_NAMES.peakRequestsPerDay.postgresql;

  const totalComponents = findMetric(usage, METRIC_NAMES.totalComponents);
  const peakRequestsPerDay =
    findMetric(usage, peakRequestsPerDayMetricName) ??
    findMetric(usage, peakRequestsPerDayFallbackName);

  // peakRequestsPerMonth ('highestMonthlyRequestMetrics') is NOT present in
  // contentUsageEvaluationResult — it comes from /service/rest/v1/monthly-metrics.
  // Do not include it in the allPresent gate; return 0 as a safe default.
  const allPresent =
    totalComponents !== undefined &&
    peakRequestsPerDay !== undefined;

  // Loading is sticky: once a metric has been seen it stays "received" even if
  // the state snapshot transiently empties on a later poll.
  //
  // These refs are intentionally mutated in the render body rather than in an
  // effect. The writes are idempotent (they only ever flip false -> true), so
  // re-running render — e.g. under StrictMode or concurrent rendering —
  // produces the same result. Moving them to a useEffect would delay the flip
  // by one commit, so the render in which a metric first arrives would still
  // read the stale `false` and briefly report loading, causing a flash.
  if (totalComponents !== undefined) {
    hasReceivedComponentsRef.current = true;
    lastComponentCountRef.current = totalComponents;
  }
  if (allPresent) {
    hasReceivedAllRef.current = true;
  }

  const componentsReady = hasReceivedComponentsRef.current;
  const allReady = hasReceivedAllRef.current;

  const componentLimit = findThreshold(usage, METRIC_NAMES.totalComponents);

  return {
    data: allReady
      ? {
          totalComponents: totalComponents ?? 0,
          peakRequestsPerDay: peakRequestsPerDay ?? 0,
          peakRequestsPerMonth: 0,
          totalComponentsLimit: componentLimit,
          peakRequestsPerDayLimit:
            findThreshold(usage, peakRequestsPerDayMetricName) ||
            findThreshold(usage, peakRequestsPerDayFallbackName),
        }
      : null,
    loading: !allReady && !timedOut,
    componentCount: componentsReady ? (totalComponents ?? lastComponentCountRef.current ?? 0) : null,
    componentLimit,
    componentsLoading: !componentsReady && !timedOut,
    componentsError: !componentsReady && timedOut,
    retry,
  };
}

export default useInstanceTotals;
