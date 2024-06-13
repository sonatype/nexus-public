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
package org.sonatype.nexus.internal.email

import java.util.function.Function
import javax.inject.Provider
import javax.net.ssl.SSLContext

import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.email.EmailConfiguration
import org.sonatype.nexus.email.EmailConfigurationChangedEvent
import org.sonatype.nexus.ssl.TrustStore

import org.apache.commons.mail.Email
import org.apache.commons.mail.SimpleEmail
import spock.lang.Specification
import spock.lang.Unroll
/**
 * Tests for {@link EmailManagerImpl}.
 */
class EmailManagerImplTest
    extends Specification
{
  def 'Configures emails to use the provided SSL and TLS configuration settings'() {
    given: 'A configured EmailManagerImpl instance'
      TrustStore trustStore = Mock(TrustStore)
      trustStore.getSSLContext() >> SSLContext.getDefault()
      EmailManagerImpl impl =
          new EmailManagerImpl(Mock(EventManager), Mock(EmailConfigurationStore), trustStore, Mock(Function), Mock(Provider))

    and: 'an populated EmailConfiguration'
      def emailConfig = Mock(EmailConfiguration) {
        _ * getHost() >> 'example.com'
        _ * getPort() >> 25
        _ * getFromAddress() >> 'sender@example.com'
        _ * isStartTlsEnabled() >> startTlsEnabled
        _ * isStartTlsRequired() >> startTlsRequired
        _ * isSslOnConnectEnabled() >> sslOnConnect
        _ * isSslCheckServerIdentityEnabled() >> checkServerIdentity
        _ * isNexusTrustStoreEnabled() >> useTrustStore
      }
    when: 'the specified email configuration is applied to the email instance'
      Email email = impl.apply(emailConfig, new SimpleEmail())
    then: 'the email will be configured accordingly'
      email.startTLSEnabled == startTlsEnabled
      email.startTLSRequired == startTlsRequired
      email.SSLOnConnect == sslOnConnect
      email.SSLCheckServerIdentity == checkServerIdentity
      email.mailSession.properties.containsKey('mail.smtp.ssl.socketFactory') == useTrustStore
      email.mailSession.properties.containsKey('mail.smtp.socketFactory.class') == hasDefaultSocketFactory
    where:
      startTlsEnabled | startTlsRequired | sslOnConnect | checkServerIdentity | useTrustStore | hasDefaultSocketFactory
      true            | false            | false        | false               | false         | false
      false           | true             | false        | false               | false         | false
      false           | false            | true         | false               | false         | true
      false           | false            | false        | true                | false         | false
      false           | false            | false        | true                | true          | false
  }

  /* Related to NEXUS-10021: postfix doesn't like empty
   * username/password credentials when authentication is turned on.
   * Make sure we pass nulls.
   */
  @Unroll
  def 'Configures emails credentials correctly for username #username and password #password.'() {
    given: 'A configured EmailManagerImpl instance'
      EmailManagerImpl impl =
          new EmailManagerImpl(Mock(EventManager), Mock(EmailConfigurationStore), Mock(TrustStore), Mock(Function), Mock(Provider))
    and: 'an populated EmailConfiguration'
      def emailConfig = Mock(EmailConfiguration) {
        _ * getHost() >> 'example.com'
        _ * getPort() >> 25
        _ * getFromAddress() >> 'sender@example.com'
        _ * getUsername() >> username
        _ * getPassword() >> password
      }
    when: 'the specified email configuration is applied to the email instance'
      Email email = impl.apply(emailConfig, new SimpleEmail())
    then: 'the email will be configured accordingly'
      email.authenticator?.passwordAuthentication?.userName == expectedUsername
      email.authenticator?.passwordAuthentication?.password == expectedPassword
    where:
      username | password | expectedUsername | expectedPassword
      'user'   | 'pwd'    | 'user'           | 'pwd'
      'user'   | ''       | 'user'           | ''
      ''       | 'pwd'    | ''               | 'pwd'
      ''       | ''       | null             | null
      null     | null     | null             | null
  }

  def 'onStoreChanged only posts a changed event for remote events'() {
    given: 'A configured EmailManagerImpl instance'
      def eventManager = Mock(EventManager)
      def emailConfigurationStore = Mock(EmailConfigurationStore)
      emailConfigurationStore.load() >> Mock(EmailConfiguration)
      EmailManagerImpl impl =
          new EmailManagerImpl(eventManager, emailConfigurationStore, Mock(TrustStore), Mock(Function), Mock(Provider))

    when: 'a local event is received'
      def localEvent = Mock(EmailConfigurationEvent)
      localEvent.isLocal() >> true
      impl.onStoreChanged(localEvent)

    then: 'the event is not posted'
      0 * eventManager.post(_ as EmailConfigurationChangedEvent)

    when: 'a remote event is received'
      def remoteEvent = Mock(EmailConfigurationEvent)
      def emailConfig = Mock(EmailConfiguration)
      remoteEvent.isLocal() >> false
      remoteEvent.getEmailConfiguration() >> emailConfig
      emailConfig.copy() >> emailConfig
      impl.onStoreChanged(remoteEvent)

    then: 'the event is posted'
      1 * eventManager.post(_ as EmailConfigurationChangedEvent)
  }
}
