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

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.transaction.TransactionalStore;

/**
 * Each {@link DataStore} contains a number of {@link DataAccess} mappings accessible via {@link DataSession}s.
 *
 * @since 3.19
 */
public interface DataStore<S extends DataSession<?>>
    extends TransactionalStore<S>, Lifecycle
{
  /**
   * Configure the data store; changes won't take effect until the store is (re)started.
   */
  void setConfiguration(DataStoreConfiguration configuration);

  /**
   * @return the data store's configuration.
   */
  DataStoreConfiguration getConfiguration();

  /**
   * @return {@code true} if the data store has been started.
   */
  boolean isStarted();

  /**
   * Registers the given {@link DataAccess} type with this store.
   */
  void register(Class<? extends DataAccess> accessType);

  /**
   * Unregisters the given {@link DataAccess} type with this store.
   */
  void unregister(Class<? extends DataAccess> accessType);

  /**
   * Opens a new JDBC {@link Connection} to this store.
   *
   * @throws UnsupportedOperationException if this store doesn't support JDBC
   */
  Connection openConnection() throws SQLException;

  /**
   * @since 3.29
   */
  DataSource getDataSource();

  /**
   * Permanently stops this data store.
   */
  void shutdown() throws Exception;

  // Note: we don't implement Freezable because data stores are prototype instances, not singleton components

  /**
   * Freezes the data store, disallowing writes.
   *
   * @since 3.21
   */
  void freeze();

  /**
   * Unfreezes the data store, allowing writes.
   *
   * @since 3.21
   */
  void unfreeze();

  /**
   * Is this data store currently frozen?
   *
   * @since 3.21
   */
  boolean isFrozen();

  /**
   * Backup this data store to the specified location.
   *
   * @throws UnsupportedOperationException if the underlying data store does not support backing up
   *
   * @since 3.21
   */
  void backup(String location) throws Exception;
}
