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
 * Apt/Debian Search Types
 *
 * Types specific to Apt (Debian/Ubuntu) package search functionality.
 */

/**
 * Apt search filter parameters.
 */
export interface AptSearchFilters {
  /** Package name */
  name?: string;
  /** Package version */
  version?: string;
  /** Architecture (amd64, arm64, i386, all) */
  architecture?: string;
  /** Distribution (e.g., bullseye, bookworm, jammy) */
  distribution?: string;
  /** Component (main, contrib, non-free) */
  component?: string;
  /** Repository filter */
  repository?: string;
}

/**
 * A single Apt package result in search results.
 */
export interface AptResult {
  /** Unique identifier */
  readonly id: string;
  /** Package name */
  readonly name: string;
  /** Display name */
  readonly displayName: string;
  /** Latest version */
  readonly latestVersion: string;
  /** Total number of versions */
  readonly versionsCount: number;
  /** Architecture */
  readonly architecture: string;
  /** Distribution */
  readonly distribution?: string;
  /** Component */
  readonly component?: string;
  /** Package description */
  readonly description?: string;
  /** Package maintainer */
  readonly maintainer?: string;
  /** Package section (e.g., utils, net, libs) */
  readonly section?: string;
  /** Package priority (required, important, standard, optional, extra) */
  readonly priority?: string;
  /** Installed size in bytes */
  readonly installedSize?: number;
  /** Number of repositories containing this package */
  readonly repositoriesCount: number;
  /** ISO 8601 timestamp of last update */
  readonly lastUpdated: string;
}

/**
 * Apt search response.
 */
export interface AptSearchResponse {
  /** Search results */
  readonly items: readonly AptResult[];
  /** Total count of matching packages */
  readonly totalCount: number;
  /** Continuation token for pagination */
  readonly continuationToken?: string;
}

/**
 * Apt package version info.
 */
export interface AptVersion {
  /** Version string */
  readonly version: string;
  /** Architecture */
  readonly architecture: string;
  /** Distribution */
  readonly distribution?: string;
  /** Component */
  readonly component?: string;
  /** ISO 8601 timestamp */
  readonly published: string;
  /** Repository containing this version */
  readonly repository: string;
  /** Download URL */
  readonly downloadUrl?: string;
}

/**
 * Apt package detail.
 */
export interface AptDetail {
  /** Package ID */
  readonly id: string;
  /** Package name */
  readonly name: string;
  /** Display name */
  readonly displayName: string;
  /** Description */
  readonly description?: string;
  /** Maintainer */
  readonly maintainer?: string;
  /** Section */
  readonly section?: string;
  /** Priority */
  readonly priority?: string;
  /** Homepage URL */
  readonly homepage?: string;
  /** All versions */
  readonly versions: readonly AptVersion[];
  /** Repositories containing this package */
  readonly repositories: readonly string[];
  /** Dependencies */
  readonly depends?: readonly string[];
  /** Recommended packages */
  readonly recommends?: readonly string[];
  /** Suggested packages */
  readonly suggests?: readonly string[];
}

/**
 * Apt search state.
 */
export interface AptSearchState {
  /** Current filters */
  readonly filters: AptSearchFilters;
  /** Loading state */
  readonly loading: boolean;
  /** Error message */
  readonly error?: string;
  /** Search results */
  readonly results: readonly AptResult[];
  /** Total count */
  readonly totalCount: number;
  /** Continuation token */
  readonly continuationToken?: string;
}


