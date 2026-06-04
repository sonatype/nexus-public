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
 * PyPI Search Module
 *
 * Public exports for PyPI (Python Package Index) search components.
 */

// Types
export type {
  PyPISearchFilters,
  PyPIResult,
  PyPISearchResponse,
  PyPIVersion,
  PyPIDetail,
  PyPISearchState,
} from './pypi.types';

// Main page components
export { PyPISearchPage } from './PyPISearchPage';
export type { PyPISearchPageProps } from './PyPISearchPage';

export { PyPIDetailPage } from './PyPIDetailPage';
export type { PyPIDetailPageProps } from './PyPIDetailPage';

// Sub-components
export { PyPISearchFilters } from './PyPISearchFilters';
export type { PyPISearchFiltersProps } from './PyPISearchFilters';

export { PyPISearchResults } from './PyPISearchResults';
export type { PyPISearchResultsProps } from './PyPISearchResults';

export { PyPIResultRow } from './PyPIResultRow';
export type { PyPIResultRowProps } from './PyPIResultRow';

// Hook
export { usePyPISearch } from './usePyPISearch';
export type { UsePyPISearchReturn } from './usePyPISearch';

// Mock data (for development only)
export { mockPyPIResults, mockPyPISearchApi, mockPyPIDetailApi } from './mockData';


