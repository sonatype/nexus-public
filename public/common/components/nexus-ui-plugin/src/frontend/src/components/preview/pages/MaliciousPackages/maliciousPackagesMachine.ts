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

import { assign, createMachine } from 'xstate';

import { restClient, ENDPOINTS } from '../../../../interface/api';
import { isHealthCheckSupportedFormat } from '../../../../utils/healthCheckFormats';
import { MaliciousFinding, MaliciousPackagesState } from './types';
import {
  fetchMalwareRemediatorTasks,
  deriveMalwareRemediatorMode,
  type MalwareRemediatorTaskListItem,
} from '../../shared/security/malwareRemediatorTask';
import {
  findingsFingerprint,
  isSchedulerTaskRunningState,
  parseTaskCurrentState,
  proxyReposEqual,
  shallowEqualRecord,
  shallowEqualStringArray,
  type BulkProgress,
  type ProxyRepo,
  type RhcScanInfo,
  type TaskInfo,
} from './maliciousPackagesUtils';

// =============================================================================
// REST response shapes (kept local — only the machine consumes them)
// =============================================================================

interface MalwareCountsResponse {
  totalCount: number;
  counts: Record<string, number>;
  hdsAvailable: boolean;
  hcEnabledRepos: string[];
  detectInitiatedRepos: Record<string, number>;
}

interface IqCapabilitiesResponse {
  connected: boolean;
  hasFirewall: boolean;
  hasLifecycle?: boolean;
  url?: string;
  deploymentId?: string;
}

interface RepositoryRef {
  name: string;
  type: string;
  format: string;
}

export interface HealthCheckSummaryItem {
  repositoryName: string;
  enabled: boolean;
  analyzing: boolean;
  lastAnalyzedDate?: number | null;
  malwareCount?: number;
}

interface FetchAllResult {
  data: MaliciousPackagesState;
  proxyRepos: ProxyRepo[];
  tasks: TaskInfo[];
  serverRhcScans: Map<string, RhcScanInfo>;
}

// =============================================================================
// Poll interval policy
// =============================================================================

const POLL_INTERVAL_FAST_MS = 5_000;
const POLL_INTERVAL_MEDIUM_MS = 15_000;
const POLL_INTERVAL_SLOW_MS = 30_000;

function selectPollIntervalMs(tasks: TaskInfo[], rhcScans: Map<string, RhcScanInfo>): number {
  const anyRunning = tasks.some((t) => isSchedulerTaskRunningState(t.currentState));
  if (anyRunning) return POLL_INTERVAL_FAST_MS;
  for (const info of rhcScans.values()) {
    if (info.phase === 'scanning') return POLL_INTERVAL_MEDIUM_MS;
  }
  return POLL_INTERVAL_SLOW_MS;
}

// =============================================================================
// RHC scan derivation (lifted from the previous hook)
// =============================================================================

export const RHC_STUCK_THRESHOLD_MS = 5 * 60 * 1000;
export const RHC_STUCK_ERROR =
  'Repository Health Check could not be enabled — check Administration > System > Tasks for details';

export function deriveServerRhcScans(
  hcSummary: HealthCheckSummaryItem[],
  detectInitiated: Record<string, number>,
  counts: Record<string, number>,
  hcEnabledRepos: string[],
): Map<string, RhcScanInfo> {
  const map = new Map<string, RhcScanInfo>();
  const hcByName = new Map(hcSummary.map((item) => [item.repositoryName, item]));
  const enabledSet = new Set(hcEnabledRepos);

  for (const [repoName, initiatedAt] of Object.entries(detectInitiated)) {
    const hc = hcByName.get(repoName);
    if (hc?.analyzing) {
      map.set(repoName, { phase: 'scanning', startedAt: initiatedAt });
    } else if (hc?.enabled && hc.lastAnalyzedDate) {
      map.set(repoName, {
        phase: 'completed',
        startedAt: initiatedAt,
        completedAt: hc.lastAnalyzedDate,
        signatureCount: counts[repoName] ?? hc.malwareCount ?? 0,
      });
    } else if (!enabledSet.has(repoName) && Date.now() - initiatedAt > RHC_STUCK_THRESHOLD_MS) {
      map.set(repoName, {
        phase: 'failed',
        startedAt: initiatedAt,
        completedAt: Date.now(),
        error: RHC_STUCK_ERROR,
      });
    } else {
      map.set(repoName, { phase: 'scanning', startedAt: initiatedAt });
    }
  }
  return map;
}

