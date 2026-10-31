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

import { MaliciousFinding, MaliciousPackagesState } from './types';
import type { MalwareRemediatorMode } from '../../shared/security/malwareRemediatorTask';

// =============================================================================
// Public types — consumed by the machine, hook, and several presentation files.
// =============================================================================

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

export type RhcScanPhase = 'scanning' | 'completed' | 'failed';

export interface RhcScanInfo {
  phase: RhcScanPhase;
  startedAt: number;
  completedAt?: number;
  error?: string;
  signatureCount?: number;
}

/** Server-side Malicious Packages task for this repo is actively executing (blocks starting another). */
export type RepoMalwareRemediatorBusyStatus = 'running';

/** Stable public contract returned by useMaliciousPackagesData — consumed by several tab files. */
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
  enableTasksForRepos: (
    repoNames: string[],
    mode: MalwareRemediatorMode,
    onProgress?: (completed: number) => void,
  ) => Promise<void>;
  reEnableTask: (taskId: string) => Promise<void>;
  refetchTasks: () => Promise<void>;
  proxyRepos: ProxyRepo[];
  enableRhc: (repoName: string) => Promise<void>;
  rhcScans: Map<string, RhcScanInfo>;
  remediateFindings: (findingIds: number[]) => Promise<RemediateResponse>;
  remediateRepository: (repoName: string) => Promise<void>;
  fetchFindings: (
    sinceDays: number,
    limit: number,
    offset: number,
    repositoryName?: string,
  ) => Promise<FindingsPage>;
  bulkProgress: BulkProgress;
  createAndRunAuditTask: (repoName: string) => Promise<MaliciousFinding[]>;
  identifyFailures: Map<string, string>;
}

// =============================================================================
// Pure helpers
// =============================================================================

export function shallowEqualStringArray(a: string[], b: string[]): boolean {
  if (a === b) return true;
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    if (a[i] !== b[i]) return false;
  }
  return true;
}

export function shallowEqualRecord(
  a: Record<string, number>,
  b: Record<string, number>,
): boolean {
  const keysA = Object.keys(a);
  const keysB = Object.keys(b);
  if (keysA.length !== keysB.length) return false;
  for (const k of keysA) {
    if (a[k] !== b[k]) return false;
  }
  return true;
}

/**
 * Cheap identity for a findings list. Server order is non-deterministic across
 * polls, so we sort the id-tokens before joining; two polls that returned the
 * same findings will produce the same fingerprint regardless of order. The sort
 * runs on every poll cycle (5–30 s), which is O(n log n) on a copy — fine for
 * expected finding counts (tens per repo). If lists grow into the thousands,
 * switch to a hash-set diff or sort at ingestion.
 */
export function findingsFingerprint(findings: MaliciousFinding[]): string {
  if (findings.length === 0) return '0:';
  const ids = findings.map((f) => `${f.id}:${f.deletedTime ?? ''}:${f.acknowledgedAt ?? ''}`);
  ids.sort();
  return `${findings.length}:${ids.join(',')}`;
}

export function proxyReposEqual(a: ProxyRepo[], b: ProxyRepo[]): boolean {
  if (a === b) return true;
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    if (
      a[i].name !== b[i].name ||
      a[i].format !== b[i].format ||
      a[i].rhcSupported !== b[i].rhcSupported ||
      a[i].rhcEnabled !== b[i].rhcEnabled
    ) {
      return false;
    }
  }
  return true;
}

export function countPendingFindingsInRepo(
  findings: MaliciousFinding[],
  repoName: string,
): number {
  return findings.filter(
    (f) => f.repositoryName === repoName && !f.deletedTime && !f.acknowledgedAt,
  ).length;
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

export function getRepoMalwareRemediatorBusyStatus(
  tasks: TaskInfo[],
  repositoryName: string,
): RepoMalwareRemediatorBusyStatus | null {
  for (const t of tasks) {
    if (t.repositoryName !== repositoryName) continue;
    const s = t.currentState;
    if (s == null) continue;
    if (isSchedulerTaskRunningState(s)) return 'running';
  }
  return null;
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
