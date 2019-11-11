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
package org.sonatype.nexus.coreui

import org.sonatype.nexus.email.EmailConfiguration
import org.sonatype.nexus.email.EmailManager
import org.sonatype.nexus.rapture.PasswordPlaceholder

import spock.lang.Specification
import spock.lang.Subject
/**
 * Test for {@link EmailComponentTest}
 */
class EmailComponentTest
    extends Specification
{
  private static final String REAL_PASSWORD = 'bar'
  private static final String ADDRESS = 'you@somewhere.com'

  EmailManager emailManager = Mock()

  @Subject
  EmailComponent underTest

  def setup() {
    underTest = new EmailComponent()
    underTest.emailManager = emailManager

    emailManager.getConfiguration() >> Mock(EmailConfiguration) {
      _ * isEnabled() >> false
      _ * getHost() >> 'localhost'
      _ * getPort() >> 25
      _ * getFromAddress() >> 'nexus@example.org'
      _ * getUsername() >> 'foo'
      _ * getPassword() >> REAL_PASSWORD
    }
  }

  def 'sending verification when password is not a placeholder'() {
    given:
    def formCredentials = new EmailConfigurationXO(enabled: false, host: 'localhost', port: 25,
        fromAddress: 'nexus@example.org', username: 'foo', password: 'baz')
      def emailConfig = Mock(EmailConfiguration)
      emailManager.newConfiguration() >> emailConfig

    when:
      underTest.sendVerification(formCredentials, ADDRESS)

    then:
      1 * emailConfig.setPassword('baz')
      1 * emailManager.sendVerification(emailConfig, ADDRESS)
  }

  def 'sending verification when password is a placeholder'() {
    given:
      def formCredentials = new EmailConfigurationXO(enabled: false, host: 'localhost', port: 25,
          fromAddress: 'nexus@example.org', username: 'foo', password: PasswordPlaceholder.get())
      def emailConfig = Mock(EmailConfiguration)
      emailManager.newConfiguration() >> emailConfig

    when:
      underTest.sendVerification(formCredentials, ADDRESS)

    then:
      1 * emailConfig.setPassword(REAL_PASSWORD)
      1 * emailManager.sendVerification(emailConfig, ADDRESS)
  }

  def 'saving credentials with placeholder will save real password'() {
    given:
      def formCredentials = new EmailConfigurationXO(enabled: false, host: 'localhost', port: 25,
          fromAddress: 'nexus@example.org', username: 'foo', password: PasswordPlaceholder.get())
      def emailConfig = Mock(EmailConfiguration)
      emailManager.newConfiguration() >> emailConfig

    when:
      underTest.update(formCredentials)

    then:
      1 * emailConfig.setPassword(REAL_PASSWORD)
      1 * emailManager.setConfiguration(emailConfig)
  }

  def 'saving credentials with new password will save real password'() {
    given:
      def formCredentials = new EmailConfigurationXO(enabled: false, host: 'localhost', port: 25,
          fromAddress: 'nexus@example.org', username: 'foo', password: 'baz')
      def emailConfig = Mock(EmailConfiguration)
      emailManager.newConfiguration() >> emailConfig

    when:
      underTest.update(formCredentials)

    then:
      1 * emailConfig.setPassword('baz')
      1 * emailManager.setConfiguration(emailConfig)
  }
}
