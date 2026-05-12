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

import './GASearchPagination.scss';

export interface GASearchPaginationProps {
  /** Whether more results are available */
  hasMore: boolean;
  /** Whether loading is in progress */
  loading: boolean;
  /** Current count of loaded results */
  loadedCount: number;
  /** Total count of available results */
  totalCount: number;
  /** Callback to load more results */
  onLoadMore: () => void;
}

/**
 * Pagination component for GA search results.
 * Uses "Load More" pattern instead of page numbers.
 */
export function GASearchPagination({
  hasMore,
  loading,
  loadedCount,
  totalCount,
  onLoadMore,
}: GASearchPaginationProps): JSX.Element | null {
  if (!hasMore && loadedCount >= totalCount) {
    return null;
  }

  return (
    <div className="ga-search-pagination">
      <div className="ga-search-pagination__info">
        Showing {loadedCount} of {totalCount} results
      </div>

      {hasMore && (
        <button
          type="button"
          className="ga-search-pagination__button"
          onClick={onLoadMore}
          disabled={loading}
        >
          {loading ? 'Loading...' : 'Load More'}
        </button>
      )}
    </div>
  );
}

export default GASearchPagination;

