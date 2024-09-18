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
package org.sonatype.nexus.httpclient.internal.secrets.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.BearerTokenAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.security.secrets.SecretMigrationException;
import org.sonatype.nexus.security.secrets.SecretsMigratorSupport;

import com.google.common.annotations.VisibleForTesting;

import static org.sonatype.nexus.httpclient.config.AuthenticationConfiguration.AUTHENTICATION_CONFIGURATION;

@Named
public class HttpSecretsMigrator
    extends SecretsMigratorSupport
{
  @VisibleForTesting
  static final String GLOBAL_AUTHENTICATION_CONFIGURATION = "global authentication configuration";

  @VisibleForTesting
  static final String HTTP_PROXY_CONFIGURATION = "http proxy configuration";

  @VisibleForTesting
  static final String HTTPS_PROXY_CONFIGURATION = "https proxy configuration";

  private final HttpClientManager httpClientManager;

  @Inject
  public HttpSecretsMigrator(
      final HttpClientManager httpClientManager, final SecretsService secretsService)
  {
    super(secretsService);
    this.httpClientManager = httpClientManager;
  }

  @Override
  public void migrate() {
    CancelableHelper.checkCancellation();

    List<Secret> secrets = new ArrayList<>();
    HttpClientConfiguration httpClientConfiguration = httpClientManager.getConfiguration();

    if (httpClientConfiguration != null) {
      // Migrate Global auth config
      migrateSecret(httpClientConfiguration.getAuthentication(), secrets, GLOBAL_AUTHENTICATION_CONFIGURATION);

      // Migrate HTTP proxy auth config
      AuthenticationConfiguration proxyHttpConfig = Optional.ofNullable(httpClientConfiguration.getProxy())
          .map(ProxyConfiguration::getHttp)
          .map(ProxyServerConfiguration::getAuthentication)
          .orElse(null);
      migrateSecret(proxyHttpConfig, secrets, HTTP_PROXY_CONFIGURATION);

      // Migrate HTTPS proxy auth config
      AuthenticationConfiguration proxyHttpsConfig = Optional.ofNullable(httpClientConfiguration.getProxy())
          .map(ProxyConfiguration::getHttps)
          .map(ProxyServerConfiguration::getAuthentication)
          .orElse(null);
      migrateSecret(proxyHttpsConfig, secrets, HTTPS_PROXY_CONFIGURATION);

      save(httpClientConfiguration, secrets);
    }
  }

  private void migrateSecret(AuthenticationConfiguration config, List<Secret> secrets, String context) {
    Secret secret = Optional.ofNullable(config)
        .map(AuthenticationConfiguration::getSecret)
        .orElse(null);

    if (secret != null && isLegacyEncryptedString(secret)) {
      Secret migratedSecret = createSecret(AUTHENTICATION_CONFIGURATION, secret, context);
      updateConfig(config, migratedSecret);
      secrets.add(migratedSecret);
    }
  }

  private void updateConfig(final AuthenticationConfiguration configuration, final Secret secret) {
    if (configuration.getClass().equals(UsernameAuthenticationConfiguration.class)) {
      ((UsernameAuthenticationConfiguration) configuration).setPassword(secret);
    }
    else if (configuration.getClass().equals(NtlmAuthenticationConfiguration.class)) {
      ((NtlmAuthenticationConfiguration) configuration).setPassword(secret);
    }
    else if (configuration.getClass().equals(BearerTokenAuthenticationConfiguration.class)) {
      ((BearerTokenAuthenticationConfiguration) configuration).setBearerToken(secret);
    }
  }

  private void save(final HttpClientConfiguration configuration, final List<Secret> secrets) {
    if (secrets.isEmpty()) {
      return;
    }

    try {
      httpClientManager.setConfiguration(configuration);
    }
    catch (Exception e) {
      quietlyRemove(secrets);
      throw new SecretMigrationException("Failed to migrate HTTP client configuration", e);
    }
  }
}
