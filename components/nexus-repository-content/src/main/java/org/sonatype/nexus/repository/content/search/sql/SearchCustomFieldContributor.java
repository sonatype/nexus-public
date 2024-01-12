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
package org.sonatype.nexus.repository.content.search.sql;

import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.search.sql.SearchRecord;

/**
 * Allows contribution of data from assets to {@link SearchRecord}
 *
 * May be used by formats to contribute custom fields to search indexes.
 */
public interface SearchCustomFieldContributor
{
  /**
   * From the given {@link Asset} the passed in {@link SearchRecord} will have its custom search fields populated.
   */
  void populateSearchCustomFields(SearchRecord searchRecord, Asset asset);

  /**
   * Should be an asset path searchable or not. By default, the path will be indexed.
   *
   * @param path the asset path.
   * @return {@code true} the path will be indexed, {@code false} otherwise.
   */
  default boolean isEnableSearchByPath(final String path) {
    return true;
  }
}
