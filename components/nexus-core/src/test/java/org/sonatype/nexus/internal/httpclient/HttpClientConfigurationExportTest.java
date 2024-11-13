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

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Serialization/Deserialization {@link HttpClientConfiguration} by {@link
 * HttpClientConfigurationExport}
 */
public class HttpClientConfigurationExportTest
{
  private final JsonExporter jsonExporter = new JsonExporter();

  private File jsonFile;

  @Before
  public void setup() throws IOException {
    jsonFile = File.createTempFile("HttpClientConfigurationData", ".json");
  }

  @After
  public void tearDown() {
    jsonFile.delete();
  }

  @Test
  public void testExportImportToJson() throws Exception {
    HttpClientConfiguration configuration = createHttpClientConfiguration();

    HttpClientConfigurationStore store = mock(HttpClientConfigurationStore.class);
    when(store.load()).thenReturn(configuration);

    HttpClientConfigurationExport exporter = new HttpClientConfigurationExport(store);
    exporter.export(jsonFile);
    Optional<HttpClientConfigurationData> importedDataOpt =
        jsonExporter.importObjectFromJson(jsonFile, HttpClientConfigurationData.class);

    assertTrue(importedDataOpt.isPresent());
    HttpClientConfigurationData importedData = importedDataOpt.get();
    AuthenticationConfiguration auth = importedData.getAuthentication();
    assertNotNull(auth);
    assertThat(auth, instanceOf(UsernameAuthenticationConfiguration.class));

    ConnectionConfiguration connection = importedData.getConnection();
    assertNotNull(connection);
    assertNotNull(connection.getTimeout());
    assertThat(connection.getTimeout().toSeconds(), is(60L));

    // check sensitive data is not stored
    UsernameAuthenticationConfiguration usernameAuth = (UsernameAuthenticationConfiguration) auth;
    assertThat(usernameAuth.getPassword(), not("admin123"));
  }

  HttpClientConfiguration createHttpClientConfiguration() {
    HttpClientConfiguration configuration = new HttpClientConfigurationData();

    ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration();
    connectionConfiguration.setUserAgentSuffix("UserAgentSuffix");
    connectionConfiguration.setRetries(10);
    connectionConfiguration.setUseTrustStore(true);
    connectionConfiguration.setTimeout(new Time(1, TimeUnit.MINUTES));

    ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
    ProxyServerConfiguration http = new ProxyServerConfiguration();
    http.setHost("localhost");
    http.setPort(80);
    http.setEnabled(true);
    proxyConfiguration.setHttp(http);

    UsernameAuthenticationConfiguration authenticationConfiguration = new UsernameAuthenticationConfiguration();
    authenticationConfiguration.setUsername("admin");
    authenticationConfiguration.setPassword(mock(Secret.class));

    configuration.setConnection(connectionConfiguration);
    configuration.setProxy(proxyConfiguration);
    configuration.setAuthentication(authenticationConfiguration);

    return configuration;
  }
}
