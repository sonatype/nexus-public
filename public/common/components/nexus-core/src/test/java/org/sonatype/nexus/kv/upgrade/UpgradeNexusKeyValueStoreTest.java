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
package org.sonatype.nexus.kv.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.internal.kv.NexusKeyValueDAO;
import org.sonatype.nexus.kv.ValueType;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real-database tests for {@link UpgradeNexusKeyValueStore}, verifying direct-SQL parity with the
 * {@code GlobalKeyValueStore} persistence format on {@code nexus_key_value}. These tests run against H2
 * only; PostgreSQL parity is not exercised here and will be covered by the forthcoming {@code UpgradeMatrixIT}
 * in {@code nexus-integration-tests}.
 */
class UpgradeNexusKeyValueStoreTest
{
  @DataSessionConfiguration(daos = {NexusKeyValueDAO.class})
  TestDataSessionSupplier dataSessionSupplier;

  // NOTE: production injects the Spring-configured ObjectMapper; these tests use a bare new ObjectMapper().
  // That is adequate for the primitive / Map<String, Object> payloads exercised here, but a typed payload
  // relying on a registered module (e.g. JavaTimeModule for Instant) could round-trip differently than
  // production does — see the parity note on UpgradeNexusKeyValueStore.
  private UpgradeNexusKeyValueStore store() {
    return new UpgradeNexusKeyValueStore(dataSessionSupplier, new ObjectMapper());
  }

  @DatabaseTest
  void getMissingKey_returnsEmpty() {
    assertThat(store().getKey("nope")).isEmpty();
  }

  @DatabaseTest
  void setAndGetBoolean() {
    UpgradeNexusKeyValueStore store = store();
    store.setBoolean("b", true);
    assertThat(store.getBoolean("b")).contains(true);
  }

  @DatabaseTest
  void setAndGetString() {
    UpgradeNexusKeyValueStore store = store();
    store.setString("s", "hello");
    assertThat(store.getString("s")).contains("hello");
    assertThat(store.getKey("s").orElseThrow().type()).isEqualTo(ValueType.CHARACTER);
  }

  @DatabaseTest
  void setAndGetInt() {
    UpgradeNexusKeyValueStore store = store();
    store.setInt("i", 42);
    assertThat(store.getInt("i")).contains(42);
  }

  @DatabaseTest
  void setAndGetObject() {
    UpgradeNexusKeyValueStore store = store();
    store.setObject("o", new Sample("x", 7));

    Optional<Sample> result = store.get("o", Sample.class);
    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name).isEqualTo("x");
    assertThat(result.orElseThrow().count).isEqualTo(7);
    assertThat(store.getKey("o").orElseThrow().type()).isEqualTo(ValueType.OBJECT);
  }

  @DatabaseTest
  void set_isUpsert() {
    UpgradeNexusKeyValueStore store = store();
    store.setString("k", "first");
    store.setString("k", "second");
    assertThat(store.getString("k")).contains("second");
  }

  @DatabaseTest
  void onDiskFormatMatchesGlobalKeyValueStoreShape() throws Exception {
    store().setString("fmt", "hello");

    // Assert the raw row matches the GlobalKeyValueStore persistence contract: the ValueType name in the
    // `type` column and a JSON object {"value": <payload>} in the JSON(B) `value` column. Parsed via
    // ObjectMapper (not string-compared) so H2 vs PostgreSQL JSON formatting differences don't make this
    // brittle.
    try (Connection conn = dataSessionSupplier.openConnection()) {
      boolean h2 = "H2".equals(conn.getMetaData().getDatabaseProductName());
      String sql = h2
          ? "SELECT type, `value` FROM nexus_key_value WHERE `key` = ?"
          : "SELECT type, value FROM nexus_key_value WHERE key = ?";
      try (PreparedStatement statement = conn.prepareStatement(sql)) {
        statement.setString(1, "fmt");
        try (ResultSet results = statement.executeQuery()) {
          assertThat(results.next()).isTrue();
          assertThat(results.getString(1)).isEqualTo(ValueType.CHARACTER.name());
          JsonNode value = new ObjectMapper().readTree(results.getBytes(2));
          assertThat(value.path("value").asText()).isEqualTo("hello");
        }
      }
    }
  }

  @DatabaseTest
  void removeKey() {
    UpgradeNexusKeyValueStore store = store();
    store.setString("r", "v");

    assertThat(store.removeKey("r")).isTrue();
    assertThat(store.getKey("r")).isEmpty();
    assertThat(store.removeKey("r")).isFalse();
  }

  @DatabaseTest
  void removeKeys_batchesDeletesAndCountsOnlyExistingRows() {
    UpgradeNexusKeyValueStore store = store();
    store.setString("a", "1");
    store.setString("b", "2");
    store.setString("c", "3");

    // "missing" has no row: the returned count reflects only rows actually deleted (2), and the batch
    // removes a and b in a single connection/transaction.
    int removed = store.removeKeys(List.of("a", "b", "missing"));

    assertThat(removed).isEqualTo(2);
    assertThat(store.getKey("a")).isEmpty();
    assertThat(store.getKey("b")).isEmpty();
    assertThat(store.getKey("c")).isPresent();
  }

  @DatabaseTest
  void removeKeys_emptyOrNull_isNoOp() {
    UpgradeNexusKeyValueStore store = store();
    assertThat(store.removeKeys(List.of())).isZero();
    assertThat(store.removeKeys(null)).isZero();
  }

  @DatabaseTest
  void getKey_unknownType_throwsIllegalStateExceptionNamingValueAndKey() throws Exception {
    UpgradeNexusKeyValueStore store = store();
    store.setString("bad", "v"); // write a well-formed row first

    // Corrupt only the `type` column to a literal not in the ValueType enum (plain VARCHAR in both dialects).
    try (Connection conn = dataSessionSupplier.openConnection()) {
      boolean h2 = "H2".equals(conn.getMetaData().getDatabaseProductName());
      String sql = h2
          ? "UPDATE nexus_key_value SET type = ? WHERE `key` = ?"
          : "UPDATE nexus_key_value SET type = ? WHERE key = ?";
      try (PreparedStatement statement = conn.prepareStatement(sql)) {
        statement.setString(1, "BOGUS");
        statement.setString(2, "bad");
        statement.executeUpdate();
      }
      if (!conn.getAutoCommit()) {
        conn.commit();
      }
    }

    // parseType wraps the unknown enum literal in an IllegalStateException naming the value and key
    assertThatThrownBy(() -> store.getKey("bad"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("BOGUS")
        .hasMessageContaining("bad");
  }

  static class Sample
  {
    public String name;

    public int count;

    Sample() {
      // for Jackson
    }

    Sample(final String name, final int count) {
      this.name = name;
      this.count = count;
    }
  }
}
