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
  Box,
  Button,
  Card,
  Callout,
  Checkbox,
  DropdownMenu,
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
import {
  AlertCircle,
  Download,
  Filter,
  MoreHorizontal,
  RefreshCw,
  Search,
  X,
} from 'lucide-react';

import type { GAAsset } from '../core';
import {
  SortableTableHeader,
  TablePagination,
  type SortDirection,
} from '../../../shared';
import { MobileFilterDrawer } from '../unified/MobileFilterDrawer';

interface GAFilesTabProps {
  assets: readonly GAAsset[];
  selectedVersion: string | null;
  loading: boolean;
}

/**
 * GAFilesTab - Files/assets for a selected version.
 * Matches nexusone-ux-prototype reference table: filter bar, action bar, Card+Inset, pagination.
 */
export function GAFilesTab({
  assets,
  selectedVersion,
  loading,
}: GAFilesTabProps) {
  const [sortKey, setSortKey] = useState<string | null>('path');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterExtensions, setFilterExtensions] = useState<string[]>([]);
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
  }, [searchQuery, filterExtensions]);

  const extensionOptions = useMemo(() => {
    const exts = new Set(assets.map((a) => a.extension));
    return [...exts].sort();
  }, [assets]);

  const filteredAssets = useMemo(() => {
    return assets.filter((a) => {
      const q = searchQuery.trim().toLowerCase();
      const fileName = a.path.split('/').pop() || a.path;
      if (
        q &&
        !(
          fileName.toLowerCase().includes(q) ||
          a.extension.toLowerCase().includes(q) ||
          (a.classifier?.toLowerCase().includes(q) ?? false)
        )
      ) {
        return false;
      }
      if (
        filterExtensions.length > 0 &&
        !filterExtensions.includes(a.extension)
      ) {
        return false;
      }
      return true;
    });
  }, [assets, searchQuery, filterExtensions]);

  const sortedAssets = useMemo(() => {
    const arr = [...filteredAssets];
    if (!sortKey) return arr;

    arr.sort((a, b) => {
      let cmp = 0;
      const aName = a.path.split('/').pop() || a.path;
      const bName = b.path.split('/').pop() || b.path;
      switch (sortKey) {
        case 'path':
          cmp = aName.localeCompare(bName);
          break;
        case 'extension':
          cmp = a.extension.localeCompare(b.extension);
          break;
        case 'size':
          cmp = a.size - b.size;
          break;
        case 'lastModified':
          cmp =
            new Date(a.lastModified).getTime() -
            new Date(b.lastModified).getTime();
          break;
        default:
          return 0;
      }
      return sortDirection === 'desc' ? -cmp : cmp;
    });
    return arr;
  }, [filteredAssets, sortKey, sortDirection]);

  const totalItems = sortedAssets.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / itemsPerPage));
  const offset = (currentPage - 1) * itemsPerPage;
  const paginatedAssets = sortedAssets.slice(offset, offset + itemsPerPage);

  const clearAllFilters = () => {
    setSearchQuery('');
    setFilterExtensions([]);
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
              value={sortKey ?? 'path'}
              onValueChange={(v) => {
                setSortKey(v);
                setCurrentPage(1);
              }}
              size="2"
            >
              <Select.Trigger />
              <Select.Content>
                <Select.Item value="path">File</Select.Item>
                <Select.Item value="extension">Type</Select.Item>
                <Select.Item value="size">Size</Select.Item>
                <Select.Item value="lastModified">Last Modified</Select.Item>
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
        {extensionOptions.length > 0 && (
          <Box>
            <Text size="2" weight="bold" mb="2" style={{ display: 'block' }}>
              Type
            </Text>
            {extensionOptions.map((ext) => (
              <Flex key={ext} align="center" gap="2" mb="1">
                <Checkbox
                  checked={filterExtensions.includes(ext)}
                  onCheckedChange={(c) =>
                    setFilterExtensions((prev) =>
                      c === true
                        ? [...prev, ext]
                        : prev.filter((x) => x !== ext),
                    )
                  }
                />
                <Text size="2">{ext}</Text>
              </Flex>
            ))}
          </Box>
        )}
      </Flex>
    </Box>
  );

  if (!selectedVersion) {
    return (
      <Callout.Root color="amber">
        <Callout.Icon>
          <AlertCircle size={16} />
        </Callout.Icon>
        <Callout.Text>
          Select a version from the <strong>Versions</strong> tab to view
          files.
        </Callout.Text>
      </Callout.Root>
    );
  }

  if (loading) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="2">
          <Spinner size="2" />
          <Text size="2" color="gray">
            Loading files...
          </Text>
        </Flex>
      </Flex>
    );
  }

  if (!assets || assets.length === 0) {
    return (
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
            No Files Found
          </Text>
          <Text size="1" color="gray" align="center">
            No files for version {selectedVersion}.
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

            {filteredAssets.length === 0 ? (
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
                    No Files Found
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
                              sortKey="path"
                              currentSortKey={sortKey}
                              currentSortDirection={sortDirection}
                              onSort={handleSort}
                              align="left"
                            >
                              File
                            </SortableTableHeader>
                            <SortableTableHeader
                              sortKey="extension"
                              currentSortKey={sortKey}
                              currentSortDirection={sortDirection}
                              onSort={handleSort}
                              align="center"
                            >
                              Type
                            </SortableTableHeader>
                            <SortableTableHeader
                              sortKey="size"
                              currentSortKey={sortKey}
                              currentSortDirection={sortDirection}
                              onSort={handleSort}
                              align="center"
                            >
                              Size
                            </SortableTableHeader>
                            <SortableTableHeader
                              sortKey="lastModified"
                              currentSortKey={sortKey}
                              currentSortDirection={sortDirection}
                              onSort={handleSort}
                              align="left"
                            >
                              Last Modified
                            </SortableTableHeader>
                            <Table.ColumnHeaderCell
                              justify="end"
                              aria-label="Row actions"
                              pr="5"
                            />
                          </Table.Row>
                        </Table.Header>

                        <Table.Body>
                          {paginatedAssets.map((asset) => (
                            <AssetRow key={asset.id} asset={asset} />
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

function AssetRow({ asset }: { asset: GAAsset }) {
  const fileName = asset.path.split('/').pop() || asset.path;

  const handleDownload = () => {
    window.open(asset.downloadUrl, '_blank');
  };

  const handleBrowse = () => {
    const browseUrl = `#browse/browse:${asset.repository}:${encodeURIComponent(asset.path)}`;
    window.location.hash = browseUrl;
  };

  return (
    <Table.Row>
      <Table.Cell>
        <Flex direction="column" gap="1">
          <Text size="2" weight="medium">
            {fileName}
          </Text>
          <Text size="2" color="gray">
            {asset.classifier || '-'}
          </Text>
        </Flex>
      </Table.Cell>

      <Table.Cell justify="center">
        <Text size="2" color="gray">
          {asset.extension}
        </Text>
      </Table.Cell>

      <Table.Cell justify="center">
        <Text size="2" color="gray">
          {formatFileSize(asset.size)}
        </Text>
      </Table.Cell>

      <Table.Cell>
        <Text size="2" color="gray">
          {formatDate(asset.lastModified)}
        </Text>
      </Table.Cell>

      <Table.Cell justify="end" pr="5">
        <DropdownMenu.Root>
          <DropdownMenu.Trigger>
            <IconButton
              variant="ghost"
              color="gray"
              size="1"
              aria-label="Row actions"
            >
              <MoreHorizontal size={16} />
            </IconButton>
          </DropdownMenu.Trigger>
          <DropdownMenu.Content align="end">
            <DropdownMenu.Item onClick={handleDownload}>
              Download
            </DropdownMenu.Item>
            <DropdownMenu.Item onClick={handleBrowse}>
              Browse
            </DropdownMenu.Item>
          </DropdownMenu.Content>
        </DropdownMenu.Root>
      </Table.Cell>
    </Table.Row>
  );
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
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

export default GAFilesTab;
