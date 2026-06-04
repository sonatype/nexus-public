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
 * NuGet Search Types
 * 
 * NuGet-specific types for the search UI.
 * Similar to Maven's GA types but adapted for NuGet package structure.
 */

/**
 * NuGet search filter values.
 */
export interface NuGetSearchFilters {
  /** Package ID filter */
  packageId?: string;
  /** Package version filter */
  version?: string;
  /** Include prerelease versions */
  prerelease?: boolean;
  /** Target framework filter (e.g., net6.0, net8.0) */
  targetFramework?: string;
}

/**
 * A single NuGet package result in the search results list.
 */
export interface NuGetResult {
  /** Unique identifier for this package */
  readonly id: string;
  /** NuGet Package ID */
  readonly packageId: string;
  /** Display name */
  readonly displayName: string;
  /** Latest version */
  readonly latestVersion: string;
  /** Total version count */
  readonly versionsCount: number;
  /** Repository count */
  readonly repositoriesCount: number;
  /** Package description */
  readonly description?: string;
  /** Authors list */
  readonly authors?: readonly string[];
  /** Project URL */
  readonly projectUrl?: string;
  /** Icon URL */
  readonly iconUrl?: string;
  /** License */
  readonly license?: string;
  /** Tags */
  readonly tags?: readonly string[];
  /** Last updated timestamp (ISO 8601) */
  readonly lastUpdated: string;
  /** Total downloads (if available) */
  readonly totalDownloads?: number;
}

/**
 * NuGet version info.
 */
export interface NuGetVersion {
  /** Version string */
  readonly version: string;
  /** Whether this is a prerelease version */
  readonly isPrerelease: boolean;
  /** Downloads for this version */
  readonly downloads?: number;
  /** Published date */
  readonly published: string;
  /** Target frameworks supported */
  readonly targetFrameworks?: readonly string[];
}

/**
 * NuGet search response.
 */
export interface NuGetSearchResponse {
  /** Search results */
  readonly items: readonly NuGetResult[];
  /** Total count */
  readonly totalCount: number;
  /** Continuation token for pagination */
  readonly continuationToken?: string;
}

/**
 * NuGet search state.
 */
export interface NuGetSearchState {
  /** Search query */
  readonly query: string;
  /** Current filters */
  readonly filters: NuGetSearchFilters;
  /** Sort field */
  readonly sort: 'relevance' | 'downloads' | 'recent';
  /** Sort direction */
  readonly sortDirection: 'asc' | 'desc';
  /** Loading state */
  readonly loading: boolean;
  /** Error message */
  readonly error?: string;
  /** Search results */
  readonly results: readonly NuGetResult[];
  /** Total count */
  readonly totalCount: number;
  /** Continuation token */
  readonly continuationToken?: string;
}

/**
 * NuGet package detail.
 */
export interface NuGetDetail {
  /** Package ID */
  readonly packageId: string;
  /** Display name */
  readonly displayName: string;
  /** Description */
  readonly description?: string;
  /** Authors */
  readonly authors?: readonly string[];
  /** Project URL */
  readonly projectUrl?: string;
  /** License URL */
  readonly licenseUrl?: string;
  /** License */
  readonly license?: string;
  /** Icon URL */
  readonly iconUrl?: string;
  /** Tags */
  readonly tags?: readonly string[];
  /** All versions */
  readonly versions: readonly NuGetVersion[];
  /** Repository names */
  readonly repositories: readonly string[];
}

