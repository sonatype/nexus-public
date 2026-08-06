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
package org.sonatype.nexus.internal.ssrf;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.internal.kv.NexusKeyValueDAO;
import org.sonatype.nexus.kv.upgrade.UpgradeNexusKeyValueStore;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;
import org.sonatype.nexus.validation.ssrf.SsrfProtectionConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Real-database tests for {@link SsrfProtectionDefaultMigrationStep_2_120}, exercising the
 * {@link UpgradeNexusKeyValueStore} direct-SQL path against the {@code nexus_key_value} table.
 */
class SsrfProtectionDefaultMigrationStep_2_120Test
{
  private static final String CONFIG_KEY = "ssrf.protection.config";

  @DataSessionConfiguration(daos = {NexusKeyValueDAO.class})
  TestDataSessionSupplier dataSessionSupplier;

  private UpgradeNexusKeyValueStore keyValueStore() {
    return new UpgradeNexusKeyValueStore(dataSessionSupplier, new ObjectMapper());
  }

  @DatabaseTest
  void migrate_newInstall_enablesSsrfProtection() throws Exception {
    UpgradeNexusKeyValueStore store = keyValueStore();
    SsrfProtectionDefaultMigrationStep_2_120 underTest = new SsrfProtectionDefaultMigrationStep_2_120(store, List.of());

    try (Connection conn = dataSessionSupplier.openConnection()) {
      underTest.migrate(conn);
    }

    SsrfProtectionConfigData config = store.get(CONFIG_KEY, SsrfProtectionConfigData.class).orElseThrow();
    assertThat(config.isEnabled()).isTrue();
    assertThat(config.getAllowedIPs()).isEmpty();
    assertThat(config.getAllowedDomains()).isEmpty();
  }

  @DatabaseTest
  void migrate_existingConfig_skips() throws Exception {
    UpgradeNexusKeyValueStore store = keyValueStore();
    store.setObject(CONFIG_KEY,
        SsrfProtectionConfigData
            .from(new SsrfProtectionConfiguration(false, Set.of("10.0.0.1"), Set.of())));

    SsrfProtectionDefaultMigrationStep_2_120 underTest =
        new SsrfProtectionDefaultMigrationStep_2_120(store, List.of(format("maven2")));

    try (Connection conn = dataSessionSupplier.openConnection()) {
      underTest.migrate(conn);
    }

    // Existing (disabled) config must be left untouched.
    assertThat(store.get(CONFIG_KEY, SsrfProtectionConfigData.class).orElseThrow().isEnabled()).isFalse();
  }

  @DatabaseTest
  void migrate_existingInstall_withComponents_skips() throws Exception {
    UpgradeNexusKeyValueStore store = keyValueStore();
    SsrfProtectionDefaultMigrationStep_2_120 underTest =
        new SsrfProtectionDefaultMigrationStep_2_120(store, List.of(format("maven2")));

    try (Connection conn = dataSessionSupplier.openConnection(); Statement stmt = conn.createStatement()) {
      stmt.execute("CREATE TABLE maven2_component (id INT)");
      stmt.execute("CREATE TABLE maven2_asset (id INT)");
      stmt.execute("INSERT INTO maven2_component (id) VALUES (1)");

      underTest.migrate(conn);
    }

    assertThat(store.getKey(CONFIG_KEY)).isEmpty();
  }

  @DatabaseTest
  void migrate_existingInstall_withAssetsOnly_skips() throws Exception {
    UpgradeNexusKeyValueStore store = keyValueStore();
    SsrfProtectionDefaultMigrationStep_2_120 underTest =
        new SsrfProtectionDefaultMigrationStep_2_120(store, List.of(format("maven2")));

    try (Connection conn = dataSessionSupplier.openConnection(); Statement stmt = conn.createStatement()) {
      stmt.execute("CREATE TABLE maven2_component (id INT)");
      stmt.execute("CREATE TABLE maven2_asset (id INT)");
      // assets present but no components
      stmt.execute("INSERT INTO maven2_asset (id) VALUES (1)");

      underTest.migrate(conn);
    }

    assertThat(store.getKey(CONFIG_KEY)).isEmpty();
  }