// =============================================================================
// Tasks normalization
// =============================================================================

function toTaskInfo(raw: MalwareRemediatorTaskListItem): TaskInfo {
  const parsed = parseTaskCurrentState(raw.currentState ?? null);
  return {
    id: raw.id,
    name: raw.name,
    repositoryName: raw.properties?.repositoryName ?? 'unknown',
    mode: deriveMalwareRemediatorMode(raw),
    enabled: raw.enabled,
    lastRun: raw.lastRun ?? null,
    lastRunResult: raw.lastRunResult ?? null,
    nextRun: raw.nextRun ?? null,
    currentState: parsed.state,
    progress: parsed.progress,
  };
}

export function normalizeTasksResponse(
  raw: readonly MalwareRemediatorTaskListItem[],
): TaskInfo[] {
  return raw.map(toTaskInfo);
}

// =============================================================================
// Services
// =============================================================================

async function fetchAllServiceImpl(): Promise<FetchAllResult> {
  const [activeFindings, malwareCounts, iqCapabilities, repos, hcSummary, rawTasks] =
    await Promise.all([
      // NEXUS-53542: active-findings is unproductized and returns 403 on every edition today.
      restClient.get<MaliciousFinding[]>(ENDPOINTS.MALICIOUS_RISK_ACTIVE_FINDINGS).catch(() => []),
      restClient.get<MalwareCountsResponse>(ENDPOINTS.MALWARE_COUNTS),
      restClient.get<IqCapabilitiesResponse>(ENDPOINTS.IQ_CAPABILITIES).catch(() => null),
      restClient.get<RepositoryRef[]>(ENDPOINTS.REPOSITORIES),
      restClient.get<HealthCheckSummaryItem[]>(ENDPOINTS.HEALTH_CHECK_SUMMARY),
      fetchMalwareRemediatorTasks().catch(() => [] as MalwareRemediatorTaskListItem[]),
    ]);

  const repoList = Array.isArray(repos) ? repos.filter((r) => r.type === 'proxy') : [];
  const enabledSet = new Set(malwareCounts?.hcEnabledRepos ?? []);
  const hcSummaryArr = Array.isArray(hcSummary) ? hcSummary : [];
  const hcSummaryRepoNames = new Set(hcSummaryArr.map((item) => item.repositoryName));

  const proxyRepos: ProxyRepo[] = repoList.map((r) => ({
    name: r.name,
    format: r.format,
    rhcSupported:
      isHealthCheckSupportedFormat(r.format) &&
      (hcSummaryRepoNames.size === 0 || hcSummaryRepoNames.has(r.name)),
    rhcEnabled: enabledSet.has(r.name),
  }));

  const rawCounts = malwareCounts?.counts ?? {};
  const filteredCounts: Record<string, number> = {};
  let filteredTotal = 0;
  for (const [repo, count] of Object.entries(rawCounts)) {
    if (enabledSet.has(repo)) {
      filteredCounts[repo] = count;
      filteredTotal += count;
    }
  }

  const detectInitiated = malwareCounts?.detectInitiatedRepos ?? {};
  const serverRhcScans = deriveServerRhcScans(
    hcSummaryArr,
    detectInitiated,
    filteredCounts,
    malwareCounts?.hcEnabledRepos ?? [],
  );

  const data: MaliciousPackagesState = {
    activeFindings: Array.isArray(activeFindings) ? activeFindings : [],
    historyFindings: [],
    malwareCount: filteredTotal,
    countsByRepo: filteredCounts,
    hasFirewall: !!iqCapabilities?.hasFirewall,
    hcEnabledRepos: malwareCounts?.hcEnabledRepos ?? [],
    totalProxyRepoCount: repoList.length,
    loading: false,
    error: null,
  };

  return {
    data,
    proxyRepos,
    tasks: normalizeTasksResponse(rawTasks),
    serverRhcScans,
  };
}

async function fetchTasksServiceImpl(): Promise<TaskInfo[]> {
  const raw = await fetchMalwareRemediatorTasks();
  return normalizeTasksResponse(raw);
}

