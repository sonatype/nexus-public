/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React, { useCallback, useEffect, useState } from 'react';
import { Box, Flex, Grid } from '@radix-ui/themes';

import { SearchSidebar } from './SearchSidebar';
import { SearchResults } from './SearchResults';
import { MobileFilterDrawer } from './MobileFilterDrawer';
import MalwareBanner from '@/components/shared/security/MalwareBanner';
import { useUnifiedSearch } from './useUnifiedSearch';
import { useRepositories } from './useRepositories';
import { useSearchNavigation, COMPONENT_DETAIL_RETURN_SEARCH_KEY } from './useSearchNavigation';
import { useSearchUrlState } from './useSearchUrlState';
import { getApiFormat } from './searchFilters';
import { isMockMode } from '@/config/previewFeatureFlags';
import { getMockSearchTotalCount } from '@/components/super/browse/mockData';
import { useInstanceTotals } from '@/components/super/pages/Welcome/dashboard/useInstanceTotals';
import type { SearchResult, SearchFormat } from './unified.types';

import './UnifiedSearchPage.scss';

/**
 * UnifiedSearchPage - Single search page for ALL formats
 *
 * Layout matches nexusone-ux-prototype v1/components/page.tsx:
 * - Box px="5" (24px) py="5" → Flex direction="column" gap="6" → Grid columns="250px 1fr"
 * - Sidebar hidden on mobile
 * - Single format selection via dropdown
 */
