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

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
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
import org.sonatype.nexus.repository.httpclient.internal.HttpClientFacetImpl.Config;

import com.google.common.collect.ImmutableMap;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.httpclient.internal.HttpClientFacetImpl.CONFIG_KEY;

/**
 * Tests for {@link HttpClientFacetImpl}.
 */
public class HttpClientFacetImplTest
    extends TestSupport
{
  private static final String DEFAULT = "default";

  private static final String NPM = "npm";

  private static final String DOCKER = "docker";

  private static final String YUM = "yum";

  private static final String TEST_REPOSITORY_NAME = "test-repository";

  private HttpClientFacetImpl underTest;

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
  private Format npmFormat;

  @Mock
  private Format yumFormat;

  @Mock
  private Format unknownFormat;

  @Mock
  private CloseableHttpClient closeableHttpClient;

  @Mock
  private EventManager eventManager;

  @Mock
  HttpClientConfiguration httpClientConfiguration;

  private HttpClientFacetImpl.Config config = new HttpClientFacetImpl.Config();

  private UsernameAuthenticationConfiguration usernameAuthentication = new UsernameAuthenticationConfiguration();

  private NtlmAuthenticationConfiguration ntlmAuthentication = new NtlmAuthenticationConfiguration();

  // Value generated using: http://www.blitter.se/utils/basic-authentication-header-generator/
  private static final String BASIC_AUTH_ENCODED = "Basic dXNlcm5hbWU6cGFzc3dvcmQ=";

  private static final String USERNAME = "username";

  private static final String PASSWORD = "password";

  private boolean disableCompression;

  @Before
  public void setUp() throws Exception {
    Map<String, AutoBlockConfiguration> autoBlockConfiguration = new HashMap<>();
    autoBlockConfiguration.put(DEFAULT, defaultAutoBlockConfiguration);
    autoBlockConfiguration.put(NPM, npmAutoBlockConfiguration);

    Map<String, NormalizationStrategy> normalizationStrategies = ImmutableMap.of(DOCKER, () -> true);

    Map<String, ContentCompressionStrategy> contentCompressionStrategiesMap =
        ImmutableMap.of(YUM, (r) -> disableCompression);

    underTest =
        new HttpClientFacetImpl(httpClientManager, autoBlockConfiguration, newHashMap(), normalizationStrategies,
            contentCompressionStrategiesMap, newHashMap(), config);
    underTest.attach(repository);
    underTest.installDependencies(eventManager);
    when(configurationFacet.readSection(configuration, CONFIG_KEY, Config.class)).thenReturn(config);

    when(repository.getName()).thenReturn(TEST_REPOSITORY_NAME);

    when(npmFormat.getValue()).thenReturn(NPM);
    when(yumFormat.getValue()).thenReturn(YUM);
    when(unknownFormat.getValue()).thenReturn("unknown");

    usernameAuthentication.setUsername(USERNAME);
    final Secret secret = mock(Secret.class);
    when(secret.decrypt()).thenReturn(PASSWORD.toCharArray());
    usernameAuthentication.setPassword(secret);
  }

  @Test
  public void createBasicAuthHeaderWithoutAuthConfiguredThrowsException() throws Exception {
    Header basicAuth = underTest.createBasicAuthHeader();

    assertThat(basicAuth, is(nullValue()));
  }

  @Test
  public void createBasicAuthHeaderWithoutUsernameAuthThrowsException() throws Exception {
    config.authentication = ntlmAuthentication;

    Header basicAuth = underTest.createBasicAuthHeader();

    assertThat(basicAuth, is(nullValue()));
  }

  @Test
  public void createBasicAuthWithUsernameAuthConfigWorks() throws Exception {
    config.authentication = usernameAuthentication;

    Header basicAuth = underTest.createBasicAuthHeader();

    assertThat(basicAuth.getName(), is(equalTo(HttpHeaders.AUTHORIZATION)));
    assertThat(basicAuth.getValue(), is(equalTo(BASIC_AUTH_ENCODED)));
  }

  @Test
  public void passFormatSpecificConfigurationToBlockingHttpClient() throws Exception {
    assertConfigurationPassedToBlockingClient(npmFormat, npmAutoBlockConfiguration);
  }

  @Test
  public void passDefaultConfigurationWhenFormatNotFound() throws Exception {
    assertConfigurationPassedToBlockingClient(unknownFormat, defaultAutoBlockConfiguration);
  }

  @Test
  public void passDisableCompression() throws Exception {
    assertDisableCompressionPassedToCustomizer(yumFormat, true);
    assertDisableCompressionPassedToCustomizer(yumFormat, false);
    assertDisableCompressionPassedToCustomizer(npmFormat, false);
  }

  private void assertConfigurationPassedToBlockingClient(final Format format,
                                                         final AutoBlockConfiguration autoBlockConfiguration)
      throws Exception
  {
    when(httpClientManager.create(any())).thenReturn(closeableHttpClient);
    when(httpClientManager.newConfiguration()).thenReturn(httpClientConfiguration);
    when(repository.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);

    when(repository.getConfiguration()).thenReturn(configuration);
    when(repository.getFormat()).thenReturn(format);

    underTest.doConfigure(configuration);

    assertThat(underTest.httpClient.autoBlockConfiguration, is(equalTo(autoBlockConfiguration)));
  }

  private void assertDisableCompressionPassedToCustomizer(final Format format,
                                                          final Boolean disableCompression)
      throws Exception
  {
    this.disableCompression = disableCompression;

    when(httpClientManager.create(any())).thenReturn(closeableHttpClient);
    when(httpClientManager.newConfiguration()).thenReturn(httpClientConfiguration);
    when(repository.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);

    when(repository.getConfiguration()).thenReturn(configuration);
    when(repository.getFormat()).thenReturn(format);

    underTest.doConfigure(configuration);

    verify(httpClientConfiguration).setDisableContentCompression(disableCompression);
  }
}
