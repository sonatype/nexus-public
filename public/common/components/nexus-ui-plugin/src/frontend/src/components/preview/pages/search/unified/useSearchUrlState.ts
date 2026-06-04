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
import type { SearchFormat, FilterValues } from './unified.types';
import { getFiltersForFormat, FORMATS } from './searchFilters';

/**
 * URL-synced search state.
 */
export interface UrlSearchState {
  /** Selected format */
  format: SearchFormat;
  /** Search query */
  query: string;
  /** Filter values */
  filters: FilterValues;
}

/**
 * Return type for useSearchUrlState hook.
 */
export interface UseSearchUrlStateReturn {
  /** Current state */
  state: UrlSearchState;
  /** Set the search format (clears filters) */
  setFormat: (format: SearchFormat) => void;
  /** Set the search query */
  setQuery: (query: string) => void;
  /** Set a single filter value */
  setFilter: (id: string, value: string) => void;
  /** Set multiple filter values at once */
  setFilters: (filters: FilterValues) => void;
  /** Reset all state to defaults */
  reset: () => void;
  /** Build a shareable URL for current state */
  getShareableUrl: () => string;
}

/**
 * Valid search formats for validation.
 */
const VALID_FORMATS = new Set<string>(Object.keys(FORMATS));

/**
 * Check if a format string is a valid SearchFormat.
 */
function isValidFormat(format: string): format is SearchFormat {
  return VALID_FORMATS.has(format);
}

/**
 * Parse the current URL to extract search state.
 */
function parseUrlState(): UrlSearchState {
  // Get query string from hash (for hash-based routing) or from search
  let queryString = '';

  // Check if we're using hash-based routing (#preview/browse/search?format=maven&q=test)
  const hash = window.location.hash;
  if (hash.includes('?')) {
    queryString = hash.split('?')[1] || '';
  } else {
    // Fall back to regular query string
    queryString = window.location.search.slice(1);
  }

  const params = new URLSearchParams(queryString);

  // Parse format with validation
  const formatParam = params.get('format') || 'all';
  const format: SearchFormat = isValidFormat(formatParam) ? formatParam : 'all';

  // Parse query
  const query = params.get('q') || '';

  // Parse filters based on format-specific filter definitions
  const filters: FilterValues = {};
  const filterDefs = getFiltersForFormat(format);

  for (const filterDef of filterDefs) {
    const value = params.get(filterDef.apiParam);
    if (value) {
      filters[filterDef.id] = value;
    }
  }

  return { format, query, filters };
}

/**
 * Build URL query string from state.
 */
function buildQueryString(state: UrlSearchState): string {
  const params = new URLSearchParams();

  // Add format (skip if 'all' which is default)
  if (state.format !== 'all') {
    params.set('format', state.format);
  }

  // Add query
  if (state.query.trim()) {
    params.set('q', state.query.trim());
  }

  // Add filters using their API param names
  const filterDefs = getFiltersForFormat(state.format);
  for (const filterDef of filterDefs) {
    const value = state.filters[filterDef.id];
    if (value && value.trim()) {
      params.set(filterDef.apiParam, value.trim());
    }
  }

  return params.toString();
}

/**
 * Update the browser URL with new state.
 */
function updateBrowserUrl(state: UrlSearchState): void {
  const queryString = buildQueryString(state);
  const hash = window.location.hash;

  // Preserve the hash path but update query params
  let newUrl: string;
  if (hash) {
    // Hash-based routing: #preview/browse/search?params
    const hashPath = hash.split('?')[0];
    newUrl = queryString
      ? `${window.location.pathname}${hashPath}?${queryString}`
      : `${window.location.pathname}${hashPath}`;
  } else {
    // Regular routing
    newUrl = queryString
      ? `${window.location.pathname}?${queryString}`
      : window.location.pathname;
  }

  window.history.pushState({}, '', newUrl);
}

/**
 * React hook that syncs search state with browser URL.
 *
 * Features:
 * - Reads initial state from URL on mount
 * - Updates URL when state changes
 * - Supports hash-based routing (#preview/browse/search?format=maven&q=test)
 * - Validates format parameter
 * - Generates shareable URLs
 *
 * URL Format:
 * - `#preview/browse/search?format=maven&q=spring&maven.groupId=org.apache`
 *
 * @example
 * ```tsx
 * function SearchPage() {
 *   const { state, setFormat, setQuery, setFilter } = useSearchUrlState();
 *
 *   return (
 *     <div>
 *       <select value={state.format} onChange={e => setFormat(e.target.value)}>
 *         ...
 *       </select>
 *       <input value={state.query} onChange={e => setQuery(e.target.value)} />
 *     </div>
 *   );
 * }
 * ```
 */
export function useSearchUrlState(): UseSearchUrlStateReturn {
  const [state, setState] = useState<UrlSearchState>(parseUrlState);

  // Handle browser back/forward navigation
  useEffect(() => {
    const handlePopState = (): void => {
      setState(parseUrlState());
    };

    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  /**
   * Set the search format.
   * Clears filters when format changes since filters are format-specific.
   */
  const setFormat = useCallback((format: SearchFormat): void => {
    const newState: UrlSearchState = {
      format,
      query: state.query,
      filters: {}, // Clear filters on format change
    };
    setState(newState);
    updateBrowserUrl(newState);
  }, [state.query]);

  /**
   * Set the search query.
   */
  const setQuery = useCallback((query: string): void => {
    const newState: UrlSearchState = {
      ...state,
      query,
    };
    setState(newState);
    updateBrowserUrl(newState);
  }, [state]);

  /**
   * Set a single filter value.
   */
  const setFilter = useCallback((id: string, value: string): void => {
    const newState: UrlSearchState = {
      ...state,
      filters: {
        ...state.filters,
        [id]: value,
      },
    };
    setState(newState);
    updateBrowserUrl(newState);
  }, [state]);

  /**
   * Set multiple filter values at once.
   */
  const setFilters = useCallback((filters: FilterValues): void => {
    const newState: UrlSearchState = {
      ...state,
      filters: {
        ...state.filters,
        ...filters,
      },
    };
    setState(newState);
    updateBrowserUrl(newState);
  }, [state]);

  /**
   * Reset all state to defaults.
   */
  const reset = useCallback((): void => {
    const newState: UrlSearchState = {
      format: 'all',
      query: '',
      filters: {},
    };
    setState(newState);
    updateBrowserUrl(newState);
  }, []);

  /**
   * Build a shareable URL for the current state.
   */
  const getShareableUrl = useCallback((): string => {
    const queryString = buildQueryString(state);
    const origin = window.location.origin;
    const hash = window.location.hash.split('?')[0] || '#preview/browse/search';

    return queryString
      ? `${origin}${hash}?${queryString}`
      : `${origin}${hash}`;
  }, [state]);

  return {
    state,
    setFormat,
    setQuery,
    setFilter,
    setFilters,
    reset,
    getShareableUrl,
  };
}

export default useSearchUrlState;


