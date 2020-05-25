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
package org.sonatype.nexus.repository.internal.blobstore

import org.sonatype.nexus.blobstore.group.BlobStoreGroup
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static org.sonatype.nexus.datastore.api.DataStoreManager.CONFIG_DATASTORE_NAME

@Category(SQLTestGroup.class)
class BlobStoreConfigurationDAOTest
  extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(BlobStoreConfigurationDAO)

  DataSession session

  BlobStoreConfigurationDAO mapper

  void setup() {
    session = sessionRule.openSession(CONFIG_DATASTORE_NAME)
    mapper = session.access(BlobStoreConfigurationDAO)
  }

  def 'browsing items works as expected'() {
    when: 'many configs are created'
      (1..5).each {
        mapper.create(new BlobStoreConfigurationData(name: "name-$it",
            type: "type-$it", attributes: [:]))
      }
    then: 'the items can be browsed'
      def items = mapper.browse().collect()
      items.size() == 5
  }

  def 'Create, read, update, and delete operations work'() {
    given: 'a BlobStoreConfiguration'
      def config = new BlobStoreConfigurationData(name: "name", type: "type", attributes: [:])
    when: 'a BlobStoreConfiguration is created'
      mapper.create(config)
    and: 'it is read back'
      def readBack = mapper.readByName(config.name)
    then: 'you read the correct values'
      readBack.isPresent()
      readBack.get().name == config.name
      readBack.get().type == config.type

    when: 'it is updated'
      config.type = 'newType'
      mapper.update(config)
    and: 'the update is read back'
      def updated = mapper.readByName(config.name)
    then: 'the values are correct'
      updated.isPresent()
      updated.get().name == config.name
      updated.get().type == 'newType'

    when: 'it is deleted'
      def isDeleted = mapper.deleteByName(config.name)
    then: 'it does not exist anymore'
      isDeleted
      !mapper.readByName(config.name).isPresent()
      mapper.browse().collect().size() == 0
  }

  def 'Finding the parent of a blob store works'() {
    given: 'a group and its members'
      def memberNames = ['A', 'B', 'C']
      def members = memberNames
          .collect { new BlobStoreConfigurationData(name: it, type: "file", attributes: [:]) }
      def config = new BlobStoreConfigurationData(
          name: "parent",
          type: BlobStoreGroup.TYPE,
          attributes: ['group':['members': memberNames]])
    when: 'it they are created'
      members.each { mapper.create(it) }
      mapper.create(config)
    and: 'it is read back'
      def foundConfig = mapper.findCandidateParents('A').stream().findFirst()
    then: 'the correct values and all member relationships exist'
      foundConfig.get().name == 'parent'
    when: 'a config with no parent is used to find a parent'
      def noParent = mapper.findCandidateParents('42').stream().findFirst()
    then: 'no parent is found'
      !noParent.isPresent()
  }
}
