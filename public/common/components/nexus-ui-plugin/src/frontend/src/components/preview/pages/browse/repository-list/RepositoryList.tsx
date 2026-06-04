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
  Card,
  Flex,
  Heading,
  IconButton,
  ScrollArea,
  Spinner,
  Table,
  Text,
  TextField,
  Tooltip,
  Callout,
} from '@radix-ui/themes';
import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  ChevronRight,
  Copy,
  Database,
  Search,
  X,
} from 'lucide-react';
import { ExtJS } from '../../../../../interface/ExtJS';

import type {
  RepositoryListProps,
  Repository,
  SortableField,
  SortDirection,
  HealthCheckStatus,
  FirewallStatus,
} from './repository-list.types';
import { useRepositoryList } from './useRepositoryList';
import { RepositoryStatusBadge } from './RepositoryStatusBadge';
import { HealthCheckCell } from '../../../shared/security/HealthCheckCell';
import { FirewallCell as IqPolicyViolationsCell } from '../../../shared/security/FirewallCell';
import { useToast } from '../../../shared';

import './RepositoryList.scss';

/**
 * UI Strings for the repository list.
 */
const STRINGS = {
  pageTitle: 'Browse',
  pageDescription: 'Browse assets and components',
  columns: {
    name: 'Name',
    type: 'Type',
    format: 'Format',
    status: 'Status',
    url: 'URL',
    healthCheck: 'Health Check',
    iqPolicyViolations: 'Firewall Report',
  },
  filterPlaceholder: 'Filter by name',
  emptyMessage: 'There are no repositories available',
  copyUrlTitle: 'Copy URL to Clipboard',
  urlCopiedMessage: 'URL Copied to Clipboard',
  errorTitle: 'Error loading repositories',
  loadingMessage: 'Loading repositories...',
};

/**
 * Get sort icon based on direction.
 */
function SortIcon({ direction }: { direction: SortDirection }): JSX.Element {
  if (direction === 'asc') {
    return <ArrowUp size={14} aria-label="Sorted ascending" />;
  }
  if (direction === 'desc') {
    return <ArrowDown size={14} aria-label="Sorted descending" />;
  }
  return <ArrowUpDown size={14} aria-label="Not sorted" />;
}

/**
 * Sortable column header component.
 */
interface SortableHeaderProps {
  label: string;
  field: SortableField;
  direction: SortDirection;
  onSort: (field: SortableField) => void;
}

function SortableHeader({
  label,
  field,
  direction,
  onSort,
}: SortableHeaderProps): JSX.Element {
  const handleClick = () => onSort(field);
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onSort(field);
    }
  };

  const sortLabel = direction === 'asc' ? 'ascending' : direction === 'desc' ? 'descending' : 'unsorted';
  const tooltipText = `${label} ${sortLabel}`;

  return (
    <Tooltip content={tooltipText}>
      <Flex
        align="center"
        gap="1"
        className="nxrm-sortable-header"
        onClick={handleClick}
        onKeyDown={handleKeyDown}
        tabIndex={0}
        role="button"
        aria-label={tooltipText}
      >
        <Text size="2" weight="medium">{label}</Text>
        <SortIcon direction={direction} />
      </Flex>
    </Tooltip>
  );
}

/**
 * Repository row component.
 */
interface RepositoryRowProps {
  repository: Repository;
  onSelect?: (name: string) => void;
  onCopyUrl?: (event: React.MouseEvent, url: string) => void;
  healthCheck?: HealthCheckStatus;
  firewallStatus?: FirewallStatus;
  showHealthCheck: boolean;
  showIqPolicyViolations: boolean;
  healthCheckError?: string;
  firewallStatusError?: string;
}

