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
import java.net.URL;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.node.LocalNodeAccess;

import com.google.common.base.Throwables;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * ElasticSearch {@link Node} provider.
 *
 * @since 3.0
 */
@Named
@Singleton
public class NodeProvider
    extends ComponentSupport
    implements Provider<Node>
{
  private final ApplicationDirectories directories;

  private final LocalNodeAccess localNodeAccess;

  private Node node;

  @Inject
  public NodeProvider(final ApplicationDirectories directories,
                      final LocalNodeAccess localNodeAccess)
  {
    this.directories = checkNotNull(directories);
    this.localNodeAccess = checkNotNull(localNodeAccess);
  }

  @Override
  public synchronized Node get() {
    if (node == null) {
      try {
        Node node = create();

        // yellow status means that node is up (green will mean that replicas are online but we have only one node)
        log.debug("Waiting for yellow-status");
        node.client().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();

        this.node = node;
      }
      catch (Exception e) {
        // If we can not acquire an ES node reference, give up
        throw Throwables.propagate(e);
      }
    }
    return node;
  }

  private Node create() throws Exception {
    File file = new File(directories.getAppDirectory("etc"), "elasticsearch.yml");
    checkState(file.exists(), "Missing configuration: %s", file);
    URL url = file.toURI().toURL();
    log.info("Creating node with config: {}", url);

    ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder()
        .classLoader(NodeProvider.class.getClassLoader())
        .loadFromUrl(url);

    // assign node.name to local node-id
    settings.put("node.name", localNodeAccess.getId());

    NodeBuilder builder = nodeBuilder().settings(settings);
    return builder.node();
  }

  @PreDestroy
  public synchronized void shutdown() {
    if (node != null) {
      log.debug("Shutting down");
      try {
        node.close();
      }
      finally {
        node = null;
      }
    }
  }
}