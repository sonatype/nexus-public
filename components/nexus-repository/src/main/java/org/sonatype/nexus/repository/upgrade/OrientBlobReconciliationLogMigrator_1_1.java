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
package org.sonatype.nexus.repository.upgrade;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import org.apache.commons.io.FileUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.BASEDIR;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.PATH_KEY;
import static org.sonatype.nexus.orient.DatabaseInstanceNames.CONFIG;

/**
 * Moves each reconciliation log from ${karaf.data}/log/blobstore/${blobstore}/%date.log to &lt;blobstore
 * root&gt;/reconciliation/%date.log
 *
 * @since 3.41
 */
@Named
@Singleton
@Upgrades(model = OrientBlobReconciliationLogMigrator_1_1.NAME, from = "1.0", to = "1.1")
@DependsOn(model = DatabaseInstanceNames.COMPONENT, version = "1.16", checkpoint = true)
@DependsOn(model = DatabaseInstanceNames.CONFIG, version = "1.9")
public class OrientBlobReconciliationLogMigrator_1_1
    extends DatabaseUpgradeSupport
{
  public static final String NAME = "OrientBlobReconciliationLogMigrator";

  public static final String RECONCILIATION_DIRECTORY_NAME = "reconciliation";

  protected static final String BLOBSTORE = "blobstore";

  protected static final String BLOBSTORE_LOG_PATH = "log" + File.separator + BLOBSTORE;

  protected static final String ATTRIBUTES = "attributes";

  private static final String BLOBSTORE_CONFIG_SELECT = "SELECT FROM repository_blobstore WHERE name = '%s'";

  private final ApplicationDirectories applicationDirectories;

  private final Provider<DatabaseInstance> databaseInstance;

  @Inject
  public OrientBlobReconciliationLogMigrator_1_1(
      final ApplicationDirectories applicationDirectories,
      @Named(CONFIG) final Provider<DatabaseInstance> databaseInstance)
  {
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.databaseInstance = checkNotNull(databaseInstance);
  }

  @Override
  public void apply() throws Exception {
    File reconciliationLogsBaseDirectory = applicationDirectories.getWorkDirectory(BLOBSTORE_LOG_PATH);
    ofNullable(reconciliationLogsBaseDirectory)
        .map(File::listFiles)
        .ifPresent(this::copyFiles);

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

  private void copyFiles(final File[] blobStoreReconciliationLogDirs) {
    log.info("Found reconciliation logs for {} blob stores to migrate", blobStoreReconciliationLogDirs.length);
    for (File reconciliationLogDirectory : blobStoreReconciliationLogDirs) {
      String blobStoreName = reconciliationLogDirectory.getName();
      getBlobStorePath(blobStoreName)
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

  private Optional<Path> getBlobStorePath(final String blobStoreName) {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      List<ODocument> results =
          db.command(new OCommandSQL(String.format(BLOBSTORE_CONFIG_SELECT, blobStoreName))).execute();
      return results
          .stream()
          .map(doc -> doc.<Map<String, Map<String, Object>>>field(ATTRIBUTES, OType.EMBEDDEDMAP))
          .map(attr -> attr.get(CONFIG_KEY))
          .filter(Objects::nonNull)
          .map(config -> config.get(PATH_KEY))
          .filter(Objects::nonNull)
          .map(Object::toString)
          .map(Paths::get)
          .findFirst();
    }
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
