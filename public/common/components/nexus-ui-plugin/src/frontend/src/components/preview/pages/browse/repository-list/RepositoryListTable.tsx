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

import React, { useCallback } from 'react';
import {
  Box,
  Callout,
  DropdownMenu,
  Flex,
  IconButton,
  Spinner,
  Table,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import { Copy, MoreHorizontal, X } from 'lucide-react';

import type { Repository, HealthCheckStatus, FirewallStatus } from './repository-list.types';
import { HealthCheckCell } from '../../../shared/security/HealthCheckCell';
import { FirewallCell } from '../../../shared/security/FirewallCell';
import {
  SortableTableHeader,
  useToast,
  type SortDirection,
} from '../../../shared';
import { FORMAT_LABELS } from '../../settings/repository/repositories/types';
import { ensureTrailingSlash } from '../../../../../utils/url';

import './RepositoryListTable.scss';

/**
 * UI Strings.
 */
const STRINGS = {
  columns: {
    name: 'Name',
    type: 'Type',
    format: 'Ecosystem',
    status: 'Status',
    url: 'URL',
    healthCheck: 'Health Check',
    iqPolicyViolations: 'Firewall Report',
  },
  emptyMessageFiltered: 'No repositories match the current filters',
  emptyMessageNoRepos: 'No repositories available. You may need to sign in or check your permissions.',
  copyUrlTitle: 'Copy URL to Clipboard',
  urlCopiedMessage: 'URL Copied to Clipboard',
  urlCopyFailedMessage: 'Failed to copy URL to clipboard',
  loadingMessage: 'Loading repositories...',
  browseRepositoryAction: 'Browse Repository',
  copyUrlAction: 'Copy URL',
};

export interface RepositoryListTableProps {
  /** Repositories to display */
  repositories: readonly Repository[];
  /** Loading state */
  loading?: boolean;
  /** Error message */
  error?: string;
  /** Callback when a repository is selected */
  onSelect?: (repositoryName: string) => void;
  /** Health check statuses by repository name */
  healthCheck?: Record<string, HealthCheckStatus>;
  /** Firewall statuses by repository name */
  firewallStatus?: Record<string, FirewallStatus>;
  /** Whether the firewall summary API has finished loading */
  firewallLoaded?: boolean;
  /** Show health check column */
  showHealthCheck?: boolean;
  /** Show IQ policy violations column */
  showIqPolicyViolations?: boolean;
  /** Whether any filters are currently active */
  hasFilters?: boolean;
  /** Callback when analyze is clicked */
  onAnalyze?: (repositoryName: string) => void;
  /** Set of repository names being analyzed */
  analyzingRepos?: Set<string>;
  /** Whether user has Firewall license */
  hasFirewallLicense?: boolean;
  /** Summary of proxy protection status */
  proxyProtectionSummary?: { totalProxy: number; protectedProxy: number };
  /** Current sort field (for server-side sort) */
  sortKey?: string;
  /** Current sort direction */
  sortDirection?: SortDirection;
  /** Callback when sort changes (from header click) */
  onSort?: (key: string, direction: SortDirection) => void;
}

/**
 * RepositoryListTable - Simple table display for repositories.
 */
export function RepositoryListTable({
  repositories,
  loading = false,
  error,
  onSelect,
  healthCheck = {},
  firewallStatus = {},
  firewallLoaded = false,
  showHealthCheck = false,
  showIqPolicyViolations = false,
  hasFilters = false,
  onAnalyze,
  analyzingRepos = new Set(),
  hasFirewallLicense = true,
  proxyProtectionSummary,
  sortKey = 'name',
  sortDirection = 'asc',
  onSort,
}: RepositoryListTableProps): JSX.Element {
  const toast = useToast();

  // Handle sort - must be declared before any early returns (Rules of Hooks)
  const handleSort = useCallback(
    (key: string, direction: SortDirection) => {
      onSort?.(key, direction);
    },
    [onSort],
  );

  // Handle row click
  const handleRowClick = (name: string) => () => {
    onSelect?.(name);
  };

  // Handle row keyboard navigation
  const handleRowKeyDown = (name: string) => (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onSelect?.(name);
    }
  };

  // Handle copy URL
  const handleCopyUrl = (url: string) => (e: React.MouseEvent) => {
    e.stopPropagation();
    const urlWithSlash = ensureTrailingSlash(url);
    navigator.clipboard.writeText(urlWithSlash)
      .then(() => {
        toast.success(STRINGS.urlCopiedMessage);
      })
      .catch(() => {
        toast.error(STRINGS.urlCopyFailedMessage);
      });
  };

  // Loading state
  if (loading) {
    return (
      <Flex direction="column" align="center" justify="center" gap="3" p="9">
        <Spinner size="3" />
        <Text color="gray">{STRINGS.loadingMessage}</Text>
      </Flex>
    );
  }

  // Error state
  if (error) {
    return (
      <Box p="4">
        <Callout.Root color="red">
          <Callout.Icon>
            <X size={16} />
          </Callout.Icon>
          <Callout.Text>{error}</Callout.Text>
        </Callout.Root>
      </Box>
    );
  }

  // Column count for empty state: name, type, format, status, url, + optional (health, IQ), + actions.
  const colCount = 6 + (showHealthCheck ? 1 : 0) + (showIqPolicyViolations ? 1 : 0);

  return (
    <Table.Root className="repository-list-table" variant="surface" size="2">
      <Table.Header>
        <Table.Row>
          {onSort ? (
            <SortableTableHeader
              sortKey="name"
              currentSortKey={sortKey}
              currentSortDirection={sortDirection}
              onSort={handleSort}
              align="left"
            >
              {STRINGS.columns.name}
            </SortableTableHeader>
          ) : (
            <Table.ColumnHeaderCell>
              {STRINGS.columns.name}
            </Table.ColumnHeaderCell>
          )}
          {onSort ? (
            <SortableTableHeader
              sortKey="type"
              currentSortKey={sortKey}
              currentSortDirection={sortDirection}
              onSort={handleSort}
              align="left"
            >
              {STRINGS.columns.type}
            </SortableTableHeader>
          ) : (
            <Table.ColumnHeaderCell>
              {STRINGS.columns.type}
            </Table.ColumnHeaderCell>
          )}
          {onSort ? (
            <SortableTableHeader
              sortKey="format"
              currentSortKey={sortKey}
              currentSortDirection={sortDirection}
              onSort={handleSort}
              align="left"
            >
              {STRINGS.columns.format}
            </SortableTableHeader>
          ) : (
            <Table.ColumnHeaderCell>
              {STRINGS.columns.format}
            </Table.ColumnHeaderCell>
          )}
          {onSort ? (
            <SortableTableHeader
              sortKey="status"
              currentSortKey={sortKey}
              currentSortDirection={sortDirection}
              onSort={handleSort}
              align="left"
            >
              {STRINGS.columns.status}
            </SortableTableHeader>
          ) : (
            <Table.ColumnHeaderCell>
              {STRINGS.columns.status}
            </Table.ColumnHeaderCell>
          )}
          <Table.ColumnHeaderCell>
            {STRINGS.columns.url}
          </Table.ColumnHeaderCell>
          {showHealthCheck && (
            <Table.ColumnHeaderCell className="table-cell-centered">
              {STRINGS.columns.healthCheck}
            </Table.ColumnHeaderCell>
          )}
          {showIqPolicyViolations && (
            <Table.ColumnHeaderCell className="table-cell-centered">
              {STRINGS.columns.iqPolicyViolations}
            </Table.ColumnHeaderCell>
          )}
          {/* paddingRight: 32 matches the Rapture-safe right gutter used across list tables (see EntityTable.scss). */}
          <Table.ColumnHeaderCell
            width="80px"
            justify="end"
            aria-label="Row actions"
          />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {repositories.length === 0 ? (
          <Table.Row>
            <Table.Cell colSpan={colCount}>
              <Flex justify="center" p="6">
                <Text color="gray">
                  {hasFilters ? STRINGS.emptyMessageFiltered : STRINGS.emptyMessageNoRepos}
                </Text>
              </Flex>
            </Table.Cell>
          </Table.Row>
        ) : (
          repositories.map((repo) => (
            <Table.Row
              key={repo.name}
              className="repository-list-table__row repository-list-table__row--clickable"
              onClick={handleRowClick(repo.name)}
              onKeyDown={handleRowKeyDown(repo.name)}
              tabIndex={0}
              role="button"
              aria-label={`Browse ${repo.name}`}
              data-analytics-id="nxrm-browse-select-repository"
            >
              <Table.Cell>
                <Text size="2" weight="medium" color="blue">
                  {repo.name}
                </Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{repo.type}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{FORMAT_LABELS[repo.format] || repo.format}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{repo.status?.online ? 'Online' : 'Offline'}</Text>
              </Table.Cell>
              <Table.Cell>
                <Tooltip content={STRINGS.copyUrlTitle}>
                  <IconButton
                    variant="ghost"
                    color="gray"
                    size="1"
                    onClick={handleCopyUrl(repo.url)}
                    aria-label={STRINGS.copyUrlTitle}
                    data-analytics-id="nxrm-repository-copy-url"
                  >
                    <Copy size={16} />
                  </IconButton>
                </Tooltip>
              </Table.Cell>
              {showHealthCheck && (
                <Table.Cell className="table-cell-centered">
                  <HealthCheckCell
                    repository={repo}
                    healthStatus={healthCheck[repo.name]}
                    onAnalyze={onAnalyze}
                    analyzeLoading={analyzingRepos.has(repo.name)}
                    rhcSupportedByBackend={repo.type === 'proxy' && Object.keys(healthCheck).length > 0 ? repo.name in healthCheck : undefined}
                  />
                </Table.Cell>
              )}
              {showIqPolicyViolations && (
                <Table.Cell className="table-cell-centered">
                  <FirewallCell
                    repository={repo}
                    firewallStatus={firewallStatus[repo.name]}
                    firewallLoaded={firewallLoaded}
                    hasFirewallLicense={hasFirewallLicense}
                    proxyProtectionSummary={proxyProtectionSummary}
                  />
                </Table.Cell>
              )}
              <Table.Cell justify="end">
                <DropdownMenu.Root>
                  <DropdownMenu.Trigger>
                    <IconButton
                      variant="ghost"
                      color="gray"
                      size="1"
                      onClick={(e) => e.stopPropagation()}
                      aria-label={`Actions for ${repo.name}`}
                    >
                      <MoreHorizontal size={16} />
                    </IconButton>
                  </DropdownMenu.Trigger>
                  <DropdownMenu.Content align="end" onClick={(e) => e.stopPropagation()}>
                    <DropdownMenu.Item
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelect?.(repo.name);
                      }}
                      data-analytics-id="nxrm-browse-repository-menu"
                    >
                      {STRINGS.browseRepositoryAction}
                    </DropdownMenu.Item>
                    <DropdownMenu.Item
                      onClick={handleCopyUrl(repo.url)}
                      data-analytics-id="nxrm-repository-copy-url-menu"
                    >
                      {STRINGS.copyUrlAction}
                    </DropdownMenu.Item>
                  </DropdownMenu.Content>
                </DropdownMenu.Root>
              </Table.Cell>
            </Table.Row>
          ))
        )}
      </Table.Body>
    </Table.Root>
  );
}

export default RepositoryListTable;

