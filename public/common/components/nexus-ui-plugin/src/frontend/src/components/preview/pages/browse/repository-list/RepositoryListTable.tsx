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
  Flex,
  IconButton,
  Link,
  Skeleton,
  Table,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import { Copy, X } from 'lucide-react';
import { useRouter } from '@uirouter/react';

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
import { BROWSE_ROUTE_STATES } from '../browse.routes';

import './RepositoryListTable.scss';

/**
 * UI Strings.
 */
const STRINGS = {
  columns: {
    name: 'Name',
    type: 'Type',
    format: 'Format',
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
};

/**
 * Skeleton rows rendered while the first page is loading, matching the NexusOne
 * table row skeleton pattern: headers stay visible, placeholders mirror the content.
 */
const SKELETON_ROW_COUNT = 5;

export interface RepositoryListTableProps {
  /** Repositories to display */
  repositories: readonly Repository[];
  /** Loading state */
  loading?: boolean;
  /**
   * Whether the first repository-list request has settled. Distinguishes a first load from a
   * refetch; without it an empty result would look like a first load on every subsequent filter
   * change and the empty state would be replaced by skeleton rows.
   */
  hasLoadedOnce?: boolean;
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
  hasLoadedOnce = false,
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

  const router = useRouter();

  /**
   * A plain primary click routes through the existing `onSelect` handler, so the router
   * navigation and its route construction stay where they already live. Modifier and
   * non-primary clicks fall through untouched, letting the anchor's `href` give the browser
   * its normal open-in-new-tab/window behaviour. So does a missing `onSelect`: cancelling
   * the default without a handler to replace it would leave a link that goes nowhere.
   */
  const handleSelectRepository =
    (name: string) => (event: React.MouseEvent<HTMLAnchorElement>) => {
      if (!onSelect || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
        return;
      }
      event.preventDefault();
      onSelect(name);
    };

  // Handle copy URL
  const handleCopyUrl = (url: string) => () => {
    const urlWithSlash = ensureTrailingSlash(url);
    navigator.clipboard.writeText(urlWithSlash)
      .then(() => {
        toast.success(STRINGS.urlCopiedMessage);
      })
      .catch(() => {
        toast.error(STRINGS.urlCopyFailedMessage);
      });
  };

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

  // Column count for the empty and skeleton states: name, type, format, status, url,
  // + optional (health, IQ).
  const colCount = 5 + (showHealthCheck ? 1 : 0) + (showIqPolicyViolations ? 1 : 0);

  // Filtering must not collapse the table. Only the very first load — before any response has
  // arrived — gets skeleton rows; every later request keeps whatever is on screen (rows, or the
  // empty state) mounted and is signalled with aria-busy instead. This keys off request
  // lifecycle rather than `repositories.length`, which cannot tell a first load from a refetch
  // after a filter matched nothing, and so would swap that empty state for skeleton rows.
  const isInitialLoad = loading && !hasLoadedOnce;
  const isRefreshing = loading && hasLoadedOnce;

  return (
    <Table.Root
      className={`repository-list-table${isRefreshing ? ' repository-list-table--refreshing' : ''}`}
      variant="surface"
      size="2"
      data-testid="repository-list-table"
      aria-busy={loading}
    >
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
          <Table.ColumnHeaderCell>
            {STRINGS.columns.status}
          </Table.ColumnHeaderCell>
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
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {isInitialLoad ? (
          Array.from({ length: SKELETON_ROW_COUNT }).map((_, rowIndex) => (
            <Table.Row key={`skeleton-${rowIndex}`} data-testid="repository-list-skeleton-row">
              {Array.from({ length: colCount }).map((__, cellIndex) => (
                <Table.Cell key={`skeleton-${rowIndex}-${cellIndex}`}>
                  <Skeleton height="18px" />
                </Table.Cell>
              ))}
            </Table.Row>
          ))
        ) : repositories.length === 0 ? (
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
              className="repository-list-table__row repository-list-table__row--hoverable"
            >
              <Table.Cell>
                {/*
                  The colour is applied inline deliberately. Nexus One resets bare anchors
                  with a high-specificity App.scss rule
                  (`.nxrm-main-content > .radix-themes:not(:has(.nxrm-ext-js-wrapper))
                  a:not([class*="x-"])` -> color: inherit), which outranks every
                  :where()-wrapped Radix rule and would otherwise leave this link looking
                  like plain dark table text. `color="blue"` stamps data-accent-color, so
                  var(--accent-a11) resolves to blue; that is the same token Radix applies
                  to `<Text color="blue">`, which is how this column has always looked.
                  No underline: this list has never underlined repository names.
                */}
                <Link
                  color="blue"
                  size="2"
                  weight="medium"
                  style={{ color: 'var(--accent-a11)' }}
                  href={router.stateService.href(BROWSE_ROUTE_STATES.repo, {
                    repoName: repo.name,
                  })}
                  onClick={handleSelectRepository(repo.name)}
                  data-analytics-id="nxrm-browse-select-repository"
                >
                  {repo.name}
                </Link>
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
            </Table.Row>
          ))
        )}
      </Table.Body>
    </Table.Root>
  );
}

export default RepositoryListTable;

