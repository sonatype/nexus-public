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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

import org.sonatype.nexus.bootstrap.Launcher;
import org.sonatype.nexus.bootstrap.internal.ShutdownHelper;
import org.sonatype.nexus.bootstrap.internal.ShutdownHelper.ShutdownDelegate;
import org.sonatype.nexus.bootstrap.jetty.JettyServerConfiguration;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.slf4j.MDC;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Collections.singletonMap;
import static org.sonatype.nexus.bootstrap.Launcher.SYSTEM_USERID;

/**
 * {@link BundleActivator} that invokes the {@link Launcher}.
 *
 * @since 3.0
 */
public class LauncherActivator
    implements BundleActivator, ShutdownDelegate
{
  private Framework framework;

  private Launcher launcher;

  private ConnectorConfigurationTracker connectorConfigurationTracker;

  private ServiceRegistration<JettyServerConfiguration> jettyServerConfigurationRegistration;

  public void start(final BundleContext bundleContext) throws Exception {
    framework = (Framework) bundleContext.getBundle(0);
    ShutdownHelper.setDelegate(this);

    final String baseDir = checkProperty(bundleContext, "karaf.base");
    final String dataDir = checkProperty(bundleContext, "karaf.data");

    final File defaultsFile = new File(baseDir, "etc/nexus-default.properties");
    final File propertiesFile = new File(dataDir, "etc/nexus.properties");
    final File nodeNamePropertiesFile = new File(dataDir, "etc/node-name.properties");

    maybeCopyDefaults(defaultsFile, propertiesFile);

    MDC.put("userId", SYSTEM_USERID);
    launcher = new Launcher(defaultsFile, propertiesFile, nodeNamePropertiesFile);
    launcher.startAsync(
        () -> {
          connectorConfigurationTracker = new ConnectorConfigurationTracker(
              bundleContext,
              launcher.getServer()
          );
          connectorConfigurationTracker.open();
        }
    );

    final Dictionary<String, ?> properties = new Hashtable<>(singletonMap("name", "nexus"));
    jettyServerConfigurationRegistration = bundleContext.registerService(
        JettyServerConfiguration.class,
        new JettyServerConfiguration(launcher.getServer().defaultConnectors()),
        properties
    );
  }

  private static void maybeCopyDefaults(final File defaultsFile, final File propertiesFile) throws Exception {
    if (defaultsFile.exists() && !propertiesFile.exists()) {

      File parentDir = propertiesFile.getParentFile();
      if (parentDir != null && !parentDir.isDirectory()) {
        Files.createDirectories(parentDir.toPath());
      }

      // Get list of default properties, commented out
      List<String> defaultProperties = getDefaultPropertiesCommentedOut(defaultsFile.toPath());

      Files.write(
          propertiesFile.toPath(),
          defaultProperties,
          ISO_8859_1);
    }
  }

  private static List<String> getDefaultPropertiesCommentedOut(final Path defaultPropertiesPath) throws IOException {
    return Files.readAllLines(defaultPropertiesPath, ISO_8859_1)
        .stream()
        .filter(l -> !l.startsWith("##"))
        .map(l -> l.startsWith("#") || l.isEmpty() ? l : "# " + l)
        .collect(Collectors.toList());
  }

  private static String checkProperty(final BundleContext bundleContext, final String name) {
    String value = bundleContext.getProperty(name);
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing property " + name);
    }
    return value;
  }

  public void stop(final BundleContext bundleContext) throws Exception {
    try {
      if (connectorConfigurationTracker != null) {
        connectorConfigurationTracker.close();
      }
      if (jettyServerConfigurationRegistration != null) {
        jettyServerConfigurationRegistration.unregister();
      }
      if (launcher != null) {
        launcher.stop();
      }
    }
    finally {
      connectorConfigurationTracker = null;
      jettyServerConfigurationRegistration = null;
      launcher = null;
    }
  }

  public void doExit(int code) {
    ShutdownHelper.setDelegate(ShutdownHelper.JAVA); // avoid recursion
    try {
      framework.stop();
      framework.waitForStop(0);
    }
    catch (InterruptedException e) {
      // proceed to exit
    }
    catch (Throwable e) {
      System.err.println("Unexpected error while stopping");
      e.printStackTrace();
    }
    finally {
      ShutdownHelper.exit(code);
    }
  }

  public void doHalt(int code) {
    ShutdownHelper.halt(code);
  }
}
