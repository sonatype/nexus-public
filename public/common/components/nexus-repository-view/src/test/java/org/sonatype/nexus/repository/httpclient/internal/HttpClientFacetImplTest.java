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
package org.sonatype.nexus.repository.httpclient.internal;

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.httpclient.AutoBlockConfiguration;
import org.sonatype.nexus.repository.httpclient.ContentCompressionStrategy;
import org.sonatype.nexus.repository.httpclient.NormalizationStrategy;
import org.sonatype.nexus.repository.httpclient.HttpClientConfig;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.httpclient.internal.HttpClientFacetImpl.CONFIG_KEY;

/**
 * Tests for {@link HttpClientFacetImpl}.
 */
@ExtendWith(MockitoExtension.class)
class HttpClientFacetImplTest
{
  private static final String DEFAULT = "default";

  private static final String NPM = "npm";

  private static final String DOCKER = "docker";

  private static final String YUM = "yum";

  private static final String TEST_REPOSITORY_NAME = "test-repository";

  @Mock
  private HttpClientManager httpClientManager;

  @Mock
  private Configuration configuration;

  @Mock
  private Repository repository;

  @Mock
  private ConfigurationFacet configurationFacet;

  @Mock
  private AutoBlockConfiguration defaultAutoBlockConfiguration;

  @Mock
  private AutoBlockConfiguration npmAutoBlockConfiguration;

  @Mock
  private Format format;

  @Mock
  private CloseableHttpClient closeableHttpClient;

  @Mock
  private EventManager eventManager;

  @Mock
  HttpClientConfiguration httpClientConfiguration;

  private HttpClientConfig config = new HttpClientConfig();

  private NtlmAuthenticationConfiguration ntlmAuthentication = new NtlmAuthenticationConfiguration();

  private MockedStatic<QualifierUtil> mockedStatic;

  // Value generated using: http://www.blitter.se/utils/basic-authentication-header-generator/
  private static final String BASIC_AUTH_ENCODED = "Basic dXNlcm5hbWU6cGFzc3dvcmQ=";

  private static final String USERNAME = "username";

  private static final String PASSWORD = "password";

  private boolean disableCompression;

