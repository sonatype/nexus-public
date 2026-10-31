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

import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Badge,
  Button,
  Card,
  Dialog,
  Flex,
  Heading,
  Separator,
  Spinner,
  Table,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import {
  AlertTriangle,
  CheckCircle,
  HelpCircle,
  Play,
  XCircle,
} from 'lucide-react';

import { isSchedulerTaskRunningState, type TaskInfo, type ProxyRepo, type RhcScanInfo } from './useMaliciousPackagesData';
import type { MaliciousFinding } from './types';
import { ExtJS } from '../../../../interface/ExtJS';
import Permissions from '../../../../constants/Permissions';

export type DetectSortField = 'name' | 'signatures' | 'priority';
type SortDir = 'asc' | 'desc';

type DetectionState = 'not-enabled' | 'scanning' | 'active-clean' | 'active-signatures' | 'failed';
type ResponseState = 'na' | 'not-analyzed' | 'analyzing' | 'identified' | 'failed' | 'blocked';

export interface DetectRow {
  name: string;
  format: string;
  detection: DetectionState;
  signatureCount: number;
  rhcLastRun: string | null;
  rhcStartedAt: number | null;
  rhcError: string | null;
  response: ResponseState;
  responseTimestamp: string | null;
  responseError: string | null;
  findingCount: number;
  task: TaskInfo | null;
  hasRepoTask: boolean;
  sortRank: number;
  failCount: number;
}

export interface DetectTableProps {
  proxyRepos: ProxyRepo[];
  hcEnabledRepos: string[];
  countsByRepo: Record<string, number>;
  rhcScans: Map<string, RhcScanInfo>;
  tasks: TaskInfo[];
  activeFindings: MaliciousFinding[];
  identifyFailures?: Map<string, string>;
  onEnableRhc: (repoName: string) => Promise<void>;
  onIdentify: (repoName: string, signatureCount: number) => void;
  onNavigateToRemediate: (repoName: string) => void;
}

function formatTimestamp(iso: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleString(undefined, {
    month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit', hour12: true,
  });
}

function formatTimestampFromMs(ms: number): string {
  return formatTimestamp(new Date(ms).toISOString());
}

function deriveDetectionState(
  repo: ProxyRepo,
  rhcScan: RhcScanInfo | undefined,
  signatureCount: number,
): DetectionState {
  if (!repo.rhcSupported) return 'not-enabled';
  if (!repo.rhcEnabled && !rhcScan) return 'not-enabled';
  if (rhcScan?.phase === 'scanning') return 'scanning';
  if (rhcScan?.phase === 'failed') return 'failed';
  if (signatureCount > 0) return 'active-signatures';
  if (repo.rhcEnabled) return 'active-clean';
  return 'not-enabled';
}

/*
 * Deep Scan state truth table (repo-specific tasks only; global "all" tasks are ignored):
 *
 *  detection          | task   | repoTask | running | result | sigs | findings | taskFresh | => response
 *  -------------------|--------|----------|---------|--------|------|----------|-----------|------------
 *  not-enabled/scan/  | any    | any      | any     | any    | any  | any      | -         | na
 *    clean/failed     |        |          |         |        |      |          |           |
 *  active-signatures  | null   | false    | -       | -      | >0   | 0        | -         | not-analyzed
 *  active-signatures  | exists | true     | true    | -      | >0   | 0        | -         | analyzing
 *  active-signatures  | exists | true     | false   | !OK    | >0   | 0        | -         | failed
 *  active-signatures  | exists | true     | false   | OK     | >0   | 0        | false     | not-analyzed (stale task)
 *  active-signatures  | exists | true     | false   | OK     | >0   | 0        | true      | identified (sigs stale cache)
 *  active-signatures  | exists | true     | false   | OK     | >0   | >0       | -         | identified (threats)
 *  active-signatures  | exists | false    | -       | -      | >0   | >0       | -         | identified (threats)
 *
 * "taskFresh" = task.lastRun >= rhcCompletedAt (task ran after RHC found the current signatures).
 * Without this check, a task that ran days ago (before RHC detected anything) would show
 * "No Threats Confirmed" — a false negative that hides unscanned signatures.
 */
function isTaskFreshForScan(task: TaskInfo, rhcCompletedAt: number | null): boolean {
  if (!task.lastRun) return false;
  if (!rhcCompletedAt) return false;
  const taskRanAt = new Date(task.lastRun).getTime();
  if (Number.isNaN(taskRanAt)) return false;
  return taskRanAt >= rhcCompletedAt;
}

