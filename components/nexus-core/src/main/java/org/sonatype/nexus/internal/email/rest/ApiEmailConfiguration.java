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
package org.sonatype.nexus.internal.email.rest;

import javax.validation.constraints.NotNull;

import org.sonatype.nexus.validation.constraint.Hostname;
import org.sonatype.nexus.validation.constraint.PortNumber;

import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

public class ApiEmailConfiguration
{
  private boolean enabled;

  @Hostname
  @NotBlank
  private String host;

  @PortNumber
  @NotNull
  private Integer port;

  private String username;

  private String password;

  @Email
  @NotBlank
  @ApiModelProperty(example = "nexus@example.org")
  private String fromAddress;

  @ApiModelProperty(value = "A prefix to add to all email subjects to aid in identifying automated emails")
  private String subjectPrefix;

  @ApiModelProperty(value = "Enable STARTTLS Support for Insecure Connections")
  private boolean startTlsEnabled;

  @ApiModelProperty(value = "Require STARTTLS Support")
  private boolean startTlsRequired;

  @ApiModelProperty(value = "Enable SSL/TLS Encryption upon Connection")
  private boolean sslOnConnectEnabled;

  @ApiModelProperty(value = "Verify the server certificate when using TLS or SSL")
  private boolean sslServerIdentityCheckEnabled;

  @ApiModelProperty(value = "Use the Nexus Repository Manager's certificate truststore")
  private boolean nexusTrustStoreEnabled;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public String getHost() {
    return host;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(final Integer port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public String getFromAddress() {
    return fromAddress;
  }

  public void setFromAddress(final String fromAddress) {
    this.fromAddress = fromAddress;
  }

  public String getSubjectPrefix() {
    return subjectPrefix;
  }

  public void setSubjectPrefix(final String subjectPrefix) {
    this.subjectPrefix = subjectPrefix;
  }

  public boolean isStartTlsEnabled() {
    return startTlsEnabled;
  }

  public void setStartTlsEnabled(final boolean startTlsEnabled) {
    this.startTlsEnabled = startTlsEnabled;
  }

  public boolean isStartTlsRequired() {
    return startTlsRequired;
  }

  public void setStartTlsRequired(final boolean startTlsRequired) {
    this.startTlsRequired = startTlsRequired;
  }

  public boolean isSslOnConnectEnabled() {
    return sslOnConnectEnabled;
  }

  public void setSslOnConnectEnabled(final boolean sslOnConnectEnabled) {
    this.sslOnConnectEnabled = sslOnConnectEnabled;
  }

  public boolean isSslServerIdentityCheckEnabled() {
    return sslServerIdentityCheckEnabled;
  }

  public void setSslServerIdentityCheckEnabled(final boolean sslServerIdentityCheckEnabled) {
    this.sslServerIdentityCheckEnabled = sslServerIdentityCheckEnabled;
  }

  public boolean isNexusTrustStoreEnabled() {
    return nexusTrustStoreEnabled;
  }

  public void setNexusTrustStoreEnabled(final boolean nexusTrustStoreEnabled) {
    this.nexusTrustStoreEnabled = nexusTrustStoreEnabled;
  }
}
