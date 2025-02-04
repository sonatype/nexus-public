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

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.security.UserIdHelper;
import org.sonatype.nexus.validation.Validate;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;

//
// FIXME: overly complex conversion due to lack of nested structure in exchange-object payload
//

/**
 * HTTP System Settings {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = "coreui_HttpSettings")
public class HttpSettingsComponent
    extends DirectComponentSupport
{
  private final HttpClientManager httpClientManager;

  private final SecretsService secretsService;

  @Inject
  public HttpSettingsComponent(final HttpClientManager httpClientManager, final SecretsService secretsService) {
    this.httpClientManager = checkNotNull(httpClientManager);
    this.secretsService = checkNotNull(secretsService);
  }

  /**
   * Retrieves HTTP system settings
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:settings:read")
  public HttpSettingsXO read() {
    return convert(httpClientManager.getConfiguration());
  }

  private HttpSettingsXO convert(final HttpClientConfiguration value) {
    HttpSettingsXO result = new HttpSettingsXO();

    if (value.getConnection() != null) {
      ConnectionConfiguration connection = value.getConnection();
      result.setUserAgentSuffix(connection.getUserAgentSuffix());
      result.setTimeout(connection.getTimeout() != null ? connection.getTimeout().toSecondsI() : null);
      result.setRetries(connection.getRetries());
    }

    if (value.getProxy() != null) {
      ProxyConfiguration proxy = value.getProxy();
      if (proxy.getHttp() != null) {
        configureHttpProxy(proxy.getHttp(), result);
      }

      if (proxy.getHttps() != null) {
        configureHttpsProxy(proxy.getHttps(), result);
      }

      if (proxy.getNonProxyHosts() != null) {
        result.setNonProxyHosts(Set.of(proxy.getNonProxyHosts()));
      }
    }

    // ignore authentication, this is not exposed for global configuration
    return result;
  }

  /**
   * Updates HTTP system settings.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  @Validate
  public HttpSettingsXO update(@NotNull @Valid final HttpSettingsXO settings) {
    HttpClientConfiguration previous = httpClientManager.getConfiguration();
    HttpClientConfiguration model = null;
    try {
      model = convert(settings, previous);
    }
    catch (Exception e) {
      removeSecrets(previous, model);
      throw e;
    }
    httpClientManager.setConfiguration(model);
    removeSecrets(previous, model);
    return read();
  }

  private HttpClientConfiguration convert(final HttpSettingsXO value, final HttpClientConfiguration previous) {
    HttpClientConfiguration result = httpClientManager.newConfiguration();

    if (!Strings2.isBlank(value.getUserAgentSuffix())) {
      ensureConnectionInitialized(result);
      result.getConnection().setUserAgentSuffix(value.getUserAgentSuffix());
    }

    if (value.getTimeout() != null) {
      ensureConnectionInitialized(result);
      result.getConnection().setTimeout(Time.seconds(value.getTimeout()));
    }

    if (value.getRetries() != null) {
      ensureConnectionInitialized(result);
      result.getConnection().setRetries(value.getRetries());
    }

    // http proxy
    if (Boolean.TRUE.equals(value.getHttpEnabled())) {
      ensureProxyInitialized(result);
      ProxyServerConfiguration proxyConfig = new ProxyServerConfiguration();
      proxyConfig.setEnabled(true);
      proxyConfig.setHost(value.getHttpHost());
      proxyConfig.setPort(value.getHttpPort());
      proxyConfig
          .setAuthentication(auth(value.getHttpAuthEnabled(), value.getHttpAuthUsername(), value.getHttpAuthPassword(),
              value.getHttpAuthNtlmHost(), value.getHttpAuthNtlmDomain(),
              getHttpSecret(previous)));
      result.getProxy().setHttp(proxyConfig);
    }

    // https proxy
    if (Boolean.TRUE.equals(value.getHttpsEnabled())) {
      ensureProxyInitialized(result);
      ProxyServerConfiguration proxyConfig = new ProxyServerConfiguration();
      proxyConfig.setEnabled(true);
      proxyConfig.setHost(value.getHttpsHost());
      proxyConfig.setPort(value.getHttpsPort());
      proxyConfig.setAuthentication(
          auth(value.getHttpsAuthEnabled(), value.getHttpsAuthUsername(), value.getHttpsAuthPassword(),
              value.getHttpsAuthNtlmHost(), value.getHttpsAuthNtlmDomain(),
              getHttpsSecret(previous)));
      result.getProxy().setHttps(proxyConfig);
    }

    if (value.getNonProxyHosts() != null) {
      ensureProxyInitialized(result);
      result.getProxy().setNonProxyHosts(value.getNonProxyHosts().toArray(new String[0]));
    }

    // ignore authentication, this is not exposed for global configuration
    return result;
  }

  @Nullable
  private AuthenticationConfiguration auth(
      final Boolean enabled,
      final String username,
      final String password,
      final String host,
      final String domain,
      final Secret previous)
  {
    if (Boolean.FALSE.equals(enabled)) {
      return null;
    }

    // HACK: non-optimal use of host/domain to determine authentication type
    if (host != null || domain != null) {
      NtlmAuthenticationConfiguration ntlmAuthConfig = new NtlmAuthenticationConfiguration();
      ntlmAuthConfig.setUsername(username);
      ntlmAuthConfig.setPassword(encrypt(password, previous));
      ntlmAuthConfig.setHost(host);
      ntlmAuthConfig.setDomain(domain);
      return ntlmAuthConfig;
    }
    else {
      UsernameAuthenticationConfiguration userAuthConfig = new UsernameAuthenticationConfiguration();
      userAuthConfig.setUsername(username);
      userAuthConfig.setPassword(encrypt(password, previous));
      return userAuthConfig;
    }
  }

  private Secret encrypt(String password, Secret previous) {
    if (Strings2.isBlank(password) || PasswordPlaceholder.is(password)) {
      return previous;
    }
    else {
      return secretsService.encryptMaven(
          AuthenticationConfiguration.AUTHENTICATION_CONFIGURATION,
          password.toCharArray(),
          UserIdHelper.get());
    }
  }

  private void removeSecrets(HttpClientConfiguration previous, HttpClientConfiguration newConfig) {
    if (!Objects.equals(getSecretId(getHttpSecret(previous)), getSecretId(getHttpSecret(newConfig)))) {
      removeSecret(getHttpAuthConfig(previous));
    }
    if (!Objects.equals(getSecretId(getHttpsSecret(previous)), getSecretId(getHttpsSecret(newConfig)))) {
      removeSecret(getHttpsAuthConfig(previous));
    }
  }

  private void removeSecret(AuthenticationConfiguration authConfig) {
    if (authConfig != null) {
      if (NtlmAuthenticationConfiguration.TYPE.equals(authConfig.getType())) {
        NtlmAuthenticationConfiguration ntlmAuth = (NtlmAuthenticationConfiguration) authConfig;
        secretsService.remove(ntlmAuth.getPassword());
      }
      else {
        UsernameAuthenticationConfiguration userNameAuth = (UsernameAuthenticationConfiguration) authConfig;
        secretsService.remove(userNameAuth.getPassword());
      }
    }
  }

  private Secret getHttpSecret(final HttpClientConfiguration configuration) {
    return Optional.ofNullable(configuration)
        .map(HttpClientConfiguration::getProxy)
        .map(ProxyConfiguration::getHttp)
        .map(ProxyServerConfiguration::getAuthentication)
        .map(AuthenticationConfiguration::getSecret)
        .orElse(null);
  }

  private Secret getHttpsSecret(final HttpClientConfiguration configuration) {
    return Optional.ofNullable(configuration)
        .map(HttpClientConfiguration::getProxy)
        .map(ProxyConfiguration::getHttps)
        .map(ProxyServerConfiguration::getAuthentication)
        .map(AuthenticationConfiguration::getSecret)
        .orElse(null);
  }

  private String getSecretId(final Secret secret) {
    return Optional.ofNullable(secret).map(Secret::getId).orElse(null);
  }

  private AuthenticationConfiguration getHttpAuthConfig(final HttpClientConfiguration configuration) {
    return Optional.ofNullable(configuration)
        .map(HttpClientConfiguration::getProxy)
        .map(ProxyConfiguration::getHttp)
        .map(ProxyServerConfiguration::getAuthentication)
        .orElse(null);
  }

  private AuthenticationConfiguration getHttpsAuthConfig(final HttpClientConfiguration configuration) {
    return Optional.ofNullable(configuration)
        .map(HttpClientConfiguration::getProxy)
        .map(ProxyConfiguration::getHttps)
        .map(ProxyServerConfiguration::getAuthentication)
        .orElse(null);
  }

  private void ensureConnectionInitialized(final HttpClientConfiguration configuration) {
    if (configuration.getConnection() == null) {
      configuration.setConnection(new ConnectionConfiguration());
    }
  }

  private void ensureProxyInitialized(final HttpClientConfiguration configuration) {
    if (configuration.getProxy() == null) {
      configuration.setProxy(new ProxyConfiguration());
    }
  }

  private void configureHttpProxy(ProxyServerConfiguration http, HttpSettingsXO result) {
    result.setHttpEnabled(http.isEnabled());
    result.setHttpHost(http.getHost());
    result.setHttpPort(http.getPort());

    if (http.getAuthentication() instanceof UsernameAuthenticationConfiguration auth) {
      result.setHttpAuthEnabled(true);
      result.setHttpAuthUsername(auth.getUsername());
      result.setHttpAuthPassword(PasswordPlaceholder.get(auth.getPassword()));
    }
    else if (http.getAuthentication() instanceof NtlmAuthenticationConfiguration auth) {
      result.setHttpAuthEnabled(true);
      result.setHttpAuthUsername(auth.getUsername());
      result.setHttpAuthPassword(PasswordPlaceholder.get(auth.getPassword()));
      result.setHttpAuthNtlmHost(auth.getHost());
      result.setHttpAuthNtlmDomain(auth.getDomain());
    }
  }

  private void configureHttpsProxy(ProxyServerConfiguration https, HttpSettingsXO result) {
    result.setHttpsEnabled(https.isEnabled());
    result.setHttpsHost(https.getHost());
    result.setHttpsPort(https.getPort());

    if (https.getAuthentication() instanceof UsernameAuthenticationConfiguration auth) {
      result.setHttpsAuthEnabled(true);
      result.setHttpsAuthUsername(auth.getUsername());
      result.setHttpsAuthPassword(PasswordPlaceholder.get(auth.getPassword()));
    }
    else if (https.getAuthentication() instanceof NtlmAuthenticationConfiguration auth) {
      result.setHttpsAuthEnabled(true);
      result.setHttpsAuthUsername(auth.getUsername());
      result.setHttpsAuthPassword(PasswordPlaceholder.get(auth.getPassword()));
      result.setHttpsAuthNtlmHost(auth.getHost());
      result.setHttpsAuthNtlmDomain(auth.getDomain());
    }
  }
}
