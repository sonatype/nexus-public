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
 * Browse Module - Public API
 *
 * This is the main entry point for the Browse module.
 * Import types, API functions, and shared components from here.
 *
 * @example
 * import { BrowseNode, fetchBrowseNodes, NodeIcon } from '../browse';
 */

// =============================================================================
// TYPES
// =============================================================================
export * from './browse.types';

// =============================================================================
// API
// =============================================================================
export * from './browse.api';

// =============================================================================
// SHARED COMPONENTS
// =============================================================================
export { NodeIcon } from './shared/NodeIcon';
export { FormatBadge } from './shared/FormatBadge';
export { StatusIndicator } from './shared/StatusIndicator';

// =============================================================================
// IN-REPOSITORY SEARCH
// =============================================================================
export { InRepositorySearch } from './InRepositorySearch';
export { SearchResultItem } from './SearchResultItem';

// =============================================================================
// MAIN COMPONENTS (to be added by agents)
// =============================================================================
// export { RepositoryList } from './repository-list/RepositoryList';
// export { BrowseTree } from './tree/BrowseTree';
// export { DetailPanel } from './detail/DetailPanel';
// export { DeleteDialog, CopyUrlButton } from './actions';
// export { default as BrowsePage } from './BrowsePage';
