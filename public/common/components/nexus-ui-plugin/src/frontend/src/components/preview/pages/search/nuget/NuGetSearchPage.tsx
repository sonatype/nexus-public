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

import type { NuGetSearchFilters as FilterValues } from './nuget.types';
import { useNuGetSearch } from './useNuGetSearch';
import { NuGetSearchFilters } from './NuGetSearchFilters';
import { NuGetSearchResults } from './NuGetSearchResults';

interface NuGetSearchPageProps {
  /** Callback when navigating to detail page */
  onNavigateToDetail?: (packageId: string) => void;
  /** Initial search query from URL */
  initialQuery?: string;
}

/**
 * Main NuGet Search page component.
 * 
 * Features:
 * - Search input
 * - NuGet-specific filters (packageId, version, prerelease, targetFramework)
 * - Results table with download counts
 * - Pagination
 */
export function NuGetSearchPage({
  onNavigateToDetail,
  initialQuery = '',
}: NuGetSearchPageProps): JSX.Element {
  const {
    state,
    search,
    loadMore,
    clear,
    hasMore,
  } = useNuGetSearch({ query: initialQuery });

  const [filters, setFilters] = useState<FilterValues>({});
  const [searchInput, setSearchInput] = useState(initialQuery);

  // Initial search if query provided
  useEffect(() => {
    if (initialQuery) {
      search({ query: initialQuery });
    }
  }, [initialQuery, search]);

  /**
   * Handle search submission.
   */
  const handleSearch = useCallback((): void => {
    search({
      query: searchInput,
      packageId: filters.packageId,
      version: filters.version,
      prerelease: filters.prerelease,
      targetFramework: filters.targetFramework,
    });
  }, [search, searchInput, filters]);

  /**
   * Handle search input keypress.
   */
  const handleSearchKeyDown = (e: React.KeyboardEvent): void => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  /**
   * Handle selecting a result.
   */
  const handleSelect = useCallback((packageId: string): void => {
    if (onNavigateToDetail) {
      onNavigateToDetail(packageId);
    } else {
      // Default: navigate using hash
      window.location.hash = `#preview/browse/search/nuget/${encodeURIComponent(packageId)}`;
    }
  }, [onNavigateToDetail]);

  /**
   * Handle clear.
   */
  const handleClear = useCallback((): void => {
    setSearchInput('');
    setFilters({});
    clear();
  }, [clear]);

  return (
    <ScrollArea scrollbars="vertical" style={{ height: '100%' }}>
      <Box p="6">
        <Flex direction="column" gap="4">
          {/* Header */}
          <Box>
            <Heading size="6" mb="1">NuGet Search</Heading>
            <Text color="gray">Search for .NET packages</Text>
          </Box>

          {/* Search Input */}
          <Flex gap="2">
            <Box style={{ flex: 1 }}>
              <TextField.Root
                size="3"
                placeholder="Search NuGet packages..."
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
          <NuGetSearchFilters
            values={filters}
            onChange={setFilters}
            onSearch={handleSearch}
            onClear={handleClear}
            loading={state.loading}
          />

          {/* Results */}
          <NuGetSearchResults
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

export default NuGetSearchPage;

