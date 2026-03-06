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
package org.sonatype.nexus.repository.rest.api;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.rest.api.model.HttpClientAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientConnectionAuthenticationAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ProxyRepositoryApiRequestToConfigurationConverter}.
 *
 */
public class ProxyRepositoryApiRequestToConfigurationConverterTest
    extends TestSupport
{
  @Mock
  private RoutingRuleStore routingRuleStore;

  @Mock
  private ConfigurationStore configurationStore;

  @Mock
  private ProxyRepositoryApiRequest request;

  private ProxyRepositoryApiRequestToConfigurationConverter<ProxyRepositoryApiRequest> underTest;

  @Before
  public void setup() {
    when(configurationStore.newConfiguration()).thenReturn(new ConfigurationData());
    underTest = new ProxyRepositoryApiRequestToConfigurationConverter<>(routingRuleStore);
    underTest.setConfigurationStore(configurationStore);
  }

  @Test
  public void testConvert_withBearerTokenAuthentication() {
    // Arrange
    String expectedBearerToken = "test-bearer-token-12345";
    String expectedRemoteUrl = "https://registry.npmjs.org";

    StorageAttributes storage = new StorageAttributes("default", true);
    when(request.getStorage()).thenReturn(storage);

    ProxyAttributes proxy = new ProxyAttributes(expectedRemoteUrl, 1440, 1440, null);
    when(request.getProxy()).thenReturn(proxy);

    HttpClientConnectionAuthenticationAttributes authentication =
        new HttpClientConnectionAuthenticationAttributes("bearerToken", null, null, null, null, expectedBearerToken);

    HttpClientAttributes httpClient = new HttpClientAttributes(false, true, null, authentication);
    when(request.getHttpClient()).thenReturn(httpClient);

    // Act
    Configuration configuration = underTest.convert(request);

    // Assert
    assertThat(configuration, is(notNullValue()));

    NestedAttributesMap httpClientConfig = configuration.attributes("httpclient");
    assertThat(httpClientConfig, is(notNullValue()));

    NestedAttributesMap authenticationConfig = httpClientConfig.child("authentication");
    assertThat(authenticationConfig, is(notNullValue()));
    assertThat(authenticationConfig.get("type"), is("bearerToken"));
    assertThat(authenticationConfig.get("bearerTokenId"), is(expectedBearerToken));
  }

  @Test
  public void testConvert_withUsernameAuthentication() {
    // Arrange
    String expectedUsername = "test-user";
    String expectedPassword = "test-password";
    String expectedRemoteUrl = "https://registry.npmjs.org";

    StorageAttributes storage = new StorageAttributes("default", true);
    when(request.getStorage()).thenReturn(storage);

    ProxyAttributes proxy = new ProxyAttributes(expectedRemoteUrl, 1440, 1440, null);
    when(request.getProxy()).thenReturn(proxy);

    HttpClientConnectionAuthenticationAttributes authentication =
        new HttpClientConnectionAuthenticationAttributes("username", expectedUsername, expectedPassword, null, null,
            null);

    HttpClientAttributes httpClient = new HttpClientAttributes(false, true, null, authentication);
    when(request.getHttpClient()).thenReturn(httpClient);

    // Act
    Configuration configuration = underTest.convert(request);

    // Assert
    assertThat(configuration, is(notNullValue()));

    NestedAttributesMap httpClientConfig = configuration.attributes("httpclient");
    assertThat(httpClientConfig, is(notNullValue()));

    NestedAttributesMap authenticationConfig = httpClientConfig.child("authentication");
    assertThat(authenticationConfig, is(notNullValue()));
    assertThat(authenticationConfig.get("type"), is("username"));
    assertThat(authenticationConfig.get("username"), is(expectedUsername));
    assertThat(authenticationConfig.get("password"), is(expectedPassword));
  }

  @Test
  public void testConvert_withNtlmAuthentication() {
    // Arrange
    String expectedUsername = "test-user";
    String expectedPassword = "test-password";
    String expectedNtlmHost = "ntlm-host";
    String expectedNtlmDomain = "ntlm-domain";
    String expectedRemoteUrl = "https://registry.npmjs.org";

    StorageAttributes storage = new StorageAttributes("default", true);
    when(request.getStorage()).thenReturn(storage);

    ProxyAttributes proxy = new ProxyAttributes(expectedRemoteUrl, 1440, 1440, null);
    when(request.getProxy()).thenReturn(proxy);

    HttpClientConnectionAuthenticationAttributes authentication =
        new HttpClientConnectionAuthenticationAttributes("ntlm", expectedUsername, expectedPassword,
            expectedNtlmHost, expectedNtlmDomain, null);

    HttpClientAttributes httpClient = new HttpClientAttributes(false, true, null, authentication);
    when(request.getHttpClient()).thenReturn(httpClient);

    // Act
    Configuration configuration = underTest.convert(request);

    // Assert
    assertThat(configuration, is(notNullValue()));

    NestedAttributesMap httpClientConfig = configuration.attributes("httpclient");
    assertThat(httpClientConfig, is(notNullValue()));

    NestedAttributesMap authenticationConfig = httpClientConfig.child("authentication");
    assertThat(authenticationConfig, is(notNullValue()));
    assertThat(authenticationConfig.get("type"), is("ntlm"));
    assertThat(authenticationConfig.get("username"), is(expectedUsername));
    assertThat(authenticationConfig.get("password"), is(expectedPassword));
    assertThat(authenticationConfig.get("ntlmHost"), is(expectedNtlmHost));
    assertThat(authenticationConfig.get("ntlmDomain"), is(expectedNtlmDomain));
  }

  @Test
  public void testConvert_withoutAuthentication() {
    // Arrange
    String expectedRemoteUrl = "https://registry.npmjs.org";

    StorageAttributes storage = new StorageAttributes("default", true);
    when(request.getStorage()).thenReturn(storage);

    ProxyAttributes proxy = new ProxyAttributes(expectedRemoteUrl, 1440, 1440, null);
    when(request.getProxy()).thenReturn(proxy);

    HttpClientAttributes httpClient = new HttpClientAttributes(false, true, null, null);
    when(request.getHttpClient()).thenReturn(httpClient);

    // Act
    Configuration configuration = underTest.convert(request);

    // Assert
    assertThat(configuration, is(notNullValue()));

    NestedAttributesMap httpClientConfig = configuration.attributes("httpclient");
    assertThat(httpClientConfig, is(notNullValue()));
    assertThat(httpClientConfig.get("blocked"), is(false));
    assertThat(httpClientConfig.get("autoBlock"), is(true));
  }
}
