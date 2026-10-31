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

import { useState, useEffect } from 'react';
import { restClient } from '../../interface/api';
import type { AuditLogResponse, AuditFilters } from './audit.types';

interface UseAuditLogApiParams {
  filters: AuditFilters;
  page: number;
  limit: number;
}

interface UseAuditLogApiResult {
  data: AuditLogResponse | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

/**
 * Hook for fetching audit log events from the REST API.
 */
export function useAuditLogApi({
  filters,
  page,
  limit,
}: UseAuditLogApiParams): UseAuditLogApiResult {
  const [data, setData] = useState<AuditLogResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [refetchTrigger, setRefetchTrigger] = useState(0);

  useEffect(() => {
    let cancelled = false;

    const fetchData = async () => {
      setLoading(true);
      setError(null);

      try {
        // Build query parameters
        const params = new URLSearchParams();
        params.append('page', page.toString());
        params.append('limit', limit.toString());

        // Add category filters
        for (const cat of filters.categories) {
          params.append('categories', cat);
        }

        // Add event type filters
        for (const type of filters.eventTypes) {
          params.append('types', type);
        }

        // Add initiator filters
        for (const init of filters.initiators) {
          params.append('initiators', init);
        }

        // Add date range
        if (filters.dateRange !== 'custom') {
          const endDate = new Date().toISOString();
          const startDate = getStartDateForRange(filters.dateRange);
          params.append('startDate', startDate);
          params.append('endDate', endDate);
        } else if (filters.customStartDate && filters.customEndDate) {
          params.append('startDate', filters.customStartDate);
          params.append('endDate', filters.customEndDate);
        }

        // Add search query (future enhancement - backend needs to support this)
        if (filters.searchQuery) {
          params.append('q', filters.searchQuery);
        }

        // Add repository filter
        if (filters.repositoryName) {
          params.append('repositoryName', filters.repositoryName);
        }

        const url = `/service/rest/internal/ui/audit-log?${params.toString()}`;
        const response = await restClient.get<AuditLogResponse>(url);

        if (!cancelled) {
          setData(response);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to fetch audit log');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchData();

    return () => {
      cancelled = true;
    };
    // refetchTrigger is intentionally a dependency to enable manual refetch via setRefetchTrigger
  }, [page, limit, refetchTrigger, filters]);

  const refetch = () => {
    setRefetchTrigger((prev) => prev + 1);
  };

  return { data, loading, error, refetch };
}

/**
 * Calculate start date based on date range selection.
 */
function getStartDateForRange(range: string): string {
  const now = new Date();
  let days = 30; // default

  switch (range) {
    case 'last-24-hours':
      days = 1;
      break;
    case 'last-7-days':
      days = 7;
      break;
    case 'last-30-days':
      days = 30;
      break;
    case 'last-90-days':
      days = 90;
      break;
  }

  const startDate = new Date(now);
  startDate.setDate(now.getDate() - days);
  return startDate.toISOString();
}
