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

import { useState, useEffect, useCallback } from 'react';
import { restClient, parseApiError, isNotFoundError, isPermissionError } from '../../../../../interface/api';

const MONTHLY_METRICS_URL = '/service/rest/v1/monthly-metrics';

/**
 * Monthly metric record from GET /service/rest/v1/monthly-metrics.
 *
 * Self-hosted (MonthlyMetricsApiResource): peakStorage, responseSize (numeric)
 * Cloud (nexus-api-rest-cloud): storage, egress (numeric or "N/A" when 0)
 *
 * @see private/selfhosted/.../MonthlyMetricsApiResource.java - createMetricMap puts peakStorage, responseSize
 * @see private/cloud/.../MonthlyMetricsApiResource.java - createMetricMap puts storage, egress
 */
export interface MonthlyMetricRecord {
  metricDate?: string;
  /** Self-hosted: peak storage in bytes */
  peakStorage?: number;
  /** Self-hosted: egress/response size in bytes */
  responseSize?: number;
  /** Cloud: storage in bytes (or "N/A" string when 0) */
  storage?: number | string;
  /** Cloud: egress in bytes (or "N/A" string when 0) */
  egress?: number | string;
  requestCount?: number;
  componentCount?: number;
}

export interface MonthlyDataPoint {
  date: string;
  value: number;
}

export interface MonthlyMetricsHistory {
  storage: MonthlyDataPoint[];
  egress: MonthlyDataPoint[];
  requests: MonthlyDataPoint[];
  components: MonthlyDataPoint[];
}

export interface MonthlyMetricsResult {
  /** Peak storage in bytes (latest month) */
  peakStorage: number | null;
  /** Egress/response size in bytes (latest month) */
  responseSize: number | null;
  /** 12-month history for charts (oldest first) */
  history: MonthlyMetricsHistory;
  loading: boolean;
  error: string | null;
}

/**
 * Format bytes as GB string for display.
 * @param allowZero - When true, 0 returns "0.00 GB" instead of "N/A" (for Egress when no data yet)
 */
export function formatBytesToGB(bytes: number | null | undefined, allowZero = false): string {
  if (bytes == null) return 'N/A';
  if (bytes === 0) return allowZero ? '0.00 GB' : 'N/A';
  const gb = bytes / 1e9;
  return `${gb.toFixed(2)} GB`;
}

/**
 * Convert API value to bytes (number). Handles Cloud "N/A" string and both field names.
 */
function toBytes(
  value: number | string | null | undefined
): number | null {
  if (value == null) return null;
  if (value === 'N/A' || value === '') return null;
  const n = typeof value === 'string' ? parseInt(value, 10) : value;
  return Number.isNaN(n) ? null : n;
}

function toNumber(
  value: number | string | null | undefined
): number | null {
  if (value == null) return null;
  if (value === 'N/A' || value === '') return null;
  const n = typeof value === 'string' ? parseInt(value, 10) : value;
  return Number.isNaN(n) ? null : n;
}

/**
 * Hook to fetch monthly metrics (Storage, Egress) from GET /service/rest/v1/monthly-metrics.
 * Returns latest month's peakStorage and responseSize.
 * Gracefully handles 404 (API may not exist in all editions).
 */
const EMPTY_HISTORY: MonthlyMetricsHistory = {
  storage: [],
  egress: [],
  requests: [],
  components: [],
};

export function useMonthlyMetrics(): MonthlyMetricsResult {
  const [state, setState] = useState<MonthlyMetricsResult>({
    peakStorage: null,
    responseSize: null,
    history: EMPTY_HISTORY,
    loading: true,
    error: null,
  });

  const fetchData = useCallback(async () => {
    setState((prev) => ({ ...prev, loading: true, error: null }));

    try {
      const data = await restClient.get<MonthlyMetricRecord[]>(MONTHLY_METRICS_URL);
      const records = Array.isArray(data) ? data : [];
      const latest = records[0] ?? records[records.length - 1];

      // Build 12-month history (oldest first for charts)
      const reversed = [...records].reverse();
      const history: MonthlyMetricsHistory = {
        storage: reversed
          .map((r) => ({
            date: r.metricDate || '',
            value: toBytes(r.peakStorage ?? r.storage) ?? 0,
          }))
          .filter((p) => p.date),
        egress: reversed
          .map((r) => ({
            date: r.metricDate || '',
            value: toBytes(r.responseSize ?? r.egress) ?? 0,
          }))
          .filter((p) => p.date),
        requests: reversed
          .map((r) => ({
            date: r.metricDate || '',
            value: toNumber(r.requestCount) ?? 0,
          }))
          .filter((p) => p.date),
        components: reversed
          .map((r) => ({
            date: r.metricDate || '',
            value: toNumber(r.componentCount) ?? 0,
          }))
          .filter((p) => p.date),
      };

      if (latest) {
        const peakStorage = toBytes(latest.peakStorage ?? latest.storage);
        const responseSize = toBytes(latest.responseSize ?? latest.egress);

        setState({
          peakStorage,
          responseSize,
          history,
          loading: false,
          error: null,
        });
      } else {
        setState({
          peakStorage: null,
          responseSize: null,
          history,
          loading: false,
          error: null,
        });
      }
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      // 404 = API doesn't exist; 403 = user lacks nexus:metrics:read
      if (isNotFoundError(apiError) || isPermissionError(apiError)) {
        setState({
          peakStorage: null,
          responseSize: null,
          history: EMPTY_HISTORY,
          loading: false,
          error: null,
        });
        return;
      }
      console.warn('Failed to fetch monthly metrics:', err);
      setState((prev) => ({
        ...prev,
        loading: false,
        error: apiError.message,
      }));
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return state;
}

export default useMonthlyMetrics;
