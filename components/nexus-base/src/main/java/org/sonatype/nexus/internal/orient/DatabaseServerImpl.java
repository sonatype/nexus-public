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
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.jmx.reflect.ManagedAttribute;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.orient.DatabaseServer;
import org.sonatype.nexus.orient.entity.EntityHook;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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

/**
 * Default {@link DatabaseServer} implementation.
 *
 * @since 3.0
 */
@Named
@Singleton
@ManagedObject
public class DatabaseServerImpl
    extends LifecycleSupport
    implements DatabaseServer
{
  private final ApplicationDirectories applicationDirectories;

  private final List<OServerHandlerConfiguration> injectedHandlers;

  private final EntityHook entityHook;

  private final ClassLoader uberClassLoader;

  private final boolean binaryListenerEnabled;

  private final boolean httpListenerEnabled;

  private boolean dynamicPlugins;

  private OServer orientServer;

  @Inject
  public DatabaseServerImpl(final ApplicationDirectories applicationDirectories,
                            final List<OServerHandlerConfiguration> injectedHandlers,
                            @Named("nexus-uber") final ClassLoader uberClassLoader,
                            @Named("${nexus.orient.binaryListenerEnabled:-false}") final boolean binaryListenerEnabled,
                            @Named("${nexus.orient.httpListenerEnabled:-false}") final boolean httpListenerEnabled,
                            @Named("${nexus.orient.dynamicPlugins:-false}") final boolean dynamicPlugins,
                            final NodeAccess nodeAccess,
                            final EntityHook entityHook)
  {
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.injectedHandlers = checkNotNull(injectedHandlers);
    this.uberClassLoader = checkNotNull(uberClassLoader);
    this.httpListenerEnabled = httpListenerEnabled;
    this.dynamicPlugins = dynamicPlugins;
    this.entityHook = checkNotNull(entityHook);

    if (nodeAccess.isClustered()) {
      this.binaryListenerEnabled = true; // clustered mode requires binary listener
    }
    else {
      this.binaryListenerEnabled = binaryListenerEnabled;
    }

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
    OServer server = new OServer();
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
      String encoding = Charsets.UTF_8.name();
      ByteArrayOutputStream buff = new ByteArrayOutputStream();
      OGlobalConfiguration.dumpConfiguration(new PrintStream(buff, true, encoding));
      log.debug("Global configuration:\n{}", new String(buff.toByteArray(), encoding));
    }

    Orient.instance().addDbLifecycleListener(entityHook);

    server.activate();
    log.info("Activated");

    this.orientServer = server;
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

    File databaseDir = applicationDirectories.getWorkDirectory("db");
    File securityFile = new File(configDir, "orientdb-security.json");

    config.properties = new OServerEntryConfiguration[]{
        new OServerEntryConfiguration("server.database.path", databaseDir.getPath()),
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

    // HACK: Optionally enable the binary listener
    if (binaryListenerEnabled) {
      OServerNetworkListenerConfiguration listener = new OServerNetworkListenerConfiguration();
      listener.ipAddress = "0.0.0.0";
      listener.portRange = "2424-2430";
      listener.protocol = "binary";
      listener.socket = "default";
      config.network.listeners.add(listener);
      log.info("Binary listener enabled: {}:[{}]", listener.ipAddress, listener.portRange);
    }

    // HACK: Optionally enable the http listener
    if (httpListenerEnabled) {
      OServerNetworkListenerConfiguration listener = new OServerNetworkListenerConfiguration();
      listener.ipAddress = "0.0.0.0";
      listener.portRange = "2480-2490";
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

      config.network.listeners.add(listener);
      log.info("HTTP listener enabled: {}:[{}]", listener.ipAddress, listener.portRange);
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

    return config;
  }

  @Override
  protected void doStop() throws Exception {
    // instance shutdown
    orientServer.shutdown();
    orientServer = null;

    // global shutdown
    Orient.instance().shutdown();

    log.info("Shutdown");
  }

  @Override
  public List<String> databases() {
    return ImmutableList.copyOf(orientServer.getAvailableStorageNames().keySet());
  }
}
