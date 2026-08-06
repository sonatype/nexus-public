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
package org.sonatype.nexus.internal.capability.storage.upgrade;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.common.entity.HasEntityId;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemData;
import org.sonatype.nexus.upgrade.datastore.UpgradeConfigStoreSupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.UPGRADE;

/**
 * UPGRADE-phase-safe equivalent of {@code CapabilityStorage} that reads/writes the
 * {@code capability_storage_item} table directly via SQL.
 *
 * <p>
 * {@code CapabilityStorageImpl} extends {@code ConfigStoreSupport}, which injects {@code EventManager}
 * (an EVENTS-phase component); it therefore cannot be injected into UPGRADE-phase migration steps. This
 * store exposes the {@code getAll}/{@code update}/{@code remove} operations those steps need, reusing the
 * {@link CapabilityStorageItemData} data object and the same JSON {@code properties} persistence.
 * </p>
 */
@Component
@ManagedLifecycle(phase = UPGRADE)
public class UpgradeCapabilityStorage
    extends UpgradeConfigStoreSupport
{
  // ORDER BY id so the row order is deterministic across H2/PostgreSQL (and stable after a Postgres VACUUM).
  // CleanupCapabilityDuplicatesService keeps the first identity in each duplicate group, so the surviving
  // row must not depend on engine-defined scan order.
  private static final String SELECT_ALL =
      "SELECT id, version, type, enabled, notes, properties FROM capability_storage_item ORDER BY id";

  private static final String UPDATE =
      "UPDATE capability_storage_item SET version = ?, type = ?, enabled = ?, notes = ?, properties = ? WHERE id = ?";

  private static final String DELETE = "DELETE FROM capability_storage_item WHERE id = ?";

  private static final TypeReference<Map<String, String>> PROPERTIES_TYPE =
      new TypeReference<Map<String, String>>()
      {
      };

  // Dedicated, customization-free mapper for the capability_storage_item `properties` column. The app-wide
  // Spring ObjectMapper may carry arbitrary modules/features (sorted keys, default typing, etc.); a private
  // mapper keeps this column's JSON pipeline aligned with the raw mapper used by the production handler
  // (AbstractRawJsonTypeHandler). The payload is Map<String,String>, so no extra Jackson modules are needed.
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired
  public UpgradeCapabilityStorage(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  /**
   * Returns all stored capabilities keyed by identity.
   */
  public Map<CapabilityIdentity, CapabilityStorageItem> getAll() {
    Map<CapabilityIdentity, CapabilityStorageItem> result = new LinkedHashMap<>();
    try (Connection conn = openConnection();
        PreparedStatement statement = conn.prepareStatement(SELECT_ALL);
        ResultSet results = statement.executeQuery()) {
      while (results.next()) {
        UUID id = (UUID) results.getObject("id");
        CapabilityStorageItemData item = new CapabilityStorageItemData();
        item.setId(new EntityUUID(id));
        item.setVersion(results.getInt("version"));
        item.setType(results.getString("type"));
        item.setEnabled(results.getBoolean("enabled"));
        item.setNotes(results.getString("notes"));
        item.setProperties(readProperties(results.getBytes("properties")));
        result.put(new CapabilityIdentity(id.toString()), item);
      }
    }
    catch (SQLException e) {
      throw new IllegalStateException("Failed to read capability_storage_item", e);
    }
    // Match the immutable-map contract of CapabilityStorageImpl.getAll().
    return Collections.unmodifiableMap(result);
  }

  /**
   * Updates the stored capability identified by {@code id}.
   */
  public boolean update(final CapabilityIdentity id, final CapabilityStorageItem item) {
    checkNotNull(id, "id");
    checkNotNull(item, "item");
    // Reconcile the item's in-memory id with the parameter id, as CapabilityStorageImpl.update does, so a
    // caller that passes an item whose id differs from (or is unset relative to) id stays consistent.
    ((HasEntityId) item).setId(new EntityUUID(UUID.fromString(id.toString())));
    byte[] properties = writeProperties(item.getProperties());
    try (Connection conn = openConnection()) {
      try {
        boolean h2 = isH2(conn);
        int updated;
        try (PreparedStatement statement = conn.prepareStatement(UPDATE)) {
          // version is written verbatim, matching CapabilityStorageImpl and the DAO update mapper (which also
          // writes #{version} as-is): neither path increments the column, and the WHERE clause carries no
          // optimistic-locking check.
          statement.setInt(1, item.getVersion());
          statement.setString(2, item.getType());
          statement.setBoolean(3, item.isEnabled());
          statement.setString(4, item.getNotes());
          setJsonParameter(statement, 5, properties, h2);
          statement.setObject(6, UUID.fromString(id.toString()));
          updated = statement.executeUpdate();
        }
        commitIfNeeded(conn);
        return updated > 0;
      }
      catch (SQLException | RuntimeException e) {
        // Roll back on any failure so a dirty transaction is not returned to the pool: try-with-resources
        // calls close(), not rollback(), and pool disposition of an in-flight transaction is unsafe.
        rollbackIfNeeded(conn, e);
        throw e;
      }
    }
    catch (SQLException e) {
      throw new IllegalStateException("Failed to update capability_storage_item " + id, e);
    }
  }

  /**
   * Removes the stored capability identified by {@code id}.
   */
  public boolean remove(final CapabilityIdentity id) {
    checkNotNull(id, "id");
    try (Connection conn = openConnection()) {
      try {
        boolean removed;
        try (PreparedStatement statement = conn.prepareStatement(DELETE)) {
          statement.setObject(1, UUID.fromString(id.toString()));
          removed = statement.executeUpdate() > 0;
        }
        commitIfNeeded(conn);
        return removed;
      }
      catch (SQLException | RuntimeException e) {
        // Roll back on any failure so a dirty transaction is not returned to the pool: try-with-resources
        // calls close(), not rollback(), and pool disposition of an in-flight transaction is unsafe.
        rollbackIfNeeded(conn, e);
        throw e;
      }
    }
    catch (SQLException e) {
      throw new IllegalStateException("Failed to remove capability_storage_item " + id, e);
    }
  }

  /**
   * Removes all stored capabilities identified by {@code ids} in a single batched transaction (one connection
   * acquisition + one commit), returning the number of rows deleted. This avoids the connection-per-row and
   * commit-per-row (fsync-per-row on PostgreSQL) cost of calling {@link #remove(CapabilityIdentity)} in a
   * loop, which turns a large duplicate cleanup into N durable commits during the UPGRADE hot path.
   */
  public int removeAll(final Collection<CapabilityIdentity> ids) {
    checkNotNull(ids, "ids");
    if (ids.isEmpty()) {
      return 0;
    }
    try (Connection conn = openConnection()) {
      try {
        int removed = 0;
        try (PreparedStatement statement = conn.prepareStatement(DELETE)) {
          for (CapabilityIdentity id : ids) {
            statement.setObject(1, UUID.fromString(id.toString()));
            statement.addBatch();
          }
          for (int updateCount : statement.executeBatch()) {
            if (updateCount > 0) {
              removed += updateCount;
            }
          }
        }
        commitIfNeeded(conn);
        return removed;
      }
      catch (SQLException | RuntimeException e) {
        rollbackIfNeeded(conn, e);
        throw e;
      }
    }
    catch (SQLException e) {
      throw new IllegalStateException("Failed to remove capability_storage_item rows", e);
    }
  }

  private Map<String, String> readProperties(final byte[] json) {
    if (json == null) {
      return new LinkedHashMap<>();
    }
    try {
      return MAPPER.readValue(json, PROPERTIES_TYPE);
    }
    catch (IOException e) {
      throw new IllegalStateException("Failed to parse capability_storage_item properties JSON", e);
    }
  }

  private byte[] writeProperties(final Map<String, String> properties) {
    try {
      return MAPPER.writeValueAsBytes(properties);
    }
    catch (IOException e) {
      throw new IllegalStateException("Failed to serialize capability_storage_item properties JSON", e);
    }
  }
}
