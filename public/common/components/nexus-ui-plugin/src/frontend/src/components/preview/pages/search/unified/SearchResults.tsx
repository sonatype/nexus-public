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
import { Package, Search, Filter } from 'lucide-react';
import { SearchResultCard } from './SearchResultCard';
import { TablePagination, PAGE_SIZE_OPTIONS } from '../../../shared';
import type { SearchResult, SortDirection, SortField } from './unified.types';
import {
  SORT_OPTIONS,
  SORT_OPTION_GROUPS,
  SortDirectionIcon,
  findSortOption,
  toSortValue,
  useSortValueChange,
} from './sortOptions';
import { DEFAULT_SORT_DIRECTION, DEFAULT_SORT_FIELD } from './useSearchUrlState';

import './SearchResults.scss';

/** Debounce delay for filter input */
const FILTER_DEBOUNCE_MS = 500;

/** Sort direction arrow size in the results-header dropdown. */
const SORT_ICON_SIZE = 14;

export interface SearchResultsProps {
  /** Search results to display */
  results: readonly SearchResult[];
  /** Whether results are loading */
  loading: boolean;
  /** Error message if any */
  error?: string;
  /** Callback to load more results. Must be a stable reference (wrapped in useCallback) to avoid infinite re-fetch loops. */
  onLoadMore: () => void;
  /** Whether more results can be loaded */
  hasMore: boolean;
  /** Callback when a result is selected */
  onSelect: (result: SearchResult) => void;
  /** Optional: callback to retry after error */
  onRetry?: () => void;
  /** Total count of results loaded so far */
  totalCount: number;
  /** Current sort field */
  sortField?: SortField;
  /** Current sort direction */
  sortDirection?: SortDirection;
  /** Callback when the user picks a different sort field and/or direction */
  onSortChange?: (field: SortField, direction: SortDirection) => void;
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
  sortField = DEFAULT_SORT_FIELD,
  sortDirection = DEFAULT_SORT_DIRECTION,
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
  const pendingPageAdvanceRef = useRef(false);

  useEffect(() => {
    setLocalFilterValue(nameFilter);
  }, [nameFilter]);

  // Cancel any in-flight filter debounce on unmount. Clicking a result card
  // mid-type unmounts this component while the 500ms timer is still pending, and
  // letting it fire would push a filter change into the parent's search machine
  // after the user has already navigated away.
  useEffect(() => {
    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, []);

  // Reset to page 1 when results shrink (new search); advance page when load-more completes
  useEffect(() => {
    if (results.length < prevResultsLengthRef.current || results.length === 0) {
      setCurrentPage(1);
      pendingPageAdvanceRef.current = false;
    } else if (results.length > prevResultsLengthRef.current && pendingPageAdvanceRef.current) {
      pendingPageAdvanceRef.current = false;
      setCurrentPage((prev) => prev + 1);
    }
    prevResultsLengthRef.current = results.length;
  }, [results.length]);

  // Auto-fetch more results when loaded count doesn't fill the current page size
  useEffect(() => {
    if (hasMore && !loading && !error && results.length > 0 && results.length < itemsPerPage) {
      onLoadMore();
    }
  }, [hasMore, loading, error, results.length, itemsPerPage, onLoadMore]);

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

  const handleLoadMore = useCallback(() => {
    pendingPageAdvanceRef.current = true;
    onLoadMore();
  }, [onLoadMore]);

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

  /* Sort dropdown - at least 180px, sized to its label, or full width when in flexible container */
  const activeSortOption = findSortOption(sortField, sortDirection) ?? SORT_OPTIONS[0];

  const handleSortValueChange = useSortValueChange(onSortChange);

  const renderSortDropdown = (triggerStyle?: React.CSSProperties) => (
    <Select.Root
      value={toSortValue(sortField, sortDirection)}
      onValueChange={handleSortValueChange}
      size="2"
    >
      <Select.Trigger
        // Sizes to the label instead of a fixed width: the closed state names
        // field *and* direction, and a fixed 180px clipped the direction off.
        // minWidth keeps the shortest label from collapsing the control.
        style={{ minWidth: 180, flexShrink: 0, ...triggerStyle }}
        // Mirrors the visible "sort: <option>" text so the accessible name
        // contains the visible label (WCAG 2.5.3 Label in Name), and matches the
        // sidebar's existing "Format: …" / "Repository: …" trigger convention.
        aria-label={`Sort: ${activeSortOption.label}`}
      >
        <Flex align="center" gap="2">
          <SortDirectionIcon direction={sortDirection} size={SORT_ICON_SIZE} />
          <Text size="2">sort:</Text>
          <Text size="2" style={{ whiteSpace: 'nowrap' }}>
            {activeSortOption.label}
          </Text>
        </Flex>
      </Select.Trigger>
      <Select.Content position="popper" side="bottom" avoidCollisions={false} sideOffset={4}>
        {SORT_OPTION_GROUPS.map((group) => (
          <Select.Group key={group.field}>
            <Select.Label>{group.label}</Select.Label>
            {group.options.map((opt) => (
              <Select.Item key={opt.value} value={opt.value}>
                <Flex align="center" gap="2">
                  <SortDirectionIcon direction={opt.direction} size={SORT_ICON_SIZE} />
                  <Text size="2">{opt.directionLabel}</Text>
                </Flex>
              </Select.Item>
            ))}
          </Select.Group>
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

  /* Desktop: single row space-between, search 300px + sort sized to its label */
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
          onLoadMore={handleLoadMore}
          mt="0"
        />
        </Box>
      </Box>
    </Box>
  );
}

export default SearchResults;
