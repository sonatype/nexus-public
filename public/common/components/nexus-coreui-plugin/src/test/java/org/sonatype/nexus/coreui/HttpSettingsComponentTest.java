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
package org.sonatype.nexus.coreui;

import java.util.Set;

import javax.validation.Validator;

import org.sonatype.goodies.common.Time;
import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link HttpSettingsComponent}.
 */
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class HttpSettingsComponentTest
    extends Test5Support
{
  @ValidationExecutor
  private final Validator validator =
      new ValidationConfiguration().validatorFactory(new ConstraintValidatorFactoryImpl()).getValidator();

  @Mock
  private HttpClientManager httpClientManager;

  @Mock
  private SecretsService secretsService;

  private HttpSettingsComponent underTest;

  @BeforeEach
  void setUp() {
    underTest = new HttpSettingsComponent(httpClientManager, secretsService);
  }

  @Test
  void testRead_emptyConfiguration() {
    HttpClientConfiguration config = mock(HttpClientConfiguration.class);
    when(httpClientManager.getConfiguration()).thenReturn(config);

    HttpSettingsXO result = underTest.read();

    assertThat(result, is(notNullValue()));
    assertThat(result.getUserAgentSuffix(), is(nullValue()));
    assertThat(result.getTimeout(), is(nullValue()));
    assertThat(result.getRetries(), is(nullValue()));
    assertThat(result.getHttpEnabled(), is(nullValue()));
    assertThat(result.getHttpsEnabled(), is(nullValue()));
    assertThat(result.getNonProxyHosts(), is(nullValue()));
  }

  @Test
  void testRead_withConnectionConfiguration() {
    HttpClientConfiguration config = mock(HttpClientConfiguration.class);
    ConnectionConfiguration connection = new ConnectionConfiguration();
    connection.setUserAgentSuffix("test-agent");
    connection.setTimeout(Time.seconds(30));
    connection.setRetries(3);
    when(config.getConnection()).thenReturn(connection);
    when(httpClientManager.getConfiguration()).thenReturn(config);

    HttpSettingsXO result = underTest.read();

    assertThat(result.getUserAgentSuffix(), is("test-agent"));
    assertThat(result.getTimeout(), is(30));
    assertThat(result.getRetries(), is(3));
  }

  @Test
  void testRead_withConnectionNullTimeout() {
    HttpClientConfiguration config = mock(HttpClientConfiguration.class);
    ConnectionConfiguration connection = new ConnectionConfiguration();
    connection.setUserAgentSuffix("agent");
    when(config.getConnection()).thenReturn(connection);
    when(httpClientManager.getConfiguration()).thenReturn(config);

    HttpSettingsXO result = underTest.read();

    assertThat(result.getUserAgentSuffix(), is("agent"));
    assertThat(result.getTimeout(), is(nullValue()));
  }

  @Test
  void testRead_withHttpProxy_usernameAuth() {
    HttpClientConfiguration config = mock(HttpClientConfiguration.class);
    ProxyConfiguration proxy = new ProxyConfiguration();
    ProxyServerConfiguration httpProxy = new ProxyServerConfiguration();
    httpProxy.setEnabled(true);
    httpProxy.setHost("proxy.example.com");
    httpProxy.setPort(8080);

    UsernameAuthenticationConfiguration auth = new UsernameAuthenticationConfiguration();
    auth.setUsername("user");
    Secret passwordSecret = mock(Secret.class);
    auth.setPassword(passwordSecret);
    httpProxy.setAuthentication(auth);

    proxy.setHttp(httpProxy);
    when(config.getProxy()).thenReturn(proxy);
    when(httpClientManager.getConfiguration()).thenReturn(config);

    HttpSettingsXO result = underTest.read();

    assertThat(result.getHttpEnabled(), is(true));
    assertThat(result.getHttpHost(), is("proxy.example.com"));
    assertThat(result.getHttpPort(), is(8080));
    assertThat(result.getHttpAuthEnabled(), is(true));
    assertThat(result.getHttpAuthUsername(), is("user"));
    assertThat(result.getHttpAuthPassword(), is(PasswordPlaceholder.get()));
  }

  @Test
  void testRead_withHttpProxy_ntlmAuth() {
    HttpClientConfiguration config = mock(HttpClientConfiguration.class);
    ProxyConfiguration proxy = new ProxyConfiguration();
    ProxyServerConfiguration httpProxy = new ProxyServerConfiguration();
    httpProxy.setEnabled(true);
    httpProxy.setHost("ntlm-proxy.example.com");
    httpProxy.setPort(3128);

    NtlmAuthenticationConfiguration auth = new NtlmAuthenticationConfiguration();
    auth.setUsername("ntlmuser");
    Secret passwordSecret = mock(Secret.class);
    auth.setPassword(passwordSecret);
    auth.setHost("ntlmhost");
    auth.setDomain("DOMAIN");
    httpProxy.setAuthentication(auth);

    proxy.setHttp(httpProxy);
    when(config.getProxy()).thenReturn(proxy);
    when(httpClientManager.getConfiguration()).thenReturn(config);

    HttpSettingsXO result = underTest.read();

    assertThat(result.getHttpAuthEnabled(), is(true));
    assertThat(result.getHttpAuthUsername(), is("ntlmuser"));
    assertThat(result.getHttpAuthPassword(), is(PasswordPlaceholder.get()));
    assertThat(result.getHttpAuthNtlmHost(), is("ntlmhost"));
    assertThat(result.getHttpAuthNtlmDomain(), is("DOMAIN"));
  }

  @Test
  void testRead_withHttpsProxy_usernameAuth() {
    HttpClientConfiguration config = mock(HttpClientConfiguration.class);
    ProxyConfiguration proxy = new ProxyConfiguration();
    ProxyServerConfiguration httpsProxy = new ProxyServerConfiguration();
    httpsProxy.setEnabled(true);
    httpsProxy.setHost("https-proxy.example.com");
    httpsProxy.setPort(8443);

    UsernameAuthenticationConfiguration auth = new UsernameAuthenticationConfiguration();
    auth.setUsername("httpsuser");
    Secret passwordSecret = mock(Secret.class);
    auth.setPassword(passwordSecret);
    httpsProxy.setAuthentication(auth);

    proxy.setHttps(httpsProxy);
    when(config.getProxy()).thenReturn(proxy);
    when(httpClientManager.getConfiguration()).thenReturn(config);

    HttpSettingsXO result = underTest.read();

    assertThat(result.getHttpsEnabled(), is(true));
    assertThat(result.getHttpsHost(), is("https-proxy.example.com"));
    assertThat(result.getHttpsPort(), is(8443));
    assertThat(result.getHttpsAuthEnabled(), is(true));
    assertThat(result.getHttpsAuthUsername(), is("httpsuser"));
    assertThat(result.getHttpsAuthPassword(), is(PasswordPlaceholder.get()));
  }

  @Test
  void testRead_withHttpsProxy_ntlmAuth() {
    HttpClientConfiguration config = mock(HttpClientConfiguration.class);
    ProxyConfiguration proxy = new ProxyConfiguration();
    ProxyServerConfiguration httpsProxy = new ProxyServerConfiguration();
    httpsProxy.setEnabled(true);
    httpsProxy.setHost("ntlm-https.example.com");
    httpsProxy.setPort(9443);

    NtlmAuthenticationConfiguration auth = new NtlmAuthenticationConfiguration();
    auth.setUsername("ntlmhttpsuser");
    Secret passwordSecret = mock(Secret.class);
    auth.setPassword(passwordSecret);
    auth.setHost("ntlmhost2");
    auth.setDomain("DOMAIN2");
    httpsProxy.setAuthentication(auth);

    proxy.setHttps(httpsProxy);
    when(config.getProxy()).thenReturn(proxy);
    when(httpClientManager.getConfiguration()).thenReturn(config);

    HttpSettingsXO result = underTest.read();

    assertThat(result.getHttpsAuthEnabled(), is(true));
    assertThat(result.getHttpsAuthUsername(), is("ntlmhttpsuser"));
    assertThat(result.getHttpsAuthPassword(), is(PasswordPlaceholder.get()));
    assertThat(result.getHttpsAuthNtlmHost(), is("ntlmhost2"));
    assertThat(result.getHttpsAuthNtlmDomain(), is("DOMAIN2"));
  }

  @Test
  void testRead_withNonProxyHosts() {
    HttpClientConfiguration config = mock(HttpClientConfiguration.class);
    ProxyConfiguration proxy = new ProxyConfiguration();
    proxy.setNonProxyHosts(new String[]{"localhost", "*.internal.com"});
    when(config.getProxy()).thenReturn(proxy);
    when(httpClientManager.getConfiguration()).thenReturn(config);

    HttpSettingsXO result = underTest.read();

    assertThat(result.getNonProxyHosts(), containsInAnyOrder("localhost", "*.internal.com"));
  }

  @Test
  void testUpdate_connectionSettings() {
    HttpClientConfiguration previousConfig = mock(HttpClientConfiguration.class);
    HttpClientConfiguration newConfig = mock(HttpClientConfiguration.class);
    ConnectionConfiguration connection = new ConnectionConfiguration();
    when(newConfig.getConnection()).thenReturn(connection);
    when(httpClientManager.getConfiguration()).thenReturn(previousConfig);
    when(httpClientManager.newConfiguration()).thenReturn(newConfig);

    HttpSettingsXO settings = new HttpSettingsXO();
    settings.setUserAgentSuffix("my-agent");
    settings.setTimeout(60);
    settings.setRetries(5);

    underTest.update(settings);

    verify(httpClientManager).setConfiguration(newConfig);
    assertThat(connection.getUserAgentSuffix(), is("my-agent"));
    assertThat(connection.getTimeout().toSecondsI(), is(60));
    assertThat(connection.getRetries(), is(5));
  }

  @Test
  void testUpdate_withHttpProxyEnabled_usernameAuth() {
    HttpClientConfiguration previousConfig = mock(HttpClientConfiguration.class);
    HttpClientConfiguration newConfig = mock(HttpClientConfiguration.class);
    ProxyConfiguration proxyConfig = new ProxyConfiguration();
    when(newConfig.getProxy()).thenReturn(proxyConfig);
    when(httpClientManager.getConfiguration()).thenReturn(previousConfig);
    when(httpClientManager.newConfiguration()).thenReturn(newConfig);

    Secret encryptedSecret = mock(Secret.class);
    when(secretsService.encryptMaven(any(), any(), any())).thenReturn(encryptedSecret);

    HttpSettingsXO settings = new HttpSettingsXO();
    settings.setHttpEnabled(true);
    settings.setHttpHost("proxy.example.com");
    settings.setHttpPort(8080);
    settings.setHttpAuthEnabled(true);
    settings.setHttpAuthUsername("proxyuser");
    settings.setHttpAuthPassword("proxypass");

    underTest.update(settings);

    verify(httpClientManager).setConfiguration(newConfig);
    ProxyServerConfiguration httpProxy = proxyConfig.getHttp();
    assertThat(httpProxy, is(notNullValue()));
    assertThat(httpProxy.isEnabled(), is(true));
    assertThat(httpProxy.getHost(), is("proxy.example.com"));
    assertThat(httpProxy.getPort(), is(8080));
    assertThat(httpProxy.getAuthentication(), is(notNullValue()));
    assertThat(httpProxy.getAuthentication() instanceof UsernameAuthenticationConfiguration, is(true));
  }

  @Test
  void testUpdate_withHttpProxyEnabled_ntlmAuth() {
    HttpClientConfiguration previousConfig = mock(HttpClientConfiguration.class);
    HttpClientConfiguration newConfig = mock(HttpClientConfiguration.class);
    ProxyConfiguration proxyConfig = new ProxyConfiguration();
    when(newConfig.getProxy()).thenReturn(proxyConfig);
    when(httpClientManager.getConfiguration()).thenReturn(previousConfig);
    when(httpClientManager.newConfiguration()).thenReturn(newConfig);

    Secret encryptedSecret = mock(Secret.class);
    when(secretsService.encryptMaven(any(), any(), any())).thenReturn(encryptedSecret);

    HttpSettingsXO settings = new HttpSettingsXO();
    settings.setHttpEnabled(true);
    settings.setHttpHost("proxy.example.com");
    settings.setHttpPort(8080);
    settings.setHttpAuthEnabled(true);
    settings.setHttpAuthUsername("ntlmuser");
    settings.setHttpAuthPassword("ntlmpass");
    settings.setHttpAuthNtlmHost("ntlmhost");
    settings.setHttpAuthNtlmDomain("DOMAIN");

    underTest.update(settings);

    verify(httpClientManager).setConfiguration(newConfig);
    ProxyServerConfiguration httpProxy = proxyConfig.getHttp();
    assertThat(httpProxy.getAuthentication() instanceof NtlmAuthenticationConfiguration, is(true));
    NtlmAuthenticationConfiguration ntlmAuth = (NtlmAuthenticationConfiguration) httpProxy.getAuthentication();
    assertThat(ntlmAuth.getUsername(), is("ntlmuser"));
    assertThat(ntlmAuth.getHost(), is("ntlmhost"));
    assertThat(ntlmAuth.getDomain(), is("DOMAIN"));
  }

  @Test
  void testUpdate_authDisabled_returnsNullAuthentication() {
    HttpClientConfiguration previousConfig = mock(HttpClientConfiguration.class);
    HttpClientConfiguration newConfig = mock(HttpClientConfiguration.class);
    ProxyConfiguration proxyConfig = new ProxyConfiguration();
    when(newConfig.getProxy()).thenReturn(proxyConfig);
    when(httpClientManager.getConfiguration()).thenReturn(previousConfig);
    when(httpClientManager.newConfiguration()).thenReturn(newConfig);

    HttpSettingsXO settings = new HttpSettingsXO();
    settings.setHttpEnabled(true);
    settings.setHttpHost("proxy.example.com");
    settings.setHttpPort(8080);
    settings.setHttpAuthEnabled(false);

    underTest.update(settings);

    verify(httpClientManager).setConfiguration(newConfig);
    ProxyServerConfiguration httpProxy = proxyConfig.getHttp();
    assertThat(httpProxy.getAuthentication(), is(nullValue()));
  }

  @Test
  void testUpdate_passwordPlaceholder_usePreviousSecret() {
    HttpClientConfiguration previousConfig = mock(HttpClientConfiguration.class);
    ProxyConfiguration prevProxy = new ProxyConfiguration();
    ProxyServerConfiguration prevHttpProxy = new ProxyServerConfiguration();
    UsernameAuthenticationConfiguration prevAuth = new UsernameAuthenticationConfiguration();
    Secret prevSecret = mock(Secret.class);
    when(prevSecret.getId()).thenReturn("secret-1");
    prevAuth.setPassword(prevSecret);
    prevAuth.setUsername("user");
    prevHttpProxy.setAuthentication(prevAuth);
    prevProxy.setHttp(prevHttpProxy);
    when(previousConfig.getProxy()).thenReturn(prevProxy);

    HttpClientConfiguration newConfig = mock(HttpClientConfiguration.class);
    ProxyConfiguration proxyConfig = new ProxyConfiguration();
    when(newConfig.getProxy()).thenReturn(proxyConfig);
    when(httpClientManager.getConfiguration()).thenReturn(previousConfig);
    when(httpClientManager.newConfiguration()).thenReturn(newConfig);

    HttpSettingsXO settings = new HttpSettingsXO();
    settings.setHttpEnabled(true);
    settings.setHttpHost("proxy.example.com");
    settings.setHttpPort(8080);
    settings.setHttpAuthEnabled(true);
    settings.setHttpAuthUsername("user");
    settings.setHttpAuthPassword(PasswordPlaceholder.get());

    underTest.update(settings);

    verify(secretsService, never()).encryptMaven(any(), any(), any());
    verify(httpClientManager).setConfiguration(newConfig);
  }

  @Test
  void testUpdate_withHttpsProxy() {
    HttpClientConfiguration previousConfig = mock(HttpClientConfiguration.class);
    HttpClientConfiguration newConfig = mock(HttpClientConfiguration.class);
    ProxyConfiguration proxyConfig = new ProxyConfiguration();
    when(newConfig.getProxy()).thenReturn(proxyConfig);
    when(httpClientManager.getConfiguration()).thenReturn(previousConfig);
    when(httpClientManager.newConfiguration()).thenReturn(newConfig);

    Secret encryptedSecret = mock(Secret.class);
    when(secretsService.encryptMaven(any(), any(), any())).thenReturn(encryptedSecret);

    HttpSettingsXO settings = new HttpSettingsXO();
    settings.setHttpsEnabled(true);
    settings.setHttpsHost("https-proxy.example.com");
    settings.setHttpsPort(8443);
    settings.setHttpsAuthEnabled(true);
    settings.setHttpsAuthUsername("httpsuser");
    settings.setHttpsAuthPassword("httpspass");

    underTest.update(settings);

    verify(httpClientManager).setConfiguration(newConfig);
    ProxyServerConfiguration httpsProxy = proxyConfig.getHttps();
    assertThat(httpsProxy, is(notNullValue()));
    assertThat(httpsProxy.isEnabled(), is(true));
    assertThat(httpsProxy.getHost(), is("https-proxy.example.com"));
    assertThat(httpsProxy.getPort(), is(8443));
  }

  @Test
  void testUpdate_withNonProxyHosts() {
    HttpClientConfiguration previousConfig = mock(HttpClientConfiguration.class);
    HttpClientConfiguration newConfig = mock(HttpClientConfiguration.class);
    ProxyConfiguration proxyConfig = new ProxyConfiguration();
    when(newConfig.getProxy()).thenReturn(proxyConfig);
    when(httpClientManager.getConfiguration()).thenReturn(previousConfig);
    when(httpClientManager.newConfiguration()).thenReturn(newConfig);

    HttpSettingsXO settings = new HttpSettingsXO();
    settings.setNonProxyHosts(Set.of("localhost", "*.internal.com"));

    underTest.update(settings);

    verify(httpClientManager).setConfiguration(newConfig);
    assertThat(proxyConfig.getNonProxyHosts(), is(notNullValue()));
  }

  @Test
  void testUpdate_removesOldHttpSecretWhenChanged() {
    Secret oldSecret = mock(Secret.class);
    when(oldSecret.getId()).thenReturn("old-secret-id");
    Secret newSecret = mock(Secret.class);
    when(newSecret.getId()).thenReturn("new-secret-id");

    HttpClientConfiguration previousConfig = mock(HttpClientConfiguration.class);
    ProxyConfiguration prevProxy = new ProxyConfiguration();
    ProxyServerConfiguration prevHttpProxy = new ProxyServerConfiguration();
    UsernameAuthenticationConfiguration prevAuth = new UsernameAuthenticationConfiguration();
    prevAuth.setUsername("olduser");
    prevAuth.setPassword(oldSecret);
    prevHttpProxy.setAuthentication(prevAuth);
    prevProxy.setHttp(prevHttpProxy);
    when(previousConfig.getProxy()).thenReturn(prevProxy);

    HttpClientConfiguration newConfig = mock(HttpClientConfiguration.class);
    ProxyConfiguration newProxy = new ProxyConfiguration();
    ProxyServerConfiguration newHttpProxy = new ProxyServerConfiguration();
    UsernameAuthenticationConfiguration newAuth = new UsernameAuthenticationConfiguration();
    newAuth.setUsername("newuser");
    newAuth.setPassword(newSecret);
    newHttpProxy.setAuthentication(newAuth);
    newProxy.setHttp(newHttpProxy);
    when(newConfig.getProxy()).thenReturn(newProxy);

    when(httpClientManager.getConfiguration()).thenReturn(previousConfig);
    when(httpClientManager.newConfiguration()).thenReturn(newConfig);
    when(secretsService.encryptMaven(any(), any(), any())).thenReturn(newSecret);

    HttpSettingsXO settings = new HttpSettingsXO();
    settings.setHttpEnabled(true);
    settings.setHttpHost("proxy.example.com");
    settings.setHttpPort(8080);
    settings.setHttpAuthEnabled(true);
    settings.setHttpAuthUsername("newuser");
    settings.setHttpAuthPassword("newpass");

    underTest.update(settings);

    verify(secretsService).remove(oldSecret);
  }

  @Test
  void testUpdate_callsReadAfterSettingConfiguration() {
    HttpClientConfiguration previousConfig = mock(HttpClientConfiguration.class);
    HttpClientConfiguration newConfig = mock(HttpClientConfiguration.class);
    when(httpClientManager.getConfiguration()).thenReturn(previousConfig);
    when(httpClientManager.newConfiguration()).thenReturn(newConfig);

    HttpSettingsXO settings = new HttpSettingsXO();

    HttpSettingsXO result = underTest.update(settings);

    assertThat(result, is(notNullValue()));
    // getConfiguration called twice: once for previous in update, once for read()
    verify(httpClientManager).setConfiguration(newConfig);
  }

  @Test
  void testUpdate_httpNotEnabled_noProxyCreated() {
    HttpClientConfiguration previousConfig = mock(HttpClientConfiguration.class);
    HttpClientConfiguration newConfig = mock(HttpClientConfiguration.class);
    when(httpClientManager.getConfiguration()).thenReturn(previousConfig);
    when(httpClientManager.newConfiguration()).thenReturn(newConfig);

    HttpSettingsXO settings = new HttpSettingsXO();
    settings.setHttpEnabled(false);

    underTest.update(settings);

    verify(httpClientManager).setConfiguration(newConfig);
    verify(newConfig, never()).setProxy(any());
  }
}
