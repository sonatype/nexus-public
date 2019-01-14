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
package org.sonatype.nexus.blobstore.api;

import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.goodies.lifecycle.Lifecycle;

/**
 * {@link BlobStore} manager.
 *
 * @since 3.0
 */
public interface BlobStoreManager
  extends Lifecycle
{
  /**
   * Default blob store name.
   */
  String DEFAULT_BLOBSTORE_NAME = "default";

  /**
   * @return all BlobStores
   */
  Iterable<BlobStore> browse();

  /**
   * Create a new BlobStore
   */
  BlobStore create(BlobStoreConfiguration blobStoreConfiguration) throws Exception;

  /**
   * Update an existing BlobStore
   *
   * @since 3.14
   */
  BlobStore update(BlobStoreConfiguration blobStoreConfiguration) throws Exception;

  /**
   * Lookup a BlobStore by name
   */
  @Nullable
  BlobStore get(String name);

  /**
   * Delete a BlobStore by name
   */
  void delete(String name) throws Exception;

  /**
   * Delete a BlobStore by name, even if it is use.
   *
   * @since 3.14
   */
  void forceDelete(String name) throws Exception;

  /**
   * Returns true if a blob store with the provided name already exists. Check is case-insensitive.
   *
   * @since 3.1
   */
  boolean exists(String name);

  /**
   * Returns the number of other blob stores that use the named blob store.
   *
   * @since 3.14
   */
  long blobStoreUsageCount(String blobStoreName);

  /**
   * Returns true if the blob store is promotable
   * @param blobStoreName
   * @return true if member is promotable
   *
   * @since 3.15
   */
  boolean isPromotable(String blobStoreName);

  /**
   * Returns the parent group of the blob store if it exists
   * @param blobStoreName
   * @return {@link java.util.Optional<String>} containing the parent group name if it exists
   *
   * @since 3.15
   */
  Optional<String> getParent(String blobStoreName);
}
