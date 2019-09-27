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
package org.sonatype.nexus.features.internal;

import java.io.File;
import java.net.MalformedURLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.util.maven.Parser.pathFromMaven;

/**
 * Resolves a bundle location into the most direct link possible.
 *
 * If the bundle exists in the installation system repository then use a direct 'file:' link to it.
 * Also prefer a 'reference:' prefix (which avoids a copy) when it's a plain undecorated file link.
 * This tells Karaf to load the bundle directly without making a copy under the cache directory.
 *
 * @since 3.19
 */
public class LocationResolver
{
  private static final Logger log = LoggerFactory.getLogger(LocationResolver.class);

  private final File systemDir = new File("system");

  /**
   * Resolves the given location to the most direct link possible to reduce I/O during startup.
   */
  public String resolve(final String location) {
    String result = location;
    try {
      int mvn = location.indexOf("mvn:");
      if (mvn == 0) {
        result = systemPath(location);
      }
      else if (mvn > 0) {
        String prefix = location.substring(0, mvn);
        int flags = location.indexOf('$');
        if (flags < 0) {
          result = prefix + systemPath(location.substring(mvn));
        }
        else {
          String suffix = location.substring(flags, location.length());
          result = prefix + systemPath(location.substring(mvn, flags)) + suffix;
        }
      }
    }
    catch (MalformedURLException e) {
      log.warn("Malformed location {}", location, e);
    }
    // add 'reference:' to undecorated file links to avoid any copying of the bundle
    if (result.startsWith("file:")) {
      result = "reference:" + result;
    }
    log.debug("Resolved {} to {}", location, result);
    return result;
  }

  /**
   * Prefer a direct 'file:' link under the system directory if the file exists and can be read.
   */
  private String systemPath(final String mvnPath) throws MalformedURLException {
    String repoPath = pathFromMaven(mvnPath);
    if (new File(systemDir, repoPath).canRead()) {
      return "file:system/" + repoPath;
    }
    else {
      return mvnPath;
    }
  }
}
