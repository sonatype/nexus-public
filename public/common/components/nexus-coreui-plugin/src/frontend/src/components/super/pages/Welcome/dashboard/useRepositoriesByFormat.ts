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

import { useState, useEffect, useMemo } from 'react';
import { restClient, parseApiError } from '@/utils/api';
import { isHealthCheckSupportedFormat } from '@/utils/healthCheckFormats';

// Import types from simplified.types.ts - REAL DATA ONLY
import type { Repository, RepositoryFormatSummary } from './simplified.types';

/** Response from GET /service/rest/internal/ui/malware/counts */
interface MalwareCountsApiResponse {
  counts: Record<string, number>;
  totalCount: number;
  hdsAvailable: boolean;
  hcEnabledRepos: string[];
}

/**
 * Return type for useRepositoriesByFormat hook.
 */
export interface UseRepositoriesByFormatResult {
  /** Aggregated repository data by format */
  data: RepositoryFormatSummary[];
  /** Loading state */
  loading: boolean;
  /** Error message if any */
  error?: string;
  /** Function to manually refetch */
  refetch: () => void;
}

/**
 * Format code to display name mapping.
 */
const FORMAT_NAMES: Record<string, string> = {
  maven2: 'Maven',
  npm: 'npm',
  docker: 'Docker',
  nuget: 'NuGet',
  pypi: 'PyPI',
  raw: 'Raw',
  helm: 'Helm',
  go: 'Go',
  rubygems: 'RubyGems',
  yum: 'Yum',
  apt: 'Apt',
  conan: 'Conan',
  conda: 'Conda',
  p2: 'P2',
  r: 'R',
  gitlfs: 'Git LFS',
  cocoapods: 'CocoaPods',
  composer: 'Composer',
  swift: 'Swift',
  cargo: 'Cargo',
};

/**
 * Hook to fetch repositories and aggregate them by format.
 * Uses REAL DATA ONLY from GET /service/rest/v1/repositories
 * 
 * @returns Object containing aggregated data, loading state, error, and refetch function
 */
async function fetchMalwareCountsSafe(): Promise<MalwareCountsApiResponse | null> {
  try {
    return await restClient.get<MalwareCountsApiResponse>(
      '/service/rest/internal/ui/malware/counts'
    );
  } catch (err) {
    // 404 when endpoint or plugin is unavailable (e.g. CE); omit malware aggregates for any failure
    void parseApiError(err);
    return null;
  }
}

export function useRepositoriesByFormat(): UseRepositoriesByFormatResult {
  const [repositories, setRepositories] = useState<Repository[]>([]);
  const [malwareCounts, setMalwareCounts] = useState<MalwareCountsApiResponse | null | undefined>(
    undefined
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | undefined>();

  const fetchRepositories = async () => {
    setLoading(true);
    setError(undefined);

    try {
      const [repoData, malwareData] = await Promise.all([
        restClient.get<Repository[]>('/service/rest/v1/repositories'),
        fetchMalwareCountsSafe(),
      ]);
      setRepositories(repoData || []);
      setMalwareCounts(malwareData);
    } catch (err) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      setMalwareCounts(undefined);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRepositories();
  }, []);

  // Group by format - REAL DATA ONLY
  const data = useMemo((): RepositoryFormatSummary[] => {
    if (repositories.length === 0) {
      return [];
    }

    const malwareAvailable = malwareCounts != null;
    const countByRepo = malwareCounts?.counts ?? {};
    const hcEnabledSet = new Set(malwareCounts?.hcEnabledRepos ?? []);

    const groups = new Map<string, {
      proxy: number;
      hosted: number;
      group: number;
      online: number;
      offline: number;
      malwareSum: number;
      hcProxyCount: number;
    }>();

    for (const repo of repositories) {
      const format = repo.format.toLowerCase();
      const existing = groups.get(format) || {
        proxy: 0,
        hosted: 0,
        group: 0,
        online: 0,
        offline: 0,
        malwareSum: 0,
        hcProxyCount: 0,
      };

      // Count by type
      if (repo.type === 'proxy') {
        existing.proxy++;
        if (malwareAvailable && hcEnabledSet.has(repo.name)) {
          existing.hcProxyCount++;
        }
      } else if (repo.type === 'hosted') {
        existing.hosted++;
      } else if (repo.type === 'group') {
        existing.group++;
      }

      if (malwareAvailable) {
        const n = countByRepo[repo.name];
        existing.malwareSum += typeof n === 'number' ? n : Number(n ?? 0);
      }

      // Count online/offline (default to online if status not specified)
      const isOnline = repo.status?.online ?? repo.online ?? true;
      if (isOnline) {
        existing.online++;
      } else {
        existing.offline++;
      }

      groups.set(format, existing);
    }

    // Convert to array
    return Array.from(groups.entries())
      .map(([formatCode, counts]) => ({
        format: FORMAT_NAMES[formatCode] || formatCode,
        formatCode,
        proxyCount: counts.proxy,
        hostedCount: counts.hosted,
        groupCount: counts.group,
        totalCount: counts.proxy + counts.hosted + counts.group,
        onlineCount: counts.online,
        offlineCount: counts.offline,
        rhcSupported: isHealthCheckSupportedFormat(formatCode),
        ...(malwareAvailable
          ? {
              malwareCountsAvailable: true,
              malwareCount: counts.malwareSum,
              hcEnabledProxyCount: counts.hcProxyCount,
            }
          : { malwareCountsAvailable: false }),
      }))
      .sort((a, b) => b.totalCount - a.totalCount);
  }, [repositories, malwareCounts]);

  return { data, loading, error, refetch: fetchRepositories };
}

export default useRepositoriesByFormat;
