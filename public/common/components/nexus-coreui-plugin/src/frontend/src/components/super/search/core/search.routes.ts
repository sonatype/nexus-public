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

/**
 * GA Search Routes - PREVIEW UI ONLY
 * 
 * AUTHORITATIVE: These are the route definitions for GA search in Preview UI.
 * 
 * IMPORTANT: GA search lives ONLY in Preview UI.
 * The existing ExtJS search at #browse/search/* remains UNCHANGED.
 * 
 * Preview UI Route Hierarchy (path-based /preview/...):
 * - preview.browse.search.maven       → GA-aggregated Maven search
 * - preview.browse.search.component    → Component detail view with tabs
 * 
 * Default UI Routes (UNCHANGED - ExtJS):
 * - browse.search.maven               → Legacy ExtJS Maven search
 * - browse.search.npm                 → Legacy ExtJS npm search
 * - etc.
 * 
 * URL Examples (path-based):
 * - /preview/browse/search/maven                    → GA search results
 * - /preview/browse/search/maven?q=commons-lang3   → GA search with query
 * - /preview/browse/search/maven/ga/:gaId          → GA detail view
 * - /preview/browse/search/maven/ga/:gaId/versions → Versions tab
 */

import type { GADetailTab } from './search.types';

// =============================================================================
// ROUTE NAMES (PREVIEW UI)
// =============================================================================

/**
 * Route name constants for GA search in Preview UI.
 * Use these in router.stateService.go() calls.
 * 
 * NOTE: These are PREVIEW routes. Default routes remain with ExtJS.
 */
export const GA_SEARCH_ROUTE_NAMES = {
  /**
   * Preview browse search parent (abstract).
   */
  ROOT: 'preview.browse.search',

  /**
   * Preview Maven search - GA aggregated results.
   * Replaces the placeholder at preview.browse.search.maven
   */
  MAVEN: 'preview.browse.search.maven',

  /**
   * Component detail view (sibling of maven, replaces search view).
   */
  MAVEN_DETAIL: 'preview.browse.search.component',

  /**
   * Component detail tabs.
   */
  MAVEN_DETAIL_OVERVIEW: 'preview.browse.search.component.overview',
  MAVEN_DETAIL_VERSIONS: 'preview.browse.search.component.versions',
  MAVEN_DETAIL_REPOS: 'preview.browse.search.component.repos',
  MAVEN_DETAIL_FILES: 'preview.browse.search.component.files',
  MAVEN_DETAIL_SECURITY: 'preview.browse.search.component.security',
} as const;

/**
 * Default UI route names (ExtJS - DO NOT MODIFY).
 * These exist for reference only. GA search does NOT touch these.
 */
export const DEFAULT_SEARCH_ROUTE_NAMES = {
  MAVEN: 'browse.search.maven',
  NPM: 'browse.search.npm',
  DOCKER: 'browse.search.docker',
  // ... etc - all remain ExtJS
} as const;

// =============================================================================
// URL PATTERNS (PREVIEW UI)
// =============================================================================

/**
 * URL patterns for GA search in Preview UI.
 *
 * Full URL examples (path-based):
 * - /preview/browse/search/maven              → Search results
 * - /preview/browse/search/maven?q=guava      → Search with query
 * - /preview/browse/search/maven/ga/maven:org.apache.commons:commons-lang3 → Detail
 */
export const GA_SEARCH_URLS = {
  /**
   * Maven search results (replaces placeholder).
   * This is the url segment for preview.browse.search.maven
   * Query params: q, groupId, artifactId, repository, sort, direction
   */
  MAVEN: '/maven:keyword',

  /**
   * GA detail view (child of MAVEN).
   * Path param: gaId (URL-encoded)
   */
  MAVEN_DETAIL: '/ga/:gaId',

  /**
   * Detail tabs (child routes of MAVEN_DETAIL).
   */
  DETAIL_OVERVIEW: '/overview',
  DETAIL_VERSIONS: '/versions',
  DETAIL_REPOS: '/repos',
  DETAIL_FILES: '/files',
  DETAIL_SECURITY: '/security',
} as const;

/**
 * Base URL for Preview UI (path-based routing).
 */
export const PREVIEW_BASE_URL = '/preview/browse/search';

// =============================================================================
// QUERY PARAMETERS
// =============================================================================

/**
 * Query parameter names for GA search URLs.
 */
