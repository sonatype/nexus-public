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
 * Image configuration metadata pulled from the Docker manifest/config blob.
 *
 * Surfaced in the Preview UI so operators can see OS/arch, environment, entrypoint,
 * and other runtime details without having to shell into the registry (NEXUS-51972).
 * All fields optional — the backend populates only what's available on each image.
 */
export interface DockerImageMetadata {
  /** Operating system (e.g. "linux") */
  os?: string;
  /** CPU architecture (e.g. "amd64", "arm64") */
  arch?: string;
  /** Image creation timestamp (ISO 8601) */
  created?: string;
  /** Image author */
  author?: string;
  /** Environment variables (KEY=VALUE entries) */
  env?: string[];
  /** Default CMD for the image */
  cmd?: string[];
  /** ENTRYPOINT for the image */
  entrypoint?: string[];
  /** Working directory inside the container */
  workingDir?: string;
  /** Exposed ports (e.g. ["80/tcp", "443/tcp"]) */
  exposedPorts?: string[];
  /** Image labels */
  labels?: Record<string, string>;
  /** Layer/build history entries */
  history?: unknown[];
  /** Total image size in bytes */
  totalSize?: number;
  /** Registry URL the image was pushed to */
  registryUrl?: string;
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
  /**
   * Registry URL (e.g. "localhost:5000"). When present it is prefixed onto
   * `docker pull` snippets so users get a ready-to-run command (NEXUS-51972).
   */
  registryUrl?: string;
  /**
   * Image configuration metadata (os/arch/env/cmd/entrypoint/etc).
   * Rendered in the detail page when present (NEXUS-51972).
   */
  metadata?: DockerImageMetadata;
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
