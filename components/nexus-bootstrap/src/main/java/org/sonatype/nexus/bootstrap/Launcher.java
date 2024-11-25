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
package org.sonatype.nexus.bootstrap;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Handler;

import javax.annotation.Nullable;

import org.sonatype.nexus.bootstrap.internal.LogbackContextProvider;
import org.sonatype.nexus.bootstrap.internal.ShutdownHelper;
import org.sonatype.nexus.bootstrap.internal.TemporaryDirectory;
import org.sonatype.nexus.bootstrap.jetty.JettyServer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

/**
 * Nexus bootstrap launcher.
 *
 * @since 2.1
 */
public class Launcher
{
  static {
    boolean hasJulBridge;
    try {
      // check whether we have access to the optional JUL->SLF4J logging bridge
      hasJulBridge = Handler.class.isAssignableFrom(org.slf4j.bridge.SLF4JBridgeHandler.class);
    }
    catch (Exception | LinkageError e) {
      hasJulBridge = false;
    }
    HAS_JUL_BRIDGE = hasJulBridge;
  }

  private static final boolean HAS_JUL_BRIDGE;

  private static final boolean HAS_CONSOLE = Boolean.getBoolean("karaf.startLocalConsole");

  private static final String LOGGING_OVERRIDE_PREFIX = "nexus.logging.level.";

  public static final String IGNORE_SHUTDOWN_HELPER = ShutdownHelper.class.getName() + ".ignore";

  public static final String SYSTEM_USERID = "*SYSTEM";

  private final JettyServer server;

  public Launcher(
      final File defaultsFile,
      @Nullable final File propertiesFile,
      @Nullable final File nodeNamePropertiesFile) throws Exception
  {

    configureLogging();

    ClassLoader cl = getClass().getClassLoader();

    ConfigurationBuilder builder = new ConfigurationBuilder().defaults();

    builder.properties(defaultsFile, true);
    builder.properties(propertiesFile, false);
    builder.properties(nodeNamePropertiesFile, "nexus.clustered.nodeName", false);
    builder.override(System.getProperties());

    Map<String, String> props = builder.build();
    System.getProperties().putAll(props);

    // log critical information about the runtime environment
    Logger log = LoggerFactory.getLogger(Launcher.class);
    if (log.isInfoEnabled()) {
      log.info("Java: {}, {}, {}, {}", System.getProperty("java.version"), System.getProperty("java.vm.name"),
          System.getProperty("java.vm.vendor"), System.getProperty("java.vm.version"));
      log.info("OS: {}, {}, {}", System.getProperty("os.name"), System.getProperty("os.version"),
          System.getProperty("os.arch"));
      log.info("User: {}, {}, {}", System.getProperty("user.name"), System.getProperty("user.language"),
          System.getProperty("user.home"));
      log.info("CWD: {}", System.getProperty("user.dir"));
    }

    // ensure the temporary directory is sane
    File tmpdir = TemporaryDirectory.get();
    log.info("TMP: {}", tmpdir);

    if (!"false".equalsIgnoreCase(getProperty(IGNORE_SHUTDOWN_HELPER, "false"))) {
      log.warn("ShutdownHelper requests will be ignored!");
      ShutdownHelper.setDelegate(ShutdownHelper.NOOP);
    }

    String args = props.get("nexus-args");
    if (args == null || args.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing nexus-args");
    }

    configureInitialLoggingOverrides(props);

    this.server = new JettyServer(cl, props, args.split(","));
  }

  public JettyServer getServer() {
    return server;
  }

  public void start() throws Exception {
    start(true, null);
  }

  /**
   * Starts Jetty without waiting for it to fully start up.
   *
   * @param callback optional, callback executed immediately after Jetty is fully started up.
   * @see JettyServer#start(boolean, Runnable)
   */
  public void startAsync(@Nullable final Runnable callback) throws Exception {
    start(false, callback);
  }

  private void start(final boolean waitForServer, @Nullable final Runnable callback) throws Exception {
    server.start(waitForServer, callback);
  }

  private String getProperty(final String name, final String defaultValue) {
    String value = System.getProperty(name, System.getenv(name));
    if (value == null) {
      value = defaultValue;
    }
    return value;
  }

  public void stop() throws Exception {
    server.stop();
  }

  /**
   * Customize logging of the application as necessary.
   */
  private void configureLogging() {
    if (!HAS_CONSOLE) {
      SysOutOverSLF4J.registerLoggingSystem("org.ops4j.pax.logging.slf4j");
      SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
    }

    if (HAS_JUL_BRIDGE) {
      org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger();
      org.slf4j.bridge.SLF4JBridgeHandler.install();
    }
  }

  private static class Property
  {
    private String key;

    private String value;

    private Property(final Entry<String, String> entry) {
      key = entry.getKey();
      value = entry.getValue();
    }

    private Property withKeyPrefixRemoved() {
      if (Launcher.LOGGING_OVERRIDE_PREFIX.length() < key.length()) {
        key = key.substring(Launcher.LOGGING_OVERRIDE_PREFIX.length());
      }
      return this;
    }
  }

  /**
   * Customize logging overrides presented as properties. These will be superseded by any logback overrides.
   */
  private void configureInitialLoggingOverrides(final Map<String, String> props) {
    LoggerContext loggerContext = loggerContext();
    if (props != null && loggerContext != null) {
      props.entrySet()
          .stream()
          .map(Property::new)
          .filter(p -> p.key.startsWith(LOGGING_OVERRIDE_PREFIX))
          .filter(p -> !p.value.isEmpty())
          .map(Property::withKeyPrefixRemoved)
          .forEach(p -> setLoggerLevel(loggerContext, p.key, p.value));
    }
  }

  private void setLoggerLevel(final LoggerContext loggerContext, final String logger, final String level) {
    loggerContext.getLogger(Launcher.class).debug("Initialising logger: {} = {}", logger, level);
    loggerContext.getLogger(logger).setLevel(Level.valueOf(level));
  }

  private static LoggerContext loggerContext() {
    return LogbackContextProvider.get();
  }
}
