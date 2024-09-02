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
package org.sonatype.nexus.coreui;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.validation.constraint.Hostname;
import org.sonatype.nexus.validation.constraint.PortNumber;

public class EmailConfigurationXO
{
  private boolean enabled;

  @Hostname
  @NotBlank
  private String host;

  @PortNumber
  @NotNull
  private int port;

  private String username;

  private String password;

  @Email
  @NotBlank
  private String fromAddress;

  private String subjectPrefix;

  private boolean startTlsEnabled;

  private boolean startTlsRequired;

  private boolean sslOnConnectEnabled;

  private boolean sslCheckServerIdentityEnabled;

  private boolean nexusTrustStoreEnabled;

  public EmailConfigurationXO(
      boolean enabled,
      String host,
      int port,
      String username,
      String password,
      String fromAddress,
      String subjectPrefix,
      boolean startTlsEnabled,
      boolean startTlsRequired,
      boolean sslOnConnectEnabled,
      boolean sslCheckServerIdentityEnabled,
      boolean nexusTrustStoreEnabled)
  {
    this.enabled = enabled;
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.fromAddress = fromAddress;
    this.subjectPrefix = subjectPrefix;
    this.startTlsEnabled = startTlsEnabled;
    this.startTlsRequired = startTlsRequired;
    this.sslOnConnectEnabled = sslOnConnectEnabled;
    this.sslCheckServerIdentityEnabled = sslCheckServerIdentityEnabled;
    this.nexusTrustStoreEnabled = nexusTrustStoreEnabled;
  }

  public String getHost() {
    return host;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public int getPort() {
    return port;
  }

  public void setPort(final int port) {
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

  public boolean isSslCheckServerIdentityEnabled() {
    return sslCheckServerIdentityEnabled;
  }

  public void setSslCheckServerIdentityEnabled(final boolean sslCheckServerIdentityEnabled) {
    this.sslCheckServerIdentityEnabled = sslCheckServerIdentityEnabled;
  }

  public boolean isNexusTrustStoreEnabled() {
    return nexusTrustStoreEnabled;
  }

  public void setNexusTrustStoreEnabled(final boolean nexusTrustStoreEnabled) {
    this.nexusTrustStoreEnabled = nexusTrustStoreEnabled;
  }
}
