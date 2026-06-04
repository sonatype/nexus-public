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
 * Docker Search Module
 *
 * Public exports for the Docker search components.
 */

// Types
export type {
  DockerSearchFilters,
  DockerResult,
  DockerTag,
  DockerDetail,
  DockerSearchRequest,
  DockerSearchResponse,
  DockerSuggestion,
  DockerSuggestResponse,
  DockerSearchState,
  DockerDetailState,
} from './docker.types';

// Main page components
export { DockerSearchPage } from './DockerSearchPage';
export type { DockerSearchPageProps } from './DockerSearchPage';

export { DockerDetailPage } from './DockerDetailPage';
export type { DockerDetailPageProps } from './DockerDetailPage';

// Sub-components (for customization)
export { DockerSearchInput } from './DockerSearchInput';
export type { DockerSearchInputProps } from './DockerSearchInput';

export { DockerSearchFilters } from './DockerSearchFilters';
export type { DockerSearchFiltersProps, FilterValues } from './DockerSearchFilters';

export { DockerSearchResults } from './DockerSearchResults';
export type { DockerSearchResultsProps } from './DockerSearchResults';

export { DockerResultRow } from './DockerResultRow';
export type { DockerResultRowProps } from './DockerResultRow';

export { DockerSearchPagination } from './DockerSearchPagination';
export type { DockerSearchPaginationProps } from './DockerSearchPagination';

// Hook
export { useDockerSearch } from './useDockerSearch';
export type { UseDockerSearchReturn, DockerSearchParams } from './useDockerSearch';

// Mock data (for development only - remove when backend is ready)
export {
  mockResults,
  mockSuggestions,
  mockTags,
  mockDetail,
  mockSearchApi,
  mockSuggestApi,
  mockDetailApi,
} from './mockData';


