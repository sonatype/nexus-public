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
package org.sonatype.nexus.repository.storage;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;

import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexCursor;

/**
 * Store providing access to components.
 *
 * @since 3.6
 */
public interface ComponentStore
{
  /**
   * @return the component for the id
   */
  Component read(EntityId id);

  /**
   * Finds and returns all the components that match the specified parameters.
   *
   * @return All the components that match the specified parameters
   * @since 3.14
   */
  List<Component> getAllMatchingComponents(final Repository repository,
                                           final String group,
                                           final String name,
                                           final Map<String, String> formatAttributes);

  /**
   * @return the number of components contained by the buckets
   */
  long countComponents(@Nullable final Iterable<Bucket> buckets);

  /**
   * Get an index to iterate over the assets in that index
   */
  OIndex<?> getIndex(final String indexName);

  /**
   * @param cursor to get the asset ids from
   * @param limit  the maximum number of records to return
   * @return a page of assets in a bucket (up to limit number of assets or the end of the records available)
   */
  <T> List<Entry<T, EntityId>> getNextPage(final OIndexCursor cursor, final int limit);
}
