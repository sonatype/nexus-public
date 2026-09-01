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

import React, { useEffect, useState } from 'react';
import {
  Badge,
  Box,
  Button,
  Callout,
  Card,
  Flex,
  Grid,
  IconButton,
  Inset,
  Select,
  Spinner,
  Table,
  Text,
  TextField,
} from '@radix-ui/themes';
import { AlertCircle, Download, Filter, RefreshCw, Search, X } from 'lucide-react';

import type { ComponentVersionSort, GAVersion } from '../core';
import {
  SortableTableHeader,
  TablePagination,
  type SortDirection,
} from '../../../shared';
import { MobileFilterDrawer } from '../unified/MobileFilterDrawer';
import { exportToCsv } from '../../../shared';

interface GAVersionsTabProps {
  /** The current page's versions, already ordered and filtered server-side. */
  versions: readonly GAVersion[];
  /** Total distinct versions matching the current filter, across all pages. */
  total: number;
  totalPages: number;
  /** 1-based, matching TablePagination. */
  currentPage: number;
  itemsPerPage: number;
  sortKey: ComponentVersionSort;
  sortDirection: 'asc' | 'desc';
  /** The committed (debounced) version filter. May lag a keystroke behind the input. */
  searchQuery: string;
  loading: boolean;
  error: string | null;
  onPageChange: (page: number) => void;
  onItemsPerPageChange: (size: number) => void;
  onSortChange: (sort: ComponentVersionSort, direction: 'asc' | 'desc') => void;
  onSearchQueryChange: (value: string) => void;
  onRetry: () => void;
  selectedVersion: string | null;
  onVersionSelect: (version: string) => void;
}

/**
 * GAVersionsTab - Render-only table of a component's versions.
 *
 * All data, filtering, sorting, and paging come from props: the owning page's
 * useComponentVersions hook fetches one page at a time from the server. This component holds
 * no version data itself — only the mobile filter-drawer open state and a local echo of the
 * search input so keystrokes render immediately ahead of the hook's debounce.
 */
