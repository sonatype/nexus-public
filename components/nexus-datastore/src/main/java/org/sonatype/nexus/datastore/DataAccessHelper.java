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
package org.sonatype.nexus.datastore;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.transaction.UnitOfWork;

/**
 * Static utility helpers for {@link DataAccess}.
 *
 * @since 3.21
 */
public class DataAccessHelper
{
  /**
   * {@link DataAccess} mapping for the given type; requires an open session.
   */
  public static <D extends DataAccess> D access(final Class<D> type) {
    return UnitOfWork.<DataSession<?>> currentSession().access(type);
  }

  /**
   * Was this exception caused by trying to insert a duplicate key?
   */
  public static boolean causedByDuplicateKey(final Exception e) {
    return "23505".equals(findSQLState(e));
  }

  /**
   * Tries to find the closest {@link SQLException#getSQLState()}; {@code null} if there was no {@link SQLException}.
   */
  @Nullable
  private static String findSQLState(final Exception cause) {
    Set<Throwable> visited = new HashSet<>(); // guard against cycles
    for (Throwable e = cause; e != null && visited.add(e); e = e.getCause()) {
      if (e instanceof SQLException) {
        return ((SQLException) e).getSQLState();
      }
    }
    return null;
  }

  private DataAccessHelper() {
    // static utility helper
  }
}
