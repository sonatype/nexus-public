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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.mybatis.handlers.SecretTypeHandler;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;
import org.sonatype.nexus.internal.httpclient.handlers.AuthenticationConfigurationHandler;
import org.sonatype.nexus.internal.httpclient.handlers.ConnectionConfigurationHandler;
import org.sonatype.nexus.internal.httpclient.handlers.ProxyConfigurationHandler;
import org.sonatype.nexus.testdb.DataSessionRule;

import java.util.concurrent.TimeUnit;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
@Category(SQLTestGroup.class)
public class HttpClientConfigurationDAOTest
{
  private static final String ID = "_1";

  @Mock
  private Secret secretPassword;

  private SecretsService secretsService = mock(SecretsService.class);

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule()
      .handle(new ConnectionConfigurationHandler(secretsService))
      .handle(new ProxyConfigurationHandler(secretsService))
      .handle(new AuthenticationConfigurationHandler(secretsService))
      .handle(new SecretTypeHandler(secretsService))
      .access(HttpClientConfigurationDAO.class);

  private DataSession<?> session;

  private HttpClientConfigurationDAO dao;

  @Before
  public void setup() {
    when(secretPassword.getId()).thenReturn(ID);
    when(secretsService.from(ID)).thenReturn(secretPassword);
    when(secretsService.encrypt(any(), any(), any())).thenReturn(secretPassword);
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(HttpClientConfigurationDAO.class);
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void itCanWriteAndReadWithAllAttributesNull() {
    HttpClientConfigurationData config = new HttpClientConfigurationData();

    dao.set(config);
    HttpClientConfigurationData readEntity = dao.get().orElse(null);

    assertNotNull(readEntity);
    assertNull(readEntity.getConnection());
    assertNull(readEntity.getAuthentication());
    assertNull(readEntity.getProxy());
    assertNull(readEntity.getRedirectStrategy());
  }

  @Test
  public void canDeleteConfiguration() {
    HttpClientConfigurationData config = new HttpClientConfigurationData();

    dao.set(config);
    dao.clear();
    HttpClientConfigurationData readEntity = dao.get().orElse(null);

    assertNull(readEntity);
  }

  @Test
  public void canUpdateConnectionConfiguration() {
    ConnectionConfiguration connection = new ConnectionConfiguration();
    connection.setRetries(Integer.MAX_VALUE);
    connection.setTimeout(new Time(1, TimeUnit.SECONDS));
    connection.setEnableCircularRedirects(false);
    connection.setUserAgentSuffix("foo");
    connection.setEnableCookies(true);

    HttpClientConfigurationData config = new HttpClientConfigurationData();
    config.setConnection(connection);

    dao.set(config);
    HttpClientConfigurationData readEntity = dao.get().orElse(null);

    assertNotNull(readEntity);
    assertThat(readEntity.getConnection().getRetries(), equalTo(Integer.MAX_VALUE));
    assertEquals(new Time(1, TimeUnit.SECONDS).asSeconds(), readEntity.getConnection().getTimeout().asSeconds());
    assertFalse(readEntity.getConnection().getEnableCircularRedirects());
    assertEquals("foo", readEntity.getConnection().getUserAgentSuffix());
    assertTrue(readEntity.getConnection().getEnableCookies());

    readEntity.getConnection().setRetries(Integer.MIN_VALUE);
    readEntity.getConnection().setTimeout(new Time(5, TimeUnit.MINUTES));
    readEntity.getConnection().setEnableCircularRedirects(true);
    readEntity.getConnection().setUserAgentSuffix("bar");
    readEntity.getConnection().setEnableCookies(false);

    dao.set(readEntity);
    readEntity = dao.get().orElse(null);

    assertNotNull(readEntity);
    assertThat(readEntity.getConnection().getRetries(), equalTo(Integer.MIN_VALUE));
    assertEquals(new Time(5, TimeUnit.MINUTES).asSeconds(), readEntity.getConnection().getTimeout().asSeconds());
    assertTrue(readEntity.getConnection().getEnableCircularRedirects());
    assertEquals("bar", readEntity.getConnection().getUserAgentSuffix());
    assertFalse(readEntity.getConnection().getEnableCookies());
  }

  @Test
  public void canUpdateAuthenticationConfiguration() {
    UsernameAuthenticationConfiguration user = new UsernameAuthenticationConfiguration();
    user.setUsername("foo");
    user.setPassword(secretsService.encrypt("test", "bob".toCharArray(), "test"));
    when(secretPassword.decrypt()).thenReturn("bob".toCharArray());

    HttpClientConfigurationData config = new HttpClientConfigurationData();
    config.setAuthentication(user);

    dao.set(config);
    HttpClientConfigurationData readEntity = dao.get().orElse(null);
    UsernameAuthenticationConfiguration usernameConfig =
        (UsernameAuthenticationConfiguration) readEntity.getAuthentication();

    assertNotNull(readEntity);
    assertEquals("username", readEntity.getAuthentication().getType());
    assertEquals("foo", usernameConfig.getUsername());
    assertEquals("bob", new String(usernameConfig.getPassword().decrypt()));

    usernameConfig.setUsername("bar");
    usernameConfig.setPassword(secretsService.encrypt("test", "obo".toCharArray(), "test"));
    when(secretPassword.decrypt()).thenReturn("obo".toCharArray());

    dao.set(readEntity);
    readEntity = dao.get().orElse(null);
    usernameConfig = (UsernameAuthenticationConfiguration) readEntity.getAuthentication();

    assertNotNull(readEntity);
    assertEquals("bar", usernameConfig.getUsername());
    assertEquals("obo", new String(usernameConfig.getPassword().decrypt()));
  }

  @Test
  public void canUpdateProxyConfiguration() {
    ProxyServerConfiguration httpConfig = createProxyServerConfig(true, "localhost", 8081);
    ProxyServerConfiguration httpsConfig = createProxyServerConfig(false, "127.0.0.1", 8082);

    ProxyConfiguration proxy = new ProxyConfiguration();
    proxy.setHttp(httpConfig);
    proxy.setHttps(httpsConfig);
    proxy.setNonProxyHosts(new String[]{"foo", "bar"});

    HttpClientConfigurationData config = new HttpClientConfigurationData();
    config.setProxy(proxy);

    dao.set(config);
    HttpClientConfigurationData readEntity = dao.get().orElse(null);

    assertNotNull(readEntity);
    assertArrayEquals(new String[]{"foo", "bar"}, readEntity.getProxy().getNonProxyHosts());
    assertProxyServerConfig(readEntity.getProxy().getHttp(), true, "localhost", 8081);
    assertProxyServerConfig(readEntity.getProxy().getHttps(), false, "127.0.0.1", 8082);

    readEntity.getProxy().getHttp().setEnabled(false);
    readEntity.getProxy().getHttps().setPort(1234);
    readEntity.getProxy().setNonProxyHosts(new String[]{"baz"});

    dao.set(readEntity);
    readEntity = dao.get().orElse(null);

    assertNotNull(readEntity);
    assertArrayEquals(new String[]{"baz"}, readEntity.getProxy().getNonProxyHosts());
    assertProxyServerConfig(readEntity.getProxy().getHttp(), false, "localhost", 8081);
    assertProxyServerConfig(readEntity.getProxy().getHttps(), false, "127.0.0.1", 1234);
  }

  private ProxyServerConfiguration createProxyServerConfig(boolean enabled, String host, int port) {
    ProxyServerConfiguration config = new ProxyServerConfiguration();
    config.setEnabled(enabled);
    config.setHost(host);
    config.setPort(port);
    return config;
  }

  private void assertProxyServerConfig(ProxyServerConfiguration config, boolean enabled, String host, int port) {
    assertEquals(enabled, config.isEnabled());
    assertEquals(host, config.getHost());
    assertEquals(port, config.getPort());
  }
}
