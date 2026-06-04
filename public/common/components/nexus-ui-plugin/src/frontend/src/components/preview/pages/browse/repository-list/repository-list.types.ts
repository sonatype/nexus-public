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
 * Repository List Types
 *
 * Types for the Browse Repository List component.
 */

/**
 * Repository status information.
 */
export interface RepositoryStatus {
  /** Repository name */
  readonly repositoryName: string;
  /** Whether the repository is online */
  readonly online: boolean;
  /** Status description */
  readonly description?: string;
  /** Status reason (e.g., why offline) */
  readonly reason?: string;
}

/**
 * Repository type enumeration.
 */
export type RepositoryType = 'hosted' | 'proxy' | 'group';

/**
 * A single repository in the browse list.
 */
export interface Repository {
  /** Repository name (unique identifier) */
  readonly name: string;
  /** Repository type */
  readonly type: RepositoryType;
  /** Repository format (e.g., maven2, npm, docker) */
  readonly format: string;
  /** Repository status */
  readonly status: RepositoryStatus;
  /** Repository URL */
  readonly url: string;
  /** Maven version policy (RELEASE, SNAPSHOT, MIXED) — only present for maven2 repos */
  readonly versionPolicy?: 'RELEASE' | 'SNAPSHOT' | 'MIXED';
  /** Raw attributes from the API (optional, used for format-specific metadata) */
  readonly attributes?: Record<string, Record<string, unknown>>;
}

/**
 * Health check status for a repository.
 */
export interface HealthCheckStatus {
  /** Repository name */
  readonly repositoryName: string;
  /** Whether health check is enabled */
  readonly enabled: boolean;
  /** Whether analysis is in progress */
  readonly analyzing: boolean;
  /** Analysis results */
  readonly results?: {
    readonly totalCount?: number;
    readonly criticalCount?: number;
    readonly severeCount?: number;
    readonly moderateCount?: number;
  };
  /** Vulnerability counts (alternate format from some API responses) */
  readonly securityIssueCount?: number;
  readonly licenseIssueCount?: number;
  /** Malware count when HDS data is available; omitted when unknown */
  readonly malwareCount?: number | null;
  /** Detailed report URL */
  readonly detailedReport?: string | null;
}

/**
 * IQ Server / Firewall status for a repository.
 */
export interface FirewallStatus {
  /** Repository name */
  readonly repositoryName: string;
  /** Count of affected components */
  readonly affectedComponentCount: number;
  /** Count of critical components */
  readonly criticalComponentCount: number;
  /** Count of severe components */
  readonly severeComponentCount: number;
  /** Count of moderate components */
  readonly moderateComponentCount: number;
  /** Count of quarantined components */
  readonly quarantinedComponentCount: number;
  /** URL to the IQ Server report */
  readonly reportUrl?: string;
  /** Message from IQ Server */
  readonly message?: string | null;
  /** Error message from IQ Server */
  readonly errorMessage?: string | null;
}

/**
 * Sort direction.
 */
export type SortDirection = 'asc' | 'desc' | null;

/**
 * Sortable fields in the repository list.
 */
export type SortableField = 'name' | 'type' | 'format' | 'status';

/**
 * Sort configuration.
 */
export interface SortConfig {
  /** Field being sorted */
  readonly field: SortableField;
  /** Sort direction */
  readonly direction: SortDirection;
}

/**
 * Repository list state.
 */
export interface RepositoryListState {
  /** All repositories */
  readonly repositories: readonly Repository[];
  /** Filtered repositories (after filter applied) */
  readonly filteredRepositories: readonly Repository[];
  /** Current filter text */
  readonly filterText: string;
  /** Current sort configuration */
  readonly sort: SortConfig;
  /** Loading state */
  readonly loading: boolean;
  /** Error message */
  readonly error?: string;
  /** Health check statuses (keyed by repository name) */
  readonly healthCheck: Record<string, HealthCheckStatus>;
  /** Firewall statuses (keyed by repository name) */
  readonly firewallStatus: Record<string, FirewallStatus>;
  /** Whether the firewall summary API call has completed */
  readonly firewallLoaded: boolean;
  /** Health check loading error */
  readonly healthCheckError?: string;
  /** Firewall status loading error */
  readonly firewallStatusError?: string;
}

/**
 * Props for the RepositoryList component.
 */
export interface RepositoryListProps {
  /** Callback when a repository is selected */
  onSelect?: (repositoryName: string) => void;
  /** Custom URL copy handler */
  onCopyUrl?: (event: React.MouseEvent, url: string) => void;
  /** Whether to show the health check column */
  showHealthCheck?: boolean;
  /** Whether to show the IQ policy violations column */
  showIqPolicyViolations?: boolean;
}

/**
 * Props for the RepositoryStatusBadge component.
 */
export interface RepositoryStatusBadgeProps {
  /** Repository status */
  status: RepositoryStatus;
}

/**
 * Props for individual repository row.
 */
export interface RepositoryRowProps {
  /** Repository data */
  repository: Repository;
  /** Callback when row is clicked */
  onSelect?: (name: string) => void;
  /** Callback when copy URL button is clicked */
  onCopyUrl?: (event: React.MouseEvent, url: string) => void;
  /** Health check status for this repository */
  healthCheck?: HealthCheckStatus;
  /** Firewall status for this repository */
  firewallStatus?: FirewallStatus;
  /** Whether to show health check column */
  showHealthCheck?: boolean;
  /** Whether to show IQ policy violations column */
  showIqPolicyViolations?: boolean;
  /** Health check loading error */
  healthCheckError?: string;
  /** Firewall status loading error */
  firewallStatusError?: string;
}

/**
 * Strings for the repository list UI.
 */
export interface RepositoryListStrings {
  /** Column headers */
  readonly columns: {
    readonly name: string;
    readonly type: string;
    readonly format: string;
    readonly status: string;
    readonly url: string;
    readonly healthCheck: string;
    readonly iqPolicyViolations: string;
  };
  /** Filter placeholder */
  readonly filterPlaceholder: string;
  /** Empty message when no repositories */
  readonly emptyMessage: string;
  /** Copy URL button title */
  readonly copyUrlTitle: string;
  /** URL copied message */
  readonly urlCopiedMessage: string;
  /** Page title */
  readonly pageTitle: string;
  /** Page description */
  readonly pageDescription: string;
}

