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
import { useRouter } from '@uirouter/react';

import type { GenericSearchFilters as FilterValues, GenericResult } from './generic.types';
import { useGenericSearch } from './useGenericSearch';
import { GenericSearchFilters } from './GenericSearchFilters';
import { GenericSearchResults } from './GenericSearchResults';
import MalwareBanner from '../../../shared/security/MalwareBanner';

export interface GenericSearchPageProps {
  /** Callback when navigating to detail page */
  onNavigateToDetail?: (id: string) => void;
  /** Initial keyword filter from URL */
  initialKeyword?: string;
  /** Initial format filter from URL */
  initialFormat?: string;
}

/**
 * Main Generic Search page component.
 *
 * Features:
 * - Search by keyword, format, repository, group, name, version
 * - Results table showing components from ALL formats
 * - Click row to navigate to detail page
 * - URL-driven state (bookmarkable)
 */
export function GenericSearchPage({
  onNavigateToDetail,
  initialKeyword = '',
  initialFormat = '',
}: GenericSearchPageProps): JSX.Element {
  const router = useRouter();
  const { state, search, loadMore, clear, hasMore } = useGenericSearch();

  // Search input state
  const [searchInput, setSearchInput] = useState(initialKeyword);

  // Local filter state
  const [filters, setFilters] = useState<FilterValues>({
    q: initialKeyword,
    format: initialFormat,
    repository: '',
    group: '',
    name: '',
    version: '',
  });

  // Perform initial search if we have initial values
  useEffect(() => {
    if (initialKeyword) {
      search({ q: initialKeyword, format: initialFormat || undefined });
    }
  }, []); // Only on mount

  /**
   * Handle search submission.
   */
  const handleSearch = useCallback((): void => {
    // Remove empty string values
    const cleanFilters: FilterValues = {};
    const q = searchInput || filters.q;
    if (q) cleanFilters.q = q;
    if (filters.format) cleanFilters.format = filters.format;
    if (filters.repository) cleanFilters.repository = filters.repository;
    if (filters.group) cleanFilters.group = filters.group;
    if (filters.name) cleanFilters.name = filters.name;
    if (filters.version) cleanFilters.version = filters.version;

    search(cleanFilters);
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
   * Handle selecting a result to view details.
   * Navigates to the unified AssetDetailPage with Component Tags.
   */
  const handleSelect = useCallback((id: string): void => {
    if (onNavigateToDetail) {
      onNavigateToDetail(id);
      return;
    }

    // Find the result to get repository and asset info
    const result = state.results.find((r: GenericResult) => r.id === id);
    if (!result) {
      console.warn('Result not found for id:', id);
      return;
    }

    // Get the first asset from the component (or use component id as fallback)
    const assetId = result.assets?.[0]?.id || id;
    const repositoryName = result.repository;

    // Navigate to unified asset detail page
    router.stateService.go('preview.browse.search.asset', {
      repositoryName,
      assetId: btoa(assetId), // Base64 encode for URL safety
      componentId: id,
    });
  }, [onNavigateToDetail, state.results, router]);

  /**
   * Handle clearing all filters.
   */
  const handleClear = useCallback((): void => {
    setSearchInput('');
    setFilters({
      q: '',
      format: '',
      repository: '',
      group: '',
      name: '',
      version: '',
    });
    clear();
  }, [clear]);

  return (
    <ScrollArea scrollbars="vertical" style={{ height: '100%' }}>
      <Box p="6">
        <Flex direction="column" gap="4">
          {/* Header */}
          <Box>
            <Heading size="6" mb="1">Search</Heading>
            <Text color="gray">Search for components across all repository formats</Text>
          </Box>

          {/* Malware Alert Banner */}
          <MalwareBanner />

          {/* Search Input */}
          <Flex gap="2">
            <Box style={{ flex: 1 }}>
              <TextField.Root
                size="3"
                placeholder="Search all components..."
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
          <GenericSearchFilters
            values={filters}
            onChange={setFilters}
            onSearch={handleSearch}
            onClear={handleClear}
            loading={state.loading}
          />

          {/* Results */}
          <GenericSearchResults
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

export default GenericSearchPage;