export default function UnifiedSearchPage(): JSX.Element {
  const [selectedFormat, setSelectedFormat] = useState<SearchFormat | ''>('');
  const [showMobileFilters, setShowMobileFilters] = useState(false);

  const {
    state,
    setFormat,
    setQuery,
    setFilter,
    setSort,
    search,
    loadMore,
    reset,
    hasMore,
  } = useUnifiedSearch();

  const handleFormatChange = useCallback(
    (format: SearchFormat | '') => {
      setSelectedFormat(format);
      setFormat(format === '' || format === 'all' ? 'all' : format);
    },
    [setFormat],
  );

  const { data: instanceTotals } = useInstanceTotals();

  const isUnfiltered =
    !state.query &&
    state.format === 'all' &&
    !state.filters['nameOrVersion'];

  // When unfiltered: show instance total (from contentUsageEvaluationResult or mock).
  // When filtered: show search result count.
  const displayTotalCount = (() => {
    if (!isUnfiltered) return state.totalCount;
    if (isMockMode()) return getMockSearchTotalCount();
    const instanceTotal = instanceTotals?.totalComponents;
    if (typeof instanceTotal === 'number' && instanceTotal >= 0) return instanceTotal;
    return state.totalCount;
  })();

  const apiFormat =
    state.format !== 'all' ? getApiFormat(state.format) : undefined;
  const { repositories, availableFormats } = useRepositories(apiFormat || undefined);

  const { navigateToDetail } = useSearchNavigation();
  const { state: urlState } = useSearchUrlState();

  const lastUrlQuery = React.useRef<string | null>(null);

  const syncFromUrl = useCallback(() => {
    const hashParams = new URLSearchParams(window.location.hash.split('?')[1] || '');
    const urlQuery = hashParams.get('q') || '';

    if (urlQuery === lastUrlQuery.current) {
      return;
    }
    lastUrlQuery.current = urlQuery;

    if (urlQuery !== state.query) {
      setQuery(urlQuery);
    }

    setTimeout(() => search(), 50);
  }, [state.query, setQuery, search]);

  useEffect(() => {
    // Restore search state when returning from component detail (breadcrumb click)
    const stored = sessionStorage.getItem(COMPONENT_DETAIL_RETURN_SEARCH_KEY);
    if (stored) {
      try {
        const { query, format, filters } = JSON.parse(stored);
        sessionStorage.removeItem(COMPONENT_DETAIL_RETURN_SEARCH_KEY);
        if (format && format !== 'all') {
          setFormat(format);
          setSelectedFormat(format);
        }
        if (query) setQuery(query);
        Object.entries(filters || {}).forEach(([id, value]) => {
          if (value) setFilter(id, value);
        });
        setTimeout(() => search(), 50);
        return;
      } catch {
        sessionStorage.removeItem(COMPONENT_DETAIL_RETURN_SEARCH_KEY);
      }
    }

    if (urlState.format !== 'all') {
      setFormat(urlState.format);
      setSelectedFormat(urlState.format);
    }
    Object.entries(urlState.filters).forEach(([id, value]) => {
      setFilter(id, value);
    });
    syncFromUrl();
  }, []);

  useEffect(() => {
    const handleHashChange = () => {
      if (window.location.hash.includes('preview/browse/search')) {
        syncFromUrl();
      }
    };
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [syncFromUrl]);

  const handleFilterChange = useCallback(
    (filterId: string, value: string) => {
      setFilter(filterId, value);
    },
    [setFilter]
  );

  const handleSidebarSearch = useCallback(() => {
    search();
  }, [search]);

  const handleReset = useCallback(() => {
    setSelectedFormat('');
    reset();
    setTimeout(() => search(), 0);
  }, [reset, search]);

  const handleSelect = useCallback(
    (result: SearchResult) => {
      const searchState = {
        query: state.query,
        format: state.format,
        filters: state.filters,
      };
      navigateToDetail(result, searchState);
    },
    [navigateToDetail, state.query, state.format, state.filters],
  );

  const handleRetry = useCallback(() => {
    search();
  }, [search]);

  const handleNameFilterChange = useCallback(
    (value: string) => {
      handleFilterChange('nameOrVersion', value);
      // Defer search to next tick so UPDATE_FILTER is processed before SEARCH runs
      setTimeout(() => search(), 50);
    },
    [handleFilterChange, search]
  );

  const handleSortChange = useCallback(
    (value: string) => {
      const field = value as 'name' | 'version' | 'lastUpdated' | 'repository';
      const direction = field === 'lastUpdated' || field === 'version' ? 'desc' : 'asc';
      setSort(field, direction);
      setTimeout(() => search(), 0);
    },
    [setSort, search]
  );

  return (
    <Box
      className="unified-search-page"
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
      width="100%"
      style={{
        minWidth: 0,
        boxSizing: 'border-box',
      }}
    >
      <Flex direction="column" gap="6" width="100%" style={{ minWidth: 0 }}>
        {/* Malware Alert Banner */}
        <MalwareBanner />
        <Grid
          columns={{ initial: '1', sm: '250px 1fr' }}
          gap="6"
          width="100%"
          style={{ minWidth: 0 }}
        >
          <Box
            className="filter-bar"
            display={{ initial: 'none', sm: 'block' }}
            style={{ overflow: 'visible', minWidth: 0 }}
            role="complementary"
            aria-label="Filter bar"
          >
            <SearchSidebar
              selectedFormat={selectedFormat}
              onFormatChange={handleFormatChange}
              onSearch={handleSidebarSearch}
              availableFormats={isMockMode() ? undefined : availableFormats}
              disabled={state.loading}
              filters={state.filters}
              onFilterChange={handleFilterChange}
              onReset={handleReset}
              repositories={repositories}
            />
          </Box>
          <Box
            className="page-content"
            minWidth="0"
            width="100%"
            role="main"
            aria-label="Page content"
          >
            <SearchResults
              results={state.results}
              loading={state.loading}
              error={state.error}
              totalCount={displayTotalCount}
              hasMore={hasMore}
              onLoadMore={loadMore}
              onSelect={handleSelect}
              onRetry={handleRetry}
              query={state.query}
              nameFilter={state.filters['nameOrVersion'] || ''}
              onNameFilterChange={handleNameFilterChange}
              sortBy={state.sortField}
              onSortChange={handleSortChange}
              onOpenMobileFilters={() => setShowMobileFilters(true)}
            />
          </Box>
        </Grid>
      </Flex>

      <MobileFilterDrawer
        isOpen={showMobileFilters}
        onClose={() => setShowMobileFilters(false)}
        title="Filter"
        onClearAll={handleReset}
      >
        <SearchSidebar
          inDrawer
          selectedFormat={selectedFormat}
          onFormatChange={handleFormatChange}
          onSearch={() => {
            handleSidebarSearch();
            setShowMobileFilters(false);
          }}
          availableFormats={isMockMode() ? undefined : availableFormats}
          disabled={state.loading}
          filters={state.filters}
          onFilterChange={handleFilterChange}
          onReset={handleReset}
          repositories={repositories}
        />
      </MobileFilterDrawer>
    </Box>
  );
}
