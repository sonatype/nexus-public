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
package org.sonatype.nexus.internal.provisioning

import org.sonatype.goodies.common.Time
import org.sonatype.nexus.CoreApi
import org.sonatype.nexus.capability.CapabilityContext
import org.sonatype.nexus.capability.CapabilityDescriptor
import org.sonatype.nexus.capability.CapabilityIdentity
import org.sonatype.nexus.capability.CapabilityRegistry
import org.sonatype.nexus.capability.CapabilityType
import org.sonatype.nexus.crypto.secrets.SecretsService
import org.sonatype.nexus.httpclient.HttpClientManager
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration
import org.sonatype.nexus.httpclient.config.ProxyConfiguration
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration
import org.sonatype.nexus.internal.app.BaseUrlCapabilityDescriptor
import org.sonatype.nexus.internal.capability.DefaultCapabilityReference
import org.sonatype.nexus.internal.httpclient.TestHttpClientConfiguration
import org.sonatype.nexus.security.UserIdHelper

import org.apache.shiro.SecurityUtils
import org.apache.shiro.mgt.SecurityManager
import org.apache.shiro.subject.Subject
import org.apache.shiro.subject.support.SubjectThreadState
import org.apache.shiro.web.util.RequestPairSource
import org.junit.After
import org.junit.Before
import org.mockito.Mock
import org.mockito.MockedStatic
import spock.lang.Specification

import static org.mockito.Mockito.mockStatic
import static org.sonatype.nexus.httpclient.config.AuthenticationConfiguration.AUTHENTICATION_CONFIGURATION

/**
 * Tests for {@link CoreApiImpl}
 * @since 3.0
 */
