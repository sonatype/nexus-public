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
package org.sonatype.nexus.internal.httpclient;

import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.RedirectStrategy;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyConfiguration;

import java.util.Optional;

public class TestHttpClientConfiguration
    implements HttpClientConfiguration
{

  private ConnectionConfiguration connection;

  private ProxyConfiguration proxy;

  private RedirectStrategy redirectStrategy;

  private AuthenticationConfiguration authentication;

  private AuthenticationStrategy authenticationStrategy;

  private Boolean normalizeUri;

  private Boolean disableContentCompression;

  public TestHttpClientConfiguration() {

  }

  public TestHttpClientConfiguration(ProxyConfiguration proxy) {
    this.proxy = proxy;
  }

  @Override
  public ConnectionConfiguration getConnection() {
    return connection;
  }

  public void setConnection(ConnectionConfiguration connection) {
    this.connection = connection;
  }

  @Override
  public ProxyConfiguration getProxy() {
    return proxy;
  }

  public void setProxy(ProxyConfiguration proxy) {
    this.proxy = proxy;
  }

  @Override
  public RedirectStrategy getRedirectStrategy() {
    return redirectStrategy;
  }

  public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
    this.redirectStrategy = redirectStrategy;
  }

  @Override
  public AuthenticationConfiguration getAuthentication() {
    return authentication;
  }

  public void setAuthentication(AuthenticationConfiguration authentication) {
    this.authentication = authentication;
  }

  @Override
  public AuthenticationStrategy getAuthenticationStrategy() {
    return authenticationStrategy;
  }

  public void setAuthenticationStrategy(AuthenticationStrategy authenticationStrategy) {
    this.authenticationStrategy = authenticationStrategy;
  }

  @Override
  public Boolean getNormalizeUri() {
    return Optional.ofNullable(normalizeUri).orElse(false);
  }

  public void setNormalizeUri(Boolean normalizeUri) {
    this.normalizeUri = normalizeUri;
  }

  @Override
  public Boolean getDisableContentCompression() {
    return disableContentCompression;
  }

  public void setDisableContentCompression(Boolean disableContentCompression) {
    this.disableContentCompression = disableContentCompression;
  }

  @Override
  public TestHttpClientConfiguration copy() {
    return this;
  }
}