// =============================================================================
// Context and events
// =============================================================================

export interface MaliciousPackagesMachineContext {
  /** Persisted public data state surfaced via the hook. */
  data: MaliciousPackagesState;
  proxyRepos: ProxyRepo[];
  tasks: TaskInfo[];
  /**
   * Tracked RHC scans: server-derived scans are merged with optimistic local entries
   * (pending enableRhc starts, failed enableRhc attempts) so the UI shows the right
   * phase without flapping when the server hasn't yet reflected the request.
   */
  rhcScans: Map<string, RhcScanInfo>;
  /** Local optimistic scans the user kicked off via enableRhc, not yet confirmed by server. */
  enablePending: Set<string>;
  bulkProgress: BulkProgress;
  identifyFailures: Map<string, string>;
  tasksLoading: boolean;
  /** Whether tasks were in a running scheduler state at the previous fetch — drives auto-refresh on completion. */
  prevAnyTaskRunning: boolean;
}

export type MaliciousPackagesMachineEvent =
  | { type: 'REFRESH' }
  | { type: 'SET_TASKS'; tasks: TaskInfo[] }
  | { type: 'SET_HISTORY'; history: MaliciousFinding[] }
  | { type: 'SET_BULK_PROGRESS'; bulkProgress: BulkProgress }
  | { type: 'SET_TASKS_LOADING'; loading: boolean }
  | { type: 'RHC_OPTIMISTIC_START'; repoName: string }
  | { type: 'RHC_OPTIMISTIC_CLEAR'; repoName: string }
  | { type: 'RHC_FAILED'; repoName: string; error: string }
  | { type: 'SET_IDENTIFY_FAILURE'; repoName: string; reason: string }
  | { type: 'CLEAR_IDENTIFY_FAILURE'; repoName: string }
  | { type: 'RETRY' };

// =============================================================================
// Initial context
// =============================================================================

export function makeInitialContext(): MaliciousPackagesMachineContext {
  return {
    data: {
      activeFindings: [],
      historyFindings: [],
      malwareCount: 0,
      countsByRepo: {},
      hasFirewall: false,
      hcEnabledRepos: [],
      totalProxyRepoCount: 0,
      loading: true,
      error: null,
    },
    proxyRepos: [],
    tasks: [],
    rhcScans: new Map(),
    enablePending: new Set(),
    bulkProgress: { total: 0, completed: 0, active: false },
    identifyFailures: new Map(),
    tasksLoading: false,
    prevAnyTaskRunning: false,
  };
}

// =============================================================================
// Actions (assigned outside createMachine for reuse and unit-testability)
// =============================================================================

export function mergeRhcScans(
  prev: Map<string, RhcScanInfo>,
  enablePending: Set<string>,
  serverScans: Map<string, RhcScanInfo>,
): Map<string, RhcScanInfo> {
  const merged = new Map(serverScans);
  // Preserve optimistic scanning entries the server hasn't yet reflected
  // and preserve failure entries so the UI doesn't lose error state on the next poll.
  for (const [name, info] of prev) {
    if (info.phase === 'scanning' && !serverScans.has(name) && enablePending.has(name)) {
      merged.set(name, info);
    }
    if (info.phase === 'failed' && !serverScans.has(name)) {
      merged.set(name, info);
    }
  }
  return merged;
}

function mergeFetchedData(
  prev: MaliciousPackagesState,
  next: MaliciousPackagesState,
): MaliciousPackagesState {
  const findingsChanged =
    findingsFingerprint(prev.activeFindings) !== findingsFingerprint(next.activeFindings);
  const countsChanged = !shallowEqualRecord(prev.countsByRepo, next.countsByRepo);
  const hcEnabledChanged = !shallowEqualStringArray(prev.hcEnabledRepos, next.hcEnabledRepos);

  // Preserve referential equality where possible so React consumers don't churn.
  const unchanged =
    !prev.loading &&
    prev.error === null &&
    prev.malwareCount === next.malwareCount &&
    prev.totalProxyRepoCount === next.totalProxyRepoCount &&
    prev.hasFirewall === next.hasFirewall &&
    !countsChanged &&
    !hcEnabledChanged &&
    !findingsChanged;

  if (unchanged) return prev;

  return {
    activeFindings: findingsChanged ? next.activeFindings : prev.activeFindings,
    historyFindings: prev.historyFindings,
    malwareCount: next.malwareCount,
    countsByRepo: countsChanged ? next.countsByRepo : prev.countsByRepo,
    hasFirewall: next.hasFirewall,
    hcEnabledRepos: hcEnabledChanged ? next.hcEnabledRepos : prev.hcEnabledRepos,
    totalProxyRepoCount: next.totalProxyRepoCount,
    loading: false,
    error: null,
  };
}

