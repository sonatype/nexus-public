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
 * Long-running async workflows for the Malicious Packages page.
 *
 * These workflows have internal polling loops (waiting for a task to leave the
 * RUNNING group of scheduler states) and are awaited by component code through
 * Promise-returning hook methods. Each accepts an AbortSignal so the hook can
 * cancel in-flight polling on unmount — the previous implementation leaked these
 * loops after the user navigated away.
 */

import { restClient, ENDPOINTS } from '../../../../interface/api';
import {
  fetchMalwareRemediatorTasks,
  setMalwareRemediatorEnabledForRepository,
  isConflictingManualRemediationTask,
  type MalwareRemediatorMode,
  type MalwareRemediatorTaskListItem,
} from '../../shared/security/malwareRemediatorTask';
import { MaliciousFinding } from './types';
import {
  countPendingFindingsInRepo,
  isSchedulerTaskRunningState,
  type TaskInfo,
} from './maliciousPackagesUtils';
import { normalizeTasksResponse } from './maliciousPackagesMachine';

// =============================================================================
// AbortSignal helpers
// =============================================================================

export class WorkflowAbortError extends Error {
  constructor() {
    super('Workflow aborted');
    this.name = 'WorkflowAbortError';
  }
}

function checkAborted(signal: AbortSignal | undefined): void {
  if (signal?.aborted) throw new WorkflowAbortError();
}

export function abortableDelay(ms: number, signal: AbortSignal | undefined): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    if (signal?.aborted) {
      reject(new WorkflowAbortError());
      return;
    }
    const timer = setTimeout(() => {
      signal?.removeEventListener('abort', onAbort);
      resolve();
    }, ms);
    const onAbort = () => {
      clearTimeout(timer);
      reject(new WorkflowAbortError());
    };
    signal?.addEventListener('abort', onAbort, { once: true });
  });
}

// =============================================================================
// Polling primitives
// =============================================================================

const TASK_POLL_INTERVAL_MS = 2_000;
/** Avoid infinite wait if the task never appears in the filtered list (misconfiguration). */
const MAX_POLLS_WITHOUT_TASK = 30;
const POST_RUN_SETTLE_MS = 500;

/**
 * Poll the malware remediator task list until the task with `taskId` is no longer
 * in a scheduler running state, calling {@link onSnapshot} with the freshly
 * normalized list after each poll so the caller can keep the machine in sync.
 */
async function pollUntilTaskFinishes(
  taskId: string,
  onSnapshot: (tasks: TaskInfo[]) => void,
  signal: AbortSignal | undefined,
): Promise<TaskInfo | undefined> {
  await abortableDelay(POST_RUN_SETTLE_MS, signal);

  let pollsWithoutTask = 0;
  let lastTarget: TaskInfo | undefined;

  for (;;) {
    checkAborted(signal);
    const raw = await fetchMalwareRemediatorTasks();
    const freshTasks = normalizeTasksResponse(raw);
    onSnapshot(freshTasks);

    const target = freshTasks.find((t) => t.id === taskId);
    if (!target) {
      pollsWithoutTask++;
      if (pollsWithoutTask > MAX_POLLS_WITHOUT_TASK) {
        throw new Error('Task not found in list — check Administration > Tasks.');
      }
      await abortableDelay(TASK_POLL_INTERVAL_MS, signal);
      continue;
    }
    pollsWithoutTask = 0;
    lastTarget = target;
    if (!isSchedulerTaskRunningState(target.currentState)) {
      return lastTarget;
    }
    await abortableDelay(TASK_POLL_INTERVAL_MS, signal);
  }
}

// =============================================================================
// Workflows
// =============================================================================

export interface RunTaskCallbacks {
  /** Called after each poll to update the tasks list in the machine. */
  onTasksSnapshot: (tasks: TaskInfo[]) => void;
  /** Called once the workflow completes to refetch all derived data. */
  onRefresh: () => void;
}

export async function runTaskWorkflow(
  taskId: string,
  signal: AbortSignal | undefined,
  callbacks: RunTaskCallbacks,
): Promise<TaskInfo | undefined> {
  await restClient.post(`${ENDPOINTS.TASKS}/${taskId}/run`);
  const finalTask = await pollUntilTaskFinishes(taskId, callbacks.onTasksSnapshot, signal);
  callbacks.onRefresh();
  return finalTask;
}

