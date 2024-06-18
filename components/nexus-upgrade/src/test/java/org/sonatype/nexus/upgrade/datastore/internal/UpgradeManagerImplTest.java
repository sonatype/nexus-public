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

import org.flywaydb.core.api.MigrationVersion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.sonatype.nexus.upgrade.datastore.UpgradeException;
import org.sonatype.nexus.upgrade.datastore.events.UpgradeCompletedEvent;
import org.sonatype.nexus.upgrade.datastore.events.UpgradeStartedEvent;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class UpgradeManagerImplTest
    extends TestSupport
{
  private static final String SELECT_FROM_FLYWAY_SCHEMA_HISTORY = "SELECT * FROM \"flyway_schema_history\"";

  private static final String SELECT_FROM_EXAMPLE = "SELECT * FROM example";

  private static final String SELECT_FROM_SKIPPED = "SELECT * FROM skipped";

  @Rule
  public DataSessionRule dataSessionRule = new DataSessionRule();

  @Mock
  private DataStoreManager dataStoreManager;

  @Mock
  private PostStartupUpgradeAuditor auditor;

  TestMigrationStep migrationStep = new TestMigrationStep();

  @Before
  public void setUp() {
    when(dataStoreManager.get(DEFAULT_DATASTORE_NAME)).thenReturn(getDataStore());
  }

  @Test
  public void testNoUpgrades() throws Exception {
    UpgradeManagerImpl upgradeManager = new UpgradeManagerImpl(dataStoreManager, auditor, emptyList());
    upgradeManager.migrate();

    try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
      Exception exception = assertThrows(Exception.class,
          () -> stmt.executeQuery(SELECT_FROM_FLYWAY_SCHEMA_HISTORY));

      if ("PostgreSQL".equals(conn.getMetaData().getDatabaseProductName())) {
        assertTrue(exception.getMessage().contains("relation \"flyway_schema_history\" does not exist"));
      }
      else {
        assertTrue(exception.getMessage().contains("Table \"flyway_schema_history\" not found; SQL statement:"));
      }
    }
    // No changes should fire no events
    verifyNoInteractions(auditor);
  }

  @Test
  public void testExampleUpgrade() throws Exception {
    UpgradeManagerImpl upgradeManager = new UpgradeManagerImpl(dataStoreManager, auditor, singletonList(migrationStep));

    upgradeManager.migrate();

    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement()) {
      try (ResultSet results = stmt.executeQuery(SELECT_FROM_FLYWAY_SCHEMA_HISTORY)) {
        if (migrationStep.isH2(conn)) {
          // for H2 flyway inserts an initial null version
          assertTrue(results.next());
          assertNull(results.getString("version"));
        }
        // assert there is history of one schema upgrade
        assertForExampleTable(results, "version", "1.0");
      }
      catch (Exception exception) {
        fail(exception.getMessage());
      }

      // check for the result of the upgrade step
      try (ResultSet results = stmt.executeQuery(SELECT_FROM_EXAMPLE)) {
        assertForExampleTable(results, "name", "fawkes");
      }
      catch (Exception exception) {
        fail(exception.getMessage());
      }
    }

    // Migrations should trigger events
    verify(auditor).post(any(UpgradeStartedEvent.class));
    verify(auditor).post(any(UpgradeCompletedEvent.class));
    verifyNoMoreInteractions(auditor);
  }

  private static void assertForExampleTable(ResultSet results, String name, String fawkes) throws SQLException {
    assertTrue(results.next());
    assertThat(results.getString(name), equalTo(fawkes));
    assertFalse(results.next());
  }

  @Test(expected = UpgradeException.class)
  public void testWithDatastoreFromFuture() throws UpgradeException {
    FutureMigrationStep futureMigrationStep = new FutureMigrationStep();

    UpgradeManagerImpl upgradeManagerWithFutureMigration = new UpgradeManagerImpl(dataStoreManager, auditor,
        Arrays.asList(migrationStep, futureMigrationStep));
    upgradeManagerWithFutureMigration.migrate();

    UpgradeManagerImpl upgradeManagerWithoutFuture =
        new UpgradeManagerImpl(dataStoreManager, auditor, singletonList(migrationStep));
    upgradeManagerWithoutFuture.migrate();
  }

  @Test
  public void testUpgradeSkippedStep() throws UpgradeException {
    FutureMigrationStep futureMigrationStep = new FutureMigrationStep();
    UpgradeManagerImpl upgradeManager =
        new UpgradeManagerImpl(dataStoreManager, auditor, Arrays.asList(migrationStep, futureMigrationStep));
    upgradeManager.migrate();

    SkippedMigrationStep skippedMigrationStep = new SkippedMigrationStep();
    UpgradeManagerImpl upgradeManagerWithSkipped
        = new UpgradeManagerImpl(dataStoreManager, auditor,
        Arrays.asList(migrationStep, futureMigrationStep, skippedMigrationStep));
    upgradeManagerWithSkipped.migrate();

    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement();
         ResultSet results = stmt.executeQuery(SELECT_FROM_SKIPPED)) {
      assertForExampleTable(results, "name", "fawkes");
    }
    catch (Exception exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void testMaxMigrations() {
    FutureMigrationStep futureMigrationStep = new FutureMigrationStep();

    UpgradeManagerImpl upgradeManagerWithFutureMigration = new UpgradeManagerImpl(dataStoreManager, auditor,
        Arrays.asList(new NullVersionMigration(), migrationStep, futureMigrationStep));
    assertTrue(upgradeManagerWithFutureMigration.getMaxMigrationVersion().isPresent());
    assertThat(upgradeManagerWithFutureMigration.getMaxMigrationVersion().get().getVersion(), equalTo("4.5.6"));
  }

  @Test
  public void testGetBaselineWorksAsExpected() {
    TestBaselineMigrationStep baselineMigrationStep = new TestBaselineMigrationStep();
    FutureMigrationStep futureMigrationStep = new FutureMigrationStep();
    UpgradeManagerImpl upgradeManager
        = new UpgradeManagerImpl(dataStoreManager, auditor,
        Arrays.asList(migrationStep, baselineMigrationStep, futureMigrationStep));

    Optional<String> baseline = upgradeManager.getBaseline(MigrationVersion.fromVersion("2.0"));

    assertTrue(baseline.isPresent());
    assertTrue(baselineMigrationStep.version().isPresent());
    assertThat(baseline.get(), equalTo(baselineMigrationStep.version().get()));
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

  /**
   * Some MigrationSteps do not have a version
   */
  private static class NullVersionMigration
      implements DatabaseMigrationStep
  {
    @Override
    public Optional<String> version() {
      return Optional.empty();
    }

    @Override
    public void migrate(final Connection connection) {
    }
  }
}
