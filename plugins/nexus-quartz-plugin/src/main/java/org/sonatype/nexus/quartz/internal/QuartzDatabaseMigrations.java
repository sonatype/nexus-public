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
package org.sonatype.nexus.quartz.internal;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

/**
 * Migration steps of Quartz database.
 *
 * @since 3.0
 */
public class QuartzDatabaseMigrations
{
  /**
   * Creates initial database.
   */
  private static final QuartzDatabaseMigration INITIAL_DB_CREATION = new QuartzDatabaseMigration(1)
  {
    @Override
    public void migrate(final Connection connection) throws SQLException {
      try {
        final String sqlScript = Resources.toString(Resources.getResource("tables_h2.sql"), Charsets.UTF_8);
        connection.createStatement().execute(sqlScript);
        connection.commit();
      }
      catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
  };

  static List<QuartzDatabaseMigration> MIGRATIONS = ImmutableList.of(INITIAL_DB_CREATION);
}