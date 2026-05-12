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

import React, { useState, useMemo, useCallback, useEffect } from 'react';
import {
  Table, Flex, Text, Badge, Button, Dialog, Heading, ScrollArea,
  Callout, Separator, Select, Skeleton, Box,
} from '@radix-ui/themes';
import {
  Trash2, Shield, ShieldCheck, Eye, Package, Info, CheckCircle, XCircle,
  Loader2, Clock, ExternalLink, ChevronRight, ChevronDown, X, Filter,
  AlertTriangle,
} from 'lucide-react';

import type { MaliciousFinding, ViewMode } from './types';
import { groupByRepository, groupByComponent, getFindingStatus, deriveComponentIdentity } from './types';
import {
  getRepoMalwareRemediatorBusyStatus,
  type RemediateResponse,
  type FindingsPage,
  type FindingsDateRange,
  type TaskInfo,
} from './useMaliciousPackagesData';
import { TablePagination } from '@/components/shared/TablePagination/TablePagination';
import { AcknowledgeDialog, type AcknowledgeDuration } from './AcknowledgeDialog';

function buildGuideUrl(finding: MaliciousFinding): string | null {
  const identity = deriveComponentIdentity(finding);
  if (!identity.name || identity.name === 'unknown') return null;
  const formatName = finding.format === 'maven2' ? 'maven' : finding.format;
  const namePart = encodeURIComponent(identity.name);
  const versionPart = identity.version ? `/${encodeURIComponent(identity.version)}` : '';
  return `https://guide.sonatype.com/component/${formatName}/${namePart}${versionPart}?referrer=repo-malicious-packages`;
}

const DATE_RANGE_OPTIONS: Array<{ value: FindingsDateRange; label: string; days: number }> = [
  { value: '1d', label: 'Last 24 hours', days: 1 },
  { value: '7d', label: 'Last 7 days', days: 7 },
  { value: '30d', label: 'Last 30 days', days: 30 },
  { value: '90d', label: 'Last 90 days', days: 90 },
  { value: 'forever', label: 'All time', days: 0 },
];

const DEFAULT_PAGE_SIZE = 20;

const STATUS_OPTIONS: Array<{ value: 'all' | 'pending' | 'deleted' | 'acknowledged'; label: string }> = [
  { value: 'all', label: 'All Statuses' },
  { value: 'pending', label: 'Pending' },
  { value: 'deleted', label: 'Remediated' },
  { value: 'acknowledged', label: 'Risk Accepted' },
];

type StatusFilterValue = 'all' | 'pending' | 'deleted' | 'acknowledged';

interface FindingsTableProps {
  onRemediateFindings: (findingIds: number[]) => Promise<RemediateResponse>;
  onRemediateRepository: (repoName: string) => Promise<void>;
  onAcknowledge: (findingId: number, reason: string, duration?: string) => Promise<void>;
  onBulkAcknowledge: (findingIds: number[], reason: string, duration?: string) => Promise<void>;
  fetchFindings: (sinceDays: number, limit: number, offset: number, repositoryName?: string) => Promise<FindingsPage>;
  signatureCount: number;
  /** Malicious Packages scheduler tasks (from Protect); used to disable delete-by-repo while a task is active. */
  tasks?: TaskInfo[];
  onDateRangeChange?: (label: string) => void;
  repoFilter?: string | null;
  onClearRepoFilter?: () => void;
  initialStatusFilter?: StatusFilterValue;
}

function getThreatBadge(threatLevel: number | null): React.ReactElement | null {
  if (threatLevel === null) return null;
  if (threatLevel >= 9) return <Badge color="red">Critical</Badge>;
  if (threatLevel >= 7) return <Badge color="orange">Severe</Badge>;
  if (threatLevel >= 4) return <Badge color="yellow">Moderate</Badge>;
  return <Badge color="gray">Low</Badge>;
}

function getStatusBadge(finding: MaliciousFinding): React.ReactElement {
  const status = getFindingStatus(finding);
  switch (status) {
    case 'deleted':
      return <Badge color="green">Remediated</Badge>;
    case 'acknowledged':
      return <Badge color="blue">Risk Accepted</Badge>;
    default:
      return <Badge color="orange">Pending</Badge>;
  }
}

