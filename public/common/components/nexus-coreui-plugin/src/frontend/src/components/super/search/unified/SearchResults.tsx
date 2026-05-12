/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React, { useCallback, useRef, useEffect, useState } from 'react';
import {
  Box,
  Flex,
  Text,
  Spinner,
  Select,
  TextField,
  Heading,
  Card,
  Button,
} from '@radix-ui/themes';
import { ArrowUpDown, Package, Search, Filter } from 'lucide-react';
import { SearchResultCard } from './SearchResultCard';
import { TablePagination, PAGE_SIZE_OPTIONS } from '../../../../components/shared';
import type { SearchResult, SortOption } from './unified.types';

import './SearchResults.scss';

/** Debounce delay for filter input */
const FILTER_DEBOUNCE_MS = 500;

/** Available sort options */
const SORT_OPTIONS: SortOption[] = [
  { value: 'lastUpdated', label: 'Latest Release' },
  { value: 'name', label: 'Name' },
  { value: 'version', label: 'Version' },
  { value: 'repository', label: 'Repository' },
];

export interface SearchResultsProps {
  /** Search results to display */
  results: readonly SearchResult[];
  /** Whether results are loading */
  loading: boolean;
  /** Error message if any */
  error?: string;
  /** Callback to load more results */
  onLoadMore: () => void;
  /** Whether more results can be loaded */
  hasMore: boolean;
  /** Callback when a result is selected */
  onSelect: (result: SearchResult) => void;
  /** Optional: callback to retry after error */
  onRetry?: () => void;
  /** Total count of results loaded so far */
  totalCount: number;
  /** Current sort value */
  sortBy?: string;
  /** Callback when sort changes */
  onSortChange?: (value: string) => void;
  /** Current search query (for display in header) */
  query?: string;
  /** Name filter value */
  nameFilter?: string;
  /** Callback when name filter changes */
  onNameFilterChange?: (value: string) => void;
  /** Callback to open mobile filter drawer (mobile only) */
  onOpenMobileFilters?: () => void;
}

