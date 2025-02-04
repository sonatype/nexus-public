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
package org.sonatype.nexus.internal.provisioning;

import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.internal.httpclient.TestHttpClientConfiguration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.CoreApi;
import org.sonatype.nexus.capability.*;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.*;
import org.sonatype.nexus.internal.app.BaseUrlCapabilityDescriptor;
import org.sonatype.nexus.internal.capability.DefaultCapabilityReference;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CoreApiImpl}
 * 
 * @since 3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class CoreApiImplTest
{

  @Mock
  private CapabilityRegistry capabilityRegistry;

  @Mock
  private DefaultCapabilityReference reference;

  @Mock
  private CapabilityContext context;

  @Mock
  private CapabilityDescriptor descriptor;

  @Mock
  private HttpClientManager httpClientManager;

  @Mock
  private SecretsService secretsService;

  private CapabilityIdentity identity = new CapabilityIdentity("test");

  private CoreApi api;

  private static final ProxyConfiguration EXISTING_HTTP;

  static {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHost("http");
    proxyServerConfiguration.setPort(1);
    proxyServerConfiguration.setEnabled(true);
    ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
    proxyConfiguration.setHttp(proxyServerConfiguration);
    EXISTING_HTTP = proxyConfiguration;
  }

  @Mock
  private Subject subject;

  private MockedStatic<SecurityUtils> securityUtils;

  @Mock
  private SecurityManager securityManager;

  @Before
  public void setUp() {
    securityUtils = mockStatic(SecurityUtils.class);
    securityUtils.when(SecurityUtils::getSubject).thenReturn(subject);
    Collection existingCapabilities = ImmutableList.of(reference);

    when(capabilityRegistry.getAll()).thenReturn(existingCapabilities);
    api = new CoreApiImpl(capabilityRegistry, httpClientManager, secretsService);
  }

  @After
  public void tearDown() {
    securityUtils.close();
  }

  @Test
  public void canSetBaseUrlWithoutAnExistingCapability() {
    Collection existingCapabilities = ImmutableList.of(reference);

    when(capabilityRegistry.getAll()).thenReturn(existingCapabilities);
    when(reference.context()).thenReturn(context);
    when(context.descriptor()).thenReturn(descriptor);
    when(descriptor.type()).thenReturn(new CapabilityType("not baseurl"));

    api.baseUrl("foo");

    verify(capabilityRegistry).add(eq(BaseUrlCapabilityDescriptor.TYPE), eq(true), any(),
        eq(ImmutableMap.of("url", "foo")));
  }

  @Test
  public void canSetBaseUrlWithAnExistingCapability() {
    Collection existingCapabilities = ImmutableList.of(reference);

    when(capabilityRegistry.getAll()).thenReturn(existingCapabilities);
    when(reference.context()).thenReturn(context);
    when(context.descriptor()).thenReturn(descriptor);
    when(descriptor.type()).thenReturn(BaseUrlCapabilityDescriptor.TYPE);
    when(reference.id()).thenReturn(identity);
    when(reference.isActive()).thenReturn(true);
    when(reference.notes()).thenReturn("whatever");

    api.baseUrl("bar");

    verify(capabilityRegistry).update(eq(identity), eq(true), eq("whatever"), eq(ImmutableMap.of("url", "bar")));
  }

  @Test
  public void canDeleteAnExistingBaseUrlCapability() {
    Collection existingCapabilities = ImmutableList.of(reference);

    when(capabilityRegistry.getAll()).thenReturn(existingCapabilities);
    when(reference.context()).thenReturn(context);
    when(context.descriptor()).thenReturn(descriptor);
    when(descriptor.type()).thenReturn(BaseUrlCapabilityDescriptor.TYPE);
    when(reference.id()).thenReturn(identity);

    api.removeBaseUrl();

    verify(capabilityRegistry).remove(identity);
  }

  @Test
  public void canDeleteWhenBaseUrlCapabilityIsNotConfigured() {
    Collection existingCapabilities = ImmutableList.of();

    when(capabilityRegistry.getAll()).thenReturn(existingCapabilities);

    api.removeBaseUrl();

    verify(capabilityRegistry, never()).remove(any());
  }

  @Test
  public void canSetHttpProxySettingsWithoutAuth() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration();

    when(httpClientManager.getConfiguration()).thenReturn(configuration);

    api.httpProxy("http", 1);

    verify(httpClientManager).setConfiguration(any());
    assertThat(configuration.getProxy(), notNullValue());
    assertThat(configuration.getProxy().getHttp(), notNullValue());
    assertThat(configuration.getProxy().getHttp().getHost(), equalTo("http"));
    assertThat(configuration.getProxy().getHttp().getPort(), equalTo(1));
    assertThat(configuration.getProxy().getHttp().getAuthentication(), nullValue());
    assertThat(configuration.getProxy().getHttps(), nullValue());
  }

  @Test
  public void canSetHttpProxySettingsWithBasicAuth() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration();

    when(httpClientManager.getConfiguration()).thenReturn(configuration);
    when(secretsService.encryptMaven(eq(AuthenticationConfiguration.AUTHENTICATION_CONFIGURATION), any(), any()))
        .thenReturn(mock(Secret.class));

    api.httpProxyWithBasicAuth("http", 1, "user", "pass");

    verify(httpClientManager).setConfiguration(any());
    assertThat(configuration.getProxy(), notNullValue());
    assertThat(configuration.getProxy().getHttp(), notNullValue());
    assertThat(configuration.getProxy().getHttp().getHost(), equalTo("http"));
    assertThat(configuration.getProxy().getHttp().getPort(), equalTo(1));
    assertThat(configuration.getProxy().getHttp().getAuthentication(),
        instanceOf(UsernameAuthenticationConfiguration.class));
    assertThat(
        ((UsernameAuthenticationConfiguration) configuration.getProxy().getHttp().getAuthentication()).getUsername(),
        equalTo("user"));
  }

  @Test
  public void canSetHttpProxySettingsWithNtlmAuth() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration();

    when(httpClientManager.getConfiguration()).thenReturn(configuration);
    when(secretsService.encryptMaven(eq(AuthenticationConfiguration.AUTHENTICATION_CONFIGURATION), any(), any()))
        .thenReturn(mock(Secret.class));

    api.httpProxyWithNTLMAuth("http", 1, "user", "pass", "ntlmHost", "domain");

    verify(httpClientManager).setConfiguration(any());
    assertThat(configuration.getProxy(), notNullValue());
    assertThat(configuration.getProxy().getHttp(), notNullValue());
    assertThat(configuration.getProxy().getHttp().getHost(), equalTo("http"));
    assertThat(configuration.getProxy().getHttp().getPort(), equalTo(1));
    assertThat(configuration.getProxy().getHttp().getAuthentication(),
        instanceOf(NtlmAuthenticationConfiguration.class));
    assertThat(((NtlmAuthenticationConfiguration) configuration.getProxy().getHttp().getAuthentication()).getUsername(),
        equalTo("user"));
  }

  @Test
  public void canRemoveHttpProxyConfiguration() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration(EXISTING_HTTP);
    System.out.println("configuration: " + configuration);
    when(httpClientManager.getConfiguration()).thenReturn(configuration);

    api.removeHTTPProxy();

    verify(httpClientManager).setConfiguration(any());
    assertThat(configuration.getProxy(), nullValue());
  }

  @Test(expected = IllegalStateException.class)
  public void cannotSetHttpsProxyWithoutHttpProxy() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration();

    when(httpClientManager.getConfiguration()).thenReturn(configuration);

    api.httpsProxy("https", 1);
  }

  @Test
  public void canSetHttpsProxySettingsWithoutAuth() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration(EXISTING_HTTP);

    when(httpClientManager.getConfiguration()).thenReturn(configuration);

    api.httpsProxy("https", 2);

    verify(httpClientManager).setConfiguration(any());
    assertThat(configuration.getProxy(), notNullValue());
    assertThat(configuration.getProxy().getHttps(), notNullValue());
    assertThat(configuration.getProxy().getHttps().getHost(), equalTo("https"));
    assertThat(configuration.getProxy().getHttps().getPort(), equalTo(2));
    assertThat(configuration.getProxy().getHttps().getAuthentication(), nullValue());
  }

  @Test
  public void canSetHttpsProxySettingsWithBasicAuth() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration(EXISTING_HTTP);

    when(httpClientManager.getConfiguration()).thenReturn(configuration);
    when(secretsService.encryptMaven(eq(AuthenticationConfiguration.AUTHENTICATION_CONFIGURATION), any(), any()))
        .thenReturn(mock(Secret.class));

    api.httpsProxyWithBasicAuth("https", 2, "user", "pass");

    verify(httpClientManager).setConfiguration(any());
    assertThat(configuration.getProxy(), notNullValue());
    assertThat(configuration.getProxy().getHttps(), notNullValue());
    assertThat(configuration.getProxy().getHttps().getHost(), equalTo("https"));
    assertThat(configuration.getProxy().getHttps().getPort(), equalTo(2));
    assertThat(configuration.getProxy().getHttps().getAuthentication(),
        instanceOf(UsernameAuthenticationConfiguration.class));
    assertThat(
        ((UsernameAuthenticationConfiguration) configuration.getProxy().getHttps().getAuthentication()).getUsername(),
        equalTo("user"));
  }

  @Test
  public void canSetHttpsProxySettingsWithNtlmAuth() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration(EXISTING_HTTP);

    when(httpClientManager.getConfiguration()).thenReturn(configuration);
    when(secretsService.encryptMaven(eq(AuthenticationConfiguration.AUTHENTICATION_CONFIGURATION), any(), any()))
        .thenReturn(mock(Secret.class));

    api.httpsProxyWithNTLMAuth("https", 2, "user", "pass", "ntlmHost", "domain");

    verify(httpClientManager).setConfiguration(any());
    assertThat(configuration.getProxy(), notNullValue());
    assertThat(configuration.getProxy().getHttps(), notNullValue());
    assertThat(configuration.getProxy().getHttps().getHost(), equalTo("https"));
    assertThat(configuration.getProxy().getHttps().getPort(), equalTo(2));
    assertThat(configuration.getProxy().getHttps().getAuthentication(),
        instanceOf(NtlmAuthenticationConfiguration.class));
    assertThat(
        ((NtlmAuthenticationConfiguration) configuration.getProxy().getHttps().getAuthentication()).getUsername(),
        equalTo("user"));
  }

  @Test
  public void canRemoveHttpsProxyConfiguration() {
    ProxyConfiguration proxies = EXISTING_HTTP.copy();
    ProxyServerConfiguration https = new ProxyServerConfiguration();
    https.setEnabled(true);
    https.setHost("https");
    https.setPort(2);
    proxies.setHttps(https);
    HttpClientConfiguration configuration = new TestHttpClientConfiguration(proxies);

    when(httpClientManager.getConfiguration()).thenReturn(configuration);

    api.removeHTTPSProxy();

    verify(httpClientManager).setConfiguration(any());
    assertThat(configuration.getProxy(), notNullValue());
    assertThat(configuration.getProxy().getHttp(), notNullValue());
    assertThat(configuration.getProxy().getHttps(), nullValue());
  }

  @Test
  public void canConfigureConnectionTimeout() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration();

    when(httpClientManager.getConfiguration()).thenReturn(configuration);

    api.connectionTimeout(5);

    verify(httpClientManager).setConfiguration(any());
    assertThat(configuration.getConnection().getTimeout(), equalTo(Time.seconds(5)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void configureConnectionTimeoutFailsWhenValuesAreOutOfBounds() {
    api.connectionTimeout(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void configureConnectionTimeoutFailsWhenValuesAreOutOfBounds2() {
    api.connectionTimeout(3601);
  }

  @Test
  public void canConfigureConnectionRetries() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration();

    when(httpClientManager.getConfiguration()).thenReturn(configuration);

    api.connectionRetryAttempts(5);

    verify(httpClientManager).setConfiguration(any());
    assertThat(configuration.getConnection().getRetries(), equalTo(5));
  }

  @Test(expected = IllegalArgumentException.class)
  public void configureConnectionRetriesFailsWhenValuesAreOutOfBounds() {
    api.connectionRetryAttempts(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void configureConnectionRetriesFailsWhenValuesAreOutOfBounds2() {
    api.connectionRetryAttempts(11);
  }

  @Test
  public void canCustomizeUserAgent() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration();

    when(httpClientManager.getConfiguration()).thenReturn(configuration);

    api.userAgentCustomization("foo");

    verify(httpClientManager).setConfiguration(any());
    assertThat(configuration.getConnection().getUserAgentSuffix(), equalTo("foo"));
  }

  @Test
  public void canSetAndUnsetNonProxyHosts() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration(EXISTING_HTTP);

    when(httpClientManager.getConfiguration()).thenReturn(configuration);

    api.nonProxyHosts("foo", "bar");

    verify(httpClientManager).setConfiguration(any());
    assertThat(configuration.getProxy(), notNullValue());
    assertThat(configuration.getProxy().getNonProxyHosts(), notNullValue());
    assertThat(configuration.getProxy().getNonProxyHosts().length, equalTo(2));
    assertThat(configuration.getProxy().getNonProxyHosts()[0], equalTo("foo"));
    assertThat(configuration.getProxy().getNonProxyHosts()[1], equalTo("bar"));

    api.nonProxyHosts();

    verify(httpClientManager, times(2)).setConfiguration(any());
    assertThat(configuration.getProxy(), notNullValue());
    assertThat(configuration.getProxy().getNonProxyHosts(), emptyArray());
  }

  @Test(expected = IllegalStateException.class)
  public void cannotNonProxyHostsWithoutAProxyConfigured() {
    HttpClientConfiguration configuration = new TestHttpClientConfiguration();

    when(httpClientManager.getConfiguration()).thenReturn(configuration);

    api.nonProxyHosts("foo", "bar");
  }
}
