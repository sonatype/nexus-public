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
package org.sonatype.nexus.coreui

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull

import org.sonatype.goodies.common.Time
import org.sonatype.nexus.common.text.Strings2
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.httpclient.HttpClientManager
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration
import org.sonatype.nexus.httpclient.config.ProxyConfiguration
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration
import org.sonatype.nexus.rapture.PasswordPlaceholder
import org.sonatype.nexus.validation.Validate

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import groovy.transform.PackageScope
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions

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
@DirectAction(action = 'coreui_HttpSettings')
class HttpSettingsComponent
    extends DirectComponentSupport
{
  @Inject
  HttpClientManager httpClientManager

  /**
   * Retrieves HTTP system settings
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:settings:read')
  HttpSettingsXO read() {
    return convert(httpClientManager.configuration)
  }

  @PackageScope
  HttpSettingsXO convert(final HttpClientConfiguration value) {
    HttpSettingsXO result = new HttpSettingsXO()

    value.connection?.with {
      result.userAgentSuffix = userAgentSuffix
      result.timeout = timeout ? timeout.toSecondsI() : null
      result.retries = maximumRetries
    }

    value.proxy?.with {
      http?.with {
        result.httpEnabled = enabled
        result.httpHost = host
        result.httpPort = port

        authentication instanceof UsernameAuthenticationConfiguration && authentication.with {
          result.httpAuthEnabled = true
          result.httpAuthUsername = username
          result.httpAuthPassword = PasswordPlaceholder.get(password)
        }
        authentication instanceof NtlmAuthenticationConfiguration && authentication.with {
          result.httpAuthEnabled = true
          result.httpAuthUsername = username
          result.httpAuthPassword = PasswordPlaceholder.get(password)
          result.httpAuthNtlmHost = host
          result.httpAuthNtlmDomain = domain
        }
      }

      https?.with {
        result.httpsEnabled = enabled
        result.httpsHost = host
        result.httpsPort = port

        authentication instanceof UsernameAuthenticationConfiguration && authentication.with {
          result.httpsAuthEnabled = true
          result.httpsAuthUsername = username
          result.httpsAuthPassword = PasswordPlaceholder.get(password)
        }
        authentication instanceof NtlmAuthenticationConfiguration && authentication.with {
          result.httpsAuthEnabled = true
          result.httpsAuthUsername = username
          result.httpsAuthPassword = PasswordPlaceholder.get(password)
          result.httpsAuthNtlmHost = host
          result.httpsAuthNtlmDomain = domain
        }
      }

      if (nonProxyHosts) {
        result.nonProxyHosts = nonProxyHosts as Set<String>
      }
    }

    // ignore authentication, this is not exposed for global configuration

    return result
  }

  /**
   * Updates HTTP system settings.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:settings:update')
  @Validate
  HttpSettingsXO update(final @NotNull @Valid HttpSettingsXO settings) {
    def previous = httpClientManager.configuration
    def model = convert(settings)
    replacePasswordPlaceholders(previous, model)
    httpClientManager.configuration = model
    return read()
  }

  @PackageScope
  HttpClientConfiguration convert(final HttpSettingsXO value) {
    def result = new HttpClientConfiguration()

    // convert connection configuration
    def connection = {
      if (result.connection == null) {
        result.connection = new ConnectionConfiguration()
      }
      return result.connection
    }

    if (!Strings2.isBlank(value.userAgentSuffix)) {
      connection().userAgentSuffix = value.userAgentSuffix
    }

    if (value.timeout != null) {
      connection().timeout = Time.seconds(value.timeout)
    }
    if (value.retries != null) {
      connection().maximumRetries = value.retries
    }

    // convert proxy configuration
    def proxy = {
      if (result.proxy == null) {
        result.proxy = new ProxyConfiguration()
      }
      return result.proxy
    }

    // http proxy
    if (value.httpEnabled) {
      proxy().http = new ProxyServerConfiguration(
          enabled: true,
          host: value.httpHost,
          port: value.httpPort,
          authentication: auth(
              value.httpAuthEnabled,
              value.httpAuthUsername,
              value.httpAuthPassword,
              value.httpAuthNtlmHost,
              value.httpAuthNtlmDomain
          )
      )
    }

    // https proxy
    if (value.httpsEnabled) {
      proxy().https = new ProxyServerConfiguration(
          enabled: true,
          host: value.httpsHost,
          port: value.httpsPort,
          authentication: auth(
              value.httpsAuthEnabled,
              value.httpsAuthUsername,
              value.httpsAuthPassword,
              value.httpsAuthNtlmHost,
              value.httpsAuthNtlmDomain
          )
      )
    }

    if (value.nonProxyHosts) {
      proxy().nonProxyHosts = value.nonProxyHosts as String[]
    }

    // ignore authentication, this is not exposed for global configuration

    return result
  }

  @PackageScope
  @Nullable
  AuthenticationConfiguration auth(final Boolean enabled,
                                   final String username,
                                   final String password,
                                   final String host,
                                   final String domain)
  {
    if (!enabled) {
      return null
    }

    // HACK: non-optimal use of host/domain to determine authentication type
    if (host || domain) {
      return new NtlmAuthenticationConfiguration(
          username: username,
          password: password,
          host: host,
          domain: domain
      )
    }
    else {
      return new UsernameAuthenticationConfiguration(
          username: username,
          password: password
      )
    }
  }

  /**
   * Replace password placeholders updated model with values from previous module.
   */
  @PackageScope
  void replacePasswordPlaceholders(final HttpClientConfiguration previous, final HttpClientConfiguration updated) {
    if (PasswordPlaceholder.is(updated?.proxy?.http?.authentication?.password)) {
      updated.proxy.http.authentication.password = previous?.proxy?.http?.authentication?.password
    }
    if (PasswordPlaceholder.is(updated?.proxy?.https?.authentication?.password)) {
      updated.proxy.https.authentication.password = previous?.proxy?.https?.authentication?.password
    }
  }
}
