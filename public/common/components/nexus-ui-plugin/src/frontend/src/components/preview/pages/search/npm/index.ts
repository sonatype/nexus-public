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
 * npm Search Module
 *
 * Public exports for npm package search components.
 */

// Types
export type {
  NpmSearchFilters,
  NpmResult,
  NpmSearchResponse,
  NpmVersion,
  NpmDetail,
  NpmSearchState,
} from './npm.types';

// Main page components
export { NpmSearchPage } from './NpmSearchPage';
export type { NpmSearchPageProps } from './NpmSearchPage';

export { NpmDetailPage } from './NpmDetailPage';
export type { NpmDetailPageProps } from './NpmDetailPage';

// Sub-components
export { NpmSearchFilters } from './NpmSearchFilters';
export type { NpmSearchFiltersProps } from './NpmSearchFilters';

export { NpmSearchResults } from './NpmSearchResults';
export type { NpmSearchResultsProps } from './NpmSearchResults';

export { NpmResultRow } from './NpmResultRow';
export type { NpmResultRowProps } from './NpmResultRow';

// Hook
export { useNpmSearch } from './useNpmSearch';
export type { UseNpmSearchReturn } from './useNpmSearch';

// Mock data (for development only)
export { mockNpmResults, mockNpmSearchApi, mockNpmDetailApi } from './mockData';

