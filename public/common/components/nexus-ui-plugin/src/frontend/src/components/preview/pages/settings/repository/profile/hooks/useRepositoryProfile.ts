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

import { useState, useCallback, useEffect } from 'react';
import { restClient, parseApiError } from '../../../../../../../interface/api';

// =============================================================================
// Types
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

// IQ / Firewall types
// Fields match HealthCheckRepositoryStatusXO from the backend
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

// Metrics types
export interface RepositoryMetrics {
  componentCount: number;
  assetCount: number;
  totalSize: number;
  downloadsPerMonth: number;
  uploadsPerMonth: number;
}

// Internal interface for UI details
interface RepositoryDetail {
  name: string;
  format: string;
  type: string;
  online: boolean;
  componentCount?: number;
  assetCount?: number;
  size?: number;
}

// Blob store types
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

// Security types
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

// System types for System tab
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

export interface UseRepositoryProfileResult {
  // Repository data
  repository: RepositoryProfileData | null;
  blobStore: BlobStoreInfo | null;
  cleanupPolicies: CleanupPolicyInfo[];
  routingRule: RoutingRuleInfo | null;

  // IQ Server data
  healthCheck: HealthCheckData | null;
  firewall: FirewallData | null;
  malwareCleanupSummary: MalwareCleanupSummary | null;
  iqMapping: IqApplicationMapping | null;
  iqCapabilities: IqCapabilities | null;

  // Metrics
  metrics: RepositoryMetrics | null;

  // Security/Access data
  privileges: PrivilegeInfo[];
  roles: RoleInfo[];
  users: UserWithAccess[];
  anonymousAccess: AnonymousAccess | null;

  // System data
  tasks: TaskInfo[];
  capabilities: CapabilityInfo[];
  httpSettings: HttpSettingsInfo | null;

  // State
  loading: boolean;
  securityLoading: boolean;
  systemLoading: boolean;
  error: string | null;

  // Actions
  refresh: () => Promise<void>;
}

// =============================================================================
// API Endpoints
// =============================================================================

const BLOB_STORES_INTERNAL_URL = '/service/rest/internal/ui/blobstores';
const SEARCH_URL = '/service/rest/v1/search';
const PRIVILEGES_URL = '/service/rest/v1/security/privileges';
const ROLES_URL = '/service/rest/v1/security/roles';
const USERS_URL = '/service/rest/v1/security/users';
const ANONYMOUS_URL = '/service/rest/v1/security/anonymous';
const TASKS_URL = '/service/rest/v1/tasks';
const CAPABILITIES_URL = '/service/rest/v1/capabilities';
const HTTP_URL = '/service/rest/v1/http';

// =============================================================================
// Hook
// =============================================================================

/**
 * useRepositoryProfile - Aggregates all data needed for the Repository Profile page
 *
 * Fetches:
 * - Repository configuration (from internal UI API)
 * - Blob store details (from REST API)
 * - Cleanup policies associated with repo
 * - Routing rules
 * - Health check status (from health check API)
 * - Firewall status (if available)
 * - Repository metrics (component/asset counts)
 * - Privileges, Roles, Users with access
 * - Tasks affecting this repo
 * - Capabilities affecting this repo
 */
