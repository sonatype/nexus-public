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
package org.sonatype.nexus.internal.selector

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.selector.SelectorConfiguration
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static com.google.common.collect.Streams.stream
import static java.util.stream.Collectors.toList
import static org.sonatype.nexus.datastore.api.DataStoreManager.CONFIG_DATASTORE_NAME

@Category(SQLTestGroup.class)
class SelectorConfigurationDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(SelectorConfigurationDAO)

  DataSession session

  SelectorConfigurationDAO dao

  void setup() {
    session = sessionRule.openSession(CONFIG_DATASTORE_NAME)
    dao = session.access(SelectorConfigurationDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'create read update delete'() {
    given: 'a SelectorConfiguration'
      SelectorConfiguration config = new SelectorConfigurationData(
          name: 'name', type: 'type', attributes: [foo: 'bar'])
    when: 'it is created'
      dao.create(config)
    and: 'it is read back'
      Optional<SelectorConfiguration> readValue = dao.read(config.name)
    then: 'you read the correct values'
      readValue.isPresent()
      def read = readValue.get()
      read.name == config.name
      read.description == config.description
      read.attributes == config.attributes
    when: 'it is updated'
      config.description = 'description'
      config.attributes["foo"] =  "baz"
      dao.update(config)
    and: 'it is read back'
      readValue = dao.read(config.name)
    then: 'you read the correct values'
      readValue.isPresent()
      def update = readValue.get()
      update.name == config.name
      update.description == config.description
      update.attributes == config.attributes
    when: 'it is deleted'
      dao.delete(config.name)
    and: 'it is read back'
      readValue = dao.read(config.name)
    then: 'read value will not be present'
      !readValue.isPresent()
  }

  def 'browse returns correct items'() {
    when: 'many configs are created'
      (1..5).each {
        SelectorConfiguration config = new SelectorConfigurationData(
            name: "name-$it", type: "type-$it", description: "description-$it", attributes: [:])
        dao.create(config)
      }
    then: 'the items can be browsed'
      List<SelectorConfiguration> configs = stream(dao.browse()).collect(toList())
      configs.size() == 5
  }
}
