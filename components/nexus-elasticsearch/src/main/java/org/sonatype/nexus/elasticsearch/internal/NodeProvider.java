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
package org.sonatype.nexus.elasticsearch.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.BindAsLifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.thread.TcclBlock;
import org.sonatype.nexus.elasticsearch.PluginLocator;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import org.elasticsearch.common.cli.Terminal;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.PluginManager;
import org.elasticsearch.plugins.PluginManager.OutputMode;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;
import static org.sonatype.nexus.common.app.ManagedLifecycleManager.isShuttingDown;

/**
 * ElasticSearch {@link Node} provider.
 *
 * @since 3.0
 */
@Named
@ManagedLifecycle(phase = STORAGE)
@Singleton
public class NodeProvider
    extends LifecycleSupport
    implements Provider<Node>
{
  private final ApplicationDirectories directories;

  private final NodeAccess nodeAccess;
  
  private final List<String> plugins;

  private final List<PluginLocator> pluginLocators;

  private Node node;

  @Inject
  public NodeProvider(final ApplicationDirectories directories,
                      final NodeAccess nodeAccess,
                      @Nullable @Named("${nexus.elasticsearch.plugins}") final String plugins,
                      @Nullable final List<PluginLocator> pluginLocators)
  {
    this.directories = checkNotNull(directories);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.plugins = plugins == null ? new ArrayList<>() : Splitter.on(",").splitToList(plugins);
    this.pluginLocators = pluginLocators == null ? Collections.emptyList() : pluginLocators;
  }

  @Override
  public synchronized Node get() {
    if (node == null) {
      try {
        Node newNode = create();

        // yellow status means that node is up (green will mean that replicas are online but we have only one node)
        log.debug("Waiting for yellow-status");
        newNode.client().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();

        this.node = newNode;
      }
      catch (Exception e) {
        // If we can not acquire an ES node reference, give up
        Throwables.throwIfUnchecked(e);
        throw new RuntimeException(e);
      }
    }
    return node;
  }

  private Node create() throws Exception {
    File file = new File(directories.getConfigDirectory("fabric"), "elasticsearch.yml");
    checkState(file.exists(), "Missing configuration: %s", file);
    log.info("Creating node with config: {}", file);

    Settings.Builder settings = Settings.builder().loadFromPath(file.toPath());

    // assign node.name to local node-id
    settings.put("node.name", nodeAccess.getId());
    settings.put("path.plugins", new File(directories.getInstallDirectory(), "plugins").getAbsolutePath());
    NodeBuilder builder = nodeBuilder().settings(settings);

    if (!plugins.isEmpty()) {
      PluginManager pluginManager = new PluginManager(new Environment(settings.build()), null, OutputMode.VERBOSE,
          new TimeValue(30000));

      for (String plugin : plugins) {
        try {
          pluginManager.downloadAndExtract(plugin, Terminal.DEFAULT, true);
        }
        catch (IOException e) {
          log.warn("Failed to install elasticsearch plugin: {}", plugin);
        }
      }
    }

    try (TcclBlock tccl = TcclBlock.begin(NodeProvider.class.getClassLoader())) {
      return new PluginUsingNode(builder.settings().build(), deployedPluginClasses()).start();
    }
  }

  private Collection<Class<? extends Plugin>> deployedPluginClasses() {
    return pluginLocators.stream().map(PluginLocator::pluginClass).collect(Collectors.toList());
  }

  @Override
  protected void doStop() {
    // elasticsearch cannot be restarted, so avoid shutting it down when bouncing the service
    if (node != null && isShuttingDown()) {
      log.debug("Shutting down");
      try {
        node.close();
      }
      finally {
        node = null;
      }
    }
  }

  /**
   * Provider implementations are not automatically exposed under additional interfaces.
   * This small module is a workaround to expose this provider as a (managed) lifecycle.
   */
  @Named
  private static class BindAsLifecycle
      extends BindAsLifecycleSupport<NodeProvider>
  {
    // empty
  }
}
