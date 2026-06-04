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
 * GA-Level Search Domain Contracts
 * 
 * AUTHORITATIVE: These types are the single source of truth for the GA search system.
 * All agents (frontend and backend) MUST conform to these contracts.
 * 
 * Phase 1: Maven ONLY - Do not generalize until Maven is complete.
 * 
 * GA Identity (Maven): groupId + artifactId
 * - One row per GA in search results (NOT per version)
 * - Versions aggregate across repositories
 * - Deterministic ordering: relevance → lastUpdated → name → gaId
 */

// =============================================================================
// FORMAT DISCRIMINATOR
// =============================================================================

/**
 * Supported formats for GA search.
 * Phase 1: Maven ONLY. Other formats are disabled.
 */
export type GAFormat = 'maven';

// =============================================================================
// VERSION STATUS
// =============================================================================

/**
 * Version status indicators.
 * Matches UX requirements for version badges.
 */
export type VersionStatus = 
  | 'recommended'     // Green badge - recommended version
  | 'quarantined'     // Yellow badge - quarantined by policy
  | 'malware'         // Red badge - confirmed malware
  | 'not-recommended' // Gray badge - not recommended (outdated, vulnerable)
  | 'none';           // No badge - standard version

// =============================================================================
// SEARCH RESULTS (GA-Level Aggregated)
// =============================================================================

/**
 * A single GA result row in the search results list.
 * Represents ONE software asset (Group + Artifact), NOT a specific version.
 * 
 * INVARIANT: Search results contain exactly ONE row per GA.
 */
export interface GAResult {
  /**
   * Unique identifier for this GA.
   * For Maven: `maven:${groupId}:${artifactId}`
   */
  readonly gaId: string;

  /**
   * Format discriminator. Phase 1: Always 'maven'.
   */
  readonly format: GAFormat;

  /**
   * Human-readable display name.
   * For Maven: artifactId (e.g., "commons-lang3")
   */
  readonly displayName: string;

  /**
   * Namespace/organization.
   * For Maven: groupId (e.g., "org.apache.commons")
   */
  readonly namespace: string;

  /**
   * Latest version available across all repositories.
   * May be undefined if no versions indexed yet.
   */
  readonly latestVersion?: string;

  /**
   * Total count of distinct versions across all repositories.
   */
  readonly versionsCount: number;

  /**
   * Count of repositories containing this GA.
   */
  readonly repositoriesCount: number;

  /**
   * License identifier (e.g., "Apache-2.0").
   * May be undefined if not declared in POM.
   */
  readonly license?: string;

  /**
   * ISO 8601 timestamp of most recent update.
   */
  readonly lastUpdated: string;
}

// =============================================================================
// GA DETAIL (Drill-down View)
// =============================================================================

/**
 * Full detail view for a specific GA.
 * Loaded when user clicks on a GA row.
 */
export interface GADetail {
  /**
   * Unique identifier (matches GAResult.gaId).
   */
  readonly gaId: string;

  /**
   * Format discriminator.
   */
  readonly format: GAFormat;

  /**
   * Human-readable display name.
   */
  readonly displayName: string;

  /**
   * Project description from POM.
   */
  readonly description?: string;

  /**
   * Project URL from POM.
   */
  readonly projectUrl?: string;

  /**
   * License identifier.
   */
  readonly license?: string;

  /**
   * All repositories containing this GA.
   */
  readonly repositories: readonly GARepository[];

  /**
   * All versions of this GA (aggregated across repositories).
   * Ordered by semantic version, descending (newest first).
   */
  readonly versions: readonly GAVersion[];
}

/**
 * Repository information for a GA.
 */
export interface GARepository {
  /**
   * Repository name.
   */
  readonly name: string;

  /**
   * Repository format (always 'maven2' for Maven).
   */
  readonly format: string;

  /**
   * Repository type.
   */
  readonly type: 'hosted' | 'proxy' | 'group';

  /**
   * Count of versions available in this repository.
   */
  readonly versionsCount: number;
}

/**
 * Version information within a GA.
 */
export interface GAVersion {
  /**
   * Version string (e.g., "3.14.0").
   */
  readonly version: string;

  /**
   * ISO 8601 timestamp of last update for this version.
   */
  readonly lastUpdated: string;

  /**
   * Repositories containing this version.
   */
  readonly repositories: readonly string[];

  /**
   * Version status indicator.
   */
  readonly status: VersionStatus;

  /**
   * Status reason (e.g., "CVE-2021-12345" for not-recommended).
   */
  readonly statusReason?: string;
}

// =============================================================================
// ASSET INFORMATION (Files Tab)
// =============================================================================

/**
 * Asset (file) within a specific version.
 */
export interface GAAsset {
  /**
   * Asset ID.
   */
  readonly id: string;

  /**
   * Repository name.
   */
  readonly repository: string;

  /**
   * Full path in repository.
   */
  readonly path: string;

  /**
   * Download URL.
   */
  readonly downloadUrl: string;

  /**
   * File format/extension (e.g., "jar", "pom", "sources.jar").
   */
  readonly format: string;

