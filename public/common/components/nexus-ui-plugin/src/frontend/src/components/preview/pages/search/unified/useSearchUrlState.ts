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

import { useCallback } from 'react';
import type { SearchFormat, FilterValues, SortField, SortDirection } from './unified.types';
import { getFiltersForFormat, FORMATS } from './searchFilters';

/** Default sort field — omitted from the URL when active. */
export const DEFAULT_SORT_FIELD: SortField = 'lastUpdated';
/** Default sort direction — omitted from the URL when active. */
export const DEFAULT_SORT_DIRECTION: SortDirection = 'desc';

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
  /** Sort field (optional — defaults to lastUpdated when absent) */
  sortField?: SortField;
  /** Sort direction (optional — defaults to desc when absent) */
  sortDirection?: SortDirection;
}

/**
 * Return type for useSearchUrlState hook.
 *
 * The hook exposes a minimal, read/write surface over the browser URL:
 * the XState search machine is the single source of truth, and this hook
 * serializes that state to (and rehydrates it from) the URL.
 */
export interface UseSearchUrlStateReturn {
  /** Read the current search state from the URL. */
  readFromUrl: () => UrlSearchState;
  /**
   * Serialize the given search state into the browser URL. Pass replace=true
   * for rapid keystroke-driven writes to avoid flooding browser history.
   */
  syncToUrl: (state: UrlSearchState, replace?: boolean) => void;
  /** Build a shareable URL for the given state. */
  getShareableUrl: (state: UrlSearchState) => string;
}

/**
 * Valid search formats for validation.
 */
const VALID_FORMATS = new Set<string>(Object.keys(FORMATS));

/** Valid sort fields for validation. */
const VALID_SORT_FIELDS = new Set<string>(['name', 'version', 'lastUpdated', 'repository']);
/** Valid sort directions for validation. */
const VALID_SORT_DIRECTIONS = new Set<string>(['asc', 'desc']);

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

  // Parse sort with validation (fall back to defaults on invalid values)
  const sortParam = params.get('sort');
  const sortField: SortField | undefined =
    sortParam && VALID_SORT_FIELDS.has(sortParam) ? (sortParam as SortField) : undefined;

  const directionParam = params.get('direction');
  const sortDirection: SortDirection | undefined =
    directionParam && VALID_SORT_DIRECTIONS.has(directionParam)
      ? (directionParam as SortDirection)
      : undefined;

  return { format, query, filters, sortField, sortDirection };
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
    if (value?.trim()) {
      params.set(filterDef.apiParam, value.trim());
    }
  }

  // Add sort (skip defaults: lastUpdated / desc). Field and direction are
  // written independently so a non-default direction on the default field
  // (e.g. lastUpdated / asc) still survives a URL round-trip.
  if (state.sortField && state.sortField !== DEFAULT_SORT_FIELD) {
    params.set('sort', state.sortField);
  }
  if (state.sortDirection && state.sortDirection !== DEFAULT_SORT_DIRECTION) {
    params.set('direction', state.sortDirection);
  }

  return params.toString();
}

/**
 * Update the browser URL with new state.
 */
function updateBrowserUrl(state: UrlSearchState, replace = false): void {
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

  // pushState for discrete commits (creates a back/forward entry);
  // replaceState for rapid keystroke-driven writes (avoids history flooding).
  if (replace) {
    window.history.replaceState({}, '', newUrl);
  } else {
    window.history.pushState({}, '', newUrl);
  }
}

/**
 * Build a shareable URL from a search state.
 */
function buildShareableUrl(state: UrlSearchState): string {
  const queryString = buildQueryString(state);
  const origin = window.location.origin;
  const hash = window.location.hash.split('?')[0] || '#preview/browse/search';

  return queryString ? `${origin}${hash}?${queryString}` : `${origin}${hash}`;
}

/**
 * React hook exposing read/write access to the search state carried in the
 * browser URL.
 *
 * The XState search machine (see `useUnifiedSearch`) is the single source of
 * truth. This hook is a thin, stateless serializer: the page reads the initial
 * state from the URL on mount (and on back/forward) via `readFromUrl`, and
 * pushes machine changes back to the URL via `syncToUrl`. No local React state
 * is held here, avoiding a second competing source of truth.
 *
 * URL format (readable query params):
 * - `#preview/browse/search?q=spring&format=maven&maven.groupId=org.apache&sort=name`
 *
 * @example
 * ```tsx
 * const { readFromUrl, syncToUrl } = useSearchUrlState();
 * // on mount: rehydrate the machine from readFromUrl()
 * // on state change: syncToUrl(machineState)
 * ```
 */
export function useSearchUrlState(): UseSearchUrlStateReturn {
  /** Read the current search state from the URL. */
  const readFromUrl = useCallback((): UrlSearchState => parseUrlState(), []);

  /**
   * Serialize the given search state into the browser URL.
   *
   * @param state the state to serialize
   * @param replace when true, use replaceState (no history entry) for rapid
   *   keystroke-driven writes; defaults to pushState for discrete commits.
   */
  const syncToUrl = useCallback((state: UrlSearchState, replace = false): void => {
    updateBrowserUrl(state, replace);
  }, []);

  /** Build a shareable URL for the given state. */
  const getShareableUrl = useCallback(
    (state: UrlSearchState): string => buildShareableUrl(state),
    [],
  );

  return {
    readFromUrl,
    syncToUrl,
    getShareableUrl,
  };
}

export default useSearchUrlState;
