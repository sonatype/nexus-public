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
package org.sonatype.nexus.orient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.stream.Collectors;

import org.sonatype.goodies.common.ComponentSupport;

import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.orient.DatabaseManagerSupport.SYSTEM_PASSWORD;
import static org.sonatype.nexus.orient.DatabaseManagerSupport.SYSTEM_USER;

/**
 * Default {@link DatabaseExternalizer} implementation.
 *
 * @since 3.0
 */
public class DatabaseExternalizerImpl
  extends ComponentSupport
  implements DatabaseExternalizer
{
  public static final int BACKUP_BUFFER_SIZE = 16 * 1024;

  public static final int IMPORT_BUFFER_SIZE = 16 * 1024;

  public static final int BACKUP_COMPRESSION_LEVEL = 9;

  private final DatabaseManager databaseManager;

  private final String name;

  public DatabaseExternalizerImpl(final DatabaseManager databaseManager, final String name) {
    this.databaseManager = checkNotNull(databaseManager);
    this.name = checkNotNull(name);
  }

  /**
   * Helper to log prefixed command output messages.
   */
  private class LoggingCommandOutputListener
      implements OCommandOutputListener
  {
    private final String prefix;

    private LoggingCommandOutputListener(final String prefix) {
      this.prefix = prefix;
    }

    @Override
    public void onMessage(final String text) {
      if (log.isDebugEnabled()) {
        log.debug("{}: {}", prefix, text.trim());
      }
    }
  }

  private ODatabaseDocumentTx openDb() {
    return databaseManager.connect(name, false);
  }

  @Override
  public void backup(final OutputStream output) throws IOException {
    checkNotNull(output);

    log.debug("Backup database: {}", name);

    try (ODatabaseDocumentTx db = openDb()) {
      checkState(db.exists(), "Database does not exist: %s", name);

      log.debug("Starting backup");
      db.backup(output, null, null, new LoggingCommandOutputListener("BACKUP"),
          BACKUP_COMPRESSION_LEVEL, BACKUP_BUFFER_SIZE);
      log.debug("Completed backup");
    }
  }

  @Override
  public void restore(final InputStream input, final boolean overwrite) throws IOException {
    checkNotNull(input);

    log.debug("Restoring database: {}", name);

    try (ODatabaseDocumentTx db = openDb()) {
      if (db.exists()) {
        checkState(overwrite, "Database already exists: %s", name);
      }
      else {
        db.create();
      }

      log.debug("Starting restore");
      db.restore(input, null, null, new LoggingCommandOutputListener("RESTORE"));
      log.debug("Completed restore");

      if (db.getStorage().isClosed()) {
        // restore can leave storage layer closed, so re-open it
        db.getStorage().open(SYSTEM_USER, SYSTEM_PASSWORD, null);
      }
    }
  }

  @Override
  public void export(final OutputStream output) throws IOException {
    export(output, Collections.emptySet());
  }

  @Override
  public void export(final OutputStream output, final Set<String> excludedClassNames) throws IOException {
    checkNotNull(output);
    checkNotNull(excludedClassNames);

    log.debug("Exporting database: {}", name);

    try (ODatabaseDocumentTx db = openDb()) {
      checkState(db.exists(), "Database does not exist: %s", name);

      log.debug("Starting export");
      ODatabaseExport exporter = new ODatabaseExport(db, output, new LoggingCommandOutputListener("EXPORT"));

      if (!excludedClassNames.isEmpty()) {
        // orientdb maps to classnames to uppercase
        Set<String> upperCasedExcludedClassNames = excludedClassNames.stream()
            .map(String::toUpperCase)
            .collect(Collectors.toSet());
        log.debug("excluding : {}", upperCasedExcludedClassNames);
        exporter.setExcludeClasses(upperCasedExcludedClassNames);
      }

      exporter.exportDatabase();
      log.debug("Completed export");
    }
  }

  @Override
  public void import_(final InputStream input, final boolean overwrite) throws IOException {
    checkNotNull(input);

    log.debug("Importing database: {}", name);

    try (ODatabaseDocumentTx db = openDb()) {
      if (db.exists()) {
        checkState(overwrite, "Database already exists: %s", name);
      }
      else {
        db.create();
      }

      import_(db, input);
    }
  }

  private void import_(final ODatabaseDocumentTx db, final InputStream input) throws IOException {
    checkNotNull(db);
    checkNotNull(input);

    log.debug("Starting import");
    ODatabaseImport importer = new ODatabaseImport(db, input, new LoggingCommandOutputListener("IMPORT"));
    importer.importDatabase();
    log.debug("Completed import");
  }

  // NOTE: Exposed as public, but not on intf yet until we refine how this is used

  /**
   * Maybe import the database if there is an export file in the standard location.
   *
   * @param dir The base directory where we expect to maybe find {@link #EXPORT_FILENAME} or {@link #EXPORT_GZ_FILENAME}.
   */
  public void maybeImportFromStandardLocation(final ODatabaseDocumentTx db, final File dir) throws Exception {
    checkNotNull(db);
    checkNotNull(dir);

    InputStream input = null;

    File file = new File(dir, EXPORT_FILENAME);
    if (file.exists()) {
      input = new BufferedInputStream(new FileInputStream(file), IMPORT_BUFFER_SIZE);
    }
    else {
      file = new File(dir, EXPORT_GZ_FILENAME);
      if (file.exists()) {
        input = new GZIPInputStream(new FileInputStream(file), IMPORT_BUFFER_SIZE);
      }
    }

    if (input != null) {
      log.debug("Importing database: {} from: {}", name, file);

      try {
        import_(db, input);
      }
      finally {
        input.close();
      }

      // TODO: Rename file now that its processed?  Or maybe delete it?
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        '}';
  }
}
