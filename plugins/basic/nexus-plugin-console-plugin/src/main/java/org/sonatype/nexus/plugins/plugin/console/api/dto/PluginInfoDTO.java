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
package org.sonatype.nexus.plugins.plugin.console.api.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlType;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias(value = "pluginInfo")
@XmlType(name = "pluginInfo")
public class PluginInfoDTO
{
  private String name;

  private String description;

  private String version;

  private String status;

  private String failureReason;

  private String scmVersion;

  private String scmTimestamp;

  private String site;

  private List<DocumentationLinkDTO> documentation;

  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getScmVersion() {
    return scmVersion;
  }

  public void setScmVersion(String scmVersion) {
    this.scmVersion = scmVersion;
  }

  public String getScmTimestamp() {
    return scmTimestamp;
  }

  public void setScmTimestamp(String scmTimestamp) {
    this.scmTimestamp = scmTimestamp;
  }

  public List<DocumentationLinkDTO> getDocumentation() {
    return documentation;
  }

  public void setDocumentation(List<DocumentationLinkDTO> documentation) {
    this.documentation = documentation;
  }

}