function deriveResponseState(
  detection: DetectionState,
  task: TaskInfo | null,
  findingCount: number,
  isRepoSpecificTask: boolean,
  signatureCount: number,
  rhcCompletedAt: number | null,
): ResponseState {
  if (detection === 'not-enabled' || detection === 'scanning' || detection === 'active-clean') return 'na';
  if (detection === 'failed') return 'na';

  if (findingCount > 0) return 'identified';

  if (!task || !isRepoSpecificTask) return 'not-analyzed';

  if (isSchedulerTaskRunningState(task.currentState)) return 'analyzing';
  if (task.lastRunResult && task.lastRunResult !== 'OK' && task.lastRunResult !== 'COMPLETED') return 'failed';

  if (task.lastRunResult === 'OK' || task.lastRunResult === 'COMPLETED') {
    if (signatureCount > 0 && !isTaskFreshForScan(task, rhcCompletedAt)) {
      return 'not-analyzed';
    }
    return 'identified';
  }

  if (process.env.NODE_ENV === 'development') {
    console.warn(
      `[DetectTable] Unexpected state: task=${task.id} detection=${detection} ` +
      `findings=${findingCount} sigs=${signatureCount} repoTask=${isRepoSpecificTask}`
    );
  }
  return 'not-analyzed';
}

function deriveResponseError(task: TaskInfo | null | undefined, failCount?: number): string | null {
  if (!task?.lastRunResult || task.lastRunResult === 'OK') return null;
  if (task.lastRunResult === 'CANCELED') {
    return 'Task was canceled by the server \u2014 verify Pro license and IQ Server connection';
  }
  const n = failCount ?? 1;
  if (n >= 3) {
    return `Task has failed ${n} times \u2014 contact your administrator`;
  }
  if (n >= 2) {
    return 'Task failed again \u2014 verify IQ Server connection in Administration > IQ Server';
  }
  return 'Task failed \u2014 open Administration > Tasks for logs';
}

function computeSortRank(detection: DetectionState, response: ResponseState): number {
  if (response === 'failed') return 0;
  if (detection === 'active-signatures' && response === 'not-analyzed') return 1;
  if (detection === 'not-enabled') return 2;
  if (response === 'identified') return 3;
  if (response === 'analyzing') return 4;
  if (detection === 'scanning') return 5;
  return 6;
}

