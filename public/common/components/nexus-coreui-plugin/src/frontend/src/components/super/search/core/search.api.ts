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
 * GA-Level Search API Contracts
 * 
 * AUTHORITATIVE: These are the API contracts that Agent 3 (Backend) must implement.
 * 
 * All endpoints are under: /service/rest/v1/search/ga
 * 
 * Phase 1: Maven ONLY
 */

import type {
  GASearchRequest,
  GASearchResponse,
  GASuggestRequest,
  GASuggestResponse,
  GADetail,
  GAAsset,
} from './search.types';

// =============================================================================
// API ENDPOINTS (Backend Contract)
// =============================================================================

/**
 * Backend API endpoints for GA search.
 * Agent 3 MUST implement these exactly as specified.
 */
export const GA_SEARCH_API = {
  /**
   * Base path for all GA search endpoints.
   */
  BASE_PATH: '/service/rest/v1/search/ga',

  /**
   * Search for GAs (aggregated results).
   * GET /service/rest/v1/search/ga
   * 
   * Query Parameters:
   * - q: Free-text search query
   * - format: Format filter (always 'maven' in Phase 1)
   * - maven.groupId: Maven group ID filter
   * - maven.artifactId: Maven artifact ID filter
   * - repository: Repository filter
   * - sort: Sort field (relevance|lastUpdated|name)
   * - direction: Sort direction (asc|desc)
   * - continuationToken: Pagination token
   * - limit: Page size (default: 50, max: 100)
   * 
   * Response: GASearchResponse
   * 
   * INVARIANTS:
   * - Returns one row per GA (NOT per version)
   * - Ordering is deterministic: same query always returns same order
   * - Order stability: loading more pages never reshuffles existing results
   */
  SEARCH: '/service/rest/v1/search/ga',

  /**
   * Get suggestions for typeahead.
   * GET /service/rest/v1/search/ga/suggest
   * 
   * Query Parameters:
   * - q: Partial query (minimum 2 characters)
   * - format: Format filter
   * - limit: Max suggestions (default: 10)
   * 
   * Response: GASuggestResponse
   */
  SUGGEST: '/service/rest/v1/search/ga/suggest',

  /**
   * Get GA detail.
   * GET /service/rest/v1/search/ga/{gaId}
   * 
   * Path Parameters:
   * - gaId: GA identifier (URL-encoded)
   * 
   * Response: GADetail
   */
  DETAIL: '/service/rest/v1/search/ga/:gaId',

  /**
   * Get assets for a specific version.
   * GET /service/rest/v1/search/ga/{gaId}/versions/{version}/assets
   * 
   * Path Parameters:
   * - gaId: GA identifier (URL-encoded)
   * - version: Version string (URL-encoded)
   * 
   * Query Parameters:
   * - repository: Optional repository filter
   * 
   * Response: { assets: GAAsset[] }
   */
  VERSION_ASSETS: '/service/rest/v1/search/ga/:gaId/versions/:version/assets',
} as const;

// =============================================================================
// API CLIENT FUNCTIONS
// =============================================================================

/**
 * Builds the URL for GA search.
 */
export function buildSearchUrl(request: GASearchRequest): string {
  const params = new URLSearchParams();
  
  if (request.query) {
    params.set('q', request.query);
  }
  params.set('format', request.format);
  
  if (request.groupId) {
    params.set('maven.groupId', request.groupId);
  }
  if (request.artifactId) {
    params.set('maven.artifactId', request.artifactId);
  }
  if (request.repository) {
    params.set('repository', request.repository);
  }
  if (request.sort) {
    params.set('sort', request.sort);
  }
  if (request.sortDirection) {
    params.set('direction', request.sortDirection);
  }
  if (request.continuationToken) {
    params.set('continuationToken', request.continuationToken);
  }
  if (request.limit) {
    params.set('limit', String(request.limit));
  }
  
  return `${GA_SEARCH_API.SEARCH}?${params.toString()}`;
}

/**
 * Builds the URL for GA suggestions.
 */
export function buildSuggestUrl(request: GASuggestRequest): string {
  const params = new URLSearchParams();
  params.set('q', request.query);
  params.set('format', request.format);
  if (request.limit) {
    params.set('limit', String(request.limit));
  }
  return `${GA_SEARCH_API.SUGGEST}?${params.toString()}`;
}

/**
 * Builds the URL for GA detail.
 */
