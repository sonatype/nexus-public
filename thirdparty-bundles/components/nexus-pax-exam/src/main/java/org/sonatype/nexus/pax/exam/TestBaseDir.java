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
package org.sonatype.nexus.pax.exam;

import java.io.File;
import java.nio.file.Paths;

/**
 * Helper to access files from the surrounding Maven project.
 * 
 * @since 3.2
 */
public class TestBaseDir
{
  private static final String BASEDIR = new File(System.getProperty("basedir", "")).getAbsolutePath();

  private TestBaseDir() {
    // hidden
  }

  /**
   * Gets the absolute path to the base directory of the surrounding Maven project.
   */
  public static String get() {
    return BASEDIR;
  }

  /**
   * Resolves path against the base directory of the surrounding Maven project.
   */
  public static File resolve(final String path) {
    return Paths.get(BASEDIR, path).toFile();
  }
}
