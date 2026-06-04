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

import { useState, useCallback, useRef } from 'react';
import Axios from 'axios';
import type { DockerSearchState, DockerResult, DockerSearchResponse } from './docker.types';
import { mockSearchApi } from './mockData';

// Feature flag to switch between real API and mock data
const USE_REAL_API = true;

/**
 * Raw search item from the API.
 */
interface RawSearchItem {
  id: string;
  repository: string;
  format: string;
  group: string | null;
  name: string;
  version: string;
  assets: Array<{
    id: string;
    path: string;
    downloadUrl: string;
    contentType?: string;
    fileSize?: number;
  }>;
}

/**
 * Raw search response from the API.
 */
interface RawSearchResponse {
  items: RawSearchItem[];
  continuationToken?: string;
}

/**
 * Search Docker images using the real API.
 */
async function searchDockerApi(
  params: DockerSearchParams,
  continuationToken?: string
): Promise<DockerSearchResponse> {
  const queryParams = new URLSearchParams();
  queryParams.set('format', 'docker');

  // Build query string from params
  if (params.query) {
    queryParams.set('q', params.query);
  }
  if (params.imageName) {
    queryParams.set('docker.imageName', params.imageName);
  }
  if (params.tag) {
    queryParams.set('docker.imageTag', params.tag);
  }
  if (params.digest) {
    queryParams.set('docker.contentDigest', params.digest);
  }
  if (params.repository) {
    queryParams.set('repository', params.repository);
  }
  if (continuationToken) {
    queryParams.set('continuationToken', continuationToken);
  }

  const url = `/service/rest/v1/search?${queryParams.toString()}`;
  const response = await Axios.get<RawSearchResponse>(url);

  // Aggregate results by image name
  const imageMap = new Map<string, DockerResult>();

  for (const item of response.data.items) {
    const imageName = item.name;
    const imageKey = `${item.repository}:${imageName}`;

    const existing = imageMap.get(imageKey);
    if (existing) {
      // Update existing entry - version in Docker is typically the tag
      const updatedResult: DockerResult = {
        ...existing,
        tagsCount: existing.tagsCount + 1,
        latestTag: item.version > existing.latestTag ? item.version : existing.latestTag,
      };
      imageMap.set(imageKey, updatedResult);
    } else {
      // Create new entry
      imageMap.set(imageKey, {
        id: `docker:${item.repository}:${imageName}`,
        imageName,
        displayName: imageName,
        latestTag: item.version || 'latest',
        tagsCount: 1,
        repository: item.repository,
        lastUpdated: new Date().toISOString(),
      });
    }
  }

  const items = Array.from(imageMap.values());

  return {
    items,
    totalCount: items.length,
    continuationToken: response.data.continuationToken,
  };
}

/**
 * Search parameters that can be set via URL or programmatically.
 */
export interface DockerSearchParams {
  query?: string;
  imageName?: string;
  tag?: string;
  digest?: string;
  repository?: string;
  sort?: 'relevance' | 'lastUpdated' | 'name';
  sortDirection?: 'asc' | 'desc';
  continuationToken?: string;
}

/**
 * Hook return type.
 */
export interface UseDockerSearchReturn {
  /** Current search state */
  state: DockerSearchState;
  /** Execute a search with new parameters */
  search: (params: DockerSearchParams) => Promise<void>;
  /** Load more results (pagination) */
  loadMore: () => Promise<void>;
  /** Clear search results */
  clear: () => void;
  /** Update sort settings */
  setSort: (sort: 'relevance' | 'lastUpdated' | 'name', direction: 'asc' | 'desc') => void;
  /** Whether more results can be loaded */
  hasMore: boolean;
}

const initialState: DockerSearchState = {
  query: '',
  sort: 'relevance',
  sortDirection: 'desc',
  loading: false,
  error: undefined,
  results: [],
  totalCount: 0,
  continuationToken: undefined,
};

/**
 * React hook for Docker search state management.
 *
 * Features:
 * - URL-driven state (reads/writes query params)
 * - Pagination via continuation token
 * - Loading and error states
 * - Sort management
 *
 * @param initialParams - Initial search parameters (usually from URL)
 */
export function useDockerSearch(initialParams?: DockerSearchParams): UseDockerSearchReturn {
  const [state, setState] = useState<DockerSearchState>(() => ({
    ...initialState,
    query: initialParams?.query ?? '',
    sort: initialParams?.sort ?? 'relevance',
    sortDirection: initialParams?.sortDirection ?? 'desc',
  }));

  // Track current search params for the continuation token
  const currentParamsRef = useRef<DockerSearchParams>({});

  /**
   * Execute a search with new parameters.
   */
  const search = useCallback(async (params: DockerSearchParams): Promise<void> => {
    // Save current params for pagination
    currentParamsRef.current = params;

    setState((prev) => ({
      ...prev,
      loading: true,
      error: undefined,
      query: params.query ?? '',
      // Reset results for new search
      results: [],
      totalCount: 0,
      continuationToken: undefined,
    }));

    try {
      // Use real API or mock data based on feature flag
      const response = USE_REAL_API
        ? await searchDockerApi(params)
        : await mockSearchApi({
            query: params.query,
            imageName: params.imageName,
            tag: params.tag,
            digest: params.digest,
            repository: params.repository,
          });

      setState((prev) => ({
        ...prev,
        loading: false,
        results: response.items,
        totalCount: response.totalCount,
        continuationToken: response.continuationToken,
      }));
    } catch (error) {
      setState((prev) => ({
        ...prev,
        loading: false,
        error: error instanceof Error ? error.message : 'Search failed',
      }));
    }
  }, []);

  /**
   * Load more results using continuation token.
   */
  const loadMore = useCallback(async (): Promise<void> => {
    if (!state.continuationToken || state.loading) {
      return;
    }

    setState((prev) => ({
      ...prev,
      loading: true,
    }));

    try {
      // Use real API or mock data based on feature flag
      const response = USE_REAL_API
        ? await searchDockerApi(currentParamsRef.current, state.continuationToken)
        : await mockSearchApi({
            ...currentParamsRef.current,
            continuationToken: state.continuationToken,
          });

      setState((prev) => ({
        ...prev,
        loading: false,
        // Append new results to existing
        results: [...prev.results, ...response.items],
        continuationToken: response.continuationToken,
      }));
    } catch (error) {
      setState((prev) => ({
        ...prev,
        loading: false,
        error: error instanceof Error ? error.message : 'Failed to load more results',
      }));
    }
  }, [state.continuationToken, state.loading]);

  /**
   * Clear search results.
   */
  const clear = useCallback((): void => {
    currentParamsRef.current = {};
    setState(initialState);
  }, []);

  /**
   * Update sort settings.
   */
  const setSort = useCallback(
    (sort: 'relevance' | 'lastUpdated' | 'name', direction: 'asc' | 'desc'): void => {
      setState((prev) => ({
        ...prev,
        sort,
        sortDirection: direction,
      }));

      // Re-search with new sort if we have results
      if (currentParamsRef.current.query || currentParamsRef.current.imageName || currentParamsRef.current.tag) {
        search({
          ...currentParamsRef.current,
          sort,
          sortDirection: direction,
        });
      }
    },
    [search]
  );

  const hasMore = Boolean(state.continuationToken);

  return {
    state,
    search,
    loadMore,
    clear,
    setSort,
    hasMore,
  };
}

export default useDockerSearch;

