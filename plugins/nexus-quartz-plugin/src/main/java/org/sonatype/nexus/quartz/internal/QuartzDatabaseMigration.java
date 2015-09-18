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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Migration step of Quartz database.
 *
 * @since 3.0
 */
public abstract class QuartzDatabaseMigration
{
  private final int version;

  public QuartzDatabaseMigration(final int version) {
    this.version = version;
  }

  /**
   * Perform a step of migration (DML or DDL), but the connection should NOT be closed.
   */
  public abstract void migrate(Connection connection) throws SQLException;

  public int getVersion() {
    return version;
  }
}