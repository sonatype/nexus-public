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
package org.sonatype.nexus.repository.content.upgrades;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.BASEDIR;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.PATH_KEY;

/**
 * Moves each reconciliation log from ${karaf.data}/log/blobstore/${blobstore}/%date.log to &lt;blobstore
 * root&gt;/reconciliation/%date.log
 *
 * @since 3.41
 */
@Named
@Singleton
public class DatastoreBlobReconciliationLogMigrator_1_11
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  public static final String RECONCILIATION_DIRECTORY_NAME = "reconciliation";

  protected static final String BLOBSTORE = "blobstore";

  protected static final String BLOBSTORE_LOG_PATH = "log" + File.separator + BLOBSTORE;

  protected static final String ATTRIBUTES = "attributes";

  protected static final String TABLE_NAME = "blob_store_configuration";

  private static final String QUERY = "SELECT * FROM %s WHERE name = ?";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final TypeReference<Map<String, Map<String, Object>>>
      ATTRIBUTES_TYPE_REF = new TypeReference<Map<String, Map<String, Object>>>() { };

  private final ApplicationDirectories applicationDirectories;

  @Inject
  public DatastoreBlobReconciliationLogMigrator_1_11(final ApplicationDirectories applicationDirectories) {
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("1.11");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (!tableExists(connection, TABLE_NAME)) {
      log.info("Table {} does not exist. Skipping upgrade step.", TABLE_NAME);
      return;
    }

    File reconciliationLogsBaseDirectory = applicationDirectories.getWorkDirectory(BLOBSTORE_LOG_PATH);
    ofNullable(reconciliationLogsBaseDirectory)
        .map(File::listFiles)
        .ifPresent(blobStoreReconciliationLogDirs -> copyFiles(blobStoreReconciliationLogDirs, connection));

    ofNullable(reconciliationLogsBaseDirectory).ifPresent(this::deleteDirectory);
  }

  private void deleteDirectory(final File reconciliationLogsBaseDirectory) {
    try {
      FileUtils.deleteDirectory(reconciliationLogsBaseDirectory);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void copyFiles(final File[] blobStoreReconciliationLogDirs, final Connection connection) {
    log.info("Found reconciliation logs for {} blob stores to migrate", blobStoreReconciliationLogDirs.length);
    for (File reconciliationLogDirectory : blobStoreReconciliationLogDirs) {
      String blobStoreName = reconciliationLogDirectory.getName();
      getBlobStorePath(blobStoreName, connection)
          .map(this::toAbsoluteBlobStorePath)
          .ifPresent(blobStorePath -> copyDirectoryContentsToBlobstore(reconciliationLogDirectory, blobStorePath));
    }
  }

  private void copyDirectoryContentsToBlobstore(final File sourceDir, final Path blobStorePath) {
    try {
      Path destination = blobStorePath.resolve(RECONCILIATION_DIRECTORY_NAME);
      List<File> logFiles = ofNullable(sourceDir.listFiles()).map(Arrays::asList).orElse(emptyList());
      log.info("Copying reconciliation logs from {} to {}", sourceDir, destination);
      FileUtils.copyToDirectory(logFiles, destination.toFile());
      log.info("Copied reconciliation logs from {} to {}", sourceDir, destination);
      FileUtils.deleteDirectory(sourceDir);
    }
    catch (IOException e) {
      log.warn("Skipping copy of reconciliation logs contained in {} because of error {}", sourceDir,
          getFullStackTrace(e));
    }
  }

  private Optional<Path> getBlobStorePath(final String blobStoreName, final Connection connection) {
    try (PreparedStatement statement = connection.prepareStatement(String.format(QUERY, TABLE_NAME))) {
      statement.setString(1, blobStoreName);
      final ResultSet resultSet = statement.executeQuery();

      if (resultSet.next()) {
        String attributeString = resultSet.getString(ATTRIBUTES);
        log.info("attributes {}", attributeString);
        Map<String, Map<String, Object>> nestedAttributesMap = MAPPER.readValue(attributeString, ATTRIBUTES_TYPE_REF);
        return Optional.of(Paths.get(nestedAttributesMap.get(CONFIG_KEY).get(PATH_KEY).toString()));
      }
    }
    catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }
    return empty();
  }

  private Path toAbsoluteBlobStorePath(final Path configurationPath) {
    if (configurationPath.isAbsolute()) {
      return configurationPath;
    }

    Path baseDir = applicationDirectories.getWorkDirectory(BASEDIR).toPath();
    try {
      Path normalizedBase = baseDir.toRealPath().normalize();
      return normalizedBase.resolve(configurationPath.normalize());
    }
    catch (IOException e) {
      log.error("Error converting {} to absolute path.", baseDir);
    }
    return null;
  }
}
