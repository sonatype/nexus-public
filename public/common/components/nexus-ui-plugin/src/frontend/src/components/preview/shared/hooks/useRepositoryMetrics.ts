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
import { fetchRepositoryMetricsDetail } from './fetchRepositoryMetricsDetail';

export interface RepositoryMetrics {
  componentCount: number;
  assetCount: number;
  totalSize: number;
}

export interface UseRepositoryMetricsResult {
  data: RepositoryMetrics | null;
  isLoading: boolean;
  error: string | null;
}

async function fetchMetrics(repositoryName: string): Promise<RepositoryMetrics> {
  try {
    const detail = await fetchRepositoryMetricsDetail(repositoryName);

    return {
      componentCount: detail?.componentCount || 0,
      assetCount: detail?.assetCount || 0,
      totalSize: detail?.size || 0,
    };
  } catch (err) {
    console.warn('Could not fetch repository metrics:', err);
    throw err;
  }
}

/**
 * Note: unlike repositoryUsageMachine.ts, this hook coerces missing metric
 * fields to 0 rather than preserving `undefined`. Callers cannot distinguish
 * "not yet calculated" from a genuine zero through this hook.
 */
export function useRepositoryMetrics(repositoryName: string): UseRepositoryMetricsResult {
  const [data, setData] = useState<RepositoryMetrics | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!repositoryName) {
      setIsLoading(false);
      return;
    }

    let cancelled = false;
    setIsLoading(true);
    setError(null);

    fetchMetrics(repositoryName)
      .then((result) => {
        if (!cancelled) {
          setData(result);
          setIsLoading(false);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          const errorMessage = err instanceof Error ? err.message : 'Failed to fetch repository metrics';
          setError(errorMessage);
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [repositoryName]);

  return { data, isLoading, error };
}
