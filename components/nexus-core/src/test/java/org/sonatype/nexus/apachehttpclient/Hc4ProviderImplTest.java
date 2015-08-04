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

import org.sonatype.nexus.apachehttpclient.Hc4Provider.Builder;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.repository.DefaultRemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.DefaultRemoteHttpProxySettings;
import org.sonatype.nexus.proxy.repository.DefaultRemoteProxySettings;
import org.sonatype.nexus.proxy.repository.NtlmRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteHttpProxySettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.repository.UsernamePasswordRemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.utils.UserAgentBuilder;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

public class Hc4ProviderImplTest
    extends TestSupport
{
  private Hc4ProviderImpl testSubject;

  @Mock
  private ApplicationConfiguration applicationConfiguration;

  @Mock
  private UserAgentBuilder userAgentBuilder;

  @Mock
  private EventBus eventBus;

  @Mock
  private RemoteStorageContext globalRemoteStorageContext;

  @Mock
  private RemoteProxySettings remoteProxySettings;

  @Mock
  private PoolingClientConnectionManagerMBeanInstaller jmxInstaller;

  @Before
  public void prepare() {
    final DefaultRemoteConnectionSettings rcs = new DefaultRemoteConnectionSettings();
    rcs.setConnectionTimeout(1234);
    when(globalRemoteStorageContext.getRemoteConnectionSettings()).thenReturn(rcs);
    when(globalRemoteStorageContext.getRemoteProxySettings()).thenReturn(remoteProxySettings);
    when(applicationConfiguration.getGlobalRemoteStorageContext()).thenReturn(globalRemoteStorageContext);
  }

  @Test
  @Ignore("DefaultHttpClient is not in use anymore")
  public void sharedInstanceConfigurationTest() {
    setParameters();
    try {
      testSubject = new Hc4ProviderImpl(applicationConfiguration, userAgentBuilder, eventBus, jmxInstaller, null);

      final HttpClient client = testSubject.createHttpClient();
      // Note: shared instance is shared across Nexus instance. It does not features connection pooling as
      // connections are
      // never reused intentionally

      // shared client does not reuse connections (no pool)
      Assert.assertTrue(((DefaultHttpClient) client).getConnectionReuseStrategy() instanceof NoConnectionReuseStrategy);
      Assert.assertTrue(((DefaultHttpClient) client).getConnectionManager() instanceof PoolingClientConnectionManager);

      // check is all set as needed: retries
      Assert.assertTrue(
          ((DefaultHttpClient) client).getHttpRequestRetryHandler() instanceof StandardHttpRequestRetryHandler);
      Assert.assertEquals(
          globalRemoteStorageContext.getRemoteConnectionSettings().getRetrievalRetryCount(),
          ((StandardHttpRequestRetryHandler) ((DefaultHttpClient) client).getHttpRequestRetryHandler())
              .getRetryCount());
      Assert.assertEquals(
          false,
          ((StandardHttpRequestRetryHandler) ((DefaultHttpClient) client).getHttpRequestRetryHandler())
              .isRequestSentRetryEnabled());

      // check is all set as needed: everything else
      Assert.assertEquals(1234L, client.getParams().getLongParameter(ClientPNames.CONN_MANAGER_TIMEOUT, 0));
      Assert.assertEquals(1234, client.getParams().getIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 0));
      Assert.assertEquals(1234, client.getParams().getIntParameter(HttpConnectionParams.SO_TIMEOUT, 0));
      Assert.assertEquals(1234, ((PoolingClientConnectionManager) client.getConnectionManager()).getMaxTotal());
      Assert.assertEquals(1234,
          ((PoolingClientConnectionManager) client.getConnectionManager()).getDefaultMaxPerRoute());
    }
    finally {
      testSubject.shutdown();
      unsetParameters();
    }
  }

  @Test
  @Ignore("DefaultHttpClient is not in use anymore")
  public void createdInstanceConfigurationTest() {
    setParameters();
    try {
      testSubject = new Hc4ProviderImpl(applicationConfiguration, userAgentBuilder, eventBus, jmxInstaller, null);

      // Note: explicitly created instance (like in case of proxies), it does pool and
      // returns customized client

      // we will reuse the "global" one, but this case is treated differently anyway by Hc4Provider
      final HttpClient client =
          testSubject.createHttpClient(applicationConfiguration.getGlobalRemoteStorageContext());
      // shared client does reuse connections (does pool)
      Assert.assertTrue(
          ((DefaultHttpClient) client).getConnectionReuseStrategy() instanceof DefaultConnectionReuseStrategy);
      Assert.assertTrue(((DefaultHttpClient) client).getConnectionManager() instanceof PoolingClientConnectionManager);

      // check is all set as needed: retries
      Assert.assertTrue(
          ((DefaultHttpClient) client).getHttpRequestRetryHandler() instanceof StandardHttpRequestRetryHandler);
      Assert.assertEquals(
          globalRemoteStorageContext.getRemoteConnectionSettings().getRetrievalRetryCount(),
          ((StandardHttpRequestRetryHandler) ((DefaultHttpClient) client).getHttpRequestRetryHandler())
              .getRetryCount());
      Assert.assertEquals(
          false,
          ((StandardHttpRequestRetryHandler) ((DefaultHttpClient) client).getHttpRequestRetryHandler())
              .isRequestSentRetryEnabled());

      // check is all set as needed: everything else
      Assert.assertEquals(1234L, client.getParams().getLongParameter(ClientPNames.CONN_MANAGER_TIMEOUT, 0));
      Assert.assertEquals(1234, client.getParams().getIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 0));
      Assert.assertEquals(1234, client.getParams().getIntParameter(HttpConnectionParams.SO_TIMEOUT, 0));
      final PoolingClientConnectionManager realConnMgr =
          (PoolingClientConnectionManager) client.getConnectionManager();
      Assert.assertEquals(1234, realConnMgr.getMaxTotal());
      Assert.assertEquals(1234, realConnMgr.getDefaultMaxPerRoute());
      client.getConnectionManager().shutdown();
    }
    finally {
      testSubject.shutdown();
      unsetParameters();
    }
  }

  @Test
  public void NEXUS6220_connectionReuse() {
    testSubject = new Hc4ProviderImpl(applicationConfiguration, userAgentBuilder, eventBus, jmxInstaller, null);

    // nothing NTLM related present
    {
      final DefaultRemoteStorageContext drsc = new DefaultRemoteStorageContext(null);
      Assert.assertFalse("No auth-proxy set", testSubject.reuseConnectionsNeeded(drsc));
    }

    // remote auth is NTLM
    {
      final DefaultRemoteStorageContext drsc = new DefaultRemoteStorageContext(null);
      drsc.setRemoteAuthenticationSettings(new NtlmRemoteAuthenticationSettings("a", "b", "c", "d"));
      Assert.assertTrue("NTLM target auth-proxy set", testSubject.reuseConnectionsNeeded(drsc));
    }

    // HTTP proxy is NTLM
    {
      final RemoteHttpProxySettings http = new DefaultRemoteHttpProxySettings();
      http.setProxyAuthentication(new NtlmRemoteAuthenticationSettings("a", "b", "c", "d"));
      final RemoteHttpProxySettings https = new DefaultRemoteHttpProxySettings();
      when(remoteProxySettings.getHttpProxySettings()).thenReturn(http);
      when(remoteProxySettings.getHttpsProxySettings()).thenReturn(https);
      Assert
          .assertTrue("NTLM HTTP proxy auth-proxy set",
              testSubject.reuseConnectionsNeeded(applicationConfiguration.getGlobalRemoteStorageContext()));
    }

    // HTTPS proxy is NTLM
    {
      final RemoteHttpProxySettings http = new DefaultRemoteHttpProxySettings();
      final RemoteHttpProxySettings https = new DefaultRemoteHttpProxySettings();
      https.setProxyAuthentication(new NtlmRemoteAuthenticationSettings("a", "b", "c", "d"));
      when(remoteProxySettings.getHttpProxySettings()).thenReturn(http);
      when(remoteProxySettings.getHttpsProxySettings()).thenReturn(https);
      Assert
          .assertTrue("NTLM HTTPS proxy auth-proxy set",
              testSubject.reuseConnectionsNeeded(applicationConfiguration.getGlobalRemoteStorageContext()));
    }
  }


  @Test
  public void credentialsProviderReplaced() {
    testSubject = new Hc4ProviderImpl(applicationConfiguration, userAgentBuilder, eventBus, jmxInstaller, null);

    final Builder builder = testSubject.prepareHttpClient(applicationConfiguration.getGlobalRemoteStorageContext());

    final RemoteAuthenticationSettings remoteAuthenticationSettings = new UsernamePasswordRemoteAuthenticationSettings(
        "user", "pass");
    testSubject.applyAuthenticationConfig(builder, remoteAuthenticationSettings, null);

    final DefaultRemoteHttpProxySettings httpProxy = new DefaultRemoteHttpProxySettings();
    httpProxy.setHostname("http-proxy");
    httpProxy.setPort(8080);
    httpProxy.setProxyAuthentication(new UsernamePasswordRemoteAuthenticationSettings("http-proxy", "http-pass"));
    final DefaultRemoteHttpProxySettings httpsProxy = new DefaultRemoteHttpProxySettings();
    httpsProxy.setHostname("https-proxy");
    httpsProxy.setPort(9090);
    httpsProxy.setProxyAuthentication(new UsernamePasswordRemoteAuthenticationSettings("https-proxy", "https-pass"));
    final DefaultRemoteProxySettings remoteProxySettings = new DefaultRemoteProxySettings();
    remoteProxySettings.setHttpProxySettings(httpProxy);
    remoteProxySettings.setHttpsProxySettings(httpsProxy);

    testSubject.applyProxyConfig(builder, remoteProxySettings);

    final CredentialsProvider credentialsProvider = builder.getCredentialsProvider();

    assertThat(credentialsProvider.getCredentials(AuthScope.ANY), notNullValue(Credentials.class));
    assertThat(credentialsProvider.getCredentials(AuthScope.ANY).getUserPrincipal().getName(), equalTo("user"));
    assertThat(credentialsProvider.getCredentials(AuthScope.ANY).getPassword(), equalTo("pass"));

    final AuthScope httpProxyAuthScope = new AuthScope(new HttpHost("http-proxy", 8080));
    assertThat(credentialsProvider.getCredentials(httpProxyAuthScope), notNullValue(Credentials.class));
    assertThat(credentialsProvider.getCredentials(httpProxyAuthScope).getUserPrincipal().getName(), equalTo("http-proxy"));
    assertThat(credentialsProvider.getCredentials(httpProxyAuthScope).getPassword(), equalTo("http-pass"));

    final AuthScope httpsProxyAuthScope = new AuthScope(new HttpHost("https-proxy", 9090));
    assertThat(credentialsProvider.getCredentials(httpsProxyAuthScope), notNullValue(Credentials.class));
    assertThat(credentialsProvider.getCredentials(httpsProxyAuthScope).getUserPrincipal().getName(), equalTo("https-proxy"));
    assertThat(credentialsProvider.getCredentials(httpsProxyAuthScope).getPassword(), equalTo("https-pass"));
  }

  // ==

  protected void setParameters() {
    System.setProperty("nexus.apacheHttpClient4x.connectionPoolMaxSize", "1234");
    System.setProperty("nexus.apacheHttpClient4x.connectionPoolSize", "1234");
    System.setProperty("nexus.apacheHttpClient4x.connectionPoolKeepalive", "1234");
    System.setProperty("nexus.apacheHttpClient4x.connectionPoolTimeout", "1234");
  }

  protected void unsetParameters() {
    System.clearProperty("nexus.apacheHttpClient4x.connectionPoolMaxSize");
    System.clearProperty("nexus.apacheHttpClient4x.connectionPoolSize");
    System.clearProperty("nexus.apacheHttpClient4x.connectionPoolKeepalive");
    System.clearProperty("nexus.apacheHttpClient4x.connectionPoolTimeout");
  }
}