function formatTimestamp(ts: string | null): string {
  if (!ts) return '';
  const d = new Date(ts);
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

type ModalState =
  | { type: 'closed' }
  | { type: 'finding-delete'; finding: MaliciousFinding }
  | { type: 'repo-review'; repoName: string; findings: MaliciousFinding[] }
  | { type: 'repo-delete'; repoName: string; findings: MaliciousFinding[] }
  | { type: 'component-review'; componentName: string; componentVersion: string | null; findings: MaliciousFinding[] }
  | { type: 'component-delete'; componentName: string; componentVersion: string | null; findings: MaliciousFinding[] };

interface DeleteStatus {
  [findingId: number]: 'pending' | 'deleting' | 'success' | 'error';
}

export function FindingsTable({
  onRemediateFindings,
  onRemediateRepository,
  onAcknowledge,
  onBulkAcknowledge,
  fetchFindings,
  signatureCount,
  tasks = [],
  onDateRangeChange,
  repoFilter,
  onClearRepoFilter,
  initialStatusFilter = 'pending',
}: FindingsTableProps): React.ReactElement {
  const [viewMode, setViewMode] = useState<ViewMode>('findings');
  const [dateRange, setDateRange] = useState<FindingsDateRange>('30d');
  const [statusFilter, setStatusFilter] = useState<StatusFilterValue>(initialStatusFilter);
  const [expandedFindings, setExpandedFindings] = useState<Set<number>>(new Set());
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [findings, setFindings] = useState<MaliciousFinding[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState<ModalState>({ type: 'closed' });
  const [deleteStatus, setDeleteStatus] = useState<DeleteStatus>({});
  const [bulkDeleting, setBulkDeleting] = useState(false);
  const [bulkDeleteDone, setBulkDeleteDone] = useState(false);
  const [bulkRepoDeleteError, setBulkRepoDeleteError] = useState<string | null>(null);
  /** Set while this repo's delete-by-repo flow is in flight (covers gap before tasks poll shows RUNNING). */
  const [repoRemediationPending, setRepoRemediationPending] = useState<string | null>(null);
  const [lastRemediationId, setLastRemediationId] = useState<string | null>(null);
  const [fetchError, setFetchError] = useState<string | null>(null);

  const sinceDays = useMemo(
    () => DATE_RANGE_OPTIONS.find((o) => o.value === dateRange)?.days ?? 30,
    [dateRange]
  );

  const loadFindings = useCallback(async () => {
    setLoading(true);
    setFetchError(null);
    try {
      const offset = (page - 1) * pageSize;
      const result = await fetchFindings(sinceDays, pageSize, offset, repoFilter ?? undefined);
      setFindings(result?.items ?? []);
      setTotalCount(result?.totalCount ?? 0);
    } catch (err) {
      setFindings([]);
      setTotalCount(0);
      setFetchError(err instanceof Error ? err.message : 'Failed to load findings -- check server logs');
    } finally {
      setLoading(false);
    }
  }, [fetchFindings, sinceDays, page, pageSize, repoFilter]);

  useEffect(() => {
    loadFindings();
  }, [loadFindings]);

  useEffect(() => {
    setPage(1);
  }, [repoFilter]);

  const handleDateRangeChange = useCallback((value: string) => {
    setDateRange(value as FindingsDateRange);
    setPage(1);
    const option = DATE_RANGE_OPTIONS.find((o) => o.value === value);
    if (option && onDateRangeChange) {
      onDateRangeChange(option.label.replace('Last ', '').toLowerCase());
    }
  }, [onDateRangeChange]);

  const handleStatusFilterChange = useCallback((value: string) => {
    setStatusFilter(value as StatusFilterValue);
    setPage(1);
  }, []);

  const handlePageSizeChange = useCallback((size: number) => {
    setPageSize(size);
    setPage(1);
  }, []);

  const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));

  const pendingFindings = useMemo(
    () => findings.filter((f) => !f.deletedTime && !f.acknowledgedAt),
    [findings]
  );

  const filteredFindings = useMemo(() => {
    let items = statusFilter === 'all' ? findings : findings.filter((f) => getFindingStatus(f) === statusFilter);
    items = [...items].sort((a, b) => {
      const statusRank = (f: MaliciousFinding) => {
        if (!f.deletedTime && !f.acknowledgedAt) return 0;
        if (f.acknowledgedAt) return 1;
        return 2;
      };
      return statusRank(a) - statusRank(b);
    });
    return items;
  }, [findings, statusFilter]);

  const repoGroups = useMemo(() => groupByRepository(filteredFindings), [filteredFindings]);
  const componentGroups = useMemo(() => groupByComponent(filteredFindings), [filteredFindings]);

  const handleDeleteSingle = useCallback(async (findingId: number) => {
    setDeleteStatus((prev) => ({ ...prev, [findingId]: 'deleting' }));
    try {
      const result = await onRemediateFindings([findingId]);
      const findingResult = result.results.find((r) => r.findingId === findingId);
      setDeleteStatus((prev) => ({
        ...prev,
        [findingId]: findingResult?.success ? 'success' : 'error',
      }));
      await loadFindings();
    } catch {
      setDeleteStatus((prev) => ({ ...prev, [findingId]: 'error' }));
    }
  }, [onRemediateFindings, loadFindings]);

  const handleDeleteAllRepo = useCallback(async (repoName: string) => {
    setRepoRemediationPending(repoName);
    setBulkDeleting(true);
    setLastRemediationId(null);
    setBulkDeleteDone(false);
    setBulkRepoDeleteError(null);
    try {
      await onRemediateRepository(repoName);
      setBulkDeleteDone(true);
      await loadFindings();
    } catch (err) {
      setBulkRepoDeleteError(err instanceof Error ? err.message : String(err));
    } finally {
      setBulkDeleting(false);
      setRepoRemediationPending(null);
    }
  }, [onRemediateRepository, loadFindings]);

  const handleDeleteAllComponent = useCallback(async (findingIds: number[]) => {
    setBulkDeleting(true);
    setLastRemediationId(null);
    setBulkDeleteDone(false);
    try {
      const result = await onRemediateFindings(findingIds);
      setLastRemediationId(result.remediationId ?? null);
      setBulkDeleteDone(true);
      await loadFindings();
    } finally {
      setBulkDeleting(false);
    }
  }, [onRemediateFindings, loadFindings]);

  const handleDeleteAll = useCallback(async () => {
    const ids = pendingFindings.map((f) => f.id);
    setBulkDeleting(true);
    setLastRemediationId(null);
    setBulkDeleteDone(false);
    try {
      let remId: string | null = null;
      for (let i = 0; i < ids.length; i += 50) {
        const chunk = ids.slice(i, i + 50);
        const result = await onRemediateFindings(chunk);
        if (!remId) remId = result.remediationId ?? null;
      }
      setLastRemediationId(remId);
      setBulkDeleteDone(true);
      await loadFindings();
    } finally {
      setBulkDeleting(false);
    }
  }, [pendingFindings, onRemediateFindings, loadFindings]);

  const toggleFindingExpanded = useCallback((findingId: number) => {
    setExpandedFindings((prev) => {
      const next = new Set(prev);
      if (next.has(findingId)) next.delete(findingId);
      else next.add(findingId);
      return next;
    });
  }, []);

  const resetModalState = useCallback(() => {
    setDeleteStatus({});
    setLastRemediationId(null);
    setBulkDeleteDone(false);
    setBulkRepoDeleteError(null);
  }, []);

  const openFindingDeleteModal = useCallback((finding: MaliciousFinding) => {
    resetModalState();
    setModal({ type: 'finding-delete', finding });
  }, [resetModalState]);

  const openRepoReviewModal = useCallback((repoName: string, repoFindings: MaliciousFinding[]) => {
    resetModalState();
    setModal({ type: 'repo-review', repoName, findings: repoFindings });
  }, [resetModalState]);

  const openRepoDeleteModal = useCallback((repoName: string, repoFindings: MaliciousFinding[]) => {
    resetModalState();
    setModal({ type: 'repo-delete', repoName, findings: repoFindings });
  }, [resetModalState]);

  const openComponentReviewModal = useCallback((componentName: string, componentVersion: string | null, compFindings: MaliciousFinding[]) => {
    resetModalState();
    setModal({ type: 'component-review', componentName, componentVersion, findings: compFindings });
  }, [resetModalState]);

  const openComponentDeleteModal = useCallback((componentName: string, componentVersion: string | null, compFindings: MaliciousFinding[]) => {
    resetModalState();
    setModal({ type: 'component-delete', componentName, componentVersion, findings: compFindings });
  }, [resetModalState]);

  const [ackTarget, setAckTarget] = useState<{ findingIds: number[] } | null>(null);

  const handleAcknowledgeConfirm = useCallback(async (reason: string, duration: AcknowledgeDuration) => {
    if (!ackTarget) return;
    try {
      if (ackTarget.findingIds.length === 1) {
        await onAcknowledge(ackTarget.findingIds[0], reason, duration);
      } else {
        await onBulkAcknowledge(ackTarget.findingIds, reason, duration);
      }
      await loadFindings();
    } finally {
      setAckTarget(null);
    }
  }, [ackTarget, onAcknowledge, onBulkAcknowledge, loadFindings]);

  const ackDialogFinding = useMemo(() => {
    if (!ackTarget || ackTarget.findingIds.length === 0) return null;
    return findings.find((f) => f.id === ackTarget.findingIds[0]) ?? null;
  }, [ackTarget, findings]);

  const renderModalFindingRow = (finding: MaliciousFinding, showRepo: boolean) => {
    const status = deleteStatus[finding.id];
    const mIdentity = deriveComponentIdentity(finding);
    const mDisplayName = mIdentity.version ? `${mIdentity.name}@${mIdentity.version}` : mIdentity.name;
    return (
      <Table.Row key={finding.id} data-testid={`modal-finding-${finding.id}`}>
        <Table.Cell>
          <Text size="2" weight="medium">{mDisplayName}</Text>
        </Table.Cell>
        {showRepo && (
          <Table.Cell>
            <Text size="2" color="gray">{finding.repositoryName}</Text>
          </Table.Cell>
        )}
        <Table.Cell>{getThreatBadge(finding.threatLevel)}</Table.Cell>
        <Table.Cell>
          <Text size="2" color="gray">{formatTimestamp(finding.firstDetectedAt)}</Text>
        </Table.Cell>
        <Table.Cell>
          {status === 'success' ? (
            <Flex align="center" gap="1"><CheckCircle size={14} color="var(--green-9)" /><Text size="1" color="green">Deleted</Text></Flex>
          ) : status === 'error' ? (
            <Flex align="center" gap="1"><XCircle size={14} color="var(--red-9)" /><Text size="1" color="red">Failed</Text></Flex>
          ) : status === 'deleting' ? (
            <Flex align="center" gap="1"><Loader2 size={14} className="animate-spin" /><Text size="1">Deleting...</Text></Flex>
          ) : (
            <Button size="1" variant="ghost" color="red" onClick={() => handleDeleteSingle(finding.id)} data-testid={`delete-finding-${finding.id}`}>
              <Trash2 size={14} />
              Delete
            </Button>
          )}
        </Table.Cell>
      </Table.Row>
    );
  };

  const isModalOpen = modal.type !== 'closed';
  /** Page-level bulk delete uses the same flag but no modal — do not trap other dialogs. */
  const blockModalDismissDuringBulk =
    bulkDeleting && (modal.type === 'repo-delete' || modal.type === 'component-delete');

  const renderFindingsView = () => (
    <Table.Root
      variant="surface"
      aria-label={`Malicious findings, ${filteredFindings.length} total`}
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Threat</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Status</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Detected</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Actions</Table.ColumnHeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {filteredFindings.map((finding) => {
          const identity = deriveComponentIdentity(finding);
          const displayName = identity.version
            ? `${identity.name}@${identity.version}`
            : identity.name;
          const isPending = !finding.deletedTime && !finding.acknowledgedAt;
          const isExpanded = expandedFindings.has(finding.id);

          return (
            <React.Fragment key={finding.id}>
              <Table.Row data-testid={`finding-row-${finding.id}`}>
                <Table.Cell>
                  <Flex align="center" gap="2">
                    <Button
                      type="button"
                      size="1"
                      variant="ghost"
                      color="gray"
                      aria-expanded={isExpanded}
                      aria-label={isExpanded ? 'Collapse threat details' : 'Expand threat details'}
                      onClick={() => toggleFindingExpanded(finding.id)}
                      data-testid={`expand-finding-${finding.id}`}
                    >
                      {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                    </Button>
                    <Shield size={14} />
                    <Text size="2">{finding.repositoryName}</Text>
                  </Flex>
                </Table.Cell>
                <Table.Cell>
                  <Text size="2" weight="medium">{displayName}</Text>
                </Table.Cell>
                <Table.Cell>{getThreatBadge(finding.threatLevel)}</Table.Cell>
                <Table.Cell>{getStatusBadge(finding)}</Table.Cell>
                <Table.Cell>
                  <Text size="2" color="gray">{formatTimestamp(finding.firstDetectedAt)}</Text>
                </Table.Cell>
                <Table.Cell>
                  {isPending && (
                    <Flex gap="2">
                      <Button
                        size="1"
                        color="red"
                        variant="soft"
                        onClick={() => openFindingDeleteModal(finding)}
                        data-testid={`delete-finding-btn-${finding.id}`}
                      >
                        <Trash2 size={14} />
                        Delete
                      </Button>
                      <Button
                        size="1"
                        color="blue"
                        variant="soft"
                        onClick={() => setAckTarget({ findingIds: [finding.id] })}
                        data-testid={`accept-risk-btn-${finding.id}`}
                      >
                        <ShieldCheck size={14} />
                        Accept Risk
                      </Button>
                    </Flex>
                  )}
                  {finding.deletedTime && (
                    <Text size="1" color="gray">
                      Deleted {formatTimestamp(finding.deletedTime)} by {finding.deletedBy ?? 'system'}
                    </Text>
                  )}
                  {finding.acknowledgedAt && (
                    <Text size="1" color="gray">
                      Risk Accepted {formatTimestamp(finding.acknowledgedAt)}
                      {finding.acknowledgedBy ? ` by ${finding.acknowledgedBy}` : ''}
                    </Text>
                  )}
                </Table.Cell>
              </Table.Row>
              {isExpanded && (
                <Table.Row data-testid={`finding-detail-row-${finding.id}`}>
                  <Table.Cell colSpan={6}>
                    <Box style={{ background: 'var(--gray-2)', borderRadius: 'var(--radius-2)', padding: '8px 12px' }}>
                      <Flex direction="column" gap="1">
                        {finding.threatSummary && <Text size="2">{finding.threatSummary}</Text>}
                        {(() => {
                          const guideUrl = buildGuideUrl(finding);
                          return guideUrl ? (
                            <Text size="2">
                              <a href={guideUrl} target="_blank" rel="noopener noreferrer">
                                Deep Research on Guide <ExternalLink size={12} />
                              </a>
                            </Text>
                          ) : null;
                        })()}
                        {finding.createdBy && (
                          <Text size="1" color="gray">
                            Uploaded by: {finding.createdBy}
                            {finding.createdByIp ? ` (${finding.createdByIp})` : ''}
                          </Text>
                        )}
                      </Flex>
                    </Box>
                  </Table.Cell>
                </Table.Row>
              )}
            </React.Fragment>
          );
        })}
      </Table.Body>
    </Table.Root>
  );

  const renderByComponentView = () => (
    <Table.Root
      variant="surface"
      aria-label={`Malicious findings by component, ${componentGroups.length} ${componentGroups.length === 1 ? 'component' : 'components'}`}
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Format</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Max Threat</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Repos</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Status</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Detected</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Actions</Table.ColumnHeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {componentGroups.map((group) => {
          const maxThreat = Math.max(...group.findings.map((f) => f.threatLevel ?? 0));
          const displayName = `${group.componentName}@${group.componentVersion ?? ''}`;
          const pendingInGroup = group.findings.filter((f) => !f.deletedTime && !f.acknowledgedAt);
          const allRemediated = pendingInGroup.length === 0;
          const earliestDetected = group.findings
            .map((f) => f.firstDetectedAt)
            .filter(Boolean)
            .sort()[0] ?? null;

          return (
            <Table.Row key={displayName} data-testid={`component-group-${group.componentName}`}>
              <Table.Cell>
                <Flex align="center" gap="2">
                  <Package size={16} />
                  <Text size="2" weight="bold">{displayName}</Text>
                </Flex>
              </Table.Cell>
              <Table.Cell>
                <Badge>{group.format}</Badge>
              </Table.Cell>
              <Table.Cell>
                {getThreatBadge(maxThreat)}
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">in {group.repositories.length} {group.repositories.length === 1 ? 'repo' : 'repos'}</Text>
              </Table.Cell>
              <Table.Cell>
                {allRemediated
                  ? <Badge color="green">Remediated</Badge>
                  : <Badge color="orange">Pending ({pendingInGroup.length})</Badge>}
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">{formatTimestamp(earliestDetected)}</Text>
              </Table.Cell>
              <Table.Cell>
                {allRemediated ? (
                  <Text size="1" color="gray">No action needed</Text>
                ) : (
                  <Flex gap="2">
                    <Button
                      size="1"
                      variant="soft"
                      onClick={() => openComponentReviewModal(group.componentName, group.componentVersion, group.findings)}
                      data-testid={`review-${group.componentName}`}
                    >
                      <Eye size={14} />
                      Review
                    </Button>
                    <Button
                      size="1"
                      color="red"
                      variant="soft"
                      onClick={() => openComponentDeleteModal(group.componentName, group.componentVersion, pendingInGroup)}
                      data-testid={`delete-${group.componentName}`}
                    >
                      <Trash2 size={14} />
                      Delete
                    </Button>
                    <Button
                      size="1"
                      color="blue"
                      variant="soft"
                      onClick={() => setAckTarget({ findingIds: pendingInGroup.map((f) => f.id) })}
                      data-testid={`accept-risk-${group.componentName}`}
                    >
                      <ShieldCheck size={14} />
                      Accept Risk
                    </Button>
                  </Flex>
                )}
              </Table.Cell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table.Root>
  );

  const renderByRepositoryView = () => (
    <Table.Root
      variant="surface"
      aria-label={`Malicious findings by repository, ${repoGroups.length} ${repoGroups.length === 1 ? 'repository' : 'repositories'}`}
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Max Threat</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Components</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Status</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Actions</Table.ColumnHeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {repoGroups.map((group) => {
          const maxThreat = Math.max(...group.findings.map((f) => f.threatLevel ?? 0));
          const uniqueComponents = new Set(
            group.findings.map((f) => {
              const id = deriveComponentIdentity(f);
              return `${id.name}@${id.version ?? ''}`;
            })
          );
          const pendingInRepo = group.findings.filter((f) => !f.deletedTime && !f.acknowledgedAt);
          const allRemediated = pendingInRepo.length === 0;
          const serverBusy = getRepoMalwareRemediatorBusyStatus(tasks, group.repositoryName);
          const localPending = repoRemediationPending === group.repositoryName;
          const deleteRepoBlocked = serverBusy != null || localPending;

          return (
            <Table.Row key={group.repositoryName} data-testid={`repo-group-${group.repositoryName}`}>
              <Table.Cell>
                <Flex align="center" gap="2">
                  <Shield size={14} />
                  <Text size="2" weight="bold">{group.repositoryName}</Text>
                </Flex>
              </Table.Cell>
              <Table.Cell>
                {getThreatBadge(maxThreat)}
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">
                  {uniqueComponents.size} {uniqueComponents.size === 1 ? 'Component' : 'Components'}
                </Text>
              </Table.Cell>
              <Table.Cell>
                {allRemediated
                  ? <Badge color="green">Remediated</Badge>
                  : <Badge color="orange">Pending ({pendingInRepo.length})</Badge>}
              </Table.Cell>
              <Table.Cell>
                {allRemediated ? (
                  <Text size="1" color="gray">No action needed</Text>
                ) : (
                  <Flex gap="2">
                    <Button
                      size="1"
                      variant="soft"
                      onClick={() => openRepoReviewModal(group.repositoryName, group.findings)}
                      data-testid={`review-repo-${group.repositoryName}`}
                    >
                      <Eye size={14} />
                      Review
                    </Button>
                    <Button
                      size="1"
                      color="red"
                      variant="soft"
                      onClick={() => openRepoDeleteModal(group.repositoryName, pendingInRepo)}
                      disabled={deleteRepoBlocked}
                      title={
                        deleteRepoBlocked
                          ? localPending
                            ? 'Remediation is starting for this repository.'
                            : 'A Malicious Packages task is running for this repository.'
                          : undefined
                      }
                      data-testid={`delete-repo-${group.repositoryName}`}
                    >
                      {deleteRepoBlocked ? (
                        <>
                          <Loader2 size={14} className="animate-spin" />
                          {localPending ? 'Starting…' : 'Task running'}
                        </>
                      ) : (
                        <>
                          <Trash2 size={14} />
                          Delete
                        </>
                      )}
                    </Button>
                    <Button
                      size="1"
                      color="blue"
                      variant="soft"
                      onClick={() => setAckTarget({ findingIds: pendingInRepo.map((f) => f.id) })}
                      data-testid={`accept-risk-repo-${group.repositoryName}`}
                    >
                      <ShieldCheck size={14} />
                      Accept Risk
                    </Button>
                  </Flex>
                )}
              </Table.Cell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table.Root>
  );

  return (
    <Flex direction="column" gap="3">
      <Flex direction="column" gap="3">
        <Flex justify="between" align="center" wrap="wrap" gap="3">
          <Heading size="4">Findings to Remediate</Heading>
          {pendingFindings.length > 0 && (
            <Button
              color="red"
              variant="soft"
              onClick={handleDeleteAll}
              disabled={bulkDeleting}
              data-testid="delete-all-malicious"
            >
              <Trash2 size={14} />
              Delete All Malicious ({pendingFindings.length})
            </Button>
          )}
        </Flex>

        {repoFilter && (
          <Flex align="center" gap="2" data-testid="repo-filter-chip">
            <Filter size={14} />
            <Badge color="blue" variant="soft" size="2">
              <Flex align="center" gap="1">
                Filtered: {repoFilter}
                <Button
                  variant="ghost"
                  size="1"
                  color="blue"
                  onClick={onClearRepoFilter}
                  style={{ padding: '0 2px', minWidth: 'auto', cursor: 'pointer' }}
                  data-testid="clear-repo-filter"
                >
                  <X size={12} />
                </Button>
              </Flex>
            </Badge>
          </Flex>
        )}

        <Flex justify="between" align="center" wrap="wrap" gap="3">
          <Flex gap="2">
            <Button
              variant={viewMode === 'findings' ? 'solid' : 'outline'}
              size="2"
              onClick={() => setViewMode('findings')}
              data-testid="tab-findings"
            >
              Findings
            </Button>
            <Button
              variant={viewMode === 'by-component' ? 'solid' : 'outline'}
              size="2"
              onClick={() => setViewMode('by-component')}
              data-testid="tab-by-component"
            >
              By Component
            </Button>
            <Button
              variant={viewMode === 'by-repository' ? 'solid' : 'outline'}
              size="2"
              onClick={() => setViewMode('by-repository')}
              data-testid="tab-by-repository"
            >
              By Repository
            </Button>
          </Flex>
          <Flex align="center" gap="3" wrap="wrap">
            <Flex align="center" gap="2">
              <Clock size={14} />
              <Select.Root value={dateRange} onValueChange={handleDateRangeChange}>
                <Select.Trigger data-testid="date-range-select" />
                <Select.Content>
                  {DATE_RANGE_OPTIONS.map((opt) => (
                    <Select.Item key={opt.value} value={opt.value}>{opt.label}</Select.Item>
                  ))}
                </Select.Content>
              </Select.Root>
            </Flex>
            <Flex align="center" gap="2">
              <Select.Root value={statusFilter} onValueChange={handleStatusFilterChange}>
                <Select.Trigger data-testid="status-filter-select" placeholder="Status" />
                <Select.Content>
                  {STATUS_OPTIONS.map((opt) => (
                    <Select.Item key={opt.value} value={opt.value}>{opt.label}</Select.Item>
                  ))}
                </Select.Content>
              </Select.Root>
            </Flex>
          </Flex>
        </Flex>
      </Flex>

      {fetchError ? (
        <Callout.Root color="red" data-testid="findings-fetch-error">
          <Callout.Icon><XCircle size={16} /></Callout.Icon>
          <Callout.Text>
            {fetchError}
            <Button variant="ghost" size="1" color="red" onClick={loadFindings} style={{ marginLeft: 8, textDecoration: 'underline', cursor: 'pointer' }}>
              Retry
            </Button>
          </Callout.Text>
        </Callout.Root>
      ) : loading ? (
        <Flex direction="column" gap="2">
          <Skeleton height="40px" />
          <Skeleton height="40px" />
          <Skeleton height="40px" />
        </Flex>
      ) : findings.length === 0 ? (
        signatureCount > 0 ? (
          <Callout.Root color="amber" data-testid="signatures-callout">
            <Callout.Icon><Info size={16} /></Callout.Icon>
            <Callout.Text>
              <Text weight="bold">{signatureCount} malicious package signatures</Text> detected.
              Run remediation tasks on impacted repositories above to confirm and identify specific components.
            </Callout.Text>
          </Callout.Root>
        ) : (
          <Callout.Root color="gray" data-testid="no-findings-callout">
            <Callout.Icon><Info size={16} /></Callout.Icon>
            <Callout.Text>No findings in the selected time range.</Callout.Text>
          </Callout.Root>
        )
      ) : filteredFindings.length === 0 ? (
        <Callout.Root color="gray" data-testid="status-filter-empty-callout">
          <Callout.Icon><Info size={16} /></Callout.Icon>
          <Callout.Text>No findings match the selected status filter.</Callout.Text>
        </Callout.Root>
      ) : (
        <>
          {pendingFindings.length > 0 && (
            <Callout.Root color="amber" data-testid="pending-findings-banner">
              <Callout.Icon><Info size={16} /></Callout.Icon>
              <Callout.Text>
                <Text weight="bold">{pendingFindings.length}</Text> of {findings.length} findings on this page require remediation.
              </Callout.Text>
            </Callout.Root>
          )}

          {viewMode === 'findings' && renderFindingsView()}
          {viewMode === 'by-component' && renderByComponentView()}
          {viewMode === 'by-repository' && renderByRepositoryView()}

          <TablePagination
            currentPage={page}
            totalPages={totalPages}
            itemsPerPage={pageSize}
            totalItems={totalCount}
            onPageChange={setPage}
            onItemsPerPageChange={handlePageSizeChange}
            mt="0"
          />
        </>
      )}

      <Dialog.Root
        open={isModalOpen}
        onOpenChange={(open) => {
          if (!open && blockModalDismissDuringBulk) {
            return;
          }
          if (!open) setModal({ type: 'closed' });
        }}
      >
        <Dialog.Content
          maxWidth="700px"
          data-testid="remediate-modal"
          onInteractOutside={(e) => {
            if (blockModalDismissDuringBulk) e.preventDefault();
          }}
          onEscapeKeyDown={(e) => {
            if (blockModalDismissDuringBulk) e.preventDefault();
          }}
        >

          {modal.type === 'finding-delete' && (() => {
            const f = modal.finding;
            const identity = deriveComponentIdentity(f);
            const name = identity.version ? `${identity.name}@${identity.version}` : identity.name;
            const status = deleteStatus[f.id];
            return (
              <>
                <Dialog.Title>Delete malicious package</Dialog.Title>
                <Dialog.Description size="2" color="gray">
                  Permanently delete <Text weight="bold">{name}</Text> from <Text weight="bold">{f.repositoryName}</Text>.
                </Dialog.Description>

                {status === 'success' ? (
                  <Callout.Root color="green" my="3" data-testid="finding-delete-success">
                    <Callout.Icon><CheckCircle size={16} /></Callout.Icon>
                    <Callout.Text>Package deleted and recorded in audit log.</Callout.Text>
                  </Callout.Root>
                ) : (
                  <Callout.Root color="amber" my="3">
                    <Callout.Icon><Shield size={16} /></Callout.Icon>
                    <Callout.Text>
                      This deletion is recorded in the audit log for traceability.
                    </Callout.Text>
                  </Callout.Root>
                )}

                <Table.Root variant="surface" size="1" my="3">
                  <Table.Body>
                    <Table.Row>
                      <Table.Cell><Text size="2" color="gray">Component</Text></Table.Cell>
                      <Table.Cell><Text size="2" weight="medium">{name}</Text></Table.Cell>
                    </Table.Row>
                    <Table.Row>
                      <Table.Cell><Text size="2" color="gray">Repository</Text></Table.Cell>
                      <Table.Cell><Text size="2">{f.repositoryName}</Text></Table.Cell>
                    </Table.Row>
                    <Table.Row>
                      <Table.Cell><Text size="2" color="gray">Threat</Text></Table.Cell>
                      <Table.Cell>{getThreatBadge(f.threatLevel)}</Table.Cell>
                    </Table.Row>
                    <Table.Row>
                      <Table.Cell><Text size="2" color="gray">Detected</Text></Table.Cell>
                      <Table.Cell><Text size="2">{formatTimestamp(f.firstDetectedAt)}</Text></Table.Cell>
                    </Table.Row>
                  </Table.Body>
                </Table.Root>

                <Separator my="3" size="4" />

                <Flex gap="3" justify="end">
                  <Dialog.Close>
                    <Button variant="soft" color="gray">{status === 'success' ? 'Close' : 'Cancel'}</Button>
                  </Dialog.Close>
                  {status !== 'success' && (
                    <Button
                      color="red"
                      onClick={() => handleDeleteSingle(f.id)}
                      disabled={status === 'deleting'}
                      data-testid="confirm-delete-finding"
                    >
                      {status === 'deleting'
                        ? <><Loader2 size={14} className="animate-spin" /> Deleting...</>
                        : <><Trash2 size={14} /> Delete</>
                      }
                    </Button>
                  )}
                </Flex>
              </>
            );
          })()}

          {(modal.type === 'repo-review' || modal.type === 'repo-delete') && (
            <>
              <Dialog.Title>
                {modal.type === 'repo-delete'
                  ? `Delete ${modal.findings.length} malicious packages from ${modal.repoName}`
                  : `${modal.repoName} -- ${modal.findings.length} malicious ${modal.findings.length === 1 ? 'package' : 'packages'}`}
              </Dialog.Title>
              <Dialog.Description size="2" color="gray">
                {modal.type === 'repo-delete'
                  ? 'This will permanently delete the listed assets from the repository.'
                  : 'Review malicious packages found in this repository. Delete individually or close and use Delete to remove all.'}
              </Dialog.Description>

              {modal.type === 'repo-delete' && !bulkDeleteDone && (
                <Callout.Root color="amber" my="3">
                  <Callout.Icon><Shield size={16} /></Callout.Icon>
                  <Callout.Text>
                    Each deletion is recorded in the audit log. A <Text weight="bold">Remediation ID</Text> will be
                    assigned to this action for traceability.
                  </Callout.Text>
                </Callout.Root>
              )}

              {bulkRepoDeleteError && (
                <Callout.Root color="red" my="3" data-testid="repo-delete-error">
                  <Callout.Icon><AlertTriangle size={16} /></Callout.Icon>
                  <Callout.Text>{bulkRepoDeleteError}</Callout.Text>
                </Callout.Root>
              )}

              {modal.type === 'repo-delete' && !bulkDeleteDone && !bulkDeleting && (() => {
                const mb = getRepoMalwareRemediatorBusyStatus(tasks, modal.repoName);
                if (mb == null) return null;
                return (
                  <Callout.Root color="amber" my="3" data-testid="repo-delete-task-already-running">
                    <Callout.Icon><Loader2 size={16} className="animate-spin" /></Callout.Icon>
                    <Callout.Text>
                      A Malicious Packages task is already running for this repository.{' '}
                      Check <Text weight="bold">Administration → Tasks</Text>. You cannot start another remediation until it
                      finishes.
                    </Callout.Text>
                  </Callout.Root>
                );
              })()}

              {modal.type === 'repo-delete' && bulkDeleting && !bulkDeleteDone && (
                <Callout.Root color="blue" my="3" data-testid="repo-remediation-progress">
                  <Callout.Icon><Loader2 size={16} className="animate-spin" /></Callout.Icon>
                  <Callout.Text>
                    Remediation runs as a server task (Malicious Packages) and often takes a minute or longer while IQ Server
                    and Nexus delete assets. Keep this dialog open until it finishes. You can also watch progress under{' '}
                    <Text weight="bold">Administration → Tasks</Text>.
                  </Callout.Text>
                </Callout.Root>
              )}

              {bulkDeleteDone && (
                <Callout.Root color="green" my="3" data-testid="remediation-id-banner">
                  <Callout.Icon><CheckCircle size={16} /></Callout.Icon>
                  <Callout.Text>
                    Deletion task submitted. The server is removing {modal.findings.length}{' '}
                    {modal.findings.length === 1 ? 'package' : 'packages'} from{' '}
                    <Text weight="bold">{modal.repoName}</Text> in the background.
                    Monitor progress under <Text weight="bold">Administration → Tasks</Text>.
                    Findings will disappear from this view once deletion completes.
                  </Callout.Text>
                </Callout.Root>
              )}

              {!bulkDeleteDone && (
                <ScrollArea style={{ maxHeight: 400 }}>
                  <Table.Root variant="surface" size="1">
                    <Table.Header>
                      <Table.Row>
                        <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Threat</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Introduced</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Action</Table.ColumnHeaderCell>
                      </Table.Row>
                    </Table.Header>
                    <Table.Body>
                      {modal.findings.map((f) => renderModalFindingRow(f, false))}
                    </Table.Body>
                  </Table.Root>
                </ScrollArea>
              )}

              {bulkDeleteDone && (
                <Box my="3" p="3" style={{ background: 'var(--green-2)', borderRadius: 'var(--radius-2)' }}>
                  <Flex direction="column" gap="1">
                    {modal.findings.map((f) => {
                      const identity = deriveComponentIdentity(f);
                      const name = identity.version ? `${identity.name}@${identity.version}` : identity.name;
                      return (
                        <Flex key={f.id} align="center" gap="2">
                          <Loader2 size={14} className="animate-spin" color="var(--green-9)" />
                          <Text size="2" color="green">{name} — queued for deletion</Text>
                        </Flex>
                      );
                    })}
                  </Flex>
                </Box>
              )}

              <Separator my="3" size="4" />

              <Flex gap="3" justify="end">
                {bulkDeleting && modal.type === 'repo-delete' ? (
                  <Button variant="soft" color="gray" disabled>
                    Cancel
                  </Button>
                ) : (
                  <Dialog.Close>
                    <Button variant="soft" color="gray">{bulkDeleteDone || modal.type === 'repo-review' ? 'Close' : 'Cancel'}</Button>
                  </Dialog.Close>
                )}
                {modal.type === 'repo-delete' && !bulkDeleteDone && (() => {
                  const modalRepoBusy = getRepoMalwareRemediatorBusyStatus(tasks, modal.repoName);
                  const confirmDisabled = bulkDeleting || modalRepoBusy != null;
                  return (
                    <Button
                      color="red"
                      onClick={() => handleDeleteAllRepo(modal.repoName)}
                      disabled={confirmDisabled}
                      title={
                        modalRepoBusy != null
                          ? 'A Malicious Packages task is running for this repository.'
                          : undefined
                      }
                      data-testid="confirm-delete-all-repo"
                    >
                      {bulkDeleting ? (
                        <><Loader2 size={14} className="animate-spin" /> Deleting...</>
                      ) : modalRepoBusy != null ? (
                        <><Loader2 size={14} className="animate-spin" /> Task running</>
                      ) : (
                        <><Trash2 size={14} /> Delete All in This Repo</>
                      )}
                    </Button>
                  );
                })()}
              </Flex>
            </>
          )}

          {(modal.type === 'component-review' || modal.type === 'component-delete') && (
            <>
              <Dialog.Title>
                {modal.type === 'component-delete' ? 'Delete ' : ''}
                {modal.componentName}@{modal.componentVersion ?? ''}
              </Dialog.Title>
              <Dialog.Description size="2" color="gray">
                {modal.type === 'component-delete'
                  ? 'This will permanently delete this component from all listed repositories.'
                  : `Found in ${modal.findings.length} location(s) across repositories.`}
              </Dialog.Description>

              {modal.type === 'component-delete' && !bulkDeleteDone && (
                <Callout.Root color="amber" my="3">
                  <Callout.Icon><Shield size={16} /></Callout.Icon>
                  <Callout.Text>
                    Each deletion is recorded in the audit log. A <Text weight="bold">Remediation ID</Text> will be
                    assigned to this action for traceability.
                  </Callout.Text>
                </Callout.Root>
              )}

              {modal.type === 'component-delete' && bulkDeleting && !bulkDeleteDone && (
                <Callout.Root color="blue" my="3" data-testid="component-bulk-delete-progress">
                  <Callout.Icon><Loader2 size={16} className="animate-spin" /></Callout.Icon>
                  <Callout.Text>
                    Deleting across repositories can take a while. Keep this dialog open until it completes.
                  </Callout.Text>
                </Callout.Root>
              )}

              {bulkDeleteDone && (
                <Callout.Root color="green" my="3" data-testid="remediation-id-banner">
                  <Callout.Icon><CheckCircle size={16} /></Callout.Icon>
                  <Callout.Text>
                    Remediation complete.{' '}
                    Remediation ID: <Text weight="bold" style={{ fontFamily: 'monospace' }}>{lastRemediationId ?? 'Not available'}</Text>
                    {' '}-- record this for audit reference.
                  </Callout.Text>
                </Callout.Root>
              )}

              <Separator my="3" size="4" />

              {!bulkDeleteDone ? (
                <ScrollArea style={{ maxHeight: 400 }}>
                  <Table.Root variant="surface" size="1">
                    <Table.Header>
                      <Table.Row>
                        <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Threat</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Introduced</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Action</Table.ColumnHeaderCell>
                      </Table.Row>
                    </Table.Header>
                    <Table.Body>
                      {modal.findings.map((f) => renderModalFindingRow(f, true))}
                    </Table.Body>
                  </Table.Root>
                </ScrollArea>
              ) : (
                <Box my="3" p="3" style={{ background: 'var(--green-2)', borderRadius: 'var(--radius-2)' }}>
                  <Flex direction="column" gap="1">
                    {modal.findings.map((f) => {
                      const identity = deriveComponentIdentity(f);
                      const name = identity.version ? `${identity.name}@${identity.version}` : identity.name;
                      return (
                        <Flex key={f.id} align="center" gap="2">
                          <CheckCircle size={14} color="var(--green-9)" />
                          <Text size="2" color="green">{name} ({f.repositoryName}) — deleted</Text>
                        </Flex>
                      );
                    })}
                  </Flex>
                </Box>
              )}

              <Separator my="3" size="4" />

              <Flex gap="3" justify="end">
                {bulkDeleting && modal.type === 'component-delete' ? (
                  <Button variant="soft" color="gray" disabled>
                    Cancel
                  </Button>
                ) : (
                  <Dialog.Close>
                    <Button variant="soft" color="gray">{modal.type === 'component-review' || bulkDeleteDone ? 'Close' : 'Cancel'}</Button>
                  </Dialog.Close>
                )}
                {modal.type === 'component-delete' && !bulkDeleteDone && (
                  <Button
                    color="red"
                    onClick={() => handleDeleteAllComponent(modal.findings.map((f) => f.id))}
                    disabled={bulkDeleting}
                    data-testid="confirm-delete-all-component"
                  >
                    {bulkDeleting ? <><Loader2 size={14} className="animate-spin" /> Deleting...</> : <><Trash2 size={14} /> Delete from All Repos</>}
                  </Button>
                )}
              </Flex>
            </>
          )}
        </Dialog.Content>
      </Dialog.Root>

      <AcknowledgeDialog
        open={ackTarget !== null}
        finding={ackDialogFinding}
        onConfirm={handleAcknowledgeConfirm}
        onCancel={() => setAckTarget(null)}
      />
    </Flex>
  );
}
