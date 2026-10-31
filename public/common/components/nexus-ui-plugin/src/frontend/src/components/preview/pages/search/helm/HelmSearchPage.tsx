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

import type { HelmSearchFilters as FilterValues } from './helm.types';
import { useHelmSearch } from './useHelmSearch';
import { HelmSearchFilters } from './HelmSearchFilters';
import { HelmSearchResults } from './HelmSearchResults';

export interface HelmSearchPageProps {
  /** Callback when navigating to detail page */
  onNavigateToDetail?: (id: string) => void;
  /** Initial name filter from URL */
  initialName?: string;
  /** Initial version filter from URL */
  initialVersion?: string;
}

/**
 * Main Helm Search page component.
 *
 * Features:
 * - Search by chart name, version, app version, description
 * - Results table showing chart info with icons
 * - Click row to navigate to detail page
 * - URL-driven state (bookmarkable)
 */
export function HelmSearchPage({
  onNavigateToDetail,
  initialName = '',
  initialVersion = '',
}: HelmSearchPageProps): JSX.Element {
  const { state, search, loadMore, clear, hasMore } = useHelmSearch();

  // Search input state
  const [searchInput, setSearchInput] = useState(initialName);

  // Local filter state
  const [filters, setFilters] = useState<FilterValues>({
    name: initialName,
    version: initialVersion,
    appVersion: '',
    description: '',
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
      window.location.hash = `#preview/browse/search/helm/detail/${encodeURIComponent(id)}`;
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
      appVersion: '',
      description: '',
    });
    clear();
  }, [clear]);

  return (
    <ScrollArea scrollbars="vertical" style={{ height: '100%' }}>
      <Box p="6">
        <Flex direction="column" gap="4">
          {/* Header */}
          <Box>
            <Heading size="6" mb="1">Helm Search</Heading>
            <Text color="gray">Search for Kubernetes Helm charts by name, version, or description</Text>
          </Box>

          {/* Search Input */}
          <Flex gap="2" align="center">
            <Badge color="blue" size="2" style={{ flexShrink: 0 }}>Helm</Badge>
            <Box style={{ flex: 1 }}>
              <Text as="label" htmlFor="helm-search-input" size="1" color="gray" mb="1">
                Chart Name
              </Text>
              <TextField.Root
                id="helm-search-input"
                size="3"
                placeholder="Search Helm charts..."
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
          <HelmSearchFilters
            values={filters}
            onChange={setFilters}
            onSearch={handleSearch}
            onClear={handleClear}
            loading={state.loading}
          />

          {/* Results */}
          <HelmSearchResults
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

export default HelmSearchPage;