export function buildDetailUrl(gaId: string): string {
  return `${GA_SEARCH_API.BASE_PATH}/${encodeURIComponent(gaId)}`;
}

/**
 * Builds the URL for version assets.
 */
export function buildVersionAssetsUrl(gaId: string, version: string, repository?: string): string {
  const base = `${GA_SEARCH_API.BASE_PATH}/${encodeURIComponent(gaId)}/versions/${encodeURIComponent(version)}/assets`;
  if (repository) {
    return `${base}?repository=${encodeURIComponent(repository)}`;
  }
  return base;
}

// =============================================================================
// API CLIENT
// =============================================================================

/**
 * GA Search API client.
 * Wraps fetch calls with proper error handling.
 */
export const gaSearchApi = {
  /**
   * Search for GAs.
   */
  async search(request: GASearchRequest): Promise<GASearchResponse> {
    const url = buildSearchUrl(request);
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
    });
    
    if (!response.ok) {
      throw new GASearchError('Search failed', response.status);
    }
    
    return response.json();
  },

  /**
   * Get suggestions for typeahead.
   */
  async suggest(request: GASuggestRequest): Promise<GASuggestResponse> {
    const url = buildSuggestUrl(request);
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
    });
    
    if (!response.ok) {
      throw new GASearchError('Suggest failed', response.status);
    }
    
    return response.json();
  },

  /**
   * Get GA detail.
   */
  async getDetail(gaId: string): Promise<GADetail> {
    const url = buildDetailUrl(gaId);
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
    });
    
    if (!response.ok) {
      if (response.status === 404) {
        throw new GASearchError('GA not found', 404);
      }
      throw new GASearchError('Failed to load GA detail', response.status);
    }
    
    return response.json();
  },

  /**
   * Get assets for a version.
   */
  async getVersionAssets(gaId: string, version: string, repository?: string): Promise<{ assets: GAAsset[] }> {
    const url = buildVersionAssetsUrl(gaId, version, repository);
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
    });
    
    if (!response.ok) {
      if (response.status === 404) {
        throw new GASearchError('Version not found', 404);
      }
      throw new GASearchError('Failed to load assets', response.status);
    }
    
    return response.json();
  },
};

/**
 * Custom error class for GA search errors.
 */
export class GASearchError extends Error {
  constructor(
    message: string,
    public readonly statusCode: number
  ) {
    super(message);
    this.name = 'GASearchError';
  }
}

// =============================================================================
// BACKEND API SPECIFICATION (For Agent 3)
// =============================================================================

/**
 * BACKEND IMPLEMENTATION REQUIREMENTS
 * 
 * Agent 3 must implement the following:
 * 
 * 1. GET /service/rest/v1/search/ga
 *    - Query the search index for components
 *    - Group by GA identity (Maven: groupId + artifactId)
 *    - Return ONE row per GA, NOT per version
 *    - Include aggregated counts (versionsCount, repositoriesCount)
 *    - Support pagination via continuation token
 *    - ORDERING MUST BE DETERMINISTIC:
 *      - Primary: relevance score (if query provided)
 *      - Secondary: lastUpdated (descending)
 *      - Tertiary: displayName (ascending)
 *      - Final: gaId (ascending, for stability)
 * 
 * 2. GET /service/rest/v1/search/ga/suggest
 *    - Fast prefix/substring matching
 *    - Return top N matching GAs
 *    - Include highlight ranges for UI
 *    - Response time < 100ms
 * 
 * 3. GET /service/rest/v1/search/ga/{gaId}
 *    - Return full GA detail
 *    - Include all versions (aggregated across repos)
 *    - Include all repositories
 *    - Include version status (recommended, quarantined, malware)
 *    - Versions ordered by semantic version (descending)
 * 
 * 4. GET /service/rest/v1/search/ga/{gaId}/versions/{version}/assets
 *    - Return all assets for a specific version
 *    - Include download URLs
 *    - Include checksums
 *    - Include file sizes
 * 
 * MAVEN GA IDENTITY:
 * - gaId = `maven:${groupId}:${artifactId}`
 * - Example: `maven:org.apache.commons:commons-lang3`
 * 
 * VERSION STATUS LOGIC:
 * - 'recommended': Marked as recommended in repository
 * - 'quarantined': Quarantined by IQ Server policy
 * - 'malware': Confirmed malware
 * - 'not-recommended': Has known vulnerabilities or is outdated
 * - 'none': Standard version with no special status
 */



