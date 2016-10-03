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
package org.sonatype.nexus.repository.browse;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Data carrier for {@link BrowseService} results, containing the results of a particular query.
 *
 * @since 3.1
 */
public class BrowseResult<T>
{
  private long total;

  private List<T> results;

  /**
   * @param total   The total result count.
   * @param results The results returned.
   */
  public BrowseResult(final long total, final List<T> results) {
    this.total = total;
    this.results = checkNotNull(results);
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
}
