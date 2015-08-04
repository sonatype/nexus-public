/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.tarball.internal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.util.DigesterUtils;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.proxy.NpmProxyRepository;
import com.bolyuba.nexus.plugin.npm.service.NpmBlob;
import com.bolyuba.nexus.plugin.npm.service.PackageVersion;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Apache HttpClient backed tarball transport implementation. It merely performs a HTTP GET and calculates response
 * body's SHA1 hash. No retries happen here (except of those at protocol level done by Apache HttpClient itself).
 */
@Singleton
@Named
public class HttpTarballTransport
    extends ComponentSupport
{
  /**
   * Using same log category as remote storage does, to simplify log reading.
   */
  private static final Logger outboundRequestLog = LoggerFactory.getLogger("remote.storage.outbound");

  /**
   * Set of HTTP response codes we do NOT want retries to happen:
   * <ul>
   * <li>404 - remote does not have it</li>
   * <li>401, 403 - probably user misconfiguration (remote authc needed or wrong)</li>
   * </ul>
   */
  private static final Set<Integer> NO_RETRIES_RESPONSE_CODES = ImmutableSet.of(404, 401, 403);

  private final Hc4Provider hc4Provider;

  private final MetricsRegistry metricsRegistry;

  @Inject
  public HttpTarballTransport(final Hc4Provider hc4Provider) {
    this.hc4Provider = checkNotNull(hc4Provider);
    this.metricsRegistry = Metrics.defaultRegistry();
  }

  public NpmBlob getTarballForVersion(final NpmProxyRepository npmProxyRepository, final File target,
                                      final PackageVersion packageVersion)
      throws IOException
  {
    final HttpClient httpClient = hc4Provider.createHttpClient(npmProxyRepository.getRemoteStorageContext());
    final HttpGet get = new HttpGet(packageVersion.getDistTarball());
    final HttpClientContext context = new HttpClientContext();
    context.setAttribute(Hc4Provider.HTTP_CTX_KEY_REPOSITORY, npmProxyRepository);
    get.addHeader("Accept", NpmRepository.TARBALL_MIME_TYPE);

    final Timer timer = timer(get, npmProxyRepository.getRemoteUrl());
    final TimerContext timerContext = timer.time();
    Stopwatch stopwatch = null;

    if (outboundRequestLog.isDebugEnabled()) {
      outboundRequestLog.debug("[{}] {} {}", npmProxyRepository.getId(), get.getMethod(), get.getURI());
      stopwatch = Stopwatch.createStarted();
    }

    final HttpResponse httpResponse;
    try {
      httpResponse = httpClient.execute(get, context);
    }
    finally {
      timerContext.stop();
      if (stopwatch != null) {
        stopwatch.stop();
      }
    }

    final StatusLine statusLine = httpResponse.getStatusLine();
    if (outboundRequestLog.isDebugEnabled()) {
      outboundRequestLog.debug("[{}] {} {} -> {}; {}", npmProxyRepository.getId(), get.getMethod(), get.getURI(),
          statusLine, stopwatch);
    }

    try {
      if (statusLine.getStatusCode() == HttpStatus.SC_OK && httpResponse.getEntity() != null) {
        final MessageDigest md = MessageDigest.getInstance("SHA1");
        try (final BufferedOutputStream bos = new BufferedOutputStream(
            new DigestOutputStream(new FileOutputStream(target), md))) {
          httpResponse.getEntity().writeTo(bos);
          bos.flush();
        }
        // TODO: Why are we sure about MIME type? Consult headers maybe?
        return new NpmBlob(target, NpmRepository.TARBALL_MIME_TYPE, packageVersion.getDistTarballFilename(),
            DigesterUtils.getDigestAsString(md.digest()));
      }
      else {
        if (log.isDebugEnabled()) {
          log.debug("[{}] {} {} -> unexpected: {}", npmProxyRepository.getId(), get.getMethod(), get.getURI(),
              statusLine);
        }
        if (NO_RETRIES_RESPONSE_CODES.contains(statusLine.getStatusCode())) {
          return null;
        }
        else {
          // in my experience, registry may throw many different errors, from 400, to 500 and then on next try
          // will happily serve the same URL! Hence, we do retry almost all of the responses that are not expected.
          throw new IOException("Unexpected response for 'GET " + get.getURI() + "': " + statusLine);
        }
      }
    }
    catch (NoSuchAlgorithmException e) {
      throw Throwables.propagate(e);
    }
    finally {
      EntityUtils.consumeQuietly(httpResponse.getEntity());
    }
  }

  private Timer timer(final HttpUriRequest httpRequest, final String baseUrl) {
    return metricsRegistry.newTimer(HttpTarballTransport.class, baseUrl, httpRequest.getMethod());
  }
}
