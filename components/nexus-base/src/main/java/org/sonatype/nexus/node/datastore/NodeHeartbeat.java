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

import java.time.OffsetDateTime;
import java.util.Map;

import org.sonatype.nexus.common.entity.ContinuationAware;

/**
 * Heartbeat of a node pinging the database indicating its presence.
 *
 * @since 3.37
 */
public interface NodeHeartbeat
    extends ContinuationAware
{
  /**
   * The time of the last heartbeat
   */
  OffsetDateTime heartbeatTime();

  /**
   * Host name derived for the local system
   */
  String hostname();

  /**
   * The database record ID
   */
  Integer id();

  Map<String, Object> nodeInfo();

  Map<String, Object> systemInfo();

  /**
   * The Heartbeat ID of the node, this is a non-persistent value that is distinct from the NodeAccess ID. Only useful
   * to
   * determine whether the heartbeat was created while an instance is running.
   */
  String heartbeatId();

  void setHeartbeatTime(final OffsetDateTime heartbeatTime);

  void setHostname(final String hostname);

  void setId(final Integer id);

  void setHeartbeatId(final String heartbeatId);

  void setNodeInfo(final Map<String, Object> nodeInfo);

  void setSystemInfo(final Map<String, Object> systemInfo);
}
