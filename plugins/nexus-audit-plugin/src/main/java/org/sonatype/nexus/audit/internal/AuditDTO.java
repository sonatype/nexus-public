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
package org.sonatype.nexus.audit.internal;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.sonatype.nexus.audit.AuditData;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple DTO for writing audit data to log file in JSON format
 *
 * @since 3.next
 */
@JsonInclude(Include.NON_NULL)
public class AuditDTO
{
  private String timestamp;

  private String nodeId;

  private String initiator;

  private String domain;

  private String type;

  private String context;

  private Map<String, String> attributes;

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSSZ");

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public AuditDTO() {
    //deserialization
  }

  public AuditDTO(AuditData auditData) {
    if (auditData.getTimestamp() != null) {
      this.timestamp = auditData.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime().format(DATE_FORMAT);
    }
    this.nodeId = auditData.getNodeId();
    this.initiator = auditData.getInitiator();
    this.domain = auditData.getDomain();
    this.type = auditData.getType();
    this.context = auditData.getContext();
    this.attributes = auditData.getAttributes();
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public String getInitiator() {
    return initiator;
  }

  public void setInitiator(String initiator) {
    this.initiator = initiator;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getContext() {
    return context;
  }

  public void setContext(String context) {
    this.context = context;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  @Override
  public String toString() {
    return OBJECT_MAPPER.valueToTree(this).toString();
  }
}
