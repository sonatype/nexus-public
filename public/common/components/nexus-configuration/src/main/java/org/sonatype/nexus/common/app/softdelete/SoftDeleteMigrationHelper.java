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
package org.sonatype.nexus.common.app.softdelete;

import java.sql.SQLException;
import java.util.Locale;

/**
 * Shared helper for detecting missing {@code deleted_on} column errors during rolling upgrades.
 *
 * During zero-downtime deployments, new application code may query the {@code deleted_on} column
 * before the database migration that adds it has completed. This helper detects the resulting
 * database error so callers can fall back to an unfiltered query.
 */
public final class SoftDeleteMigrationHelper
{
  /**
   * PostgreSQL SQLState for {@code undefined_column}. Not localized by {@code lc_messages}, unlike the
   * message text.
   */
  private static final String POSTGRES_UNDEFINED_COLUMN = "42703";

  /**
   * H2 SQLState for a column that cannot be resolved ({@code COLUMN_NOT_FOUND_1}).
   */
  private static final String H2_COLUMN_NOT_FOUND = "42S22";

  private SoftDeleteMigrationHelper() {
  }

  /**
   * Returns {@code true} if the exception (or any cause in its chain) indicates that the
   * {@code deleted_on} column does not exist in the database schema.
   *
   * <p>
   * The primary check is on the JDBC {@link SQLException#getSQLState() SQLState} ({@code 42703} on
   * PostgreSQL, {@code 42S22} on H2) combined with a {@code deleted_on} mention. SQLState and the column
   * name are not translated by the database locale, so this works regardless of the server's
   * {@code lc_messages} -- unlike matching the English message text, which failed on localized
   * PostgreSQL servers (NEXUS-55897). The legacy English/H2 message-text checks are kept as a backstop
   * for drivers or wrappers that drop the SQLState from the cause chain.
   */
  public static boolean isMissingDeletedOnColumn(final Exception e) {
    Throwable cause = e;
    while (cause != null) {
      String msg = cause.getMessage();

      // Primary, locale-independent check: a missing-column SQLState naming deleted_on.
      if (cause instanceof SQLException) {
        String sqlState = ((SQLException) cause).getSQLState();
        if ((POSTGRES_UNDEFINED_COLUMN.equals(sqlState) || H2_COLUMN_NOT_FOUND.equals(sqlState))
            && msg != null && msg.toLowerCase(Locale.ROOT).contains("deleted_on")) {
          return true;
        }
      }

      // Backstop: legacy English (PostgreSQL) / H2 message-text matching.
      if (msg != null) {
        // PostgreSQL: column "deleted_on" does not exist
        if (msg.contains("deleted_on") && msg.contains("does not exist")) {
          return true;
        }
        // H2: Column "DELETED_ON" not found
        String upper = msg.toUpperCase(Locale.ROOT);
        if (upper.contains("DELETED_ON") && upper.contains("NOT FOUND")) {
          return true;
        }
      }
      cause = cause.getCause();
    }
    return false;
  }
}
