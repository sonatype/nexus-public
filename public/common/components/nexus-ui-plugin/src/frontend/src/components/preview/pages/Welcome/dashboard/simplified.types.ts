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
 * Simplified Dashboard Types - REAL DATA ONLY
 * 
 * All types here represent data available from existing APIs.
 * No MOCK data structures.
 */

// =============================================================================
// REPOSITORY TYPES (from /service/rest/v1/repositories)
// =============================================================================

/**
 * Repository data from API
 */
export interface Repository {
  name: string;
  format: string;
  type: 'hosted' | 'proxy' | 'group';
  url?: string;
  online?: boolean;
  status?: {
    online: boolean;
    description?: string;
    reason?: string;
  };
}

// =============================================================================
// REPOSITORIES BY FORMAT (REAL DATA)
// =============================================================================

/**
 * Aggregated repository data by format - SIMPLIFIED
 * ALL fields come from real API data.
 */
export interface RepositoryFormatSummary {
  /** Display name (e.g., 'Maven', 'npm') */
  format: string;
  /** API format code (e.g., 'maven2', 'npm') */
  formatCode: string;
  /** Count of proxy repositories */
  proxyCount: number;
  /** Count of hosted repositories */
  hostedCount: number;
  /** Count of group repositories */
  groupCount: number;
  /** Total repository count */
  totalCount: number;
  /** Count of online repositories */
  onlineCount: number;
  /** Count of offline repositories */
  offlineCount: number;
  /** True when GET /service/rest/internal/ui/malware/counts succeeded (Pro/Cloud with plugin) */
  malwareCountsAvailable?: boolean;
  /** Sum of per-repository malware counts for repos in this format (Health Check / HDS) */
  malwareCount?: number;
  /** Proxies in this format with Repository Health Check enabled */
  hcEnabledProxyCount?: number;
  /** True when Repository Health Check supports this format for malware scanning */
  rhcSupported?: boolean;
}

// =============================================================================
// INSTANCE TOTALS (from contentUsageEvaluationResult)
// =============================================================================

/**
 * Instance-wide usage metrics.
 * Data comes from ExtJS.state().getValue('contentUsageEvaluationResult')
 */
export interface InstanceTotals {
  /** Total components stored in this instance */
  totalComponents: number;
  /** Peak requests per day (highest recorded) */
  peakRequestsPerDay: number;
  /** Peak requests per month (highest recorded) */
  peakRequestsPerMonth: number;
  /** CE hard limit for total components (0 when no limit applies) */
  totalComponentsLimit: number;
  /** CE hard limit for peak requests per day (0 when no limit applies) */
  peakRequestsPerDayLimit: number;
}

// =============================================================================
// PANEL PROPS
// =============================================================================

export interface SimplifiedPanelProps {
  loading?: boolean;
  error?: string;
}

export interface RepositoriesByFormatPanelProps extends SimplifiedPanelProps {
  data: RepositoryFormatSummary[];
  onViewRepos?: (formatCode: string) => void;
  onRetry?: () => void;
}

export interface InstanceTotalsPanelProps extends SimplifiedPanelProps {
  data: InstanceTotals | null;
}

