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
package org.sonatype.nexus.internal.backup;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Utilities for doing backups of databases
 *
 * @since 3.2
 */
public interface DatabaseBackup
{

  /**
   * @return java.util.List&lt;String&gt; Names of databases
   */
  List<String> dbNames();

  /**
   * Creates a backup job
   *
   * @param backupFolder Name of folder where backup file will be created
   * @param dbName The name of the database being backed up
   * @return java.util.concurrent.Callable For storing backup data
   * @throws IOException
   */
  Callable<Void> fullBackup(String backupFolder, String dbName) throws IOException;

}