export interface IdentifyCallbacks {
  onTasksSnapshot: (tasks: TaskInfo[]) => void;
  onRefresh: () => void;
  onClearIdentifyFailure: (repoName: string) => void;
  onSetIdentifyFailure: (repoName: string, reason: string) => void;
}

/**
 * Schedule the post-identify-success "settle" refetch outside the AbortController scope —
 * the user has already closed the modal by this point, so the refetch is a best-effort
 * UI update that shouldn't be cancelled.
 */
const IDENTIFY_SETTLE_DELAY_MS = 5_000;

export async function createAndRunAuditTaskWorkflow(
  repoName: string,
  signal: AbortSignal | undefined,
  callbacks: IdentifyCallbacks,
): Promise<MaliciousFinding[]> {
  callbacks.onClearIdentifyFailure(repoName);

  // Delete any prior Identify task for this repo so we always start fresh.
  const existingTasks = await fetchMalwareRemediatorTasks();
  const existingForRepo = existingTasks.find(
    (t) => t.properties?.repositoryName === repoName && t.name?.startsWith('Identify'),
  );
  if (existingForRepo) {
    try {
      await restClient.delete(`${ENDPOINTS.TASKS}/${existingForRepo.id}`);
    } catch {
      /* ok — best-effort cleanup */
    }
  }

  checkAborted(signal);
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

  checkAborted(signal);
  await restClient.post(`${ENDPOINTS.TASKS}/${taskId}/run`);

  const finalTask = await pollUntilTaskFinishes(taskId, callbacks.onTasksSnapshot, signal);

  const taskResult = finalTask?.lastRunResult;
  const taskFailed = !taskResult || (taskResult !== 'OK' && taskResult !== 'COMPLETED');
  if (taskFailed) {
    const reason =
      taskResult === 'CANCELED'
        ? 'Task was canceled by the server — check Pro license and IQ Server connection in Administration > IQ Server.'
        : taskResult === 'ERROR'
          ? 'Task encountered an error — check Administration > Tasks for details.'
          : `Task finished with unexpected result: ${taskResult ?? 'unknown'}`;
    callbacks.onSetIdentifyFailure(repoName, reason);
    callbacks.onRefresh();
    throw new Error(reason);
  }

  callbacks.onRefresh();

  const allFindings = await restClient.get<MaliciousFinding[]>(
    ENDPOINTS.MALICIOUS_RISK_ACTIVE_FINDINGS,
  );
  const repoFindings = (Array.isArray(allFindings) ? allFindings : []).filter(
    (f) => f.repositoryName === repoName,
  );

  try {
    await restClient.delete(`${ENDPOINTS.TASKS}/${taskId}`);
  } catch {
    /* best-effort cleanup */
  }

  setTimeout(() => callbacks.onRefresh(), IDENTIFY_SETTLE_DELAY_MS);
  return repoFindings;
}

export interface RemediateCallbacks {
  onTasksSnapshot: (tasks: TaskInfo[]) => void;
  onRefresh: () => void;
}

const REMEDIATE_VERIFY_INTERVAL_MS = 1_500;
const REMEDIATE_MAX_VERIFY_ATTEMPTS = 24;

export async function remediateRepositoryWorkflow(
  repoName: string,
  signal: AbortSignal | undefined,
  callbacks: RemediateCallbacks,
): Promise<void> {
  // Stop and delete any existing manual remediation task for this repo so the
  // POST below isn't rejected with HTTP 409 (TaskUtils.validateTaskCreationForAPI).
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
    checkAborted(signal);
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

  checkAborted(signal);
  await restClient.post(`${ENDPOINTS.TASKS}/${taskId}/run`);
  const finalTask = await pollUntilTaskFinishes(taskId, callbacks.onTasksSnapshot, signal);
  callbacks.onRefresh();

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
    /* one-shot task cleanup is best-effort */
  }

  // Task can report OK even when no repository was processed (appliesTo filtered all repos).
  // Active findings can also lag briefly after the task finishes — poll until cleared or timeout.
  for (let attempt = 0; attempt < REMEDIATE_MAX_VERIFY_ATTEMPTS; attempt++) {
    checkAborted(signal);
    callbacks.onRefresh();
    const active = await restClient.get<MaliciousFinding[]>(
      ENDPOINTS.MALICIOUS_RISK_ACTIVE_FINDINGS,
    );
    const list = Array.isArray(active) ? active : [];
    if (countPendingFindingsInRepo(list, repoName) === 0) {
      return;
    }
    if (attempt < REMEDIATE_MAX_VERIFY_ATTEMPTS - 1) {
      await abortableDelay(REMEDIATE_VERIFY_INTERVAL_MS, signal);
    }
  }

  // Explicit guard: in practice a mid-delay abort throws before we reach this
  // fallback GET, but relying on that is fragile — surface the abort intent.
  checkAborted(signal);
  const active = await restClient.get<MaliciousFinding[]>(ENDPOINTS.MALICIOUS_RISK_ACTIVE_FINDINGS);
  const remaining = countPendingFindingsInRepo(Array.isArray(active) ? active : [], repoName);
  throw new Error(
    `Remediation reported success but ${remaining} malicious package(s) still show as active for "${repoName}". ` +
      'The task may not have run on this repository (check Administration > Tasks), or results are still updating — refresh and try again.',
  );
}

