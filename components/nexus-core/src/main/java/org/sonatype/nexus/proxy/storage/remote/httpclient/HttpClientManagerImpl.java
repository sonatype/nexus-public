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
package org.sonatype.nexus.proxy.storage.remote.httpclient;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.apachehttpclient.Hc4Provider.Builder;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.remote.RemoteItemNotFoundException;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.utils.UserAgentBuilder;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of {@link HttpClientManager}.
 *
 * @author cstamas
 * @since 2.2
 */
@Singleton
@Named
public class HttpClientManagerImpl
    extends ComponentSupport
    implements HttpClientManager
{
  public static final String NX_REMOTE_ENABLE_CIRCULAR_REDIRECTS_KEY = "nexus.remoteStorage.enableCircularRedirectsForHosts";

  public static final String NX_REMOTE_USE_COOKIES_KEY = "nexus.remoteStorage.useCookiesForHosts";

  private final Hc4Provider hc4Provider;

  private final UserAgentBuilder userAgentBuilder;

  private final Set<String> enableCircularRedirectsForHosts;

  private final Set<String> useCookiesForHosts;

  /**
   * Constructor.
   *
   * @param hc4Provider      the {@link HttpClient} provider to be used with this manager.
   * @param userAgentBuilder the {@link UserAgentBuilder} component.
   */
  @Inject
  public HttpClientManagerImpl(final Hc4Provider hc4Provider, final UserAgentBuilder userAgentBuilder) {
    this.hc4Provider = checkNotNull(hc4Provider);
    this.userAgentBuilder = checkNotNull(userAgentBuilder);
    this.enableCircularRedirectsForHosts = HostnameHelper
        .parseAndNormalizeCsvProperty(NX_REMOTE_ENABLE_CIRCULAR_REDIRECTS_KEY);
    this.useCookiesForHosts = HostnameHelper.parseAndNormalizeCsvProperty(NX_REMOTE_USE_COOKIES_KEY);
  }

  @Override
  public HttpClient create(final ProxyRepository proxyRepository, final RemoteStorageContext ctx) {
    checkNotNull(proxyRepository);
    checkNotNull(ctx);
    final Builder builder = hc4Provider.prepareHttpClient(ctx);
    configure(proxyRepository, ctx, builder);
    return builder.build();
  }

  @Override
  public void release(final ProxyRepository proxyRepository, final RemoteStorageContext ctx) {
    // nop for now
  }

  /**
   * Configures the fresh instance of HttpClient for given proxy repository specific needs. Right now it sets
   * appropriate redirect strategy only.
   */
  protected void configure(final ProxyRepository proxyRepository, final RemoteStorageContext ctx,
                           final Builder builder)
  {
    // set UA, as Proxy reposes have different than the "generic" one set by Hc4Provider
    builder.getHttpClientBuilder().setUserAgent(userAgentBuilder.formatRemoteRepositoryStorageUserAgentString(proxyRepository, ctx));

    // set proxy redirect strategy
    builder.getHttpClientBuilder().setRedirectStrategy(getProxyRepositoryRedirectStrategy(proxyRepository, ctx));

    final String proxyHostName = HostnameHelper.normalizeHostname(proxyRepository);
    if (enableCircularRedirectsForHosts.contains(proxyHostName)) {
      log.info("Allowing circular redirects in proxy {}", proxyRepository);
      builder.getRequestConfigBuilder().setCircularRedirectsAllowed(true); // allow circular redirects
      builder.getRequestConfigBuilder().setMaxRedirects(10); // lessen max redirects from default 50
    }
    if (useCookiesForHosts.contains(proxyHostName)) {
      log.info("Allowing cookie use in proxy {}", proxyRepository);
      builder.getHttpClientBuilder().setDefaultCookieStore(new BasicCookieStore()); // in memory only
      builder.getRequestConfigBuilder().setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY); // emulate browsers
    }
  }

  /**
   * Returns {@link RedirectStrategy} used by proxy repository instances. This special strategy will kick in only
   * if Nexus performs content retrieval. In every other case (non-GET method or GET method used in remote
   * availability check) this strategy defaults to {@link DefaultRedirectStrategy} behavior.
   * <p/>In case of content retrieval, the "do not follow redirect to index pages (collections), but accept and follow
   * any other redirects" strategy kicks in. If index page redirect is detected (by checking the URL path for trailing
   * slash), redirection mechanism of HC4 is stopped, and hence, the response will return with redirect response code
   * (301, 302 or 307). These responses are handled within {@link HttpClientRemoteStorage} and is handled by
   * throwing a {@link RemoteItemNotFoundException}. Main goal of this {@link RedirectStrategy} is to save the
   * subsequent (the one following the redirect) request once we learn it would lead us to index page, as we
   * don't need index pages (hence, we do not fetch it only to throw it away).
   * <p/>
   * Usual problems are misconfiguration, where a repository published over HTTPS is configured with HTTP (ie.
   * admin mistyped the URL). Seemingly all work, but that is a source of performance issue, as every outgoing
   * Nexus request will "bounce", as usually HTTP port will redirect Nexus to HTTPS port, and then the artifact
   * will be fetched. Remedy for these scenarios is to edit the proxy repository configuration and update the
   * URL to proper protocol.
   * <p/>
   * This code <strong>assumes</strong> that remote repository is set up by best practices and common conventions,
   * hence, index page redirect means that target URL ends with slash. For more about this topic, read the
   * "To slash or not to slash" Google blog entry.
   *
   * @return the strategy to use with HC4 to follow redirects.
   * @see <a href="http://googlewebmastercentral.blogspot.hu/2010/04/to-slash-or-not-to-slash.html">To slash or not to
   * slash</a>
   */
  protected RedirectStrategy getProxyRepositoryRedirectStrategy(final ProxyRepository proxyRepository,
                                                                final RemoteStorageContext ctx)
  {
    // Prevent redirection to index pages
    return new DefaultRedirectStrategy()
    {
      @Override
      public boolean isRedirected(final HttpRequest request, final HttpResponse response, final HttpContext context)
          throws ProtocolException
      {
        final Logger logger = HttpClientRemoteStorage.outboundRequestLog;

        if (super.isRedirected(request, response, context)) {
          // code below comes from DefaultRedirectStrategy, as method super.getLocationURI cannot be used
          // since it modifies context state, and would result in false circular reference detection
          final Header locationHeader = response.getFirstHeader("location");
          if (locationHeader == null) {
            // got a redirect response, but no location header
            throw new ProtocolException("Received redirect response " + response.getStatusLine() +
                " from proxy " + proxyRepository + " but no location present");
          }

          boolean redirecting = true;

          final URI targetUri = createLocationURI(locationHeader.getValue());

          // this logic below should trigger only for content fetches made by RRS retrieveItem
          // hence, we do this ONLY if the HttpRequest is "marked" as such request
          if (Boolean.TRUE == context.getAttribute(HttpClientRemoteStorage.CONTENT_RETRIEVAL_MARKER_KEY)) {
            if (targetUri.getPath().endsWith("/")) {
              redirecting = false;
            }
          }

          // Additional verification when debugging...

          if (logger.isDebugEnabled()) {
            final String repoId = proxyRepository.getId();

            URI sourceUri = ((HttpUriRequest) request).getURI();
            if (!sourceUri.isAbsolute()) {
              try {
                sourceUri = URI.create(proxyRepository.getRemoteUrl()).resolve(sourceUri);
              }
              catch (Exception e) {
                logger.debug("[{}] Problem resolving {} against {}", repoId, sourceUri, proxyRepository.getRemoteUrl());
              }
            }

            final String sourceScheme = schemeOf(sourceUri);
            final String sourceHost = hostOf(sourceUri);

            final String targetScheme = schemeOf(targetUri);
            final String targetHost = hostOf(targetUri);

            final int redirectCode = response.getStatusLine().getStatusCode();

            // nag about redirection peculiarities, in any case
            if (!Objects.equals(sourceScheme, targetScheme)) {
              if ("http".equals(targetScheme)) {
                // security risk: HTTPS > HTTP downgrade, you are not safe as you think!
                logger.debug("[{}] Downgrade from HTTPS to HTTP during {} redirect {} -> {}", repoId, redirectCode,
                    sourceUri, targetUri);
              }
              else if ("https".equals(targetScheme) && Objects.equals(sourceHost, targetHost)) {
                // misconfiguration: your repository configured with wrong protocol and causes performance problems?
                logger.debug("[{}] Protocol upgrade during {} redirect on same host {} -> {}", repoId, redirectCode,
                    sourceUri, targetUri);
              }
            }

            if (redirecting) {
              logger.debug("[{}] Following {} redirect {} -> {}", repoId, redirectCode, sourceUri, targetUri);
            }
            else {
              logger.debug("[{}] Not following {} redirect {} -> {}", repoId, redirectCode, sourceUri, targetUri);
            }
          }

          return redirecting;
        }

        return false;
      }
    };
  }

  /**
   * Return the scheme of given uri in lower-case, or null if the scheme can not be determined.
   */
  @Nullable
  private static String schemeOf(final URI uri) {
    if (uri != null) {
      String scheme = uri.getScheme();
      if (scheme != null) {
        return scheme.toLowerCase(Locale.US);
      }
    }
    return null;
  }

  /**
   * Return host of given uri or null.
   */
  @Nullable
  private static String hostOf(final URI uri) {
    if (uri != null) {
      return uri.getHost();
    }
    return null;
  }
}
