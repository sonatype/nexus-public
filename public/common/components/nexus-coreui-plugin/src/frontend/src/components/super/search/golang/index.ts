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
 * Go (Golang) Search Module
 *
 * Public exports for Go module search components.
 */

// Types
export type {
  GolangSearchFilters,
  GolangResult,
  GolangSearchResponse,
  GolangVersion,
  GolangDetail,
  GolangSearchState,
} from './golang.types';

// Main page components
export { GolangSearchPage } from './GolangSearchPage';
export type { GolangSearchPageProps } from './GolangSearchPage';

export { GolangDetailPage } from './GolangDetailPage';
export type { GolangDetailPageProps } from './GolangDetailPage';

// Sub-components
export { GolangSearchFilters } from './GolangSearchFilters';
export type { GolangSearchFiltersProps } from './GolangSearchFilters';

export { GolangSearchResults } from './GolangSearchResults';
export type { GolangSearchResultsProps } from './GolangSearchResults';

export { GolangResultRow } from './GolangResultRow';
export type { GolangResultRowProps } from './GolangResultRow';

// Hook
export { useGolangSearch } from './useGolangSearch';
export type { UseGolangSearchReturn } from './useGolangSearch';

// Mock data (for development only)
export { mockGolangResults, mockGolangSearchApi, mockGolangDetailApi } from './mockData';


