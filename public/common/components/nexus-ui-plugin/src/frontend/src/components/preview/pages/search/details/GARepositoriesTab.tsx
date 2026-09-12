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

import React from 'react';
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
  Spinner,
  Table,
  Text,
  TextField,
} from '@radix-ui/themes';
import { AlertCircle, Download, Filter, RefreshCw, Search, X } from 'lucide-react';

import { SortableTableHeader, TablePagination, exportToCsv } from '../../../shared';
import { MobileFilterDrawer } from '../unified/MobileFilterDrawer';
import type { RepoRow } from './gaRepositoriesMachine';
import { GA_REPOSITORIES_STRINGS as UI } from './GARepositoriesStrings';
import { useGARepositoriesTab } from './useGARepositoriesTab';

interface GARepositoriesTabProps {
  rows: readonly RepoRow[];
  loading: boolean;
  error: string | null;
  selectedVersion: string | null;
}

export function GARepositoriesTab({
  rows,
  loading,
  error,
  selectedVersion,
}: GARepositoriesTabProps) {
  const vm = useGARepositoriesTab({ rows });

  const filterBarContent = (
    <Box p="1" pt="0">
      <Button variant="outline" color="gray" size="2" mb="4" onClick={vm.clearAllFilters}>
        <RefreshCw size={12} />
        {UI.RESET_FILTERS}
      </Button>
      <Flex direction="column" gap="4">
        <Box>
          <Text size="2" weight="bold" mb="2" style={{ display: 'block' }}>
            {UI.FILTERS.SORT_LABEL}
          </Text>
          <Flex direction="column" gap="2">
            <Select.Root
              value={vm.sortKey}
              onValueChange={vm.changeSortKey}
              size="2"
            >
              <Select.Trigger />
              <Select.Content>
                <Select.Item value="repositoryName">{UI.COLUMNS.REPOSITORY}</Select.Item>
                <Select.Item value="type">{UI.COLUMNS.TYPE}</Select.Item>
                <Select.Item value="versionCount">{UI.COLUMNS.VERSIONS_IN_REPO}</Select.Item>
              </Select.Content>
            </Select.Root>
            <Select.Root
              value={vm.sortDirection}
              onValueChange={(v) => vm.changeSortDirection(v as typeof vm.sortDirection)}
              size="2"
            >
              <Select.Trigger />
              <Select.Content>
                <Select.Item value="asc">{UI.FILTERS.SORT_ASC}</Select.Item>
                <Select.Item value="desc">{UI.FILTERS.SORT_DESC}</Select.Item>
              </Select.Content>
            </Select.Root>
          </Flex>
        </Box>
        <Box>
          <Text size="2" weight="bold" mb="2" style={{ display: 'block' }}>
            {UI.FILTERS.TYPE_LABEL}
          </Text>
          {UI.TYPE_OPTIONS.map((opt) => (
            <Flex key={opt.value} align="center" gap="2" mb="1" asChild>
              <label>
                <Checkbox
                  checked={vm.filterTypes.includes(opt.value)}
                  onCheckedChange={(c) => vm.toggleTypeFilter(opt.value, c === true)}
                />
                <Text size="2">{opt.label}</Text>
              </label>
            </Flex>
          ))}
        </Box>
      </Flex>
    </Box>
  );

  if (loading) {
    return (
      <Flex justify="center" align="center" style={{ minHeight: '160px' }}>
        <Spinner size="3" />
      </Flex>
    );
  }

  if (error) {
    return (
      <Callout.Root color="red">
        <Callout.Icon><AlertCircle size={16} /></Callout.Icon>
        <Callout.Text>{error}</Callout.Text>
      </Callout.Root>
    );
  }

  if (rows.length === 0) {
    return (
      <Card size="1">
        <Flex direction="column" align="center" justify="center" gap="2" p="4" style={{ minHeight: '160px' }}>
          <Text size="3" weight="medium">{UI.EMPTY.NO_REPOS_TITLE}</Text>
          <Text size="1" color="gray">
            {selectedVersion
              ? UI.EMPTY.NO_REPOS_FOR_VERSION(selectedVersion)
              : UI.EMPTY.NO_REPOS_FOR_COMPONENT}
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
                placeholder={UI.FILTER_PLACEHOLDER}
                value={vm.searchQuery}
                onChange={(e) => vm.changeSearchQuery(e.target.value)}
                style={{ width: '100%', minWidth: 0 }}
              >
                <TextField.Slot><Search size={14} /></TextField.Slot>
                {vm.searchQuery && (
                  <TextField.Slot side="right">
                    <IconButton
                      variant="ghost"
                      color="gray"
                      size="1"
                      onClick={() => vm.changeSearchQuery('')}
                      aria-label={UI.CLEAR_SEARCH_ARIA}
                    >
                      <X size={14} />
                    </IconButton>
                  </TextField.Slot>
                )}
              </TextField.Root>
              <Flex gap="2" style={{ flexShrink: 0 }}>
                <Box display={{ initial: 'block', sm: 'none' }}>
                  <Button size="2" variant="outline" color="gray" onClick={vm.openMobileFilters}>
                    <Filter size={14} />
                    {UI.FILTER_BUTTON}
                  </Button>
                </Box>
                <Button
                  size="2"
                  variant="outline"
                  color="gray"
                  type="button"
                  disabled={vm.sortedRows.length === 0}
                  aria-label={UI.EXPORT_CSV_ARIA}
                  onClick={() =>
                    exportToCsv(
                      vm.sortedRows.map((r) => ({
                        repositoryName: r.repositoryName,
                        type: r.type,
                        versionCount: r.versionCount,
                      })),
                      UI.EXPORT_FILENAME,
                      ['repositoryName', 'type', 'versionCount'],
                    )
                  }
                >
                  <Download size={14} />
                  <Box asChild display={{ initial: 'none', sm: 'inline' }} style={{ marginLeft: 6 }}>
                    <span>{UI.EXPORT_CSV}</span>
                  </Box>
                </Button>
              </Flex>
            </Flex>

            {vm.filteredRows.length === 0 ? (
              <Card>
                <Flex direction="column" align="center" justify="center" gap="2" py="8" px="4" style={{ minHeight: '160px' }}>
                  <Text size="3" weight="medium">{UI.EMPTY.NO_RESULTS_TITLE}</Text>
                  <Text size="1" color="gray" align="center">{UI.EMPTY.NO_RESULTS_DETAIL}</Text>
                  <Button size="1" variant="solid" mt="1" onClick={vm.clearAllFilters}>
                    <RefreshCw size={12} />
                    {UI.RESET_FILTERS}
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
                              sortKey="repositoryName"
                              currentSortKey={vm.sortKey}
                              currentSortDirection={vm.sortDirection}
                              onSort={vm.changeSort}
                              align="left"
                            >
                              {UI.COLUMNS.REPOSITORY}
                            </SortableTableHeader>
                            <SortableTableHeader
                              sortKey="type"
                              currentSortKey={vm.sortKey}
                              currentSortDirection={vm.sortDirection}
                              onSort={vm.changeSort}
                              align="center"
                            >
                              {UI.COLUMNS.TYPE}
                            </SortableTableHeader>
                            <SortableTableHeader
                              sortKey="versionCount"
                              currentSortKey={vm.sortKey}
                              currentSortDirection={vm.sortDirection}
                              onSort={vm.changeSort}
                              align="center"
                            >
                              {UI.COLUMNS.VERSIONS_IN_REPO}
                            </SortableTableHeader>
                          </Table.Row>
                        </Table.Header>

                        <Table.Body>
                          {vm.paginatedRows.map((r) => (
                            <Table.Row key={r.repositoryName}>
                              <Table.Cell>
                                <Text size="2" weight="medium">{r.repositoryName}</Text>
                              </Table.Cell>
                              <Table.Cell justify="center">
                                <Text size="2" color="gray">{r.type}</Text>
                              </Table.Cell>
                              <Table.Cell justify="center">
                                <Badge color="gray" variant="solid" radius="full" size="1">
                                  {UI.VERSIONS_BADGE(r.versionCount)}
                                </Badge>
                              </Table.Cell>
                            </Table.Row>
                          ))}
                        </Table.Body>
                      </Table.Root>
                    </Box>
                  </Inset>
                </Card>

                {vm.totalItems > 0 && (
                  <TablePagination
                    currentPage={vm.currentPage}
                    totalPages={vm.totalPages}
                    itemsPerPage={vm.itemsPerPage}
                    totalItems={vm.totalItems}
                    onPageChange={vm.changePage}
                    onItemsPerPageChange={vm.changeItemsPerPage}
                    mt="0"
                  />
                )}
              </Flex>
            )}
          </Flex>
        </Box>
      </Grid>

      <MobileFilterDrawer
        isOpen={vm.showMobileFilters}
        onClose={vm.closeMobileFilters}
        title={UI.FILTER_BUTTON}
        onClearAll={vm.clearAllFilters}
      >
        {filterBarContent}
      </MobileFilterDrawer>
    </>
  );
}

export default GARepositoriesTab;
