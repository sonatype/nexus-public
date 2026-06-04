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
 * npm Search Types
 *
 * Types specific to npm package search functionality.
 */

/**
 * npm search filter parameters.
 */
export interface NpmSearchFilters {
  /** @scope (e.g., @angular, @types) */
  scope?: string;
  /** Package name */
  name?: string;
  /** Semver version */
  version?: string;
  /** dist-tag (latest, next, beta) */
  tag?: string;
  /** Repository filter */
  repository?: string;
}

/**
 * A single npm package result in search results.
 */
export interface NpmResult {
  /** Unique identifier */
  readonly id: string;
  /** @scope or empty string */
  readonly scope: string;
  /** Package name (without scope) */
  readonly name: string;
  /** Full display name (@scope/name or just name) */
  readonly displayName: string;
  /** Latest published version */
  readonly latestVersion: string;
  /** Total number of versions */
  readonly versionsCount: number;
  /** Package description from package.json */
  readonly description?: string;
  /** Package author */
  readonly author?: string;
  /** Package license */
  readonly license?: string;
  /** Keywords/tags */
  readonly keywords?: readonly string[];
  /** Number of repositories containing this package */
  readonly repositoriesCount: number;
  /** ISO 8601 timestamp of last update */
  readonly lastUpdated: string;
}

/**
 * npm search response.
 */
export interface NpmSearchResponse {
  /** Search results */
  readonly items: readonly NpmResult[];
  /** Total count of matching packages */
  readonly totalCount: number;
  /** Continuation token for pagination */
  readonly continuationToken?: string;
}

/**
 * npm package version info.
 */
export interface NpmVersion {
  /** Version string (semver) */
  readonly version: string;
  /** dist-tags pointing to this version */
  readonly tags: readonly string[];
  /** ISO 8601 timestamp */
  readonly published: string;
  /** Repository containing this version */
  readonly repository: string;
}

/**
 * npm package detail.
 */
export interface NpmDetail {
  /** Package ID */
  readonly id: string;
  /** Scope */
  readonly scope: string;
  /** Package name */
  readonly name: string;
  /** Display name */
  readonly displayName: string;
  /** Description */
  readonly description?: string;
  /** Author info */
  readonly author?: string;
  /** License */
  readonly license?: string;
  /** Homepage URL */
  readonly homepage?: string;
  /** Repository URL */
  readonly repositoryUrl?: string;
  /** Keywords */
  readonly keywords?: readonly string[];
  /** All versions */
  readonly versions: readonly NpmVersion[];
  /** Repositories containing this package */
  readonly repositories: readonly string[];
}

/**
 * npm search state.
 */
export interface NpmSearchState {
  /** Current filters */
  readonly filters: NpmSearchFilters;
  /** Loading state */
  readonly loading: boolean;
  /** Error message */
  readonly error?: string;
  /** Search results */
  readonly results: readonly NpmResult[];
  /** Total count */
  readonly totalCount: number;
  /** Continuation token */
  readonly continuationToken?: string;
}