export function useRepositoryProfile(repositoryName: string): UseRepositoryProfileResult {
  // Repository data
  const [repository, setRepository] = useState<RepositoryProfileData | null>(null);
  const [blobStore, setBlobStore] = useState<BlobStoreInfo | null>(null);
  const [cleanupPolicies, setCleanupPolicies] = useState<CleanupPolicyInfo[]>([]);
  const [routingRule, setRoutingRule] = useState<RoutingRuleInfo | null>(null);

  // IQ Server data
  const [healthCheck, setHealthCheck] = useState<HealthCheckData | null>(null);
  const [firewall, setFirewall] = useState<FirewallData | null>(null);
  const [malwareCleanupSummary, setMalwareCleanupSummary] = useState<MalwareCleanupSummary | null>(null);
  const [iqMapping, setIqMapping] = useState<IqApplicationMapping | null>(null);
  const [iqCapabilities, setIqCapabilities] = useState<IqCapabilities | null>(null);

  // Metrics
  const [metrics, setMetrics] = useState<RepositoryMetrics | null>(null);

  // Security/Access data
  const [privileges, setPrivileges] = useState<PrivilegeInfo[]>([]);
  const [roles, setRoles] = useState<RoleInfo[]>([]);
  const [users, setUsers] = useState<UserWithAccess[]>([]);
  const [anonymousAccess, setAnonymousAccess] = useState<AnonymousAccess | null>(null);

  // System data
  const [tasks, setTasks] = useState<TaskInfo[]>([]);
  const [capabilities, setCapabilities] = useState<CapabilityInfo[]>([]);
  const [httpSettings, setHttpSettings] = useState<HttpSettingsInfo | null>(null);

  // State
  const [loading, setLoading] = useState(true);
  const [securityLoading, setSecurityLoading] = useState(true);
  const [systemLoading, setSystemLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch repository data from REST API
   */
  const fetchRepository = useCallback(async (): Promise<RepositoryProfileData | null> => {
    try {
      const fullConfig = await restClient.get<RepositoryProfileData & { storage?: { blobStoreName?: string } }>(
        `/service/rest/internal/ui/repositories/repository/${encodeURIComponent(repositoryName)}`
      );
      if (!fullConfig) {
        return null;
      }

      const internal = fullConfig as {
        storage?: { blobStoreName?: string; strictContentTypeValidation?: boolean; writePolicy?: string };
        cleanup?: { policyName?: string[] };
        routingRuleName?: string;
      };
      const config: RepositoryProfileData = {
        name: fullConfig.name,
        type: fullConfig.type as 'hosted' | 'proxy' | 'group',
        format: fullConfig.format,
        url: fullConfig.url,
        online: fullConfig.online,
        status: fullConfig.status,
        recipe: fullConfig.recipe,
        attributes: {
          ...fullConfig.attributes,
          storage: internal.storage,
          cleanup: internal.cleanup,
        },
        routingRuleId: internal.routingRuleName,
      };
      return config;
    } catch (err) {
      console.warn('Could not fetch repository config:', err);
      return null;
    }
  }, [repositoryName]);

  /**
   * Fetch blob store details from REST API
   */
  const fetchBlobStore = useCallback(async (blobStoreName: string): Promise<BlobStoreInfo | null> => {
    try {
      const data = await restClient.get<any[]>(BLOB_STORES_INTERNAL_URL);
      const match = data?.find((b) => b.name === blobStoreName);
      if (!match) {
        return null;
      }
      return {
        name: match.name,
        type: match.typeName ?? match.typeId ?? 'Unknown',
        path: match.path,
        unavailable: match.unavailable,
        totalSizeInBytes: match.totalSizeInBytes,
        availableSpaceInBytes: match.availableSpaceInBytes,
        blobCount: match.blobCount,
      };
    } catch (err) {
      console.warn('Could not fetch blob store details:', err);
      return null;
    }
  }, []);

  /**
   * Fetch routing rule details from REST API
   */
  const fetchRoutingRule = useCallback(async (ruleId: string): Promise<RoutingRuleInfo | null> => {
    try {
      const data = await restClient.get<RoutingRuleInfo>(
        `/service/rest/v1/routing-rules/${encodeURIComponent(ruleId)}`
      );
      return data || null;
    } catch (err) {
      console.warn('Could not fetch routing rule:', err);
      return null;
    }
  }, []);

  /**
   * Fetch repository metrics
   */
  const fetchMetrics = useCallback(async (): Promise<RepositoryMetrics | null> => {
    try {
      const details = await restClient.get<RepositoryDetail[]>(
        `/service/rest/internal/ui/repositories/details?name=${encodeURIComponent(repositoryName)}`
      );
      const detail = Array.isArray(details) ? details.find((r) => r.name === repositoryName) : null;
      
      let totalSize = detail?.size;
      let assetCount = detail?.assetCount;
      let componentCount = detail?.componentCount;

      return {
        componentCount: componentCount || 0,
        assetCount: assetCount || 0,
        totalSize: totalSize || 0,
        downloadsPerMonth: 0,
        uploadsPerMonth: 0,
      };
    } catch (err) {
      console.warn('Could not fetch repository metrics:', err);
      return { componentCount: 0, assetCount: 0, totalSize: 0, downloadsPerMonth: 0, uploadsPerMonth: 0 };
    }
  }, [repositoryName]);

  /**
   * Fetch health check status
   */
  const fetchHealthCheck = useCallback(async (): Promise<HealthCheckData | null> => {
    try {
      const raw = await restClient.get<{
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
      }>(`/service/rest/internal/ui/healthcheck/${encodeURIComponent(repositoryName)}`);
      if (!raw) return null;
      return {
        enabled: raw.enabled,
        analyzing: raw.analyzing,
        securityIssueCount: raw.securityIssueCount,
        licenseIssueCount: raw.licenseIssueCount,
        malwareCount: raw.malwareCount,
        vulnerableCounts: raw.vulnerableCounts,
        totalCounts: raw.totalCounts,
        summaryUrl: raw.summaryUrl,
        detailUrl: raw.detailUrl,
        lastAnalyzedDate: raw.lastAnalyzedDate,
      };
    } catch (err) {
      return null;
    }
  }, [repositoryName]);

  /**
   * Fetch firewall/IQ status for this repo via the per-repo endpoint.
   * Makes at most one IQ Server call instead of fetching all repos.
   */
  const fetchFirewall = useCallback(async (): Promise<FirewallData | null> => {
    try {
      const match = await restClient.get<{
        repositoryName: string;
        affectedComponentCount?: number;
        criticalComponentCount?: number;
        severeComponentCount?: number;
        moderateComponentCount?: number;
        quarantinedComponentCount?: number;
        reportUrl?: string;
        message?: string;
        errorMessage?: string;
        enabled?: boolean;
        quarantineEnabled?: boolean;
      }>(`/service/rest/internal/ui/firewall/status/repo/${encodeURIComponent(repositoryName)}`);

      if (!match) return null;

      const msg = match.message?.toLowerCase() ?? '';
      const isQuarantine = msg.includes('quarantine') || match.quarantineEnabled === true;
      const isAudit = msg.includes('audit');
      const isActive = isQuarantine || isAudit || msg.includes('enabled') || match.enabled === true;

      return {
        enabled: isActive,
        quarantineEnabled: isQuarantine,
        affectedComponentCount: match.affectedComponentCount,
        criticalComponentCount: match.criticalComponentCount,
        severeComponentCount: match.severeComponentCount,
        moderateComponentCount: match.moderateComponentCount,
        quarantinedComponentCount: match.quarantinedComponentCount,
        reportUrl: match.reportUrl,
        message: match.message,
        errorMessage: match.errorMessage,
      };
    } catch (err) {
      console.warn('Could not fetch firewall status for', repositoryName, err);
      return null;
    }
  }, [repositoryName]);

  /**
   * Fetch malware cleanup summary.
   * Tries the dedicated summary endpoint first; if unavailable (404),
   * derives basic task-enabled state from the standard Tasks API.
   */
  const fetchMalwareCleanupSummary = useCallback(async (): Promise<MalwareCleanupSummary | null> => {
    try {
      const summary = await restClient.get<MalwareCleanupSummary>(
        `/service/rest/internal/ui/iq/malware-cleanup/summary/${encodeURIComponent(repositoryName)}`
      );
      if (summary && summary.taskCleanupEnabled === undefined) {
        try {
          const tasksResponse = await restClient.get<{ items: Array<{
            enabled: boolean; properties?: Record<string, string>;
          }> }>('/service/rest/v1/tasks?type=malware.remediator');
          const task = tasksResponse?.items?.find(t =>
            t.properties?.repositoryName === repositoryName ||
            t.properties?.repositoryName === 'all'
          );
          summary.taskCleanupEnabled = task?.properties?.enableMalwareCleanup === 'true';
        } catch {
          summary.taskCleanupEnabled = true;
        }
      }
      return summary;
    } catch {
      // Fallback: derive from the Tasks API
      try {
        const tasksResponse = await restClient.get<{ items: Array<{
          id: string; name: string; enabled: boolean; schedule?: string;
          lastRun?: string; lastRunResult?: string;
          properties?: Record<string, string>;
        }> }>('/service/rest/v1/tasks?type=malware.remediator');
        const task = tasksResponse?.items?.find(t =>
          t.properties?.repositoryName === repositoryName ||
          t.properties?.repositoryName === 'all'
        );
        if (task) {
          return {
            repositoryName,
            scrubbedCount: 0,
            pendingCount: 0,
            lastRun: task.lastRun ?? undefined,
            taskStatus: task.enabled ? 'RUNNING' : 'DISABLED',
            taskEnabled: task.enabled,
            taskCleanupEnabled: task.properties?.enableMalwareCleanup === 'true',
          };
        }
      } catch {
        // Tasks API also unavailable
      }
      return null;
    }
  }, [repositoryName]);

  /**
   * Fetch IQ capabilities
   */
  const fetchIqCapabilities = useCallback(async (): Promise<IqCapabilities | null> => {
    try {
      return await restClient.get<IqCapabilities>('/service/rest/v1/iq/capabilities');
    } catch (err) {
      return null;
    }
  }, []);

  /**
   * Fetch privileges related to this repository
   */
  const fetchPrivileges = useCallback(async (): Promise<PrivilegeInfo[]> => {
    try {
      const allPrivileges = await restClient.get<PrivilegeInfo[]>(PRIVILEGES_URL) || [];
      return allPrivileges.filter((p) => {
        if (p.repository === repositoryName) {
          return true;
        }
        const repoNameLower = repositoryName.toLowerCase();
        const privNameLower = p.name.toLowerCase();
        if (privNameLower.includes(`-${repoNameLower}-`) ||
            privNameLower.includes(`-${repoNameLower}*`) ||
            privNameLower.endsWith(`-${repoNameLower}`)) {
          return true;
        }
        if (p.repository === '*' && p.format === repository?.format) {
          return true;
        }
        return false;
      });
    } catch (err) {
      console.warn('Could not fetch privileges:', err);
      return [];
    }
  }, [repositoryName, repository?.format]);

  /**
   * Fetch roles containing specified privileges
   */
  const fetchRoles = useCallback(async (privs: PrivilegeInfo[]): Promise<RoleInfo[]> => {
    try {
      const allRoles = await restClient.get<RoleInfo[]>(ROLES_URL) || [];
      const privNames = privs.map((p) => p.name);
      return allRoles.filter((r) =>
        r.privileges.some((rp) => privNames.includes(rp))
      );
    } catch (err) {
      console.warn('Could not fetch roles:', err);
      return [];
    }
  }, []);

  /**
   * Fetch users assigned specific roles
   */
  const fetchUsers = useCallback(async (matchedRoles: RoleInfo[]): Promise<UserWithAccess[]> => {
    try {
      const allUsers = await restClient.get<UserWithAccess[]>(USERS_URL) || [];
      const roleIds = matchedRoles.map((r) => r.id);
      return allUsers.filter((u) =>
        u.roles?.some((ur) => roleIds.includes(ur))
      );
    } catch (err) {
      console.warn('Could not fetch users:', err);
      return [];
    }
  }, []);

  const fetchAnonymousAccess = useCallback(async (): Promise<AnonymousAccess | null> => {
    try {
      return await restClient.get<AnonymousAccess>(ANONYMOUS_URL);
    } catch (err) {
      console.warn('Could not fetch anonymous access:', err);
      return null;
    }
  }, []);

  const fetchTasks = useCallback(async (): Promise<TaskInfo[]> => {
    try {
      const data = await restClient.get<{ items: any[] }>(TASKS_URL);
      const allTasks = data?.items || [];
      return allTasks
        .filter((t) => t.properties?.repositoryName === repositoryName)
        .map((t) => ({
          id: t.id,
          name: t.name,
          type: t.typeId,
          schedule: t.schedule,
          lastRun: t.lastRun,
          lastRunResult: t.lastRunResult,
          nextRun: t.nextRun,
          currentState: t.currentState,
        }));
    } catch (err) {
      console.warn('Could not fetch tasks:', err);
      return [];
    }
  }, [repositoryName]);

  const fetchCapabilities = useCallback(async (): Promise<CapabilityInfo[]> => {
    try {
      const data = await restClient.get<CapabilityInfo[]>(CAPABILITIES_URL);
      const allCapabilities: CapabilityInfo[] = Array.isArray(data) ? data : [];
      return allCapabilities.filter((c) => {
        if (c.properties?.repository === repositoryName || c.properties?.repositoryName === repositoryName) {
          return true;
        }
        if (!c.properties?.repository && !c.properties?.repositoryName) {
          return true;
        }
        return false;
      });
    } catch (err) {
      console.warn('Could not fetch capabilities:', err);
      return [];
    }
  }, [repositoryName]);

  const fetchHttpSettings = useCallback(async (): Promise<HttpSettingsInfo | null> => {
    try {
      return await restClient.get<HttpSettingsInfo>(HTTP_URL);
    } catch (err) {
      console.warn('Could not fetch http settings:', err);
      return null;
    }
  }, []);

  const loadCoreData = useCallback(async () => {
    setLoading(true);
    try {
      const repoData = await fetchRepository();
      setRepository(repoData);

      if (repoData) {
        const [bsData, mData, hcData, fwData, mwSumData, iqCapData] = await Promise.all([
          repoData.attributes?.storage?.blobStoreName ? fetchBlobStore(repoData.attributes.storage.blobStoreName) : Promise.resolve(null),
          fetchMetrics(),
          fetchHealthCheck(),
          fetchFirewall(),
          fetchMalwareCleanupSummary(),
          fetchIqCapabilities(),
        ]);

        setBlobStore(bsData);
        setMetrics(mData);
        setHealthCheck(hcData);
        setFirewall(fwData);
        setMalwareCleanupSummary(mwSumData);
        setIqCapabilities(iqCapData);

        if (repoData.routingRuleId) {
          const rrData = await fetchRoutingRule(repoData.routingRuleId);
          setRoutingRule(rrData);
        }
      }
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setLoading(false);
    }
  }, [fetchRepository, fetchBlobStore, fetchMetrics, fetchHealthCheck, fetchFirewall, fetchMalwareCleanupSummary, fetchIqCapabilities, fetchRoutingRule]);

  const loadSecurityData = useCallback(async () => {
    setSecurityLoading(true);
    try {
      const [privilegesData, anonymousData] = await Promise.all([
        fetchPrivileges(),
        fetchAnonymousAccess(),
      ]);

      setPrivileges(privilegesData);
      setAnonymousAccess(anonymousData);

      const rolesData = await fetchRoles(privilegesData);
      setRoles(rolesData);

      const usersData = await fetchUsers(rolesData);
      setUsers(usersData);
    } catch (err) {
      console.error('Failed to load security data:', err);
    } finally {
      setSecurityLoading(false);
    }
  }, [fetchPrivileges, fetchRoles, fetchUsers, fetchAnonymousAccess]);

  const loadSystemData = useCallback(async () => {
    setSystemLoading(true);
    try {
      const [tasksData, capabilitiesData, httpData] = await Promise.all([
        fetchTasks(),
        fetchCapabilities(),
        fetchHttpSettings(),
      ]);

      setTasks(tasksData);
      setCapabilities(capabilitiesData);
      setHttpSettings(httpData);
    } catch (err) {
      console.error('Failed to load system data:', err);
    } finally {
      setSystemLoading(false);
    }
  }, [fetchTasks, fetchCapabilities, fetchHttpSettings]);

  const loadAllData = useCallback(async () => {
    await loadCoreData();
    await Promise.all([loadSecurityData(), loadSystemData()]);
  }, [loadCoreData, loadSecurityData, loadSystemData]);

  useEffect(() => {
    loadAllData();
  }, [loadAllData]);

  return {
    repository,
    blobStore,
    cleanupPolicies,
    routingRule,
    healthCheck,
    firewall,
    malwareCleanupSummary,
    iqMapping,
    iqCapabilities,
    metrics,
    privileges,
    roles,
    users,
    anonymousAccess,
    tasks,
    capabilities,
    httpSettings,
    loading,
    securityLoading,
    systemLoading,
    error,
    refresh: loadAllData,
  };
}

export default useRepositoryProfile;
