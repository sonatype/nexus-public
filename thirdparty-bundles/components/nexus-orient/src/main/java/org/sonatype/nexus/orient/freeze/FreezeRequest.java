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
package org.sonatype.nexus.orient.freeze;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a request to "freeze" the orient databases, e.g. make them read-only.
 */
public class FreezeRequest
{
  private final InitiatorType initiatorType;

  private final String initiatorId;

  private final DateTime timestamp;

  private String nodeId;

  /**
   * Creates a new instance with a timestamp of {@link DateTime#now()}.
   * @param initiatorType
   * @param initiatorId
   */
  public FreezeRequest(final InitiatorType initiatorType, final String initiatorId) {
    // make sure timestamp is UTC
    this(initiatorType, initiatorId, DateTime.now(DateTimeZone.UTC));
  }

  /**
   * Constructor used by Jackson.
   *
   * @param initiatorType
   * @param initiatorId
   * @param timestamp
   */
  @JsonCreator
  public FreezeRequest(@JsonProperty("initiatorType") final InitiatorType initiatorType,
                @JsonProperty("initiatorId") final String initiatorId,
                @JsonProperty("timestamp") final DateTime timestamp) {
    this.initiatorType = checkNotNull(initiatorType);
    this.initiatorId = checkNotNull(initiatorId);
    this.timestamp = checkNotNull(timestamp);
  }

  public InitiatorType getInitiatorType() {
    return initiatorType;
  }

  public String getInitiatorId() {
    return initiatorId;
  }

  public DateTime getTimestamp() {
    return timestamp;
  }

  @Nullable
  public String getNodeId() {
    return nodeId;
  }

  @JsonProperty("nodeId")
  public FreezeRequest setNodeId(String nodeId) {
    this.nodeId = nodeId;
    return this;
  }

  @Override
  public String toString() {
    return "FreezeRequest{" +
        "initiatorType=" + initiatorType +
        ", initiatorId='" + initiatorId + '\'' +
        ", timestamp=" + timestamp +
        ", nodeId='" + nodeId + '\'' +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FreezeRequest request = (FreezeRequest) o;

    if (initiatorType != request.initiatorType) {
      return false;
    }
    if (!initiatorId.equals(request.initiatorId)) {
      return false;
    }
    if (!timestamp.equals(request.timestamp)) {
      return false;
    }
    return nodeId != null ? nodeId.equals(request.nodeId) : request.nodeId == null;
  }

  @Override
  public int hashCode() {
    int result = initiatorType.hashCode();
    result = 31 * result + initiatorId.hashCode();
    result = 31 * result + timestamp.hashCode();
    result = 31 * result + (nodeId != null ? nodeId.hashCode() : 0);
    return result;
  }

  /**
   * Enumeration representing types of database freeze initiators.
   */
  public enum InitiatorType
  {
    /**
     * Internally caused.
     */
    SYSTEM,
    /**
     * Requested by a human being via the UI.
     */
    USER_INITIATED
  }
}
