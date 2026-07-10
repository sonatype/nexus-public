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

// =============================================================================
// Repository Profile Types
// Shared types for the Repository Profile page machine, hook, and component
// =============================================================================

// =============================================================================
// Repository Data Types
// =============================================================================

export interface RepositoryProfileData {
  // Basic info
  name: string;
  type: 'hosted' | 'proxy' | 'group';
  format: string;
  url: string;
  online: boolean;
  recipe?: string;

  // Status
  status?: {
    online: boolean;
    description?: string;
    reason?: string;
  };

  // Full configuration attributes
  attributes?: {
    storage?: {
      blobStoreName?: string;
      strictContentTypeValidation?: boolean;
      writePolicy?: string;
    };
    maven?: {
      versionPolicy?: string;
      layoutPolicy?: string;
      contentDisposition?: string;
    };
    proxy?: {
      remoteUrl?: string;
      contentMaxAge?: number;
      metadataMaxAge?: number;
    };
    negativeCache?: {
      enabled?: boolean;
      timeToLive?: number;
    };
    httpClient?: {
      blocked?: boolean;
      autoBlock?: boolean;
      connection?: {
        retries?: number;
        userAgentSuffix?: string;
        timeout?: number;
        enableCircularRedirects?: boolean;
        enableCookies?: boolean;
        useTrustStore?: boolean;
      };
      authentication?: {
        type?: string;
        username?: string;
        ntlmHost?: string;
        ntlmDomain?: string;
      };
    };
    cleanup?: {
      policyName?: string[];
    };
    component?: {
      proprietaryComponents?: boolean;
    };
    docker?: {
      httpPort?: number;
      httpsPort?: number;
      v1Enabled?: boolean;
      forceBasicAuth?: boolean;
      subdomain?: string;
    };
    dockerProxy?: {
      indexType?: string;
      indexUrl?: string;
      cacheForeignLayers?: boolean;
      foreignLayerUrlWhitelist?: string[];
    };
    group?: {
      memberNames?: string[];
      writableMember?: string;
    };
    replication?: {
      preemptivePullEnabled?: boolean;
      preemptivePullRetryInterval?: number;
    };
    cocoapods?: {
      podBaseUrl?: string;
    };
    conan?: {
      forceBasicAuth?: boolean;
    };
    conda?: {
      assetHistoryWindow?: number;
    };
    go?: {
      loadParentModules?: boolean;
    };
    gitlfs?: {
      strictContentTypeValidation?: boolean;
    };
    helm?: {
      repositoryRootUrl?: string;
    };
    npm?: {
      removeQuarantined?: boolean;
    };
    nugetProxy?: {
      queryCacheItemMaxAge?: number;
      nugetVersion?: string;
    };
    p2?: {
      proxyType?: string;
    };
    r?: {
      pathId?: string;
    };
    rubygems?: {
      storeExtraMetadata?: boolean;
    };
    yum?: {
      repodataDepth?: number;
      groupId?: string;
    };
    apt?: {
      distribution?: string;
      flat?: boolean;
    };
    aptSigning?: {
      keyId?: string;
      passphrase?: string;
    };
    raw?: {
      contentDisposition?: string;
    };
  };
  routingRuleId?: string;
}

// =============================================================================
// IQ / Firewall Types
// =============================================================================

/**
 * Health check data for a repository.
 * Fields match HealthCheckRepositoryStatusXO from the backend.
 */
export interface HealthCheckData {
  enabled: boolean;
  analyzing?: boolean;
  securityIssueCount?: number;
  licenseIssueCount?: number;
  malwareCount?: number | null;
  vulnerableCounts?: number[];
  totalCounts?: number[];
  summaryUrl?: string;
  detailUrl?: string;
  lastAnalyzedDate?: number;
}

export interface FirewallData {
  enabled: boolean;
  quarantineEnabled?: boolean;
  affectedComponentCount?: number;
  criticalComponentCount?: number;
  severeComponentCount?: number;
  moderateComponentCount?: number;
  quarantinedComponentCount?: number;
  reportUrl?: string;
  message?: string;
  errorMessage?: string;
}

export interface MalwareCleanupSummary {
  repositoryName: string;
  scrubbedCount: number;
  pendingCount: number;
  lastRun?: string;
  taskStatus: string;
  taskEnabled: boolean;
  /** Whether the task has enableMalwareCleanup=true (delete mode vs audit-only) */
  taskCleanupEnabled: boolean;
}

