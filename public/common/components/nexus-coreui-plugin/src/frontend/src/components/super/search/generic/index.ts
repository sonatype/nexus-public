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
 * Generic Search Module - Phase 4C
 * 
 * Search components for all repository formats.
 * Agent 1: Generic Search implementation complete.
 */

// Page components
export { GenericSearchPage } from './GenericSearchPage';
export { GenericDetailPage } from './GenericDetailPage';

// Sub-components
export { GenericSearchFilters } from './GenericSearchFilters';
export { GenericSearchResults } from './GenericSearchResults';
export { GenericResultRow } from './GenericResultRow';

// Hook
export { useGenericSearch } from './useGenericSearch';

// Types
export type {
  GenericResult,
  GenericAsset,
  GenericSearchFilters as GenericSearchFiltersType,
  GenericSearchResponse,
  GenericSearchState,
} from './generic.types';

export { FORMAT_CONFIG } from './generic.types';
