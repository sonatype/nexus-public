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
 * RubyGems Search Types
 *
 * Types specific to RubyGems package search functionality.
 */

/**
 * RubyGems search filter parameters.
 */
export interface RubyGemsSearchFilters {
  /** Gem name */
  name?: string;
  /** Gem version */
  version?: string;
  /** Platform (ruby, java, etc.) */
  platform?: string;
  /** Repository filter */
  repository?: string;
}

/**
 * A single RubyGems result in search results.
 */
export interface RubyGemsResult {
  /** Unique identifier */
  readonly id: string;
  /** Gem name */
  readonly name: string;
  /** Display name */
  readonly displayName: string;
  /** Latest published version */
  readonly latestVersion: string;
  /** Total number of versions */
  readonly versionsCount: number;
  /** Platform (ruby, java, etc.) */
  readonly platform: string;
  /** Gem summary/short description */
  readonly summary?: string;
  /** Gem description */
  readonly description?: string;
  /** Gem authors */
  readonly authors?: string;
  /** Gem licenses */
  readonly licenses?: readonly string[];
  /** Homepage URL */
  readonly homepage?: string;
  /** Number of repositories containing this gem */
  readonly repositoriesCount: number;
  /** ISO 8601 timestamp of last update */
  readonly lastUpdated: string;
}

/**
 * RubyGems search response.
 */
export interface RubyGemsSearchResponse {
  /** Search results */
  readonly items: readonly RubyGemsResult[];
  /** Total count of matching gems */
  readonly totalCount: number;
  /** Continuation token for pagination */
  readonly continuationToken?: string;
}

/**
 * RubyGems version info.
 */
export interface RubyGemsVersion {
  /** Version string */
  readonly version: string;
  /** Platform */
  readonly platform: string;
  /** ISO 8601 timestamp */
  readonly published: string;
  /** Repository containing this version */
  readonly repository: string;
  /** Ruby version requirement */
  readonly rubyVersion?: string;
}

/**
 * RubyGems detail.
 */
export interface RubyGemsDetail {
  /** Gem ID */
  readonly id: string;
  /** Gem name */
  readonly name: string;
  /** Display name */
  readonly displayName: string;
  /** Summary (short description) */
  readonly summary?: string;
  /** Full description */
  readonly description?: string;
  /** Authors */
  readonly authors?: string;
  /** Licenses */
  readonly licenses?: readonly string[];
  /** Homepage URL */
  readonly homepage?: string;
  /** Source code URL */
  readonly sourceCodeUri?: string;
  /** Documentation URL */
  readonly documentationUri?: string;
  /** All versions */
  readonly versions: readonly RubyGemsVersion[];
  /** Repositories containing this gem */
  readonly repositories: readonly string[];
  /** Ruby version requirement */
  readonly rubyVersion?: string;
}

/**
 * RubyGems search state.
 */
export interface RubyGemsSearchState {
  /** Current filters */
  readonly filters: RubyGemsSearchFilters;
  /** Loading state */
  readonly loading: boolean;
  /** Error message */
  readonly error?: string;
  /** Search results */
  readonly results: readonly RubyGemsResult[];
  /** Total count */
  readonly totalCount: number;
  /** Continuation token */
  readonly continuationToken?: string;
}


