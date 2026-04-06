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
package com.sonatype.nexus.ssl.plugin.configuration.model;

import java.util.List;

import org.sonatype.nexus.configuration.model.ConfigurationXO;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration transfer object for SSL/TLS certificate list.
 *
 * @since 3.91
 */
public class CertificateListConfigurationXO
    implements ConfigurationXO
{
  public static final String TYPE_ID = "certificate_list";

  @JsonProperty("certificateConfigurationsXOs")
  private List<String> certificateConfigurationsXOs;

  @JsonProperty("configurationTypeId")
  private String configurationTypeId = TYPE_ID;

  public CertificateListConfigurationXO() {
    // Jackson deserialization
  }

  public CertificateListConfigurationXO(final List<String> certificateConfigurationsXOs) {
    this.certificateConfigurationsXOs = certificateConfigurationsXOs;
  }

  public List<String> getCertificateConfigurationsXOs() {
    return certificateConfigurationsXOs;
  }

  public void setCertificateConfigurationsXOs(final List<String> certificateConfigurationsXOs) {
    this.certificateConfigurationsXOs = certificateConfigurationsXOs;
  }

  public String getConfigurationTypeId() {
    return configurationTypeId;
  }

  public void setConfigurationTypeId(final String configurationTypeId) {
    this.configurationTypeId = configurationTypeId;
  }
}
