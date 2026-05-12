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
package org.sonatype.nexus.api.rest.selfhosted.security.ssrf.model;

import java.util.Set;

import javax.validation.constraints.NotNull;

import org.sonatype.nexus.validation.ssrf.SsrfProtectionConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * REST API exchange object for SSRF protection configuration.
 */
public class SsrfProtectionConfigurationXO
{
  @NotNull
  @JsonProperty(required = true)
  @ApiModelProperty(value = "Whether SSRF protection is enabled", example = "true", required = true)
  private Boolean enabled;

  @ApiModelProperty(value = "List of IP addresses allowed to bypass SSRF protection",
      example = "[\"10.0.0.50\", \"192.168.1.100\"]")
  private Set<String> allowedIPs = Set.of();

  @ApiModelProperty(value = "List of domain names allowed to bypass SSRF protection",
      example = "[\"internal.corp.com\", \"registry.local\"]")
  private Set<String> allowedDomains = Set.of();

  @SuppressWarnings("unused")
  public SsrfProtectionConfigurationXO() {
    // for deserialization
  }

  public SsrfProtectionConfigurationXO(final SsrfProtectionConfiguration config) {
    checkNotNull(config);
    this.enabled = config.enabled();
    this.allowedIPs = config.allowedIPs();
    this.allowedDomains = config.allowedDomains();
  }

  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final Boolean enabled) {
    this.enabled = enabled;
  }

  public Set<String> getAllowedIPs() {
    return allowedIPs;
  }

  public void setAllowedIPs(final Set<String> allowedIPs) {
    this.allowedIPs = allowedIPs != null ? allowedIPs : Set.of();
  }

  public Set<String> getAllowedDomains() {
    return allowedDomains;
  }

  public void setAllowedDomains(final Set<String> allowedDomains) {
    this.allowedDomains = allowedDomains != null ? allowedDomains : Set.of();
  }

  public SsrfProtectionConfiguration toConfiguration() {
    return new SsrfProtectionConfiguration(
        checkNotNull(enabled, "'enabled' is required"),
        allowedIPs,
        allowedDomains);
  }
}
