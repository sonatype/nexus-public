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
package org.sonatype.nexus.datastore.mybatis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.ManagedLifecycleManager;
import org.sonatype.nexus.common.io.FileFinder;
import org.sonatype.nexus.common.io.ZipSupport;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.deleteIfExists;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Named
@Singleton
public class H2VersionUpgrader
    extends ComponentSupport
{
  private final ApplicationDirectories directories;

  private final ManagedLifecycleManager managedLifecycleManager;

  private final String TIMESTAMP_FORMAT = "%1$tY-%1$tm-%1$td-%1$tH-%1$tM-%1$tS";

  private final String ZIP_FILE_NAME = DEFAULT_DATASTORE_NAME + "-" +
      String.format(TIMESTAMP_FORMAT, LocalDateTime.now()) + "-backup.zip";

  @Inject
  public H2VersionUpgrader(
      ApplicationDirectories directories,
      ManagedLifecycleManager managedLifecycleManager)
  {
    this.directories = checkNotNull(directories);
    this.managedLifecycleManager = checkNotNull(managedLifecycleManager);
  }

  public HikariDataSource upgradeH2Database(String storeName, HikariConfig hikariConfig) throws Exception {
    HikariDataSource dataSource = null;
    log.info("Upgrade H2 database from V1.4 to V2.2 starts");
    Path dbPath = directories.getWorkDirectory("db").toPath();
    Optional<Path> sqlLatestFile = FileFinder.findLatestTimestampedFile(dbPath, "nexus-", ".sql");

    if(validateH2SqlFileExists(sqlLatestFile)) {
      backupCurrentH2Database(dbPath, storeName);
      dataSource = createUpgradedH2Database(hikariConfig, sqlLatestFile.get().toString());
    }
    log.info("Upgrade H2 database from V1.4 to V2.2 ends");
    return dataSource;
  }

  private boolean validateH2SqlFileExists(Optional<Path> sqlFile) throws Exception {
    if (!sqlFile.isPresent()) {
      StringBuilder buf = new StringBuilder();
      buf.append("\n-------------------------------------------------------------------------------------------" + 
      "----------------------------------------------------------------------------------\n\n");
      buf.append("Your H2 database version is no longer supported. Upgrade to a supported version using upgrade " +
      "instructions at https://links.sonatype.com/products/nxrm3/docs/upgrade-h2.html.");
      buf.append("\n\n-----------------------------------------------------------------------------------------" + 
      "------------------------------------------------------------------------------------\n\n");
      log.error(buf.toString());
      managedLifecycleManager.shutdownWithExitCode(1);
      return false;
    }
    return true;
  }

  private void backupCurrentH2Database(Path dbPath, String storeName) throws Exception {
    log.info("H2 database V1.4 backup starts");
    String dbFileName = storeName + ".mv.db";
    String dbTraceFileName = storeName + ".trace.db";
    List<String> filesToZip = Arrays.asList(dbFileName, dbTraceFileName);
    String zipPath = dbPath + "/" + ZIP_FILE_NAME;
    ZipSupport.zipFiles(dbPath, filesToZip, zipPath);
    deleteFile(dbPath.resolve(dbFileName));
    deleteFile(dbPath.resolve(dbTraceFileName));
    log.info("H2 database V1.4 backup ends, file created: " + zipPath);
  }

  private HikariDataSource createUpgradedH2Database(HikariConfig hikariConfig, String sqlFilePath) throws Exception {
    log.info("Create H2 database V2.2 starts");
    HikariDataSource dataSource = new HikariDataSource(hikariConfig);
    try (Connection conn = dataSource.getConnection()) {
        try (PreparedStatement scriptStmt = conn.prepareStatement("RUNSCRIPT FROM '" + sqlFilePath + "' FROM_1X")) {
          scriptStmt.execute();
        }
    }
    log.info("Create H2 database V2.2 ends");
    return dataSource;
  }

  private void deleteFile(final Path tempFile) throws Exception {
    if (Files.exists(tempFile) && !Files.isDirectory(tempFile)) {
      deleteIfExists(tempFile);
    }
  }
}
