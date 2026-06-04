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

import React, { useMemo, useState, useEffect } from 'react';
import {
  Badge,
  Box,
  Button,
  Card,
  Checkbox,
  Flex,
  Grid,
  IconButton,
  Inset,
  Select,
  Table,
  Text,
  TextField,
} from '@radix-ui/themes';
import { Download, Filter, RefreshCw, Search, X } from 'lucide-react';

import type { GAVersion } from '../core';
import {
  SortableTableHeader,
  TablePagination,
  type SortDirection,
} from '../../../shared';
import { MobileFilterDrawer } from '../unified/MobileFilterDrawer';

const STATUS_OPTIONS = [
  { value: 'recommended', label: 'Recommended' },
  { value: 'quarantined', label: 'Quarantined' },
  { value: 'malware', label: 'Malware' },
  { value: 'not-recommended', label: 'Not Recommended' },
  { value: 'none', label: 'None' },
] as const;

interface GAVersionsTabProps {
  versions: readonly GAVersion[];
  selectedVersion: string | null;
  onVersionSelect: (version: string) => void;
}

/**
 * GAVersionsTab - Table of versions with status badges.
 * Matches nexusone-ux-prototype reference table: filter bar, action bar, Card+Inset, pagination.
 */
export function GAVersionsTab({
  versions,
  selectedVersion,
  onVersionSelect,
}: GAVersionsTabProps) {
  const [sortKey, setSortKey] = useState<string | null>('lastUpdated');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatuses, setFilterStatuses] = useState<string[]>([]);
  const [showMobileFilters, setShowMobileFilters] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(20);

  const handleSort = (key: string, direction: SortDirection) => {
    setSortKey(key);
    setSortDirection(direction ?? 'asc');
    setCurrentPage(1);
  };

  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, filterStatuses]);

  const filteredVersions = useMemo(() => {
    return versions.filter((v) => {
      const q = searchQuery.trim().toLowerCase();
      if (
        q &&
        !(
          v.version.toLowerCase().includes(q) ||
          v.status.toLowerCase().includes(q) ||
          v.repositories.some((r) => r.toLowerCase().includes(q))
        )
      ) {
        return false;
      }
      if (
        filterStatuses.length > 0 &&
        !filterStatuses.includes(v.status)
      ) {
        return false;
      }
      return true;
    });
  }, [versions, searchQuery, filterStatuses]);

  const sortedVersions = useMemo(() => {
    const arr = [...filteredVersions];
    if (!sortKey) return arr;

    arr.sort((a, b) => {
      let cmp = 0;
      switch (sortKey) {
        case 'version':
          cmp = compareVersions(a.version, b.version);
          break;
        case 'status':
          cmp = a.status.localeCompare(b.status);
          break;
        case 'repositories':
          cmp = a.repositories.length - b.repositories.length;
          break;
        case 'lastUpdated':
          cmp =
            new Date(a.lastUpdated).getTime() -
            new Date(b.lastUpdated).getTime();
          break;
        default:
          return 0;
      }
      return sortDirection === 'desc' ? -cmp : cmp;
    });
    return arr;
  }, [filteredVersions, sortKey, sortDirection]);

  const totalItems = sortedVersions.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / itemsPerPage));
  const offset = (currentPage - 1) * itemsPerPage;
  const paginatedVersions = sortedVersions.slice(
    offset,
    offset + itemsPerPage,
  );

  const clearAllFilters = () => {
    setSearchQuery('');
    setFilterStatuses([]);
    setCurrentPage(1);
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
              value={sortKey ?? 'lastUpdated'}
              onValueChange={(v) => {
                setSortKey(v);
                setCurrentPage(1);
              }}
              size="2"
            >
              <Select.Trigger />
              <Select.Content>
                <Select.Item value="version">Version</Select.Item>
                <Select.Item value="status">Status</Select.Item>
                <Select.Item value="repositories">Repositories</Select.Item>
                <Select.Item value="lastUpdated">Last Updated</Select.Item>
              </Select.Content>
            </Select.Root>
            <Select.Root
              value={sortDirection ?? 'desc'}
              onValueChange={(v) => {
                setSortDirection(v as SortDirection);
                setCurrentPage(1);
              }}
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
        <Box>
          <Text size="2" weight="bold" mb="2" style={{ display: 'block' }}>
            Status
          </Text>
          {STATUS_OPTIONS.map((opt) => (
            <Flex key={opt.value} align="center" gap="2" mb="1">
              <Checkbox
                checked={filterStatuses.includes(opt.value)}
                onCheckedChange={(c) =>
                  setFilterStatuses((prev) =>
                    c === true
                      ? [...prev, opt.value]
                      : prev.filter((x) => x !== opt.value),
                  )
                }
              />
              <Text size="2">{opt.label}</Text>
            </Flex>
          ))}
        </Box>
      </Flex>
    </Box>
  );

  if (versions.length === 0) {
    return (
      <Card size="1">
        <Flex
          direction="column"
          align="center"
          justify="center"
          gap="2"
          p="4"
          style={{ minHeight: '160px' }}
        >
          <Text size="3" weight="medium">
            No versions found
          </Text>
          <Text size="1" color="gray">
            No versions are available for this component.
          </Text>
        </Flex>
      </Card>
    );
  }

  return (
    <>
      <Grid columns={{ initial: '1', sm: '280px 1fr' }} gap="4">
        <Box display={{ initial: 'none', sm: 'block' }}>{filterBarContent}</Box>

        <Box minWidth="0">
          <Flex direction="column" gap="3">
            <Flex justify="between" align="center" gap="2">
              <TextField.Root
                placeholder="Filter"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                style={{ width: '100%', minWidth: 0 }}
              >
                <TextField.Slot>
                  <Search size={14} />
                </TextField.Slot>
                {searchQuery && (
                  <TextField.Slot side="right">
                    <IconButton
                      variant="ghost"
                      color="gray"
                      size="1"
                      onClick={() => setSearchQuery('')}
                      aria-label="Clear search"
                    >
                      <X size={14} />
                    </IconButton>
                  </TextField.Slot>
                )}
              </TextField.Root>
              <Flex gap="2" style={{ flexShrink: 0 }}>
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
                <Button size="2" variant="outline" color="gray">
                  <Download size={14} />
                  <Box
                    asChild
                    display={{ initial: 'none', sm: 'inline' }}
                    style={{ marginLeft: 6 }}
                  >
                    <span>Export CSV</span>
                  </Box>
                </Button>
              </Flex>
            </Flex>

            {filteredVersions.length === 0 ? (
              <Card>
                <Flex
                  direction="column"
                  align="center"
                  justify="center"
                  gap="2"
                  py="8"
                  px="4"
                  style={{ minHeight: '160px' }}
                >
                  <Text size="3" weight="medium">
                    No Versions Found
                  </Text>
                  <Text size="1" color="gray" align="center">
                    Try Adjusting Your Search Terms Or Filters.
                  </Text>
                  <Button
                    size="1"
                    variant="solid"
                    mt="1"
                    onClick={clearAllFilters}
                  >
                    <RefreshCw size={12} />
                    Reset Filters
                  </Button>
                </Flex>
              </Card>
            ) : (
              <Flex direction="column" gap="3">
                <Card size="1">
                  <Inset clip="padding-box" side="bottom">
                    <Box style={{ overflowX: 'auto' }}>
                      <Table.Root size="2">
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
                              sortKey="status"
                              currentSortKey={sortKey}
                              currentSortDirection={sortDirection}
                              onSort={handleSort}
                              align="center"
                            >
                              Status
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
                          {paginatedVersions.map((version) => (
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
                                <Text size="2" color="gray">
                                  {version.status === 'none'
                                    ? '-'
                                    : formatStatusLabel(version.status)}
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
                                  {version.repositories.length !== 1
                                    ? 's'
                                    : ''}
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

                {totalItems > 0 && (
                  <TablePagination
                    currentPage={currentPage}
                    totalPages={totalPages}
                    itemsPerPage={itemsPerPage}
                    totalItems={totalItems}
                    onPageChange={setCurrentPage}
                    onItemsPerPageChange={(limit) => {
                      setItemsPerPage(limit);
                      setCurrentPage(1);
                    }}
                    mt="0"
                  />
                )}
              </Flex>
            )}
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

function formatStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    recommended: 'Recommended',
    quarantined: 'Quarantined',
    malware: 'Malware',
    'not-recommended': 'Not Recommended',
  };
  return labels[status] ?? status;
}

function compareVersions(a: string, b: string): number {
  const parse = (v: string) =>
    v.split('.').map((n) => parseInt(n, 10) || 0);
  const aParts = parse(a);
  const bParts = parse(b);
  const len = Math.max(aParts.length, bParts.length);
  for (let i = 0; i < len; i++) {
    const x = aParts[i] ?? 0;
    const y = bParts[i] ?? 0;
    if (x !== y) return x - y;
  }
  return 0;
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
