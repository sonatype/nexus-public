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

/**
 * Unified Search Module
 * 
 * Single search page that supports all formats with dynamic filters.
 */

// Types
export type {
  SearchFormat,
  FormatInfo,
  FilterType,
  FilterDefinition,
  FilterValues,
  FormatFilterConfig,
  SearchResult,
  SearchResponse,
  SortField,
  SortDirection,
  UnifiedSearchState,
  SearchHeaderProps,
  SearchSidebarProps,
  SearchResultsProps,
  SearchResultCardProps,
} from './unified.types';

// Filter definitions
export {
  FORMATS,
  FORMAT_FILTERS,
  getFormatOptions,
  getFiltersForFormat,
  getPlaceholderForFormat,
  getApiFormat,
  buildQueryParams,
} from './searchFilters';

// Hooks
export { useUnifiedSearch } from './useUnifiedSearch';
export type { UseUnifiedSearchReturn } from './useUnifiedSearch';

// Agent 1: Repository data hook
export { useRepositories } from './useRepositories';
export type { Repository, UseRepositoriesResult } from './useRepositories';

// Agent 2: URL state sync hook
export { useSearchUrlState } from './useSearchUrlState';
export type { UrlSearchState, UseSearchUrlStateReturn } from './useSearchUrlState';

// Agent 3: Navigation hook
export { useSearchNavigation } from './useSearchNavigation';
export type {
  DetailRoute,
  UseSearchNavigationReturn,
  SearchStateToPreserve,
} from './useSearchNavigation';
export { COMPONENT_DETAIL_RETURN_SEARCH_KEY } from './useSearchNavigation';

// Components
export { default as SearchHeader } from './SearchHeader';
export { SearchSidebar } from './SearchSidebar';
export type { SearchSidebarProps } from './SearchSidebar';
export { SearchResults } from './SearchResults';
export { SearchResultCard } from './SearchResultCard';
export { default as UnifiedSearchPage } from './UnifiedSearchPage';

// Filter components
export { FilterSection, TextFilter, SelectFilter } from './filters';
export type { FilterSectionProps, TextFilterProps, SelectFilterProps, SelectFilterOption } from './filters';
