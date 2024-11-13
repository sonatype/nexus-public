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
package org.sonatype.nexus.internal.httpclient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ByteSize;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.httpclient.HttpClientPlan;
import org.sonatype.nexus.httpclient.HttpDefaultsCustomizer;
import org.sonatype.nexus.utils.httpclient.UserAgentGenerator;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Applies defaults to {@link HttpClientPlan}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class DefaultsCustomizer
  extends ComponentSupport
  implements HttpDefaultsCustomizer
{
  private final UserAgentGenerator userAgentGenerator;

  private final Time requestTimeout;

  private final Time connectionRequestTimeout;

  private final Time keepAliveDuration;

  private final ByteSize bufferSize;

  private final int retryCount;

  @Inject
  public DefaultsCustomizer(
      final UserAgentGenerator userAgentGenerator,
      @Named("${nexus.httpclient.requestTimeout:-20s}") final Time requestTimeout,
      @Named("${nexus.httpclient.connectionRequestTimeout:-30s}") final Time connectionRequestTimeout,
      @Named("${nexus.httpclient.keepAliveDuration:-30s}") final Time keepAliveDuration,
      @Named("${nexus.httpclient.bufferSize:-8k}") final ByteSize bufferSize,
      @Named("${nexus.httpclient.retryCount:-2}") final int retryCount)
  {
    this.userAgentGenerator = checkNotNull(userAgentGenerator);

    this.requestTimeout = checkNotNull(requestTimeout);
    log.debug("Request timeout: {}", requestTimeout);

    this.connectionRequestTimeout = checkNotNull(connectionRequestTimeout);
    log.debug("Connection request timeout: {}", connectionRequestTimeout);

    this.keepAliveDuration = checkNotNull(keepAliveDuration);
    log.debug("Keep-alive duration: {}", keepAliveDuration);

    this.bufferSize = checkNotNull(bufferSize);
    log.debug("Buffer-size: {}", bufferSize);

    this.retryCount = checkNotNull(retryCount);
  }

  @Override
  public void customize(final HttpClientPlan plan) {
    checkNotNull(plan);

    plan.setUserAgentBase(userAgentGenerator.generate());

    plan.getClient().setKeepAliveStrategy(new NexusConnectionKeepAliveStrategy(keepAliveDuration.toMillis()));
    plan.getClient().setRetryHandler(new StandardHttpRequestRetryHandler(retryCount, false));

    plan.getConnection().setBufferSize(bufferSize.toBytesI());

    plan.getRequest().setConnectionRequestTimeout(connectionRequestTimeout.toMillisI());
    plan.getRequest().setCookieSpec(CookieSpecs.IGNORE_COOKIES);
    plan.getRequest().setExpectContinueEnabled(false);

    int requestTimeoutMillis = requestTimeout.toMillisI();
    plan.getSocket().setSoTimeout(requestTimeoutMillis);
    plan.getRequest().setConnectTimeout(requestTimeoutMillis);
    plan.getRequest().setSocketTimeout(requestTimeoutMillis);
  }

  @Override
  public Time getRequestTimeout() {
    return requestTimeout;
  }

  @Override
  public int getRetryCount() {
    return retryCount;
  }
}
