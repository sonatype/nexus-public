/*
 * Copyright (c) 2008-present Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import { useState, useEffect, useCallback } from 'react';
import { restClient, parseApiError, isNotFoundError, isPermissionError } from '@/utils/api';

export interface DataPoint {
  date: string;
  value: number;
}

export interface UsageHistoryResponse {
  metric: string;
  period: string;
  data: DataPoint[];
}

export interface UsageHistoryState {
  requestsDaily: DataPoint[];
  requestsMonthly: DataPoint[];
  componentsDaily: DataPoint[];
  componentsMonthly: DataPoint[];
  loading: boolean;
  error: string | null;
}

const API_BASE = '/service/rest/v1/usage-history';

/**
 * Hook to fetch usage history data for sparklines.
 * 
 * Fetches historical data for:
 * - Request counts (daily and monthly)
 * - Component counts (daily and monthly)
 */
export function useUsageHistory(skip = false): UsageHistoryState & { refresh: () => void } {
  const [state, setState] = useState<UsageHistoryState>({
    requestsDaily: [],
    requestsMonthly: [],
    componentsDaily: [],
    componentsMonthly: [],
    loading: !skip,
    error: null
  });

  const fetchMetric = useCallback(async (
    metric: string,
    period: string
  ): Promise<DataPoint[]> => {
    try {
      const res = await restClient.get<UsageHistoryResponse>(
        `${API_BASE}?metric=${metric}&period=${period}`
      );
      return res?.data || [];
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      // 404 = API doesn't exist in this edition; 403 = user lacks nexus:metrics:read
      if (isNotFoundError(apiError) || isPermissionError(apiError)) {
        return [];
      }
      throw err;
    }
  }, []);

  const fetchData = useCallback(async () => {
    setState(prev => ({ ...prev, loading: true, error: null }));

    try {
      const [requestsDaily, requestsMonthly, componentsDaily, componentsMonthly] =
        await Promise.all([
          fetchMetric('requests', 'daily'),
          fetchMetric('requests', 'monthly'),
          fetchMetric('components', 'daily'),
          fetchMetric('components', 'monthly'),
        ]);

      setState({
        requestsDaily,
        requestsMonthly,
        componentsDaily,
        componentsMonthly,
        loading: false,
        error: null,
      });
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      // Don't log 403 - user simply lacks nexus:metrics:read (expected for non-admins)
      if (!isPermissionError(apiError)) {
        console.error('Failed to fetch usage history:', err);
      }
      setState((prev) => ({
        ...prev,
        loading: false,
        error: apiError.message,
      }));
    }
  }, [fetchMetric]);

  useEffect(() => {
    if (skip) return;
    fetchData();
  }, [fetchData, skip]);

  return {
    ...state,
    refresh: fetchData
  };
}

export default useUsageHistory;

