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

import { useCallback, useEffect, useMemo, useRef } from 'react';
import { useMachine } from '@xstate/react';

import { restClient, ENDPOINTS } from '../../../../interface/api';
import { MaliciousFinding } from './types';
import { maliciousPackagesMachine } from './maliciousPackagesMachine';
import type { MalwareRemediatorMode } from '../../shared/security/malwareRemediatorTask';
import {
  bulkEnableTasksWorkflow,
  createAndRunAuditTaskWorkflow,
  enableRhcWorkflow,
  reEnableTaskWorkflow,
  remediateRepositoryWorkflow,
  runTaskWorkflow,
} from './maliciousPackagesWorkflows';
import type {
  FindingsPage,
  MaliciousPackagesDataSnapshot,
  RemediateResponse,
  TaskInfo,
} from './maliciousPackagesUtils';

// Re-exports preserve compatibility with components that import these from here.
export {
  getRepoMalwareRemediatorBusyStatus,
  isSchedulerTaskRunningState,
  parseTaskCurrentState,
} from './maliciousPackagesUtils';
export type {
  BulkProgress,
  FindingsDateRange,
  FindingsPage,
  MaliciousPackagesDataSnapshot,
  ProxyRepo,
  RemediateResponse,
  RepoMalwareRemediatorBusyStatus,
  RhcScanInfo,
  RhcScanPhase,
  TaskInfo,
} from './maliciousPackagesUtils';