export const GA_SEARCH_PARAMS = {
  /**
   * Search query.
   */
  QUERY: 'q',

  /**
   * Format filter.
   */
  FORMAT: 'format',

  /**
   * Maven group ID.
   */
  GROUP_ID: 'groupId',

  /**
   * Maven artifact ID.
   */
  ARTIFACT_ID: 'artifactId',

  /**
   * Repository filter.
   */
  REPOSITORY: 'repository',

  /**
   * Sort field.
   */
  SORT: 'sort',

  /**
   * Sort direction.
   */
  DIRECTION: 'direction',

  /**
   * Selected version (for files/security tabs).
   */
  VERSION: 'version',
} as const;

// =============================================================================
// URL BUILDERS (PREVIEW UI)
// =============================================================================

/**
 * Builds a GA search URL for Preview UI (path-based routing).
 * Result: /preview/browse/search/maven?q=...
 */
export function buildSearchRoute(params?: {
  query?: string;
  groupId?: string;
  artifactId?: string;
  repository?: string;
  sort?: string;
  direction?: string;
}): string {
  const searchParams = new URLSearchParams();
  
  if (params?.query) {
    searchParams.set(GA_SEARCH_PARAMS.QUERY, params.query);
  }
  if (params?.groupId) {
    searchParams.set(GA_SEARCH_PARAMS.GROUP_ID, params.groupId);
  }
  if (params?.artifactId) {
    searchParams.set(GA_SEARCH_PARAMS.ARTIFACT_ID, params.artifactId);
  }
  if (params?.repository) {
    searchParams.set(GA_SEARCH_PARAMS.REPOSITORY, params.repository);
  }
  if (params?.sort) {
    searchParams.set(GA_SEARCH_PARAMS.SORT, params.sort);
  }
  if (params?.direction) {
    searchParams.set(GA_SEARCH_PARAMS.DIRECTION, params.direction);
  }
  
  const queryString = searchParams.toString();
  const base = `${PREVIEW_BASE_URL}/maven`;
  return queryString ? `${base}?${queryString}` : base;
}

/**
 * Builds a GA detail URL for Preview UI (path-based routing).
 * Result: /preview/browse/search/maven/ga/{gaId}
 */
export function buildDetailRoute(gaId: string, tab?: GADetailTab, version?: string): string {
  const encodedGaId = encodeURIComponent(gaId);
  let url = `${PREVIEW_BASE_URL}/maven/ga/${encodedGaId}`;
  
  if (tab && tab !== 'overview') {
    url += `/${tab}`;
  }
  
  if (version && (tab === 'files' || tab === 'security')) {
    url += `?${GA_SEARCH_PARAMS.VERSION}=${encodeURIComponent(version)}`;
  }
  
  return url;
}

/**
 * Builds URL to switch back to Default UI (ExtJS) search.
 */
export function buildDefaultSearchRoute(): string {
  return '#browse/search/maven';
}

// =============================================================================
// URL PARSERS
// =============================================================================

/**
 * Parses GA ID from URL parameter.
 */
export function parseGaId(encodedGaId: string): string {
  return decodeURIComponent(encodedGaId);
}

/**
 * Extracts search params from URL.
 */
export function parseSearchParams(searchParams: URLSearchParams): {
  query?: string;
  format?: string;
  groupId?: string;
  artifactId?: string;
  repository?: string;
  sort?: string;
  direction?: string;
} {
  return {
    query: searchParams.get(GA_SEARCH_PARAMS.QUERY) ?? undefined,
    format: searchParams.get(GA_SEARCH_PARAMS.FORMAT) ?? undefined,
    groupId: searchParams.get(GA_SEARCH_PARAMS.GROUP_ID) ?? undefined,
    artifactId: searchParams.get(GA_SEARCH_PARAMS.ARTIFACT_ID) ?? undefined,
    repository: searchParams.get(GA_SEARCH_PARAMS.REPOSITORY) ?? undefined,
    sort: searchParams.get(GA_SEARCH_PARAMS.SORT) ?? undefined,
    direction: searchParams.get(GA_SEARCH_PARAMS.DIRECTION) ?? undefined,
  };
}

// =============================================================================
// TAB NAVIGATION
// =============================================================================

/**
 * Maps tab names to route names for Maven GA detail.
 */
export const TAB_ROUTE_MAP: Record<GADetailTab, string> = {
  overview: GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_OVERVIEW,
  versions: GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_VERSIONS,
  repositories: GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_REPOS,
  files: GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_FILES,
  security: GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_SECURITY,
};

/**
 * Gets the tab from a route name.
 */
export function getTabFromRoute(routeName: string): GADetailTab {
  switch (routeName) {
    case GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_VERSIONS:
      return 'versions';
    case GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_REPOS:
      return 'repositories';
    case GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_FILES:
      return 'files';
    case GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_SECURITY:
      return 'security';
    default:
      return 'overview';
  }
}

