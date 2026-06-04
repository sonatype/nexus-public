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

import { useCallback, useEffect, useRef, useState } from 'react';
import { restClient, ENDPOINTS } from '../../../../interface/api';
import { MaliciousFinding, MaliciousPackagesState } from './types';
import {
  fetchMalwareRemediatorTasks,
  setMalwareRemediatorEnabledForRepository,
  deriveMalwareRemediatorMode,
  isConflictingManualRemediationTask,
} from '../../shared/security/malwareRemediatorTask';
import { MalwareRemediatorTaskListItem, MalwareRemediatorMode } from '../../shared/security/malwareRemediatorTask';
import { isHealthCheckSupportedFormat } from '../../../../utils/healthCheckFormats';

function shallowEqualStringArray(a: string[], b: string[]): boolean {
  if (a === b) return true;
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    if (a[i] !== b[i]) return false;
  }
  return true;
}

function shallowEqualRecord(a: Record<string, number>, b: Record<string, number>): boolean {
  const keysA = Object.keys(a);
  const keysB = Object.keys(b);
  if (keysA.length !== keysB.length) return false;
  for (const k of keysA) {
    if (a[k] !== b[k]) return false;
  }
  return true;
}

function findingsFingerprint(findings: MaliciousFinding[]): string {
  if (findings.length === 0) return '0:';
  const ids = findings.map((f) => `${f.id}:${f.deletedTime ?? ''}:${f.acknowledgedAt ?? ''}`);
  ids.sort();
  return `${findings.length}:${ids.join(',')}`;
}

function proxyReposEqual(a: ProxyRepo[], b: ProxyRepo[]): boolean {
  if (a === b) return true;
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    if (a[i].name !== b[i].name || a[i].format !== b[i].format ||
        a[i].rhcSupported !== b[i].rhcSupported || a[i].rhcEnabled !== b[i].rhcEnabled) return false;
  }
  return true;
}

export interface ProxyRepo {
  name: string;
  format: string;
  rhcSupported: boolean;
  rhcEnabled: boolean;
}

export interface BulkProgress {
  total: number;
  completed: number;
  active: boolean;
}

export interface RemediateResponse {
  remediationId?: string | null;
  totalRequested: number;
  totalDeleted: number;
  totalFailed: number;
  results: Array<{
    findingId: number;
    repositoryName: string;
    assetPath: string;
    success: boolean;
    error: string | null;
    remediationId?: string | null;
  }>;
}

export type FindingsDateRange = '1d' | '7d' | '30d' | '90d' | 'forever';

export interface FindingsPage {
  items: MaliciousFinding[];
  totalCount: number;
}

export interface TaskInfo {
  id: string;
  name: string;
  repositoryName: string;
  mode: MalwareRemediatorMode;
  enabled: boolean;
  lastRun: string | null;
  lastRunResult: string | null;
  nextRun: string | null;
  currentState: string | null;
  /** Progress text from REST when running (e.g. "17%"), split from "RUNNING: 17%". */
  progress: string | null;
}

/** TaskState Group.RUNNING values (see org.sonatype.nexus.scheduling.TaskState). */
const SCHEDULER_RUNNING_STATES = new Set([
  'RUNNING_STARTING',
  'RUNNING_BLOCKED',
  'RUNNING',
  'RUNNING_CANCELED',
]);

/**
 * True when the scheduler reports the task instance is still in a running group state.
 * REST may embed progress in currentState as "RUNNING: 17%"; normalize with {@link parseTaskCurrentState} first.
 */
export function isSchedulerTaskRunningState(state: string | null | undefined): boolean {
  return state != null && SCHEDULER_RUNNING_STATES.has(state);
}

/** Server-side Malicious Packages task for this repo is actively executing (blocks starting another). */
export type RepoMalwareRemediatorBusyStatus = 'running';

export function getRepoMalwareRemediatorBusyStatus(
  tasks: TaskInfo[],
  repositoryName: string
): RepoMalwareRemediatorBusyStatus | null {
  for (const t of tasks) {
    if (t.repositoryName !== repositoryName) continue;
    const s = t.currentState;
    if (s == null) continue;
    if (isSchedulerTaskRunningState(s)) return 'running';
  }
  return null;
}

