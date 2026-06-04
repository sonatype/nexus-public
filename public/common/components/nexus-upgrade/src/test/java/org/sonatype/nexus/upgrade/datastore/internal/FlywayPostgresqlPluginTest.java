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
package org.sonatype.nexus.upgrade.datastore.internal;

import javax.sql.DataSource;

import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sonatype.nexus.testdb.DatabaseExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Verifies that {@code flyway-database-postgresql} ServiceLoader plugin is on the classpath
 * and activates correctly when Flyway connects to a PostgreSQL datasource.
 *
 * Flyway 10+ split PostgreSQL support into a separate artifact. Without it on the classpath,
 * {@code flyway.info()} throws "No database found to handle jdbc:postgresql://..." at runtime.
 */
@ExtendWith(DatabaseExtension.class)
class FlywayPostgresqlPluginTest
{
  @DataSessionConfiguration(daos = {}, postgresql = true)
  TestDataSessionSupplier supplier;

  @DatabaseTest(postgresql = true, h2 = false)
  void postgresqlPluginLoads() {
    DataSource dataSource = supplier.getDataSource()
        .orElseThrow(() -> new IllegalStateException("No DataSource available"));

    Flyway flyway = Flyway.configure()
        .dataSource(dataSource)
        .locations(new String[0]) // no script scanning — same as UpgradeManagerImpl
        .cleanDisabled(true)
        .load();

    assertDoesNotThrow(flyway::info,
        "flyway-database-postgresql plugin must be on the classpath and load via ServiceLoader " +
            "— if this fails with 'No database found to handle jdbc:postgresql://' the runtime artifact is missing");
  }
}
