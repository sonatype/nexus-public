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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class UpgradeManagerTest
    extends TestSupport
{
  @Rule
  public DataSessionRule dataSessionRule = new DataSessionRule();

  @Mock
  DataStoreManager dataStoreManager;

  TestMigrationStep migrationStep = new TestMigrationStep();

  @Before
  public void setUp() throws Exception {
    when(dataStoreManager.get(DEFAULT_DATASTORE_NAME)).thenReturn(getDataStore());
  }

  @Test
  public void testNoUpgrades() throws Exception {
    UpgradeManager upgradeManager = new UpgradeManager(dataStoreManager, emptyList());

    upgradeManager.migrate();

    try (Connection conn = getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        // check the flyway schema history
        try (ResultSet results = stmt.executeQuery("SELECT * FROM \"flyway_schema_history\"")) {
          if (migrationStep.isH2(conn)) {
            // for H2 flyway inserts an initial null version
            assertThat(results.next(), equalTo(true));
            assertThat(results.getString("version"), nullValue());
          }
          // assert there is no history of schema upgrades
          assertThat(results.next(), equalTo(false));
        }
      }
    }
  }

  @Test
  public void testExampleUpgrade() throws Exception {
    UpgradeManager upgradeManager = new UpgradeManager(dataStoreManager, singletonList(migrationStep));

    upgradeManager.migrate();

    try (Connection conn = getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        // check the flyway schema history
        try (ResultSet results = stmt.executeQuery("SELECT * FROM \"flyway_schema_history\"")) {
          if (migrationStep.isH2(conn)) {
            // for H2 flyway inserts an initial null version
            assertThat(results.next(), equalTo(true));
            assertThat(results.getString("version"), nullValue());
          }
          // assert there is history of one schema upgrade
          assertThat(results.next(), equalTo(true));
          assertThat(results.getString("version"), equalTo("1.0"));
          assertThat(results.next(), equalTo(false));
        }

        // check for the result of the upgrade step
        try (ResultSet results = stmt.executeQuery("SELECT * FROM example")) {
          assertThat(results.next(), equalTo(true));
          assertThat(results.getString("name"), equalTo("fawkes"));
          assertThat(results.next(), equalTo(false));
        }
      }
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testWithDatastoreFromFuture() {
    FutureMigrationStep futureMigrationStep = new FutureMigrationStep();

    UpgradeManager upgradeManagerWithFutureMigration = new UpgradeManager(dataStoreManager,
        Arrays.asList(migrationStep, futureMigrationStep));
    upgradeManagerWithFutureMigration.migrate();

    UpgradeManager upgradeManagerWithoutFuture = new UpgradeManager(dataStoreManager, singletonList(migrationStep));
    upgradeManagerWithoutFuture.migrate();
  }

  @Test
  public void testUpgradeSkippedStep() throws SQLException {
    FutureMigrationStep futureMigrationStep = new FutureMigrationStep();
    UpgradeManager upgradeManager = new UpgradeManager(dataStoreManager, Arrays.asList(migrationStep, futureMigrationStep));
    upgradeManager.migrate();

    SkippedMigrationStep skippedMigrationStep = new SkippedMigrationStep();
    UpgradeManager upgradeManagerWithSkipped = new UpgradeManager(dataStoreManager, Arrays.asList(migrationStep, futureMigrationStep, skippedMigrationStep));
    upgradeManagerWithSkipped.migrate();

    try (Connection conn = getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        // check for the result of the upgrade step
        try (ResultSet results = stmt.executeQuery("SELECT * FROM skipped")) {
          assertThat(results.next(), equalTo(true));
          assertThat(results.getString("name"), equalTo("fawkes"));
          assertThat(results.next(), equalTo(false));
        }
      }
    }
  }

  private Optional<DataStore<?>> getDataStore() {
    return dataSessionRule.getDataStore(DEFAULT_DATASTORE_NAME);
  }

  private Connection getConnection() throws SQLException {
    return getDataStore()
        .orElseThrow(() -> new IllegalStateException("No DataStore found"))
        .getDataSource()
        .getConnection();
  }
}
