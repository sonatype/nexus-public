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
  VisuallyHidden,
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
import { exportToCsv } from '../../../shared';

interface GAFilesTabProps {
  assets: readonly GAAsset[];
  selectedVersion: string | null;
  loading: boolean;
}

/** Rendered wherever an asset carries no usable timestamp. */
const NO_DATE = '—';

/**
 * Sort key for an asset's timestamp. Undated assets collapse to one bucket at the bottom of a
 * descending sort rather than to NaN, which would make the whole ordering arbitrary.
 */
function lastModifiedSortValue(asset: GAAsset): number {
  if (!asset.lastModified) return -Infinity;
  const time = new Date(asset.lastModified).getTime();
  return Number.isNaN(time) ? -Infinity : time;
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
  }, []);

  const extensionOptions = useMemo(() => {
    const exts = new Set(assets.map((a) => a.extension));
    return [...exts].sort();
  }, [assets]);

  const filteredAssets = useMemo(() => {
    return assets.filter((a) => {
      const q = searchQuery.trim().toLowerCase();
      const fileName = a.path.split('/').pop() || a.path;
      // `repository` is matched because it is the sub-line under each filename and the only
      // field distinguishing assets that share one (NEXUS-54201). `classifier` stays in the
      // list for the mock path, but the real API never populates it — see
      // componentVersionDetailApi.toAsset — so it matches nothing in production.
      if (
        q &&
        !(
          fileName.toLowerCase().includes(q) ||
          a.extension.toLowerCase().includes(q) ||
          a.repository.toLowerCase().includes(q) ||
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
        case 'lastModified': {
          // Compared, not subtracted: -Infinity - -Infinity is NaN, and a NaN comparator makes
          // the ordering of two undated assets arbitrary.
          const aTime = lastModifiedSortValue(a);
          const bTime = lastModifiedSortValue(b);
          cmp = aTime === bTime ? 0 : aTime < bTime ? -1 : 1;
          break;
        }
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

  // === null, not truthiness: '' is the valid selected version for versionless formats (raw).
  if (selectedVersion === null) {
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
                <Button asChild size="2" variant="outline" color="gray">
                  <button
                    type="button"
                    disabled={sortedAssets.length === 0}
                    aria-label="Export all filtered results as CSV"
                    onClick={() =>
                      // `repository` replaces `classifier`: the real API never populates the
                      // latter (componentVersionDetailApi.toAsset hardcodes it undefined), so the
                      // column was always blank, and the repository is what the rows now show.
                      exportToCsv(
                        sortedAssets.map((a) => ({
                          file: a.path.split('/').pop() || a.path,
                          extension: a.extension,
                          repository: a.repository,
                          size: a.size,
                          lastModified: a.lastModified ?? '',
                          downloadUrl: a.downloadUrl,
                        })),
                        'files.csv',
                        ['file', 'extension', 'repository', 'size', 'lastModified', 'downloadUrl'],
                      )
                    }
                  >
                    <Download size={14} />
                    <Box
                      asChild
                      display={{ initial: 'none', sm: 'inline' }}
                      style={{ marginLeft: 6 }}
                    >
                      <span>Export CSV</span>
                    </Box>
                  </button>
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
        {/* align="start": Flex defaults to stretch, which would grow the badge's pill background
            to the column's full width instead of hugging the repository name. */}
        <Flex direction="column" gap="1" align="start">
          <Text size="2" weight="medium">
            {fileName}
          </Text>
          {/* Same badge convention as the per-row repository/version counts in GAVersionsTab and
              GARepositoriesTab. Unlike those, this one has no column header to anchor it, so it
              still needs the hidden label — without it a screen reader announces the File cell as
              two bare strings. */}
          <Badge color="gray" variant="solid" radius="full" size="1">
            <VisuallyHidden>Repository: </VisuallyHidden>
            {asset.repository}
          </Badge>
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

/**
 * An asset with no timestamp renders as an em dash, not as "Invalid Date".
 *
 * The try/catch cannot cover this on its own: `new Date('')` and `new Date('nonsense')` throw
 * nothing, they produce an Invalid Date whose toLocaleDateString() returns the literal string
 * "Invalid Date". Both the null and the unparseable case have to be checked explicitly.
 */
function formatDate(dateString: string | null): string {
  if (!dateString) return NO_DATE;
  const parsed = new Date(dateString);
  if (Number.isNaN(parsed.getTime())) return NO_DATE;
  return parsed.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export default GAFilesTab;
