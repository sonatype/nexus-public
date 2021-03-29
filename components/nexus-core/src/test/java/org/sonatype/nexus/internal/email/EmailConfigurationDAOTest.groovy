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

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.email.EmailConfiguration
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME

@Category(SQLTestGroup.class)
class EmailConfigurationDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(EmailConfigurationDAO)

  DataSession session

  EmailConfigurationDAO dao

  void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)
    dao = session.access(EmailConfigurationDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'It will read and write a single email configuration'() {
    given: 'an item'
      EmailConfiguration entity = new EmailConfigurationData(
        enabled: true,
        host: 'localhost',
        port: 25,
        username: 'email_user',
        password: 'email_password',
        fromAddress: 'foo@example.com',
        subjectPrefix: 'PREFIX: ',
        startTlsEnabled: true,
        startTlsRequired: false,
        sslOnConnectEnabled: true,
        sslCheckServerIdentityEnabled: false,
        nexusTrustStoreEnabled: true)

    when: 'it is inserted'
      dao.set(entity)

    and: 'it is read'
      def readEntity = dao.get().orElse(null)

    then: 'it is persisted'
      readEntity.enabled == true
      readEntity.host == 'localhost'
      readEntity.port == 25
      readEntity.username == 'email_user'
      readEntity.password == 'email_password'
      readEntity.fromAddress == 'foo@example.com'
      readEntity.subjectPrefix == 'PREFIX: '
      readEntity.startTlsEnabled == true
      readEntity.startTlsRequired == false
      readEntity.sslOnConnectEnabled == true
      readEntity.sslCheckServerIdentityEnabled == false
      readEntity.nexusTrustStoreEnabled == true

    when: 'it is updated'
      readEntity.enabled = false
      readEntity.host = 'remotehost'
      readEntity.port = 26
      readEntity.username = 'email_user2'
      readEntity.password = 'email_password2'
      readEntity.fromAddress = 'bar@example.com'
      readEntity.subjectPrefix = 'XYZ: '
      readEntity.startTlsEnabled = false
      readEntity.startTlsRequired = true
      readEntity.sslOnConnectEnabled = false
      readEntity.sslCheckServerIdentityEnabled = true
      readEntity.nexusTrustStoreEnabled = false
      dao.set(readEntity)

    and: 'it is read back'
      def updatedEntity = dao.get().orElse(null)

    then: 'it was updated'
      updatedEntity.enabled == false
      updatedEntity.host == 'remotehost'
      updatedEntity.port == 26
      updatedEntity.username == 'email_user2'
      updatedEntity.password == 'email_password2'
      updatedEntity.fromAddress == 'bar@example.com'
      updatedEntity.subjectPrefix == 'XYZ: '
      updatedEntity.startTlsEnabled == false
      updatedEntity.startTlsRequired == true
      updatedEntity.sslOnConnectEnabled == false
      updatedEntity.sslCheckServerIdentityEnabled == true
      updatedEntity.nexusTrustStoreEnabled == false
  }

  def 'It can delete an email configuration'() {
    given: 'an item'
      EmailConfiguration entity = new EmailConfigurationData(
          enabled: true,
          host: 'localhost',
          port: 25,
          username: 'email_user',
          password: 'email_password',
          fromAddress: 'foo@example.com',
          subjectPrefix: 'PREFIX: ',
          startTlsEnabled: true,
          startTlsRequired: false,
          sslOnConnectEnabled: true,
          sslCheckServerIdentityEnabled: false,
          nexusTrustStoreEnabled: true)

    when: 'it is inserted'
      dao.set(entity)

    then: 'it should exist'
      def readEntity = dao.get().orElse(null)
      assert readEntity != null

    when: 'it is deleted'
      dao.clear()

    and: 'it is read back'
      readEntity = dao.get().orElse(null)

    then: 'it should not exist'
      assert readEntity == null
  }
}
