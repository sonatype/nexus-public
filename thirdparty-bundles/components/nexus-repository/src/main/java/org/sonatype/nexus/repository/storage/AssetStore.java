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
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.EntityId;

import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexCursor;

/**
 * Store providing access to assets.
 *
 * @since 3.6
 */
public interface AssetStore
{
  /**
   * Get the asset matching the id or null
   */
  Asset getById(EntityId id);

  /**
   * Get the assets matching the ids or an empty iterable
   */
  Iterable<Asset> getByIds(Iterable<EntityId> id);

  /**
   * Gets the number of assets matching the given {@link Query} clause.
   */
  long countAssets(@Nullable Iterable<Bucket> buckets);

  /**
   * Get an index to iterate over the assets in that index
   */
  OIndex<?> getIndex(String indexName);

  /**
   * @param cursor to get the asset ids from
   * @param limit the maximum number of records to return
   * @return a page of assets in a bucket (up to limit number of assets or the end of the records available)
   */
  <T> List<Entry<T, EntityId>> getNextPage(OIndexCursor cursor, int limit);

  /**
   * Save changes made to an asset
   */
  Asset save(Asset asset);

  /**
   * Save changes to multiple assets
   */
  void save(Iterable<Asset> assets);
}
