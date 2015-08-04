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
package org.sonatype.nexus.client.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.client.internal.util.Check;

/**
 * @since 2.1
 */
public class ConnectionInfo
{
  /**
   * Tri-state enum used in various validation scenarios.
   *
   * @since 2.8.0
   */
  public static enum ValidationLevel
  {
    NONE, LAX, STRICT;
  }

  /**
   * The Nexus baseUrl.
   */
  private final BaseUrl baseUrl;

  /**
   * Authentication information to use for connection.
   */
  private final AuthenticationInfo authenticationInfo;

  /**
   * Proxy information to use for connection, per-{@link Protocol} (different proxies are possible for HTTP and HTTPS).
   */
  private final Map<Protocol, ProxyInfo> proxyInfos;

  /**
   * Validation level of SSL certificate validation be performed for HTTPS connections? The {@link
   * ValidationLevel#STRICT} is the default, and will validate the HTTPS connection certificate against JVM known CAs
   * (thus, not allowing self signed certificates unless the CA for it is imported). The {@link ValidationLevel#LAX}
   * will allow self signed certificates too (those having CA chain of length 1), and {@link ValidationLevel#NONE} will
   * completely neglect the certificate material. The levels in short:
   * <ul>
   * <li>{@link ValidationLevel#STRICT} - (default) will perform certificate validation.</li>
   * <li>{@link ValidationLevel#LAX} - performs certificate validation allowing self signed certificates too.</li>
   * <li>{@link ValidationLevel#NONE} - neglects completely the certificate material.</li>
   * </ul>
   * Warning: values other than {@link ValidationLevel#STRICT} are meant for development use only!
   *
   * @since 2.8.0
   */
  private final ValidationLevel sslCertificateValidation;

  /**
   * Validation level of SSL certificate X.509 hostname matching. The {@link ValidationLevel#LAX} is the default,
   * and allows "browser like" behaviour, where sub-domain wildcards matches all sub-domains (any depth).
   * <ul>
   * <li>{@link ValidationLevel#STRICT} - will perform strict hostname validation, wildcard in X.509 hostname is
   * matched only one level deep.</li>
   * <li>{@link ValidationLevel#LAX} - (default) performs relaxed hostname validation (similar to like made by
   * browsers), where wildcard in X.509 hostname matches all sub-domains.</li>
   * <li>{@link ValidationLevel#NONE} - neglects completely the X.509 hostname.</li>
   * </ul>
   * Warning: value {@link ValidationLevel#NONE} is meant for development use only!
   *
   * @since 2.8.0
   */
  private final ValidationLevel sslCertificateHostnameValidation;

  public ConnectionInfo(final BaseUrl baseUrl, final AuthenticationInfo authenticationInfo,
                        final Map<Protocol, ProxyInfo> proxyInfos)
  {
    this(baseUrl, authenticationInfo, proxyInfos, ValidationLevel.STRICT, ValidationLevel.LAX);
  }

  public ConnectionInfo(final BaseUrl baseUrl, final AuthenticationInfo authenticationInfo,
                        final Map<Protocol, ProxyInfo> proxyInfos, final ValidationLevel sslCertificateValidation,
                        final ValidationLevel sslCertificateHostnameValidation)
  {
    this.baseUrl = Check.notNull(baseUrl, "Base URL is null!");
    this.authenticationInfo = authenticationInfo;
    HashMap<Protocol, ProxyInfo> proxies = new HashMap<Protocol, ProxyInfo>();
    if (proxyInfos != null) {
      proxies.putAll(proxyInfos);
    }
    this.proxyInfos = Collections.unmodifiableMap(proxies);
    this.sslCertificateValidation = Check.notNull(sslCertificateValidation, "sslCertificateValidation is null!");
    this.sslCertificateHostnameValidation = Check
        .notNull(sslCertificateHostnameValidation, "sslCertificateHostnameValidation is null");
  }

  public BaseUrl getBaseUrl() {
    return baseUrl;
  }

  public AuthenticationInfo getAuthenticationInfo() {
    return authenticationInfo;
  }

  public Map<Protocol, ProxyInfo> getProxyInfos() {
    return proxyInfos;
  }

  /**
   * Returns {@link ValidationLevel} of SSL certification checking made against HTTPS connections.
   *
   * @since 2.8.0
   */
  public ValidationLevel getSslCertificateValidation() { return sslCertificateValidation; }

  /**
   * Returns {@link ValidationLevel} of SSL certificate X.509 hostname checking made against HTTPS connections.c
   *
   * @since 2.8.0
   */
  public ValidationLevel getSslCertificateHostnameValidation() { return sslCertificateHostnameValidation; }

  // ==

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("(");
    sb.append("url=").append(getBaseUrl());
    if (getAuthenticationInfo() != null) {
      sb.append(",authc=").append(getAuthenticationInfo());
    }
    if (!getProxyInfos().isEmpty()) {
      sb.append(",proxy=").append(getProxyInfos());
    }
    sb.append(",sslCertificateValidation=").append(getSslCertificateValidation());
    sb.append(",sslCertificateHostnameValidation=").append(getSslCertificateHostnameValidation());
    sb.append(")");
    return sb.toString();
  }
}