export function useDetectRows(
  proxyRepos: ProxyRepo[],
  hcEnabledRepos: string[],
  countsByRepo: Record<string, number>,
  rhcScans: Map<string, RhcScanInfo>,
  tasks: TaskInfo[],
  activeFindings: MaliciousFinding[],
  identifyFailures?: Map<string, string>,
): DetectRow[] {
  return useMemo(() => {
    const tasksByRepo = new Map<string, TaskInfo>();
    for (const t of tasks) {
      if (t.repositoryName !== 'all') tasksByRepo.set(t.repositoryName, t);
    }

    const findingCountsByRepo = new Map<string, number>();
    for (const f of activeFindings) {
      if (!f.deletedTime && !f.acknowledgedAt) {
        findingCountsByRepo.set(f.repositoryName, (findingCountsByRepo.get(f.repositoryName) ?? 0) + 1);
      }
    }

    const rows: DetectRow[] = [];

    for (const repo of proxyRepos) {
      if (!repo.rhcSupported) continue;

      const signatureCount = countsByRepo[repo.name] ?? 0;
      const rhcScan = rhcScans.get(repo.name);
      const repoTask = tasksByRepo.get(repo.name) ?? null;
      const findingCount = findingCountsByRepo.get(repo.name) ?? 0;

      const detection = deriveDetectionState(repo, rhcScan, signatureCount);
      const rhcCompletedAt = rhcScan?.completedAt ?? null;
      let response = deriveResponseState(detection, repoTask, findingCount, !!repoTask, signatureCount, rhcCompletedAt);
      let responseError = deriveResponseError(repoTask);

      if (response === 'not-analyzed' && identifyFailures?.has(repo.name)) {
        response = 'failed';
        responseError = identifyFailures.get(repo.name) ?? 'Analysis failed';
      }

      const sortRank = computeSortRank(detection, response);

      const needsAttention =
        signatureCount > 0 ||
        findingCount > 0 ||
        detection === 'scanning' ||
        detection === 'failed' ||
        response === 'failed' ||
        response === 'analyzing';

      if (!needsAttention) continue;

      rows.push({
        name: repo.name,
        format: repo.format,
        detection,
        signatureCount,
        rhcLastRun: rhcScan?.completedAt ? new Date(rhcScan.completedAt).toISOString() : null,
        rhcStartedAt: rhcScan?.startedAt ?? null,
        rhcError: rhcScan?.error ?? null,
        response,
        responseTimestamp: repoTask?.lastRun ?? null,
        responseError,
        findingCount,
        task: repoTask,
        hasRepoTask: !!repoTask,
        sortRank,
        failCount: 0,
      });
    }

    for (const [name, scan] of rhcScans) {
      if (rows.some((r) => r.name === name)) continue;
      const repo = proxyRepos.find((r) => r.name === name);
      if (!repo) continue;
      const signatureCount = countsByRepo[name] ?? 0;
      const repoTask = tasksByRepo.get(name) ?? null;
      const findingCount = findingCountsByRepo.get(name) ?? 0;
      const detection = deriveDetectionState(repo, scan, signatureCount);
      const scanCompletedAt = scan.completedAt ?? null;
      let response = deriveResponseState(detection, repoTask, findingCount, !!repoTask, signatureCount, scanCompletedAt);
      let responseError = deriveResponseError(repoTask);

      if (response === 'not-analyzed' && identifyFailures?.has(name)) {
        response = 'failed';
        responseError = identifyFailures.get(name) ?? 'Analysis failed';
      }

      const scanNeedsAttention =
        signatureCount > 0 ||
        findingCount > 0 ||
        detection === 'scanning' ||
        detection === 'failed' ||
        response === 'failed' ||
        response === 'analyzing';

      if (!scanNeedsAttention) continue;

      rows.push({
        name,
        format: repo.format,
        detection,
        signatureCount,
        rhcLastRun: scan.completedAt ? new Date(scan.completedAt).toISOString() : null,
        rhcStartedAt: scan.startedAt ?? null,
        rhcError: scan.error ?? null,
        response,
        responseTimestamp: repoTask?.lastRun ?? null,
        responseError,
        findingCount,
        task: repoTask,
        hasRepoTask: !!repoTask,
        sortRank: computeSortRank(detection, response),
        failCount: 0,
      });
    }

    return rows;
  }, [proxyRepos, countsByRepo, rhcScans, tasks, activeFindings, identifyFailures]);
}

type RowFingerprint = `${DetectionState}|${ResponseState}|${number}`;

function fingerprint(row: DetectRow): RowFingerprint {
  return `${row.detection}|${row.response}|${row.findingCount}`;
}

function useChangedRows(rows: DetectRow[]): Set<string> {
  const prevRef = useRef<Map<string, RowFingerprint>>(new Map());
  const [changed, setChanged] = useState<Set<string>>(new Set());

  useEffect(() => {
    const prev = prevRef.current;
    const next = new Map<string, RowFingerprint>();
    const nowChanged = new Set<string>();

    for (const row of rows) {
      const fp = fingerprint(row);
      next.set(row.name, fp);
      const old = prev.get(row.name);
      if (old !== undefined && old !== fp) {
        nowChanged.add(row.name);
      }
    }

    prevRef.current = next;

    if (nowChanged.size > 0) {
      setChanged(nowChanged);
      const timer = setTimeout(() => setChanged(new Set()), 2000);
      return () => clearTimeout(timer);
    }
  }, [rows]);

  return changed;
}

const ROW_HIGHLIGHT_STYLE: React.CSSProperties = {
  animation: 'row-highlight-fade 2s ease-out',
  backgroundColor: 'var(--yellow-3)',
};

