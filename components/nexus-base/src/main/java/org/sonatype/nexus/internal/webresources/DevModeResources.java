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
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;

/**
 * Utility related to finding resources when {@code NEXUS_RESOURCE_DIRS} environment-variable
 * or {@code nexus.resource.dirs} system-property is set.
 *
 * @since 2.7
 */
@Named
@Singleton
public class DevModeResources
    extends ComponentSupport
{
  private static final String ENV_VAR = "NEXUS_RESOURCE_DIRS";

  private static final String SYS_PROP = "nexus.resource.dirs";

  /**
   * List of directories configured as dev-mode resource locations, or null if not configured.
   */
  @Nullable
  private final List<File> resourceLocations;

  public DevModeResources() {
    this.resourceLocations = initializeResourceLocations();
  }

  /**
   * Detect the dev-mode resources search path from environment-variable or system-property, or null if not configured.
   */
  @Nullable
  private String detectSearchPath() {
    String envVar = System.getenv(ENV_VAR);
    String sysProp = System.getProperty(SYS_PROP);

    // complain if both mechanisms to configure are present, this can/will cause confusion
    if (envVar != null && sysProp != null) {
      log.warn("Both environment-variable: {} and system-property: {} are set; environment-variable takes precedence",
          ENV_VAR, SYS_PROP);
    }

    if (envVar != null) {
      log.info("Search-path configured from environment-variable: {}", ENV_VAR);
      return envVar;
    }
    if (sysProp != null) {
      log.info("Search-path configured from system-property: {}", SYS_PROP);
      return sysProp;
    }

    // not configured
    return null;
  }

  @Nullable
  private List<File> initializeResourceLocations() {
    String searchPath = detectSearchPath();

    if (searchPath != null) {
      List<File> locations = new ArrayList<>();
      for (String segment : searchPath.split(",")) {
        try {
          File dir = new File(segment).getCanonicalFile();
          if (dir.exists() && dir.isDirectory()) {
            locations.add(dir);
          }
          else {
            log.warn("Invalid search-path segment: {}; ignoring", segment);
          }
        }
        catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }
      if (!locations.isEmpty()) {
        return locations;
      }
    }
    return null;
  }

  /**
   * Returns list of detected dev-mode resource locations or null if not configured or not valid locations detected.
   *
   * @since 3.0
   */
  @Nullable
  public List<File> getResourceLocations() {
    return resourceLocations;
  }

  /**
   * Returns a file reference for given path if dev-mode is configured and a matching file exists.
   */
  @Nullable
  public File getFileIfOnFileSystem(final String path) {
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
