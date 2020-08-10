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

import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.TransactionalSession;

/**
 * Represents a session with a {@link DataStore}.
 *
 * @since 3.19
 */
public interface DataSession<T extends Transaction>
    extends TransactionalSession<T>
{
  /**
   * {@link DataAccess} mapping for the given type.
   */
  <D extends DataAccess> D access(Class<D> type);

  /**
   * Registers a hook to run before any changes are committed in this session.
   *
   * @since 3.26
   */
  void preCommit(Runnable hook);

  /**
   * Registers a hook to run after changes have been committed in this session.
   *
   * @since 3.26
   */
  void postCommit(Runnable hook);

  /**
   * Registers a hook to run after changes have been rolled back in this session.
   *
   * @since 3.26
   */
  void onRollback(Runnable hook);

  /**
   * Returns the SQL dialect of the database backing this session.
   *
   * @since 3.26
   */
  String sqlDialect();
}
