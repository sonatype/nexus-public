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

import { createMachine, assign } from 'xstate';

import { restClient, parseApiError, ENDPOINTS } from '../../../../../../interface/api';
import type {
  RepositoryProfileData,
  BlobStoreInfo,
  CleanupPolicyInfo,
  RoutingRuleInfo,
  HealthCheckData,
  FirewallData,
  MalwareCleanupSummary,
  IqCapabilities,
  RepositoryMetrics,
  PrivilegeInfo,
  RoleInfo,
  UserWithAccess,
  AnonymousAccess,
  TaskInfo,
  CapabilityInfo,
  HttpSettingsInfo,
} from './types';

// =============================================================================
// Context
// =============================================================================

export interface RepositoryProfileContext {
  // Identity
  repositoryName: string;

  // Repository data
  repository: RepositoryProfileData | null;
  blobStore: BlobStoreInfo | null;
  cleanupPolicies: CleanupPolicyInfo[];
  routingRule: RoutingRuleInfo | null;

  // IQ Server data
  healthCheck: HealthCheckData | null;
  firewall: FirewallData | null;
  malwareCleanupSummary: MalwareCleanupSummary | null;
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

  // Action error — persists in context so the UI can display it after returning to loaded
  actionError: string | null;

  // Error state
  loadError: string | null;
}

// =============================================================================
// Events
// =============================================================================

export type RepositoryProfileEvent =
  // Data loading
  | { type: 'LOAD' }
  | { type: 'REFRESH' }
  // Actions requiring confirmation
  | { type: 'INVALIDATE_CACHE' }
  | { type: 'REBUILD_INDEX' }
  | { type: 'TOGGLE_ONLINE' }
  // Immediate actions (no confirmation needed)
  | { type: 'TOGGLE_HEALTH_CHECK'; enabled: boolean }
  | { type: 'TOGGLE_INSTANCE_HEALTH_CHECK'; enabled: boolean; useTrustStore: boolean }
  // Confirmation flow
  | { type: 'CONFIRM' }
  | { type: 'CANCEL' };

// =============================================================================
// API Constants
// =============================================================================

const BLOB_STORES_INTERNAL_URL = '/service/rest/internal/ui/blobstores';
const PRIVILEGES_URL = '/service/rest/v1/security/privileges';
const ROLES_URL = '/service/rest/v1/security/roles';
const USERS_URL = '/service/rest/v1/security/users';
const ANONYMOUS_URL = '/service/rest/v1/security/anonymous';
const TASKS_URL = '/service/rest/v1/tasks';
const CAPABILITIES_URL = '/service/rest/v1/capabilities';
const HTTP_URL = '/service/rest/v1/http';

// =============================================================================
// API Helpers
// =============================================================================

interface RepositoryDetail {
  name: string;
  format: string;
  type: string;
  online: boolean;
  componentCount?: number;
  assetCount?: number;
  size?: number;
}

