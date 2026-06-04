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
 * GA Search Results Module
 *
 * Public exports for the search results components.
 */

// Main page component
export { GASearchPage } from './GASearchPage';
export type { GASearchPageProps } from './GASearchPage';

// Sub-components (for customization)
export { GASearchInput } from './GASearchInput';
export type { GASearchInputProps } from './GASearchInput';

export { GASearchFilters } from './GASearchFilters';
export type { GASearchFiltersProps, FilterValues } from './GASearchFilters';

export { GASearchResults } from './GASearchResults';
export type { GASearchResultsProps } from './GASearchResults';

export { GAResultRow } from './GAResultRow';
export type { GAResultRowProps } from './GAResultRow';

export { GASearchPagination } from './GASearchPagination';
export type { GASearchPaginationProps } from './GASearchPagination';

// Hook
export { useGASearch } from './useGASearch';
export type { UseGASearchReturn, SearchParams } from './useGASearch';

// Mock data (for development only - remove when backend is ready)
export { mockResults, mockSearchApi, mockSuggestApi } from './mockData';

