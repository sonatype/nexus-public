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
package org.sonatype.nexus.analytics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.common.entity.Entity;

import com.google.common.base.Throwables;

/**
 * Analytics event data.
 *
 * @since 3.0
 */
public class EventData
  extends Entity
  implements Cloneable, Serializable
{
  private static final long serialVersionUID = 1L;

  /**
   * Event type.
   */
  private String type;

  /**
   * Event timestamp in milliseconds.
   */
  private Long timestamp;

  /**
   * Event sequence (rolling) to distinguish events which have the same timestamp.
   */
  private Long sequence;

  /**
   * Event duration in nanoseconds.
   */
  private Long duration;

  /**
   * Event user-id.
   */
  private String userId;

  /**
   * Event session-id.
   */
  private String sessionId;

  /**
   * Event attributes.
   */
  private Map<String, String> attributes = new HashMap<>();

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Long timestamp) {
    this.timestamp = timestamp;
  }

  public Long getSequence() {
    return sequence;
  }

  public void setSequence(final Long sequence) {
    this.sequence = sequence;
  }

  public Long getDuration() {
    return duration;
  }

  public void setDuration(final Long duration) {
    this.duration = duration;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(final String userId) {
    this.userId = userId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(final String sessionId) {
    this.sessionId = sessionId;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  /**
   * Returns a deeply cloned copy.
   */
  public EventData copy() {
    try {
      EventData copy = (EventData) clone();
      copy.attributes = new HashMap<>(this.attributes);
      return copy;
    }
    catch (CloneNotSupportedException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "type='" + type + '\'' +
        ", timestamp=" + timestamp +
        ", sequence=" + sequence +
        ", duration=" + duration +
        ", userId='" + userId + '\'' +
        ", sessionId='" + sessionId + '\'' +
        ", attributes=" + attributes +
        '}';
  }
}
