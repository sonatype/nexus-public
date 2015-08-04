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
package org.sonatype.nexus.client.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper static methods for reading Maven properties file to get the version from it.
 *
 * @author cstamas
 * @since 2.5.0
 */
public class Version
{
  private static final Logger LOG = LoggerFactory.getLogger(Version.class);

  private Version() {
    // no instances
  }

  /**
   * Reads the version from a properties file, the one embedded by Maven into Jar during build.
   *
   * @param cl             the class loader to be used to load the properties file.
   * @param path           the binary path of the properties file to read from (might have more in case of shaded
   *                       JAR).
   * @param defaultVersion the version string to return in case of unsuccessful read of the properties file.
   * @return the version from the Maven properties file on given path, embedded into JAR.
   * @since 2.4.1
   */
  public static String readVersion(final ClassLoader cl, final String path, final String defaultVersion) {
    String version = defaultVersion;
    InputStream is = null;
    try {
      final Properties props = new Properties();
      is = cl.getResourceAsStream(path);
      if (is != null) {
        props.load(is);
        version = props.getProperty("version");
      }
    }
    catch (IOException e) {
      LOG.error("Could not load/read version from " + path, e);
    }
    finally {
      IOUtils.closeQuietly(is);
    }
    return version;
  }

  /**
   * Shorthand method. Reads the version from a properties file using classloader that loaded up this class.
   *
   * @param path           the binary path of the properties file to read from (might have more in case of shaded
   *                       JAR).
   * @param defaultVersion the version string to return in case of unsuccessful read of the properties file.
   * @return the version from the Maven properties file on given path, embedded into JAR.
   * @since 2.4.1
   */
  public static String readVersion(final String path, final String defaultVersion) {
    return readVersion(Version.class.getClassLoader(), path, defaultVersion);
  }
}
