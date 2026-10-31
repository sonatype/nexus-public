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
package org.sonatype.nexus.internal.capability.storage;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.UUID;

import org.sonatype.nexus.datastore.api.DataSessionSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Shared test helper for inserting {@code capability_storage_item} rows with dialect-correct JSON binding
 * (H2 {@code byte[]} / PostgreSQL {@code String}). Keeps the insert/binding logic in one place rather than
 * duplicated across the upgrade and cleanup tests.
 */
public final class CapabilityStorageItemTestSupport
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String INSERT =
      "INSERT INTO capability_storage_item (id, version, type, enabled, notes, properties) "
          + "VALUES (?, ?, ?, ?, ?, ?)";

  private CapabilityStorageItemTestSupport() {
    // static helper
  }

  /**
   * Inserts a row on its own connection (committing if needed) and returns the generated id.
   */
  public static UUID insert(
      final DataSessionSupplier supplier,
      final String type,
      final Map<String, String> properties) throws Exception
  {
    try (Connection conn = supplier.openConnection(DEFAULT_DATASTORE_NAME)) {
      UUID id = insert(conn, type, properties);
      if (!conn.getAutoCommit()) {
        conn.commit();
      }
      return id;
    }
  }

  /**
   * Inserts a row on the supplied connection (no commit — transaction control stays with the caller) and
   * returns the generated id.
   */
  public static UUID insert(
      final Connection conn,
      final String type,
      final Map<String, String> properties) throws Exception
  {
    UUID id = UUID.randomUUID();
    try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
      boolean h2 = "H2".equals(conn.getMetaData().getDatabaseProductName());
      ps.setObject(1, id);
      ps.setInt(2, 1);
      ps.setString(3, type);
      ps.setBoolean(4, true);
      ps.setString(5, "notes");
      byte[] json = MAPPER.writeValueAsBytes(properties);
      if (h2) {
        ps.setBytes(6, json);
      }
      else {
        ps.setString(6, new String(json, StandardCharsets.UTF_8));
      }
      ps.executeUpdate();
    }
    return id;
  }
}
