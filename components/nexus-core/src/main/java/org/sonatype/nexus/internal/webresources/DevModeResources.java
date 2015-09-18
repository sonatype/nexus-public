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
package org.sonatype.nexus.internal.webresources;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * Utility related to finding resources when {@code NEXUS_RESOURCE_DIRS} environment-variable
 * or {@code nexus.resource.dirs} system-property is set.
 *
 * @since 2.7
 */
public class DevModeResources
{
  private DevModeResources() {
    // empty
  }

  private static final List<File> resourceLocations = initializeResourceLocations();

  @Nullable
  private static List<File> initializeResourceLocations() {
    String directoriesToSearchProperty = System.getenv("NEXUS_RESOURCE_DIRS");
    if (directoriesToSearchProperty == null) {
      directoriesToSearchProperty = System.getProperty("nexus.resource.dirs");
    }
    if (directoriesToSearchProperty != null) {
      List<File> directoriesToSearch = Lists.newArrayList();
      String[] segments = directoriesToSearchProperty.split(",");
      for (String segment : segments) {
        try {
          File dir = new File(segment).getCanonicalFile();
          if (dir.exists() && dir.isDirectory()) {
            directoriesToSearch.add(dir);
          }
        }
        catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }
      if (!directoriesToSearch.isEmpty()) {
        return directoriesToSearch;
      }
    }
    return null;
  }

  /**
   * @since 3.0
   */
  @Nullable
  public static List<File> getResourceLocations() {
    return resourceLocations;
  }

  public static boolean hasResourceLocations() {
    return resourceLocations != null;
  }

  @Nullable
  public static URL getResourceIfOnFileSystem(final String path) {
    try {
      File file = getFileIfOnFileSystem(path);
      if (file != null) {
        return file.getAbsoluteFile().toURI().toURL();
      }
    }
    catch (MalformedURLException e) {
      // ignore
    }
    return null;
  }

  @Nullable
  public static File getFileIfOnFileSystem(final String path) {
    if (resourceLocations != null) {
      for (File dir : resourceLocations) {
        File file = new File(dir, path);
        if (file.exists()) {
          return file;
        }
      }
    }
    return null;
  }

}
