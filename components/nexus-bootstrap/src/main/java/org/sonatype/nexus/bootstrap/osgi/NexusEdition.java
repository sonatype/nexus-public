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

  private static final String NEXUS_LOAD_AS_OSS_PROP_NAME = "nexus.loadAsOSS";

  private static final String NEXUS_LOAD_AS_PRO_STARTER_PROP_NAME = "nexus.loadAsProStarter";

  private static final String EDITION_PRO_PATH = "edition_pro";

  private static final String EDITION_PRO_STARTER_PATH = "edition_pro_starter";

  private static final String NEXUS_FEATURES = "nexus-features";

  private static final Logger log = LoggerFactory.getLogger(NexusEdition.class);

  public abstract NexusEditionType getEdition();

  public abstract NexusEditionFeature getEditionFeature();

  public abstract void adjustEditionProperties(Path workDirPath, Properties properties);

  protected void adjustEditionPropertiesToOSS(final Properties properties) {
    log.info("Loading OSS Edition");
    //override to load nexus-oss-edition
    properties.put(NEXUS_EDITION, NexusEditionType.OSS.editionString);
    String updatedNexusFeaturesProps = properties.getProperty(NEXUS_FEATURES)
        .replace(NexusEditionFeature.PRO_FEATURE.featureString, NexusEditionFeature.OSS_FEATURE.featureString)
        .replace(NexusEditionFeature.PRO_STARTER_FEATURE.featureString, NexusEditionFeature.OSS_FEATURE.featureString);

    properties.put(NEXUS_FEATURES, updatedNexusFeaturesProps);
  }

  protected void adjustEditionPropertiesToStarter(final Properties properties) {
    log.info("Loading Pro Starter Edition");
    //override to load nexus-pro-starter-edition
    properties.put(NEXUS_EDITION, NexusEditionType.PRO_STARTER.editionString);
    String updatedNexusFeaturesProps = properties.getProperty(NEXUS_FEATURES)
        .replace(NexusEditionFeature.PRO_FEATURE.featureString, NexusEditionFeature.PRO_STARTER_FEATURE.featureString);

    properties.put(NEXUS_FEATURES, updatedNexusFeaturesProps);
  }

  /**
   * Determine whether or not we should be booting the Pro Starter edition or not, based on the presence of a pro
   * edition marker file, license, or a System property that can be used to override the behaviour.
   */
  protected boolean shouldSwitchToProStarter(final Path workDirPath) {
    File proEditionMarker = getEditionMarker(workDirPath, NexusEditionType.PRO);
    File starterEditionMarker = getEditionMarker(workDirPath, NexusEditionType.PRO_STARTER);
    boolean switchToStarter = false;
    if (hasNexusLoadAsStarter()) {
      switchToStarter = isNexusLoadAsStarter();
    }
    if (starterEditionMarker.exists()) {
      switchToStarter = true;
    }
    if (proEditionMarker.exists()) {
      switchToStarter = false;
    }
    if (isNexusClustered()) {
      switchToStarter = false;
    }
    return switchToStarter;
  }

  /**
   * Determine whether or not we should be booting the OSS edition or not, based on the presence of a pro/pro starter
   * edition markers file, license, or a System property that can be used to override the behaviour.
   */
  protected boolean shouldSwitchToOss(final Path workDirPath) {
    File proEditionMarker = getEditionMarker(workDirPath, NexusEditionType.PRO);
    File proStarterEditionMarker = getEditionMarker(workDirPath, NexusEditionType.PRO_STARTER);
    boolean switchToOss;

    if (hasNexusLoadAsStarter() && isNexusLoadAsStarter()) {
      switchToOss = false;
    }
    else if (hasNexusLoadAsOSS()) {
      switchToOss = isNexusLoadAsOSS();
    }
    else if (proEditionMarker.exists() || proStarterEditionMarker.exists()) {
      switchToOss = false;
    }
    else if (isNexusClustered()) {
      switchToOss = false; // avoid switching the edition when clustered
    }
    else {
      switchToOss = isNullNexusLicenseFile() && isNullJavaPrefLicense();
    }

    return switchToOss;
  }

  protected File getEditionMarker(final Path workDirPath, NexusEditionType edition) {
    switch (edition) {
      case PRO: {
        return workDirPath.resolve(EDITION_PRO_PATH).toFile();
      }
      case PRO_STARTER: {
        return workDirPath.resolve(EDITION_PRO_STARTER_PATH).toFile();
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

  protected boolean hasNexusLoadAsOSS() {
    return null != System.getProperty(NEXUS_LOAD_AS_OSS_PROP_NAME);
  }

  protected boolean isNexusLoadAsOSS() {
    return Boolean.getBoolean(NEXUS_LOAD_AS_OSS_PROP_NAME);
  }

  protected boolean hasNexusLoadAsStarter() {
    return null != System.getProperty(NEXUS_LOAD_AS_PRO_STARTER_PROP_NAME);
  }

  protected boolean isNexusLoadAsStarter() {
    return Boolean.getBoolean(NEXUS_LOAD_AS_PRO_STARTER_PROP_NAME);
  }

  protected boolean isNexusClustered() {
    return Boolean.getBoolean("nexus.clustered");
  }

  protected boolean isNullNexusLicenseFile() {
    return System.getProperty("nexus.licenseFile") == null && System.getenv("NEXUS_LICENSE_FILE") == null;
  }

  protected boolean isNullJavaPrefLicense() {
    Thread currentThread = Thread.currentThread();
    ClassLoader tccl = currentThread.getContextClassLoader();
    // Java prefs spawns a Timer-Task that inherits the current TCCL;
    // temporarily clear it so we can be GC'd if we bounce the KERNEL
    currentThread.setContextClassLoader(null);
    try {
      return userRoot().node("/com/sonatype/nexus/professional").get("license", null) == null;
    }
    finally {
      currentThread.setContextClassLoader(tccl);
    }
  }
}