export interface IqApplicationMapping {
  applicationId: string;
  applicationName?: string;
  repositoryName: string;
}

export interface IqCapabilities {
  connected: boolean;
  hasFirewall: boolean;
  hasLifecycle: boolean;
  url?: string;
  deploymentId?: string;
}

// =============================================================================
// Metrics Types
// =============================================================================

export interface RepositoryMetrics {
  componentCount: number;
  assetCount: number;
  totalSize: number;
  downloadsPerMonth: number;
  uploadsPerMonth: number;
}

// =============================================================================
// Blob Store Types
// =============================================================================

export interface BlobStoreInfo {
  name: string;
  type: string;
  path?: string;
  unavailable: boolean;
  totalSizeInBytes?: number;
  availableSpaceInBytes?: number;
  blobCount?: number;
}

export interface CleanupPolicyInfo {
  name: string;
  format: string;
  notes?: string;
  lastRun?: string;
}

export interface RoutingRuleInfo {
  name: string;
  description?: string;
  mode: string;
  matchers: string[];
}

// =============================================================================
// Security Types
// =============================================================================

export interface PrivilegeInfo {
  name: string;
  description?: string;
  type: string;
  repository?: string;
  format?: string;
  actions?: string[];
}

export interface RoleInfo {
  id: string;
  name: string;
  description?: string;
  privileges: string[];
  roles: string[];
}

export interface UserWithAccess {
  userId: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  roles?: string[];
  permissions?: string[];
}

export interface AnonymousAccess {
  enabled: boolean;
  userId?: string;
  realmName?: string;
  realm?: string;
  canRead?: boolean;
  canBrowse?: boolean;
}

// =============================================================================
// System Types
// =============================================================================

export interface TaskInfo {
  id: string;
  name: string;
  type: string;
  schedule?: string;
  lastRun?: string;
  lastRunResult?: string;
  nextRun?: string;
  currentState?: string;
}

export interface CapabilityInfo {
  id: string;
  type: string;
  enabled: boolean;
  notes?: string;
  affectsRepo?: boolean;
  affectsReason?: string;
  properties?: Record<string, string>;
}

export interface HttpSettingsInfo {
  userAgentSuffix?: string;
  httpProxy?: {
    host: string;
    port: number;
  };
  httpsProxy?: {
    host: string;
    port: number;
  };
  nonProxyHosts?: string[];
}

// =============================================================================
// Action Types (for machine)
// =============================================================================

/**
 * Actions that require confirmation before execution.
 */
export type RepositoryAction =
  | 'invalidate-cache'
  | 'rebuild-index'
  | 'toggle-online';

/**
 * Action metadata for confirmation dialogs.
 */
export interface ActionMetadata {
  title: string;
  message: string;
  confirmLabel: string;
  variant: 'warning' | 'danger';
}

/**
 * Map of action types to their confirmation dialog content.
 */
export const ACTION_METADATA: Record<RepositoryAction, ActionMetadata> = {
  'invalidate-cache': {
    title: 'Invalidate Cache',
    message: 'This will clear all cached content from this proxy repository. Components will need to be re-fetched from the remote repository. This action cannot be undone.',
    confirmLabel: 'Invalidate Cache',
    variant: 'warning',
  },
  'rebuild-index': {
    title: 'Rebuild Index',
    message: 'This will rebuild the search index for this repository. The process may take a while for large repositories. During rebuild, search results may be incomplete.',
    confirmLabel: 'Rebuild Index',
    variant: 'warning',
  },
  'toggle-online': {
    title: 'Toggle Repository Status',
    message: 'This will change the online status of the repository. Users may be affected.',
    confirmLabel: 'Continue',
    variant: 'warning',
  },
};

// =============================================================================
// Tab Types
// =============================================================================

export type TabId =
  | 'repository'
  | 'structure'
  | 'membership'
  | 'usage'
  | 'audit'
  | 'security'
  | 'system'
  | 'instance-config';

export const TABS: Array<{ id: TabId; label: string }> = [
  { id: 'repository', label: 'Repository' },
  { id: 'structure', label: 'Structure' },
  { id: 'membership', label: 'Group Membership' },
  { id: 'usage', label: 'Usage' },
  { id: 'audit', label: 'Audit' },
  { id: 'security', label: 'Access & Security' },
  { id: 'system', label: 'System' },
  { id: 'instance-config', label: 'Instance Config' },
];
