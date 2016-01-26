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

import javax.inject.Provider
import javax.net.ssl.SSLContext

import org.sonatype.nexus.ssl.TrustStore

import org.sonatype.nexus.email.EmailConfiguration
import org.sonatype.nexus.email.EmailConfigurationStore

import org.apache.commons.mail.Email
import org.apache.commons.mail.SimpleEmail
import spock.lang.Specification

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
      EmailManagerImpl impl = new EmailManagerImpl(Mock(EmailConfigurationStore), trustStore, Mock(Provider))
    when: 'the specified email configuration is applied to the email instance'
      Email email = impl.apply(
          new EmailConfiguration(
              host: 'example.com',
              port: 25,
              fromAddress: 'sender@example.com',
              startTlsEnabled: startTlsEnabled,
              startTlsRequired: startTlsRequired,
              sslOnConnectEnabled: sslOnConnect,
              sslCheckServerIdentityEnabled: checkServerIdentity,
              nexusTrustStoreEnabled: useTrustStore),
          new SimpleEmail())
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
}
