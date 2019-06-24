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

import javax.annotation.Nullable;

import org.sonatype.goodies.lifecycle.Lifecycle;

/**
 * {@link DataStore} manager.
 *
 * @since 3.next
 */
public interface DataStoreManager
    extends Lifecycle
{
  String CONFIG_DATASTORE_NAME = "config";

  String COMPONENT_DATASTORE_NAME = "component";

  /**
   * @return all data stores
   */
  Iterable<DataStore<?>> browse();

  /**
   * Create a new data store.
   */
  DataStore<?> create(DataStoreConfiguration dataStoreConfiguration) throws Exception;

  /**
   * Update an existing data store.
   */
  DataStore<?> update(DataStoreConfiguration dataStoreConfiguration) throws Exception;

  /**
   * Lookup a data store by name.
   */
  @Nullable
  DataStore<?> get(String name);

  /**
   * Delete a data store by name.
   */
  void delete(String name) throws Exception;

  /**
   * Returns true if a data store with the provided name already exists. Check is case-insensitive.
   */
  boolean exists(String name);
}
