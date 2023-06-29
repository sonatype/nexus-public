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
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.sonatype.nexus.bootstrap.internal.DirectoryHelper;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.FeaturesService.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.parseBoolean;
import static java.util.prefs.Preferences.userRoot;
import static org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshBundles;
import static org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshManagedBundles;
import static org.sonatype.nexus.common.app.FeatureFlags.*;

/**
 * {@link ServletContextListener} that bootstraps an OSGi-based application.
 *
 * @since 3.0
 */
public class BootstrapListener
    implements ServletContextListener
{
  private static final String NEXUS_LOAD_AS_OSS_PROP_NAME = "nexus.loadAsOSS";

  private static final String EDITION_PRO = "edition_pro";

  private static final Logger log = LoggerFactory.getLogger(BootstrapListener.class);

  private static final String NEXUS_EDITION = "nexus-edition";

  private static final String NEXUS_FULL_EDITION = "nexus-full-edition";

  private static final String NEXUS_FEATURES = "nexus-features";

  private static final String NEXUS_PRO_FEATURE = "nexus-pro-feature";

  private static final String NEXUS_OSS_EDITION = "nexus-oss-edition";

  private static final String NEXUS_OSS_FEATURE = "nexus-oss-feature";

  private static final String NEXUS_DB_FEATURE = "nexus-db-feature";

  private static final String NEXUS_EXCLUDE_FEATURES =  "nexus-exclude-features";

  private ListenerTracker listenerTracker;

  private FilterTracker filterTracker;

  private Path workDirPath;

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    log.info("Initializing");

    ServletContext servletContext = event.getServletContext();

    try {
      Properties properties = System.getProperties();
      if (properties == null) {
        throw new IllegalStateException("Missing bootstrap configuration properties");
      }

      // Ensure required properties exist
      requireProperty(properties, "karaf.base");
      requireProperty(properties, "karaf.data");

      File workDir = new File(properties.getProperty("karaf.data")).getCanonicalFile();
      workDirPath = workDir.toPath();
      DirectoryHelper.mkdir(workDirPath);

      if (hasProFeature(properties)) {
        if (shouldSwitchToOss(workDirPath)) {
          adjustEditionProperties(properties);
        }
        else {
          createProEditionMarker(workDirPath);
        }
      }

      selectDatastoreFeature(properties);
      selectAuthenticationFeature(properties);
      readEnvironmentVariables(properties);

      // pass bootstrap properties to embedded servlet listener
      servletContext.setAttribute("nexus.properties", properties);

      // are we already running in OSGi or should we embed OSGi?
      Bundle containingBundle = FrameworkUtil.getBundle(getClass());
      BundleContext bundleContext;
      if (containingBundle != null) {
        bundleContext = containingBundle.getBundleContext();
      }
      else {
        // when we support running in embedded mode this is where it'll go
        throw new UnsupportedOperationException("Missing OSGi container");
      }

      // bootstrap our chosen Nexus edition
      requireProperty(properties, NEXUS_EDITION);
      requireProperty(properties, NEXUS_DB_FEATURE);
      installNexusEdition(bundleContext, properties);

      // watch out for the real Nexus listener
      listenerTracker = new ListenerTracker(bundleContext, "nexus", servletContext);
      listenerTracker.open();

      // watch out for the real Nexus filter
      filterTracker = new FilterTracker(bundleContext, "nexus");
      filterTracker.open();

      listenerTracker.waitForService(0);
      filterTracker.waitForService(0);
    }
    catch (Exception e) {
      log.error("Failed to initialize", e);
      throw e instanceof RuntimeException ? ((RuntimeException) e) : new RuntimeException(e);
    }

    log.info("Initialized");
  }

  private boolean hasProFeature(final Properties properties) {
    return properties.getProperty(NEXUS_FEATURES, "").contains(NEXUS_PRO_FEATURE);
  }

  /**
   * Ensure that the oss edition is loaded, regardless of what the configuration specifies.
   * @param properties
   */
  private void adjustEditionProperties(final Properties properties) {
    log.info("Loading OSS Edition");
    //override to load nexus-oss-edition
    properties.put(NEXUS_EDITION, NEXUS_OSS_EDITION);
    properties
        .put(NEXUS_FEATURES, properties.getProperty(NEXUS_FEATURES).replace(NEXUS_PRO_FEATURE, NEXUS_OSS_FEATURE));
  }

  /**
   * Determine whether or not we should be booting the OSS edition or not, based on the presence of a pro edition marker
   * file, license, or a System property that can be used to override the behaviour.
   */
  boolean shouldSwitchToOss(final Path workDirPath) {
    File proEditionMarker = getProEditionMarker(workDirPath);
    boolean switchToOss;

    if (hasNexusLoadAsOSS()) {
      switchToOss = isNexusLoadAsOSS();
    }
    else if (proEditionMarker.exists()) {
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

  boolean hasNexusLoadAsOSS() {
    return null != System.getProperty(NEXUS_LOAD_AS_OSS_PROP_NAME);
  }

  boolean isNexusLoadAsOSS() {
    return Boolean.getBoolean(NEXUS_LOAD_AS_OSS_PROP_NAME);
  }

  File getProEditionMarker(final Path workDirPath) {
    return workDirPath.resolve(EDITION_PRO).toFile();
  }

  private void createProEditionMarker(final Path workDirPath) {
    File proEditionMarker = getProEditionMarker(workDirPath);
    try {
      if (proEditionMarker.createNewFile()) {
        log.debug("Created pro edition marker file: {}", proEditionMarker);
      }
    }
    catch (IOException e) {
      log.error("Failed to create pro edition marker file: {}", proEditionMarker, e);
    }
  }

  boolean isNexusClustered() {
    return Boolean.getBoolean("nexus.clustered");
  }

  boolean isNullNexusLicenseFile() {
    return System.getProperty("nexus.licenseFile") == null && System.getenv("NEXUS_LICENSE_FILE") == null;
  }

  boolean isNullJavaPrefLicense() {
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

  private void readEnvironmentVariables(final Properties properties) {

    if (properties.getProperty(CHANGE_REPO_BLOBSTORE_TASK_ENABLED) == null) {
      properties.setProperty(CHANGE_REPO_BLOBSTORE_TASK_ENABLED,
         Boolean.toString(parseBoolean(System.getenv("CHANGE_REPO_BLOBSTORE_TASK_ENABLED"))));
    }
  }

  private void selectDatastoreFeature(final Properties properties) {
    // datastore developer mode includes datastore user mode
    if (parseBoolean(properties.getProperty(DATASTORE_DEVELOPER, "false"))) {
      properties.setProperty(DATASTORE_ENABLED, "true");
    }

    // table search should only be turned on via clustered flag
    if (parseBoolean(properties.getProperty(DATASTORE_CLUSTERED_ENABLED,
        Optional.ofNullable(System.getenv("DATASTORE_CLUSTERED_ENABLED")).orElse("false")))) {
      // As we read the ENV variable we need to enable feature flagged classes using in-memory properties hashtable
      properties.setProperty(DATASTORE_CLUSTERED_ENABLED, "true");
      properties.setProperty(DATASTORE_ENABLED, "true");
      properties.setProperty(DATASTORE_TABLE_SEARCH, "true");
      properties.setProperty(ELASTIC_SEARCH_ENABLED, "false");
      properties.setProperty(SQL_DISTRIBUTED_CACHE, "true");

      // JWT and Blobstore Metrics should also be enabled for clustered
      properties.setProperty(JWT_ENABLED, "true");
      properties.setProperty(DATASTORE_BLOBSTORE_METRICS, "true");
    }

    // datastore search mode enables datastore user mode
    // disables elastic search mode
    // table search should only be turned on via clustered flag
    if (parseBoolean(properties.getProperty(DATASTORE_TABLE_SEARCH, "false"))) {
      properties.setProperty(DATASTORE_ENABLED, "true");
      properties.setProperty(ELASTIC_SEARCH_ENABLED, "false");
    }

    // elastic search disables datastore search mode
    if (parseBoolean(properties.getProperty(ELASTIC_SEARCH_ENABLED, "false"))) {
      properties.setProperty(DATASTORE_TABLE_SEARCH, "false");
    }

    if (parseBoolean(properties.getProperty(DATASTORE_ENABLED, "false"))) {
      // datastore mode disables orient
      properties.setProperty(ORIENT_ENABLED, "false");

      // datastore mode, but not developer mode
      if (!parseBoolean(properties.getProperty(DATASTORE_DEVELOPER, "false"))) {
        // exclude unfinished format features
        properties.setProperty(NEXUS_EXCLUDE_FEATURES,
                properties.getProperty(NEXUS_EXCLUDE_FEATURES, ""));
      }
    }

    selectDbFeature(properties);
  }

  private static void selectDbFeature(final Properties properties) {
    if (parseBoolean(properties.getProperty(ORIENT_ENABLED, "true"))) {
      properties.setProperty(NEXUS_DB_FEATURE, "nexus-orient");
      properties.setProperty(ORIENT_ENABLED, "true");
    }
    else {
      properties.setProperty(NEXUS_DB_FEATURE, "nexus-datastore-mybatis");
      properties.setProperty(DATASTORE_ENABLED, "true");
      properties.setProperty("nexus.quartz.jobstore.jdbc", "true");
    }
  }

  private static void selectAuthenticationFeature(final Properties properties) {
    if (parseBoolean(properties.getProperty(SESSION_ENABLED, "true"))) {
      properties.setProperty(SESSION_ENABLED, "true");
    }
    if (parseBoolean(properties.getProperty(JWT_ENABLED, "false"))) {
      properties.setProperty(SESSION_ENABLED, "false");
    }
  }

  private static void installNexusEdition(final BundleContext ctx, final Properties properties)
      throws Exception
  {
    String editionName = properties.getProperty(NEXUS_EDITION);
    if (editionName != null && editionName.length() > 0) {
      final ServiceTracker<?, FeaturesService> tracker = new ServiceTracker<>(ctx, FeaturesService.class, null);
      tracker.open();
      try {
        FeaturesService featuresService = tracker.waitForService(1000);
        Feature editionFeature = featuresService.getFeature(editionName);
        properties.put(NEXUS_FULL_EDITION, editionFeature.toString());

        Feature dbFeature = featuresService.getFeature(properties.getProperty(NEXUS_DB_FEATURE));

        log.info("Installing: {} ({})", editionFeature, dbFeature);

        Set<String> featureIds = new LinkedHashSet<>();
        if (!featuresService.isInstalled(editionFeature)) {
          featureIds.add(editionFeature.getId());
        }
        if (!featuresService.isInstalled(dbFeature)) {
          featureIds.add(dbFeature.getId());
        }

        // edition might already be installed in the cache; if so then skip installation
        if (!featureIds.isEmpty()) {
          // avoid auto-refreshing bundles as that could trigger unwanted restart/lifecycle events
          EnumSet<Option> options = EnumSet.of(NoAutoRefreshBundles, NoAutoRefreshManagedBundles);
          featuresService.installFeatures(featureIds, options);
        }

        log.info("Installed: {} ({})", editionFeature, dbFeature);
      }
      finally {
        tracker.close();
      }
    }
  }

  private static void requireProperty(final Properties properties, final String name) {
    if (!properties.containsKey(name)) {
      throw new IllegalStateException("Missing required property: " + name);
    }
  }

  @Override
  public void contextDestroyed(final ServletContextEvent event) {
    log.info("Destroying");

    if (filterTracker != null) {
      filterTracker.close();
      filterTracker = null;
    }

    if (listenerTracker != null) {
      listenerTracker.close();
      listenerTracker = null;
    }

    log.info("Destroyed");
  }
}