  @BeforeEach
  public void setUp() throws Exception {
    mockedStatic = mockStatic(QualifierUtil.class);

    when(configurationFacet.readSection(configuration, CONFIG_KEY, HttpClientConfig.class)).thenReturn(config);

    when(repository.getName()).thenReturn(TEST_REPOSITORY_NAME);
    when(repository.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
    when(repository.getConfiguration()).thenReturn(configuration);

    when(repository.getFormat()).thenReturn(format);

    when(httpClientManager.create(any())).thenReturn(closeableHttpClient);
    when(httpClientManager.newConfiguration()).thenReturn(httpClientConfiguration);
  }

  @AfterEach
  public void tearDown() {
    mockedStatic.close();
  }

  @Test
  public void createBasicAuthHeaderWithoutAuthConfiguredThrowsException() throws Exception {
    Header basicAuth = createFacet(NPM).createBasicAuthHeader();

    assertThat(basicAuth, is(nullValue()));
  }

  @Test
  public void createBasicAuthHeaderWithoutUsernameAuthThrowsException() throws Exception {
    config.authentication = ntlmAuthentication;

    Header basicAuth = createFacet(NPM).createBasicAuthHeader();

    assertThat(basicAuth, is(nullValue()));
  }

  @Test
  public void createBasicAuthWithUsernameAuthConfigWorks() throws Exception {
    UsernameAuthenticationConfiguration usernameAuthentication = new UsernameAuthenticationConfiguration();
    usernameAuthentication.setUsername(USERNAME);
    final Secret secret = mock(Secret.class);
    when(secret.decrypt()).thenReturn(PASSWORD.toCharArray());
    usernameAuthentication.setPassword(secret);

    config.authentication = usernameAuthentication;

    Header basicAuth = createFacet(NPM).createBasicAuthHeader();

    assertThat(basicAuth.getName(), is(equalTo(HttpHeaders.AUTHORIZATION)));
    assertThat(basicAuth.getValue(), is(equalTo(BASIC_AUTH_ENCODED)));
  }

  @Test
  public void passFormatSpecificConfigurationToBlockingHttpClient() throws Exception {
    assertConfigurationPassedToBlockingClient(NPM, npmAutoBlockConfiguration);
  }

  @Test
  public void passDefaultConfigurationWhenFormatNotFound() throws Exception {
    assertConfigurationPassedToBlockingClient("unknown", defaultAutoBlockConfiguration);
  }

  @Test
  public void passDisableCompression() throws Exception {
    assertDisableCompressionPassedToCustomizer(YUM, true);
    assertDisableCompressionPassedToCustomizer("unknown", false);
    assertDisableCompressionPassedToCustomizer(NPM, false);
  }

  @Test
  public void passwordCacheIsResetOnDoConfigure() throws Exception {
    UsernameAuthenticationConfiguration usernameAuthentication = new UsernameAuthenticationConfiguration();
    usernameAuthentication.setUsername(USERNAME);
    final Secret secret = mock(Secret.class);
    when(secret.decrypt()).thenReturn(PASSWORD.toCharArray());
    usernameAuthentication.setPassword(secret);

    config.authentication = usernameAuthentication;

    HttpClientFacetImpl underTest = createFacet(DOCKER);
    Header basicAuth1 = underTest.createBasicAuthHeader();
    assertThat(basicAuth1.getValue(), is(equalTo(BASIC_AUTH_ENCODED)));

    final Secret newSecret = mock(Secret.class);
    when(newSecret.decrypt()).thenReturn("newpassword".toCharArray());
    usernameAuthentication.setPassword(newSecret);

    underTest.doConfigure(configuration);

    String expected =
        "Basic " + java.util.Base64.getEncoder().encodeToString((USERNAME + ":newpassword").getBytes("ISO-8859-1"));
    Header basicAuth2 = underTest.createBasicAuthHeader();
    assertThat(basicAuth2.getValue(), is(equalTo(expected)));
  }

  private void assertConfigurationPassedToBlockingClient(
      final String format,
      final AutoBlockConfiguration autoBlockConfiguration) throws Exception
  {
    HttpClientFacetImpl underTest = createFacet(format);
    underTest.doConfigure(configuration);

    assertThat(underTest.httpClient.autoBlockConfiguration, is(equalTo(autoBlockConfiguration)));
  }

  private void assertDisableCompressionPassedToCustomizer(
      final String format,
      final Boolean disableCompression) throws Exception
  {
    reset(httpClientConfiguration);

    this.disableCompression = disableCompression;

    createFacet(format);

    if (disableCompression) {
      verify(httpClientConfiguration).setDisableContentCompression(disableCompression);
    }
    else {
      verify(httpClientConfiguration, never()).setDisableContentCompression(any());
    }
  }

  private HttpClientFacetImpl createFacet(final String formatName) throws Exception {
    when(QualifierUtil.buildQualifierBeanMap(any())).thenReturn(
        Map.of(DEFAULT, defaultAutoBlockConfiguration, NPM, npmAutoBlockConfiguration),
        Map.of(),
        Map.of(DOCKER, (NormalizationStrategy) () -> true),
        Map.of(YUM, (ContentCompressionStrategy) (r) -> disableCompression),
        Map.of());

    when(format.getValue()).thenReturn(formatName);

    HttpClientFacetImpl underTest =
        new HttpClientFacetImpl(httpClientManager, List.of(defaultAutoBlockConfiguration, npmAutoBlockConfiguration),
            List.of(), List.of(), List.of(), List.of(), config);
    underTest.attach(repository);
    underTest.installDependencies(eventManager);

    underTest.init();
    underTest.start();

    return underTest;
  }
}