  @DatabaseTest
  void migrate_mixedFormats_someHaveComponents_skips() throws Exception {
    UpgradeNexusKeyValueStore store = keyValueStore();
    SsrfProtectionDefaultMigrationStep_2_120 underTest =
        new SsrfProtectionDefaultMigrationStep_2_120(store, List.of(format("maven2"), format("npm")));

    try (Connection conn = dataSessionSupplier.openConnection(); Statement stmt = conn.createStatement()) {
      stmt.execute("CREATE TABLE maven2_component (id INT)");
      stmt.execute("CREATE TABLE maven2_asset (id INT)");
      stmt.execute("CREATE TABLE npm_component (id INT)");
      stmt.execute("CREATE TABLE npm_asset (id INT)");
      // only maven2 has component rows; npm is empty
      stmt.execute("INSERT INTO maven2_component (id) VALUES (1)");

      underTest.migrate(conn);
    }

    assertThat(store.getKey(CONFIG_KEY)).isEmpty();
  }

  @DatabaseTest
  void migrate_mixedFormats_someHaveAssetsOnly_skips() throws Exception {
    UpgradeNexusKeyValueStore store = keyValueStore();
    SsrfProtectionDefaultMigrationStep_2_120 underTest =
        new SsrfProtectionDefaultMigrationStep_2_120(store, List.of(format("maven2"), format("npm")));

    try (Connection conn = dataSessionSupplier.openConnection(); Statement stmt = conn.createStatement()) {
      stmt.execute("CREATE TABLE maven2_component (id INT)");
      stmt.execute("CREATE TABLE maven2_asset (id INT)");
      stmt.execute("CREATE TABLE npm_component (id INT)");
      stmt.execute("CREATE TABLE npm_asset (id INT)");
      // only npm has asset rows; maven2 is empty
      stmt.execute("INSERT INTO npm_asset (id) VALUES (1)");

      underTest.migrate(conn);
    }

    assertThat(store.getKey(CONFIG_KEY)).isEmpty();
  }

  @DatabaseTest
  void migrate_firstFormatTablesMissing_laterFormatHasContent_skips() throws Exception {
    // Regression guard for the transaction-abort cascade: the first-iterated format's content tables are
    // absent while a later format has content. A missing table must be skipped (via tableExists) rather
    // than misclassify the whole install. On PostgreSQL the pre-fix code queried the missing maven2 table,
    // aborted the transaction (42P01 -> 25P02), and every later probe returned false -> the existing npm
    // content was missed and SSRF protection was wrongly enabled. The step must detect an existing install.
    UpgradeNexusKeyValueStore store = keyValueStore();
    SsrfProtectionDefaultMigrationStep_2_120 underTest =
        new SsrfProtectionDefaultMigrationStep_2_120(store, List.of(format("maven2"), format("npm")));

    try (Connection conn = dataSessionSupplier.openConnection(); Statement stmt = conn.createStatement()) {
      // maven2_component / maven2_asset intentionally absent; npm has a component row
      stmt.execute("CREATE TABLE npm_component (id INT)");
      stmt.execute("CREATE TABLE npm_asset (id INT)");
      stmt.execute("INSERT INTO npm_component (id) VALUES (1)");

      underTest.migrate(conn);
    }

    assertThat(store.getKey(CONFIG_KEY)).isEmpty();
  }

  @DatabaseTest
  void migrate_contentTablesMissing_treatedAsNewInstall() throws Exception {
    UpgradeNexusKeyValueStore store = keyValueStore();
    SsrfProtectionDefaultMigrationStep_2_120 underTest =
        new SsrfProtectionDefaultMigrationStep_2_120(store, List.of(format("maven2")));

    try (Connection conn = dataSessionSupplier.openConnection()) {
      underTest.migrate(conn);
    }

    assertThat(store.get(CONFIG_KEY, SsrfProtectionConfigData.class).orElseThrow().isEnabled()).isTrue();
  }

  @DatabaseTest
  void migrate_idempotent_secondRunIsNoOp() throws Exception {
    UpgradeNexusKeyValueStore store = keyValueStore();
    SsrfProtectionDefaultMigrationStep_2_120 underTest = new SsrfProtectionDefaultMigrationStep_2_120(store, List.of());

    try (Connection conn = dataSessionSupplier.openConnection()) {
      underTest.migrate(conn);
      assertDoesNotThrow(() -> underTest.migrate(conn));
    }

    assertThat(store.get(CONFIG_KEY, SsrfProtectionConfigData.class).orElseThrow().isEnabled()).isTrue();
  }

  private static Format format(final String value) {
    Format format = mock(Format.class);
    lenient().when(format.getValue()).thenReturn(value);
    return format;
  }
}
