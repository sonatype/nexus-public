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
package org.sonatype.nexus.internal.email;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.email.EmailConfiguration;

/**
 * Email configuration that only exists in memory
 *
 * @since 3.20
 */
public class MemoryEmailConfiguration
    implements Cloneable, EmailConfiguration
{
  private boolean enabled;

  private String host;

  private int port;

  private String username;

  private Secret password;

  private String fromAddress;

  private String subjectPrefix;

  private boolean startTlsEnabled;

  private boolean startTlsRequired;

  private boolean sslOnConnectEnabled;

  private boolean sslCheckServerIdentityEnabled;

  private boolean nexusTrustStoreEnabled;

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public void setHost(final String host) {
    this.host = host;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public void setPort(final int port) {
    this.port = port;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public void setUsername(final String username) {
    this.username = username;
  }

  @Override
  public Secret getPassword() {
    return password;
  }

  @Override
  public void setPassword(final Secret password) {
    this.password = password;
  }

  @Override
  public String getFromAddress() {
    return fromAddress;
  }

  @Override
  public void setFromAddress(final String fromAddress) {
    this.fromAddress = fromAddress;
  }

  @Override
  public String getSubjectPrefix() {
    return subjectPrefix;
  }

  @Override
  public void setSubjectPrefix(final String subjectPrefix) {
    this.subjectPrefix = subjectPrefix;
  }

  @Override
  public boolean isStartTlsEnabled() {
    return startTlsEnabled;
  }

  @Override
  public void setStartTlsEnabled(final boolean startTlsEnabled) {
    this.startTlsEnabled = startTlsEnabled;
  }

  @Override
  public boolean isStartTlsRequired() {
    return startTlsRequired;
  }

  @Override
  public void setStartTlsRequired(final boolean startTlsRequired) {
    this.startTlsRequired = startTlsRequired;
  }

  @Override
  public boolean isSslOnConnectEnabled() {
    return sslOnConnectEnabled;
  }

  @Override
  public void setSslOnConnectEnabled(final boolean sslOnConnectEnabled) {
    this.sslOnConnectEnabled = sslOnConnectEnabled;
  }

  @Override
  public boolean isSslCheckServerIdentityEnabled() {
    return sslCheckServerIdentityEnabled;
  }

  @Override
  public void setSslCheckServerIdentityEnabled(final boolean sslCheckServerIdentityEnabled) {
    this.sslCheckServerIdentityEnabled = sslCheckServerIdentityEnabled;
  }

  @Override
  public boolean isNexusTrustStoreEnabled() {
    return nexusTrustStoreEnabled;
  }

  @Override
  public void setNexusTrustStoreEnabled(final boolean nexusTrustStoreEnabled) {
    this.nexusTrustStoreEnabled = nexusTrustStoreEnabled;
  }

  @Override
  public MemoryEmailConfiguration copy() {
    try {
      return (MemoryEmailConfiguration)clone();
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "enabled=" + enabled +
        ", host='" + host + '\'' +
        ", port=" + port +
        ", username='" + username + '\'' +
        ", password='" + Strings2.MASK + '\'' +
        ", fromAddress='" + fromAddress + '\'' +
        ", subjectPrefix='" + subjectPrefix + '\'' +
        ", startTlsEnabled=" + startTlsEnabled +
        ", startTlsRequired=" + startTlsRequired +
        ", sslOnConnectEnabled=" + sslOnConnectEnabled +
        ", sslCheckServerIdentityEnabled=" + sslCheckServerIdentityEnabled +
        ", nexusTrustStoreEnabled=" + nexusTrustStoreEnabled +
        '}';
  }
}

