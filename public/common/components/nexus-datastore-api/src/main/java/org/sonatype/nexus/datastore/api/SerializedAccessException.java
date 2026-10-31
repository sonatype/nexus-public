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

/**
 * Thrown when a transaction is aborted by the database because of a transient concurrency conflict that the
 * caller can safely retry: a serialization failure (SQLState {@value #SQL_STATE}) or a detected deadlock
 * (SQLState {@value #DEADLOCK_SQL_STATE}).
 */
public class SerializedAccessException
    extends DataAccessException
{
  private static final long serialVersionUID = 98739582308995723L;

  /**
   * PostgreSQL {@code serialization_failure}.
   */
  public static final String SQL_STATE = "40001";

  /**
   * PostgreSQL {@code deadlock_detected}. A deadlock aborts one of the participating transactions; retrying the
   * aborted transaction after the survivor releases its locks is the standard remedy.
   */
  public static final String DEADLOCK_SQL_STATE = "40P01";

  public SerializedAccessException(final Throwable cause) {
    super("Serialization failure or deadlock detected", cause);
  }
}
