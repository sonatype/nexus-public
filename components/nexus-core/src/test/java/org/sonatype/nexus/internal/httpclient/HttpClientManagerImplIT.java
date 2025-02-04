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

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.goodies.httpfixture.validation.ValidatingBehaviour;
import org.sonatype.goodies.httpfixture.validation.ValidatingProxyServer;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.testcommon.validation.HeaderValidator;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;

/**
 * Small-scale integration tests for {@link HttpClientManagerImpl}.
 */
public class HttpClientManagerImplIT
{
  private static final String SUFFIX = "my user agent suffix";

  private static final String EXPECTED_USER_AGENT = String.format(
      "Nexus/1234.5678.910-345 (OSS; %s; %s; %s; %s) " + SUFFIX,
      System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"),
      System.getProperty("java.version"));

  private static Server targetServerSSL;

  private static Server targetServer;

  private static HeaderValidator headerValidator = new HeaderValidator(SUFFIX);

  private static ValidatingProxyServer proxyServer = new ValidatingProxyServer(headerValidator);

  private static ValidatingBehaviour httpValidatingBehaviour = new ValidatingBehaviour(headerValidator);

  private static ValidatingBehaviour httpsValidatingBehaviour = new ValidatingBehaviour(headerValidator);

  private HttpClientManagerImpl underTest;

  @BeforeClass
  public static void beforeClass() throws Exception {
    targetServerSSL = createHeaderValidatingServerSSL().start();
    targetServer = createHeaderValidatingServer().start();
    proxyServer.start();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    targetServer.stop();
    targetServerSSL.stop();
    proxyServer.stop();
  }

  @Before
  public void before() {
    underTest = new HttpClientManagerImpl(mock(EventManager.class), mock(HttpClientConfigurationStore.class),
        () -> mock(HttpClientConfiguration.class), mock(SharedHttpClientConnectionManager.class),
        mock(DefaultsCustomizer.class));

    resetCounts();
  }

  @Test
  public void testPrepareHttpClientBuilderHttpRequestWithProxy() throws Exception {
    testPrepareHttpClientBuilderHttpRequest(false, true);
  }

  @Test
  public void testPrepareHttpClientBuilderHttpRequestWithProxySSL() throws Exception {
    testPrepareHttpClientBuilderHttpRequest(true, true);
  }

  @Test
  public void testPrepareHttpClientBuilderHttpRequestNoProxySSL() throws Exception {
    testPrepareHttpClientBuilderHttpRequest(true, false);
  }

  @Test
  public void testPrepareHttpClientBuilderHttpRequestNoProxy() throws Exception {
    testPrepareHttpClientBuilderHttpRequest(false, false);
  }

  private void testPrepareHttpClientBuilderHttpRequest(boolean isSSL, boolean isProxy) throws Exception {
    // Setup
    HttpClientBuilder builder = underTest.prepare(plan -> plan.setUserAgentBase(EXPECTED_USER_AGENT));
    builder.setConnectionManager(null);
    if (isProxy) {
      builder.setProxy(new HttpHost(proxyServer.getHostName(), proxyServer.getPort()));
    }
    if (isSSL) {
      setSSL(builder);
    }

    String url;
    if (isSSL) {
      url = "https://" + targetServerSSL.getUrl().getHost() + ":" + targetServerSSL.getPort();
    }
    else {
      url = "http://" + targetServer.getUrl().getHost() + ":" + targetServer.getPort();
    }

    CloseableHttpResponse resp = null;
    // Execute
    try (CloseableHttpClient client = builder.build()) {
      resp = client.execute(new HttpGet(new URI(url)));
    }
    finally {
      if (resp != null) {
        resp.close();
      }
    }

    // Verify
    assertThat(resp.getStatusLine().getStatusCode(), equalTo(HttpStatus.OK));
    if (isSSL) {
      assertThat(httpsValidatingBehaviour.getSuccessCount(), equalTo(1));
    }
    else {
      assertThat(httpValidatingBehaviour.getSuccessCount(), equalTo(1));
    }
    if (isProxy) {
      if (isSSL) {
        // Only one filterable request in SSL (CONNECT) without using MITM
        assertThat(proxyServer.getSuccessCount(), equalTo(1));
      }
      else {
        // Two filterable requests in non-SSL (initiate and real request)
        assertThat(proxyServer.getSuccessCount(), equalTo(2));
      }
    }
  }

  private void setSSL(
      HttpClientBuilder builder) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException
  {
    SSLContext sslContext = SSLContexts.custom()
        .loadTrustMaterial(this.getClass().getClassLoader().getResource("testkeystore"), "password".toCharArray(),
            new TrustSelfSignedStrategy())
        .build();
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new String[]{"TLSv1.2"}, null,
        SSLConnectionSocketFactory.getDefaultHostnameVerifier());
    builder.setSSLSocketFactory(sslsf);
  }

  private static Server createHeaderValidatingServerSSL() {
    return Server.server()
        .withKeystore(HttpClientManagerImplIT.class.getClassLoader().getResource("testkeystore").getFile(), "password")
        .serve("")
        .withBehaviours(httpsValidatingBehaviour);
  }

  private static Server createHeaderValidatingServer() {
    return Server.server().serve("").withBehaviours(httpValidatingBehaviour);
  }

  private void resetCounts() {
    httpValidatingBehaviour.resetSuccessCount();
    httpsValidatingBehaviour.resetSuccessCount();
    proxyServer.resetSuccessCount();
  }
}