async function fetchRepository(repositoryName: string): Promise<RepositoryProfileData | null> {
  try {
    const fullConfig = await restClient.get<RepositoryProfileData & { storage?: { blobStoreName?: string } }>(
      `/service/rest/internal/ui/repositories/repository/${encodeURIComponent(repositoryName)}`
    );
    if (!fullConfig) {
      return null;
    }

    const internal = fullConfig as RepositoryProfileData & {
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
}

async function fetchBlobStore(blobStoreName: string): Promise<BlobStoreInfo | null> {
  try {
    const data = await restClient.get<unknown[]>(BLOB_STORES_INTERNAL_URL);
    const match = Array.isArray(data) ? data.find((b: any) => b.name === blobStoreName) : null;
    if (!match) {
      return null;
    }
    const bs = match as Record<string, unknown>;
    return {
      name: String(bs.name),
      type: String(bs.typeName ?? bs.typeId ?? 'Unknown'),
      path: bs.path ? String(bs.path) : undefined,
      unavailable: Boolean(bs.unavailable),
      totalSizeInBytes: bs.totalSizeInBytes ? Number(bs.totalSizeInBytes) : undefined,
      availableSpaceInBytes: bs.availableSpaceInBytes ? Number(bs.availableSpaceInBytes) : undefined,
      blobCount: bs.blobCount ? Number(bs.blobCount) : undefined,
    };
  } catch (err) {
    console.warn('Could not fetch blob store details:', err);
    return null;
  }
}

async function fetchRoutingRule(ruleId: string): Promise<RoutingRuleInfo | null> {
  try {
    const data = await restClient.get<RoutingRuleInfo>(
      `/service/rest/v1/routing-rules/${encodeURIComponent(ruleId)}`
    );
    return data || null;
  } catch (err) {
    console.warn('Could not fetch routing rule:', err);
    return null;
  }
}

async function fetchMetrics(repositoryName: string): Promise<RepositoryMetrics> {
  try {
    const details = await restClient.get<RepositoryDetail[]>(
      `/service/rest/internal/ui/repositories/details?name=${encodeURIComponent(repositoryName)}`
    );
    const detail = Array.isArray(details) ? details.find((r) => r.name === repositoryName) : null;

    return {
      componentCount: detail?.componentCount || 0,
      assetCount: detail?.assetCount || 0,
      totalSize: detail?.size || 0,
      downloadsPerMonth: 0,
      uploadsPerMonth: 0,
    };
  } catch (err) {
    console.warn('Could not fetch repository metrics:', err);
    return { componentCount: 0, assetCount: 0, totalSize: 0, downloadsPerMonth: 0, uploadsPerMonth: 0 };
  }
}

async function fetchHealthCheck(repositoryName: string): Promise<HealthCheckData | null> {
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
  } catch {
    return null;
  }
}

async function fetchFirewall(repositoryName: string): Promise<FirewallData | null> {
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
}

async function fetchMalwareCleanupSummary(repositoryName: string): Promise<MalwareCleanupSummary | null> {
  try {
    const summary = await restClient.get<MalwareCleanupSummary>(
      `/service/rest/internal/ui/iq/malware-cleanup/summary/${encodeURIComponent(repositoryName)}`
    );
    if (summary && summary.taskCleanupEnabled === undefined) {
      try {
        const tasksResponse = await restClient.get<{ items: Array<{
          enabled: boolean;
          properties?: Record<string, string>;
        }> }>('/service/rest/v1/tasks?type=malware.remediator');
        const task = tasksResponse?.items?.find((t) =>
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
        id: string;
        name: string;
        enabled: boolean;
        schedule?: string;
        lastRun?: string;
        lastRunResult?: string;
        properties?: Record<string, string>;
      }> }>('/service/rest/v1/tasks?type=malware.remediator');
      const task = tasksResponse?.items?.find((t) =>
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
}

async function fetchIqCapabilities(): Promise<IqCapabilities | null> {
  try {
    return await restClient.get<IqCapabilities>('/service/rest/v1/iq/capabilities');
  } catch {
    return null;
  }
}

async function fetchPrivileges(repositoryName: string, repositoryFormat?: string): Promise<PrivilegeInfo[]> {
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
      if (p.repository === '*' && p.format === repositoryFormat) {
        return true;
      }
      return false;
    });
  } catch (err) {
    console.warn('Could not fetch privileges:', err);
    return [];
  }
}

async function fetchRoles(privs: PrivilegeInfo[]): Promise<RoleInfo[]> {
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
}

async function fetchUsers(matchedRoles: RoleInfo[]): Promise<UserWithAccess[]> {
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
}

async function fetchAnonymousAccess(): Promise<AnonymousAccess | null> {
  try {
    return await restClient.get<AnonymousAccess>(ANONYMOUS_URL);
  } catch (err) {
    console.warn('Could not fetch anonymous access:', err);
    return null;
  }
}

async function fetchTasks(repositoryName: string): Promise<TaskInfo[]> {
  try {
    const data = await restClient.get<{ items: unknown[] }>(TASKS_URL);
    const allTasks = data?.items || [];
    return allTasks
      .filter((t: any) => t.properties?.repositoryName === repositoryName)
      .map((t: any) => ({
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
}

async function fetchCapabilities(repositoryName: string): Promise<CapabilityInfo[]> {
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
}

async function fetchHttpSettings(): Promise<HttpSettingsInfo | null> {
  try {
    return await restClient.get<HttpSettingsInfo>(HTTP_URL);
  } catch (err) {
    console.warn('Could not fetch http settings:', err);
    return null;
  }
}

// =============================================================================
// Shared action error assignment
// =============================================================================

const setActionError = assign({
  actionError: (_: RepositoryProfileContext, event: any) =>
    parseApiError(event.data as Error).message,
});

// =============================================================================
// Machine Factory
// =============================================================================

export interface RepositoryProfileMachineOptions {
  repositoryName: string;
}

/**
 * Create a repository profile machine with XState.
 *
 * Manages:
 * - Data loading (core, security, system in parallel)
 * - Action execution with confirmation dialogs
 * - Operation locking (prevent concurrent operations)
 *
 * Loading phases (securityLoading, systemLoading) are tracked by the parallel
 * sub-states `loading.security` and `loading.system` rather than boolean flags
 * in context. Use `state.matches({ loading: { security: 'fetching' } })` instead
 * of `context.securityLoading`.
 *
 * Actions that require confirmation each have their own confirming/executing
 * state pair (e.g. `confirmingInvalidateCache` / `executingInvalidateCache`),
 * removing the need for a `pendingAction` context field.
 */
export function createRepositoryProfileMachine(options: RepositoryProfileMachineOptions) {
  const { repositoryName } = options;

  return createMachine<RepositoryProfileContext, RepositoryProfileEvent>(
    {
      id: `repository-profile-${repositoryName}`,
      initial: 'loading',
      context: {
        repositoryName,
        repository: null,
        blobStore: null,
        cleanupPolicies: [],
        routingRule: null,
        healthCheck: null,
        firewall: null,
        malwareCleanupSummary: null,
        iqCapabilities: null,
        metrics: null,
        privileges: [],
        roles: [],
        users: [],
        anonymousAccess: null,
        tasks: [],
        capabilities: [],
        httpSettings: null,
        actionError: null,
        loadError: null,
      },

      states: {
        // ========================================
        // Loading State (parallel phases)
        //
        // Check loading status via state.matches:
        //   security loading: state.matches({ loading: { security: 'fetching' } })
        //   system loading:   state.matches({ loading: { system: 'fetching' } })
        // ========================================
        loading: {
          type: 'parallel',
          states: {
            core: {
              initial: 'fetching',
              states: {
                fetching: {
                  invoke: {
                    src: 'loadCoreData',
                    onDone: {
                      target: 'done',
                      actions: 'setCoreData',
                    },
                    onError: {
                      target: 'done',
                      actions: 'setLoadError',
                    },
                  },
                },
                done: { type: 'final' },
              },
            },
            security: {
              initial: 'fetching',
              states: {
                fetching: {
                  invoke: {
                    src: 'loadSecurityData',
                    onDone: {
                      target: 'done',
                      actions: 'setSecurityData',
                    },
                    onError: {
                      target: 'done',
                    },
                  },
                },
                done: { type: 'final' },
              },
            },
            system: {
              initial: 'fetching',
              states: {
                fetching: {
                  invoke: {
                    src: 'loadSystemData',
                    onDone: {
                      target: 'done',
                      actions: 'setSystemData',
                    },
                    onError: {
                      target: 'done',
                    },
                  },
                },
                done: { type: 'final' },
              },
            },
          },
          onDone: 'loaded',
        },

        // ========================================
        // Loaded State
        // ========================================
        loaded: {
          // Clear any prior action error when we re-enter loaded via a new action
          // (exit fires before the next state is entered)
          exit: assign({ actionError: null }),
          on: {
            REFRESH: 'loading',
            INVALIDATE_CACHE: 'confirmingInvalidateCache',
            REBUILD_INDEX: 'confirmingRebuildIndex',
            TOGGLE_ONLINE: 'confirmingToggleOnline',
            TOGGLE_HEALTH_CHECK: 'executingToggleHealthCheck',
            TOGGLE_INSTANCE_HEALTH_CHECK: 'executingToggleInstanceHealthCheck',
          },
        },

        // ========================================
        // Confirming States
        // ========================================
        confirmingInvalidateCache: {
          on: {
            CONFIRM: 'executingInvalidateCache',
            CANCEL: 'loaded',
          },
        },

        confirmingRebuildIndex: {
          on: {
            CONFIRM: 'executingRebuildIndex',
            CANCEL: 'loaded',
          },
        },

        confirmingToggleOnline: {
          on: {
            CONFIRM: 'executingToggleOnline',
            CANCEL: 'loaded',
          },
        },

        // ========================================
        // Executing States
        // ========================================
        executingInvalidateCache: {
          invoke: {
            src: 'executeInvalidateCache',
            onDone: { target: 'loaded' },
            onError: { target: 'loaded', actions: setActionError },
          },
        },

        executingRebuildIndex: {
          invoke: {
            src: 'executeRebuildIndex',
            onDone: { target: 'loaded' },
            onError: { target: 'loaded', actions: setActionError },
          },
        },

        executingToggleOnline: {
          invoke: {
            src: 'executeToggleOnline',
            // Reload after a successful toggle so repository.online, status.online,
            // status.description, and any other server-derived state are fresh.
            onDone: { target: 'loading' },
            onError: { target: 'loaded', actions: setActionError },
          },
        },

        executingToggleHealthCheck: {
          invoke: {
            src: 'executeToggleHealthCheck',
            // Reload after a successful toggle so healthCheck data is fresh (mirrors executeToggleOnline).
            // Without this, the UI stays stuck on the pre-toggle state (e.g., "Analyzing…" after disabling).
            onDone: { target: 'loading' },
            onError: { target: 'loaded', actions: setActionError },
          },
        },

        executingToggleInstanceHealthCheck: {
          invoke: {
            src: 'executeToggleInstanceHealthCheck',
            // Reload after instance capability toggle so context.capabilities is fresh.
            // Without this, the card stays stuck on the pre-toggle state (e.g., still
            // showing "Enabled" after the user disables the instance capability).
            onDone: { target: 'loading' },
            onError: { target: 'loaded', actions: setActionError },
          },
        },

        // Note: load errors are stored in context.loadError and surfaced via the
        // hook's `error` field. All parallel loading phases gracefully degrade to
        // `done` on failure so the machine always reaches `loaded`; there is no
        // separate error state to transition to.
      },
    },
    {
      services: {
        loadCoreData: async (context) => {
          const repoData = await fetchRepository(context.repositoryName);

          let blobStore: BlobStoreInfo | null = null;
          let routingRule: RoutingRuleInfo | null = null;

          if (repoData) {
            const blobStoreName = repoData.attributes?.storage?.blobStoreName;
            if (blobStoreName) {
              blobStore = await fetchBlobStore(blobStoreName);
            }
            if (repoData.routingRuleId) {
              routingRule = await fetchRoutingRule(repoData.routingRuleId);
            }
          }

          const [metricsData, healthCheckData, firewallData, malwareSummaryData, iqCapData] = await Promise.all([
            fetchMetrics(context.repositoryName),
            fetchHealthCheck(context.repositoryName),
            fetchFirewall(context.repositoryName),
            fetchMalwareCleanupSummary(context.repositoryName),
            fetchIqCapabilities(),
          ]);

          return {
            repository: repoData,
            blobStore,
            routingRule,
            metrics: metricsData,
            healthCheck: healthCheckData,
            firewall: firewallData,
            malwareCleanupSummary: malwareSummaryData,
            iqCapabilities: iqCapData,
          };
        },

        loadSecurityData: async (context) => {
          const privilegesData = await fetchPrivileges(context.repositoryName, context.repository?.format);
          const rolesData = await fetchRoles(privilegesData);
          const usersData = await fetchUsers(rolesData);
          const anonymousData = await fetchAnonymousAccess();

          return {
            privileges: privilegesData,
            roles: rolesData,
            users: usersData,
            anonymousAccess: anonymousData,
          };
        },

        loadSystemData: async (context) => {
          const [tasksData, capabilitiesData, httpData] = await Promise.all([
            fetchTasks(context.repositoryName),
            fetchCapabilities(context.repositoryName),
            fetchHttpSettings(),
          ]);

          return {
            tasks: tasksData,
            capabilities: capabilitiesData,
            httpSettings: httpData,
          };
        },

        executeInvalidateCache: async (context) => {
          await restClient.post(
            `/service/rest/v1/repositories/${encodeURIComponent(context.repositoryName)}/invalidate-cache`
          );
        },

        executeRebuildIndex: async (context) => {
          await restClient.post(
            `/service/rest/v1/repositories/${encodeURIComponent(context.repositoryName)}/rebuild-index`
          );
        },

        executeToggleOnline: async (context) => {
          if (!context.repository) {
            throw new Error('Repository not loaded');
          }
          const { format, type, repositoryName } = {
            format: context.repository.format,
            type: context.repository.type,
            repositoryName: context.repositoryName,
          };
          // Fetch the full format-specific config from the public v1 GET endpoint so the
          // subsequent PUT receives the complete payload it requires. Spreading
          // context.repository is wrong here because it is built from the internal UI
          // endpoint and contains internal-only fields (status, url, recipe, attributes,
          // routingRuleId) that the public v1 PUT does not accept.
          const fullConfig = await restClient.get<Record<string, unknown>>(
            `/service/rest/v1/repositories/${encodeURIComponent(format)}/${encodeURIComponent(type)}/${encodeURIComponent(repositoryName)}`
          );
          if (!fullConfig) {
            throw new Error('Could not fetch repository configuration for update');
          }
          await restClient.put(
            `/service/rest/v1/repositories/${encodeURIComponent(format)}/${encodeURIComponent(type)}/${encodeURIComponent(repositoryName)}`,
            { ...fullConfig, online: !context.repository.online }
          );
        },

        executeToggleHealthCheck: async (context, event) => {
          const { enabled } = event as Extract<RepositoryProfileEvent, { type: 'TOGGLE_HEALTH_CHECK' }>;
          const url = `/service/rest/v1/repositories/${encodeURIComponent(context.repositoryName)}/health-check`;
          try {
            if (enabled) {
              await restClient.post(url);
            } else {
              await restClient.delete(url);
            }
          } catch (err) {
            const apiError = parseApiError(err);
            if (apiError.status === 405 && enabled) {
              // Backend returns 405 when a repository was auto-enabled by the instance-level
              // "configured for all" capability and was then explicitly disabled. The server
              // does not support re-enabling an individually-disabled repo via POST in this
              // state. Workaround: toggle the instance-level Health Check capability off then
              // back on, which resets the per-repository override.
              throw new Error(
                'Health Check cannot be re-enabled at the repository level when it was automatically ' +
                'configured by the instance capability. To re-enable, go to System Capabilities and ' +
                'toggle the Health Check capability off then on again.'
              );
            }
            throw new Error(apiError.message);
          }
        },

        executeToggleInstanceHealthCheck: async (context, event) => {
          const { enabled, useTrustStore } = event as Extract<
            RepositoryProfileEvent,
            { type: 'TOGGLE_INSTANCE_HEALTH_CHECK' }
          >;
          const healthCheckCapability = context.capabilities.find((c) => c.type === 'healthcheck');
          if (healthCheckCapability) {
            await restClient.put(`${ENDPOINTS.CAPABILITIES}/${healthCheckCapability.id}`, {
              enabled,
              properties: {
                ...healthCheckCapability.properties,
                useTrustStore: String(useTrustStore ?? false),
              },
            });
          } else if (enabled) {
            await restClient.post(ENDPOINTS.CAPABILITIES, {
              type: 'healthcheck',
              enabled: true,
              properties: {
                configuredForAll: 'true',
                useTrustStore: String(useTrustStore ?? false),
              },
            });
          }
        },
      },

      actions: {
        setCoreData: assign((_, event) => {
          const data = (event as any).data;
          return {
            repository: data.repository,
            blobStore: data.blobStore,
            routingRule: data.routingRule,
            metrics: data.metrics,
            healthCheck: data.healthCheck,
            firewall: data.firewall,
            malwareCleanupSummary: data.malwareCleanupSummary,
            iqCapabilities: data.iqCapabilities,
          };
        }),

        setSecurityData: assign((_, event) => {
          const data = (event as any).data;
          return {
            privileges: data.privileges,
            roles: data.roles,
            users: data.users,
            anonymousAccess: data.anonymousAccess,
          };
        }),

        setSystemData: assign((_, event) => {
          const data = (event as any).data;
          return {
            tasks: data.tasks,
            capabilities: data.capabilities,
            httpSettings: data.httpSettings,
          };
        }),

        setLoadError: assign({
          loadError: (_, event) => {
            const err = (event as any).data;
            return parseApiError(err).message;
          },
        }),
      },
    }
  );
}
