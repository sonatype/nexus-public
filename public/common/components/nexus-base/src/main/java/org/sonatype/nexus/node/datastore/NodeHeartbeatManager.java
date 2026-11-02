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
import java.util.Objects;
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

  String NEXUS_STATUS_KEY = "nexus-status";

  String BUILD_REVISION_ATTRIBUTE = "buildRevision";

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

  /**
   * Determines whether active cluster members are reporting more than one distinct version/build combination,
   * i.e. a rolling upgrade is currently in progress. Returns {@code false} for single-node/OSS deployments.
   */
  default boolean hasVersionSkew() {
    return getActiveNodeHeartbeatData()
        .stream()
        .map(NodeHeartbeat::systemInfo)
        .filter(Objects::nonNull)
        .map(systemInfo -> systemInfo.get(NEXUS_STATUS_KEY))
        .filter(Objects::nonNull)
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .map(NodeHeartbeatManager::versionBuildKey)
        // a heartbeat missing either field is dropped rather than compared - concatenating nulls would fabricate
        // a "null|null"/"1.0|null"-style key that never matches a well-formed node, forcing false-positive skew
        .filter(Objects::nonNull)
        .distinct()
        .count() > 1L;
  }

  private static String versionBuildKey(final Map<?, ?> nexusStatus) {
    Object version = nexusStatus.get(VERSION_ATTRIBUTE);
    Object buildRevision = nexusStatus.get(BUILD_REVISION_ATTRIBUTE);
    return version == null || buildRevision == null ? null : version + "|" + buildRevision;
  }
}
