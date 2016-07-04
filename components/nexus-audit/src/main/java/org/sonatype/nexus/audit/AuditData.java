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

package org.sonatype.nexus.audit;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.node.NodeAccess;

import com.google.common.base.Throwables;

/**
 * Audit data.
 *
 * @since 3.1
 */
public class AuditData
    extends Entity
    implements Cloneable, Serializable
{
  private static final long serialVersionUID = 1L;

  /**
   * The domain of the audit data.
   *
   * ex: 'security.user'
   *
   * Domain should use dot-notation to encode hierarchy structure.
   */
  private String domain;

  /**
   * The type of audit data relevant for domain.
   *
   * ex: 'create'
   */
  private String type;

  /**
   * Context of the audit data for domain and type.
   *
   * ex: 'jdillon'
   */
  private String context;

  /**
   * When the change occurred.
   */
  private Date timestamp;

  /**
   * The node-id where the change occurred.
   *
   * @see NodeAccess#getId()
   */
  private String nodeId;

  /**
   * Who initiated the change.
   *
   * ex: 'admin/192.168.0.57'
   */
  private String initiator;

  /**
   * Extensible attributes for the change.
   */
  private Map<String, String> attributes = new LinkedHashMap<>();

  public String getDomain() {
    return domain;
  }

  public void setDomain(final String domain) {
    this.domain = domain;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getContext() {
    return context;
  }

  public void setContext(final String context) {
    this.context = context;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Date timestamp) {
    this.timestamp = timestamp;
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(final String nodeId) {
    this.nodeId = nodeId;
  }

  public String getInitiator() {
    return initiator;
  }

  public void setInitiator(final String initiator) {
    this.initiator = initiator;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  /**
   * Returns a deeply cloned copy.
   */
  public AuditData copy() {
    try {
      AuditData copy = (AuditData) clone();
      copy.attributes = new LinkedHashMap<>(this.attributes);
      return copy;
    }
    catch (CloneNotSupportedException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Returns deeply cloned copy detached from {@link EntityMetadata}.
   */
  public AuditData detach() {
    AuditData copy = copy();
    copy.setEntityMetadata(null);
    return copy;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "domain='" + domain + '\'' +
        ", type='" + type + '\'' +
        ", context='" + context + '\'' +
        ", timestamp=" + timestamp +
        ", nodeId='" + nodeId + '\'' +
        ", initiator='" + initiator + '\'' +
        ", attributes=" + attributes +
        '}';
  }
}
