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

import { buildDetailRoute } from '../core';
import { useGASearch } from './useGASearch';
import { GASearchFilters, FilterValues } from './GASearchFilters';
import { GASearchResults } from './GASearchResults';
import { FormatBadge } from '../../../shared';

export interface GASearchPageProps {
  /** Callback when navigating to detail page */
  onNavigateToDetail?: (gaId: string) => void;
  /** Initial search query from URL */
  initialQuery?: string;
  /** Initial groupId filter from URL */
  initialGroupId?: string;
  /** Initial artifactId filter from URL */
  initialArtifactId?: string;
}

/**
 * Main GA Search page component.
 * 
 * Features:
 * - Search input with typeahead
 * - Filter controls (groupId, artifactId, repository)
 * - Results table with sorting
 * - Pagination (load more)
 * - URL-driven state
 * 
 * Shows ONE row per GA (groupId:artifactId), NOT per version.
 */
export function GASearchPage({
  onNavigateToDetail,
  initialQuery = '',
  initialGroupId = '',
  initialArtifactId = '',
}: GASearchPageProps): JSX.Element {
  // Search state from hook
  const {
    state,
    search,
    loadMore,
    clear,
    setSort,
    hasMore,
  } = useGASearch({
    query: initialQuery,
  });

  // Local filter state
  const [filters, setFilters] = useState<FilterValues>({
    groupId: initialGroupId,
    artifactId: initialArtifactId,
    repository: '',
  });

  // Search input value (separate from committed query)
  const [searchInput, setSearchInput] = useState(initialQuery);

  // Perform initial search if we have initial values
  useEffect(() => {
    if (initialQuery || initialGroupId || initialArtifactId) {
      search({
        query: initialQuery,
        groupId: initialGroupId,
        artifactId: initialArtifactId,
      });
    }
  }, []); // Only on mount

  /**
   * Handle search submission.
   */
  const handleSearch = useCallback((): void => {
    search({
      query: searchInput,
      groupId: filters.groupId,
      artifactId: filters.artifactId,
      repository: filters.repository,
      sort: state.sort,
      sortDirection: state.sortDirection,
    });
  }, [search, searchInput, filters, state.sort, state.sortDirection]);

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
  const handleSelect = useCallback((gaId: string): void => {
    if (onNavigateToDetail) {
      onNavigateToDetail(gaId);
    } else {
      // Default: navigate using route builder
      const detailUrl = buildDetailRoute(gaId);
      window.location.hash = detailUrl;
    }
  }, [onNavigateToDetail]);

  /**
   * Handle clearing all filters and search.
   */
  const handleClear = useCallback((): void => {
    setSearchInput('');
    setFilters({
      groupId: '',
      artifactId: '',
      repository: '',
    });
    clear();
  }, [clear]);

  /**
   * Handle sort change.
   */
  const handleSortChange = useCallback((
    field: 'relevance' | 'lastUpdated' | 'name',
    direction: 'asc' | 'desc'
  ): void => {
    setSort(field, direction);
  }, [setSort]);

  return (
    <ScrollArea scrollbars="vertical" style={{ height: '100%' }}>
      <Box p="6">
        <Flex direction="column" gap="4">
          {/* Header */}
          <Box>
            <Heading size="6" mb="1">Maven Search</Heading>
            <Text color="gray">Search for Maven artifacts by Group ID and Artifact ID</Text>
          </Box>

          {/* Search Input */}
          <Flex gap="2" align="center">
            <Box style={{ flexShrink: 0 }}>
              <FormatBadge format="maven2" size={24} showLabel={false} />
            </Box>
            <Box style={{ flex: 1 }}>
              <TextField.Root
                size="3"
                placeholder="Search Maven artifacts..."
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
          <GASearchFilters
            values={filters}
            onChange={setFilters}
            onSearch={handleSearch}
            onClear={handleClear}
            loading={state.loading}
          />

          {/* Results */}
          <GASearchResults
            results={state.results}
            loading={state.loading}
            error={state.error}
            totalCount={state.totalCount}
            onSelect={handleSelect}
            sortField={state.sort}
            sortDirection={state.sortDirection}
            onSortChange={handleSortChange}
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

export default GASearchPage;