export function GAVersionsTab({
  versions,
  total,
  totalPages,
  currentPage,
  itemsPerPage,
  sortKey,
  sortDirection,
  searchQuery,
  loading,
  error,
  onPageChange,
  onItemsPerPageChange,
  onSortChange,
  onSearchQueryChange,
  onRetry,
  selectedVersion,
  onVersionSelect,
}: GAVersionsTabProps) {
  const [showMobileFilters, setShowMobileFilters] = useState(false);
  // searchInput is local so the field stays responsive while onSearchQueryChange debounces.
  // The effect keeps it a function of the prop: any committed filter the parent produces
  // without an edit here — a reset, a restored URL, a re-mount with existing state — would
  // otherwise leave the field showing text the server is no longer filtering on.
  const [searchInput, setSearchInput] = useState(searchQuery);

  useEffect(() => {
    setSearchInput(searchQuery);
  }, [searchQuery]);

  const handleSearchChange = (value: string) => {
    setSearchInput(value);
    onSearchQueryChange(value);
  };

  const handleSort = (key: string, direction: SortDirection) => {
    onSortChange(key as ComponentVersionSort, direction ?? 'asc');
  };

  const clearAllFilters = () => {
    setSearchInput('');
    onSearchQueryChange('');
    setShowMobileFilters(false);
  };

  const filterBarContent = (
    <Box p="1" pt="0">
      <Button
        variant="outline"
        color="gray"
        size="2"
        mb="4"
        onClick={clearAllFilters}
      >
        <RefreshCw size={12} />
        Reset filters
      </Button>
      <Flex direction="column" gap="4">
        <Box>
          <Text size="2" weight="bold" mb="2" style={{ display: 'block' }}>
            Sort
          </Text>
          <Flex direction="column" gap="2">
            <Select.Root
              value={sortKey}
              onValueChange={(v) => onSortChange(v as ComponentVersionSort, sortDirection)}
              size="2"
            >
              <Select.Trigger />
              <Select.Content>
                <Select.Item value="version">Version</Select.Item>
                <Select.Item value="repositories">Repositories</Select.Item>
                <Select.Item value="lastUpdated">Last Updated</Select.Item>
              </Select.Content>
            </Select.Root>
            <Select.Root
              value={sortDirection}
              onValueChange={(v) => onSortChange(sortKey, v as 'asc' | 'desc')}
              size="2"
            >
              <Select.Trigger />
              <Select.Content>
                <Select.Item value="asc">Ascending</Select.Item>
                <Select.Item value="desc">Descending</Select.Item>
              </Select.Content>
            </Select.Root>
          </Flex>
        </Box>
      </Flex>
    </Box>
  );

  if (error) {
    return (
      <Card size="1">
        <Flex
          direction="column"
          align="center"
          justify="center"
          gap="3"
          p="6"
          style={{ minHeight: '160px' }}
        >
          <Callout.Root color="red" style={{ width: '100%' }}>
            <Callout.Icon>
              <AlertCircle size={16} />
            </Callout.Icon>
            <Callout.Text>{error}</Callout.Text>
          </Callout.Root>
          <Button
            size="2"
            variant="outline"
            onClick={onRetry}
            data-testid="versions-retry-button"
          >
            <RefreshCw size={14} />
            Retry
          </Button>
        </Flex>
      </Card>
    );
  }

  if (loading && versions.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="2">
          <Spinner size="2" />
          <Text size="2" color="gray">
            Loading versions...
          </Text>
        </Flex>
      </Flex>
    );
  }

  if (!loading && total === 0) {
    return (
      <Card size="1">
        <Flex
          direction="column"
          align="center"
          justify="center"
          gap="2"
          py="8"
          px="4"
          style={{ minHeight: '160px' }}
        >
          {searchQuery ? (
            <>
              <Text size="3" weight="medium">
                No Versions Found
              </Text>
              <Text size="1" color="gray" align="center">
                Try Adjusting Your Search Terms Or Filters.
              </Text>
              <Button size="1" variant="solid" mt="1" onClick={clearAllFilters}>
                <RefreshCw size={12} />
                Reset Filters
              </Button>
            </>
          ) : (
            <>
              <Text size="3" weight="medium">
                No versions found
              </Text>
              <Text size="1" color="gray">
                No versions are available for this component.
              </Text>
            </>
          )}
        </Flex>
      </Card>
    );
  }

  const totalPagesDisplay = Math.max(totalPages, 1);
  const exportFilename = `versions-page-${currentPage}-of-${totalPagesDisplay}.csv`;
  const exportAriaLabel = `Export this page of versions as CSV (page ${currentPage} of ${totalPagesDisplay})`;

  return (
    <>
      <Grid columns={{ initial: '1', sm: '280px 1fr' }} gap="4">
        <Box display={{ initial: 'none', sm: 'block' }}>{filterBarContent}</Box>

        <Box minWidth="0">
          <Flex direction="column" gap="3">
            <Flex justify="between" align="center" gap="2">
              <TextField.Root
                placeholder="Filter by version"
                value={searchInput}
                onChange={(e) => handleSearchChange(e.target.value)}
                style={{ width: '100%', minWidth: 0 }}
              >
                <TextField.Slot>
                  <Search size={14} />
                </TextField.Slot>
                {searchInput && (
                  <TextField.Slot side="right">
                    <IconButton
                      variant="ghost"
                      color="gray"
                      size="1"
                      onClick={() => handleSearchChange('')}
                      aria-label="Clear search"
                    >
                      <X size={14} />
                    </IconButton>
                  </TextField.Slot>
                )}
              </TextField.Root>
              <Flex gap="2" align="center" style={{ flexShrink: 0 }}>
                {loading && versions.length > 0 && <Spinner size="1" data-testid="versions-inline-spinner" />}
                <Box display={{ initial: 'block', sm: 'none' }}>
                  <Button
                    size="2"
                    variant="outline"
                    color="gray"
                    onClick={() => setShowMobileFilters(true)}
                  >
                    <Filter size={14} />
                    Filter
                  </Button>
                </Box>
                <Button asChild size="2" variant="outline" color="gray">
                  <button
                    type="button"
                    disabled={versions.length === 0}
                    aria-label={exportAriaLabel}
                    onClick={() =>
                      exportToCsv(
                        versions.map((v) => ({
                          version: v.version,
                          repositories: v.repositories.join(';'),
                          lastUpdated: v.lastUpdated,
                        })),
                        exportFilename,
                        ['version', 'repositories', 'lastUpdated'],
                      )
                    }
                  >
                    <Download size={14} />
                    <Box
                      asChild
                      display={{ initial: 'none', sm: 'inline' }}
                      style={{ marginLeft: 6 }}
                    >
                      <span>Export page</span>
                    </Box>
                  </button>
                </Button>
              </Flex>
            </Flex>

            <Flex direction="column" gap="3">
              <Card size="1">
                <Inset clip="padding-box" side="bottom">
                  <Box style={{ overflowX: 'auto' }}>
                    <Table.Root size="2" data-testid="versions-table">
                      <Table.Header>
                        <Table.Row>
                          <SortableTableHeader
                            sortKey="version"
                            currentSortKey={sortKey}
                            currentSortDirection={sortDirection}
                            onSort={handleSort}
                            align="left"
                          >
                            Version
                          </SortableTableHeader>
                          <SortableTableHeader
                            sortKey="repositories"
                            currentSortKey={sortKey}
                            currentSortDirection={sortDirection}
                            onSort={handleSort}
                            align="center"
                          >
                            Repositories
                          </SortableTableHeader>
                          <SortableTableHeader
                            sortKey="lastUpdated"
                            currentSortKey={sortKey}
                            currentSortDirection={sortDirection}
                            onSort={handleSort}
                            align="left"
                          >
                            Last Updated
                          </SortableTableHeader>
                        </Table.Row>
                      </Table.Header>

                      <Table.Body>
                        {versions.map((version) => (
                          <Table.Row
                            key={version.version}
                            data-version={version.version}
                            data-selected={selectedVersion === version.version}
                            style={{
                              cursor: 'pointer',
                              backgroundColor:
                                selectedVersion === version.version
                                  ? 'var(--blue-2)'
                                  : undefined,
                              borderLeft:
                                selectedVersion === version.version
                                  ? '3px solid var(--blue-9)'
                                  : undefined,
                              boxSizing: 'border-box',
                            }}
                            onClick={(e) => {
                              e.preventDefault();
                              e.stopPropagation();
                              onVersionSelect(version.version);
                            }}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter' || e.key === ' ') {
                                e.preventDefault();
                                onVersionSelect(version.version);
                              }
                            }}
                            tabIndex={0}
                            role="button"
                            aria-pressed={selectedVersion === version.version}
                          >
                            <Table.Cell>
                              <Text size="2" weight="medium" color="blue">
                                {version.version}
                              </Text>
                            </Table.Cell>

                            <Table.Cell justify="center">
                              <Badge
                                color="gray"
                                variant="solid"
                                radius="full"
                                size="1"
                              >
                                {version.repositories.length} repo
                                {version.repositories.length !== 1 ? 's' : ''}
                              </Badge>
                            </Table.Cell>

                            <Table.Cell>
                              <Text size="2" color="gray">
                                {formatDate(version.lastUpdated)}
                              </Text>
                            </Table.Cell>
                          </Table.Row>
                        ))}
                      </Table.Body>
                    </Table.Root>
                  </Box>
                </Inset>
              </Card>

              <TablePagination
                currentPage={currentPage}
                totalPages={totalPages}
                itemsPerPage={itemsPerPage}
                totalItems={total}
                onPageChange={onPageChange}
                onItemsPerPageChange={onItemsPerPageChange}
                mt="0"
              />
            </Flex>
          </Flex>
        </Box>
      </Grid>

      <MobileFilterDrawer
        isOpen={showMobileFilters}
        onClose={() => setShowMobileFilters(false)}
        title="Filter"
        onClearAll={clearAllFilters}
      >
        {filterBarContent}
      </MobileFilterDrawer>
    </>
  );
}

function formatDate(dateString: string): string {
  try {
    return new Date(dateString).toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return dateString;
  }
}

export default GAVersionsTab;