function DetectionCell({ row }: { row: DetectRow }): React.ReactElement {
  switch (row.detection) {
    case 'not-enabled':
      return <Badge variant="soft" color="gray">Not Enabled</Badge>;
    case 'scanning':
      return (
        <Flex direction="column" gap="1">
          <Flex align="center" gap="1">
            <Spinner size="1" />
            <Badge variant="soft" color="blue">Scanning</Badge>
          </Flex>
          {row.rhcStartedAt && (
            <Text size="1" color="gray">Started {formatTimestampFromMs(row.rhcStartedAt)}</Text>
          )}
        </Flex>
      );
    case 'active-clean':
      return (
        <Flex direction="column" gap="1">
          <Flex align="center" gap="2" wrap="wrap">
            <Badge variant="soft" color="green">Active</Badge>
            <Text size="2" color="green">No signatures</Text>
          </Flex>
          {row.rhcLastRun && <Text size="1" color="gray">Last run {formatTimestamp(row.rhcLastRun)}</Text>}
        </Flex>
      );
    case 'active-signatures':
      return (
        <Flex direction="column" gap="1">
          <Flex align="center" gap="2" wrap="wrap">
            <Badge variant="soft" color="green">Active</Badge>
            <Badge variant="soft" color="red">{row.signatureCount} signatures</Badge>
          </Flex>
          {row.rhcLastRun && <Text size="1" color="gray">Last run {formatTimestamp(row.rhcLastRun)}</Text>}
        </Flex>
      );
    case 'failed':
      return (
        <Flex direction="column" gap="1">
          <Flex align="center" gap="1">
            <XCircle size={10} color="var(--red-9)" />
            <Badge variant="soft" color="red">Failed</Badge>
          </Flex>
          <Text size="1" color="red">
            {row.rhcError || 'Repository Health Check could not be enabled'}
          </Text>
          {row.rhcStartedAt && (
            <Text size="1" color="gray">Initiated {formatTimestamp(new Date(row.rhcStartedAt).toISOString())}</Text>
          )}
          <Text
            size="1"
            color="blue"
            style={{ cursor: 'pointer', textDecoration: 'underline' }}
            onClick={() => window.open(`#admin/repository/repositories:${row.name}`, '_blank')}
            data-testid={`rhc-troubleshoot-${row.name}`}
          >
            View repository settings
          </Text>
        </Flex>
      );
  }
}

function ResponseCell({ row }: { row: DetectRow }): React.ReactElement {
  switch (row.response) {
    case 'na':
      return <Text size="2" color="gray">—</Text>;
    case 'not-analyzed':
      return <Text size="2" color="gray">Not Analyzed</Text>;
    case 'analyzing':
      return (
        <Flex direction="column" gap="1">
          <Flex align="center" gap="1" wrap="wrap">
            <Spinner size="1" />
            <Badge variant="soft" color="blue">
              Analyzing
              {row.task?.progress ? ` ${row.task.progress}` : ''}
            </Badge>
          </Flex>
          {row.responseTimestamp && (
            <Text size="1" color="gray">Started {formatTimestamp(row.responseTimestamp)}</Text>
          )}
          {row.task && (
            <Text
              size="1"
              color="blue"
              style={{ cursor: 'pointer', textDecoration: 'underline' }}
              onClick={() => window.open(`#admin/system/tasks:${row.task!.id}`, '_blank')}
              data-testid={`analyzing-task-log-link-${row.name}`}
            >
              View task log
            </Text>
          )}
        </Flex>
      );
    case 'identified':
      return (
        <Flex direction="column" gap="1">
          <Flex align="center" gap="1">
            {row.findingCount > 0 ? (
              <>
                <XCircle size={10} color="var(--red-9)" />
                <Badge variant="soft" color="red">{row.findingCount} Found</Badge>
              </>
            ) : (
              <>
                <CheckCircle size={10} color="var(--green-9)" />
                <Badge variant="soft" color="green">No Threats Confirmed</Badge>
              </>
            )}
          </Flex>
          {row.responseTimestamp && (
            <Text size="1" color="gray">{formatTimestamp(row.responseTimestamp)}</Text>
          )}
          {row.findingCount === 0 && row.signatureCount > 0 && (
            <Text size="1" color="gray">
              RHC signatures may take up to 6 hours to clear
            </Text>
          )}
        </Flex>
      );
    case 'failed':
      return (
        <Flex direction="column" gap="1">
          <Flex align="center" gap="1">
            <XCircle size={10} color="var(--red-9)" />
            <Badge variant="soft" color="red">Failed</Badge>
          </Flex>
          <Text size="1" color="red">
            {row.responseError || 'Analysis failed'}
          </Text>
          {row.task && (
            <Text
              size="1"
              color="blue"
              style={{ cursor: 'pointer', textDecoration: 'underline' }}
              onClick={() => window.open(`#admin/system/tasks:${row.task!.id}`, '_blank')}
              data-testid={`task-log-link-${row.name}`}
            >
              View task log
            </Text>
          )}
          {row.responseTimestamp && (
            <Text size="1" color="gray">Last attempt {formatTimestamp(row.responseTimestamp)}</Text>
          )}
        </Flex>
      );
    case 'blocked':
      return (
        <Flex direction="column" gap="1">
          <Badge variant="soft" color="gray">Blocked</Badge>
          <Text size="1" color="gray">Cleanup job in progress</Text>
        </Flex>
      );
  }
}

