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

import { useRef, useState, useEffect } from 'react';

import { ExtJS } from '../../../../../interface/ExtJS';
import type { InstanceTotals } from './simplified.types';

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
  data: InstanceTotals | null;
  loading: boolean;
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

  useEffect(() => {
    const poll = () => {
      const newUsage = readUsageFromState();
      const newIsPostgres = readIsPostgresFromState();
      setUsage(newUsage);
      setIsPostgres(newIsPostgres);
    };

    const id = setInterval(poll, 500);
    return () => clearInterval(id);
  }, []);

  const hasReceivedDataRef = useRef(false);

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

  if (!allPresent && !hasReceivedDataRef.current) {
    return { data: null, loading: true };
  }

  if (allPresent) {
    hasReceivedDataRef.current = true;
  }

  return {
    data: {
      totalComponents: totalComponents ?? 0,
      peakRequestsPerDay: peakRequestsPerDay ?? 0,
      peakRequestsPerMonth: 0,
      totalComponentsLimit: findThreshold(usage, METRIC_NAMES.totalComponents),
      peakRequestsPerDayLimit:
        findThreshold(usage, peakRequestsPerDayMetricName) ||
        findThreshold(usage, peakRequestsPerDayFallbackName),
    },
    loading: false,
  };
}

export default useInstanceTotals;