export function SearchResults({
  results,
  loading,
  error,
  onLoadMore,
  hasMore,
  onSelect,
  onRetry,
  totalCount,
  sortBy = 'lastUpdated',
  onSortChange,
  query,
  nameFilter = '',
  onNameFilterChange,
  onOpenMobileFilters,
}: SearchResultsProps): JSX.Element {
  const [localFilterValue, setLocalFilterValue] = useState(nameFilter);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(PAGE_SIZE_OPTIONS[0]);
  const prevResultsLengthRef = useRef(results.length);

  useEffect(() => {
    setLocalFilterValue(nameFilter);
  }, [nameFilter]);

  // Reset to page 1 when results shrink (new search)
  useEffect(() => {
    if (results.length < prevResultsLengthRef.current || results.length === 0) {
      setCurrentPage(1);
    }
    prevResultsLengthRef.current = results.length;
  }, [results.length]);

  const totalItems = results.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / itemsPerPage));
  const effectivePage = Math.min(currentPage, totalPages);
  const startIdx = (effectivePage - 1) * itemsPerPage;
  const displayedResults = results.slice(startIdx, startIdx + itemsPerPage);

  const handlePageChange = useCallback(
    (page: number) => {
      setCurrentPage(page);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    []
  );

  const handleFilterChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const value = e.target.value;
      setLocalFilterValue(value);
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
      debounceRef.current = setTimeout(() => {
        onNameFilterChange?.(value);
      }, FILTER_DEBOUNCE_MS);
    },
    [onNameFilterChange]
  );

  const handleFilterKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        if (debounceRef.current) {
          clearTimeout(debounceRef.current);
        }
        onNameFilterChange?.(localFilterValue);
      }
    },
    [onNameFilterChange, localFilterValue]
  );

  const displayTotal = hasMore
    ? `${totalCount.toLocaleString()}+`
    : totalCount.toLocaleString();

  /* Desktop search - fixed 300px (matches ux-lab ComponentsHeader) */
  const desktopSearchInput = (
    <TextField.Root
      placeholder="Filter by component name or version"
      value={localFilterValue}
      onChange={handleFilterChange}
      onKeyDown={handleFilterKeyDown}
      size="2"
      style={{ width: 300 }}
    >
      <TextField.Slot>
        <Search size={14} />
      </TextField.Slot>
    </TextField.Root>
  );

  /* Sort dropdown - fixed 180px (matches ux-lab), or full width when in flexible container */
  const renderSortDropdown = (triggerStyle?: React.CSSProperties) => (
    <Select.Root value={sortBy} onValueChange={onSortChange} size="2">
      <Select.Trigger style={{ width: 180, flexShrink: 0, ...triggerStyle }}>
        <Flex align="center" gap="2">
          <ArrowUpDown size={14} aria-hidden />
          <Text size="2">sort:</Text>
          <Text size="2">
            {SORT_OPTIONS.find((o) => o.value === sortBy)?.label ?? 'Latest Release'}
          </Text>
        </Flex>
      </Select.Trigger>
      <Select.Content position="popper" side="bottom" avoidCollisions={false} sideOffset={4}>
        {SORT_OPTIONS.map((opt) => (
          <Select.Item key={opt.value} value={opt.value}>
            {opt.label}
          </Select.Item>
        ))}
      </Select.Content>
    </Select.Root>
  );
  const sortDropdown = renderSortDropdown();

  const titleAndCount = (
    <Flex align="center" gap="2">
      <Heading size="6">Components</Heading>
      <Text size="2" color="gray">
        {displayTotal}
      </Text>
    </Flex>
  );

  /* Mobile: stacked layout - search full width, then filter + sort (wraps on very small screens) */
  const mobileHeader = (
    <Box className="header" display={{ initial: 'block', sm: 'none' }} mb="4" role="banner" aria-label="Header">
      <Flex align="center" gap="2" mb="3">
        {titleAndCount}
      </Flex>
      <Flex direction="column" gap="3" role="toolbar" aria-label="Actions bar">
        {/* Search - full width on mobile */}
        <Box style={{ width: '100%' }}>
          <TextField.Root
            placeholder="Filter by component name or version"
            value={localFilterValue}
            onChange={handleFilterChange}
            onKeyDown={handleFilterKeyDown}
            size="2"
            style={{ width: '100%' }}
          >
            <TextField.Slot>
              <Search size={14} />
            </TextField.Slot>
          </TextField.Root>
        </Box>
        {/* Filter + Sort - row that wraps on very narrow screens */}
        <Flex align="center" gap="3" wrap="wrap">
          {onOpenMobileFilters && (
            <Button
              variant="outline"
              size="2"
              color="gray"
              onClick={onOpenMobileFilters}
              aria-label="Open filters"
            >
              <Filter size={14} />
              Filter
            </Button>
          )}
          {/* Sort - fills remaining space on mobile */}
          <Box style={{ flex: 1, minWidth: 180 }}>
            {renderSortDropdown({ width: '100%' })}
          </Box>
        </Flex>
      </Flex>
    </Box>
  );

  /* Tablet: title + count, then search + sort (sidebar visible, no filter button) */
  const tabletSearchInput = (
    <Box style={{ flex: 1, minWidth: 0 }}>
      <TextField.Root
        placeholder="Filter by component name or version"
        value={localFilterValue}
        onChange={handleFilterChange}
        onKeyDown={handleFilterKeyDown}
        size="2"
        style={{ width: '100%' }}
      >
        <TextField.Slot>
          <Search size={14} />
        </TextField.Slot>
      </TextField.Root>
    </Box>
  );

  const tabletHeader = (
    <Box className="header" display={{ initial: 'none', sm: 'block', lg: 'none' }} mb="4" role="banner" aria-label="Header">
      <Flex align="center" gap="2" mb="3">
        {titleAndCount}
      </Flex>
      <Flex className="actions-bar" align="center" justify="between" gap="4" role="toolbar" aria-label="Actions bar">
        {tabletSearchInput}
        {sortDropdown}
      </Flex>
    </Box>
  );

  /* Desktop: single row space-between, search 300px + sort 180px (matches ux-lab) */
  const desktopHeader = (
    <Box className="header" display={{ initial: 'none', lg: 'block' }} mb="4" role="banner" aria-label="Header">
      <Flex align="center" justify="between" mb="3">
        {titleAndCount}
        <Flex className="actions-bar" align="center" gap="3" role="toolbar" aria-label="Actions bar">
          {desktopSearchInput}
          {sortDropdown}
        </Flex>
      </Flex>
    </Box>
  );

  const resultsHeader = (
    <>
      {mobileHeader}
      {tabletHeader}
      {desktopHeader}
    </>
  );

  if (loading && results.length === 0) {
    return (
      <Box>
        {resultsHeader}
        <Box className="cards-results" role="region" aria-label="Cards results">
          <Flex justify="center" p="4">
            <Spinner size="2" />
          </Flex>
        </Box>
      </Box>
    );
  }

  if (error) {
    return (
      <Box>
        {resultsHeader}
        <Box className="cards-results" role="region" aria-label="Cards results">
        <Card>
          <Box p="5" size="3" style={{ textAlign: 'center' }}>
            <Heading size="4" mb="2">
              Search Failed
            </Heading>
            <Text as="p" color="gray" size="2" mb="4">
              {error}
            </Text>
            <Button color="blue" variant="solid" onClick={onRetry}>
              Retry
            </Button>
          </Box>
        </Card>
        </Box>
      </Box>
    );
  }

  if (results.length === 0) {
    return (
      <Box>
        {resultsHeader}
        <Box className="cards-results" role="region" aria-label="Cards results">
        <Card>
          <Box p="5" size="3" style={{ textAlign: 'center' }}>
            <Package size={48} style={{ margin: '0 auto 16px', color: 'var(--gray-9)' }} />
            <Heading size="4" mb="2">
              No components found
            </Heading>
            <Text as="p" color="gray" size="2">
              Try adjusting your search criteria or filters
            </Text>
          </Box>
        </Card>
        </Box>
      </Box>
    );
  }

  return (
    <Box>
      {resultsHeader}
      <Box className="cards-results" role="region" aria-label="Cards results">
        <Flex direction="column" gap="3">
          {displayedResults.map((result) => (
            <SearchResultCard
              key={result.id}
              result={result}
              onClick={() => onSelect(result)}
            />
          ))}
        </Flex>

        {loading && results.length > 0 && (
          <Flex justify="center" p="4">
            <Flex align="center" gap="2">
              <Spinner size="2" />
              <Text size="2" color="gray">
                Loading more components...
              </Text>
            </Flex>
          </Flex>
        )}

        <Box className="pagination" pt="4" mt="4" role="navigation" aria-label="Pagination">
          <TablePagination
          currentPage={effectivePage}
          totalPages={totalPages}
          itemsPerPage={itemsPerPage}
          totalItems={totalItems}
          onPageChange={handlePageChange}
          onItemsPerPageChange={(size) => {
            setItemsPerPage(size);
            setCurrentPage(1);
          }}
          totalItemsSuffix={hasMore ? '+' : undefined}
          hasMore={hasMore}
          loadingMore={loading}
          onLoadMore={onLoadMore}
          mt="0"
        />
        </Box>
      </Box>
    </Box>
  );
}

export default SearchResults;
