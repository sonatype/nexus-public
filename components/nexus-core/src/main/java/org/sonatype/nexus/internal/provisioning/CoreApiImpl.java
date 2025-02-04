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
package org.sonatype.nexus.internal.provisioning;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.CoreApi;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;
import org.sonatype.nexus.internal.app.BaseUrlCapabilityDescriptor;
import org.sonatype.nexus.internal.capability.DefaultCapabilityReference;
import org.sonatype.nexus.security.UserIdHelper;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.httpclient.config.AuthenticationConfiguration.AUTHENTICATION_CONFIGURATION;

/**
 * @since 3.0
 */
@Named
@Singleton
public class CoreApiImpl
    extends ComponentSupport
    implements CoreApi
{
  private final CapabilityRegistry capabilityRegistry;

  private final HttpClientManager httpClientManager;

  private final SecretsService secretsService;

  @Inject
  public CoreApiImpl(
      final CapabilityRegistry capabilityRegistry,
      final HttpClientManager httpClientManager,
      final SecretsService secretsService)
  {
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
    this.httpClientManager = checkNotNull(httpClientManager);
    this.secretsService = checkNotNull(secretsService);
  }

  @Override
  public void baseUrl(final String url) {
    checkNotNull(url);
    DefaultCapabilityReference existing = (DefaultCapabilityReference) capabilityRegistry.getAll()
        .stream()
        .filter(
            capabilityReference -> BaseUrlCapabilityDescriptor.TYPE.equals(
                capabilityReference.context().descriptor().type()))
        .findFirst()
        .orElse(null);

    if (existing != null) {
      log.info("BaseUrl capability updated to: {}",
          capabilityRegistry.update(existing.id(), existing.isActive(), existing.notes(), ImmutableMap.of("url", url)));
    }
    else {
      log.info("BaseUrl capability created as: {}",
          capabilityRegistry.add(BaseUrlCapabilityDescriptor.TYPE, true, "configured through api",
              ImmutableMap.of("url", url)));
    }
  }

  @Override
  public void removeBaseUrl() {
    DefaultCapabilityReference existing = (DefaultCapabilityReference) capabilityRegistry.getAll()
        .stream()
        .filter(
            capabilityReference -> BaseUrlCapabilityDescriptor.TYPE.equals(
                capabilityReference.context().descriptor().type()))
        .findFirst()
        .orElse(null);

    if (existing != null) {
      log.info("Deleting BaseUrl capability");
      capabilityRegistry.remove(existing.id());
    }
    else {
      log.info("No BaseUrl capability configured to remove");
    }
  }

  @Override
  public void userAgentCustomization(final String userAgentSuffix) {
    checkNotNull(userAgentSuffix);
    HttpClientConfiguration configuration = detachedConfiguration();
    connection(configuration).setUserAgentSuffix(userAgentSuffix);
    httpClientManager.setConfiguration(configuration);
  }

  @Override
  public void connectionTimeout(final int timeout) {
    checkArgument((timeout >= 1 && timeout <= 3600), "Value must be between 1 and 3600");
    HttpClientConfiguration configuration = detachedConfiguration();
    connection(configuration).setTimeout(Time.seconds(timeout));
    httpClientManager.setConfiguration(configuration);
  }

  @Override
  public void connectionRetryAttempts(final int retries) {
    checkArgument((retries >= 0 && retries <= 10), "Value must be between 0 and 10");
    HttpClientConfiguration configuration = detachedConfiguration();
    connection(configuration).setRetries(retries);
    httpClientManager.setConfiguration(configuration);
  }

  @Override
  public void httpProxy(final String host, final int port) {
    checkNotNull(host);
    checkPort(port);
    configureProxy(host, port, false, null, null);
  }

  @Override
  public void httpProxyWithBasicAuth(final String host, final int port, final String username, final String password) {
    checkNotNull(host);
    checkPort(port);
    checkNotNull(username);
    checkNotNull(password);
    UsernameAuthenticationConfiguration config = new UsernameAuthenticationConfiguration();
    config.setUsername(username);
    final Secret secret = encrypt(password);
    config.setPassword(secret);
    configureProxy(host, port, false, config, secret);
  }

  private Secret encrypt(final String password) {
    return secretsService.encryptMaven(AUTHENTICATION_CONFIGURATION, password.toCharArray(), UserIdHelper.get());
  }

  @Override
  public void httpProxyWithNTLMAuth(
      final String host,
      final int port,
      final String username,
      final String password,
      final String ntlmHost,
      final String domain)
  {
    checkNotNull(host);
    checkPort(port);
    checkNotNull(username);
    checkNotNull(password);
    checkNotNull(ntlmHost);
    checkNotNull(domain);
    NtlmAuthenticationConfiguration config = new NtlmAuthenticationConfiguration();
    config.setUsername(username);
    final Secret secret = encrypt(password);
    config.setPassword(secret);
    config.setHost(ntlmHost);
    config.setDomain(domain);
    configureProxy(host, port, false, config, secret);
  }

  @Override
  public void removeHTTPProxy() {
    HttpClientConfiguration configuration = detachedConfiguration();
    if (configuration.getProxy() != null) {
      configuration.setProxy(null);
      httpClientManager.setConfiguration(configuration);
    }
  }

  @Override
  public void httpsProxy(final String host, final int port) {
    checkNotNull(host);
    checkPort(port);
    configureProxy(host, port, true, null, null);
  }

  @Override
  public void httpsProxyWithBasicAuth(final String host, final int port, final String username, final String password) {
    checkNotNull(host);
    checkPort(port);
    checkNotNull(username);
    checkNotNull(password);
    UsernameAuthenticationConfiguration config = new UsernameAuthenticationConfiguration();
    config.setUsername(username);
    final Secret secret = encrypt(password);
    config.setPassword(secret);
    configureProxy(host, port, true, config, secret);
  }

  @Override
  public void httpsProxyWithNTLMAuth(
      final String host,
      final int port,
      final String username,
      final String password,
      final String ntlmHost,
      final String domain)
  {
    checkNotNull(host);
    checkPort(port);
    checkNotNull(username);
    checkNotNull(password);
    checkNotNull(ntlmHost);
    checkNotNull(domain);
    NtlmAuthenticationConfiguration config = new NtlmAuthenticationConfiguration();
    config.setUsername(username);
    final Secret secret = encrypt(password);
    config.setPassword(secret);
    config.setHost(ntlmHost);
    config.setDomain(domain);
    configureProxy(host, port, true, config, secret);
  }

  @Override
  public void removeHTTPSProxy() {
    HttpClientConfiguration configuration = detachedConfiguration();
    if (configuration.getProxy() != null) {
      configuration.getProxy().setHttps(null);
      httpClientManager.setConfiguration(configuration);
    }
  }

  @Override
  public void nonProxyHosts(final String... nonProxyHosts) {
    HttpClientConfiguration configuration = detachedConfiguration();
    if (configuration.getProxy() == null) {
      throw new IllegalStateException("Cannot configure non-proxy hosts without a configured proxy");
    }
    configuration.getProxy().setNonProxyHosts(nonProxyHosts);
    httpClientManager.setConfiguration(configuration);
  }

  private ConnectionConfiguration connection(final HttpClientConfiguration configuration) {
    if (configuration.getConnection() == null) {
      configuration.setConnection(new ConnectionConfiguration());
    }
    return configuration.getConnection();
  }

  private ProxyConfiguration proxy(final HttpClientConfiguration configuration) {
    if (configuration.getProxy() == null) {
      configuration.setProxy(new ProxyConfiguration());
    }
    return configuration.getProxy();
  }

  private void configureProxy(
      final String host,
      final int port,
      final boolean https,
      @Nullable final AuthenticationConfiguration auth,
      @Nullable final Secret secret)
  {
    HttpClientConfiguration configuration = detachedConfiguration();
    if (https && (configuration.getProxy() == null || configuration.getProxy().getHttp() == null)) {
      throw new IllegalStateException("Cannot configure https proxy without http proxy");
    }
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setEnabled(true);
    proxyServerConfiguration.setHost(host);
    proxyServerConfiguration.setPort(port);
    proxyServerConfiguration.setAuthentication(auth);
    if (https) {
      proxy(configuration).setHttps(proxyServerConfiguration);
    }
    else {
      proxy(configuration).setHttp(proxyServerConfiguration);
    }
    try {
      httpClientManager.setConfiguration(configuration);
    }
    catch (Exception e) {
      secretsService.remove(secret);
      throw e;
    }
  }

  /**
   * Must use a 'detached' copy otherwise we can't serialize the singleton configuration back to the db.
   */
  private HttpClientConfiguration detachedConfiguration() {
    HttpClientConfiguration configuration = httpClientManager.getConfiguration();
    EntityHelper.clearMetadata(configuration);
    return configuration;
  }

  private void checkPort(final int port) {
    checkArgument(port >= 1 && port <= 65535, "Port must be between 1 and 65535");
  }
}
