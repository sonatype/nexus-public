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
package org.sonatype.nexus.internal.commands;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.orient.DatabaseServer;
import org.sonatype.nexus.orient.quorum.DatabaseQuorumService;
import org.sonatype.nexus.orient.quorum.DatabaseQuorumStatus;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Console command that displays cluster details that would be used to calculate the write quorum setting. Also allows
 * the cluster to be reset to a single node. After using the `-r` switch, the only node left in the cluster is the node
 * where the command is executed.
 *
 * @since 3.4
 */
@Named
@Command(name = "quorum", scope = "nexus", description = "Check or set database read and write quorum values")
public class DatabaseQuorumAction
    implements Action
{

  private final DatabaseQuorumService databaseQuorumService;

  private final DatabaseServer databaseServer;

  @Option(name = "-r", aliases = {"--reset"},
      description = "specify when updating quorum value")
  private boolean reset = false;

  @Inject
  public DatabaseQuorumAction(final DatabaseQuorumService databaseQuorumService, final DatabaseServer databaseServer) {
    this.databaseQuorumService = checkNotNull(databaseQuorumService);
    this.databaseServer = checkNotNull(databaseServer);
  }

  @Override
  public Object execute() throws Exception {
    if (reset) {
      System.out.format("resetting write quorum on databases\n"); // NOSONAR
      databaseQuorumService.resetWriteQuorum();
    }
    else {
      databaseServer.databases().forEach(this::logQuorumStatus);
    }

    return null;
  }

  private void logQuorumStatus(String db) {
    DatabaseQuorumStatus status = databaseQuorumService.getQuorumStatus(db);
    System.out.format("write quorum value: %d, quorum present: %b, on database: %s%n", // NOSONAR
        status.getMinimumForQuorum(), status.isQuorumPresent(), db);
    System.out.format("\tavailable members: %s%n", // NOSONAR
        String.join(",", status.getMembers()));
    System.out.format("\tall configured members: %s%n", // NOSONAR
        String.join(",", status.getAllConfiguredServers()));
  }

}
