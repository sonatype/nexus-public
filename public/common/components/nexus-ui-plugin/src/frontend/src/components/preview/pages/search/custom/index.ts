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
 * Custom Search Module
 *
 * Provides a dynamic query builder UI for creating custom search filters.
 */

// Main page component
export { CustomSearchPage } from './CustomSearchPage';
export type { CustomSearchPageProps } from './CustomSearchPage';

// Builder components
export { CustomSearchBuilder } from './CustomSearchBuilder';
export type { CustomSearchBuilderProps } from './CustomSearchBuilder';

export { CustomFilterRow } from './CustomFilterRow';
export type { CustomFilterRowProps } from './CustomFilterRow';

// Results components
export { CustomSearchResults } from './CustomSearchResults';
export type { CustomSearchResultsProps } from './CustomSearchResults';

export { CustomSearchPagination } from './CustomSearchPagination';
export type { CustomSearchPaginationProps } from './CustomSearchPagination';

// Hook
export { useCustomSearch } from './useCustomSearch';
export type { UseCustomSearchReturn } from './useCustomSearch';

// Types
export type {
  CustomFilter,
  CustomSearchResult,
  CustomSearchResponse,
  CustomSearchState,
  FilterField,
  FilterOperator,
  FilterFieldOption,
  FilterOperatorOption,
} from './custom.types';

export {
  FILTER_FIELD_OPTIONS,
  FILTER_OPERATOR_OPTIONS,
  createEmptyFilter,
} from './custom.types';
