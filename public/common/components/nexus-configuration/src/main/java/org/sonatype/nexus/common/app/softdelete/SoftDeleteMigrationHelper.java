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

/**
 * Shared helper for detecting missing {@code deleted_on} column errors during rolling upgrades.
 *
 * During zero-downtime deployments, new application code may query the {@code deleted_on} column
 * before the database migration that adds it has completed. This helper detects the resulting
 * database error so callers can fall back to an unfiltered query.
 */
public final class SoftDeleteMigrationHelper
{
  private SoftDeleteMigrationHelper() {
  }

  /**
   * Returns {@code true} if the exception (or any cause in its chain) indicates that the
   * {@code deleted_on} column does not exist in the database schema.
   */
  public static boolean isMissingDeletedOnColumn(final Exception e) {
    Throwable cause = e;
    while (cause != null) {
      String msg = cause.getMessage();
      if (msg != null) {
        // PostgreSQL: column "deleted_on" does not exist
        if (msg.contains("deleted_on") && msg.contains("does not exist")) {
          return true;
        }
        // H2: Column "DELETED_ON" not found
        String upper = msg.toUpperCase();
        if (upper.contains("DELETED_ON") && upper.contains("NOT FOUND")) {
          return true;
        }
      }
      cause = cause.getCause();
    }
    return false;
  }
}
