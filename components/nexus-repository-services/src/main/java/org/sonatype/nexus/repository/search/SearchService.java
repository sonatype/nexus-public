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
package org.sonatype.nexus.repository.search;

/**
 * This defines the db-agnostic interface for searching. All search requests should go through this.
 *
 * @since 3.38
 */
public interface SearchService
{
  /**
   * Search for components with the passed in {@link SearchRequest}
   *
   * @since 3.38
   */
  SearchResponse search(SearchRequest searchRequest);

  /**
   * Browse through all of the components returned
   *
   * @since 3.38
   */
  Iterable<ComponentSearchResult> browse(SearchRequest searchRequest);

  /**
   * Return the count of components matching the {@link SearchRequest}
   *
   * @since 3.38
   */
  long count(SearchRequest searchRequest);

  default void waitForCalm() {
    // not required for default
  }

  default void waitForReady() {
    // not required for default
  }
}
