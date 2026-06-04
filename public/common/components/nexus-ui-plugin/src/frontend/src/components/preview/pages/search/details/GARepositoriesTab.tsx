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
  Callout,
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
import { Download, Filter, Info, RefreshCw, Search, X } from 'lucide-react';

import type { GARepository, GAVersion } from '../core';
import {
  SortableTableHeader,
  TablePagination,
  type SortDirection,
} from '../../../shared';
import { FORMAT_LABELS } from '../../settings/repository/repositories/types';
import { MobileFilterDrawer } from '../unified/MobileFilterDrawer';

const TYPE_OPTIONS = [
  { value: 'hosted', label: 'Hosted' },
  { value: 'proxy', label: 'Proxy' },
  { value: 'group', label: 'Group' },
] as const;

interface GARepositoriesTabProps {
  repositories: readonly GARepository[];
  /** When set, filter to only repos that contain this version */
  selectedVersion?: string | null;
  /** All versions data (used to find repos for a specific version) */
  versions?: readonly GAVersion[];
}

/**
 * GARepositoriesTab - List of repositories containing this component.
 * Matches nexusone-ux-prototype reference table: filter bar, action bar, Card+Inset, pagination.
 */
export function GARepositoriesTab({
  repositories,
  selectedVersion,
  versions,
}: GARepositoriesTabProps) {
  const [sortKey, setSortKey] = useState<string | null>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterTypes, setFilterTypes] = useState<string[]>([]);
  const [filterFormats, setFilterFormats] = useState<string[]>([]);
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
  }, [searchQuery, filterTypes, filterFormats]);

  const versionFilteredRepos = useMemo(() => {
    if (!selectedVersion || !versions) return [...repositories];

    const versionEntry = versions.find((v) => v.version === selectedVersion);
    if (!versionEntry) return [...repositories];

    const versionRepoNames = new Set(versionEntry.repositories);
    return repositories.filter((repo) => versionRepoNames.has(repo.name));
  }, [repositories, selectedVersion, versions]);

  const formatOptions = useMemo(() => {
    const fmts = new Set(versionFilteredRepos.map((r) => r.format));
    return [...fmts].sort();
  }, [versionFilteredRepos]);

  const filteredRepos = useMemo(() => {
    return versionFilteredRepos.filter((repo) => {
      const q = searchQuery.trim().toLowerCase();
      if (
        q &&
        !(
          repo.name.toLowerCase().includes(q) ||
          repo.type.toLowerCase().includes(q) ||
          (FORMAT_LABELS[repo.format] || repo.format)
            .toLowerCase()
            .includes(q)
        )
      ) {
        return false;
      }
      if (filterTypes.length > 0 && !filterTypes.includes(repo.type)) {
        return false;
      }
      if (filterFormats.length > 0 && !filterFormats.includes(repo.format)) {
        return false;
      }
      return true;
    });
  }, [versionFilteredRepos, searchQuery, filterTypes, filterFormats]);

  const sortedRepos = useMemo(() => {
    const arr = [...filteredRepos];
    if (!sortKey) return arr;

    arr.sort((a, b) => {
      let cmp = 0;
      switch (sortKey) {
        case 'name':
          cmp = a.name.localeCompare(b.name);
          break;
        case 'type':
          cmp = a.type.localeCompare(b.type);
          break;
        case 'format':
          cmp = a.format.localeCompare(b.format);
          break;
        case 'versionsCount':
          cmp = a.versionsCount - b.versionsCount;
          break;
        default:
          return 0;
      }
      return sortDirection === 'desc' ? -cmp : cmp;
    });
    return arr;
  }, [filteredRepos, sortKey, sortDirection]);

  const totalItems = sortedRepos.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / itemsPerPage));
  const offset = (currentPage - 1) * itemsPerPage;
  const paginatedRepos = sortedRepos.slice(offset, offset + itemsPerPage);

  const clearAllFilters = () => {
    setSearchQuery('');
    setFilterTypes([]);
    setFilterFormats([]);
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
              value={sortKey ?? 'name'}
              onValueChange={(v) => {
                setSortKey(v);
                setCurrentPage(1);
              }}
              size="2"
            >
              <Select.Trigger />
              <Select.Content>
                <Select.Item value="name">Repository</Select.Item>
                <Select.Item value="type">Type</Select.Item>
                <Select.Item value="format">Format</Select.Item>
                <Select.Item value="versionsCount">Versions in Repo</Select.Item>
              </Select.Content>
            </Select.Root>
            <Select.Root
              value={sortDirection ?? 'asc'}
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
            Type
          </Text>
          {TYPE_OPTIONS.map((opt) => (
            <Flex key={opt.value} align="center" gap="2" mb="1">
              <Checkbox
                checked={filterTypes.includes(opt.value)}
                onCheckedChange={(c) =>
                  setFilterTypes((prev) =>
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
        {formatOptions.length > 0 && (
          <Box>
            <Text size="2" weight="bold" mb="2" style={{ display: 'block' }}>
              Format
            </Text>
            {formatOptions.map((fmt) => (
              <Flex key={fmt} align="center" gap="2" mb="1">
                <Checkbox
                  checked={filterFormats.includes(fmt)}
                  onCheckedChange={(c) =>
                    setFilterFormats((prev) =>
                      c === true
                        ? [...prev, fmt]
                        : prev.filter((x) => x !== fmt),
                    )
                  }
                />
                <Text size="2">{FORMAT_LABELS[fmt] || fmt}</Text>
              </Flex>
            ))}
          </Box>
        )}
      </Flex>
    </Box>
  );

  if (versionFilteredRepos.length === 0) {
    return (
      <Flex direction="column" gap="3">
        {selectedVersion && (
          <Callout.Root color="blue" size="1">
            <Callout.Icon>
              <Info size={14} />
            </Callout.Icon>
            <Callout.Text>
              Showing repositories containing v{selectedVersion}. No
              repositories found.
            </Callout.Text>
          </Callout.Root>
        )}
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
              No repositories found
            </Text>
            <Text size="1" color="gray">
              {selectedVersion
                ? `No repositories contain version ${selectedVersion}.`
                : 'No repositories are available for this component.'}
            </Text>
          </Flex>
        </Card>
      </Flex>
    );
  }

  return (
    <>
      {selectedVersion && (
        <Callout.Root color="blue" size="1" mb="4">
          <Callout.Icon>
            <Info size={14} />
          </Callout.Icon>
          <Callout.Text>
            Showing repositories containing v{selectedVersion}.
            {versionFilteredRepos.length !== repositories.length && (
              <>
                {' '}
                ({versionFilteredRepos.length} of {repositories.length}{' '}
                repositories)
              </>
            )}
          </Callout.Text>
        </Callout.Root>
      )}

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

            {filteredRepos.length === 0 ? (
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
                    No Repositories Found
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
                              sortKey="name"
                              currentSortKey={sortKey}
                              currentSortDirection={sortDirection}
                              onSort={handleSort}
                              align="left"
                            >
                              Repository
                            </SortableTableHeader>
                            <SortableTableHeader
                              sortKey="type"
                              currentSortKey={sortKey}
                              currentSortDirection={sortDirection}
                              onSort={handleSort}
                              align="center"
                            >
                              Type
                            </SortableTableHeader>
                            <SortableTableHeader
                              sortKey="format"
                              currentSortKey={sortKey}
                              currentSortDirection={sortDirection}
                              onSort={handleSort}
                              align="center"
                            >
                              Format
                            </SortableTableHeader>
                            <SortableTableHeader
                              sortKey="versionsCount"
                              currentSortKey={sortKey}
                              currentSortDirection={sortDirection}
                              onSort={handleSort}
                              align="center"
                            >
                              Versions in Repo
                            </SortableTableHeader>
                          </Table.Row>
                        </Table.Header>

                        <Table.Body>
                          {paginatedRepos.map((repo) => (
                            <Table.Row key={repo.name}>
                              <Table.Cell>
                                <Text size="2" weight="medium">
                                  {repo.name}
                                </Text>
                              </Table.Cell>

                              <Table.Cell justify="center">
                                <Text size="2" color="gray">
                                  {repo.type}
                                </Text>
                              </Table.Cell>

                              <Table.Cell justify="center">
                                <Text size="2" color="gray">
                                  {FORMAT_LABELS[repo.format] || repo.format}
                                </Text>
                              </Table.Cell>

                              <Table.Cell justify="center">
                                <Badge
                                  color="gray"
                                  variant="solid"
                                  radius="full"
                                  size="1"
                                >
                                  {repo.versionsCount} version
                                  {repo.versionsCount !== 1 ? 's' : ''}
                                </Badge>
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

export default GARepositoriesTab;
