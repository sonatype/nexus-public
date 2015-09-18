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
package org.sonatype.nexus.orient;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

/**
 * Database manager.
 *
 * @since 3.0
 */
public interface DatabaseManager
{
  /**
   * Open a non-pooled connection to the named database.
   *
   * @param name    The name of the database to open.
   * @param create  {@code true} to create the database if it does not exist.
   */
  ODatabaseDocumentTx connect(String name, boolean create);

  /**
   * Access externalizer for a named database.
   */
  DatabaseExternalizer externalizer(String name);

  /**
   * Access named shared database pool.
   *
   * If the pool does not already exist it will be created.
   */
  DatabasePool pool(String name);

  /**
   * Create a new (non-shared) pool for the given named configuration.
   */
  DatabasePool newPool(String name);

  /**
   * Access named database instance.
   *
   * If the instance does not already exist it will be created.
   */
  DatabaseInstance instance(String name);
}