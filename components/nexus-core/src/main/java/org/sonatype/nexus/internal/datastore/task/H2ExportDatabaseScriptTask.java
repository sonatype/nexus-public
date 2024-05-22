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
package org.sonatype.nexus.internal.datastore.task;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.datastore.api.DataStoreNotFoundException;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.scheduling.spi.TaskResultStateStore;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.datastore.mybatis.MyBatisDataStore.H2_DATABASE;
import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;

/**
 * A {@link Task} for exporting the SQL database to a script
 */
@Named
@TaskLogging(NEXUS_LOG_ONLY)
public class H2ExportDatabaseScriptTask
    extends TaskSupport implements Cancelable
{
  private final DataStoreManager dataStoreManager;

  private final ApplicationDirectories applicationDirectories;

  private final FreezeService freezeService;

  private final TaskResultStateStore taskResultStateStore;

  private final AuditRecorder auditRecorder;

  private final H2TaskSupport h2TaskSupport;

  private static final String DEFAULT_LOCATION = "db";

  private static final String AUDIT_DOMAIN = "backup";

  private static final long MINIMUM_SPACE = 5368709120L;

  @Inject
  public H2ExportDatabaseScriptTask(
      final DataStoreManager dataStoreManager,
      final ApplicationDirectories applicationDirectories,
      final FreezeService freezeService,
      final TaskResultStateStore taskResultStateStore,
      @Nullable final AuditRecorder auditRecorder)
  {
    this.dataStoreManager = checkNotNull(dataStoreManager);
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.freezeService = checkNotNull(freezeService);
    this.taskResultStateStore = checkNotNull(taskResultStateStore);
    this.auditRecorder = auditRecorder;
    this.h2TaskSupport = new H2TaskSupport();
  }

  @Override
  public String getMessage() {
    return "Export H2 SQL database to the specified location";
  }

  @Override
  protected Object execute() throws Exception {
    DataStore<?> dataStore = verifyDataStorePresence(dataStoreManager);

    String locationPath = getLocationPath();

    File scriptFolder = applicationDirectories.getWorkDirectory(locationPath);
    File scriptFile = new File(scriptFolder, getScriptName());

    checkFreeSpace(scriptFolder);

    verifyFileDoesNotAlreadyExist(scriptFile);

    log.info("Starting script generation of {} to {}", DEFAULT_DATASTORE_NAME, scriptFile.getAbsolutePath());

    long start = System.currentTimeMillis();

    freezeService.freezeDuring("System is in read-only mode during script generation", () -> export(dataStore, scriptFile));

    log.info("Completed script generation of {} in {} ms", DEFAULT_DATASTORE_NAME,
        System.currentTimeMillis() - start);

    return null;
  }

  private void export(final DataStore<?> dataStore, final File scriptFile) {
    AtomicBoolean cancellationCheck = new AtomicBoolean(false);
    CancelableHelper.set(cancellationCheck);
    try (Connection connection = dataStore.openConnection()) {
      if (H2_DATABASE.equals(connection.getMetaData().getDatabaseProductName())) {
        String location = scriptFile.getAbsolutePath();
        log.info("Beginning H2 SQL database export to {}", location);
        long linesExported = h2TaskSupport.exportDatabase(connection, location,
            progressMessage -> updateProgress(taskResultStateStore, progressMessage));
        log.info("Exported {} lines of SQL to {}", linesExported, location);
        // Although tasks runs are already audited, they have limited information. Therefore, we try (softly) to audit
        // additional information that may be of benefit to support if the nexus.log is lost.
        logExportToAudit(location, linesExported);
      }
      else {
        throw new UnsupportedOperationException("The underlying database is not an H2 Database.");
      }
    }
    catch (SQLException e) {
      throw new SqlScriptGenerationException("Script generation failed. Failed to open database connection: ", e);
    }
    finally {
      CancelableHelper.remove();
    }
  }

  private void logExportToAudit(String location, long lineCount) {
    if (auditRecorder!=null && auditRecorder.isEnabled()) {
      try {
        AuditData auditData = new AuditData();
        auditData.setContext("Exporting H2 Database as SQL");
        auditData.setDomain(AUDIT_DOMAIN);
        auditData.setTimestamp(new Date());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("location", location);
        attributes.put("lineCount", lineCount);

        auditData.setAttributes(attributes);

        auditRecorder.record(auditData);
      }
      catch (Exception ex) {
        log.warn("Failed to log H2 SQL database export to audit. Enable debug for more details.");
        if (log.isDebugEnabled()) {
          log.debug("Stack Trace:", ex);
        }
      }
    }
  }

  private DataStore<?> verifyDataStorePresence(DataStoreManager dataStoreManager) throws DataStoreNotFoundException {
    Optional<DataStore<?>> dataStore = dataStoreManager.get(DEFAULT_DATASTORE_NAME);
    if (!dataStore.isPresent()) {
      throw new DataStoreNotFoundException(DEFAULT_DATASTORE_NAME);
    }
    return dataStore.get();
  }

  @VisibleForTesting
  String getLocationPath() {
    String locationPath = getConfigurationLocationPath();

    if (Strings.isNullOrEmpty(locationPath)) {
      locationPath = DEFAULT_LOCATION;
    }

    return locationPath;
  }

  @VisibleForTesting
  String getConfigurationLocationPath() {
    return getConfiguration().getString(H2ExportDatabaseScriptTaskDescriptor.LOCATION);
  }

  private String getScriptName() {
    return DEFAULT_DATASTORE_NAME + "-" +
        String.format(TIMESTAMP_FORMAT, LocalDateTime.now()) + ".sql";
  }

  private void checkFreeSpace(File scriptFolder) throws InsufficientStorageException {
    if (scriptFolder.getUsableSpace() < MINIMUM_SPACE) {
      throw new InsufficientStorageException(
          "The script location does not have the recommended 5GB of free space available.");
    }
  }

  private void verifyFileDoesNotAlreadyExist(File scriptFile) throws IOException {
    if (scriptFile.isFile()) {
      throw new IOException("File already exists at script location: " + scriptFile.getAbsolutePath());
    }
  }
}
