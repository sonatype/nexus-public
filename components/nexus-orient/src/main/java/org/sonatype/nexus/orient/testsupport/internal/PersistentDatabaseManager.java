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
package org.sonatype.nexus.orient.testsupport.internal;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.sonatype.nexus.common.io.DirectoryHelper;
import org.sonatype.nexus.orient.DatabaseManager;
import org.sonatype.nexus.orient.DatabaseManagerSupport;

import com.google.common.base.Throwables;
import com.orientechnologies.common.io.OFileUtils;

/**
 * File-based {@link DatabaseManager} implementation.
 * 
 * @since 3.1
 */
public class PersistentDatabaseManager
    extends DatabaseManagerSupport
{
  private final File databasesDirectory;

  public PersistentDatabaseManager() {
    File targetDir = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile();
    databasesDirectory = new File(targetDir, "test-db." + UUID.randomUUID().toString().replace("-", ""));
    log.info("Database dir: {}", databasesDirectory);
  }

  /**
   * Returns the directory for the given named database. Directory may or may not exist.
   */
  private File directory(final String name) throws IOException {
    return new File(databasesDirectory, name).getCanonicalFile();
  }

  @Override
  protected String connectionUri(final String name) {
    try {
      File dir = directory(name);
      DirectoryHelper.mkdir(dir);

      return "plocal:" + OFileUtils.getPath(dir.getAbsolutePath()).replace("//", "/");
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