class CoreApiImplTest
    extends Specification
{
  CapabilityRegistry capabilityRegistry = Mock()

  DefaultCapabilityReference reference = Mock()

  CapabilityContext context = Mock()

  CapabilityDescriptor descriptor = Mock()

  HttpClientManager httpClientManager = Mock()

  SecretsService secretsService = Mock()

  CapabilityIdentity identity = new CapabilityIdentity('test')

  CoreApi api = new CoreApiImpl(capabilityRegistry, httpClientManager, secretsService)

  private static final ProxyConfiguration EXISTING_HTTP = new ProxyConfiguration(
      http: new ProxyServerConfiguration(enabled: true, host: 'http', port: 1))

  @Mock
  Subject subject

  MockedStatic<SecurityUtils> securityUtils

  @Mock
  SecurityManager securityManager

  def setup() {
    securityUtils = mockStatic(SecurityUtils.class)
    securityUtils.when({ -> SecurityUtils.getSubject() }).thenReturn(subject)
    setSecurityManager(securityManager)
  }

  def cleanup() {
    securityUtils.close()
  }

  def 'Can set base url without an existing capability'() {
    given:
      List existingCapabilities = [reference]

    when:
      api.baseUrl('foo')

    then:
      1 * capabilityRegistry.all >> existingCapabilities
      1 * reference.context() >> context
      1 * context.descriptor() >> descriptor
      1 * descriptor.type() >> new CapabilityType('not baseurl')
      1 * capabilityRegistry.add(BaseUrlCapabilityDescriptor.TYPE, true, _, [url: 'foo'])
  }

  def 'Can set base url with an existing capability'() {
    given:
      List existingCapabilities = [reference]

    when:
      api.baseUrl('bar')

    then:
      1 * capabilityRegistry.all >> existingCapabilities
      1 * reference.context() >> context
      1 * context.descriptor() >> descriptor
      1 * descriptor.type() >> BaseUrlCapabilityDescriptor.TYPE
      1 * reference.id() >> identity
      1 * reference.active >> true
      1 * reference.notes() >> 'whatever'
      1 * capabilityRegistry.update(identity, true, 'whatever', [url: 'bar'])
  }

  def 'Can delete an existing base url capability'() {
    given:
      List existingCapabilities = [reference]

    when:
      api.removeBaseUrl()

    then:
      1 * capabilityRegistry.all >> existingCapabilities
      1 * reference.context() >> context
      1 * context.descriptor() >> descriptor
      1 * descriptor.type() >> BaseUrlCapabilityDescriptor.TYPE
      1 * reference.id() >> identity
      1 * capabilityRegistry.remove(identity)
  }

  def 'Can delete when base url capability is not configured'() {
    given:
      List existingCapabilities = []

    when:
      api.removeBaseUrl()

    then:
      1 * capabilityRegistry.all >> existingCapabilities
      0 * capabilityRegistry.remove(identity)
  }

  def 'Can set http proxy settings without auth'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration()

    when:
      api.httpProxy('http', 1)

    then:
      0 * secretsService.encryptMaven(_,_,_)
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      configuration.proxy
      with(configuration.proxy) {
        http
        http.host == 'http'
        http.port == 1
        !http.authentication
        !https
      }
  }

  def 'Can set http proxy settings with basic auth'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration()

    when:
      api.httpProxyWithBasicAuth('http', 1, 'user', 'pass')

    then:
      1 * secretsService.encryptMaven(AUTHENTICATION_CONFIGURATION,'pass'.chars, UserIdHelper.get())
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      configuration.proxy
      with(configuration.proxy) {
        http
        http.host == 'http'
        http.port == 1
        http.authentication
        http.authentication instanceof UsernameAuthenticationConfiguration
        http.authentication.username == 'user'
        http.authentication.password ==
            secretsService.encrypt(AUTHENTICATION_CONFIGURATION, 'pass'.toCharArray(), UserIdHelper.get())
        !https
      }
  }

  def 'Can set http proxy settings with NTLM auth'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration()

    when:
      api.httpProxyWithNTLMAuth('http', 1, 'user', 'pass', 'ntlmHost', 'domain')

    then:
      1 * secretsService.encryptMaven(AUTHENTICATION_CONFIGURATION,'pass'.chars, UserIdHelper.get())
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      configuration.proxy
      with(configuration.proxy) {
        http
        http.host == 'http'
        http.port == 1
        http.authentication
        http.authentication instanceof NtlmAuthenticationConfiguration
        http.authentication.username == 'user'
        http.authentication.password ==
            secretsService.encryptMaven(AUTHENTICATION_CONFIGURATION, 'pass'.toCharArray(), UserIdHelper.get())
        http.authentication.host == 'ntlmHost'
        http.authentication.domain == 'domain'
        !https
      }
  }

  def 'Can remove http proxy configuration'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration(proxy: EXISTING_HTTP)

    when:
      api.removeHTTPProxy()

    then:
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      !configuration.proxy
  }

  def 'Cannot set https proxy without http proxy'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration()

    when:
      api.httpsProxy('https', 1)

    then:
      1 * httpClientManager.getConfiguration() >> configuration
      IllegalStateException e = thrown()
      e.message == 'Cannot configure https proxy without http proxy'
      0 * httpClientManager.setConfiguration(_)
  }

  def 'Can set https proxy settings without auth'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration(proxy: EXISTING_HTTP)

    when:
      api.httpsProxy('https', 2)

    then:
      0 * secretsService.encryptMaven(AUTHENTICATION_CONFIGURATION,'pass'.chars, UserIdHelper.get())
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      configuration.proxy
      with(configuration.proxy) {
        http
        https
        https.host == 'https'
        https.port == 2
        !http.authentication
      }
  }

  def 'Can set https proxy settings with basic auth'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration(proxy: EXISTING_HTTP)

    when:
      api.httpsProxyWithBasicAuth('https', 2, 'user', 'pass')

    then:
      1 * secretsService.encryptMaven(AUTHENTICATION_CONFIGURATION,'pass'.chars, UserIdHelper.get())
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      configuration.proxy
      with(configuration.proxy) {
        http
        https
        https.host == 'https'
        https.port == 2
        https.authentication
        https.authentication instanceof UsernameAuthenticationConfiguration
        https.authentication.username == 'user'
        https.authentication.password ==
            secretsService.encryptMaven(AUTHENTICATION_CONFIGURATION, 'pass'.toCharArray(), UserIdHelper.get())

      }
  }

  def 'Can set https proxy settings with NTLM auth'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration(proxy: EXISTING_HTTP)

    when:
      api.httpsProxyWithNTLMAuth('https', 2, 'user', 'pass', 'ntlmHost', 'domain')

    then:
      1 * secretsService.encryptMaven(AUTHENTICATION_CONFIGURATION,'pass'.chars, UserIdHelper.get())
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      configuration.proxy
      with(configuration.proxy) {
        http
        https
        https.host == 'https'
        https.port == 2
        https.authentication
        https.authentication instanceof NtlmAuthenticationConfiguration
        https.authentication.username == 'user'
        https.authentication.password ==
            secretsService.encryptMaven(AUTHENTICATION_CONFIGURATION, 'pass'.toCharArray(), UserIdHelper.get())
        https.authentication.host == 'ntlmHost'
        https.authentication.domain == 'domain'
      }
  }

  def 'Can remove https proxy configuration'() {
    given:
      ProxyConfiguration proxies = EXISTING_HTTP.copy()
      proxies.https = new ProxyServerConfiguration(enabled: true, host:'https', port: 2)
      HttpClientConfiguration configuration = new TestHttpClientConfiguration(proxy: proxies)

    when:
      api.removeHTTPSProxy()

    then:
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      configuration.proxy
      configuration.proxy.http
      !configuration.proxy.https
  }

  def 'Can configure connection timeout'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration()

    when:
      api.connectionTimeout(5)

    then:
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      configuration.connection.timeout == Time.seconds(5)
  }

  def 'Configure connection timeout fails when values are out of bounds'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration()

    when:
      api.connectionTimeout(value)

    then:
      0 * httpClientManager.getConfiguration() >> configuration
      0 * httpClientManager.setConfiguration(_)
      IllegalArgumentException e = thrown()

    where:
      value << [-1, 3601]
  }

  def 'Can configure connection retries'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration()

    when:
      api.connectionRetryAttempts(5)

    then:
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      configuration.connection.retries == 5
  }

  def 'Configure connection retries fails when values are out of bounds'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration()

    when:
      api.connectionRetryAttempts(value)

    then:
      0 * httpClientManager.getConfiguration() >> configuration
      0 * httpClientManager.setConfiguration(_)
      IllegalArgumentException e = thrown()

    where:
      value << [-1, 11]
  }

  def 'Can customize user agent'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration()

    when:
      api.userAgentCustomization('foo')

    then:
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      configuration.connection.userAgentSuffix == 'foo'
  }

  def 'Can set and unset non-proxy hosts'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration(proxy: EXISTING_HTTP)

    when:
      api.nonProxyHosts('foo', 'bar')

    then:
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      configuration.proxy.nonProxyHosts == ['foo', 'bar'] as String[]

    when:
      api.nonProxyHosts()

    then:
      1 * httpClientManager.getConfiguration() >> configuration
      1 * httpClientManager.setConfiguration(_)
      !configuration.proxy.nonProxyHosts
  }

  def 'Cannot non-proxy hosts without a proxy configured'() {
    given:
      HttpClientConfiguration configuration = new TestHttpClientConfiguration()

    when:
      api.nonProxyHosts('foo', 'bar')

    then:
      1 * httpClientManager.getConfiguration() >> configuration
      0 * httpClientManager.setConfiguration(_)
      IllegalStateException e = thrown()
  }
}
