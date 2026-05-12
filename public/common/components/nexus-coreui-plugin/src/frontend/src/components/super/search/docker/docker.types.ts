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
 * Docker Search Types
 *
 * Types specific to Docker image search in Preview UI.
 * Follows the Maven/GA search pattern.
 */

/**
 * Docker search filter parameters.
 */
export interface DockerSearchFilters {
  /** Image name (e.g., nginx, ubuntu) */
  imageName?: string;
  /** Image tag (e.g., latest, 1.0.0, alpine) */
  tag?: string;
  /** SHA256 digest */
  digest?: string;
}

/**
 * A single Docker search result representing an image.
 */
export interface DockerResult {
  /** Unique identifier for this image */
  id: string;
  /** Full image name (registry/name) */
  imageName: string;
  /** Short display name */
  displayName: string;
  /** Most recent tag */
  latestTag: string;
  /** Number of tags */
  tagsCount: number;
  /** Image size (human readable) */
  size?: string;
  /** Last update timestamp (ISO 8601) */
  lastUpdated: string;
  /** Repository name */
  repository?: string;
}

/**
 * A tag/version for a Docker image.
 */
export interface DockerTag {
  /** Tag name (e.g., latest, 1.0.0) */
  name: string;
  /** Digest (SHA256) */
  digest: string;
  /** Size in bytes */
  sizeBytes?: number;
  /** Human-readable size */
  size?: string;
  /** Push timestamp */
  pushedAt: string;
  /** Operating system */
  os?: string;
  /** Architecture */
  architecture?: string;
}

/**
 * Full detail for a Docker image.
 */
export interface DockerDetail {
  /** Unique identifier */
  id: string;
  /** Full image name */
  imageName: string;
  /** Short display name */
  displayName: string;
  /** Description */
  description?: string;
  /** All available tags */
  tags: DockerTag[];
  /** Total tags count */
  tagsCount: number;
  /** Repository name */
  repository: string;
  /** Last update timestamp */
  lastUpdated: string;
}

/**
 * Docker search request parameters.
 */
export interface DockerSearchRequest {
  /** Free text query */
  query?: string;
  /** Filter by image name */
  imageName?: string;
  /** Filter by tag */
  tag?: string;
  /** Filter by digest */
  digest?: string;
  /** Repository filter */
  repository?: string;
  /** Sort field */
  sort?: 'relevance' | 'lastUpdated' | 'name';
  /** Sort direction */
  sortDirection?: 'asc' | 'desc';
  /** Pagination token */
  continuationToken?: string;
  /** Results per page */
  limit?: number;
}

/**
 * Docker search response.
 */
export interface DockerSearchResponse {
  /** Search results */
  items: DockerResult[];
  /** Total count of matching results */
  totalCount: number;
  /** Token for next page */
  continuationToken?: string;
}

/**
 * Typeahead suggestion for Docker search.
 */
export interface DockerSuggestion {
  /** Unique ID for navigation */
  id: string;
  /** Display text for the suggestion */
  displayText: string;
  /** Highlight ranges [start, end] */
  highlights?: [number, number][];
}

/**
 * Docker suggest response.
 */
export interface DockerSuggestResponse {
  /** Suggestions */
  suggestions: DockerSuggestion[];
}

/**
 * State for Docker search UI.
 */
export interface DockerSearchState {
  /** Current query */
  query: string;
  /** Current sort field */
  sort: 'relevance' | 'lastUpdated' | 'name';
  /** Sort direction */
  sortDirection: 'asc' | 'desc';
  /** Loading state */
  loading: boolean;
  /** Error message */
  error?: string;
  /** Search results */
  results: DockerResult[];
  /** Total result count */
  totalCount: number;
  /** Pagination token */
  continuationToken?: string;
}

/**
 * State for Docker detail UI.
 */
export interface DockerDetailState {
  /** Loading state */
  loading: boolean;
  /** Error message */
  error?: string;
  /** Image detail */
  detail?: DockerDetail;
}


