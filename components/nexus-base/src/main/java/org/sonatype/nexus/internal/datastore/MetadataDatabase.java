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
package org.sonatype.nexus.internal.datastore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Add {@link SupportBundle} to export information about database.
 *
 * @since 3.30
 */
@Named
@Singleton
public class MetadataDatabase
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private static final Path PATH = Paths.get("work");

  private static final String DB_INFO_FILE_NAME = "db_info.properties";

  private final DataStoreManager dataStoreManager;

  @Inject
  public MetadataDatabase(final DataStoreManager dataStoreManager) {
    this.dataStoreManager = checkNotNull(dataStoreManager);
  }

  @Override
  public void customize(final SupportBundle supportBundle) {
    supportBundle.add(getDbInfo(DB_INFO_FILE_NAME));
  }

  private GeneratedContentSourceSupport getDbInfo(final String fileName) {
    return new GeneratedContentSourceSupport(Type.CONFIG, PATH.resolve(fileName).toString())
    {
      @Override
      protected void generate(final File file) throws Exception {
        Optional<DataStore<?>> dataStore = dataStoreManager.get(DataStoreManager.DEFAULT_DATASTORE_NAME);
        if (dataStore.isPresent()) {
          try (OutputStream output = new FileOutputStream(file)) {
            Properties dsProperties = getDbInfo(dataStore.get());
            dsProperties.store(output, null);
          }
        }
      }
    };
  }

  private Properties getDbInfo(final DataStore<?> dataStore) {
    Properties dsProperties = new Properties();
    try (Connection connection = dataStore.getDataSource().getConnection()) {
      dsProperties.setProperty("DatabaseProductName", connection.getMetaData().getDatabaseProductName());
      dsProperties.setProperty("DatabaseProductVersion", connection.getMetaData().getDatabaseProductVersion());
    }
    catch (SQLException e) {
      log.error("Can't collect datastore information", e);
    }

    return dsProperties;
  }
}
