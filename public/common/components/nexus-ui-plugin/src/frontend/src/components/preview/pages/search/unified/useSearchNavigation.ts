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
import type { SearchResult, SearchFormat, FilterValues } from './unified.types';

/** Storage key for preserving search state when navigating to component detail */
export const COMPONENT_DETAIL_RETURN_SEARCH_KEY = 'nexus-component-detail-return-search';

/**
 * Search state to preserve when navigating to detail and restore when returning.
 */
export interface SearchStateToPreserve {
  query: string;
  format: SearchFormat;
  filters: FilterValues;
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
  navigateToDetail: (result: SearchResult, searchState?: SearchStateToPreserve) => void;
  /** Get the route configuration without navigating */
  getDetailRoute: (result: SearchResult) => DetailRoute;
}

/**
 * Build the GA Detail URL for a search result.
 *
 * Navigates to the component detail page (GADetailPage) which shows
 * Overview, Versions, Repositories, Files, and Security tabs.
 *
 * The gaId format is: format:group:name (or format:name if no group).
 *
 * UIRouter route structure:
 *   preview.browse.search.maven       → /maven/:keyword
 *   preview.browse.search.maven.detail → /ga/:gaId
 *
 * So the full URL is: preview/browse/search/maven/{keyword}/ga/{gaId}
 *
 * Examples:
 * - preview/browse/search/maven/lodash/ga/npm%3Alodash
 * - preview/browse/search/maven/commons-lang3/ga/maven2%3Aorg.apache.commons%3Acommons-lang3
 * - preview/browse/search/maven/curl/ga/apt%3Aamd64%3Acurl
 */
function buildComponentDetailUrl(result: SearchResult): string {
  // Build gaId from format, group, and name
  const parts = [result.format];
  if (result.group) {
    parts.push(result.group);
  }
  parts.push(result.name);
  const gaId = parts.join(':');

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
   * Optionally stores search state in sessionStorage for restoration when returning.
   */
  const navigateToDetail = useCallback(
    (result: SearchResult, searchState?: SearchStateToPreserve): void => {
      if (searchState) {
        sessionStorage.setItem(
          COMPONENT_DETAIL_RETURN_SEARCH_KEY,
          JSON.stringify(searchState),
        );
      }

      // Build gaId from format, group, name
      const parts = [result.format];
      if (result.group) parts.push(result.group);
      parts.push(result.name);
      const gaId = encodeURIComponent(parts.join(':'));

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
