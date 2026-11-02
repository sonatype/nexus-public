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
package org.sonatype.nexus.node.upgrade;

import javax.annotation.Nullable;

import org.sonatype.nexus.node.datastore.NodeHeartbeatManager;
import org.sonatype.nexus.scheduling.TaskSupport;

/**
 * Base class for migration tasks.
 */
public abstract class MigrationTaskSupport
    extends TaskSupport
{
  protected final NodeHeartbeatManager heartbeatManager;

  /**
   * @param heartbeatManager {@code @Nullable} because the sole implementation lives in a Pro-only plugin and is
   *          absent from OSS/single-node Spring contexts; {@link #oldNodesRunning()} treats a missing manager as
   *          "no skew".
   */
  public MigrationTaskSupport(
      @Nullable final NodeHeartbeatManager heartbeatManager)
  {
    this.heartbeatManager = heartbeatManager;
  }

  /**
   * Checks if any nodes in the cluster are running different versions, Returns false in OSS/single-node
   * deployments (no HA).
   *
   * @return true if any nodes have different version/buildRevision, false otherwise
   */
  protected boolean oldNodesRunning() {
    return heartbeatManager != null && heartbeatManager.hasVersionSkew();
  }
}