// XState v4 does not infer `event` type inside `assign` callbacks. Every action
// below therefore accepts `event: any` and casts to the concrete payload shape
// at the top of the body. A future XState v5 upgrade would let us drop the
// `any`s in favor of typed model events.
const applyFetchResult = assign((ctx: MaliciousPackagesMachineContext, event: any) => {
  const result = event.data as FetchAllResult;
  const nextData = mergeFetchedData(ctx.data, result.data);
  const nextProxyRepos = proxyReposEqual(ctx.proxyRepos, result.proxyRepos)
    ? ctx.proxyRepos
    : result.proxyRepos;
  const nextRhcScans = mergeRhcScans(ctx.rhcScans, ctx.enablePending, result.serverRhcScans);
  return {
    data: nextData,
    proxyRepos: nextProxyRepos,
    tasks: result.tasks,
    rhcScans: nextRhcScans,
    prevAnyTaskRunning: result.tasks.some((t) => isSchedulerTaskRunningState(t.currentState)),
  };
});

const setError = assign({
  data: (ctx: MaliciousPackagesMachineContext, event: any) => ({
    ...ctx.data,
    loading: false,
    error: event.data instanceof Error ? event.data.message : 'Failed to load malicious packages data',
  }),
});

const setTasksFromEvent = assign({
  tasks: (_ctx: MaliciousPackagesMachineContext, event: any) =>
    (event as { tasks: TaskInfo[] }).tasks,
});

const updatePrevAnyTaskRunning = assign({
  prevAnyTaskRunning: (ctx: MaliciousPackagesMachineContext) =>
    ctx.tasks.some((t) => isSchedulerTaskRunningState(t.currentState)),
});

// =============================================================================
// Guards
// =============================================================================

const guards = {
  /**
   * True when the previous fetch saw a running task but the current fetch doesn't —
   * mirrors the old `wasRunningRef` effect that triggered an extra fetchData when
   * tasks transitioned from running → not-running. Reads tasks from the incoming
   * `onDone` event payload because guards run before actions, so `ctx.tasks` still
   * holds the previous fetch.
   */
  tasksJustFinished: (ctx: MaliciousPackagesMachineContext, event: any) => {
    const result = event.data as FetchAllResult | undefined;
    if (!result) return false;
    const anyRunning = result.tasks.some((t) => isSchedulerTaskRunningState(t.currentState));
    return ctx.prevAnyTaskRunning && !anyRunning;
  },
};

// =============================================================================
// Machine
// =============================================================================

export const maliciousPackagesMachine = createMachine<
  MaliciousPackagesMachineContext,
  MaliciousPackagesMachineEvent
