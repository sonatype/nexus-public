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
 * RubyGems Search Module
 *
 * Public exports for RubyGems search components.
 */

// Types
export type {
  RubyGemsSearchFilters,
  RubyGemsResult,
  RubyGemsSearchResponse,
  RubyGemsVersion,
  RubyGemsDetail,
  RubyGemsSearchState,
} from './rubygems.types';

// Main page components
export { RubyGemsSearchPage } from './RubyGemsSearchPage';
export type { RubyGemsSearchPageProps } from './RubyGemsSearchPage';

export { RubyGemsDetailPage } from './RubyGemsDetailPage';
export type { RubyGemsDetailPageProps } from './RubyGemsDetailPage';

// Sub-components
export { RubyGemsSearchFilters } from './RubyGemsSearchFilters';
export type { RubyGemsSearchFiltersProps } from './RubyGemsSearchFilters';

export { RubyGemsSearchResults } from './RubyGemsSearchResults';
export type { RubyGemsSearchResultsProps } from './RubyGemsSearchResults';

export { RubyGemsResultRow } from './RubyGemsResultRow';
export type { RubyGemsResultRowProps } from './RubyGemsResultRow';

// Hook
export { useRubyGemsSearch } from './useRubyGemsSearch';
export type { UseRubyGemsSearchReturn } from './useRubyGemsSearch';

// Mock data (for development only)
export { mockRubyGemsResults, mockRubyGemsSearchApi, mockRubyGemsDetailApi } from './mockData';


