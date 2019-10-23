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

/**
 * {@link DataSession} supplier; for use by clients who don't need the full store API.
 *
 * @since 3.19
 */
public interface DataSessionSupplier
{
  /**
   * Opens a new {@link DataSession} against the named data store.
   *
   * @throws DataStoreNotFoundException if the store does not exist
   */
  DataSession<?> openSession(String storeName);

  /**
   * Opens a new JDBC {@link Connection} to the named data store.
   *
   * @throws DataStoreNotFoundException if the store does not exist
   * @throws UnsupportedOperationException if the store doesn't support JDBC
   */
  Connection openConnection(String storeName) throws SQLException;
}
