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
package org.sonatype.nexus.internal.httpclient

import java.util.concurrent.TimeUnit

import org.sonatype.goodies.common.Time
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl
import org.sonatype.nexus.crypto.internal.MavenCipherImpl
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration
import org.sonatype.nexus.httpclient.config.ProxyConfiguration
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration
import org.sonatype.nexus.internal.httpclient.handlers.AuthenticationConfigurationHandler
import org.sonatype.nexus.internal.httpclient.handlers.ConnectionConfigurationHandler
import org.sonatype.nexus.internal.httpclient.handlers.ProxyConfigurationHandler
import org.sonatype.nexus.security.PasswordHelper
import org.sonatype.nexus.crypto.PhraseService
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME

@Category(SQLTestGroup.class)
class HttpClientConfigurationDAOTest
    extends Specification
{
  PasswordHelper passwordHelper = new PasswordHelper(new MavenCipherImpl(new CryptoHelperImpl()),
      PhraseService.LEGACY_PHRASE_SERVICE)

  @Rule
  DataSessionRule sessionRule = new DataSessionRule()
      .handle(new ConnectionConfigurationHandler(passwordHelper))
      .handle(new ProxyConfigurationHandler(passwordHelper))
      .handle(new AuthenticationConfigurationHandler(passwordHelper))
      .access(HttpClientConfigurationDAO)

  DataSession session

  HttpClientConfigurationDAO dao

  void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)
    dao = session.access(HttpClientConfigurationDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'It can write and read with all attributes null'() {
    given: 'an item'
      def config = new HttpClientConfigurationData()

    when: 'inserted'
      dao.set(config)

    and: 'it is read back'
      def readEntity = dao.get().orElse(null)

    then: 'it is persisted'
      assert readEntity != null
      assert readEntity.connection == null
      assert readEntity.authentication == null
      assert readEntity.proxy == null
      assert readEntity.redirectStrategy == null
  }

  def 'Can delete configuration'() {
    given: 'an item'
      def config = new HttpClientConfigurationData()

    and: 'inserted'
      dao.set(config)

    when: 'item is deleted'
      dao.clear()

    and: 'read back'
      def readEntity = dao.get().orElse(null)

    then: 'result is empty'
      assert readEntity == null
  }

  def 'Can update connection configuration'() {
    given: 'an item'
      def connection = new ConnectionConfiguration(retries: Integer.MAX_VALUE,
          timeout: new Time(1, TimeUnit.SECONDS), enableCircularRedirects: false, userAgentSuffix: 'foo',
          enableCookies: true)

      def config = new HttpClientConfigurationData()
      config.connection = connection

    when: 'is inserted'
      dao.set(config)

    and: 'read back'
      def readEntity = dao.get().orElse(null)

    then: 'content has been saved'
      assert readEntity.connection.retries == Integer.MAX_VALUE
      assert readEntity.connection.timeout.asSeconds() == new Time(1, TimeUnit.SECONDS).asSeconds()
      assert !readEntity.connection.enableCircularRedirects
      assert readEntity.connection.userAgentSuffix == 'foo'
      assert readEntity.connection.enableCookies

    when: 'values are changed'
      readEntity.connection.retries = Integer.MIN_VALUE
      readEntity.connection.timeout = new Time(5, TimeUnit.MINUTES)
      readEntity.connection.enableCircularRedirects = true
      readEntity.connection.userAgentSuffix = 'bar'
      readEntity.connection.enableCookies = false

    and: 'is updated'
      dao.set(readEntity)

    and: 'read back again'
      readEntity = dao.get().orElse(null)

    then: 'values have been updated'
      assert readEntity.connection.retries == Integer.MIN_VALUE
      assert readEntity.connection.timeout.asSeconds() == new Time(5, TimeUnit.MINUTES).asSeconds()
      assert readEntity.connection.enableCircularRedirects
      assert readEntity.connection.userAgentSuffix == 'bar'
      assert !readEntity.connection.enableCookies
  }

  def 'can update authentication configuration'() {
    given: 'an item'
      def user = new UsernameAuthenticationConfiguration()
      user.username = 'foo'
      user.password = 'bob'

      def config = new HttpClientConfigurationData()
      config.authentication = user

    when: 'is inserted'
      dao.set(config)

    and: 'read back'
      def readEntity = dao.get().orElse(null)
      UsernameAuthenticationConfiguration usernameConfig =
          readEntity.authentication as UsernameAuthenticationConfiguration

    then: 'content has been saved'
      assert readEntity.authentication.type == 'username'
      assert usernameConfig.username == 'foo'
      assert usernameConfig.password == 'bob'

    when: 'values are changed'
      usernameConfig.username = 'bar'
      usernameConfig.password = 'obo'

    and: 'is updated'
      dao.set(readEntity)

    and: 'read back again'
      readEntity = dao.get().orElse(null)
      usernameConfig = readEntity.authentication as UsernameAuthenticationConfiguration

    then: 'values have been updated'
      assert usernameConfig.username == 'bar'
      assert usernameConfig.password == 'obo'
  }

  def 'Can update proxy configuration'() {
    given: 'an item'
      def httpConfig = createProxyServerConfig(true, 'localhost', 8081)
      def httpsConfig = createProxyServerConfig(false, '127.0.0.1', 8082)

      def proxy = new ProxyConfiguration()
      proxy.http = httpConfig
      proxy.https = httpsConfig
      proxy.nonProxyHosts = ['foo', 'bar']

      def config = new HttpClientConfigurationData()
      config.proxy = proxy

    when: 'item is inserted'
      dao.set(config)

    and: 'read back'
      def readEntity = dao.get().orElse(null)

    then: 'content has been saved'
      assert readEntity.proxy.nonProxyHosts == ['foo', 'bar']
      assertProxyServerConfig(readEntity.proxy.http, true, 'localhost', 8081)
      assertProxyServerConfig(readEntity.proxy.https, false, '127.0.0.1', 8082)

    when: 'values are changed'
      readEntity.proxy.http.enabled = false
      readEntity.proxy.https.port = 1234
      readEntity.proxy.nonProxyHosts = ['baz']

    and: 'item is updated'
      dao.set(readEntity)

    and: 'read back again'
      readEntity = dao.get().orElse(null)

    then: 'values have been updated'
      assert readEntity.proxy.nonProxyHosts == ['baz']
      assertProxyServerConfig(readEntity.proxy.http, false, 'localhost', 8081)
      assertProxyServerConfig(readEntity.proxy.https, false, '127.0.0.1', 1234)
  }

  private ProxyServerConfiguration createProxyServerConfig(boolean enabled, String host, int port) {
    def httpConfig = new ProxyServerConfiguration()
    httpConfig.enabled = enabled
    httpConfig.host = host
    httpConfig.port = port
    httpConfig
  }

  private void assertProxyServerConfig(ProxyServerConfiguration config, boolean enabled, String host, int port) {
    assert config.enabled == enabled
    assert config.host == host
    assert config.port == port
  }
}