export interface BulkEnableCallbacks {
  onProgressSnapshot: (completed: number, total: number) => void;
}

/**
 * Enable the malware remediator task for each repo in sequence. Refresh is *not*
 * triggered here — the caller owns post-success state cleanup (resetting the
 * tasksLoading and bulkProgress flags) and only then dispatches REFRESH, so the
 * machine's poll never races a still-true `tasksLoading` flag.
 */
export async function bulkEnableTasksWorkflow(
  repoNames: string[],
  mode: MalwareRemediatorMode,
  signal: AbortSignal | undefined,
  callbacks: BulkEnableCallbacks,
  onUserProgress?: (completed: number) => void,
): Promise<void> {
  const existing = await fetchMalwareRemediatorTasks();
  const baseCount = existing.length;
  for (let i = 0; i < repoNames.length; i++) {
    checkAborted(signal);
    await setMalwareRemediatorEnabledForRepository(repoNames[i], mode, baseCount + i);
    callbacks.onProgressSnapshot(i + 1, repoNames.length);
    onUserProgress?.(i + 1);
  }
}

export interface ReEnableCallbacks {
  onRefreshTasks: () => void;
}

export async function reEnableTaskWorkflow(
  taskId: string,
  signal: AbortSignal | undefined,
  callbacks: ReEnableCallbacks,
): Promise<void> {
  checkAborted(signal);
  const allTasks: MalwareRemediatorTaskListItem[] = await fetchMalwareRemediatorTasks();
  const task = allTasks.find((t) => t.id === taskId);
  if (task) {
    checkAborted(signal);
    const repo = task.properties?.repositoryName ?? 'unknown';
    await setMalwareRemediatorEnabledForRepository(repo, 'audit');
    callbacks.onRefreshTasks();
  }
}

// =============================================================================
// Enable RHC
// =============================================================================

const ENABLE_RHC_SETTLE_MS = 3_000;

export interface EnableRhcCallbacks {
  onOptimisticStart: (repoName: string) => void;
  onOptimisticClear: (repoName: string) => void;
  onFailed: (repoName: string, error: string) => void;
  onRefresh: () => void;
}

export async function enableRhcWorkflow(
  repoName: string,
  signal: AbortSignal | undefined,
  callbacks: EnableRhcCallbacks,
): Promise<void> {
  callbacks.onOptimisticStart(repoName);
  try {
    await restClient.post(`${ENDPOINTS.HEALTH_CHECK_ANALYZE(repoName)}?source=detect`, {});
  } catch (err: unknown) {
    const axiosData = (err as { response?: { data?: string } })?.response?.data;
    const serverMsg = typeof axiosData === 'string' ? axiosData.replace(/^"|"$/g, '') : null;
    const errorMsg = serverMsg || (err instanceof Error ? err.message : String(err));
    callbacks.onFailed(repoName, errorMsg);
    return;
  }

  try {
    await abortableDelay(ENABLE_RHC_SETTLE_MS, signal);
  } catch {
    // Aborted — still clear the optimistic flag so the next mount doesn't see stale state.
    callbacks.onOptimisticClear(repoName);
    return;
  }
  // Clear the optimistic pending entry *before* triggering a refresh so the
  // machine's mergeRhcScans run inside applyFetchResult doesn't observe the
  // repo as still-pending and resurrect the optimistic entry for another tick.
  callbacks.onOptimisticClear(repoName);
  callbacks.onRefresh();
}
