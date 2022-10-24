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
package org.sonatype.nexus.common.event;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base class for events which carry origin information
 */
public abstract class EventWithSource
    implements HasLocality
{
  private String remoteNodeId;

  /**
   * @return {@code true} if this is a local event
   */
  @JsonIgnore
  @Override
  public boolean isLocal() {
    return remoteNodeId == null;
  }

  /**
   * @param remoteNodeId the remote node that sent this event; {@code null} if event is local
   */
  public void setRemoteNodeId(@Nullable final String remoteNodeId) {
    this.remoteNodeId = remoteNodeId;
  }

  /**
   * @return the remote node that sent this event; {@code null} if event is local
   */
  @Nullable
  public String getRemoteNodeId() {
    return remoteNodeId;
  }
}
