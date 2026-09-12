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
import { useRouter } from '@uirouter/react';
import type { SearchResult } from './unified.types';

/**
 * Storage key holding the search URL to return to from a component detail page.
 *
 * The value is the search page's own `window.location.hash` — one plain string,
 * not machine state, and not JSON.
 *
 * This replaces `COMPONENT_DETAIL_RETURN_SEARCH_KEY`, which stored
 * query/format/filters. That channel existed only because `nameOrVersion` could
 * not be serialized into the URL; NEXUS-54333 made the URL carry it (pinned by a
 * round-trip test in `useSearchUrlState.test.ts`), after which the duplicate
 * channel was pure risk: the search page read it ahead of the URL on *every*
 * mount, and browsers copy sessionStorage into a tab opened from a link, so a
 * stale payload beat a fresh deep link or header search (NEXUS-54503 Defect 1).
 */
export const SEARCH_RETURN_URL_KEY = 'nexus-search-return-url';

/** Hash of the unified-search page, without its query string. */
const SEARCH_HASH_PREFIX = '#preview/browse/search';

/**
 * True only for the unified-search page's own hash.
 *
 * A plain `startsWith` is not enough: `#preview/browse/search` also prefixes
 * every component-detail hash (`#preview/browse/search/maven/{keyword}/ga/{gaId}`),
 * so `navigateToDetail` would store a detail URL on a detail -> detail hop and
 * `consumeSearchReturnUrl` would accept it — the breadcrumb would then assign
 * the hash it is already on and silently do nothing. The prefix therefore only
 * counts when the whole hash *is* the search path, optionally followed by its
 * query string.
 */
function isUnifiedSearchHash(hash: string): boolean {
  return hash === SEARCH_HASH_PREFIX || hash.startsWith(`${SEARCH_HASH_PREFIX}?`);
}

/**
 * Read and clear the stored search return URL.
 *
 * Returns undefined unless the stored value is a unified-search hash, so a value
 * tampered with in sessionStorage cannot send the breadcrumb somewhere else. The
 * key is cleared either way — it is single-use whether or not it was valid.
 *
 * Storage access is guarded: `sessionStorage` throws on access in Safari private
 * mode and storage-blocked embeds, and an exception here would abort the
 * breadcrumb click before it navigates. An unreadable store degrades to "no
 * stored URL", which the caller already handles by falling back to bare search.
 */
export function consumeSearchReturnUrl(): string | undefined {
  let stored: string | null = null;
  try {
    stored = sessionStorage.getItem(SEARCH_RETURN_URL_KEY);
    sessionStorage.removeItem(SEARCH_RETURN_URL_KEY);
  } catch {
    // Cleared rather than returned: if the remove failed, the value is no longer
    // single-use, and re-serving it later is worse than losing it now.
    stored = null;
  }
  if (!stored || !isUnifiedSearchHash(stored)) {
    return undefined;
  }
  return stored;
}

/**
 * Build the component-detail `gaId` for a search result: `format:group:name`,
 * or `format:name` when the result has no group.
 *
 * Shared by {@link buildComponentDetailUrl} and `navigateToDetail` so the URL
 * string and the router param can never drift apart.
 */
function buildGaId(result: Pick<SearchResult, 'format' | 'group' | 'name'>): string {
  const parts = [result.format];
  if (result.group) {
    parts.push(result.group);
  }
  parts.push(result.name);
  return parts.join(':');
}

/**
 * Route configuration for detail page navigation.
 */
export interface DetailRoute {
  /** UI-Router state name or hash URL */
  url: string;
  /** Whether this navigates to Preview UI Browse */
  isPreviewBrowse: boolean;
}

/**
 * Return type for the useSearchNavigation hook.
 */
export interface UseSearchNavigationReturn {
  /** Navigate to the detail page for a search result */
  navigateToDetail: (result: SearchResult) => void;
  /** Get the route configuration without navigating */
  getDetailRoute: (result: SearchResult) => DetailRoute;
}

/**
 * Build a URL string for a search result's component detail, returned by
 * {@link useSearchNavigation}'s `getDetailRoute`. This only builds a string; it
 * does not navigate — the primary navigation path is `navigateToDetail`, which
 * uses `router.stateService.go('preview.browse.search.component', { gaId, version })`.
 *
 * The gaId format is `format:group:name` (or `format:name` when there is no group).
 *
 * The returned string has the form:
 *   preview/browse/search/maven/{encodeURIComponent(name)}/ga/{encodeURIComponent(gaId)}[?version=...]
 *
 * Examples:
 * - preview/browse/search/maven/lodash/ga/npm%3Alodash
 * - preview/browse/search/maven/commons-lang3/ga/maven2%3Aorg.apache.commons%3Acommons-lang3
 * - preview/browse/search/maven/curl/ga/apt%3Aamd64%3Acurl
 */
function buildComponentDetailUrl(result: SearchResult): string {
  const gaId = buildGaId(result);

  // The keyword segment is the component name (used by the parent maven search route)
  const keyword = encodeURIComponent(result.name);

  // Full URL with keyword segment: /maven/{keyword}/ga/{gaId}
  let url = `preview/browse/search/maven/${keyword}/ga/${encodeURIComponent(gaId)}`;
  if (result.version) {
    url += `?version=${encodeURIComponent(result.version)}`;
  }
  return url;
}

/**
 * Hook that provides navigation from search results to component detail pages.
 *
 * Navigates to Preview Browse to view the component in the repository tree.
 * This keeps users within the Preview UI experience.
 */
export function useSearchNavigation(): UseSearchNavigationReturn {
  const router = useRouter();

  /**
   * Get the detail route configuration for a search result.
   */
  const getDetailRoute = useCallback((result: SearchResult): DetailRoute => {
    return {
      url: buildComponentDetailUrl(result),
      isPreviewBrowse: false,
    };
  }, []);

  /**
   * Navigate to the detail page for a search result.
   * Uses UIRouter stateService.go() for proper state resolution.
   *
   * Captures the current search URL under {@link SEARCH_RETURN_URL_KEY} so the
   * detail page's breadcrumb can return to it. The caller must have flushed any
   * pending debounced URL write first, or the captured URL is one edit stale —
   * `UnifiedSearchPage.handleSelect` does exactly that.
   */
  const navigateToDetail = useCallback(
    (result: SearchResult): void => {
      const hash = window.location.hash;
      if (isUnifiedSearchHash(hash)) {
        try {
          sessionStorage.setItem(SEARCH_RETURN_URL_KEY, hash);
        } catch {
          // Storage unavailable or full. The breadcrumb falls back to bare
          // search; losing the return URL must not block the click-through.
        }
      }

      // Pass gaId plain — the route declares it as `type: 'path'`, so UI-Router encodes it on
      // write and decodes it on read. Pre-encoding here made the stored param ('npm%3Aa%3Ab')
      // differ from the value parsed back out of the URL ('npm:a:b'); since gaId is not
      // `dynamic`, the router settled that mismatch with a state re-entry, remounting the detail
      // page and firing every request on it twice (NEXUS-54201).
      const gaId = buildGaId(result);

      const params: Record<string, string | null> = {
        keyword: result.name,
        gaId,
      };
      if (result.version) {
        params.version = result.version;
      }

      // Navigate to component detail (sibling of maven/npm search, replaces search view)
      router.stateService.go('preview.browse.search.component', params);
    },
    [router],
  );

  return { navigateToDetail, getDetailRoute };
}

export default useSearchNavigation;
