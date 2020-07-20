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
package org.sonatype.nexus.repository.query;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Data carrier for paged results, containing one page of a particular query.
 *
 * @since 3.1
 */
public class PageResult<T>
{
  private long total;

  private List<T> results;

  /**
   * @param total   The total result count.
   * @param results The results returned.
   */
  public PageResult(final long total, final List<T> results) {
    this.total = total;
    this.results = checkNotNull(results);
  }

  public PageResult(final QueryOptions queryOptions, final List<T> results) {
    this(estimateCount(queryOptions, results), results);
  }

  /**
   * Returns the total count of entries available, not just those returned by this particular query.
   */
  public long getTotal() {
    return total;
  }

  /**
   * Returns the results from this particular query, which may be a subset or page of all possible results.
   */
  public List<T> getResults() {
    return results;
  }

  private static long estimateCount(final QueryOptions queryOptions, final List<?> items) {
    long count = items.size();
    if (queryOptions.getStart() != null && queryOptions.getLimit() != null) {
      count += queryOptions.getStart();
      // estimate an additional page if the number of items returned was limited
      if (items.size() == queryOptions.getLimit()) {
        count += queryOptions.getLimit();
      }
    }
    return count;
  }

}
