/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 */

import { useCallback, useEffect, useState } from 'react';
import { restClient, ENDPOINTS } from '../../../../interface/api';

export interface ArtifactSecurityItem {
  id?: string;
  group?: string;
  name?: string;
  version?: string;
  format?: string;
  repository?: string;
  criticalCount?: number;
  severeCount?: number;
  moderateCount?: number;
  licenseThreatLevel?: number;
  licenseThreatName?: string;
}

interface ArtifactListResponse {
  items: ArtifactSecurityItem[];
  continuationToken?: string | null;
}

export interface UseArtifactListResult {
  items: ArtifactSecurityItem[];
  loading: boolean;
  error: string | null;
  hasMore: boolean;
  loadMore: () => Promise<void>;
  refresh: () => Promise<void>;
  /** True if endpoint returned data; false if 404/403; null if not yet tried */
  endpointAvailable: boolean | null;
}

/**
 * Fetches paginated artifact list with security data for Security Report.
 * Falls back gracefully when the endpoint is not available (e.g. 404).
 */
export function useArtifactList(
  repositoryName: string,
  reportType: 'health-check' | 'firewall'
): UseArtifactListResult {
  const [items, setItems] = useState<ArtifactSecurityItem[]>([]);
  const [continuationToken, setContinuationToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [endpointAvailable, setEndpointAvailable] = useState<boolean | null>(null);

  const fetchPage = useCallback(
    async (token: string | null = null) => {
      if (!(repositoryName && reportType)) return;

      setLoading(true);
      setError(null);

      try {
        const params = new URLSearchParams();
        params.set('repository', repositoryName);
        params.set('reportType', reportType);
        if (token) {
          params.set('continuationToken', token);
        }

        const response = await restClient.get<ArtifactListResponse>(
          `${ENDPOINTS.SECURITY_REPORT_ARTIFACTS}?${params.toString()}`
        );

        if (endpointAvailable === null) {
          setEndpointAvailable(true);
        }

        const newItems = response?.items ?? [];
        const nextToken = response?.continuationToken ?? null;

        setItems((prev) => (token ? [...prev, ...newItems] : newItems));
        setContinuationToken(nextToken);
      } catch (err: unknown) {
        const status = (err as { response?: { status?: number } })?.response?.status;
        if (status === 404 || status === 403) {
          setEndpointAvailable(false);
          setItems([]);
          setContinuationToken(null);
        } else {
          setError(err instanceof Error ? err.message : 'Failed to load artifacts');
        }
      } finally {
        setLoading(false);
      }
    },
    [repositoryName, reportType, endpointAvailable]
  );

  const loadMore = useCallback(async () => {
    if (continuationToken && endpointAvailable !== false) {
      await fetchPage(continuationToken);
    }
  }, [continuationToken, endpointAvailable, fetchPage]);

  const refresh = useCallback(async () => {
    setItems([]);
    setContinuationToken(null);
    setEndpointAvailable(null);
    await fetchPage(null);
  }, [fetchPage]);

  useEffect(() => {
    if (repositoryName && reportType) {
      fetchPage(null);
    }
  }, [repositoryName, reportType, fetchPage]); 

  return {
    items,
    loading,
    error,
    hasMore: Boolean(continuationToken),
    loadMore,
    refresh,
    endpointAvailable: endpointAvailable ?? null,
  };
}
