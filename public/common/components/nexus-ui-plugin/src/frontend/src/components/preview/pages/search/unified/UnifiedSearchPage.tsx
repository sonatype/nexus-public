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
import MalwareBanner from '../../../shared/security/MalwareBanner';
import { useUnifiedSearch } from './useUnifiedSearch';
import { useRepositories } from './useRepositories';
import { useSearchNavigation } from './useSearchNavigation';
import {
  useSearchUrlState,
  DEFAULT_SORT_FIELD,
  DEFAULT_SORT_DIRECTION,
} from './useSearchUrlState';
import { getApiFormat } from './searchFilters';
import { isMockMode } from '../../../config/featureFlags';
import { getMockSearchTotalCount } from '../../browse/mockData';
import { useInstanceTotals } from '../../Welcome/dashboard/useInstanceTotals';
import type { SearchResult, SearchFormat, SortField, SortDirection } from './unified.types';

import './UnifiedSearchPage.scss';

/**
 * Delay before committing a search-state change to the browser URL. Chosen to
 * match the sidebar/results filter search debounce so a burst of keystrokes
 * produces a single history entry once the user pauses.
 */
const URL_WRITE_DEBOUNCE_MS = 350;

/**
 * Produce a canonical string for a search state, used as the echo-guard key
 * that keeps URL <-> machine in sync without feedback loops. Default sort is
 * normalized to undefined so a URL without sort params compares equal to the
 * machine's default sort.
 */
