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
package org.sonatype.nexus.orient.migrator;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.LogManager;

import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.conflict.OVersionRecordConflictStrategy;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Migrates the contents of a local Orient component database to new-DB.
 *
 * @since 3.20
 */
public class Main
{
  public static void main(final String[] args) throws SQLException, IOException {
    if (args.length < 2) {
      System.err.println("Usage: java -jar nexus-orient-migrator.jar <plocal:component-db-path> <jdbc:target-db>"); // NOSONAR
      System.exit(1);
    }

    LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));

    // register placeholder so we can load databases that use our custom conflict hook
    Orient.instance().getRecordConflictStrategy().registerImplementation("ConflictHook",
        new OVersionRecordConflictStrategy());

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(args[1]);

    if (hikariConfig.getJdbcUrl().contains("postgresql")) {
      // workaround https://github.com/pgjdbc/pgjdbc/issues/265
      hikariConfig.getDataSourceProperties().setProperty("stringtype", "unspecified");
    }

    try (ODatabaseDocumentTx componentDb = new ODatabaseDocumentTx(args[0])) {
      componentDb.open("admin", "admin");
      try (HikariDataSource targetDb = new HikariDataSource(hikariConfig)) {
        ContentMigrator migrator = new ContentMigrator(componentDb, targetDb.getConnection(), false);

       migrator.extractAll();
      }
    }
  }

  private Main() {
    // static entry-class
  }
}
