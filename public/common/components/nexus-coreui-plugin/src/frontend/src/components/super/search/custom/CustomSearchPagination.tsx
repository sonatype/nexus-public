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

import React from 'react';

import './CustomSearchPagination.scss';

export interface CustomSearchPaginationProps {
  /** Whether more results are available */
  hasMore: boolean;
  /** Whether currently loading */
  loading: boolean;
  /** Number of results currently loaded */
  loadedCount: number;
  /** Total count of results */
  totalCount: number;
  /** Callback to load more results */
  onLoadMore: () => void;
}

/**
 * Pagination component for custom search results.
 * Shows a "Load More" button when more results are available.
 */
export function CustomSearchPagination({
  hasMore,
  loading,
  loadedCount,
  totalCount,
  onLoadMore,
}: CustomSearchPaginationProps): JSX.Element | null {
  // Don't render if no results or no more to load
  if (totalCount === 0 || !hasMore) {
    return null;
  }

  return (
    <div className="custom-search-pagination">
      <div className="custom-search-pagination__info">
        Showing {loadedCount} of {totalCount} results
      </div>
      <button
        type="button"
        className="custom-search-pagination__load-more"
        onClick={onLoadMore}
        disabled={loading}
      >
        {loading ? 'Loading...' : 'Load More'}
      </button>
    </div>
  );
}

export default CustomSearchPagination;


