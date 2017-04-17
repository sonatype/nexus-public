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
package org.sonatype.nexus.orient.internal.quorum;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.DatabaseServer;
import org.sonatype.nexus.orient.quorum.DatabaseQuorumService;
import org.sonatype.nexus.orient.quorum.DatabaseQuorumStatus;

import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.distributed.ODistributedConfiguration;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;

/**
 * Default DatabaseQuorumService implementation.
 *
 * @since 3.4
 */
@Named
@ManagedLifecycle(phase = STORAGE)
@Singleton
public class DatabaseQuorumServiceImpl
    extends ComponentSupport
    implements DatabaseQuorumService
{

  private final Provider<OServer> serverProvider;

  private final DatabaseServer databaseServer;

  private final NodeAccess nodeAccess;

  @Inject
  public DatabaseQuorumServiceImpl(final Provider<OServer> serverProvider,
                                   final DatabaseServer databaseServer, final NodeAccess nodeAccess) {
    this.serverProvider = checkNotNull(serverProvider);
    this.databaseServer = checkNotNull(databaseServer);
    this.nodeAccess = checkNotNull(nodeAccess);
  }

  /**
   * @return a {@link DatabaseQuorumStatus} reflecting current state
   */
  @Override
  public DatabaseQuorumStatus getQuorumStatus() {
    DatabaseQuorumStatus status = DatabaseQuorumStatus.singleNode();
    if (!nodeAccess.isClustered()) {
      return status;
    }

    ODistributedServerManager serverManager = serverProvider.get().getDistributedManager();

    for (String databaseName : databaseServer.databases()) {
      // this method gives us real-time names of nodes that are up, connected to cluster, and stable
      List<String> onlineNodes = serverManager.getOnlineNodes(databaseName);

      ODistributedConfiguration configuration = serverManager.getDatabaseConfiguration(databaseName);
      // this method gives us set of names of all observed nodes; may include nodes potentially unreachable
      Set<String> members = configuration.getAllConfiguredServers();
      // calculate write quorum against nodes we expect to see, not just what's online now
      int writeQuorum = configuration.getWriteQuorum(null, members.size(), null);

      status = new DatabaseQuorumStatus(onlineNodes, writeQuorum, databaseName);
      if (!status.isQuorumPresent()) {
        // short circuit if not present
        return status;
      }
    }

    return status;
  }
}
