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

import java.util.Set;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.sonatype.nexus.httpclient.config.NonProxyHosts;
import org.sonatype.nexus.validation.constraint.Hostname;
import org.sonatype.nexus.validation.constraint.PortNumber;

/**
 * HTTP System Settings exchange object.
 *
 * @since 3.0
 */
public class HttpSettingsXO
{
  private String userAgentSuffix;

  /**
   * Timeout seconds.
   */
  @Min(1L)
  @Max(3600L)
  private Integer timeout;

  @Min(0L)
  @Max(10L)
  private Integer retries;

  private Boolean httpEnabled;

  @Hostname
  private String httpHost;

  @PortNumber
  private Integer httpPort;

  private Boolean httpAuthEnabled;

  private String httpAuthUsername;

  private String httpAuthPassword;

  private String httpAuthNtlmHost;

  private String httpAuthNtlmDomain;

  private Boolean httpsEnabled;

  @Hostname
  private String httpsHost;

  @PortNumber
  private Integer httpsPort;

  private Boolean httpsAuthEnabled;

  private String httpsAuthUsername;

  private String httpsAuthPassword;

  private String httpsAuthNtlmHost;

  private String httpsAuthNtlmDomain;

  @NonProxyHosts
  private Set<String> nonProxyHosts;

  public String getUserAgentSuffix() {
    return userAgentSuffix;
  }

  public void setUserAgentSuffix(String userAgentSuffix) {
    this.userAgentSuffix = userAgentSuffix;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public Integer getRetries() {
    return retries;
  }

  public void setRetries(Integer retries) {
    this.retries = retries;
  }

  public Boolean getHttpEnabled() {
    return httpEnabled;
  }

  public void setHttpEnabled(Boolean httpEnabled) {
    this.httpEnabled = httpEnabled;
  }

  public String getHttpHost() {
    return httpHost;
  }

  public void setHttpHost(String httpHost) {
    this.httpHost = httpHost;
  }

  public Integer getHttpPort() {
    return httpPort;
  }

  public void setHttpPort(Integer httpPort) {
    this.httpPort = httpPort;
  }

  public Boolean getHttpAuthEnabled() {
    return httpAuthEnabled;
  }

  public void setHttpAuthEnabled(Boolean httpAuthEnabled) {
    this.httpAuthEnabled = httpAuthEnabled;
  }

  public String getHttpAuthUsername() {
    return httpAuthUsername;
  }

  public void setHttpAuthUsername(String httpAuthUsername) {
    this.httpAuthUsername = httpAuthUsername;
  }

  public String getHttpAuthPassword() {
    return httpAuthPassword;
  }

  public void setHttpAuthPassword(String httpAuthPassword) {
    this.httpAuthPassword = httpAuthPassword;
  }

  public String getHttpAuthNtlmHost() {
    return httpAuthNtlmHost;
  }

  public void setHttpAuthNtlmHost(String httpAuthNtlmHost) {
    this.httpAuthNtlmHost = httpAuthNtlmHost;
  }

  public String getHttpAuthNtlmDomain() {
    return httpAuthNtlmDomain;
  }

  public void setHttpAuthNtlmDomain(String httpAuthNtlmDomain) {
    this.httpAuthNtlmDomain = httpAuthNtlmDomain;
  }

  public Boolean getHttpsEnabled() {
    return httpsEnabled;
  }

  public void setHttpsEnabled(Boolean httpsEnabled) {
    this.httpsEnabled = httpsEnabled;
  }

  public String getHttpsHost() {
    return httpsHost;
  }

  public void setHttpsHost(String httpsHost) {
    this.httpsHost = httpsHost;
  }

  public Integer getHttpsPort() {
    return httpsPort;
  }

  public void setHttpsPort(Integer httpsPort) {
    this.httpsPort = httpsPort;
  }

  public Boolean getHttpsAuthEnabled() {
    return httpsAuthEnabled;
  }

  public void setHttpsAuthEnabled(Boolean httpsAuthEnabled) {
    this.httpsAuthEnabled = httpsAuthEnabled;
  }

  public String getHttpsAuthUsername() {
    return httpsAuthUsername;
  }

  public void setHttpsAuthUsername(String httpsAuthUsername) {
    this.httpsAuthUsername = httpsAuthUsername;
  }

  public String getHttpsAuthPassword() {
    return httpsAuthPassword;
  }

  public void setHttpsAuthPassword(String httpsAuthPassword) {
    this.httpsAuthPassword = httpsAuthPassword;
  }

  public String getHttpsAuthNtlmHost() {
    return httpsAuthNtlmHost;
  }

  public void setHttpsAuthNtlmHost(String httpsAuthNtlmHost) {
    this.httpsAuthNtlmHost = httpsAuthNtlmHost;
  }

  public String getHttpsAuthNtlmDomain() {
    return httpsAuthNtlmDomain;
  }

  public void setHttpsAuthNtlmDomain(String httpsAuthNtlmDomain) {
    this.httpsAuthNtlmDomain = httpsAuthNtlmDomain;
  }

  public Set<String> getNonProxyHosts() {
    return nonProxyHosts;
  }

  public void setNonProxyHosts(Set<String> nonProxyHosts) {
    this.nonProxyHosts = nonProxyHosts;
  }

  @Override
  public String toString() {
    return "HttpSettingsXO{" +
        "userAgentSuffix='" + userAgentSuffix + '\'' +
        ", timeout=" + timeout +
        ", retries=" + retries +
        ", httpEnabled=" + httpEnabled +
        ", httpHost='" + httpHost + '\'' +
        ", httpPort=" + httpPort +
        ", httpAuthEnabled=" + httpAuthEnabled +
        ", httpAuthUsername='" + httpAuthUsername + '\'' +
        ", httpAuthPassword='" + httpAuthPassword + '\'' +
        ", httpAuthNtlmHost='" + httpAuthNtlmHost + '\'' +
        ", httpAuthNtlmDomain='" + httpAuthNtlmDomain + '\'' +
        ", httpsEnabled=" + httpsEnabled +
        ", httpsHost='" + httpsHost + '\'' +
        ", httpsPort=" + httpsPort +
        ", httpsAuthEnabled=" + httpsAuthEnabled +
        ", httpsAuthUsername='" + httpsAuthUsername + '\'' +
        ", httpsAuthPassword='" + httpsAuthPassword + '\'' +
        ", httpsAuthNtlmHost='" + httpsAuthNtlmHost + '\'' +
        ", httpsAuthNtlmDomain='" + httpsAuthNtlmDomain + '\'' +
        ", nonProxyHosts=" + nonProxyHosts +
        '}';
  }
}