function countPendingFindingsInRepo(findings: MaliciousFinding[], repoName: string): number {
  return findings.filter(
    (f) => f.repositoryName === repoName && !f.deletedTime && !f.acknowledgedAt,
  ).length;
}

/**
 * Splits REST TaskXO currentState when progress is appended: "RUNNING: 17%" → state + progress.
 */
export function parseTaskCurrentState(raw: string | null | undefined): {
  state: string | null;
  progress: string | null;
} {
  if (raw == null || raw === '') {
    return { state: null, progress: null };
  }
  const idx = raw.indexOf(': ');
  if (idx === -1) {
    return { state: raw, progress: null };
  }
  return {
    state: raw.slice(0, idx),
    progress: raw.slice(idx + 2) || null,
  };
}

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

export type RhcScanPhase = 'scanning' | 'completed' | 'failed';

export interface RhcScanInfo {
  phase: RhcScanPhase;
  startedAt: number;
  completedAt?: number;
  error?: string;
  signatureCount?: number;
}

interface HealthCheckSummaryItem {
  repositoryName: string;
  enabled: boolean;
  analyzing: boolean;
  lastAnalyzedDate?: number | null;
  malwareCount?: number;
}

export interface MaliciousPackagesDataSnapshot extends MaliciousPackagesState {
  acknowledge: (findingId: number, reason: string, duration?: string) => Promise<void>;
  deleteFinding: (findingId: number) => Promise<void>;
  bulkDelete: (findingIds: number[]) => Promise<void>;
  bulkAcknowledge: (findingIds: number[], reason: string, duration?: string) => Promise<void>;
  refetch: () => void;
  fetchHistory: (limit: number, offset: number) => Promise<void>;
  tasks: TaskInfo[];
  tasksLoading: boolean;
  runTask: (taskId: string) => Promise<TaskInfo | undefined>;
  enableTasksForRepos: (repoNames: string[], mode: MalwareRemediatorMode, onProgress?: (completed: number) => void) => Promise<void>;
  reEnableTask: (taskId: string) => Promise<void>;
  refetchTasks: () => Promise<void>;
  proxyRepos: ProxyRepo[];
  enableRhc: (repoName: string) => Promise<void>;
  rhcScans: Map<string, RhcScanInfo>;
  remediateFindings: (findingIds: number[]) => Promise<RemediateResponse>;
  remediateRepository: (repoName: string) => Promise<void>;
  fetchFindings: (sinceDays: number, limit: number, offset: number, repositoryName?: string) => Promise<FindingsPage>;
  bulkProgress: BulkProgress;
  createAndRunAuditTask: (repoName: string) => Promise<MaliciousFinding[]>;
  identifyFailures: Map<string, string>;
}

