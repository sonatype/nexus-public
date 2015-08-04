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
package org.sonatype.nexus.apachehttpclient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.sonatype.nexus.apachehttpclient.Hc4Provider.Builder;
import org.sonatype.nexus.proxy.repository.ClientSSLRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.NtlmRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.repository.UsernamePasswordRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.utils.UserAgentBuilder;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support class for implementation of {@link Hc4Provider}.
 *
 * @author cstamas
 * @since 2.2
 */
public class Hc4ProviderBase
    extends ComponentSupport
{

  /**
   * Key for customizing default (and max) keep alive duration when remote server does not state anything, or states
   * some unreal high value. Value is milliseconds.
   */
  private static final String KEEP_ALIVE_MAX_DURATION_KEY = "nexus.apacheHttpClient4x.keepAliveMaxDuration";

  /**
   * Default keep alive max duration: 30 seconds.
   */
  private static final long KEEP_ALIVE_MAX_DURATION_DEFAULT = TimeUnit.SECONDS.toMillis(30);

  /**
   * UA builder component.
   */
  private final UserAgentBuilder userAgentBuilder;

  /**
   * @param userAgentBuilder UA builder component, must not be {@code null}.
   */
  public Hc4ProviderBase(final UserAgentBuilder userAgentBuilder) {
    this.userAgentBuilder = checkNotNull(userAgentBuilder);
  }

  // configuration

  // ==

  protected Builder prepareHttpClient(final RemoteStorageContext context,
                                     final HttpClientConnectionManager httpClientConnectionManager)
  {
    final Builder builder = new Builder();
    builder.getHttpClientBuilder().setConnectionManager(httpClientConnectionManager);
    builder.getHttpClientBuilder().addInterceptorFirst(new ResponseContentEncoding());
    applyConfig(builder, context);
    applyAuthenticationConfig(builder, context.getRemoteAuthenticationSettings(), null);
    applyProxyConfig(builder, context.getRemoteProxySettings());
    // obey the given retries count and apply it to client.
    final int retries =
        context.getRemoteConnectionSettings() != null
            ? context.getRemoteConnectionSettings().getRetrievalRetryCount()
            : 0;
    builder.getHttpClientBuilder().setRetryHandler(new StandardHttpRequestRetryHandler(retries, false));
    builder.getHttpClientBuilder().setKeepAliveStrategy(new NexusConnectionKeepAliveStrategy(getKeepAliveMaxDuration()));
    return builder;
  }

  protected void applyConfig(final Builder builder, final RemoteStorageContext context) {
    builder.getSocketConfigBuilder().setSoTimeout(getSoTimeout(context));

    builder.getConnectionConfigBuilder().setBufferSize(8 * 1024);

    builder.getRequestConfigBuilder().setCookieSpec(CookieSpecs.IGNORE_COOKIES);
    builder.getRequestConfigBuilder().setExpectContinueEnabled(false);
    builder.getRequestConfigBuilder().setStaleConnectionCheckEnabled(false);
    builder.getRequestConfigBuilder().setConnectTimeout(getConnectionTimeout(context));
    builder.getRequestConfigBuilder().setSocketTimeout(getSoTimeout(context));

    final String userAgent = userAgentBuilder.formatUserAgentString(context);
    builder.getHttpClientBuilder().setUserAgent(userAgent);
    builder.getHttpClientBuilder().setRequestExecutor(new HttpRequestExecutor() {
      @Override
      public void preProcess(final HttpRequest request, final HttpProcessor processor, final HttpContext ctx)
          throws HttpException, IOException
      {
        // NEXUS-7575: In case of HTTP Proxy tunnel, add generic UA while performing CONNECT
        if (!request.containsHeader(HTTP.USER_AGENT)) {
          request.addHeader(new BasicHeader(HTTP.USER_AGENT, userAgent));
        }
        super.preProcess(request, processor, ctx);
      }
    });
  }

  /**
   * Returns the maximum Keep-Alive duration in milliseconds.
   */
  protected long getKeepAliveMaxDuration() {
    return SystemPropertiesHelper.getLong(KEEP_ALIVE_MAX_DURATION_KEY, KEEP_ALIVE_MAX_DURATION_DEFAULT);
  }

  /**
   * Returns the connection timeout in milliseconds. The timeout until connection is established.
   */
  protected int getConnectionTimeout(final RemoteStorageContext context) {
    if (context.getRemoteConnectionSettings() != null) {
      return context.getRemoteConnectionSettings().getConnectionTimeout();
    }
    else {
      // see DefaultRemoteConnectionSetting
      return 1000;
    }
  }

  /**
   * Returns the SO_SOCKET timeout in milliseconds. The timeout for waiting for data on established connection.
   */
  protected int getSoTimeout(final RemoteStorageContext context) {
    // this parameter is actually set from #getConnectionTimeout
    return getConnectionTimeout(context);
  }

  // ==

  /**
   * Returns {@code true} if passed in {@link RemoteStorageContext} contains some configuration element that
   * does require connection reuse (typically remote NTLM authentication or proxy with NTLM authentication set).
   *
   * @param context the remote storage context to test for need of reused connections.
   * @return {@code true} if connection reuse is required according to remote storage context.
   * @since 2.7.2
   */
  protected boolean reuseConnectionsNeeded(final RemoteStorageContext context) {
    // return true if any of the auth is NTLM based, as NTLM must have keep-alive to work
    if (context != null) {
      if (context.getRemoteAuthenticationSettings() instanceof NtlmRemoteAuthenticationSettings) {
        return true;
      }
      if (context.getRemoteProxySettings() != null) {
        if (context.getRemoteProxySettings().getHttpProxySettings() != null &&
            context.getRemoteProxySettings().getHttpProxySettings()
                .getProxyAuthentication() instanceof NtlmRemoteAuthenticationSettings) {
          return true;
        }
        if (context.getRemoteProxySettings().getHttpsProxySettings() != null &&
            context.getRemoteProxySettings().getHttpsProxySettings()
                .getProxyAuthentication() instanceof NtlmRemoteAuthenticationSettings) {
          return true;
        }
      }
    }
    return false;
  }

  protected void applyAuthenticationConfig(final Builder builder,
                                           final RemoteAuthenticationSettings ras,
                                           final HttpHost proxyHost)
  {
    if (ras != null) {
      String authScope = "target";
      if (proxyHost != null) {
        authScope = proxyHost.toHostString() + " proxy";
      }

      final List<String> authorisationPreference = Lists.newArrayListWithExpectedSize(3);
      authorisationPreference.add(AuthSchemes.DIGEST);
      authorisationPreference.add(AuthSchemes.BASIC);
      Credentials credentials = null;
      if (ras instanceof ClientSSLRemoteAuthenticationSettings) {
        throw new IllegalArgumentException("SSL client authentication not yet supported!");
      }
      else if (ras instanceof NtlmRemoteAuthenticationSettings) {
        final NtlmRemoteAuthenticationSettings nras = (NtlmRemoteAuthenticationSettings) ras;
        // Using NTLM auth, adding it as first in policies
        authorisationPreference.add(0, AuthSchemes.NTLM);
        log.debug("{} authentication setup for NTLM domain '{}'", authScope, nras.getNtlmDomain());
        credentials = new NTCredentials(
            nras.getUsername(), nras.getPassword(), nras.getNtlmHost(), nras.getNtlmDomain()
        );
      }
      else if (ras instanceof UsernamePasswordRemoteAuthenticationSettings) {
        final UsernamePasswordRemoteAuthenticationSettings uras =
            (UsernamePasswordRemoteAuthenticationSettings) ras;
        log.debug("{} authentication setup for remote storage with username '{}'", authScope,
            uras.getUsername());
        credentials = new UsernamePasswordCredentials(uras.getUsername(), uras.getPassword());
      }

      if (credentials != null) {
        if (proxyHost != null) {
          builder.setCredentials(new AuthScope(proxyHost), credentials);
          builder.getRequestConfigBuilder().setProxyPreferredAuthSchemes(authorisationPreference);
        }
        else {
          builder.setCredentials(AuthScope.ANY, credentials);
          builder.getRequestConfigBuilder().setTargetPreferredAuthSchemes(authorisationPreference);
        }
      }
    }
  }

  /**
   * @since 2.6
   */
  protected void applyProxyConfig(final Builder builder,
                                  final RemoteProxySettings remoteProxySettings)
  {
    if (remoteProxySettings != null
        && remoteProxySettings.getHttpProxySettings() != null
        && remoteProxySettings.getHttpProxySettings().isEnabled()) {
      final Map<String, HttpHost> proxies = Maps.newHashMap();

      final HttpHost httpProxy = new HttpHost(
          remoteProxySettings.getHttpProxySettings().getHostname(),
          remoteProxySettings.getHttpProxySettings().getPort()
      );
      applyAuthenticationConfig(
          builder, remoteProxySettings.getHttpProxySettings().getProxyAuthentication(), httpProxy
      );

      log.debug(
          "http proxy setup with host '{}'", remoteProxySettings.getHttpProxySettings().getHostname()
      );
      proxies.put("http", httpProxy);
      proxies.put("https", httpProxy);

      if (remoteProxySettings.getHttpsProxySettings() != null
          && remoteProxySettings.getHttpsProxySettings().isEnabled()) {
        final HttpHost httpsProxy = new HttpHost(
            remoteProxySettings.getHttpsProxySettings().getHostname(),
            remoteProxySettings.getHttpsProxySettings().getPort()
        );
        applyAuthenticationConfig(
            builder, remoteProxySettings.getHttpsProxySettings().getProxyAuthentication(), httpsProxy
        );
        log.debug(
            "https proxy setup with host '{}'", remoteProxySettings.getHttpsProxySettings().getHostname()
        );
        proxies.put("https", httpsProxy);
      }

      final Set<Pattern> nonProxyHostPatterns = Sets.newHashSet();
      if (remoteProxySettings.getNonProxyHosts() != null && !remoteProxySettings.getNonProxyHosts().isEmpty()) {
        for (String nonProxyHostRegex : remoteProxySettings.getNonProxyHosts()) {
          try {
            nonProxyHostPatterns.add(Pattern.compile(nonProxyHostRegex, Pattern.CASE_INSENSITIVE));
          }
          catch (PatternSyntaxException e) {
            log.warn("Invalid non proxy host regex: {}", nonProxyHostRegex, e);
          }
        }
      }

      builder.getHttpClientBuilder().setRoutePlanner(
          new NexusHttpRoutePlanner(
              proxies, nonProxyHostPatterns, DefaultSchemePortResolver.INSTANCE
          )
      );
    }
  }
}
