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
package org.sonatype.nexus.testsuite;

import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import static org.sonatype.sisu.bl.BundleConfiguration.RANDOM_PORT;

/**
 * Support for Nexus-serving-HTTPS integration tests.
 *
 * @since 2.11.1
 */
public abstract class NexusHttpsITSupport
    extends NexusRunningParametrizedITSupport
{

  public NexusHttpsITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration config) {
    return super.configureNexus(config).enableHttps(RANDOM_PORT, testData().resolveFile("keystore.jks"), "changeit");
  }

  protected HttpClientBuilder clientBuilder() throws Exception {
    HttpClientBuilder builder = HttpClients.custom();
    builder.setDefaultRequestConfig(requestConfig());
    builder.setDefaultCredentialsProvider(credentialsProvider());
    builder.setSSLSocketFactory(sslSocketFactory());
    return builder;
  }

  protected RequestConfig requestConfig() {
    return RequestConfig.custom()
        .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY)
        .build();
  }

  protected CredentialsProvider credentialsProvider() {
    String hostname = nexus().getConfiguration().getHostName();
    AuthScope scope = new AuthScope(hostname, -1);
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(scope, credentials());
    return credentialsProvider;
  }

  protected Credentials credentials() {
    return new UsernamePasswordCredentials("admin", "admin123");
  }

  protected SSLConnectionSocketFactory sslSocketFactory() throws Exception {
    SSLContext context = SSLContexts.custom().loadTrustMaterial(trustStore(), new TrustSelfSignedStrategy()).build();
    return new SSLConnectionSocketFactory(context, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
  }

  protected KeyStore trustStore() throws Exception {
    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (FileInputStream instream = new FileInputStream(testData().resolveFile("truststore.jks"))) {
      trustStore.load(instream, "changeit".toCharArray());
    }
    return trustStore;
  }

  protected HttpClientContext clientContext() {
    HttpClientContext context = HttpClientContext.create();
    context.setAuthCache(basicAuthCache());
    return context;
  }

  protected AuthCache basicAuthCache() {
    String hostname = nexus().getConfiguration().getHostName();
    AuthCache authCache = new BasicAuthCache();
    HttpHost hostHttp = new HttpHost(hostname, nexus().getPort(), "http");
    HttpHost hostHttps = new HttpHost(hostname, nexus().getSslPort(), "https");
    authCache.put(hostHttp, new BasicScheme());
    authCache.put(hostHttps, new BasicScheme());
    return authCache;
  }

  @Nullable
  protected Cookie getSessionCookie(CookieStore cookieStore) {
    for (Cookie cookie : cookieStore.getCookies()) {
      if ("NXSESSIONID".equals(cookie.getName())) {
        return cookie;
      }
    }
    return null;
  }

  /**
   * @return CookieOrigin suitable for validating cookies from the Nexus URL
   */
  public static CookieOrigin cookieOrigin(final URL nexusUrl) {
    return new CookieOrigin(nexusUrl.getHost(), nexusUrl.getPort(),
        expectedCookiePath(nexusUrl), nexusUrl.getProtocol().equals("https"));
  }

  /**
   * @return the expected cookie path value of our session cookie from the provided Nexus origin base URL
   */
  public static String expectedCookiePath(final URL nexusUrl) {
    return nexusUrl.getPath().substring(0, nexusUrl.getPath().length() - 1);
  }


}
