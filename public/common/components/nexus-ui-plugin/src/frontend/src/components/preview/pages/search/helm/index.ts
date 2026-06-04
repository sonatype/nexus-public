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
 * Helm Search Module
 *
 * Public exports for Helm (Kubernetes) chart search components.
 */

// Types
export type {
  HelmSearchFilters,
  HelmResult,
  HelmSearchResponse,
  HelmVersion,
  HelmDetail,
  HelmSearchState,
  HelmMaintainer,
} from './helm.types';

// Main page components
export { HelmSearchPage } from './HelmSearchPage';
export type { HelmSearchPageProps } from './HelmSearchPage';

export { HelmDetailPage } from './HelmDetailPage';
export type { HelmDetailPageProps } from './HelmDetailPage';

// Sub-components
export { HelmSearchFilters } from './HelmSearchFilters';
export type { HelmSearchFiltersProps } from './HelmSearchFilters';

export { HelmSearchResults } from './HelmSearchResults';
export type { HelmSearchResultsProps } from './HelmSearchResults';

export { HelmResultRow } from './HelmResultRow';
export type { HelmResultRowProps } from './HelmResultRow';

// Hook
export { useHelmSearch } from './useHelmSearch';
export type { UseHelmSearchReturn } from './useHelmSearch';

// Mock data (for development only)
export { mockHelmResults, mockHelmSearchApi, mockHelmDetailApi } from './mockData';


