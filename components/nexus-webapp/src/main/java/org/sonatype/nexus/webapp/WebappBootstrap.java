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
package org.sonatype.nexus.webapp;

import java.io.File;
import java.util.Map;
import java.util.ServiceLoader;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.sonatype.nexus.NxApplication;
import org.sonatype.nexus.bootstrap.ConfigurationBuilder;
import org.sonatype.nexus.bootstrap.ConfigurationHolder;
import org.sonatype.nexus.bootstrap.EnvironmentVariables;
import org.sonatype.nexus.guice.NexusModules.CoreModule;
import org.sonatype.nexus.log.LogManager;
import org.sonatype.nexus.util.LockFile;
import org.sonatype.nexus.util.file.DirSupport;

import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.eclipse.sisu.plexus.PlexusSpaceModule;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.ClassSpace;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

/**
 * Web application bootstrap {@link ServletContextListener}.
 *
 * @since 2.8
 */
public class WebappBootstrap
    extends GuiceServletContextListener
{
  private static final Logger log = LoggerFactory.getLogger(WebappBootstrap.class);

  private LockFile lockFile;

  private Framework framework;

  private PlexusContainer container;

  private Injector injector;

  private NxApplication application;

  private LogManager logManager;

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    log.info("Initializing");

    ServletContext context = event.getServletContext();

    try {
      // Use bootstrap configuration if it exists, else load it
      Map<String, String> properties = ConfigurationHolder.get();
      if (properties != null) {
        log.info("Using bootstrap launcher configuration");
      }
      else {
        log.info("Loading configuration for WAR deployment environment");

        // FIXME: This is what was done before, it seems completely wrong in WAR deployment since there is no bundle
        String baseDir = System.getProperty("bundleBasedir", context.getRealPath("/WEB-INF"));

        properties = new ConfigurationBuilder()
            .defaults()
            .set("bundleBasedir", new File(baseDir).getCanonicalPath())
            .properties("/nexus.properties", true)
            .properties("/nexus-test.properties", false)
            .custom(new EnvironmentVariables())
            .override(System.getProperties())
            .build();

        System.getProperties().putAll(properties);
        ConfigurationHolder.set(properties);
      }

      // Ensure required properties exist
      requireProperty(properties, "bundleBasedir");
      requireProperty(properties, "nexus-work");
      requireProperty(properties, "nexus-app");
      requireProperty(properties, "application-conf");
      requireProperty(properties, "security-xml-file");

      // lock the work directory
      File workDir = new File(properties.get("nexus-work")).getCanonicalFile();
      DirSupport.mkdir(workDir);
      lockFile = new LockFile(new File(workDir, "nexus.lock"));
      checkState(lockFile.lock(), "Nexus work directory already in use: %s", workDir);

      properties.put(Constants.FRAMEWORK_STORAGE, workDir + "/felix-cache");
      properties.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
      properties.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);
      properties.put(Constants.FRAMEWORK_BOOTDELEGATION, "*");

      framework = ServiceLoader.load(FrameworkFactory.class).iterator().next().newFramework(properties);

      framework.init();
      framework.start();

      File[] bundles = new File(properties.get("nexus-app") + "/bundles").listFiles();

      // auto-install anything in the bundles repository
      if (bundles != null && bundles.length > 0) {
        BundleContext ctx = framework.getBundleContext();
        for (File bundle : bundles) {
          try {
            ctx.installBundle("reference:" + bundle.toURI()).start();
          }
          catch (Exception e) {
            log.warn("Problem installing: {}", bundle, e);
          }
        }
      }

      // create the injector
      ClassSpace coreSpace = new URLClassSpace(Thread.currentThread().getContextClassLoader());
      injector = Guice.createInjector(
          new WireModule(
              new CoreModule(context, properties, framework),
              new PlexusSpaceModule(coreSpace, BeanScanning.INDEX)));
      log.debug("Injector: {}", injector);
      
      container = injector.getInstance(PlexusContainer.class);
      context.setAttribute(PlexusConstants.PLEXUS_KEY, container);
      injector.getInstance(Context.class).put(PlexusConstants.PLEXUS_KEY, container);
      log.debug("Container: {}", container);

      // configure guice servlet (add injector to servlet context)
      super.contextInitialized(event);

      // configure logging
      logManager = container.lookup(LogManager.class);
      log.debug("Log manager: {}", logManager);
      logManager.configure();

      // start the application
      application = container.lookup(NxApplication.class);
      log.debug("Application: {}", application);
      application.start();
    }
    catch (Exception e) {
      log.error("Failed to initialize", e);
      throw Throwables.propagate(e);
    }

    log.info("Initialized");
  }

  private static void requireProperty(final Map<String, String> properties, final String name) {
    if (!properties.containsKey(name)) {
      throw new IllegalStateException("Missing required property: " + name);
    }
  }

  @Override
  public void contextDestroyed(final ServletContextEvent event) {
    log.info("Destroying");

    ServletContext context = event.getServletContext();

    // stop application
    if (application != null) {
      try {
        application.stop();
      }
      catch (Exception e) {
        log.error("Failed to stop application", e);
      }
      application = null;
    }

    // shutdown logging
    if (logManager != null) {
      logManager.shutdown();
      logManager = null;
    }

    // unset injector from context
    super.contextDestroyed(event);
    injector = null;

    // cleanup the container
    if (container != null) {
      container.dispose();
      context.removeAttribute(PlexusConstants.PLEXUS_KEY);
      container = null;
    }

    // stop OSGi framework
    if (framework != null) {
      try {
        framework.stop();
        framework.waitForStop(0);
      }
      catch (Exception e) {
        log.error("Failed to stop OSGi framework", e);
      }
      framework = null;
    }

    // release lock
    if (lockFile != null) {
      lockFile.release();
      lockFile = null;
    }

    log.info("Destroyed");
  }

  @Override
  protected Injector getInjector() {
    checkState(injector != null, "Missing injector reference");
    return injector;
  }
}
