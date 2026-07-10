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
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.common.upgrade.events.UpgradeCompletedEvent;
import org.sonatype.nexus.common.upgrade.events.UpgradeStartedEvent;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseExtension;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.sonatype.nexus.upgrade.datastore.UpgradeException;

import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@ExtendWith({MockitoExtension.class, DatabaseExtension.class})
public class UpgradeManagerImplTest
{
  private static final String SELECT_FROM_FLYWAY_SCHEMA_HISTORY = "SELECT * FROM \"flyway_schema_history\"";

  private static final String SELECT_FROM_EXAMPLE = "SELECT * FROM example";

  private static final String SELECT_FROM_SKIPPED = "SELECT * FROM skipped";

  @DataSessionConfiguration(daos = {})
  TestDataSessionSupplier dataSessionSupplier;

  @Mock
  private DataStoreManager dataStoreManager;

  @Mock
  private PostStartupUpgradeAuditor auditor;

  TestMigrationStep migrationStep = new TestMigrationStep();

  @BeforeEach
  public void setUp() {
    if (dataSessionSupplier == null) {
      return;
    }
    when(dataStoreManager.get(DEFAULT_DATASTORE_NAME)).thenReturn(Optional.of(getDataStore()));
  }

  @DatabaseTest
  public void testNoUpgrades() throws Exception {
    UpgradeManagerImpl upgradeManager = underTest();
    upgradeManager.migrate();

    try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
      Exception exception = assertThrows(Exception.class,
          () -> stmt.executeQuery(SELECT_FROM_FLYWAY_SCHEMA_HISTORY));

      if ("PostgreSQL".equals(conn.getMetaData().getDatabaseProductName())) {
        assertThat(exception.getMessage(), containsString("relation \"flyway_schema_history\" does not exist"));
      }
      else {
        assertThat(exception.getMessage(), containsString("Table \"flyway_schema_history\" not found"));
      }
    }
    // No changes should fire no events
    verifyNoInteractions(auditor);
  }

  @DatabaseTest
  public void testExampleUpgrade() throws Exception {
    UpgradeManagerImpl upgradeManager = underTest(migrationStep);

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

  private static void assertForExampleTable(
      final ResultSet results,
      final String name,
      final String fawkes) throws SQLException
  {
    assertTrue(results.next());
    assertThat(results.getString(name), equalTo(fawkes));
    assertFalse(results.next());
  }

  @DatabaseTest
  public void testWithDatastoreFromFuture() throws UpgradeException {
    FutureMigrationStep futureMigrationStep = new FutureMigrationStep();

    UpgradeManagerImpl upgradeManagerWithFutureMigration = underTest(
        migrationStep, futureMigrationStep);
    upgradeManagerWithFutureMigration.migrate();

    UpgradeManagerImpl upgradeManagerWithoutFuture =
        underTest(migrationStep);

    assertThrows(UpgradeException.class, upgradeManagerWithoutFuture::migrate);
  }

  @DatabaseTest
  public void testUpgradeSkippedStep() throws UpgradeException {
    FutureMigrationStep futureMigrationStep = new FutureMigrationStep();
    UpgradeManagerImpl upgradeManager =
        underTest(migrationStep, futureMigrationStep);
    upgradeManager.migrate();

    SkippedMigrationStep skippedMigrationStep = new SkippedMigrationStep();
    UpgradeManagerImpl upgradeManagerWithSkipped = underTest(
        migrationStep, futureMigrationStep, skippedMigrationStep);
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

  @DatabaseTest
  public void testMaxMigrations() {
    FutureMigrationStep futureMigrationStep = new FutureMigrationStep();

    UpgradeManagerImpl upgradeManagerWithFutureMigration =
        underTest(new NullVersionMigration(), migrationStep, futureMigrationStep);
    assertTrue(upgradeManagerWithFutureMigration.getMaxMigrationVersion().isPresent());
    assertThat(upgradeManagerWithFutureMigration.getMaxMigrationVersion().get().getVersion(), equalTo("4.5.6"));
  }

  @DatabaseTest
  public void testGetBaselineWorksAsExpected() {
    TestBaselineMigrationStep baselineMigrationStep = new TestBaselineMigrationStep();
    FutureMigrationStep futureMigrationStep = new FutureMigrationStep();
    UpgradeManagerImpl upgradeManager = underTest(migrationStep, baselineMigrationStep, futureMigrationStep);

    Optional<String> baseline = upgradeManager.getBaseline(MigrationVersion.fromVersion("2.0"));

    assertTrue(baseline.isPresent());
    assertTrue(baselineMigrationStep.version().isPresent());
    assertThat(baseline.get(), equalTo(baselineMigrationStep.version().get()));
  }

  @DatabaseTest
  public void testMigrate_exceptionCauseChainContainsNoAdvertising() throws Exception {
    UpgradeManagerImpl upgradeManager = underTest(migrationStep);
    upgradeManager.migrate();

    try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(
          "UPDATE \"flyway_schema_history\" SET \"description\" = 'WrongDescriptionForTest' WHERE \"version\" = '1.0'");
    }

    UpgradeManagerImpl upgradeManagerWithNewStep = underTest(migrationStep, new AnotherMigrationStep());
    UpgradeException thrown = assertThrows(UpgradeException.class, upgradeManagerWithNewStep::migrate);

    assertThat(thrown.getMessage(), not(containsString("Learn more:")));
    assertThat(thrown.getCause().getMessage(), not(containsString("Learn more:")));
    assertThat(thrown.getCause().getClass().getName(), not(containsString("flyway")));
  }

  @Test
  public void sanitizeFlywayMessage_removesLearnMoreLinks() {
    String input = "Validate failed: Migrations have failed validation. " +
        "Either revert the changes to the migration, or run repair to update the schema history. " +
        "Need more flexibility with validation rules? Learn more: https://rd.gt/3AbJUZE";

    String result = UpgradeManagerImpl.sanitizeFlywayMessage(input);

    assertThat(result, not(containsString("Learn more:")));
    assertThat(result, not(containsString("rd.gt")));
    assertThat(result, containsString("run repair to update the schema history"));
  }

  @Test
  public void sanitizeFlywayMessage_handlesNullMessage() {
    assertNull(UpgradeManagerImpl.sanitizeFlywayMessage(null));
  }

  @Test
  public void sanitizeFlywayMessage_preservesMessagesWithoutAdvertising() {
    String input = "Validate failed: Migrations have failed validation.";
    assertThat(UpgradeManagerImpl.sanitizeFlywayMessage(input), equalTo(input));
  }

  @Test
  public void sanitizeFlywayMessage_advertisingOnlyMessage_returnsOriginal() {
    String input = "Need more flexibility? Learn more: https://rd.gt/3AbJUZE";
    String result = UpgradeManagerImpl.sanitizeFlywayMessage(input);
    assertThat(result, equalTo(input));
  }

  @Test
  public void sanitizeFlywayMessage_multipleLearnMoreLinks() {
    String input = "Error occurred. Need help? Learn more: https://rd.gt/1 More info? Learn more: https://rd.gt/2";
    String result = UpgradeManagerImpl.sanitizeFlywayMessage(input);
    assertThat(result, not(containsString("Learn more:")));
    assertThat(result, containsString("Error occurred."));
  }

  @Test
  public void sanitizeFlywayMessage_removesHelpRedGateLinks() {
    String input = "Either revert the changes to the migration, or run repair to update the schema history.\n" +
        "Need more flexibility with validation rules? Learn more: " +
        "https://help.red-gate.com/help/flyway-cli12/help_4.aspx?topic=flyway-blog/older-posts/customize-validation-rules-with-ignoremigrationpatterns";

    String result = UpgradeManagerImpl.sanitizeFlywayMessage(input);

    assertThat(result, not(containsString("Learn more:")));
    assertThat(result, not(containsString("help.red-gate.com")));
    assertThat(result, containsString("run repair to update the schema history"));
  }

  private DataStore<?> getDataStore() {
    return dataSessionSupplier.getDataStore(DEFAULT_DATASTORE_NAME);
  }

  private Connection getConnection() throws SQLException {
    return getDataStore()
        .getDataSource()
        .getConnection();
  }

  private UpgradeManagerImpl underTest(final DatabaseMigrationStep... steps) {
    UpgradeManagerImpl underTest = new UpgradeManagerImpl(dataStoreManager, auditor, List.of(steps));

    try {
      underTest.start();
    }
    catch (Exception e) {
      fail(e);
    }

    return underTest;
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

  private static class AnotherMigrationStep
      implements DatabaseMigrationStep
  {
    @Override
    public Optional<String> version() {
      return Optional.of("2.0");
    }

    @Override
    public void migrate(final Connection connection) {
    }
  }
}
