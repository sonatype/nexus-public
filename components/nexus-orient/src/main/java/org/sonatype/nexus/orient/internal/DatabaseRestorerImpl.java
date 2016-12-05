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
package org.sonatype.nexus.orient.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.orient.DatabaseRestorer;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static java.util.stream.Collectors.toList;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Restores orient databases from standard location "sonatype-work/nexus3/backup".
 *
 * @since 3.2
 */
@Named
@Singleton
public class DatabaseRestorerImpl
    extends ComponentSupport
    implements DatabaseRestorer
{

  private static final String RESTORE_FROM_LOCATION = "backup";

  private final ApplicationDirectories applicationDirectories;

  @Inject
  public DatabaseRestorerImpl(final ApplicationDirectories applicationDirectories) {
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  @Override
  public boolean maybeRestoreDatabase(final ODatabaseDocumentTx db, final String databaseName) throws IOException {
    checkNotNull(db);
    checkNotNull(databaseName);

    File restoreFromLocation = applicationDirectories.getWorkDirectory(RESTORE_FROM_LOCATION);

    List<Path> backupFiles = Files.list(restoreFromLocation.toPath())
        .filter(path -> isBackupFileForDatabase(path, databaseName))
        .collect(toList());

    if (backupFiles.size() > 1) {
      throw new IllegalStateException("more than 1 backup file found for database " + databaseName + ": " +
          backupFiles);
    }

    if (!backupFiles.isEmpty()) {
      Path path = backupFiles.get(0);
      log.info("restoration of database {} from file {} starting", databaseName, path);
      doRestore(path.toFile(), db, databaseName);
      return true;
    }

    return false;
  }

  private void doRestore(final File file, final ODatabaseDocumentTx db, final String databaseName) {
    try (InputStream inputStream = new FileInputStream(file)) {
      db.restore(inputStream, null, null, iText -> {
          log.debug("database restore of {}, received message '{}'", databaseName, iText);
        });
      log.info("database {} restored", databaseName);
    }
    catch (Exception e) {
      throw new RuntimeException(String.format("database restore of %s from %s failed", databaseName, file), e);
    }
  }

  private boolean isBackupFileForDatabase(final Path path, final String databaseName) {
    checkNotNull(path);
    checkNotNull(databaseName);

    Path pathFile = path.getFileName();
    if (pathFile != null) {
      String filename = pathFile.toString();
      return
          Files.isRegularFile(path) &&
          filename.startsWith(databaseName + '-') &&
          filename.endsWith(".bak");
    }
    else {
      return false;
    }
  }
}
