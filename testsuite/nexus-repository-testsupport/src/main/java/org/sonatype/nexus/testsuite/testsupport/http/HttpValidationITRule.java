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
package org.sonatype.nexus.testsuite.testsupport.http;

import javax.inject.Provider;

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.goodies.httpfixture.validation.HttpValidator;
import org.sonatype.goodies.httpfixture.validation.ValidatingBehaviour;
import org.sonatype.goodies.httpfixture.validation.ValidatingProxyServer;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.common.net.PortAllocator;
import org.sonatype.nexus.httpclient.HttpClientManager;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Rule which supports easy re-use of an {@link Server} and {@link ValidatingProxyServer}.
 * Handles starting and stopping both servers before and after each test. Both the
 * {@link Server} and {@link ValidatingProxyServer} are owned by this rule, but the
 * upstream server's behavior can be modified by retrieving the {@link Server} with
 * {@link #getUpstreamServer()}. Also handles configuring NX3 to pass all http requests
 * through the proxy server, if present. Supports a global user agent suffix, configuring
 * that in NX3 as well.
 */
public class HttpValidationITRule
    extends ExternalResource
{

  private static final Logger log = LoggerFactory.getLogger(HttpValidationITRule.class);

  private final HttpConfigurationTestHelper configTestHelper;

  private final Server upstreamServer;

  private ValidatingProxyServer proxy;

  private String suffix;

  private int expectedProxyExecutionCount = -1;

  private int expectedUpstreamExecutionCount = -1;

  private ValidatingBehaviour upstreamValidatingBehaviour;

  public HttpValidationITRule(ValidatingBehaviour upstreamValidatingBehavior,
                              Provider<HttpClientManager> httpClientManagerProvider)
  {
    this.upstreamValidatingBehaviour = checkNotNull(upstreamValidatingBehavior);
    this.configTestHelper = new HttpConfigurationTestHelper(checkNotNull(httpClientManagerProvider));

    // Initialize the upstream server with basic validation behavior
    this.upstreamServer = Server.server().port(PortAllocator.nextFreePort()).serve("")
        .withBehaviours(upstreamValidatingBehavior);
  }

  @Override
  protected void before() throws Throwable {
    upstreamServer.start();
    if (proxy != null) {
      proxy.start();
    }
    configureNX3();
  }

  @Override
  protected void after() {
    try {
      upstreamServer.stop();
    }
    catch (Exception e) {
      log.error("Unable to stop the upstream server.", e);
    }
    if (proxy != null) {
      proxy.stop();
    }
    validateExecutionCounts();
    resetExecutionCounts();
  }

  /**
   * Reset expected counts, making sure that any previous requests aren't interfering with the current test.
   */
  public HttpValidationITRule reset() {
    resetExecutionCounts();
    expectedUpstreamExecutionCount = -1;
    expectedProxyExecutionCount = -1;

    return this;
  }

  /**
   * Create a {@link ValidatingProxyServer} that will be used by the rule.
   * 
   * @param validators The validator(s) to use with the {@link ValidatingProxyServer}.
   */
  public HttpValidationITRule withValidatingProxy(HttpValidator... validators) {
    checkArgument(validators != null && validators.length > 0, "Must have at least one validator.");
    this.proxy = new ValidatingProxyServer(validators).withPort(PortAllocator.nextFreePort());

    return this;
  }

  /**
   * Set to validate the number of times the upstream server was hit.
   */
  public HttpValidationITRule expectUpstreamExecutionCount(int count) {
    checkArgument(count > -1, "Upstream execution count must be greater than or equal to zero.");
    this.expectedUpstreamExecutionCount = count;

    return this;
  }

  /**
   * Set to validate the number of times the proxy server was hit. Note, for non-SSL, the proxy
   * will register two hits per request (one to connect to the proxy, a second for the actual request),
   * and for SSL, the proxy will only register the initial CONNECT request since the main request
   * goes over the encrypted channel setup by the inital CONNECT request.
   */
  public HttpValidationITRule expectProxyExecutionCount(int count) {
    checkArgument(count > -1, "Proxy execution count must be greater than or equal to zero.");
    this.expectedProxyExecutionCount = count;

    return this;
  }

  public HttpValidationITRule withGlobalUserAgentSuffix(String suffix) {
    checkArgument(!Strings2.isBlank(suffix), "User agent suffix must be a non-blank string.");
    this.suffix = suffix;

    return this;
  }

  private void validateExecutionCounts() {
    if (expectedUpstreamExecutionCount > -1) {
      checkNotNull(upstreamValidatingBehaviour);
      assertEquals("Upstream execution count invalid.", expectedUpstreamExecutionCount,
          upstreamValidatingBehaviour.getSuccessCount());
    }
    if (expectedProxyExecutionCount > -1) {
      assertEquals("Proxy execution count invalid.", expectedProxyExecutionCount, proxy.getSuccessCount());
    }
  }

  private void resetExecutionCounts() {
    proxy.resetSuccessCount();
    upstreamValidatingBehaviour.resetSuccessCount();
  }

  /**
   * <pre>
   * (1) Set both proxy and user agent suffix if present, together so only one config call is made.
   * (2) If only proxy is present, set the proxy alone.
   * (3) If only user agent suffix is present, set that alone.
   * </pre>
   */
  private void configureNX3() {
    boolean hasSuffix = !Strings2.isBlank(suffix);
    if (proxy != null) {
      if (hasSuffix) {
        configTestHelper.enableProxyWithUserAgentSuffix(proxy.getHostName(), proxy.getPort(), suffix);
      }
      else {
        configTestHelper.enableProxy(proxy.getHostName(), proxy.getPort());
      }
    }
    else if (hasSuffix) {
      configTestHelper.setGlobalUserAgentSuffix(suffix);
    }
  }

  public Server getUpstreamServer() {
    return upstreamServer;
  }

}
