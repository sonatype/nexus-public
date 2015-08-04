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
package org.sonatype.nexus.proxy.walker;

import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;

/**
 * A logical AND between two or more walker filters.
 *
 * @author Alin Dreghiciu
 */
public class ConjunctionWalkerFilter
    implements WalkerFilter
{

  /**
   * AND-ed filters (can be null or empty).
   */
  private final WalkerFilter[] m_filters;

  /**
   * Constructor.
   *
   * @param filters AND-ed filters (can be null or empty)
   */
  public ConjunctionWalkerFilter(final WalkerFilter... filters) {
    m_filters = filters;
  }

  /**
   * Performs a logical AND between results of calling {@link #shouldProcess(org.sonatype.nexus.proxy.walker.WalkerContext,
   * org.sonatype.nexus.proxy.item.StorageItem)} on all
   * filters. It will exit at first filter that returns false. <br/>
   * If no filters were provided returns true.
   *
   * {@inheritDoc}
   */
  public boolean shouldProcess(final WalkerContext context,
                               final StorageItem item)
  {
    if (m_filters == null || m_filters.length == 0) {
      return true;
    }
    for (WalkerFilter filter : m_filters) {
      if (!filter.shouldProcess(context, item)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Performs a logical AND between results of calling
   * {@link #shouldProcessRecursively(org.sonatype.nexus.proxy.walker.WalkerContext,
   * org.sonatype.nexus.proxy.item.StorageCollectionItem)}  on all filters. It will exit at first
   * filter that returns false.<br/>
   * If no filters were provided returns true.
   *
   * {@inheritDoc}
   */
  public boolean shouldProcessRecursively(final WalkerContext context,
                                          final StorageCollectionItem coll)
  {
    if (m_filters == null || m_filters.length == 0) {
      return true;
    }
    for (WalkerFilter filter : m_filters) {
      if (!filter.shouldProcessRecursively(context, coll)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Builder method.
   *
   * @param filters AND-ed filters (can be null or empty)
   * @return conjunction between filters
   */
  public static ConjunctionWalkerFilter satisfiesAllOf(final WalkerFilter... filters) {
    return new ConjunctionWalkerFilter(filters);
  }

}