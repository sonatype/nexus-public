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
package org.sonatype.nexus.internal.security.realm

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static org.sonatype.nexus.datastore.api.DataStoreManager.CONFIG_DATASTORE_NAME

@Category(SQLTestGroup.class)
class RealmConfigurationDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(RealmConfigurationDAO)

  DataSession session

  RealmConfigurationDAO dao

  void setup() {
    session = sessionRule.openSession(CONFIG_DATASTORE_NAME)
    dao = session.access(RealmConfigurationDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'It will read and write a single realm configuration'() {
    given: 'an item'
      def config = new RealmConfigurationData()
      config.setRealmNames(["hello", "world"])

    when: 'inserted'
      dao.set(config)

    and: 'it is read'
      def readEntity = dao.get().orElse(null)

    then: 'it is persisted'
      assert readEntity != null
      assert readEntity.realmNames.size() == 2
      assert readEntity.realmNames.contains('hello')
      assert readEntity.realmNames.contains('world')

    when: 'it is updated'
      config.setRealmNames(['foo', 'bar'])
      dao.set(config)

    and: 'it is read back'
      readEntity = dao.get().orElse(null)

    then: 'it was updated'
      assert readEntity != null
      assert readEntity.realmNames.size() == 2
      assert readEntity.realmNames.contains('foo')
      assert readEntity.realmNames.contains('bar')
  }
}
