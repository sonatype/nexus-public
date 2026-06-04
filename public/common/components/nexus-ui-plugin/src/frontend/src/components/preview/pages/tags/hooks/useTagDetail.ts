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
import { fetchTagDetail } from '../tags.api';
import type { TagDetail } from '../tags.types';

/**
 * State for the useTagDetail hook.
 */
export interface TagDetailState {
  /** Tag detail data */
  tagDetail: TagDetail | null;
  /** Whether data is currently loading */
  loading: boolean;
  /** Error message if fetch failed */
  error: string | null;
}

/**
 * Actions returned by the useTagDetail hook.
 */
export interface TagDetailActions {
  /** Retry fetching data after an error */
  retry: () => void;
}

/**
 * Return type for the useTagDetail hook.
 */
export interface UseTagDetailResult {
  state: TagDetailState;
  actions: TagDetailActions;
}

/**
 * Hook to fetch and manage tag detail data.
 *
 * @param tagName - Name of the tag to fetch
 * @returns State and actions for tag detail
 *
 * @example
 * ```tsx
 * const { state, actions } = useTagDetail('release-1.0');
 *
 * if (state.loading) return <Spinner />;
 * if (state.error) return <Error onRetry={actions.retry} />;
 *
 * return <div>{state.tagDetail?.name}</div>;
 * ```
 */
export function useTagDetail(tagName: string): UseTagDetailResult {
  const [tagDetail, setTagDetail] = useState<TagDetail | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState<number>(0);

  /**
   * Fetch tag detail from the API.
   */
  const loadTagDetail = useCallback(async () => {
    if (!tagName) {
      setError('Tag name is required');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const data = await fetchTagDetail(tagName);
      setTagDetail(data);
      setError(null);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load tag details';
      setError(message);
      setTagDetail(null);
    } finally {
      setLoading(false);
    }
  }, [tagName]);

  /**
   * Retry fetching data after an error.
   */
  const retry = useCallback(() => {
    setRetryCount((prev) => prev + 1);
  }, []);

  // Load tag detail on mount and when tagName or retryCount changes
  useEffect(() => {
    loadTagDetail();
  }, [loadTagDetail, retryCount]);

  return {
    state: {
      tagDetail,
      loading,
      error,
    },
    actions: {
      retry,
    },
  };
}

