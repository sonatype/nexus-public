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
 * Go (Golang) Search Types
 *
 * Types specific to Go module search functionality.
 */

/**
 * Go search filter parameters.
 */
export interface GolangSearchFilters {
  /** Go module path (e.g., github.com/user/repo) */
  module?: string;
  /** Semver version */
  version?: string;
  /** Keyword search */
  keyword?: string;
  /** Repository filter */
  repository?: string;
}

/**
 * A single Go module result in search results.
 */
export interface GolangResult {
  /** Unique identifier */
  readonly id: string;
  /** Go module path (e.g., github.com/user/repo) */
  readonly module: string;
  /** Latest version */
  readonly latestVersion: string;
  /** Total number of versions */
  readonly versionsCount: number;
  /** Module description */
  readonly description?: string;
  /** Module license */
  readonly license?: string;
  /** Number of repositories containing this module */
  readonly repositoriesCount: number;
  /** ISO 8601 timestamp of last update */
  readonly lastUpdated: string;
}

/**
 * Go search response.
 */
export interface GolangSearchResponse {
  /** Search results */
  readonly items: readonly GolangResult[];
  /** Total count of matching modules */
  readonly totalCount: number;
  /** Continuation token for pagination */
  readonly continuationToken?: string;
}

/**
 * Go module version info.
 */
export interface GolangVersion {
  /** Version string (semver) */
  readonly version: string;
  /** ISO 8601 timestamp */
  readonly published: string;
  /** Repository containing this version */
  readonly repository: string;
}

/**
 * Go module detail.
 */
export interface GolangDetail {
  /** Module ID */
  readonly id: string;
  /** Go module path */
  readonly module: string;
  /** Description */
  readonly description?: string;
  /** License */
  readonly license?: string;
  /** Homepage URL */
  readonly homepage?: string;
  /** Repository URL */
  readonly repositoryUrl?: string;
  /** All versions */
  readonly versions: readonly GolangVersion[];
  /** Repositories containing this module */
  readonly repositories: readonly string[];
}

/**
 * Go search state.
 */
export interface GolangSearchState {
  /** Current filters */
  readonly filters: GolangSearchFilters;
  /** Loading state */
  readonly loading: boolean;
  /** Error message */
  readonly error?: string;
  /** Search results */
  readonly results: readonly GolangResult[];
  /** Total count */
  readonly totalCount: number;
  /** Continuation token */
  readonly continuationToken?: string;
}


