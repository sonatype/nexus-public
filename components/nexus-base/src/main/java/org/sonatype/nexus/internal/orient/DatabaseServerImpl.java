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
package org.sonatype.nexus.internal.orient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.log.LoggerLevelChangedEvent;
import org.sonatype.nexus.common.log.LoggersResetEvent;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.jmx.reflect.ManagedAttribute;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.orient.DatabaseServer;
import org.sonatype.nexus.orient.OrientConfigCustomizer;
import org.sonatype.nexus.orient.entity.EntityHook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.OConstants;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.config.OServerCommandConfiguration;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.orientechnologies.orient.server.config.OServerEntryConfiguration;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkListenerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkProtocolConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.config.OServerSecurityConfiguration;
import com.orientechnologies.orient.server.config.OServerStorageConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;
import com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary;
import com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetStaticContent;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.DatabaseInstanceNames.DATABASE_NAMES;

/**
 * Default {@link DatabaseServer} implementation.
 *
 * @since 3.0
 */
@Named
@Singleton
@ManagedObject
public class DatabaseServerImpl
    extends StateGuardLifecycleSupport
    implements DatabaseServer, EventAware, EventAware.Asynchronous
{
  private static final String JUL_ROOT_LOGGER = "";

  private static final String ORIENTDB_PARENT_LOGGER = "com";

  private static final String ORIENTDB_LOGGER = ORIENTDB_PARENT_LOGGER + ".orientechnologies";

  private final ApplicationDirectories applicationDirectories;

  private final File databasesDir;

  private final List<OServerHandlerConfiguration> injectedHandlers;

  private final List<OrientConfigCustomizer> configCustomizers;

  private final EntityHook entityHook;

  private final ClassLoader uberClassLoader;

  private final boolean binaryListenerEnabled;

  private final boolean httpListenerEnabled;

  private boolean dynamicPlugins;

  private final String binaryPortRange;

  private final String httpPortRange;

  private OServer orientServer;

  @Inject
  public DatabaseServerImpl(final ApplicationDirectories applicationDirectories,
                            final List<OServerHandlerConfiguration> injectedHandlers,
                            final List<OrientConfigCustomizer> configCustomizers,
                            @Named("nexus-uber") final ClassLoader uberClassLoader,
                            @Named("${nexus.orient.binaryListenerEnabled:-false}") final boolean binaryListenerEnabled,
                            @Named("${nexus.orient.httpListenerEnabled:-false}") final boolean httpListenerEnabled,
                            @Named("${nexus.orient.dynamicPlugins:-false}") final boolean dynamicPlugins,
                            @Named("${nexus.orient.binaryListener.portRange:-2424-2430}") final String binaryPortRange,
                            @Named("${nexus.orient.httpListener.portRange:-2480-2490}") final String httpPortRange,
                            final NodeAccess nodeAccess,
                            final EntityHook entityHook)
  {
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.injectedHandlers = checkNotNull(injectedHandlers);
    this.configCustomizers = checkNotNull(configCustomizers);
    this.uberClassLoader = checkNotNull(uberClassLoader);
    this.httpListenerEnabled = httpListenerEnabled;
    this.dynamicPlugins = dynamicPlugins;
    this.binaryPortRange = binaryPortRange;
    this.httpPortRange = httpPortRange;
    this.entityHook = checkNotNull(entityHook);

    if (nodeAccess.isClustered()) {
      this.binaryListenerEnabled = true; // clustered mode requires binary listener
    }
    else {
      this.binaryListenerEnabled = binaryListenerEnabled;
    }

    databasesDir = applicationDirectories.getWorkDirectory("db");

    log.info("OrientDB version: {}", OConstants.getVersion());
  }

  @ManagedAttribute
  public boolean isBinaryListenerEnabled() {
    return binaryListenerEnabled;
  }

  @ManagedAttribute
  public boolean isHttpListenerEnabled() {
    return httpListenerEnabled;
  }

  // FIXME: May need to revisit embedded configuration strategy, this is quite nasty

  @Override
  protected void doStart() throws Exception {

    // global startup
    Orient.instance().startup();

    // instance startup
    OServer server = new OServer(false)
    {
      @Override
      public Map<String, String> getAvailableStorageNames() {
        return getExistingDatabaseUrls();
      }
    };

    configureOrientMinimumLogLevel();
    server.setExtensionClassLoader(uberClassLoader);
    OServerConfiguration config = createConfiguration();
    server.startup(config);

    // remove Orient shutdown-hooks added during startup, we'll manage shutdown ourselves
    Orient.instance().removeShutdownHook();
    server.removeShutdownHook();

    // create default root user to avoid orientdb prompt on console
    server.addUser(OServerConfiguration.DEFAULT_ROOT_USER, null, "*");

    // Log global configuration
    if (log.isDebugEnabled()) {
      // dumpConfiguration() only accepts ancient stream api
      String encoding = StandardCharsets.UTF_8.name();
      ByteArrayOutputStream buff = new ByteArrayOutputStream();
      OGlobalConfiguration.dumpConfiguration(new PrintStream(buff, true, encoding));
      log.debug("Global configuration:\n{}", new String(buff.toByteArray(), encoding));
    }

    Orient.instance().addDbLifecycleListener(entityHook);

    server.activate();
    log.info("Activated");

    this.orientServer = server;
  }

  private Map<String, String> getExistingDatabaseUrls() {
    return DATABASE_NAMES.stream()
        .filter(this::databaseExists)
        .collect(toImmutableMap(identity(), this::resolveDatabaseUrl));
  }

  private boolean databaseExists(String name) {
    return new File(resolveDatabaseDir(name), "database.ocf").exists();
  }

  private String resolveDatabaseUrl(String name) {
    return "plocal:" + resolveDatabaseDir(name).getAbsolutePath();
  }

  private File resolveDatabaseDir(String name) {
    return new File(databasesDir, name);
  }

  private OServerConfiguration createConfiguration() {
    File configDir = applicationDirectories.getConfigDirectory("fabric");

    // FIXME: Unsure what this directory is used for
    File orientDir = applicationDirectories.getWorkDirectory("orient");
    System.setProperty("orient.home", orientDir.getPath());
    System.setProperty(Orient.ORIENTDB_HOME, orientDir.getPath());

    OServerConfiguration config = new OServerConfiguration();

    // FIXME: Unsure what this is used for, its apparently assigned to xml location, but forcing it here
    config.location = "DYNAMIC-CONFIGURATION";

    File securityFile = new File(configDir, "orientdb-security.json");

    config.properties = new OServerEntryConfiguration[]{
        new OServerEntryConfiguration("server.database.path", databasesDir.getPath()),
        new OServerEntryConfiguration("server.security.file", securityFile.getPath()),
        new OServerEntryConfiguration("plugin.dynamic", String.valueOf(dynamicPlugins))
    };

    config.handlers = new ArrayList<>(injectedHandlers);
    config.hooks = new ArrayList<>();

    config.network = new OServerNetworkConfiguration();
    config.network.protocols = Lists.newArrayList(
        new OServerNetworkProtocolConfiguration("binary", ONetworkProtocolBinary.class.getName()),
        new OServerNetworkProtocolConfiguration("http", ONetworkProtocolHttpDb.class.getName())
    );

    config.network.listeners = new ArrayList<>();

    OServerNetworkListenerConfiguration binaryListener = null, httpListener = null;

    if (binaryListenerEnabled) {
      binaryListener = createBinaryListener(binaryPortRange);
      config.network.listeners.add(binaryListener);
    }

    if (httpListenerEnabled) {
      httpListener = createHttpListener(httpPortRange);
      config.network.listeners.add(httpListener);
    }

    config.storages = new OServerStorageConfiguration[]{};

    config.users = new OServerUserConfiguration[]{
        new OServerUserConfiguration("admin", "admin", "*")
    };

    config.security = new OServerSecurityConfiguration();
    config.security.users = new ArrayList<>();
    config.security.resources = new ArrayList<>();

    // latest advice is to disable DB compression as it doesn't buy much,
    // also snappy has issues with use of native lib (unpacked under tmp)
    OGlobalConfiguration.STORAGE_COMPRESSION_METHOD.setValue("nothing");
    
    // ensure we don't set a file lock, which can behave badly on NFS https://issues.sonatype.org/browse/NEXUS-11289
    OGlobalConfiguration.FILE_LOCK.setValue(false);

    // disable auto removal of servers, SharedHazelcastPlugin removes gracefully shutdown nodes but for crashes and
    // especially network partitions we don't want the write quorum getting lowered and endanger consistency 
    OGlobalConfiguration.DISTRIBUTED_AUTO_REMOVE_OFFLINE_SERVERS.setValue(-1);

    // Apply customizations to server configuration
    configCustomizers.forEach((it) -> it.apply(config));

    if (binaryListener != null) {
      log.info("Binary listener enabled: {}:[{}]", binaryListener.ipAddress, binaryListener.portRange);
    }

    if (httpListener != null) {
      log.info("HTTP listener enabled: {}:[{}]", httpListener.ipAddress, httpListener.portRange);
    }

    return config;
  }

  private OServerNetworkListenerConfiguration createBinaryListener(final String binaryPortRange) {
    OServerNetworkListenerConfiguration listener;
    listener = new OServerNetworkListenerConfiguration();
    listener.ipAddress = "0.0.0.0";
    listener.portRange = binaryPortRange;
    listener.protocol = "binary";
    listener.socket = "default";
    return listener;
  }

  private OServerNetworkListenerConfiguration createHttpListener(final String httpPortRange) {
    OServerNetworkListenerConfiguration listener;
    listener = new OServerNetworkListenerConfiguration();
    listener.ipAddress = "0.0.0.0";
    listener.portRange = httpPortRange;
    listener.protocol = "http";
    listener.socket = "default";
    listener.parameters = new OServerParameterConfiguration[] {
        new OServerParameterConfiguration("network.http.charset", "UTF-8"),
        new OServerParameterConfiguration("network.http.jsonResponseError", "true")
    };

    OServerCommandConfiguration getCommand = new OServerCommandConfiguration();
    getCommand.implementation = OServerCommandGetStaticContent.class.getName();
    getCommand.pattern = "GET|www GET|studio/ GET| GET|*.htm GET|*.html GET|*.xml GET|*.jpeg GET|*.jpg GET|*.png GET|*.gif GET|*.js GET|*.css GET|*.swf GET|*.ico GET|*.txt GET|*.otf GET|*.pjs GET|*.svg GET|*.json GET|*.woff GET|*.ttf GET|*.svgz";
    getCommand.parameters = new OServerEntryConfiguration[] {
        new OServerEntryConfiguration("http.cache:*.htm *.html", "Cache-Control: no-cache, no-store, max-age=0, must-revalidate\\r\\nPragma: no-cache"),
        new OServerEntryConfiguration("http.cache:default", "Cache-Control: max-age=120")
    };
    listener.commands = new OServerCommandConfiguration[] {
        getCommand
    };
    return listener;
  }

  @Override
  protected void doStop() throws Exception {
    try {
      // instance shutdown
      orientServer.shutdown();
      orientServer = null;
    }
    finally {
      // global shutdown
      Orient.instance().shutdown();
    }

    log.info("Shutdown");
  }

  @Override
  @Guarded(by = STARTED)
  public List<String> databases() {
    return ImmutableList.copyOf(getExistingDatabaseUrls().keySet());
  }

  @Guarded(by = STARTED)
  public OServer getOrientServer() {
    return orientServer;
  }

  /**
   * Provider-shim to support injection of {@link OServer} into freeze service.
   */
  @Named
  @Singleton
  private static class OServerProvider
      implements Provider<OServer>
  {
    private final DatabaseServerImpl databaseServer;

    @Inject
    public OServerProvider(final DatabaseServerImpl databaseServer) {
      this.databaseServer = checkNotNull(databaseServer);
    }

    @Override
    public OServer get() {
      return databaseServer.getOrientServer();
    }
  }

  @Subscribe
  public void onLoggerLevelChanged(final LoggerLevelChangedEvent event) {
    String logger = event.getLogger();
    if (ORIENTDB_LOGGER.startsWith(logger) || logger.startsWith(ORIENTDB_LOGGER)
        || org.slf4j.Logger.ROOT_LOGGER_NAME.equals(logger)) {
      configureOrientMinimumLogLevel();
    }
  }

  @Subscribe
  public void onLoggersReset(final LoggersResetEvent event) {
    configureOrientMinimumLogLevel();
  }

  /**
   * Until OrientDB cleans up its logging infrastructure, this synchronizes its global minimum log level to the minimum
   * log level configured for any of its loggers.
   * 
   * @see http://www.prjhub.com/#/issues/3744
   * @see http://www.prjhub.com/#/issues/5327
   */
  private void configureOrientMinimumLogLevel() {
    int minimumLevel = getOrientMininumLogLevel();
    log.debug("Configuring OrientDB global minimum log level to {}", minimumLevel);
    OLogManager logManager = OLogManager.instance();
    logManager.setDebugEnabled(minimumLevel <= Level.FINE.intValue());
    logManager.setInfoEnabled(minimumLevel <= Level.INFO.intValue());
    logManager.setWarnEnabled(minimumLevel <= Level.WARNING.intValue());
    logManager.setErrorEnabled(minimumLevel <= Level.SEVERE.intValue());
  }

  private int getOrientMininumLogLevel() {
    int minimumLogLevel = 0;
    for (String loggerName : new String[] { ORIENTDB_LOGGER, ORIENTDB_PARENT_LOGGER, JUL_ROOT_LOGGER }) {
      Integer logLevel = getSafeLogLevel(loggerName);
      if (logLevel != null) {
        minimumLogLevel = logLevel;
        break;
      }
    }
    String orientLoggerPrefix = ORIENTDB_LOGGER + '.';
    for (Enumeration<String> en = LogManager.getLogManager().getLoggerNames(); en.hasMoreElements()
        && minimumLogLevel > Level.FINE.intValue();) {
      String loggerName = en.nextElement();
      if (!loggerName.startsWith(orientLoggerPrefix)) {
        continue;
      }
      Integer logLevel = getSafeLogLevel(loggerName);
      if (logLevel != null && logLevel < minimumLogLevel) {
        minimumLogLevel = logLevel;
      }
    }
    return minimumLogLevel;
  }

  @Nullable
  private Integer getSafeLogLevel(final String loggerName) {
    Logger logger = LogManager.getLogManager().getLogger(loggerName);
    if (logger == null) {
      return null;
    }
    Level level = logger.getLevel();
    return (level != null) ? level.intValue() : null;
  }
}
