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
package org.sonatype.nexus.httpclient.config;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.httpclient.HttpClientPlan;
import org.sonatype.nexus.httpclient.SSLContextSelector;
import org.sonatype.nexus.httpclient.internal.NexusHttpRoutePlanner;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.http.client.config.AuthSchemes.BASIC;
import static org.apache.http.client.config.AuthSchemes.DIGEST;
import static org.apache.http.client.config.AuthSchemes.NTLM;
import static org.sonatype.nexus.httpclient.HttpSchemes.HTTP;
import static org.sonatype.nexus.httpclient.HttpSchemes.HTTPS;

/**
 * Applies {@link HttpClientConfiguration} to {@link HttpClientPlan}.
 *
 * @since 3.0
 */
public class ConfigurationCustomizer
    extends ComponentSupport
    implements HttpClientPlan.Customizer
{
  /**
   * Simple reusable function that converts "glob-like" expressions to regexp.
   */
  private static final Function<String, String> GLOB_STRING_TO_REGEXP_STRING = new Function<String, String>()
  {
    @Override
    public String apply(final String input) {
      return "(" +
          input.toLowerCase(Locale.US).replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*?").replaceAll("\\[", "\\\\[")
              .replaceAll("\\]", "\\\\]") + ")";
    }
  };

  static {
    /**
     * Install custom {@link Authenticator} for proxy.
     */
    Authenticator.setDefault(new Authenticator()
    {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        if (getRequestorType() == RequestorType.PROXY) {
          String prot = getRequestingProtocol().toLowerCase();
          String host = System.getProperty(prot + ".proxyHost", "");
          String port = System.getProperty(prot + ".proxyPort", "80");
          String user = System.getProperty(prot + ".proxyUser", "");
          String password = System.getProperty(prot + ".proxyPassword", "");

          if (getRequestingHost().equalsIgnoreCase(host)) {
            if (Integer.parseInt(port) == getRequestingPort()) {
              // Seems to be OK.
              return new PasswordAuthentication(user, password.toCharArray());
            }
          }
        }
        return null;
      }
    });
  }

  private final HttpClientConfiguration configuration;

  public ConfigurationCustomizer(final HttpClientConfiguration configuration) {
    this.configuration = checkNotNull(configuration);
  }

  @Override
  public void customize(final HttpClientPlan plan) {
    checkNotNull(plan);

    if (configuration.getConnection() != null) {
      apply(configuration.getConnection(), plan);
    }
    if (configuration.getProxy() != null) {
      apply(configuration.getProxy(), plan);
    }
    if (configuration.getAuthentication() != null) {
      apply(configuration.getAuthentication(), plan, null);
    }
    if (configuration.getRedirectStrategy() != null) {
      apply(configuration.getRedirectStrategy(), plan);
    }
  }

  /**
   * Apply connection configuration to plan.
   */
  private void apply(final ConnectionConfiguration connection, final HttpClientPlan plan) {
    if (connection.getTimeout() != null) {
      int timeout = connection.getTimeout().toMillisI();
      plan.getSocket().setSoTimeout(timeout);
      plan.getRequest().setConnectTimeout(timeout);
      plan.getRequest().setSocketTimeout(timeout);
    }

    if (connection.getMaximumRetries() != null) {
      plan.getClient().setRetryHandler(new StandardHttpRequestRetryHandler(connection.getMaximumRetries(), false));
    }

    if (connection.getUserAgentSuffix() != null) {
      checkState(plan.getUserAgentBase() != null, "Default User-Agent not set");
      plan.setUserAgentSuffix(connection.getUserAgentSuffix());
    }

    if (Boolean.TRUE.equals(connection.getUseTrustStore())) {
      plan.getAttributes().put(SSLContextSelector.USE_TRUST_STORE, Boolean.TRUE);
    }

    if (Boolean.TRUE.equals(connection.getEnableCircularRedirects())) {
      plan.getRequest().setCircularRedirectsAllowed(true);
    }

    if (Boolean.TRUE.equals(connection.getEnableCookies())) {
      plan.getRequest().setCookieSpec(CookieSpecs.DEFAULT);
    }
  }

  /**
   * Apply proxy-server configuration to plan.
   */
  private void apply(final ProxyConfiguration proxy, final HttpClientPlan plan) {
    // HTTP proxy
    ProxyServerConfiguration http = proxy.getHttp();
    if (http != null && http.isEnabled()) {
      HttpHost host = new HttpHost(http.getHost(), http.getPort());
      if (http.getAuthentication() != null) {
        apply(http.getAuthentication(), plan, host);
      }
    }

    // HTTPS proxy
    ProxyServerConfiguration https = proxy.getHttps();
    if (https != null && https.isEnabled()) {
      HttpHost host = new HttpHost(https.getHost(), https.getPort());
      if (https.getAuthentication() != null) {
        apply(https.getAuthentication(), plan, host);
      }
    }
    plan.getClient().setRoutePlanner(createRoutePlanner(proxy));
  }

  /**
   * Apply redirect strategy to plan.
   */
  private void apply(final RedirectStrategy redirectStrategy, final HttpClientPlan plan) {
    plan.getClient().setRedirectStrategy(redirectStrategy);
  }

  /**
   * Creates instance of {@link NexusHttpRoutePlanner} from passed in configuration, never {@code null}.
   */
  @VisibleForTesting
  NexusHttpRoutePlanner createRoutePlanner(final ProxyConfiguration proxy) {
    Map<String, HttpHost> proxies = new HashMap<>(2);

    // HTTP proxy
    ProxyServerConfiguration http = proxy.getHttp();
    if (http != null && http.isEnabled()) {
      HttpHost host = new HttpHost(http.getHost(), http.getPort());
      proxies.put(HTTP, host);
      proxies.put(HTTPS, host);
    }

    // HTTPS proxy
    ProxyServerConfiguration https = proxy.getHttps();
    if (https != null && https.isEnabled()) {
      HttpHost host = new HttpHost(https.getHost(), https.getPort());
      proxies.put(HTTPS, host);
    }

    // Non-proxy hosts (Java http.nonProxyHosts formatted glob-like patterns converted to single Regexp expression)
    LinkedHashSet<String> patterns = new LinkedHashSet<>();
    if (proxy.getNonProxyHosts() != null) {
      patterns.addAll(Arrays.asList(proxy.getNonProxyHosts()));
    }
    String nonProxyPatternString = Joiner.on("|").join(Iterables.transform(patterns, GLOB_STRING_TO_REGEXP_STRING));
    Pattern nonProxyPattern = null;
    if (!Strings2.isBlank(nonProxyPatternString)) {
      try {
        nonProxyPattern = Pattern.compile(nonProxyPatternString, Pattern.CASE_INSENSITIVE);
      }
      catch (PatternSyntaxException e) {
        log.warn("Invalid non-proxy host regex: {}, using defaults", nonProxyPatternString, e);
      }
    }
    return new NexusHttpRoutePlanner(proxies, nonProxyPattern);
  }

  /**
   * Apply authentication configuration to plan.
   */
  private void apply(final AuthenticationConfiguration authentication,
                     final HttpClientPlan plan,
                     @Nullable final HttpHost proxyHost)
  {
    Credentials credentials;
    List<String> authSchemes;

    if (authentication instanceof UsernameAuthenticationConfiguration) {
      UsernameAuthenticationConfiguration auth = (UsernameAuthenticationConfiguration) authentication;
      authSchemes = ImmutableList.of(DIGEST, BASIC);
      credentials = new UsernamePasswordCredentials(auth.getUsername(), auth.getPassword());
    }
    else if (authentication instanceof NtlmAuthenticationConfiguration) {
      NtlmAuthenticationConfiguration auth = (NtlmAuthenticationConfiguration) authentication;
      authSchemes = ImmutableList.of(NTLM, DIGEST, BASIC);
      credentials = new NTCredentials(auth.getUsername(), auth.getPassword(), auth.getHost(), auth.getDomain());
    }
    else {
      throw new IllegalArgumentException("Unsupported authentication configuration: " + authentication);
    }

    if (proxyHost != null) {
      plan.addCredentials(new AuthScope(proxyHost), credentials);
      plan.getRequest().setProxyPreferredAuthSchemes(authSchemes);
    }
    else {
      plan.addCredentials(AuthScope.ANY, credentials);
      plan.getRequest().setTargetPreferredAuthSchemes(authSchemes);
    }
  }
}
