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

package org.sonatype.nexus.internal.capability.storage

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME

@Category(SQLTestGroup.class)
class CapabilityStorageItemDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(CapabilityStorageItemDAO)

  DataSession session

  CapabilityStorageItemDAO dao

  CapabilityStorageImpl store

  void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)
    dao = session.access(CapabilityStorageItemDAO)
    store = new CapabilityStorageImpl(sessionRule)
  }

  void cleanup() {
    session.close()
  }

  def 'It will create, read, update, delete and browse capability storage items'() {
    given: 'an item'
      CapabilityStorageItem entity = store.newStorageItem(1, 'type', true, 'notes', [foo: 'bar'])
    when: 'it is created'
      dao.create(entity)
    and: 'it is read'
      def readEntity = dao.read(entity.id)
    then: 'it is persisted'
      readEntity.get().version == 1
      readEntity.get().type == 'type'
      readEntity.get().enabled
      readEntity.get().notes == 'notes'
      readEntity.get().properties == [foo: 'bar']
    when: 'it is updated'
      entity.version = 2
      entity.type = 'type2'
      entity.enabled = false
      entity.notes = 'notes2'
      entity.properties = [foo: 'bar2']
      dao.update(entity)
    and: 'it is read'
      readEntity = dao.read(entity.id)
    then: 'it is persisted with updates'
      readEntity.get().version == 2
      readEntity.get().type == 'type2'
      !readEntity.get().enabled
      readEntity.get().notes == 'notes2'
      readEntity.get().properties == [foo: 'bar2']
    when: 'it is deleted'
      dao.delete(entity.id)
    then: 'it does not exist'
      !dao.read(entity.id).isPresent()
    when: 'many items are created'
      (2..5).each {
        dao.create(store.newStorageItem( it, "type $it", true, "notes $it", [foo: "bar $it"]))
      }
    then: 'the items can be browsed'
      def items = dao.browse().collect()
      items.size() == 4
  }
}
