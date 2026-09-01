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
import { SORT_OPTIONS } from './sortOptions';

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

/**
 * Valid sort fields for validation, derived from the options the sort controls
 * actually offer. Deriving rather than duplicating the list means a URL can
 * never carry a sort field the controls cannot display — which would leave the
 * dropdown showing one sort while the request used another.
 */
const VALID_SORT_FIELDS = new Set<string>(SORT_OPTIONS.map((option) => option.field));
/** Valid sort directions for validation. */
const VALID_SORT_DIRECTIONS = new Set<string>(['asc', 'desc']);

/**
 * Maximum accepted length for any single search value carried in the URL.
 *
 * Generous relative to real search terms and far below any URL length limit.
 * A longer value is dropped rather than truncated: a truncated prefix produces
 * a search the user never asked for, and is no cheaper to run.
 */
export const MAX_PARAM_VALUE_LENGTH = 256;

/**
 * C0 and C1 control characters. A URL-carried search value containing one
 * cannot be anything a user typed, and forwarding it would put a control byte
 * into an API query string, so such a value is rejected outright.
 *
 * biome-ignore lint/suspicious/noControlCharactersInRegex: This regex deliberately matches control characters to reject malicious/malformed input.
 */
const CONTROL_CHARS = /[\x00-\x1F\x7F-\x9F]/;

/**
 * Read one search param, returning it only if it passes the allowlist rules.
 *
 * An over-long or control-character-bearing value is dropped so the remaining
 * criteria still restore — a slightly malformed shared link degrades to a
 * narrower search rather than to nothing. Returns undefined when the param is
 * absent or rejected; callers cannot tell the two apart, and do not need to.
 */
function readSafeParam(params: URLSearchParams, name: string): string | undefined {
  const raw = params.get(name);
  if (raw === null || !isSafeParamValue(raw)) {
    return undefined;
  }
  return raw;
}

/**
 * The allowlist rules for a single URL-carried search value, applied on both
 * read and write.
 *
 * Enforcing this on read only would let `buildQueryString` write a value that
 * `parseUrlState` then drops: the user's own term would reach the URL and
 * silently disappear on refresh, back-nav or breadcrumb return, widening the
 * search without notice. Writes and reads therefore share one predicate.
 */
function isSafeParamValue(value: string): boolean {
  return value.length <= MAX_PARAM_VALUE_LENGTH && !CONTROL_CHARS.test(value);
}

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
  const query = readSafeParam(params, 'q') ?? '';

  // Parse filters based on format-specific filter definitions
  const filters: FilterValues = {};
  const filterDefs = getFiltersForFormat(format);

  for (const filterDef of filterDefs) {
    const value = readSafeParam(params, filterDef.apiParam);
    if (value) {
      filters[filterDef.id] = value;
    }
  }

  // Universal virtual filters that don't appear in any format's filter defs but
  // are used by the machine and translated to API params in buildQueryParams.
  // Keeping them in the URL lets refresh / back-nav / breadcrumb-return restore
  // the exact state the user had.
  const nameOrVersion = readSafeParam(params, 'nameOrVersion');
  if (nameOrVersion) {
    filters.nameOrVersion = nameOrVersion;
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
 *
 * Exported so callers outside the hook (e.g. the component-detail breadcrumb
 * handler) can produce a URL that `parseUrlState` will round-trip cleanly.
 *
 * Free-text values are held to {@link isSafeParamValue}, the same rule the read
 * path applies, so every value written here survives being read back. A value
 * that fails is omitted rather than truncated, matching `readSafeParam`.
 *
 * @returns an `application/x-www-form-urlencoded` query string with no leading
 *   `?`, containing only non-default, round-trippable criteria — empty when the
 *   state carries none.
 */
export function buildQueryString(state: UrlSearchState): string {
  const params = new URLSearchParams();

  // Add format (skip if 'all' which is default)
  if (state.format !== 'all') {
    params.set('format', state.format);
  }

  // Add query
  const query = state.query.trim();
  if (query && isSafeParamValue(query)) {
    params.set('q', query);
  }

  // Add filters using their API param names
  const filterDefs = getFiltersForFormat(state.format);
  for (const filterDef of filterDefs) {
    const value = state.filters[filterDef.id]?.trim();
    if (value && isSafeParamValue(value)) {
      params.set(filterDef.apiParam, value);
    }
  }

  // Universal virtual filter (see parseUrlState). Round-tripping this in the
  // URL prevents refresh / back-nav / component-detail-return from silently
  // losing the results-page filter input.
  const nameOrVersion = state.filters.nameOrVersion?.trim();
  if (nameOrVersion && isSafeParamValue(nameOrVersion)) {
    params.set('nameOrVersion', nameOrVersion);
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
  // Carry the pre-hash query string through. `?debug` lives in location.search,
  // and composing only pathname + hash silently drops it, kicking the developer
  // out of filesystem-resource mode mid-session (NEXUS-54503 Defect 2).
  const preHashSearch = window.location.search;

  // Preserve the hash path but update query params
  let newUrl: string;
  if (hash) {
    // Hash-based routing: #preview/browse/search?params
    const hashPath = hash.split('?')[0];
    newUrl = queryString
      ? `${window.location.pathname}${preHashSearch}${hashPath}?${queryString}`
      : `${window.location.pathname}${preHashSearch}${hashPath}`;
  } else {
    // Regular routing: the search state *is* the pre-hash query string here,
    // so it replaces rather than appends to location.search.
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
 *
 * Deliberately omits `window.location.search`, unlike {@link updateBrowserUrl}:
 * a link handed to someone else should carry the search criteria and nothing
 * else, least of all a local `?debug` flag.
 *
 * `pathname` is kept, however: Nexus is routinely served under a context path
 * (`/nexus/`), and composing only origin + hash produced a link that 404s for
 * the recipient on every such install.
 */
function buildShareableUrl(state: UrlSearchState): string {
  const queryString = buildQueryString(state);
  const base = `${window.location.origin}${window.location.pathname}`;
  const hash = window.location.hash.split('?')[0] || '#preview/browse/search';

  return queryString ? `${base}${hash}?${queryString}` : `${base}${hash}`;
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
