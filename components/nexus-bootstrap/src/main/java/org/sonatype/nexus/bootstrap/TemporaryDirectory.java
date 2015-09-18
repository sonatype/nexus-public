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
package org.sonatype.nexus.bootstrap;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper to ensure temporary directory is sane.
 *
 * @since 2.8
 */
public class TemporaryDirectory
{
  public static final String PROPERTY = "java.io.tmpdir";

  // NOTE: Do not reset the system property, we can not ensure this value will be used

  public static File get() throws IOException {
    String location = System.getProperty(PROPERTY, "tmp");
    File dir = new File(location).getCanonicalFile();
    mkdir(dir);

    // ensure we can create temporary files in this directory
    Path file = Files.createTempFile("nexus-tmpdir", ".tmp");
    Files.delete(file);
    return dir;
  }

  private static void mkdir(final File dir) throws IOException {
    try {
      Files.createDirectories(dir.toPath());
    }
    catch (FileAlreadyExistsException e) {
      // handle symlink case
      if (!Files.isDirectory(dir.toPath())) {
        throw new IOException("Unable to create java.io.tmpdir: " + dir, e);
      }
    }
  }
}