function RepositoryRow({
  repository,
  onSelect,
  onCopyUrl,
  healthCheck,
  firewallStatus,
  showHealthCheck,
  showIqPolicyViolations,
  healthCheckError,
  firewallStatusError,
}: RepositoryRowProps): JSX.Element {
  const toast = useToast();
  const handleRowClick = () => {
    if (onSelect) {
      onSelect(repository.name);
    }
  };

  const handleRowKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleRowClick();
    }
  };

  const handleCopyClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (onCopyUrl) {
      onCopyUrl(e, repository.url);
    } else {
      navigator.clipboard.writeText(repository.url).then(() => {
        toast.success(STRINGS.urlCopiedMessage);
      });
    }
  };

  return (
    <Table.Row
      className="nxrm-repository-row"
      onClick={handleRowClick}
      onKeyDown={handleRowKeyDown}
      tabIndex={0}
      role="button"
      aria-label={`View ${repository.name}`}
    >
      <Table.Cell>
        <Text weight="medium">{repository.name}</Text>
      </Table.Cell>
      <Table.Cell>
        <Text>{repository.type}</Text>
      </Table.Cell>
      <Table.Cell>
        <Text>{repository.format}</Text>
      </Table.Cell>
      <Table.Cell>
        <RepositoryStatusBadge status={repository.status} />
      </Table.Cell>
      <Table.Cell>
        <Tooltip content={STRINGS.copyUrlTitle}>
          <IconButton
            variant="ghost"
            size="1"
            onClick={handleCopyClick}
            aria-label={STRINGS.copyUrlTitle}
          >
            <Copy size={16} />
          </IconButton>
        </Tooltip>
      </Table.Cell>
      {showHealthCheck && (
        <Table.Cell className="nxrm-table-cell-centered">
          {(() => {
            const mappedStatus = healthCheck ? {
              enabled: healthCheck.enabled,
              analyzing: healthCheck.analyzing,
              securityIssueCount: healthCheck.results?.criticalCount,
              licenseIssueCount: healthCheck.results?.moderateCount,
            } : undefined;
            return (
              <HealthCheckCell
                repository={repository}
                healthStatus={mappedStatus}
              />
            );
          })()}
        </Table.Cell>
      )}
      {showIqPolicyViolations && (
        <Table.Cell className="nxrm-table-cell-centered">
          <IqPolicyViolationsCell
            repository={repository}
            firewallStatus={firewallStatus}
          />
        </Table.Cell>
      )}
      <Table.Cell>
        <ChevronRight size={16} className="nxrm-chevron" aria-hidden="true" />
      </Table.Cell>
    </Table.Row>
  );
}

/**
 * RepositoryList displays a table of browseable repositories.
 *
 * Features:
 * - Sortable columns (name, type, format, status)
 * - Filter by name
 * - Copy URL to clipboard
 * - Health Check column (when IQ Server permissions allow)
 * - Firewall Report column (when IQ Server is enabled)
 * - Click row to navigate to repository detail
 */
