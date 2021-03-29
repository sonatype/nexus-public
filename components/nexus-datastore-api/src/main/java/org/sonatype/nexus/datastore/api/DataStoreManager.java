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
package org.sonatype.nexus.datastore.api;

import java.util.Optional;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.Freezable;

/**
 * {@link DataStore} manager.
 *
 * @since 3.19
 */
public interface DataStoreManager
    extends Lifecycle, Freezable
{
  String DEFAULT_DATASTORE_NAME = "nexus";

  /**
   * Browse existing data stores.
   */
  Iterable<DataStore<?>> browse();

  /**
   * Create a new data store.
   */
  DataStore<?> create(DataStoreConfiguration configuration) throws Exception;

  /**
   * Update an existing data store.
   */
  DataStore<?> update(DataStoreConfiguration configuration) throws Exception;

  /**
   * Lookup a data store by name.
   */
  Optional<DataStore<?>> get(String storeName);

  /**
   * Delete a data store by name.
   */
  boolean delete(String storeName) throws Exception;

  /**
   * @return {@code true} if the named data store already exists. Check is case-insensitive.
   */
  boolean exists(String storeName);
}
