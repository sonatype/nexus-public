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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.routing.discovery.RemoteStrategy;
import org.sonatype.nexus.proxy.maven.routing.discovery.StrategyFailedException;
import org.sonatype.nexus.proxy.maven.routing.discovery.StrategyResult;
import org.sonatype.nexus.proxy.storage.remote.httpclient.HttpClientManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract class for {@link RemoteStrategy} implementations that is using HC4, see {@link Hc4Provider}.
 * 
 * @author cstamas
 * @since 2.7.0
 */
public abstract class AbstractHttpRemoteStrategy
    extends AbstractRemoteStrategy
{
  private final HttpClientManager httpClientManager;

  protected AbstractHttpRemoteStrategy(final int priority, final String id, final HttpClientManager httpClientManager) {
    super(priority, id);
    this.httpClientManager = checkNotNull(httpClientManager);
  }

  protected String getRemoteUrlOf(final MavenProxyRepository mavenProxyRepository) throws MalformedURLException {
    final String remoteRepositoryRootUrl = mavenProxyRepository.getRemoteUrl();
    final URL remoteUrl = new URL(remoteRepositoryRootUrl);
    if (!"http".equalsIgnoreCase(remoteUrl.getProtocol()) && !"https".equalsIgnoreCase(remoteUrl.getProtocol())) {
      throw new MalformedURLException("URL protocol unsupported: " + remoteRepositoryRootUrl);
    }
    return remoteRepositoryRootUrl;
  }

  /**
   * Creates a client configured in same way as RRS of it.
   */
  protected HttpClient createHttpClientFor(final MavenProxyRepository mavenProxyRepository) {
    final HttpClient client = httpClientManager.create(mavenProxyRepository,
        mavenProxyRepository.getRemoteStorageContext());
    return client;
  }

  /**
   * Returns {@code true} if remote server (proxies by {@link MavenProxyRepository}) is recognized as server that MUST
   * NOT be trusted for any automatic routing feature.
   * 
   * @throws StrategyFailedException if server is recognized as blacklisted.
   */
  protected void checkIsBlacklistedRemoteServer(final MavenProxyRepository mavenProxyRepository)
      throws StrategyFailedException, IOException
  {
    // check URL first, we currently test HTTP and HTTPS only for blacklist, if not, just skip this
    // but do not report blacklist at all (nor attempt)
    final String remoteUrl;
    try {
      remoteUrl = getRemoteUrlOf(mavenProxyRepository);
    }
    catch (MalformedURLException e) {
      // non HTTP/HTTPS, just return
      return;
    }
    final HttpClient httpClient = createHttpClientFor(mavenProxyRepository);
    {
      // NEXUS-5849: Artifactory will happily serve Central prefixes, effectively shading all the other artifacts from
      // it's group
      final HttpGet get = new HttpGet(remoteUrl);
      final BasicHttpContext httpContext = new BasicHttpContext();
      httpContext.setAttribute(Hc4Provider.HTTP_CTX_KEY_REPOSITORY, mavenProxyRepository);
      final HttpResponse response  = httpClient.execute(get, httpContext);

      try {
        if (response.containsHeader("X-Artifactory-Id")) {
          log.debug("Remote server of proxy {} recognized as ARTF by response header", mavenProxyRepository);
          throw new StrategyFailedException("Server proxied by " + mavenProxyRepository
              + " proxy repository is not supported by automatic routing discovery");
        }
        if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 499) {
          if (response.getEntity() != null) {
            final Document document = Jsoup.parse(response.getEntity().getContent(), null, remoteUrl);
            final Elements addressElements = document.getElementsByTag("address");
            if (!addressElements.isEmpty()) {
              final String addressText = addressElements.get(0).text();
              if (addressText != null && addressText.toLowerCase(Locale.ENGLISH).startsWith("artifactory")) {
                log.debug("Remote server of proxy {} recognized as ARTF by address element in body",
                    mavenProxyRepository);
                throw new StrategyFailedException("Server proxied by " + mavenProxyRepository
                    + " proxy repository is not supported by automatic routing discovery");
              }
            }
          }
        }
      }
      finally {
        EntityUtils.consumeQuietly(response.getEntity());
      }
    }
  }

  @Override
  public StrategyResult discover(final MavenProxyRepository mavenProxyRepository) throws StrategyFailedException,
      IOException
  {
    checkIsBlacklistedRemoteServer(mavenProxyRepository);
    return doDiscover(mavenProxyRepository);
  }

  protected abstract StrategyResult doDiscover(final MavenProxyRepository mavenProxyRepository)
      throws StrategyFailedException, IOException;
}
