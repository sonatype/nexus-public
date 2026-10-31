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
  ScrollArea,
} from '@radix-ui/themes';
import { Search } from 'lucide-react';

import type { AptSearchFilters as FilterValues } from './apt.types';
import { useAptSearch } from './useAptSearch';
import { AptSearchFilters } from './AptSearchFilters';
import { AptSearchResults } from './AptSearchResults';

export interface AptSearchPageProps {
  /** Callback when navigating to detail page */
  onNavigateToDetail?: (id: string) => void;
  /** Initial name filter from URL */
  initialName?: string;
  /** Initial version filter from URL */
  initialVersion?: string;
}

/**
 * Main Apt Search page component.
 *
 * Features:
 * - Search by name, version, architecture, distribution, component
 * - Results table showing package info
 * - Click row to navigate to detail page
 * - URL-driven state (bookmarkable)
 */
export function AptSearchPage({
  onNavigateToDetail,
  initialName = '',
  initialVersion = '',
}: AptSearchPageProps): JSX.Element {
  const { state, search, loadMore, clear, hasMore } = useAptSearch();

  // Search input state
  const [searchInput, setSearchInput] = useState(initialName);

  // Local filter state
  const [filters, setFilters] = useState<FilterValues>({
    name: initialName,
    version: initialVersion,
    architecture: '',
    distribution: '',
    component: '',
  });

  // Perform initial search if we have initial values
  useEffect(() => {
    if (initialName || initialVersion) {
      search({
        name: initialName,
        version: initialVersion,
      });
    }
  }, [initialName, initialVersion, search]); // Only on mount

  /**
   * Handle search submission.
   */
  const handleSearch = useCallback((): void => {
    search({
      ...filters,
      name: searchInput || filters.name,
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
      window.location.hash = `#preview/browse/search/apt/detail/${encodeURIComponent(id)}`;
    }
  }, [onNavigateToDetail]);

  /**
   * Handle clearing all filters.
   */
  const handleClear = useCallback((): void => {
    setSearchInput('');
    setFilters({
      name: '',
      version: '',
      architecture: '',
      distribution: '',
      component: '',
    });
    clear();
  }, [clear]);

  return (
    <ScrollArea scrollbars="vertical" style={{ height: '100%' }}>
      <Box p="6">
        <Flex direction="column" gap="4">
          {/* Header */}
          <Box>
            <Heading size="6" mb="1">Apt/Debian Search</Heading>
            <Text color="gray">Search for Debian and Ubuntu packages by name, version, architecture, or distribution</Text>
          </Box>

          {/* Search Input */}
          <Flex gap="2">
            <Box style={{ flex: 1 }}>
              <TextField.Root
                size="3"
                placeholder="Search Apt packages..."
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
          <AptSearchFilters
            values={filters}
            onChange={setFilters}
            onSearch={handleSearch}
            onClear={handleClear}
            loading={state.loading}
          />

          {/* Results */}
          <AptSearchResults
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

export default AptSearchPage;
