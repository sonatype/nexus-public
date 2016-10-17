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
import java.util.EnumSet;
import java.util.Map;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.sonatype.nexus.bootstrap.ConfigurationHolder;
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

import static java.util.prefs.Preferences.userRoot;
import static org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshBundles;
import static org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshManagedBundles;

/**
 * {@link ServletContextListener} that bootstraps an OSGi-based application.
 * 
 * @since 3.0
 */
public class BootstrapListener
    implements ServletContextListener
{

  private static final String NEXUS_LOAD_AS_OSS_PROP_NAME = "nexus.loadAsOSS";

  private static final Logger log = LoggerFactory.getLogger(BootstrapListener.class);

  private ListenerTracker listenerTracker;

  private FilterTracker filterTracker;

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    log.info("Initializing");

    ServletContext servletContext = event.getServletContext();
    
    try {
      Map<String, String> properties = ConfigurationHolder.get();
      if (properties == null) {
        throw new IllegalStateException("Missing bootstrap configuration properties");
      }

      // Ensure required properties exist
      requireProperty(properties, "karaf.base");
      requireProperty(properties, "karaf.data");

      if (shouldSwitchToOss()) {
        adjustEditionProperties(properties);  
      }

      // pass bootstrap properties to embedded servlet listener
      servletContext.setAttribute("nexus.properties", properties);

      File workDir = new File(properties.get("karaf.data")).getCanonicalFile();
      DirectoryHelper.mkdir(workDir.toPath());

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
      requireProperty(properties, "nexus-edition");
      installNexusEdition(bundleContext, properties.get("nexus-edition"));

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

  /**
   * Ensure that the oss edition is loaded, regardless of what the configuration specifies.
   * @param properties
   */
  private void adjustEditionProperties(final Map<String, String> properties) {
    log.info("Loading OSS Edition");
    //override to load nexus-oss-edition
    properties.put("nexus-edition", "nexus-oss-edition");
    properties
        .put("nexus-features", properties.get("nexus-features").replace("nexus-pro-feature", "nexus-oss-feature"));
  }

  /**
   * Determine whether or not we should be booting the OSS edition or not, based on the presence of a license or
   * a System property that can be used to override the behaviour.
   */
  private static boolean shouldSwitchToOss() {
    if (null != System.getProperty(NEXUS_LOAD_AS_OSS_PROP_NAME)) {
      return Boolean.valueOf(System.getProperty(NEXUS_LOAD_AS_OSS_PROP_NAME));
    }
    return userRoot().node("/com/sonatype/nexus/professional").get("license", null) == null;
  }

  private static void installNexusEdition(final BundleContext ctx, @Nullable final String editionName)
      throws Exception
  {
    if (editionName != null && editionName.length() > 0) {
      final ServiceTracker<?, FeaturesService> tracker = new ServiceTracker<>(ctx, FeaturesService.class, null);
      tracker.open();
      try {
        FeaturesService featuresService = tracker.waitForService(1000);
        Feature editionFeature = featuresService.getFeature(editionName);

        log.info("Installing: {}", editionFeature);

        // edition might already be installed in the cache; if so then skip installation
        if (!featuresService.isInstalled(editionFeature)) {
          // avoid auto-refreshing bundles as that could trigger unwanted restart/lifecycle events
          EnumSet<Option> options = EnumSet.of(NoAutoRefreshBundles, NoAutoRefreshManagedBundles);
          featuresService.installFeature(editionFeature.getId(), options);
        }

        log.info("Installed: {}", editionFeature);
      }
      finally {
        tracker.close();
      }
    }
  }

  private static void requireProperty(final Map<String, String> properties, final String name) {
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