>(
  {
    id: 'maliciousPackages',
    initial: 'loading',
    context: makeInitialContext(),
    on: {
      // Imperative state-sync events accepted in every state — workflows in the hook
      // dispatch these as their REST calls progress so the machine remains the single
      // source of truth for UI state.
      SET_TASKS: { actions: ['setTasksFromEvent', 'updatePrevAnyTaskRunning'] },
      SET_HISTORY: { actions: 'setHistory' },
      SET_BULK_PROGRESS: { actions: 'setBulkProgress' },
      SET_TASKS_LOADING: { actions: 'setTasksLoading' },
      RHC_OPTIMISTIC_START: { actions: 'addEnablePendingOptimistic' },
      RHC_OPTIMISTIC_CLEAR: { actions: 'clearEnablePending' },
      RHC_FAILED: { actions: 'markRhcFailed' },
      SET_IDENTIFY_FAILURE: { actions: 'setIdentifyFailure' },
      CLEAR_IDENTIFY_FAILURE: { actions: 'clearIdentifyFailure' },
    },
    states: {
      loading: {
        invoke: {
          id: 'initialLoad',
          src: 'fetchAll',
          onDone: { target: 'ready', actions: 'applyFetchResult' },
          onError: { target: 'failed', actions: 'setError' },
        },
      },
      failed: {
        on: {
          REFRESH: 'loading',
          RETRY: 'loading',
        },
      },
      ready: {
        initial: 'idle',
        states: {
          idle: {
            after: {
              POLL_INTERVAL: { target: 'polling' },
            },
          },
          polling: {
            invoke: {
              id: 'poll',
              src: 'fetchAll',
              // Successful poll: apply the new data and, if tasks just finished,
              // do not wait the full interval — refetch immediately for fresh derived state.
              onDone: [
                {
                  target: 'polling',
                  cond: 'tasksJustFinished',
                  actions: 'applyFetchResult',
                  internal: false,
                },
                {
                  target: 'idle',
                  actions: 'applyFetchResult',
                },
              ],
              // Swallow poll errors: the UI keeps the last good data and the next
              // tick will retry. Failed polls don't bounce the user to an error screen.
              onError: { target: 'idle' },
            },
          },
        },
        on: {
          REFRESH: '.polling',
        },
      },
    },
  },
  {
    delays: {
      // Variable poll cadence based on whether tasks are running or scans are in progress.
      POLL_INTERVAL: (ctx) => selectPollIntervalMs(ctx.tasks, ctx.rhcScans),
    },
    services: {
      fetchAll: () => fetchAllServiceImpl(),
      fetchTasks: () => fetchTasksServiceImpl(),
    },
    actions: {
      applyFetchResult,
      setError,
      setTasksFromEvent,
      updatePrevAnyTaskRunning,
      setHistory: assign({
        data: (ctx, event) => ({
          ...ctx.data,
          historyFindings: (event as { history: MaliciousFinding[] }).history,
        }),
      }),
      setBulkProgress: assign({
        bulkProgress: (_ctx, event) => (event as { bulkProgress: BulkProgress }).bulkProgress,
      }),
      setTasksLoading: assign({
        tasksLoading: (_ctx, event) => (event as { loading: boolean }).loading,
      }),
      addEnablePendingOptimistic: assign((ctx, event) => {
        const { repoName } = event as { repoName: string };
        const nextPending = new Set(ctx.enablePending);
        nextPending.add(repoName);
        const nextScans = new Map(ctx.rhcScans);
        nextScans.set(repoName, { phase: 'scanning', startedAt: Date.now() });
        return { enablePending: nextPending, rhcScans: nextScans };
      }),
      clearEnablePending: assign((ctx, event) => {
        const { repoName } = event as { repoName: string };
        if (!ctx.enablePending.has(repoName)) return ctx;
        const nextPending = new Set(ctx.enablePending);
        nextPending.delete(repoName);
        return { enablePending: nextPending };
      }),
      markRhcFailed: assign((ctx, event) => {
        const { repoName, error } = event as { repoName: string; error: string };
        const nextPending = new Set(ctx.enablePending);
        nextPending.delete(repoName);
        const nextScans = new Map(ctx.rhcScans);
        // Preserve the original scan startedAt so the UI can show elapsed time
        // even on failure; fall back to now if this is the first time we see the repo.
        const startedAt = ctx.rhcScans.get(repoName)?.startedAt ?? Date.now();
        nextScans.set(repoName, {
          phase: 'failed',
          startedAt,
          completedAt: Date.now(),
          error,
        });
        return { enablePending: nextPending, rhcScans: nextScans };
      }),
      setIdentifyFailure: assign((ctx, event) => {
        const { repoName, reason } = event as { repoName: string; reason: string };
        const next = new Map(ctx.identifyFailures);
        next.set(repoName, reason);
        return { identifyFailures: next };
      }),
      clearIdentifyFailure: assign((ctx, event) => {
        const { repoName } = event as { repoName: string };
        if (!ctx.identifyFailures.has(repoName)) return ctx;
        const next = new Map(ctx.identifyFailures);
        next.delete(repoName);
        return { identifyFailures: next };
      }),
    },
    guards,
  },
);