function FindingsCell({
  row,
  onNavigateToRemediate,
}: {
  row: DetectRow;
  onNavigateToRemediate: (repoName: string) => void;
}): React.ReactElement {
  if (row.response !== 'identified' || row.findingCount === 0) {
    return <Text size="2" color="gray">—</Text>;
  }

  return (
    <Button
      variant="ghost"
      size="1"
      color="red"
      style={{ fontWeight: 600, textDecoration: 'underline', cursor: 'pointer', padding: 0 }}
      onClick={() => onNavigateToRemediate(row.name)}
      data-testid={`findings-link-${row.name}`}
    >
      {row.findingCount} malicious {row.findingCount === 1 ? 'package' : 'packages'}
    </Button>
  );
}

function ActionCell({
  row,
  onEnableRhc,
  onIdentify,
  onNavigateToRemediate,
  canUpdateHealthCheck,
  canRunAnalysis,
}: {
  row: DetectRow;
  onEnableRhc: (repoName: string) => void;
  onIdentify: (repoName: string, signatureCount: number) => void;
  onNavigateToRemediate: (repoName: string) => void;
  canUpdateHealthCheck: boolean;
  canRunAnalysis: boolean;
}): React.ReactElement {
  // Hide detection-enable actions without healthcheck:update, and deep-scan (analysis)
  // actions without tasks:create (NEXUS-54212). Fall through to the neutral dash otherwise.
  if (row.detection === 'not-enabled') {
    return canUpdateHealthCheck ? (
      <Button size="1" variant="solid" color="red" onClick={() => onEnableRhc(row.name)} data-testid={`enable-rhc-${row.name}`}>
        Enable Detection
      </Button>
    ) : (
      <Text size="2" color="gray">—</Text>
    );
  }
  if (row.detection === 'scanning') {
    return <Text size="1" color="blue">Scanning…</Text>;
  }
  if (row.detection === 'failed') {
    return canUpdateHealthCheck ? (
      <Button size="1" variant="solid" color="red" onClick={() => onEnableRhc(row.name)} data-testid={`retry-rhc-${row.name}`}>
        Retry
      </Button>
    ) : (
      <Text size="2" color="gray">—</Text>
    );
  }
  if (row.response === 'not-analyzed' && row.signatureCount > 0) {
    return canRunAnalysis ? (
      <Button size="1" variant="solid" color="red" onClick={() => onIdentify(row.name, row.signatureCount)} data-testid={`identify-${row.name}`}>
        <Play size={12} /> Run One-Time Analysis
      </Button>
    ) : (
      <Text size="2" color="gray">—</Text>
    );
  }
  if (row.response === 'analyzing') {
    return <Text size="1" color="blue">Analyzing…</Text>;
  }
  if (row.response === 'failed') {
    if (!canRunAnalysis) {
      return <Text size="2" color="gray">—</Text>;
    }
    if (row.failCount >= 3) {
      return (
        <Tooltip content="Multiple failures detected. Check IQ Server connectivity and task logs before retrying.">
          <Button size="1" variant="outline" color="gray" disabled data-testid={`retry-analysis-${row.name}`}>
            Retry Analysis
          </Button>
        </Tooltip>
      );
    }
    return (
      <Button size="1" variant="solid" color="red" onClick={() => onIdentify(row.name, row.signatureCount)} data-testid={`retry-analysis-${row.name}`}>
        Retry Analysis
      </Button>
    );
  }
  if (row.response === 'identified' && row.findingCount > 0) {
    return (
      <Button size="1" variant="outline" color="red" onClick={() => onNavigateToRemediate(row.name)} data-testid={`view-results-${row.name}`}>
        View Results
      </Button>
    );
  }
  if (row.response === 'identified' && row.findingCount === 0) {
    return <Text size="1" color="green">Scan complete</Text>;
  }
  if (row.response === 'blocked') {
    return <Button size="1" variant="outline" color="gray">View Job</Button>;
  }
  return <Text size="2" color="gray">—</Text>;
}

