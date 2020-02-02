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
package com.sonatype.nexus.ssl.plugin.internal.keystore

import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import spock.lang.Specification

import static org.sonatype.nexus.datastore.api.DataStoreManager.CONFIG_DATASTORE_NAME

class KeyStoreDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(KeyStoreDAO)

  DataSession session

  KeyStoreDAO dao

  void setup() {
    session = sessionRule.openSession(CONFIG_DATASTORE_NAME)
    dao = session.access(KeyStoreDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'Create, read, update, and delete operations work'() {
    given: 'a KeyStoreData entity'
      KeyStoreData entity = new KeyStoreData(
          name: 'keystorename',
          bytes: [1, 2, 3] as byte[])
    when: 'a KeyStoreData is created'
      dao.save(entity)
    and: 'it is read back'
      def readBack = dao.load(entity.name)
    then: 'you read the correct values'
      readBack.isPresent()
      readBack.get().name == entity.name
      readBack.get().bytes == entity.bytes

    when: 'it is updated'
      entity.bytes = [4, 5, 6] as byte[]
      dao.save(entity)
    and: 'the update is read back'
      def updated = dao.load(entity.name)
    then: 'the values are correct'
      updated.isPresent()
      updated.get().name == entity.name
      updated.get().bytes == [4, 5, 6] as byte[]

    when: 'it is deleted'
      def isDeleted = dao.delete(entity.name)
    then: 'it does not exist anymore'
      isDeleted
      !dao.load(entity.name).isPresent()
  }
}