export function RepositoryList({
  onSelect,
  onCopyUrl,
}: RepositoryListProps): JSX.Element {
  const {
    state,
    setFilter,
    clearFilter,
    toggleSort,
    getSortDirection,
    showHealthCheckColumn,
    showIqPolicyViolationsColumn,
  } = useRepositoryList();

  const handleFilterChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setFilter(e.target.value);
    },
    [setFilter]
  );

  const handleFilterClear = useCallback(() => {
    clearFilter();
  }, [clearFilter]);

  const handleFilterKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Escape') {
        clearFilter();
      }
    },
    [clearFilter]
  );

  // Loading state
  if (state.loading) {
    return (
      <Flex
        direction="column"
        align="center"
        justify="center"
        gap="3"
        className="nxrm-repository-list-loading"
        p="9"
      >
        <Spinner size="3" />
        <Text color="gray">{STRINGS.loadingMessage}</Text>
      </Flex>
    );
  }

  // Error state
  if (state.error) {
    return (
      <Box p="4">
        <Callout.Root color="red">
          <Callout.Icon>
            <X size={16} />
          </Callout.Icon>
          <Callout.Text>{state.error}</Callout.Text>
        </Callout.Root>
      </Box>
    );
  }

  return (
    <Flex direction="column" className="nxrm-repository-list-container">
      {/* Sticky header */}
      <Box className="nxrm-repository-list-sticky-header" px="5" pt="5" pb="3">
        <Box className="nxrm-repository-list" style={{ marginBottom: 0 }}>
          <Flex align="center" gap="3" mb="3">
            <Database size={24} className="nxrm-page-icon" />
            <Box>
              <Heading size="6">{STRINGS.pageTitle}</Heading>
              <Text color="gray" size="2">{STRINGS.pageDescription}</Text>
            </Box>
          </Flex>

          <Flex justify="end" p="3" className="nxrm-repository-list__filter-bar">
            <Box style={{ width: '300px' }}>
              <TextField.Root
                id="filter"
                placeholder={STRINGS.filterPlaceholder}
                value={state.filterText}
                onChange={handleFilterChange}
                onKeyDown={handleFilterKeyDown}
              >
                <TextField.Slot>
                  <Search size={16} />
                </TextField.Slot>
                {state.filterText && (
                  <TextField.Slot>
                    <IconButton
                      variant="ghost"
                      size="1"
                      onClick={handleFilterClear}
                      aria-label="Clear filter"
                    >
                      <X size={14} />
                    </IconButton>
                  </TextField.Slot>
                )}
              </TextField.Root>
            </Box>
          </Flex>
        </Box>
      </Box>

      {/* Scrollable table */}
      <ScrollArea scrollbars="vertical" style={{ flex: 1 }}>
        <Box className="nxrm-repository-list" px="5" pb="5" pt="0">
          <Card>
            <Table.Root>
              <Table.Header className="nxrm-repository-list__table-header">
                <Table.Row>
                  <Table.ColumnHeaderCell>
                    <SortableHeader
                      label={STRINGS.columns.name}
                      field="name"
                      direction={getSortDirection('name')}
                      onSort={toggleSort}
                    />
                  </Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>
                    <SortableHeader
                      label={STRINGS.columns.type}
                      field="type"
                      direction={getSortDirection('type')}
                      onSort={toggleSort}
                    />
                  </Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>
                    <SortableHeader
                      label={STRINGS.columns.format}
                      field="format"
                      direction={getSortDirection('format')}
                      onSort={toggleSort}
                    />
                  </Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>
                    <SortableHeader
                      label={STRINGS.columns.status}
                      field="status"
                      direction={getSortDirection('status')}
                      onSort={toggleSort}
                    />
                  </Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>
                    <Text size="2" weight="medium">{STRINGS.columns.url}</Text>
                  </Table.ColumnHeaderCell>
                  {showHealthCheckColumn && (
                    <Table.ColumnHeaderCell className="nxrm-table-cell-centered">
                      <Text size="2" weight="medium">{STRINGS.columns.healthCheck}</Text>
                    </Table.ColumnHeaderCell>
                  )}
                  {showIqPolicyViolationsColumn && (
                    <Table.ColumnHeaderCell className="nxrm-table-cell-centered">
                      <Text size="2" weight="medium">{STRINGS.columns.iqPolicyViolations}</Text>
                    </Table.ColumnHeaderCell>
                  )}
                  <Table.ColumnHeaderCell style={{ width: '40px' }} />
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {state.filteredRepositories.length === 0 ? (
                  <Table.Row>
                    <Table.Cell
                      colSpan={
                        5 +
                        (showHealthCheckColumn ? 1 : 0) +
                        (showIqPolicyViolationsColumn ? 1 : 0) +
                        1
                      }
                    >
                      <Flex justify="center" p="6">
                        <Text color="gray">{STRINGS.emptyMessage}</Text>
                      </Flex>
                    </Table.Cell>
                  </Table.Row>
                ) : (
                  state.filteredRepositories.map((repository) => (
                    <RepositoryRow
                      key={repository.name}
                      repository={repository}
                      onSelect={onSelect}
                      onCopyUrl={onCopyUrl}
                      healthCheck={state.healthCheck[repository.name]}
                      firewallStatus={state.firewallStatus[repository.name]}
                      showHealthCheck={showHealthCheckColumn}
                      showIqPolicyViolations={showIqPolicyViolationsColumn}
                      healthCheckError={state.healthCheckError}
                      firewallStatusError={state.firewallStatusError}
                    />
                  ))
                )}
              </Table.Body>
            </Table.Root>
          </Card>
        </Box>
      </ScrollArea>
    </Flex>
  );
}

export default RepositoryList;

