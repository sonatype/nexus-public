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
 * Configuration transfer object for SSL/TLS trust store certificates.
 */
public class TrustStoreConfigurationXO
    implements ConfigurationXO
{
  public static final String TYPE_ID = "security.trust";

  @JsonProperty("certificates")
  private List<String> certificates;

  @JsonProperty("configurationTypeId")
  private String configurationTypeId = TYPE_ID;

  public TrustStoreConfigurationXO() {
    // Jackson deserialization
  }

  public TrustStoreConfigurationXO(final List<String> certificates) {
    this.certificates = certificates;
  }

  public List<String> getCertificates() {
    return certificates;
  }

  public void setCertificates(final List<String> certificates) {
    this.certificates = certificates;
  }

  public String getConfigurationTypeId() {
    return configurationTypeId;
  }
}
