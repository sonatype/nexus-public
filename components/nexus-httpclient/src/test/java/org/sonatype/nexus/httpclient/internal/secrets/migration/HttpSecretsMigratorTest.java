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
package org.sonatype.nexus.httpclient.internal.secrets.migration;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.BearerTokenAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;
import org.sonatype.nexus.security.UserIdHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.httpclient.config.AuthenticationConfiguration.AUTHENTICATION_CONFIGURATION;

public class HttpSecretsMigratorTest
    extends TestSupport
{
  @Mock
  private HttpClientManager httpClientManager;

  @Mock
  private SecretsService secretsService;

  private MockedStatic<UserIdHelper> userIdHelperMock;

  private HttpSecretsMigrator underTest;

  @Before
  public void setUp() {
    userIdHelperMock = mockStatic(UserIdHelper.class);
    userIdHelperMock.when(UserIdHelper::get).thenReturn("system");
    underTest = new HttpSecretsMigrator(httpClientManager, secretsService);
  }

  @After
  public void teardown() {
    userIdHelperMock.close();
  }

  @Test
  public void testMigratesConfiguration() {
    HttpClientConfiguration configuration = mockConfiguration(true, true, true);
    when(httpClientManager.getConfiguration()).thenReturn(configuration);

    underTest.migrate();

    verify(httpClientManager).getConfiguration();

    //verify we updated global auth
    verify((UsernameAuthenticationConfiguration) configuration.getAuthentication(), times(1)).setPassword(
        any(Secret.class));

    //verify we updated proxy http auth
    verify((NtlmAuthenticationConfiguration) configuration.getProxy().getHttp().getAuthentication(),
        times(1)).setPassword(any(Secret.class));

    //verify we updated proxy https auth
    verify((BearerTokenAuthenticationConfiguration) configuration.getProxy().getHttps().getAuthentication(),
        times(1)).setBearerToken(any(Secret.class));

    //verify we updated the client manager afterward
    verify(httpClientManager).setConfiguration(configuration);
  }

  @Test
  public void testMigratesPartialConfiguration() {
    HttpClientConfiguration configuration = mockConfiguration(true, false, true);
    when(httpClientManager.getConfiguration()).thenReturn(configuration);

    underTest.migrate();

    verify(httpClientManager).getConfiguration();

    //verify we updated global auth
    verify((UsernameAuthenticationConfiguration) configuration.getAuthentication(), times(1)).setPassword(
        any(Secret.class));
    //verify we updated proxy https auth
    verify((BearerTokenAuthenticationConfiguration) configuration.getProxy().getHttps().getAuthentication(),
        times(1)).setBearerToken(any(Secret.class));

    //verify we updated the client manager afterward
    verify(httpClientManager).setConfiguration(configuration);
  }

  @Test
  public void testNoMigrationIfNullConfig() {
    when(httpClientManager.getConfiguration()).thenReturn(null);

    underTest.migrate();

    verify(httpClientManager, never()).setConfiguration(any(HttpClientConfiguration.class));
  }

  private HttpClientConfiguration mockConfiguration(
      final boolean includeAuth,
      final boolean includeHttpProxyAuth,
      final boolean includeHttpsProxyAuth)
  {
    HttpClientConfiguration configuration = mock(HttpClientConfiguration.class);

    if (includeAuth) {
      //username auth
      UsernameAuthenticationConfiguration globalAuth = mock(UsernameAuthenticationConfiguration.class);
      Secret passwordSecret = getMockSecret("global-test-password");
      when(globalAuth.getSecret()).thenReturn(passwordSecret);
      when(passwordSecret.getId()).thenReturn("global-test-password");
      Secret migratedPasswordSecret = getMockSecret("global-test-password");
      when(secretsService.encrypt(AUTHENTICATION_CONFIGURATION, "global-test-password".toCharArray(), "system"))
          .thenReturn(migratedPasswordSecret);
      when(configuration.getAuthentication()).thenReturn(globalAuth);
    }

    ProxyConfiguration proxyConfiguration = mock(ProxyConfiguration.class);
    ProxyServerConfiguration http = mock(ProxyServerConfiguration.class);
    ProxyServerConfiguration https = mock(ProxyServerConfiguration.class);

    when(proxyConfiguration.getHttp()).thenReturn(http);
    when(proxyConfiguration.getHttps()).thenReturn(https);
    when(configuration.getProxy()).thenReturn(proxyConfiguration);

    if (includeHttpProxyAuth) {
      //ntlm auth
      NtlmAuthenticationConfiguration httpProxyAuth = mock(NtlmAuthenticationConfiguration.class);
      Secret passwordSecret = getMockSecret("http-test-password");
      when(httpProxyAuth.getSecret()).thenReturn(passwordSecret);
      when(passwordSecret.getId()).thenReturn("http-test-password");
      Secret migratedPasswordSecret = getMockSecret("http-test-password");
      when(secretsService.encrypt(AUTHENTICATION_CONFIGURATION, "http-test-password".toCharArray(), "system"))
          .thenReturn(migratedPasswordSecret);
      when(http.getAuthentication()).thenReturn(httpProxyAuth);
    }

    if (includeHttpsProxyAuth) {
      //bearer token auth
      BearerTokenAuthenticationConfiguration httpsProxyAuth = mock(BearerTokenAuthenticationConfiguration.class);
      Secret tokenSecret = getMockSecret("https-test-password");
      when(httpsProxyAuth.getSecret()).thenReturn(tokenSecret);
      when(tokenSecret.getId()).thenReturn("https-test-password");
      Secret migratedPasswordSecret = getMockSecret("https-test-password");
      when(secretsService.encrypt(AUTHENTICATION_CONFIGURATION, "https-test-password".toCharArray(), "system"))
          .thenReturn(migratedPasswordSecret);
      when(https.getAuthentication()).thenReturn(httpsProxyAuth);
    }

    return configuration;
  }

  private Secret getMockSecret(String token) {
    Secret secret = mock(Secret.class);
    when(secret.decrypt()).thenReturn(token.toCharArray());
    return secret;
  }
}
