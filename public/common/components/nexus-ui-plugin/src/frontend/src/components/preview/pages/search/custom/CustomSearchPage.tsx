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

import React, { useState } from 'react';
import {
  Box,
  Flex,
  Heading,
  Text,
  Button,
  ScrollArea,
} from '@radix-ui/themes';

import { useCustomSearch } from './useCustomSearch';
import { CustomSearchBuilder } from './CustomSearchBuilder';
import { CustomSearchResults } from './CustomSearchResults';

export interface CustomSearchPageProps {
  /** Optional callback when navigating to component detail */
  onNavigateToDetail?: (componentId: string) => void;
}

/**
 * Custom Search Page component.
 *
 * Allows users to build custom search queries using dynamic filters.
 * Supports multiple filter criteria with different fields and operators.
 *
 * Features:
 * - Dynamic filter builder (add/remove filter rows)
 * - Multiple field types (format, repository, group, name, version, tag, keyword)
 * - Multiple operators (equals, contains, startsWith, endsWith)
 * - Results display across all formats
 * - Pagination support
 */
export function CustomSearchPage({
  onNavigateToDetail,
}: CustomSearchPageProps): JSX.Element {
  // Track whether a search has been performed
  const [hasSearched, setHasSearched] = useState(false);

  const {
    state,
    addFilter,
    removeFilter,
    updateFilter,
    search,
    loadMore,
    clear,
    hasMore,
    hasFilters,
  } = useCustomSearch();

  /**
   * Handle search execution.
   */
  const handleSearch = async (): Promise<void> => {
    setHasSearched(true);
    await search();
  };

  /**
   * Handle clear all.
   */
  const handleClear = (): void => {
    setHasSearched(false);
    clear();
  };

  return (
    <ScrollArea scrollbars="vertical" style={{ height: '100%' }}>
      <Box p="6">
        <Flex direction="column" gap="4">
          {/* Header */}
          <Box>
            <Heading size="6" mb="1">Custom Search</Heading>
            <Text color="gray">Build custom search queries with multiple filter criteria</Text>
          </Box>

          {/* Search Builder */}
          <CustomSearchBuilder
            filters={state.filters}
            onUpdateFilter={updateFilter}
            onRemoveFilter={removeFilter}
            onAddFilter={addFilter}
            onSearch={handleSearch}
            onClear={handleClear}
            loading={state.loading}
            hasFilters={hasFilters}
          />

          {/* Results */}
          <CustomSearchResults
            results={state.results}
            loading={state.loading}
            error={state.error}
            totalCount={state.totalCount}
            isInitial={!hasSearched}
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

export default CustomSearchPage;
