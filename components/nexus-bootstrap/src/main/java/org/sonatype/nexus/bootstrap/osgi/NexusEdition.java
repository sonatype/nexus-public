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
package org.sonatype.nexus.bootstrap.osgi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.prefs.Preferences.userRoot;

public abstract class NexusEdition
{
  static final String NEXUS_EDITION = "nexus-edition";

  public static final String NEXUS_FEATURES = "nexus-features";

  private static final Logger log = LoggerFactory.getLogger(NexusEdition.class);

  public static final String NEXUS_LOAD_AS_OSS_PROP_NAME = "nexus.loadAsOSS";

  private static final String EDITION_PRO_PATH = "edition_pro";

  public static final String PRO_LICENSE_LOCATION = "/com/sonatype/nexus/professional";

  public abstract NexusEditionType getEdition();

  public abstract NexusEditionFeature getEditionFeature();

  /**
   * Determine whether or not we should be booting to the corresponding edition or not, based on the presence of a
   * edition marker file, license, or a System property that can be used to override the behaviour.
   */
  protected abstract boolean doesApply(final Properties properties, final Path workDirPath);

  protected abstract void doApply(final Properties properties, final Path workDirPath);

  protected abstract boolean shouldSwitchToOss(final Path workDirPath);

  public boolean applies(final Properties properties, final Path workDirPath) {
    return doesApply(properties, workDirPath);
  }

  public void apply(final Properties properties, final Path workDirPath) {
    doApply(properties, workDirPath);
  }

  protected boolean hasNexusLoadAs(final String nexusProperty) {
    return null != System.getProperty(nexusProperty);
  }

  public boolean isNexusLoadAs(final String nexusProperty) {
    return Boolean.getBoolean(nexusProperty);
  }

  public boolean hasFeature(final Properties properties, final String feature) {
    return properties.getProperty(NEXUS_FEATURES, "")
        .contains(feature);
  }

  protected boolean isNullNexusLicenseFile() {
    return System.getProperty("nexus.licenseFile") == null && System.getenv("NEXUS_LICENSE_FILE") == null;
  }

  protected boolean isNullJavaPrefLicensePath(final String licensePath) {
    Thread currentThread = Thread.currentThread();
    ClassLoader tccl = currentThread.getContextClassLoader();
    //Java prefs spawns a Timer-Task that inherits the current TCCL;
    //temporarily clear it so we can be GC'd if we bounce the KERNEL
    currentThread.setContextClassLoader(null);
    try {
      return userRoot().node(licensePath).get("license", null) == null;
    }
    finally {
      currentThread.setContextClassLoader(tccl);
    }
  }

  protected File getEditionMarker(final Path workDirPath, NexusEditionType edition) {
    switch (edition) {
      case PRO: {
        return workDirPath.resolve(EDITION_PRO_PATH).toFile();
      }
      default: {
        throw new IllegalStateException("Marker for OSS edition not supported!");
      }
    }
  }

  protected void createEditionMarker(final Path workDirPath, NexusEditionType edition) {
    File editionMarker = getEditionMarker(workDirPath, edition);
    try {
      if (editionMarker.createNewFile()) {
        log.debug("Created {} edition marker file: {}", edition.name(), editionMarker);
      }
    }
    catch (IOException e) {
      log.error("Failed to create {}} edition marker file: {}", edition.name(), editionMarker, e);
    }
  }

}

