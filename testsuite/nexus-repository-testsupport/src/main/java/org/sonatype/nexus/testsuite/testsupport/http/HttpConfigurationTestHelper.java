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
package org.sonatype.nexus.testsuite.testsupport.http;

import javax.inject.Provider;

import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class HttpConfigurationTestHelper
{

  private final Provider<HttpClientManager> httpClientManagerProvider;

  public HttpConfigurationTestHelper(Provider<HttpClientManager> httpClientManagerProvider) {
    checkNotNull(httpClientManagerProvider);
    this.httpClientManagerProvider = httpClientManagerProvider;
  }

  public void enableProxy(String proxyHost, int proxyPort) {
    checkProxyArgs(proxyHost, proxyPort);
    HttpClientConfiguration httpConfig = getCurrentConfig();

    enableHttpAndHttpsProxy(httpConfig, proxyHost, proxyPort);

    setConfig(httpConfig);
  }

  public void disableProxy() {
    HttpClientConfiguration httpConfig = getCurrentConfig();

    disableHttpAndHttpsProxy(httpConfig);

    setConfig(httpConfig);
  }

  public void enableProxyWithUserAgentSuffix(String proxyHost, int proxyPort, String userAgentSuffix) {
    checkProxyArgs(proxyHost, proxyPort);
    checkUserAgentSuffixArg(userAgentSuffix);

    HttpClientConfiguration httpConfig = getCurrentConfig();

    enableHttpAndHttpsProxy(httpConfig, proxyHost, proxyPort);
    setGlobalUserAgentSuffix(httpConfig, userAgentSuffix);

    setConfig(httpConfig);
  }

  public void disableProxyAndClearUserAgentSuffix() {
    HttpClientConfiguration httpConfig = getCurrentConfig();

    disableHttpAndHttpsProxy(httpConfig);
    removeGlobalUserAgentSuffix(httpConfig);

    setConfig(httpConfig);
  }

  public void setGlobalUserAgentSuffix(String userAgentSuffix) {
    checkUserAgentSuffixArg(userAgentSuffix);
    HttpClientConfiguration httpConfig = getCurrentConfig();

    setGlobalUserAgentSuffix(httpConfig, userAgentSuffix);

    setConfig(httpConfig);
  }

  private void setConfig(HttpClientConfiguration config) {
    provideHttpClientManager().setConfiguration(config);
  }

  private HttpClientConfiguration getCurrentConfig() {
    // Clear entity metadata to avoid data/serialization conflicts
    HttpClientConfiguration httpConfig = provideHttpClientManager().getConfiguration().copy();
    EntityHelper.clearMetadata(httpConfig);
    return httpConfig;
  }

  private HttpClientManager provideHttpClientManager() {
    HttpClientManager manager = httpClientManagerProvider.get();
    checkNotNull(manager);
    return manager;
  }

  private void checkUserAgentSuffixArg(String suffix) {
    checkArgument(!Strings2.isBlank(suffix), "User agent suffix must be a non-blank string.");
  }

  private void checkProxyArgs(String proxyHost, int proxyPort) {
    checkArgument(!Strings2.isBlank(proxyHost), "Proxy host must be a non-blank string.");
    checkArgument(proxyPort > 0, "Proxy port must be greater than zero.");
  }

  private void enableHttpAndHttpsProxy(HttpClientConfiguration httpConfig, String proxyHost, int proxyPort) {
    ProxyServerConfiguration server = new ProxyServerConfiguration();
    server.setHost(proxyHost);
    server.setPort(proxyPort);
    server.setEnabled(true);

    ProxyConfiguration config = new ProxyConfiguration();
    config.setHttp(server);
    config.setHttps(server);

    httpConfig.setProxy(config);
  }

  private void disableHttpAndHttpsProxy(HttpClientConfiguration httpConfig) {
    httpConfig.setProxy(null);
  }

  private void setGlobalUserAgentSuffix(HttpClientConfiguration httpConfig, String suffix) {
    ConnectionConfiguration conn = httpConfig.getConnection();
    if (conn == null) {
      conn = new ConnectionConfiguration();
    }
    conn.setUserAgentSuffix(suffix);

    httpConfig.setConnection(conn);
  }

  private void removeGlobalUserAgentSuffix(HttpClientConfiguration httpConfig) {
    ConnectionConfiguration conn = httpConfig.getConnection();
    if (conn == null) {
      conn = new ConnectionConfiguration();
    }
    conn.setUserAgentSuffix(null);

    httpConfig.setConnection(conn);
  }

}