export function serializeSearchState(s: {
  format: SearchFormat;
  query: string;
  filters: Record<string, string>;
  sortField?: SortField;
  sortDirection?: SortDirection;
}): string {
  // Field and direction are normalized independently so a non-default direction
  // on the default field (e.g. lastUpdated / asc) is not silently collapsed to
  // the default — matching buildQueryString so the guard stays in sync with the URL.
  const sortField = s.sortField && s.sortField !== DEFAULT_SORT_FIELD ? s.sortField : undefined;
  const sortDirection =
    s.sortDirection && s.sortDirection !== DEFAULT_SORT_DIRECTION ? s.sortDirection : undefined;
  return JSON.stringify({
    format: s.format,
    query: s.query,
    filters: s.filters,
    sortField,
    sortDirection,
  });
}

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
    setFilters,
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
    !state.filters.nameOrVersion;

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
  const { repositories } = useRepositories(apiFormat || undefined);

  const { navigateToDetail } = useSearchNavigation();
  const { readFromUrl, syncToUrl } = useSearchUrlState();

  // Serialized snapshot of the last state we read from OR wrote to the URL.
  // Used to break the echo loop: a URL-driven rehydration must not immediately
  // trigger a write of the same state back to the URL, and vice versa.
  const lastSerialized = React.useRef<string | null>(null);

  // Debounce timer for URL writes. Rapid state changes (e.g. typing in a filter
  // field, where state.filters updates on every keystroke) must not create a
  // browser-history entry per character — that pollutes history and breaks
  // Back/Forward. We coalesce them into a single pushState after the user pauses.
  const urlWriteTimerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null);

  /**
   * Rehydrate the search machine from a URL state, then run a single search.
   * Uses bulk setFilters so all filters are applied in one machine transition
   * (avoids N sequential sends and the previous fragile setTimeout chain).
   *
   * Format and sort are always applied (including their defaults) so a
   * Back/Forward to a URL without those params resets the machine rather than
   * retaining stale values.
   */
  const rehydrateFromUrl = useCallback(
    (url: ReturnType<typeof readFromUrl>) => {
      // Cancel any pending URL write so a queued keystroke-write can't fire
      // after (and overwrite) a Back/Forward navigation or deep-link load.
      if (urlWriteTimerRef.current) {
        clearTimeout(urlWriteTimerRef.current);
        urlWriteTimerRef.current = null;
      }
      lastSerialized.current = serializeSearchState(url);

      // Always apply the format so a Back/Forward to a URL without a format
      // param resets the machine to 'all' rather than retaining a stale format.
      setFormat(url.format);
      setSelectedFormat(url.format === 'all' ? '' : url.format);
      setQuery(url.query);
      setFilters(url.filters);
      // Always apply sort, resolving missing or invalid params to the canonical
      // defaults. Applying it conditionally let the machine keep a previous
      // non-default sort on a Back to a parameterless URL, which Effect 2 then
      // wrote back onto that history entry.
      setSort(url.sortField ?? DEFAULT_SORT_FIELD, url.sortDirection ?? DEFAULT_SORT_DIRECTION);
      // Defer the search to the next tick so the machine has processed the bulk
      // state above. This relies on XState's default SYNCHRONOUS send: the
      // setFormat/setQuery/setFilters/setSort events are all applied to context
      // before this timeout fires. If the machine is ever switched to async
      // dispatch, this ordering guarantee breaks and search() would run against
      // stale context — consolidate into a single machine action if that happens.
      setTimeout(() => search(), 0);
    },
    [setFormat, setQuery, setFilters, setSort, search],
  );

  // Mount: restore state from the URL. The URL is the only source; there is no
  // sessionStorage fallback. A machine-state copy in sessionStorage used to take
  // precedence here, which meant a stale payload — copied into a tab opened from
  // a link, or simply left over from an earlier detail visit — beat a fresh deep
  // link or header search (NEXUS-54503 Defect 1). The URL now carries every
  // criterion, `nameOrVersion` included, so the fallback had nothing left to add.
  useEffect(() => {
    rehydrateFromUrl(readFromUrl());
    // Mount-only: intentionally run once.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Serialize machine state -> URL whenever search state changes.
  //
  // The write is debounced so a burst of rapid changes (typing in a filter
  // field updates state.filters on every keystroke) collapses into a single
  // history entry instead of one entry per character. Any pending write is
  // cancelled and rescheduled on each change; when it finally fires it does a
  // single pushState for the settled value, so Back/Forward steps between
  // meaningful states rather than individual keystrokes.
  useEffect(() => {
    const serialized = serializeSearchState(state);
    // Skip if this exact state came from (or was already written to) the URL.
    if (serialized === lastSerialized.current) {
      return;
    }

    const snapshot = {
      format: state.format,
      query: state.query,
      filters: state.filters,
      sortField: state.sortField,
      sortDirection: state.sortDirection,
    };

    if (urlWriteTimerRef.current) {
      clearTimeout(urlWriteTimerRef.current);
    }
    urlWriteTimerRef.current = setTimeout(() => {
      urlWriteTimerRef.current = null;
      // Re-check the guard in case a URL-driven rehydration landed on this same
      // state while the write was pending.
      if (serialized === lastSerialized.current) {
        return;
      }
      lastSerialized.current = serialized;
      syncToUrl(snapshot);
    }, URL_WRITE_DEBOUNCE_MS);

    return () => {
      if (urlWriteTimerRef.current) {
        clearTimeout(urlWriteTimerRef.current);
        urlWriteTimerRef.current = null;
      }
    };
  }, [state.format, state.query, state.filters, state.sortField, state.sortDirection, syncToUrl]);

  // Back/forward (popstate) and header-driven hash changes: re-read the URL and
  // rehydrate the machine.
  useEffect(() => {
    const handleUrlChange = () => {
      if (window.location.hash.includes('preview/browse/search')) {
        const url = readFromUrl();
        // Ignore changes that merely reflect state we already hold.
        if (serializeSearchState(url) === lastSerialized.current) {
          return;
        }
        rehydrateFromUrl(url);
      }
    };
    window.addEventListener('popstate', handleUrlChange);
    window.addEventListener('hashchange', handleUrlChange);
    return () => {
      window.removeEventListener('popstate', handleUrlChange);
      window.removeEventListener('hashchange', handleUrlChange);
    };
  }, [readFromUrl, rehydrateFromUrl]);

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
    // reset() clears all machine state (format -> 'all', query -> '', filters ->
    // {}, sort -> defaults); we only clear the local selectedFormat mirror here.
    // The subsequent deferred search re-runs against the cleared state.
    setSelectedFormat('');
    reset();
    // Defer to the next tick so the machine applies reset() before search()
    // reads context. Relies on XState's synchronous send (see rehydrateFromUrl).
    setTimeout(() => search(), 0);
  }, [reset, search]);

  /**
   * Write the current state to the URL now, cancelling any pending debounced
   * write.
   *
   * Called before navigating away. A queued write would otherwise fire after the
   * navigation, or be cancelled by unmount, leaving the URL one edit behind —
   * and the URL is exactly what `navigateToDetail` captures as the breadcrumb's
   * return target. Uses replace, not push: this rewrites the history entry the
   * user is standing on, and the navigation that follows adds its own.
   */
  const flushUrlWrite = useCallback(() => {
    if (urlWriteTimerRef.current) {
      clearTimeout(urlWriteTimerRef.current);
      urlWriteTimerRef.current = null;
    }
    const snapshot = {
      format: state.format,
      query: state.query,
      filters: state.filters,
      sortField: state.sortField,
      sortDirection: state.sortDirection,
    };
    lastSerialized.current = serializeSearchState(snapshot);
    syncToUrl(snapshot, true);
  }, [
    state.format,
    state.query,
    state.filters,
    state.sortField,
    state.sortDirection,
    syncToUrl,
  ]);

  const handleSelect = useCallback(
    (result: SearchResult) => {
      flushUrlWrite();
      navigateToDetail(result);
    },
    [flushUrlWrite, navigateToDetail],
  );

  const handleRetry = useCallback(() => {
    search();
  }, [search]);

  /**
   * The results-page filter input is the single editable home of the search
   * text. On edit it takes ownership from the global header bar: `query` is
   * cleared and the value moves to `nameOrVersion`, so exactly one text value is
   * ever live. Without that, `buildQueryParams` would space-join the two into a
   * single API `q` and the user would get results for both terms at once.
   *
   * Both sends land before the deferred search runs — see the note on
   * `rehydrateFromUrl` about XState's synchronous dispatch.
   */
  const handleNameFilterChange = useCallback(
    (value: string) => {
      setQuery('');
      handleFilterChange('nameOrVersion', value);
      // Defer search to the next tick so both events above are processed before
      // SEARCH runs. 0, like every other deferred search here: XState's default
      // dispatch is synchronous, so both sends have already landed in context by
      // the time this fires, and no wall-clock delay buys anything.
      setTimeout(() => search(), 0);
    },
    [handleFilterChange, search, setQuery]
  );

  /**
   * Sort handler for the results-header dropdown — the only sorting control.
   * It reports an explicit field AND direction, so there is no direction
   * inference here.
   */
  const handleSortChange = useCallback(
    (field: SortField, direction: SortDirection) => {
      setSort(field, direction);
      // Defer to the next tick so the machine applies SET_SORT before search()
      // reads context. Relies on XState's synchronous send (see rehydrateFromUrl).
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
              // Display fallback: a query arriving from the global header bar as
              // ?q= must be visible in this input (BDD-002). nameOrVersion wins
              // when set, because an edit here transfers ownership to it.
              nameFilter={state.filters.nameOrVersion || state.query}
              onNameFilterChange={handleNameFilterChange}
              sortField={state.sortField}
              sortDirection={state.sortDirection}
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
