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

import { useState, useEffect, useCallback, useMemo } from 'react';
import Axios from 'axios';
import { isMockMode } from '@/config/previewFeatureFlags';
import { getMockRepositoriesForSearch } from '@/components/super/browse/mockData';

/**
 * Repository data returned from the API.
 * The API returns format (e.g., 'maven2', 'npm', 'docker') for each repository.
 */
export interface Repository {
  name: string;
  format: string;
  type: string;
  url?: string;
}

/**
 * Return type for useRepositories hook.
 */
export interface UseRepositoriesResult {
  /** List of repository names (filtered by format if specified) */
  repositories: string[];
  /** Set of unique formats available to the user (based on accessible repositories) */
  availableFormats: Set<string>;
  /** Count of repositories per format (apiFormat -> count), for filter badges */
  formatCounts: Record<string, number>;
  /** Loading state */
  loading: boolean;
  /** Error message if fetch failed */
  error?: string;
  /** Function to manually refetch repositories */
  refetch: () => Promise<void>;
}

// Cache for all repositories to avoid refetching
let repositoriesCache: Repository[] | null = null;
let cachePromise: Promise<Repository[]> | null = null;

/**
 * Fetch all repositories (with caching).
 * API doesn't support format filtering, so we fetch all and filter client-side.
 */
async function fetchAllRepositories(): Promise<Repository[]> {
  // Return cached data if available
  if (repositoriesCache) {
    return repositoriesCache;
  }

  // If a fetch is already in progress, wait for it
  if (cachePromise) {
    return cachePromise;
  }

  // Start a new fetch
  cachePromise = Axios.get<Repository[]>('/service/rest/v1/repositories')
    .then((response) => {
      repositoriesCache = response.data;
      cachePromise = null;
      return response.data;
    })
    .catch((error) => {
      cachePromise = null;
      throw error;
    });

  return cachePromise;
}

/**
 * Hook to fetch repository names for search filter dropdowns.
 * 
 * @param format - Optional format to filter repositories (e.g., 'maven2', 'npm')
 *                 Note: Filtering is done client-side since API doesn't support it.
 * @returns Object containing repositories, loading state, error, and refetch function
 * 
 * @example
 * // Fetch all repositories
 * const { repositories, loading } = useRepositories();
 * 
 * @example
 * // Fetch only Maven repositories (client-side filtering)
 * const { repositories } = useRepositories('maven2');
 */
export function useRepositories(format?: string): UseRepositoriesResult {
  const [allRepositories, setAllRepositories] = useState<Repository[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | undefined>();

  const fetchRepos = useCallback(async () => {
    setLoading(true);
    setError(undefined);

    try {
      if (isMockMode()) {
        const data = getMockRepositoriesForSearch();
        setAllRepositories(data);
      } else {
        const data = await fetchAllRepositories();
        setAllRepositories(data);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to fetch repositories');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRepos();
  }, [fetchRepos]);

  // Filter repositories by format client-side
  // API returns format like 'maven2', 'npm', 'docker', etc.
  const repositories = useMemo(() => {
    if (!format) {
      // No format specified - return all repository names
      return allRepositories.map((r) => r.name);
    }

    // Filter by format (case-insensitive comparison)
    const filtered = allRepositories.filter(
      (r) => r.format.toLowerCase() === format.toLowerCase()
    );
    
    return filtered.map((r) => r.name);
  }, [allRepositories, format]);

  // Compute unique formats available to the user
  // This is used to filter the format dropdown to only show formats
  // the user has access to (based on accessible repositories)
  const availableFormats = useMemo(() => {
    const formats = new Set<string>();
    allRepositories.forEach((r) => {
      formats.add(r.format.toLowerCase());
    });
    return formats;
  }, [allRepositories]);

  // Count repositories per format (for filter badges)
  const formatCounts = useMemo(() => {
    const counts: Record<string, number> = {};
    allRepositories.forEach((r) => {
      const fmt = r.format.toLowerCase();
      counts[fmt] = (counts[fmt] || 0) + 1;
    });
    return counts;
  }, [allRepositories]);

  return { repositories, availableFormats, formatCounts, loading, error, refetch: fetchRepos };
}

/**
 * Clear the repositories cache.
 * Useful when repositories are added/removed.
 */
export function clearRepositoriesCache(): void {
  repositoriesCache = null;
  cachePromise = null;
}

export default useRepositories;