export function useMaliciousPackagesData(): MaliciousPackagesDataSnapshot {
  const [state, setState] = useState<MaliciousPackagesState>({
    activeFindings: [],
    historyFindings: [],
    malwareCount: 0,
    countsByRepo: {},
    hasFirewall: false,
    hcEnabledRepos: [],
    totalProxyRepoCount: 0,
    loading: true,
    error: null,
  });

  const [proxyRepos, setProxyRepos] = useState<ProxyRepo[]>([]);
  const [bulkProgress, setBulkProgress] = useState<BulkProgress>({ total: 0, completed: 0, active: false });
  const [rhcScans, setRhcScans] = useState<Map<string, RhcScanInfo>>(new Map());
  const enablePendingRef = useRef<Set<string>>(new Set());

  const deriveRhcScans = useCallback((
    hcSummary: HealthCheckSummaryItem[],
    detectInitiated: Record<string, number>,
    counts: Record<string, number>,
    hcEnabledRepos: string[],
  ) => {
    const map = new Map<string, RhcScanInfo>();
    const hcByName = new Map(hcSummary.map((item) => [item.repositoryName, item]));
    const enabledSet = new Set(hcEnabledRepos);
    const STUCK_THRESHOLD_MS = 5 * 60 * 1000;

    for (const [repoName, initiatedAt] of Object.entries(detectInitiated)) {
      const hc = hcByName.get(repoName);

      if (hc?.analyzing) {
        map.set(repoName, {
          phase: 'scanning',
          startedAt: initiatedAt,
        });
      }
      else if (hc?.enabled && hc.lastAnalyzedDate) {
        map.set(repoName, {
          phase: 'completed',
          startedAt: initiatedAt,
          completedAt: hc.lastAnalyzedDate,
          signatureCount: counts[repoName] ?? hc.malwareCount ?? 0,
        });
      }
      else if (!enabledSet.has(repoName) && (Date.now() - initiatedAt) > STUCK_THRESHOLD_MS) {
        map.set(repoName, {
          phase: 'failed',
          startedAt: initiatedAt,
          completedAt: Date.now(),
          error: 'Repository Health Check could not be enabled — check Administration > System > Tasks for details',
        });
      }
      else {
        map.set(repoName, {
          phase: 'scanning',
          startedAt: initiatedAt,
        });
      }
    }
    return map;
  }, []);

  const fetchData = useCallback(async () => {
    setState((prev) => ({ ...prev, error: null }));
    try {
      const [activeFindings, malwareCounts, iqCapabilities, repos, hcSummary] = await Promise.all([
        restClient.get<MaliciousFinding[]>(ENDPOINTS.MALICIOUS_RISK_ACTIVE_FINDINGS),
        restClient.get<MalwareCountsResponse>(ENDPOINTS.MALWARE_COUNTS),
        restClient.get<IqCapabilitiesResponse>(ENDPOINTS.IQ_CAPABILITIES).catch(() => null),
        restClient.get<RepositoryRef[]>(ENDPOINTS.REPOSITORIES),
        restClient.get<HealthCheckSummaryItem[]>(ENDPOINTS.HEALTH_CHECK_SUMMARY),
      ]);

      const nextProxyRepos = (Array.isArray(repos) ? repos : []).filter((r) => r.type === 'proxy');
      const enabledSet = new Set(malwareCounts?.hcEnabledRepos ?? []);
      const hcSummaryRepoNames = new Set(
        (Array.isArray(hcSummary) ? hcSummary : []).map((item) => item.repositoryName),
      );

      setProxyRepos((prev) => {
        const next = nextProxyRepos.map((r) => ({
          name: r.name,
          format: r.format,
          rhcSupported: isHealthCheckSupportedFormat(r.format) && (hcSummaryRepoNames.size === 0 || hcSummaryRepoNames.has(r.name)),
          rhcEnabled: enabledSet.has(r.name),
        }));
        return proxyReposEqual(prev, next) ? prev : next;
      });

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
      const serverScans = deriveRhcScans(
        Array.isArray(hcSummary) ? hcSummary : [],
        detectInitiated,
        filteredCounts,
        malwareCounts?.hcEnabledRepos ?? [],
      );
      setRhcScans((prev) => {
        const merged = new Map(serverScans);
        for (const [name, info] of prev) {
          if (info.phase === 'scanning' && !serverScans.has(name) && enablePendingRef.current.has(name)) {
            merged.set(name, info);
          }
          if (info.phase === 'failed' && !serverScans.has(name)) {
            merged.set(name, info);
          }
        }
        return merged;
      });

      const nextFindings = Array.isArray(activeFindings) ? activeFindings : [];
      const nextHcEnabled = malwareCounts?.hcEnabledRepos ?? [];
      const nextFirewall = !!iqCapabilities?.hasFirewall;
      const nextFindingsFp = findingsFingerprint(nextFindings);

      setState((prev) => {
        const prevFindingsFp = findingsFingerprint(prev.activeFindings);
        const findingsChanged = prevFindingsFp !== nextFindingsFp;
        const countsChanged = !shallowEqualRecord(prev.countsByRepo, filteredCounts);
        const hcEnabledChanged = !shallowEqualStringArray(prev.hcEnabledRepos, nextHcEnabled);

        const unchanged =
          !prev.loading &&
          prev.error === null &&
          prev.malwareCount === filteredTotal &&
          prev.totalProxyRepoCount === nextProxyRepos.length &&
          prev.hasFirewall === nextFirewall &&
          !countsChanged &&
          !hcEnabledChanged &&
          !findingsChanged;

        if (unchanged) return prev;

        return {
          activeFindings: findingsChanged ? nextFindings : prev.activeFindings,
          historyFindings: prev.historyFindings,
          malwareCount: filteredTotal,
          countsByRepo: countsChanged ? filteredCounts : prev.countsByRepo,
          hasFirewall: nextFirewall,
          hcEnabledRepos: hcEnabledChanged ? nextHcEnabled : prev.hcEnabledRepos,
          totalProxyRepoCount: nextProxyRepos.length,
          loading: false,
          error: null,
        };
      });
    } catch (err) {
      setState((prev) => ({
        ...prev,
        loading: false,
        error: err instanceof Error ? err.message : 'Failed to load malicious packages data',
      }));
    }
  }, [deriveRhcScans]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const acknowledge = useCallback(async (findingId: number, reason: string, duration?: string) => {
    await restClient.post(`${ENDPOINTS.MALICIOUS_RISK_ACKNOWLEDGE}/${findingId}`, { reason, duration });
    await fetchData();
  }, [fetchData]);

  const deleteFinding = useCallback(async (findingId: number) => {
    await restClient.post(`${ENDPOINTS.MALICIOUS_RISK_DELETE_FINDING}/${findingId}`);
    await fetchData();
  }, [fetchData]);

  const bulkDelete = useCallback(async (findingIds: number[]) => {
    await Promise.all(
      findingIds.map((id) => restClient.post(`${ENDPOINTS.MALICIOUS_RISK_DELETE_FINDING}/${id}`))
    );
    await fetchData();
  }, [fetchData]);

  const bulkAcknowledge = useCallback(async (findingIds: number[], reason: string, duration?: string) => {
    await Promise.all(
      findingIds.map((id) => restClient.post(`${ENDPOINTS.MALICIOUS_RISK_ACKNOWLEDGE}/${id}`, { reason, duration }))
    );
    await fetchData();
  }, [fetchData]);

  const fetchHistory = useCallback(async (limit: number, offset: number) => {
    try {
      const history = await restClient.get<MaliciousFinding[]>(
        `${ENDPOINTS.MALICIOUS_RISK_HISTORY}?limit=${limit}&offset=${offset}`
      );
      setState((prev) => ({
        ...prev,
        historyFindings: Array.isArray(history) ? history : [],
      }));
    } catch {
      setState((prev) => ({
        ...prev,
        historyFindings: [],
      }));
    }
  }, []);

  const enableRhc = useCallback(async (repoName: string) => {
    enablePendingRef.current = new Set(enablePendingRef.current).add(repoName);
    setRhcScans((prev) => new Map(prev).set(repoName, { phase: 'scanning', startedAt: Date.now() }));

    try {
      await restClient.post(`${ENDPOINTS.HEALTH_CHECK_ANALYZE(repoName)}?source=detect`, {});
    } catch (err: unknown) {
      enablePendingRef.current.delete(repoName);
      const axiosData = (err as { response?: { data?: string } })?.response?.data;
      const serverMsg = typeof axiosData === 'string' ? axiosData.replace(/^"|"$/g, '') : null;
      const errorMsg = serverMsg || (err instanceof Error ? err.message : String(err));
      setRhcScans((prev) => new Map(prev).set(repoName, {
        phase: 'failed',
        startedAt: Date.now(),
        completedAt: Date.now(),
        error: errorMsg,
      }));
      return;
    }

    await new Promise<void>((resolve) => setTimeout(resolve, 3000));
    await fetchData();
    enablePendingRef.current.delete(repoName);
  }, [fetchData]);

  const [tasks, setTasks] = useState<TaskInfo[]>([]);
  const [tasksLoading, setTasksLoading] = useState(false);
  const wasRunningRef = useRef(false);
  const [identifyFailures, setIdentifyFailures] = useState<Map<string, string>>(new Map());

  function toTaskInfo(t: MalwareRemediatorTaskListItem & Record<string, unknown>): TaskInfo {
    const parsed = parseTaskCurrentState((t.currentState as string) ?? null);
    return {
      id: t.id,
      name: t.name,
      repositoryName: t.properties?.repositoryName ?? 'unknown',
      mode: deriveMalwareRemediatorMode(t),
      enabled: t.enabled,
      lastRun: (t.lastRun as string) ?? null,
      lastRunResult: (t.lastRunResult as string) ?? null,
      nextRun: (t.nextRun as string) ?? null,
      currentState: parsed.state,
      progress: parsed.progress,
    };
  }

  const refetchTasks = useCallback(async () => {
    try {
      const raw = await fetchMalwareRemediatorTasks();
      setTasks(raw.map((t) => toTaskInfo(t as MalwareRemediatorTaskListItem & Record<string, unknown>)));
    } catch {
      // keep previous tasks -- wiping to [] would cause rows to lose failed/analyzed state
    }
  }, []);

  useEffect(() => {
    refetchTasks();
  }, [refetchTasks]);

  useEffect(() => {
    const anyRunning = tasks.some((t) => isSchedulerTaskRunningState(t.currentState));
    if (!anyRunning && wasRunningRef.current) {
      fetchData();
    }
    wasRunningRef.current = anyRunning;
  }, [tasks, fetchData]);

  const pollTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    const anyScanning = [...rhcScans.values()].some((s) => s.phase === 'scanning');
    const anyRunning = tasks.some((t) => isSchedulerTaskRunningState(t.currentState));

    let interval: number;
    if (anyRunning) {
      interval = 5_000;
    } else if (anyScanning) {
      interval = 15_000;
    } else {
      interval = 30_000;
    }

    if (pollTimerRef.current) clearInterval(pollTimerRef.current);

    pollTimerRef.current = setInterval(() => {
      if (!document.hidden) {
        fetchData();
        refetchTasks();
      }
    }, interval);

    return () => {
      if (pollTimerRef.current) {
        clearInterval(pollTimerRef.current);
        pollTimerRef.current = null;
      }
    };
  }, [rhcScans, tasks, fetchData, refetchTasks]);

  const runTask = useCallback(async (taskId: string): Promise<TaskInfo | undefined> => {
    await restClient.post(`${ENDPOINTS.TASKS}/${taskId}/run`);

    const POLL_INTERVAL = 2000;
    /** Avoid infinite wait if the task never appears in the filtered list (misconfiguration). */
    const MAX_POLLS_WITHOUT_TASK = 30;

    await new Promise<void>((resolve) => setTimeout(resolve, 500));

    let lastTarget: TaskInfo | undefined;
    let pollsWithoutTask = 0;

    for (;;) {
      const raw = await fetchMalwareRemediatorTasks();
      const freshTasks = raw.map((t) => toTaskInfo(t as MalwareRemediatorTaskListItem & Record<string, unknown>));
      setTasks(freshTasks);

      const target = freshTasks.find((t) => t.id === taskId);
      if (!target) {
        pollsWithoutTask++;
        if (pollsWithoutTask > MAX_POLLS_WITHOUT_TASK) {
          throw new Error('Task not found in list — check Administration > Tasks.');
        }
        await new Promise<void>((resolve) => setTimeout(resolve, POLL_INTERVAL));
        continue;
      }
      pollsWithoutTask = 0;
      lastTarget = target;
      if (!isSchedulerTaskRunningState(target.currentState)) {
        break;
      }
      await new Promise<void>((resolve) => setTimeout(resolve, POLL_INTERVAL));
    }

    await fetchData();
    return lastTarget;
  }, [fetchData]);

  const createAndRunAuditTask = useCallback(async (repoName: string): Promise<MaliciousFinding[]> => {
    setIdentifyFailures((prev) => { const next = new Map(prev); next.delete(repoName); return next; });

    const existingTasks = await fetchMalwareRemediatorTasks();
    const existingForRepo = existingTasks.find(
      (t) => t.properties?.repositoryName === repoName && t.name?.startsWith('Identify')
    );
    if (existingForRepo) {
      try { await restClient.delete(`${ENDPOINTS.TASKS}/${existingForRepo.id}`); } catch { /* ok */ }
    }

    const taskResponse = await restClient.post<{ id: string }>(ENDPOINTS.TASKS, {
      type: 'malware.remediator',
      name: `Identify - ${repoName} - ${new Date().toISOString()}`,
      enabled: true,
      notificationCondition: 'FAILURE',
      frequency: { schedule: 'manual' },
      properties: {
        repositoryName: repoName,
        enableMalwareCleanup: 'false',
      },
    });
    const taskId = taskResponse.id;

    await restClient.post(`${ENDPOINTS.TASKS}/${taskId}/run`);

    const POLL_INTERVAL = 2000;
    const MAX_POLLS_WITHOUT_TASK = 30;
    let finalTask: TaskInfo | undefined;
    let pollsWithoutTask = 0;
    await new Promise<void>((resolve) => setTimeout(resolve, 500));

    for (;;) {
      const raw = await fetchMalwareRemediatorTasks();
      const freshTasks = raw.map((t) => toTaskInfo(t as MalwareRemediatorTaskListItem & Record<string, unknown>));
      setTasks(freshTasks);
      finalTask = freshTasks.find((t) => t.id === taskId);
      if (!finalTask) {
        pollsWithoutTask++;
        if (pollsWithoutTask > MAX_POLLS_WITHOUT_TASK) {
          throw new Error('Identify task not found — check Administration > Tasks.');
        }
        await new Promise<void>((resolve) => setTimeout(resolve, POLL_INTERVAL));
        continue;
      }
      pollsWithoutTask = 0;
      if (!isSchedulerTaskRunningState(finalTask.currentState)) {
        break;
      }
      await new Promise<void>((resolve) => setTimeout(resolve, POLL_INTERVAL));
    }

    const taskResult = finalTask?.lastRunResult;
    const taskFailed = !taskResult || (taskResult !== 'OK' && taskResult !== 'COMPLETED');

    if (taskFailed) {
      const reason = taskResult === 'CANCELED'
        ? 'Task was canceled by the server — check Pro license and IQ Server connection in Administration > IQ Server.'
        : taskResult === 'ERROR'
          ? 'Task encountered an error — check Administration > Tasks for details.'
          : `Task finished with unexpected result: ${taskResult ?? 'unknown'}`;

      setIdentifyFailures((prev) => new Map(prev).set(repoName, reason));
      await refetchTasks();
      await fetchData();
      throw new Error(reason);
    }

    await fetchData();
    const allFindings = await restClient.get<MaliciousFinding[]>(ENDPOINTS.MALICIOUS_RISK_ACTIVE_FINDINGS);
    const repoFindings = (Array.isArray(allFindings) ? allFindings : []).filter(
      (f) => f.repositoryName === repoName
    );

    try {
      await restClient.delete(`${ENDPOINTS.TASKS}/${taskId}`);
    } catch {
      // best-effort cleanup
    }

    setTimeout(() => fetchData(), 5000);

    return repoFindings;
  }, [fetchData, refetchTasks]);

  const enableTasksForRepos = useCallback(async (
    repoNames: string[],
    mode: MalwareRemediatorMode,
    onProgress?: (completed: number) => void
  ) => {
    setTasksLoading(true);
    setBulkProgress({ total: repoNames.length, completed: 0, active: true });
    try {
      const existing = await fetchMalwareRemediatorTasks();
      const baseCount = existing.length;
      for (let i = 0; i < repoNames.length; i++) {
        await setMalwareRemediatorEnabledForRepository(repoNames[i], mode, baseCount + i);
        setBulkProgress((prev) => ({ ...prev, completed: i + 1 }));
        onProgress?.(i + 1);
      }
      await refetchTasks();
    } finally {
      setTasksLoading(false);
      setBulkProgress({ total: 0, completed: 0, active: false });
    }
  }, [refetchTasks]);

  const reEnableTask = useCallback(async (taskId: string) => {
    const allTasks = await fetchMalwareRemediatorTasks();
    const task = allTasks.find((t) => t.id === taskId);
    if (task) {
      const repo = task.properties?.repositoryName ?? 'unknown';
      await setMalwareRemediatorEnabledForRepository(repo, 'audit');
      await refetchTasks();
    }
  }, [refetchTasks]);

  const remediateFindings = useCallback(async (findingIds: number[]): Promise<RemediateResponse> => {
    const result = await restClient.post<RemediateResponse>(ENDPOINTS.MALICIOUS_RISK_REMEDIATE, { findingIds });
    await fetchData();
    return result;
  }, [fetchData]);

  const remediateRepository = useCallback(async (repoName: string) => {
    const existingTasks = await fetchMalwareRemediatorTasks();
    const conflicting = existingTasks.filter((t) => isConflictingManualRemediationTask(t, repoName));
    for (const t of conflicting) {
      try {
        await restClient.post(`${ENDPOINTS.TASKS}/${t.id}/stop`);
      } catch {
        /* ignore — task may not be running */
      }
      try {
        await restClient.delete(`${ENDPOINTS.TASKS}/${t.id}`);
      } catch {
        /* ignore — best-effort before create */
      }
    }

    const taskResponse = await restClient.post<{ id: string }>(ENDPOINTS.TASKS, {
      type: 'malware.remediator',
      name: `Remediate - ${repoName} - ${new Date().toISOString()}`,
      enabled: true,
      notificationCondition: 'FAILURE',
      frequency: { schedule: 'manual' },
      properties: {
        repositoryName: repoName,
        enableMalwareCleanup: 'true',
      },
    });
    const taskId = taskResponse.id;

    const finalTask = await runTask(taskId);
    if (!finalTask) {
      throw new Error('Remediation task did not complete — check Administration > Tasks for errors.');
    }

    const taskResult = finalTask.lastRunResult;
    const taskFailed = !taskResult || (taskResult !== 'OK' && taskResult !== 'COMPLETED');
    if (taskFailed) {
      const reason =
        taskResult === 'CANCELED'
          ? 'Remediation was canceled — verify Pro license and IQ Server connection in Administration > IQ Server.'
          : taskResult === 'ERROR' || taskResult === 'FAILED'
            ? 'Remediation encountered an error — check Administration > Tasks for details.'
            : `Remediation finished with unexpected result: ${taskResult ?? 'unknown'}. Check Administration > Tasks.`;

      throw new Error(reason);
    }

    try {
      await restClient.delete(`${ENDPOINTS.TASKS}/${taskId}`);
    } catch {
      // One-shot task cleanup is best-effort
    }

    // Task can report OK even when no repository was processed (appliesTo filtered all repos).
    // Active findings can also lag briefly after the task finishes — poll until cleared or timeout.
    const MAX_VERIFY_ATTEMPTS = 24;
    const VERIFY_INTERVAL_MS = 1500;
    for (let attempt = 0; attempt < MAX_VERIFY_ATTEMPTS; attempt++) {
      await fetchData();
      const active = await restClient.get<MaliciousFinding[]>(ENDPOINTS.MALICIOUS_RISK_ACTIVE_FINDINGS);
      const list = Array.isArray(active) ? active : [];
      if (countPendingFindingsInRepo(list, repoName) === 0) {
        return;
      }
      if (attempt < MAX_VERIFY_ATTEMPTS - 1) {
        await new Promise<void>((resolve) => setTimeout(resolve, VERIFY_INTERVAL_MS));
      }
    }

    const active = await restClient.get<MaliciousFinding[]>(ENDPOINTS.MALICIOUS_RISK_ACTIVE_FINDINGS);
    const remaining = countPendingFindingsInRepo(Array.isArray(active) ? active : [], repoName);
    throw new Error(
      `Remediation reported success but ${remaining} malicious package(s) still show as active for "${repoName}". ` +
        'The task may not have run on this repository (check Administration > Tasks), or results are still updating — refresh and try again.',
    );
  }, [runTask, fetchData]);

  const fetchFindings = useCallback(async (sinceDays: number, limit: number, offset: number, repositoryName?: string): Promise<FindingsPage> => {
    let params = `?sinceDays=${sinceDays}&limit=${limit}&offset=${offset}`;
    if (repositoryName) {
      params += `&repositoryName=${encodeURIComponent(repositoryName)}`;
    }
    const result = await restClient.get<FindingsPage>(`${ENDPOINTS.MALICIOUS_RISK_FINDINGS}${params}`);
    return {
      items: Array.isArray(result?.items) ? result.items : [],
      totalCount: typeof result?.totalCount === 'number' ? result.totalCount : 0,
    };
  }, []);

  return {
    ...state,
    acknowledge,
    deleteFinding,
    bulkDelete,
    bulkAcknowledge,
    refetch: fetchData,
    fetchHistory,
    tasks,
    tasksLoading,
    runTask,
    enableTasksForRepos,
    reEnableTask,
    refetchTasks,
    proxyRepos,
    enableRhc,
    rhcScans,
    bulkProgress,
    remediateFindings,
    remediateRepository,
    fetchFindings,
    createAndRunAuditTask,
    identifyFailures,
  };
}
