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
package org.sonatype.nexus.supportzip.datastore;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * Restore Helper class.
 *
 * @since 3.30
 */
@Component
public class RestoreHelper
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String FILE_SUFFIX = ".json";

  private static final String DB_FOLDER_NAME = "db";

  private final Path dbPath;

  @Autowired
  public RestoreHelper(final ApplicationDirectories applicationDirectories) {
    this.dbPath = checkNotNull(applicationDirectories).getWorkDirectory(DB_FOLDER_NAME, false).toPath();
  }

  /**
   * Get the absolute path to the DB directory.
   * 
   * @return the DB path
   */
  public Path getDbPath() {
    return dbPath;
  }
}
