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

import React, { useState, useCallback, useEffect } from 'react';
import {
  Box,
  Flex,
  Heading,
  Text,
  TextField,
  Button,
  Badge,
  ScrollArea,
} from '@radix-ui/themes';
import { Search } from 'lucide-react';

import type { GolangSearchFilters as FilterValues } from './golang.types';
import { useGolangSearch } from './useGolangSearch';
import { GolangSearchFilters } from './GolangSearchFilters';
import { GolangSearchResults } from './GolangSearchResults';

export interface GolangSearchPageProps {
  /** Callback when navigating to detail page */
  onNavigateToDetail?: (id: string) => void;
  /** Initial module filter from URL */
  initialModule?: string;
  /** Initial version filter from URL */
  initialVersion?: string;
}

/**
 * Main Go Search page component.
 *
 * Features:
 * - Search by module path, version, keyword
 * - Results table showing module info
 * - Click row to navigate to detail page
 * - URL-driven state (bookmarkable)
 */
export function GolangSearchPage({
  onNavigateToDetail,
  initialModule = '',
  initialVersion = '',
}: GolangSearchPageProps): JSX.Element {
  const { state, search, loadMore, clear, hasMore } = useGolangSearch();

  // Search input state
  const [searchInput, setSearchInput] = useState(initialModule);

  // Local filter state
  const [filters, setFilters] = useState<FilterValues>({
    module: initialModule,
    version: initialVersion,
    keyword: '',
  });

  // Perform initial search if we have initial values
  useEffect(() => {
    if (initialModule || initialVersion) {
      search({
        module: initialModule,
        version: initialVersion,
      });
    }
  }, []); // Only on mount

  /**
   * Handle search submission.
   */
  const handleSearch = useCallback((): void => {
    search({
      ...filters,
      module: searchInput || filters.module,
    });
  }, [search, filters, searchInput]);

  /**
   * Handle search input keypress.
   */
  const handleSearchKeyDown = (e: React.KeyboardEvent): void => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  /**
   * Handle selecting a result to view details.
   */
  const handleSelect = useCallback((id: string): void => {
    if (onNavigateToDetail) {
      onNavigateToDetail(id);
    } else {
      // Default: navigate using hash
      window.location.hash = `#preview/browse/search/go/detail/${encodeURIComponent(id)}`;
    }
  }, [onNavigateToDetail]);

  /**
   * Handle clearing all filters.
   */
  const handleClear = useCallback((): void => {
    setSearchInput('');
    setFilters({
      module: '',
      version: '',
      keyword: '',
    });
    clear();
  }, [clear]);

  return (
    <ScrollArea scrollbars="vertical" style={{ height: '100%' }}>
      <Box p="6">
        <Flex direction="column" gap="4">
          {/* Header */}
          <Box>
            <Heading size="6" mb="1">Go Search</Heading>
            <Text color="gray">Search for Go modules by module path, version, or keyword</Text>
          </Box>

          {/* Search Input */}
          <Flex gap="2" align="center">
            <Badge color="cyan" size="2" style={{ flexShrink: 0 }}>Go</Badge>
            <Box style={{ flex: 1 }}>
              <TextField.Root
                size="3"
                placeholder="Search Go modules..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                onKeyDown={handleSearchKeyDown}
                disabled={state.loading}
              >
                <TextField.Slot>
                  <Search size={16} />
                </TextField.Slot>
              </TextField.Root>
            </Box>
            <Button size="3" onClick={handleSearch} disabled={state.loading}>
              Search
            </Button>
          </Flex>

          {/* Filters */}
          <GolangSearchFilters
            values={filters}
            onChange={setFilters}
            onSearch={handleSearch}
            onClear={handleClear}
            loading={state.loading}
          />

          {/* Results */}
          <GolangSearchResults
            results={state.results}
            loading={state.loading}
            error={state.error}
            totalCount={state.totalCount}
            onSelect={handleSelect}
          />

          {/* Load More */}
          {hasMore && !state.loading && (
            <Flex justify="center">
              <Button variant="soft" onClick={loadMore}>
                Load More
              </Button>
            </Flex>
          )}
        </Flex>
      </Box>
    </ScrollArea>
  );
}

export default GolangSearchPage;
