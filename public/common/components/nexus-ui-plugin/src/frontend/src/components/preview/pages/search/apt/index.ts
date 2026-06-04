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
 * Apt/Debian Search Module
 *
 * Public exports for Apt (Debian/Ubuntu) package search components.
 */

// Types
export type {
  AptSearchFilters,
  AptResult,
  AptSearchResponse,
  AptVersion,
  AptDetail,
  AptSearchState,
} from './apt.types';

// Main page components
export { AptSearchPage } from './AptSearchPage';
export type { AptSearchPageProps } from './AptSearchPage';

export { AptDetailPage } from './AptDetailPage';
export type { AptDetailPageProps } from './AptDetailPage';

// Sub-components
export { AptSearchFilters } from './AptSearchFilters';
export type { AptSearchFiltersProps } from './AptSearchFilters';

export { AptSearchResults } from './AptSearchResults';
export type { AptSearchResultsProps } from './AptSearchResults';

export { AptResultRow } from './AptResultRow';
export type { AptResultRowProps } from './AptResultRow';

// Hook
export { useAptSearch } from './useAptSearch';
export type { UseAptSearchReturn } from './useAptSearch';

// Mock data (for development only)
export { mockAptResults, mockAptSearchApi, mockAptDetailApi } from './mockData';


