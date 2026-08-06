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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.kv.upgrade.UpgradeNexusKeyValueStore;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.sonatype.nexus.validation.ssrf.SsrfProtectionConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Enables SSRF protection by default on new installations (those with no components across any format).
 * Existing installations (with components or an existing SSRF config) are unaffected.
 *
 * Detection: if the key-value store has no {@code ssrf.protection.config} entry AND no format-specific
 * component or asset table ({@code {format}_component}, {@code {format}_asset}) has any rows, the
 * instance is considered new and SSRF protection is enabled.
 *
 * <p>
 * Uses {@link UpgradeNexusKeyValueStore} (direct SQL on {@code nexus_key_value}) rather than
 * {@code GlobalKeyValueStore}, which extends {@code ConfigStoreSupport} and transitively binds the
 * EVENTS-phase {@code EventManager}; that store is not available during the UPGRADE phase in which this
 * step runs.
 * </p>
 */
@Component
public class SsrfProtectionDefaultMigrationStep_2_120
    implements DatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  static final String CONFIG_KEY = "ssrf.protection.config";

  private final UpgradeNexusKeyValueStore keyValueStore;

  private final List<Format> formats;

  @Autowired
  public SsrfProtectionDefaultMigrationStep_2_120(
      final UpgradeNexusKeyValueStore keyValueStore,
      final List<Format> formats)
  {
    this.keyValueStore = checkNotNull(keyValueStore);
    this.formats = checkNotNull(formats);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.120");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    // Ordering contract: the existing-config check MUST run (and return) before hasAnyContent() scans the
    // content tables. An existing ssrf.protection.config is the authoritative "already configured" signal,
    // so it is the intended fast-path and avoids touching content tables on every re-run. Do not reorder
    // these two branches.
    if (keyValueStore.get(CONFIG_KEY, SsrfProtectionConfigData.class).isPresent()) {
      log.info("SSRF protection config already exists in database, skipping default enablement");
      return;
    }

    if (hasAnyContent(connection)) {
      log.info("Existing installation detected (content found), skipping SSRF default enablement");
      return;
    }

    log.info("New installation detected (no content found), enabling SSRF protection by default");
    SsrfProtectionConfigData config = SsrfProtectionConfigData.from(
        new SsrfProtectionConfiguration(true, Set.of(), Set.of()));
    keyValueStore.setObject(CONFIG_KEY, config);
  }

  private boolean hasAnyContent(final Connection connection) throws SQLException {
    for (Format format : formats) {
      String formatName = format.getValue();
      if (hasRows(connection, formatName + "_component") || hasRows(connection, formatName + "_asset")) {
        log.debug("Found content in format: {}", formatName);
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether the given table has at least one row. The table is existence-checked first so that a
   * lazily-created (not-yet-materialized) {@code {format}_component}/{@code _asset} table is skipped without
   * issuing a query against it. Previously this swallowed every exception and returned {@code false}: on
   * PostgreSQL a query against a missing table raises {@code 42P01}, which aborts the migration transaction
   * and makes every subsequent probe fail with {@code 25P02} ("current transaction is aborted"). That
   * silently misclassified an existing multi-format install as a new install and enabled SSRF protection
   * without consent. Genuine query failures now propagate and fail the migration rather than being hidden.
   */
  private boolean hasRows(final Connection connection, final String tableName) throws SQLException {
    if (!tableExists(connection, tableName)) {
      return false;
    }
    String sql = "SELECT 1 FROM " + tableName + " LIMIT 1";
    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      return rs.next();
    }
  }
}
