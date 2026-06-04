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
package org.sonatype.nexus.repository.search.sql.query;

import javax.sql.DataSource;

import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Detects the database type at runtime by examining the DataSource.
 * This is necessary because database configuration is loaded after Spring Boot conditionals are evaluated.
 * The DataSource access is deferred until getDatabaseId() is called (after STORAGE lifecycle has initialized).
 */
@Component
public class DatabaseTypeDetector
{
  private final DataStoreManager dataStoreManager;

  private volatile String databaseId;

  @Autowired
  public DatabaseTypeDetector(final DataStoreManager dataStoreManager) {
    this.dataStoreManager = checkNotNull(dataStoreManager);
  }

  /**
   * Returns the database ID (e.g., "H2", "PostgreSQL") by querying the DataSource.
   * Result is cached after first call.
   * The DataSource is accessed via DataStoreManager to ensure it's available (created during STORAGE lifecycle).
   */
  private String getDatabaseId() {
    if (databaseId == null) {
      synchronized (this) {
        if (databaseId == null) {
          try {
            DataStore<?> dataStore = dataStoreManager.get(DEFAULT_DATASTORE_NAME)
                .orElseThrow(() -> new IllegalStateException("DataStore not found: " + DEFAULT_DATASTORE_NAME));
            DataSource dataSource = dataStore.getDataSource();
            databaseId = new VendorDatabaseIdProvider().getDatabaseId(dataSource);
          }
          catch (Exception e) {
            throw new IllegalStateException("Failed to detect database type", e);
          }
        }
      }
    }
    return databaseId;
  }

  /**
   * Returns true if the configured database is H2.
   */
  public boolean isH2() {
    return "H2".equalsIgnoreCase(getDatabaseId());
  }

  /**
   * Returns true if the configured database is PostgreSQL.
   */
  public boolean isPostgreSQL() {
    return "PostgreSQL".equalsIgnoreCase(getDatabaseId());
  }
}
