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
 * Helm (Kubernetes) Search Types
 *
 * Types specific to Helm chart search functionality.
 */

/**
 * Helm search filter parameters.
 */
export interface HelmSearchFilters {
  /** Chart name */
  name?: string;
  /** Chart version */
  version?: string;
  /** Application version (the version of the app inside the chart) */
  appVersion?: string;
  /** Chart description keyword search */
  description?: string;
  /** Repository filter */
  repository?: string;
}

/**
 * A single Helm chart result in search results.
 */
export interface HelmResult {
  /** Unique identifier */
  readonly id: string;
  /** Chart name */
  readonly name: string;
  /** Display name (same as name for Helm) */
  readonly displayName: string;
  /** Latest chart version */
  readonly latestVersion: string;
  /** Application version */
  readonly appVersion?: string;
  /** Total number of versions */
  readonly versionsCount: number;
  /** Chart description */
  readonly description?: string;
  /** Chart icon URL */
  readonly icon?: string;
  /** Chart home URL */
  readonly home?: string;
  /** Chart sources */
  readonly sources?: readonly string[];
  /** Chart maintainers */
  readonly maintainers?: readonly HelmMaintainer[];
  /** Keywords */
  readonly keywords?: readonly string[];
  /** Number of repositories containing this chart */
  readonly repositoriesCount: number;
  /** ISO 8601 timestamp of last update */
  readonly lastUpdated: string;
}

/**
 * Helm chart maintainer info.
 */
export interface HelmMaintainer {
  /** Maintainer name */
  readonly name: string;
  /** Maintainer email */
  readonly email?: string;
  /** Maintainer URL */
  readonly url?: string;
}

/**
 * Helm search response.
 */
export interface HelmSearchResponse {
  /** Search results */
  readonly items: readonly HelmResult[];
  /** Total count of matching charts */
  readonly totalCount: number;
  /** Continuation token for pagination */
  readonly continuationToken?: string;
}

/**
 * Helm chart version info.
 */
export interface HelmVersion {
  /** Chart version string */
  readonly version: string;
  /** Application version */
  readonly appVersion?: string;
  /** ISO 8601 timestamp of publish date */
  readonly created: string;
  /** Repository containing this version */
  readonly repository: string;
  /** Digest/checksum */
  readonly digest?: string;
}

/**
 * Helm chart detail.
 */
export interface HelmDetail {
  /** Chart ID */
  readonly id: string;
  /** Chart name */
  readonly name: string;
  /** Display name */
  readonly displayName: string;
  /** Description */
  readonly description?: string;
  /** Chart icon URL */
  readonly icon?: string;
  /** Chart home URL */
  readonly home?: string;
  /** Source URLs */
  readonly sources?: readonly string[];
  /** Maintainers */
  readonly maintainers?: readonly HelmMaintainer[];
  /** Keywords */
  readonly keywords?: readonly string[];
  /** All versions */
  readonly versions: readonly HelmVersion[];
  /** Repositories containing this chart */
  readonly repositories: readonly string[];
}

/**
 * Helm search state.
 */
export interface HelmSearchState {
  /** Current filters */
  readonly filters: HelmSearchFilters;
  /** Loading state */
  readonly loading: boolean;
  /** Error message */
  readonly error?: string;
  /** Search results */
  readonly results: readonly HelmResult[];
  /** Total count */
  readonly totalCount: number;
  /** Continuation token */
  readonly continuationToken?: string;
}


