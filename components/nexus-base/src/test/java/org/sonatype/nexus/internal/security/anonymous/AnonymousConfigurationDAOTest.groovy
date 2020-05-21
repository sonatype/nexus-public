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
package org.sonatype.nexus.internal.security.anonymous

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static org.sonatype.nexus.datastore.api.DataStoreManager.CONFIG_DATASTORE_NAME

@Category(SQLTestGroup.class)
class AnonymousConfigurationDAOTest extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(AnonymousConfigurationDAO)

  DataSession session

  AnonymousConfigurationDAO dao

  void setup() {
    session = sessionRule.openSession(CONFIG_DATASTORE_NAME)
    dao = session.access(AnonymousConfigurationDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'create read update delete'() {
    given: 'an anonymous configuration'
      def config = new AnonymousConfigurationData(enabled: true, userId: 'anon', realmName: 'local')
    when: 'it is created'
      dao.set(config)
    and: 'it is read back'
      def read = dao.get().orElse(null)
    then:
      read != null
      read.enabled
      read.userId == 'anon'
      read.realmName == 'local'
    when: 'it is updated'
      config.enabled = false
      config.userId = 'anonymous'
      config.realmName = 'remote'
      dao.set(config)
    and: 'it is read back'
      read = dao.get().orElse(null)
    then:
      read != null
      !read.enabled
      read.userId == 'anonymous'
      read.realmName == 'remote'
    when: 'it is deleted'
      dao.clear()
    and: 'it is read back'
      read = dao.get().orElse(null)
    then: 'it will be null'
      read == null
  }
}