  /**
   * Maven classifier (e.g., "sources", "javadoc").
   * Undefined for main artifact.
   */
  readonly classifier?: string;

  /**
   * Maven extension (e.g., "jar", "pom", "war").
   */
  readonly extension: string;

  /**
   * File size in bytes.
   */
  readonly size: number;

  /**
   * Content type.
   */
  readonly contentType: string;

  /**
   * ISO 8601 timestamp of last modified.
   */
  readonly lastModified: string;

  /**
   * Checksum information.
   */
  readonly checksums: {
    readonly sha1?: string;
    readonly sha256?: string;
    readonly sha512?: string;
    readonly md5?: string;
  };
}

// =============================================================================
// SEARCH REQUEST/RESPONSE
// =============================================================================

/**
 * Search request parameters.
 */
export interface GASearchRequest {
  /**
   * Free-text search query.
   */
  readonly query?: string;

  /**
   * Format filter. Phase 1: Always 'maven'.
   */
  readonly format: GAFormat;

  /**
   * Maven-specific: Group ID filter.
   */
  readonly groupId?: string;

  /**
   * Maven-specific: Artifact ID filter.
   */
  readonly artifactId?: string;

  /**
   * Repository filter (search within specific repository).
   */
  readonly repository?: string;

  /**
   * Sort field.
   */
  readonly sort?: 'relevance' | 'lastUpdated' | 'name';

  /**
   * Sort direction.
   */
  readonly sortDirection?: 'asc' | 'desc';

  /**
   * Continuation token for pagination.
   */
  readonly continuationToken?: string;

  /**
   * Page size (default: 50).
   */
  readonly limit?: number;
}

/**
 * Search response.
 */
export interface GASearchResponse {
  /**
   * GA results (aggregated, one per GA).
   */
  readonly items: readonly GAResult[];

  /**
   * Total count of matching GAs (not items in this page).
   */
  readonly totalCount: number;

  /**
   * Continuation token for next page.
   * Undefined if no more results.
   */
  readonly continuationToken?: string;
}

// =============================================================================
// SUGGEST (Typeahead)
// =============================================================================

/**
 * Suggest request for typeahead.
 */
export interface GASuggestRequest {
  /**
   * Partial query string (minimum 2 characters).
   */
  readonly query: string;

  /**
   * Format filter.
   */
  readonly format: GAFormat;

  /**
   * Maximum suggestions (default: 10).
   */
  readonly limit?: number;
}

/**
 * Suggest response.
 */
export interface GASuggestResponse {
  /**
   * Suggested GAs matching the query.
   */
  readonly suggestions: readonly GASuggestion[];
}

/**
 * A single suggestion.
 */
export interface GASuggestion {
  /**
   * GA identifier.
   */
  readonly gaId: string;

  /**
   * Display text for the suggestion.
   */
  readonly displayText: string;

  /**
   * Highlight ranges for matched text.
   */
  readonly highlights: readonly [number, number][];
}

// =============================================================================
// DETAIL REQUEST
// =============================================================================

/**
 * Request for GA detail.
 */
export interface GADetailRequest {
  /**
   * GA identifier.
   */
  readonly gaId: string;
}

/**
 * Request for version assets.
 */
export interface GAVersionAssetsRequest {
  /**
   * GA identifier.
   */
  readonly gaId: string;

  /**
   * Version string.
   */
  readonly version: string;

  /**
   * Optional repository filter.
   */
  readonly repository?: string;
}

// =============================================================================
// UI STATE
// =============================================================================

/**
 * Active tab in GA detail view.
 */
export type GADetailTab = 'overview' | 'versions' | 'repositories' | 'files' | 'security';

/**
 * Search UI state.
 */
export interface GASearchState {
  /**
   * Current search query.
   */
  readonly query: string;

  /**
   * Active format filter.
   */
  readonly format: GAFormat;

  /**
   * Current sort field.
   */
  readonly sort: 'relevance' | 'lastUpdated' | 'name';

  /**
   * Sort direction.
   */
  readonly sortDirection: 'asc' | 'desc';

  /**
   * Loading state.
   */
  readonly loading: boolean;

  /**
   * Error message if any.
   */
  readonly error?: string;

  /**
   * Search results.
   */
  readonly results: readonly GAResult[];

  /**
   * Total count of matching GAs.
   */
  readonly totalCount: number;

  /**
   * Continuation token for next page.
   */
  readonly continuationToken?: string;
}

/**
 * GA detail UI state.
 */
export interface GADetailState {
  /**
   * GA identifier.
   */
  readonly gaId: string;

  /**
   * Active tab.
   */
  readonly activeTab: GADetailTab;

  /**
   * GA detail data.
   */
  readonly detail?: GADetail;

  /**
   * Selected version (for files/security tabs).
   */
  readonly selectedVersion?: string;

  /**
   * Assets for selected version.
   */
  readonly assets?: readonly GAAsset[];

  /**
   * Loading state.
   */
  readonly loading: boolean;

  /**
   * Error message if any.
   */
  readonly error?: string;
}



