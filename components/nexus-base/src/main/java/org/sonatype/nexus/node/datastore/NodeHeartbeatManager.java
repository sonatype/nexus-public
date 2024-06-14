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
package org.sonatype.nexus.node.datastore;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.sonatype.nexus.systemchecks.NodeSystemCheckResult;

/**
 * Create the job which will collect information about nodes and periodically write it to DB.
 */
public interface NodeHeartbeatManager
{
  String VERSION_ATTRIBUTE = "version";

  String CLUSTERED_ATTRIBUTE = "clustered";

  String NODE_ID = "nodeId";

  String CACHE_KEY = "nodes";

  /**
   * Determines if the current deployment is on sync with their database/deployment type
   *
   * @return {@code true} in case of a valid deployment, otherwise {@code false}.
   */
  boolean isValidNodeDeployment();

  /**
   * Get the {@link NodeSystemCheckResult} for the active nodes
   */
  Stream<NodeSystemCheckResult> getSystemChecks();

  /**
   * Determines if the current node is in a clustered mode
   */
  boolean isCurrentNodeClustered();

  /**
   * Triggers a write of the latest heartbeat information
   */
  void writeHeartbeat();

  /**
   * Collects and transforms system info from heartbeat table
   */
  Map<String, Map<String, Object>> getSystemInformationForNodes();

  /**
   * Collects nodeInfo from the heartbeat table
   */
  Collection<NodeHeartbeat> getActiveNodeHeartbeatData();
}