export function useMaliciousPackagesData(): MaliciousPackagesDataSnapshot {
  const [state, send] = useMachine(maliciousPackagesMachine);

  // Tracks every in-flight workflow's AbortController so polling loops stop on unmount.
  const abortControllersRef = useRef<Set<AbortController>>(new Set());

  useEffect(() => {
    const controllers = abortControllersRef.current;
    return () => {
      for (const c of controllers) c.abort();
      controllers.clear();
    };
  }, []);

  const runWithAbort = useCallback(
    async <T,>(work: (signal: AbortSignal) => Promise<T>): Promise<T> => {
      const ac = new AbortController();
      abortControllersRef.current.add(ac);
      try {
        return await work(ac.signal);
      } finally {
        abortControllersRef.current.delete(ac);
      }
    },
    [],
  );

  // Single bag of handlers reused by every workflow. Workflows pick the keys they need
  // (TypeScript structural typing tolerates extras on non-literal objects).
  const handlers = useMemo(
    () => ({
      onTasksSnapshot: (tasks: TaskInfo[]) => send({ type: 'SET_TASKS', tasks }),
      onRefresh: () => send({ type: 'REFRESH' }),
      onRefreshTasks: () => send({ type: 'REFRESH' }),
      onClearIdentifyFailure: (repoName: string) =>
        send({ type: 'CLEAR_IDENTIFY_FAILURE', repoName }),
      onSetIdentifyFailure: (repoName: string, reason: string) =>
        send({ type: 'SET_IDENTIFY_FAILURE', repoName, reason }),
      onOptimisticStart: (repoName: string) => send({ type: 'RHC_OPTIMISTIC_START', repoName }),
      onOptimisticClear: (repoName: string) => send({ type: 'RHC_OPTIMISTIC_CLEAR', repoName }),
      onFailed: (repoName: string, error: string) =>
        send({ type: 'RHC_FAILED', repoName, error }),
      onProgressSnapshot: (completed: number, total: number) =>
        send({ type: 'SET_BULK_PROGRESS', bulkProgress: { total, completed, active: true } }),
    }),
    [send],
  );

  // Imperative mutations: POST/GET, then REFRESH so the machine pulls the new state.
  const refetch = useCallback(() => send({ type: 'REFRESH' }), [send]);
  const refetchTasks = useCallback(async () => void send({ type: 'REFRESH' }), [send]);

  const acknowledge = useCallback(
    async (id: number, reason: string, duration?: string) => {
      await restClient.post(`${ENDPOINTS.MALICIOUS_RISK_ACKNOWLEDGE}/${id}`, { reason, duration });
      send({ type: 'REFRESH' });
    },
    [send],
  );

  const deleteFinding = useCallback(
    async (id: number) => {
      await restClient.post(`${ENDPOINTS.MALICIOUS_RISK_DELETE_FINDING}/${id}`);
      send({ type: 'REFRESH' });
    },
    [send],
  );

  const bulkDelete = useCallback(
    async (ids: number[]) => {
      await Promise.all(
        ids.map((id) => restClient.post(`${ENDPOINTS.MALICIOUS_RISK_DELETE_FINDING}/${id}`)),
      );
      send({ type: 'REFRESH' });
    },
    [send],
  );

  const bulkAcknowledge = useCallback(
    async (ids: number[], reason: string, duration?: string) => {
      await Promise.all(
        ids.map((id) =>
          restClient.post(`${ENDPOINTS.MALICIOUS_RISK_ACKNOWLEDGE}/${id}`, { reason, duration }),
        ),
      );
      send({ type: 'REFRESH' });
    },
    [send],
  );

  const fetchHistory = useCallback(
    async (limit: number, offset: number) => {
      try {
        const history = await restClient.get<MaliciousFinding[]>(
          `${ENDPOINTS.MALICIOUS_RISK_HISTORY}?limit=${limit}&offset=${offset}`,
        );
        send({ type: 'SET_HISTORY', history: Array.isArray(history) ? history : [] });
      } catch {
        send({ type: 'SET_HISTORY', history: [] });
      }
    },
    [send],
  );

  const remediateFindings = useCallback(
    async (ids: number[]): Promise<RemediateResponse> => {
      const result = await restClient.post<RemediateResponse>(
        ENDPOINTS.MALICIOUS_RISK_REMEDIATE,
        { findingIds: ids },
      );
      send({ type: 'REFRESH' });
      return result;
    },
    [send],
  );

  const fetchFindings = useCallback(
    async (
      sinceDays: number,
      limit: number,
      offset: number,
      repositoryName?: string,
    ): Promise<FindingsPage> => {
      const repoParam = repositoryName
        ? `&repositoryName=${encodeURIComponent(repositoryName)}`
        : '';
      const result = await restClient.get<FindingsPage>(
        `${ENDPOINTS.MALICIOUS_RISK_HISTORY}?sinceDays=${sinceDays}&limit=${limit}&offset=${offset}${repoParam}`,
      );
      return {
        items: Array.isArray(result?.items) ? result.items : [],
        totalCount: typeof result?.totalCount === 'number' ? result.totalCount : 0,
      };
    },
    [],
  );

  // Long-running workflows: poll internally, accept AbortSignal so unmount cancels them.
  const runTask = useCallback(
    (id: string) => runWithAbort((signal) => runTaskWorkflow(id, signal, handlers)),
    [handlers, runWithAbort],
  );

  const createAndRunAuditTask = useCallback(
    (repoName: string) =>
      runWithAbort((signal) => createAndRunAuditTaskWorkflow(repoName, signal, handlers)),
    [handlers, runWithAbort],
  );

  const remediateRepository = useCallback(
    (repoName: string) =>
      runWithAbort((signal) => remediateRepositoryWorkflow(repoName, signal, handlers)),
    [handlers, runWithAbort],
  );

  const reEnableTask = useCallback(
    (id: string) => runWithAbort((signal) => reEnableTaskWorkflow(id, signal, handlers)),
    [handlers, runWithAbort],
  );

  const enableRhc = useCallback(
    (repoName: string) =>
      runWithAbort((signal) => enableRhcWorkflow(repoName, signal, handlers)),
    [handlers, runWithAbort],
  );

  const enableTasksForRepos = useCallback(
    async (
      repoNames: string[],
      mode: MalwareRemediatorMode,
      onProgress?: (completed: number) => void,
    ) => {
      send({ type: 'SET_TASKS_LOADING', loading: true });
      send({
        type: 'SET_BULK_PROGRESS',
        bulkProgress: { total: repoNames.length, completed: 0, active: true },
      });
      let succeeded = false;
      try {
        await runWithAbort((signal) =>
          bulkEnableTasksWorkflow(repoNames, mode, signal, handlers, onProgress),
        );
        succeeded = true;
      } finally {
        send({ type: 'SET_TASKS_LOADING', loading: false });
        send({
          type: 'SET_BULK_PROGRESS',
          bulkProgress: { total: 0, completed: 0, active: false },
        });
      }
      // Refresh *after* the loading/progress flags are cleared so consumers
      // never observe fresh data alongside a still-true tasksLoading flag.
      if (succeeded) send({ type: 'REFRESH' });
    },
    [send, handlers, runWithAbort],
  );

  return {
    ...state.context.data,
    acknowledge,
    deleteFinding,
    bulkDelete,
    bulkAcknowledge,
    refetch,
    fetchHistory,
    tasks: state.context.tasks,
    tasksLoading: state.context.tasksLoading,
    runTask,
    enableTasksForRepos,
    reEnableTask,
    refetchTasks,
    proxyRepos: state.context.proxyRepos,
    enableRhc,
    rhcScans: state.context.rhcScans,
    bulkProgress: state.context.bulkProgress,
    remediateFindings,
    remediateRepository,
    fetchFindings,
    createAndRunAuditTask,
    identifyFailures: state.context.identifyFailures,
  };
}