export function DetectTable({
  proxyRepos,
  hcEnabledRepos,
  countsByRepo,
  rhcScans,
  tasks,
  activeFindings,
  identifyFailures,
  onEnableRhc,
  onIdentify,
  onNavigateToRemediate,
}: DetectTableProps): React.ReactElement {
  const [sortField, setSortField] = useState<DetectSortField>('name');
  const [sortDir, setSortDir] = useState<SortDir>('asc');
  const [confirmRepo, setConfirmRepo] = useState<string | null>(null);

  // Hide detection-enable actions without healthcheck:update, deep-scan actions without
  // tasks:create (NEXUS-54212). coreui never mounts a <PermissionsProvider>, so the
  // context-based usePermission would return false for everyone; use the provider-independent
  // ExtJS.usePermission (matches FindingsTable, the sibling in this page).
  const hasUser = ExtJS.useUser() ?? false;
  const canUpdateHealthCheck = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.HEALTHCHECK.UPDATE),
    [hasUser],
  );
  const canRunAnalysis = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.TASKS.CREATE),
    [hasUser],
  );

  const rawRows = useDetectRows(proxyRepos, hcEnabledRepos, countsByRepo, rhcScans, tasks, activeFindings, identifyFailures);

  const failCountRef = useRef<Map<string, number>>(new Map());
  const prevResponseRef = useRef<Map<string, ResponseState>>(new Map());

  useEffect(() => {
    for (const row of rawRows) {
      const prev = prevResponseRef.current.get(row.name);
      if (row.response === 'failed' && prev !== undefined && prev !== 'failed') {
        failCountRef.current.set(row.name, (failCountRef.current.get(row.name) ?? 0) + 1);
      }
      if (row.response !== 'failed' && prev === 'failed') {
        failCountRef.current.delete(row.name);
      }
      prevResponseRef.current.set(row.name, row.response);
    }
  }, [rawRows]);

  const rows = useMemo(() =>
    rawRows.map((row) => ({
      ...row,
      failCount: row.response === 'failed' ? Math.max(failCountRef.current.get(row.name) ?? 1, 1) : 0,
      responseError: row.response === 'failed'
        ? deriveResponseError(row.task, failCountRef.current.get(row.name) ?? 1)
        : row.responseError,
    })),
  [rawRows]);

  const changedRows = useChangedRows(rows);

  const sorted = useMemo(() => {
    const copy = [...rows];
    copy.sort((a, b) => {
      let cmp = 0;
      switch (sortField) {
        case 'name': cmp = a.name.localeCompare(b.name); break;
        case 'signatures': cmp = a.signatureCount - b.signatureCount; break;
        case 'priority': cmp = a.sortRank - b.sortRank; break;
      }
      return sortDir === 'desc' ? -cmp : cmp;
    });
    return copy;
  }, [rows, sortField, sortDir]);

  const toggleSort = (field: DetectSortField) => {
    if (sortField === field) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDir('asc');
    }
  };

  const handleEnableRhc = (repoName: string) => {
    setConfirmRepo(repoName);
  };

  const confirmEnableRhc = () => {
    if (confirmRepo) {
      onEnableRhc(confirmRepo);
      setConfirmRepo(null);
    }
  };

  const sortIndicator = (field: DetectSortField) =>
    sortField === field ? (sortDir === 'asc' ? ' \u2191' : ' \u2193') : '';

  const unmonitoredRepos = useMemo(() => {
    const result = proxyRepos.filter((r) => r.rhcSupported && !hcEnabledRepos.includes(r.name));
    result.sort((a, b) => a.name.localeCompare(b.name));
    return result;
  }, [proxyRepos, hcEnabledRepos]);

  return (
    <>
      <style>{`
        @keyframes row-highlight-fade {
          0% { background-color: var(--yellow-4); }
          100% { background-color: transparent; }
        }
      `}</style>

      {rows.length > 0 && (
        <Card size="2" data-testid="detect-table">
          <Flex direction="column" gap="3">
            <Flex align="center" gap="2">
              <AlertTriangle size={16} />
              <Heading as="h3" size="3">Repositories Needing Attention</Heading>
            </Flex>

            <Table.Root size="1" variant="surface">
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeaderCell style={{ cursor: 'pointer' }} onClick={() => toggleSort('name')}>
                    Repository{sortIndicator('name')}
                  </Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>
                    <Flex align="center" gap="1">
                      Initial Scan
                      <Tooltip content="Repository Health Check performs a lightweight scan to identify malicious package signatures without impacting repository performance. Runs continuously once enabled.">
                        <HelpCircle size={13} color="var(--gray-9)" style={{ cursor: 'help' }} />
                      </Tooltip>
                    </Flex>
                  </Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>
                    <Flex align="center" gap="1">
                      Deep Scan
                      <Tooltip content="Deep Scan uses Firewall analysis to identify specific malicious packages so you can research, delete, and assess blast radius. Runs on-demand for repositories with detected signatures.">
                        <HelpCircle size={13} color="var(--gray-9)" style={{ cursor: 'help' }} />
                      </Tooltip>
                    </Flex>
                  </Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>
                    Findings
                  </Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell style={{ textAlign: 'center' }}>
                    Action
                  </Table.ColumnHeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {sorted.map((row) => (
                  <Table.Row
                    key={row.name}
                    data-testid={`detect-row-${row.name}`}
                    style={changedRows.has(row.name) ? ROW_HIGHLIGHT_STYLE : undefined}
                  >
                    <Table.Cell>
                      <Text size="2" weight="medium">{row.name}</Text>
                    </Table.Cell>
                    <Table.Cell><DetectionCell row={row} /></Table.Cell>
                    <Table.Cell><ResponseCell row={row} /></Table.Cell>
                    <Table.Cell>
                      <FindingsCell row={row} onNavigateToRemediate={onNavigateToRemediate} />
                    </Table.Cell>
                    <Table.Cell style={{ textAlign: 'center' }}>
                      <ActionCell
                        row={row}
                        onEnableRhc={handleEnableRhc}
                        onIdentify={onIdentify}
                        onNavigateToRemediate={onNavigateToRemediate}
                        canUpdateHealthCheck={canUpdateHealthCheck}
                        canRunAnalysis={canRunAnalysis}
                      />
                    </Table.Cell>
                  </Table.Row>
                ))}
              </Table.Body>
            </Table.Root>
          </Flex>
        </Card>
      )}

      {unmonitoredRepos.length > 0 && (
        <Card size="2" data-testid="detect-unmonitored-table">
          <Flex direction="column" gap="3">
            <Heading as="h3" size="3">Unmonitored Repositories</Heading>
            <Text size="2" color="gray">
              These proxy repositories support malicious package detection but have not been enabled.
              Enable detection to scan for malicious package signatures.
            </Text>
            <Table.Root size="1" variant="surface">
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Format</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell style={{ textAlign: 'center' }}>Action</Table.ColumnHeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {unmonitoredRepos.map((repo) => (
                  <Table.Row key={repo.name} data-testid={`unmonitored-row-${repo.name}`}>
                    <Table.Cell>
                      <Text size="2" weight="medium">{repo.name}</Text>
                    </Table.Cell>
                    <Table.Cell>
                      <Badge variant="soft" color="gray">{repo.format}</Badge>
                    </Table.Cell>
                    <Table.Cell style={{ textAlign: 'center' }}>
                      {canUpdateHealthCheck ? (
                        <Button size="1" variant="solid" color="red" onClick={() => handleEnableRhc(repo.name)} data-testid={`enable-rhc-${repo.name}`}>
                          Enable Detection
                        </Button>
                      ) : (
                        <Text size="2" color="gray">—</Text>
                      )}
                    </Table.Cell>
                  </Table.Row>
                ))}
              </Table.Body>
            </Table.Root>
          </Flex>
        </Card>
      )}

      <Dialog.Root open={confirmRepo !== null} onOpenChange={(open) => { if (!open) setConfirmRepo(null); }}>
        <Dialog.Content style={{ maxWidth: 520 }}>
          <Dialog.Title>Enable RHC Detection</Dialog.Title>
          <Flex direction="column" gap="3">
            <Text size="2">
              Enable Repository Health Check for <Text weight="bold">{confirmRepo}</Text>. RHC will continuously
              monitor for malicious package signatures.
            </Text>
            <Separator size="4" />
            <Flex justify="end" gap="2">
              <Button variant="soft" color="gray" onClick={() => setConfirmRepo(null)}>Cancel</Button>
              <Button variant="solid" color="red" onClick={confirmEnableRhc}>Enable Detection</Button>
            </Flex>
          </Flex>
        </Dialog.Content>
      </Dialog.Root>

    </>
  );
}
