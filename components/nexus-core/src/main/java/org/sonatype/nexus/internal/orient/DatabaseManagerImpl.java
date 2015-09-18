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
package org.sonatype.nexus.internal.orient;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.io.DirSupport;
import org.sonatype.nexus.jmx.reflect.ManagedAttribute;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.orient.DatabaseExternalizer;
import org.sonatype.nexus.orient.DatabaseExternalizerImpl;
import org.sonatype.nexus.orient.DatabaseManager;
import org.sonatype.nexus.orient.DatabaseManagerSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@code plocal} {@link DatabaseManager} implementation.
 *
 * @since 3.0
 */
@Named
@Singleton
@ManagedObject
public class DatabaseManagerImpl
    extends DatabaseManagerSupport
{
  public static final String WORK_PATH = "db";

  private final File databasesDirectory;

  @Inject
  public DatabaseManagerImpl(final ApplicationDirectories applicationDirectories) {
    checkNotNull(applicationDirectories);
    this.databasesDirectory = applicationDirectories.getWorkDirectory(WORK_PATH);
    log.debug("Databases directory: {}", databasesDirectory);
  }

  @VisibleForTesting
  public DatabaseManagerImpl(final File databasesDirectory) {
    this.databasesDirectory = checkNotNull(databasesDirectory);
    log.debug("Databases directory: {}", databasesDirectory);
  }

  @ManagedAttribute
  public File getDatabasesDirectory() {
    return databasesDirectory;
  }

  /**
   * Returns the directory for the given named database.  Directory may or may not exist.
   */
  private File directory(final String name) throws IOException {
    return new File(databasesDirectory, name).getCanonicalFile();
  }

  @Override
  protected String connectionUri(final String name) {
    try {
      File dir = directory(name);
      DirSupport.mkdir(dir);

      return "plocal:" + dir.toURI().getPath();
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * When the database is being created, maybe import from the standard export location.
   *
   * @see DatabaseExternalizer#EXPORT_FILENAME
   * @see DatabaseExternalizer#EXPORT_GZ_FILENAME
   * @see DatabaseExternalizerImpl#maybeImportFromStandardLocation(ODatabaseDocumentTx, File)
   */
  @Override
  protected void created(final ODatabaseDocumentTx db, final String name) throws Exception {
    File dir = directory(name);
    DatabaseExternalizerImpl externalizer = externalizer(name);
    externalizer.maybeImportFromStandardLocation(db, dir);
  }
}
