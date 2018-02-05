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
package org.sonatype.nexus.internal.provisioning

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.goodies.common.Time
import org.sonatype.nexus.CoreApi
import org.sonatype.nexus.capability.CapabilityReference
import org.sonatype.nexus.capability.CapabilityRegistry
import org.sonatype.nexus.httpclient.HttpClientManager
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration
import org.sonatype.nexus.httpclient.config.ProxyConfiguration
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration
import org.sonatype.nexus.internal.app.BaseUrlCapabilityDescriptor
import org.sonatype.nexus.internal.capability.DefaultCapabilityReference

import static com.google.common.base.Preconditions.checkArgument
import static com.google.common.base.Preconditions.checkNotNull

/**
 * @since 3.0
 */
@Named
@Singleton
class CoreApiImpl
    extends ComponentSupport
    implements CoreApi
{
  @Inject
  CapabilityRegistry capabilityRegistry

  @Inject
  HttpClientManager httpClientManager

  @Override
  void baseUrl(final String url) {
    checkNotNull(url)
    DefaultCapabilityReference existing = capabilityRegistry.all.find { CapabilityReference capabilityReference ->
      capabilityReference.context().descriptor().type() == BaseUrlCapabilityDescriptor.TYPE
    }
    if (existing) {
      log.info('BaseUrl capability updated to: {}',
          capabilityRegistry.update(existing.id(), existing.active, existing.notes(), [url: url]).toString()
      )
    }
    else {
      log.info('BaseUrl capability created as: {}', capabilityRegistry.
          add(BaseUrlCapabilityDescriptor.TYPE, true, 'configured through api', [url: url]).toString()
      )
    }
  }

  @Override
  void removeBaseUrl() {
    DefaultCapabilityReference existing = capabilityRegistry.all.find { CapabilityReference capabilityReference ->
      capabilityReference.context().descriptor().type() == BaseUrlCapabilityDescriptor.TYPE
    }
    if (existing) {
      log.info('Deleting BaseUrl capability')
      capabilityRegistry.remove(existing.id())
    }
    else {
      log.info('No BaseUrl capability configured to remove')
    }
  }

  @Override
  void userAgentCustomization(String userAgentSuffix) {
    checkNotNull(userAgentSuffix)
    HttpClientConfiguration configuration = detachedConfiguration()
    connection(configuration).userAgentSuffix = userAgentSuffix
    httpClientManager.configuration = configuration
  }

  @Override
  void connectionTimeout(int timeout) {
    checkArgument((timeout >= 1 && timeout <= 3600), 'Value must be between 1 and 3600')
    HttpClientConfiguration configuration = detachedConfiguration()
    connection(configuration).timeout = Time.seconds(timeout)
    httpClientManager.configuration = configuration
  }

  @Override
  void connectionRetryAttempts(int retries) {
    checkArgument((retries >= 0 && retries <= 10), 'Value must be between 0 and 10')
    HttpClientConfiguration configuration = detachedConfiguration()
    connection(configuration).maximumRetries = retries
    httpClientManager.configuration = configuration
  }

  @Override
  void httpProxy(final String host, final int port) {
    checkNotNull(host)
    checkPort(port)
    configureProxy(host, port)
  }

  @Override
  void httpProxyWithBasicAuth(final String host, final int port, final String username, final String password) {
    checkNotNull(host)
    checkPort(port)
    checkNotNull(username)
    checkNotNull(password)
    configureProxy(host, port, false, new UsernameAuthenticationConfiguration(username: username, password: password))
  }

  @Override
  void httpProxyWithNTLMAuth(final String host, final int port, final String username, final String password,
                             final String ntlmHost, final String domain)
  {
    checkNotNull(host)
    checkPort(port)
    checkNotNull(username)
    checkNotNull(password)
    checkNotNull(ntlmHost)
    checkNotNull(domain)
    configureProxy(host, port, false, new NtlmAuthenticationConfiguration(
        username: username,
        password: password,
        host: ntlmHost,
        domain: domain)
    )
  }

  @Override
  void removeHTTPProxy() {
    HttpClientConfiguration configuration = detachedConfiguration()
    if(configuration.proxy != null) {
      configuration.proxy = null
      httpClientManager.configuration = configuration
    }
  }

  @Override
  void httpsProxy(final String host, final int port) {
    checkNotNull(host)
    checkPort(port)
    configureProxy(host, port, true)
  }

  @Override
  void httpsProxyWithBasicAuth(final String host, final int port, final String username, final String password) {
    checkNotNull(host)
    checkPort(port)
    checkNotNull(username)
    checkNotNull(password)
    configureProxy(host, port, true, new UsernameAuthenticationConfiguration(username: username, password: password))
  }

  @Override
  void httpsProxyWithNTLMAuth(final String host, final int port, final String username, final String password,
                              final String ntlmHost, final String domain)
  {
    checkNotNull(host)
    checkPort(port)
    checkNotNull(username)
    checkNotNull(password)
    checkNotNull(ntlmHost)
    checkNotNull(domain)
    configureProxy(host, port, true, new NtlmAuthenticationConfiguration(
        username: username,
        password: password,
        host: ntlmHost,
        domain: domain)
    )
  }

  @Override
  void removeHTTPSProxy() {
    HttpClientConfiguration configuration = detachedConfiguration()
    if(configuration.proxy != null) {
      configuration.proxy.https = null
      httpClientManager.configuration = configuration
    }
  }

  @Override
  void nonProxyHosts(final String... nonProxyHosts) {
    HttpClientConfiguration configuration = detachedConfiguration()
    if(configuration.proxy == null) {
      throw new IllegalStateException('Cannot configure non-proxy hosts without a configured proxy')
    }
    configuration.proxy.nonProxyHosts = nonProxyHosts
    httpClientManager.configuration = configuration
  }

  ConnectionConfiguration connection(HttpClientConfiguration configuration) {
    if (configuration.connection == null) {
      configuration.connection = new ConnectionConfiguration()
    }
    return configuration.connection
  }

  ProxyConfiguration proxy(HttpClientConfiguration configuration) {
    if (configuration.proxy == null) {
      configuration.proxy = new ProxyConfiguration()
    }
    return configuration.proxy
  }

  private void configureProxy(String host, int port, boolean https = false,
                              @Nullable AuthenticationConfiguration auth = null)
  {
    HttpClientConfiguration configuration = detachedConfiguration()
    if(https && (configuration.proxy == null || configuration.proxy.http == null)) {
      throw new IllegalStateException('Cannot configure https proxy without http proxy')
    }
    proxy(configuration)."${https ? 'https' : 'http'}" = new ProxyServerConfiguration(
        enabled: true,
        host: host,
        port: port,
        authentication: auth
    )
    httpClientManager.configuration = configuration
  }

  /**
   * Must use a 'detached' copy otherwise we can't serialize the singleton configuration back to the db.
   */
  HttpClientConfiguration detachedConfiguration() {
    HttpClientConfiguration configuration = httpClientManager.configuration
    configuration.entityMetadata = null
    return configuration
  }

  private checkPort(int port) {
    checkArgument(port >= 1 && port <= 65535, 'Port must be between 1 and 65535')
  }
}
