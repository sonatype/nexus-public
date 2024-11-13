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
package org.sonatype.nexus.repository.config


import org.sonatype.nexus.common.entity.EntityId
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.repository.config.internal.ConfigurationData
import org.sonatype.nexus.repository.routing.internal.RoutingRuleDAO
import org.sonatype.nexus.repository.routing.internal.RoutingRuleData
import org.sonatype.nexus.testdb.DataSessionRule

import com.google.common.collect.ImmutableSet
import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME
import static org.sonatype.nexus.repository.routing.RoutingMode.ALLOW

@Category(SQLTestGroup.class)
class ConfigurationDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule()
      .access(RoutingRuleDAO)
      .access(ConfigurationDAO)

  DataSession session

  ConfigurationDAO dao

  RoutingRuleDAO routingRuleDAO

  EntityId id1, id2, id3

  void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)
    dao = session.access(ConfigurationDAO)
    routingRuleDAO = session.access(RoutingRuleDAO)

    def routingRule = new RoutingRuleData(id: null, name: 'foo', mode: ALLOW,
        description: 'desc1',
        matchers: ['a', 'b', 'c'])
    routingRuleDAO.create(routingRule)

    routingRule = new RoutingRuleData(id: null, name: 'bar', mode: ALLOW,
        description: 'desc2',
        matchers: ['d', 'e', 'f'])
    routingRuleDAO.create(routingRule)

    routingRule = new RoutingRuleData(id: null, name: 'baz', mode: ALLOW,
        description: 'desc1',
        matchers: ['a', 'b', 'c'])
    routingRuleDAO.create(routingRule)

    id1 = routingRuleDAO.readByName('foo').get().id
    id2 = routingRuleDAO.readByName('bar').get().id
    id3 = routingRuleDAO.readByName('baz').get().id
  }

  void cleanup() {
    session.close()
  }

  def 'create read update delete'() {
    given: 'a Configuration'
      def configuration = new ConfigurationData(name: 'foo', recipeName: 'bar', online: true,
          attributes: [baz: [buzz: 'booz']], routingRuleId: id1)

    when: 'the configuration is stored'
      dao.create(configuration)

    and: 'it is read back'
      def read = dao.readByName(configuration.name).orElse(null)

    then: 'the read value matches the original'
      read.name == configuration.name
      read.recipeName == configuration.recipeName
      read.online == configuration.online
      read.routingRuleId == configuration.routingRuleId
      read.attributes == configuration.attributes

    when: 'it is updated'
      configuration.recipeName = "notBar"
      configuration.online = false
      configuration.routingRuleId = id2
      configuration.attributes = [baz2: [buzz2: 'booz2']]
      dao.update(configuration)

    and: 'it is read back'
      ConfigurationData update = dao.readByName(configuration.name).orElse(null)

    then: 'the read value matches the update'
      update.name == configuration.name
      update.online == configuration.online
      update.routingRuleId == configuration.routingRuleId
      update.attributes == configuration.attributes

    and: 'recipe name is not changed'
      update.recipeName == 'bar'

    when: 'it is deleted'
      dao.deleteByName(configuration.name)

    then: 'no configuration is found by that name'
      !dao.readByName(configuration.name).isPresent()
  }

  def 'Password attribute can be written and read back'() {
    given: 'a Configuration'
      def configuration = new ConfigurationData(name: 'foo', recipeName: 'bar', online: true,
          attributes: [baz: [userpassword: 'booz']], routingRuleId: id1)

    when: 'the configuration is stored'
      dao.create(configuration)

    and: 'it is read back'
      def read = dao.readByName(configuration.name).orElse(null)

    then: 'the read value matches the original'
      read.getAttributes().get('baz').get('userpassword') == 'booz'
  }

  def 'read by names'() {
    given: 'a Configuration'

      def configuration1 = new ConfigurationData(name: 'foo', recipeName: 'foo', online: true,
          attributes: [baz: [buzz: 'booz']], routingRuleId: id1)

      def configuration2 = new ConfigurationData(name: 'barr', recipeName: 'barr', online: true,
          attributes: [bar: [burr: 'foo']], routingRuleId: id2)

      def configuration3 = new ConfigurationData(name: 'bazz', recipeName: 'bazz', online: true,
          attributes: [baz: [bazz: 'bar']], routingRuleId: id3)

    when: 'the configurations are stored'
      dao.create(configuration1)
      dao.create(configuration2)
      dao.create(configuration3)

    and: 'it is read back'
      def results = dao.readByNames(ImmutableSet.of('_oo', 'b%z_'))

    then: 'the result contains two items'
      results.size() == 2
    and: 'the items are same as original'
      def names = results.stream().collect { it.repositoryName }
      names.contains(configuration1.name)
      names.contains(configuration3.name)
  }

  def 'read by recipe'() {
    given: 'a Configuration'
      def conanProxyConfig1 = new ConfigurationData(name: 'conan-proxy-1', recipeName: 'conan-proxy', online: true,
          attributes: [baz: [buzz: 'booz']], routingRuleId: id1)
      def conanProxyConfig2 = new ConfigurationData(name: 'conan-proxy-2', recipeName: 'conan-proxy', online: true,
          attributes: [baz: [buzz: 'booz']], routingRuleId: id1)
      def conanProxyConfig3 = new ConfigurationData(name: 'conan-proxy-3', recipeName: 'conan-proxy', online: true,
          attributes: [baz: [buzz: 'booz']], routingRuleId: id1)
      def conanProxyConfig4 = new ConfigurationData(name: 'conan-proxy-4', recipeName: 'conan-proxy', online: true,
          attributes: [baz: [buzz: 'booz']], routingRuleId: id1)

      def anotherConfig1 = new ConfigurationData(name: 'foo', recipeName: 'foo', online: true,
          attributes: [baz: [buzz: 'booz']], routingRuleId: id1)
      def anotherConfig2 = new ConfigurationData(name: 'barr', recipeName: 'barr', online: true,
          attributes: [bar: [burr: 'foo']], routingRuleId: id2)
      def anotherConfig3 = new ConfigurationData(name: 'bazz', recipeName: 'bazz', online: true,
          attributes: [baz: [bazz: 'bar']], routingRuleId: id3)

    when: 'configurations are stored'
      dao.create(conanProxyConfig1)
      dao.create(conanProxyConfig2)
      dao.create(conanProxyConfig3)
      dao.create(conanProxyConfig4)
      dao.create(anotherConfig1)
      dao.create(anotherConfig2)
      dao.create(anotherConfig3)

    and: 'it is read back'
      def results = dao.readByRecipe('conan-proxy')

    then: 'the result contains only configurations with given recipe'
      results.size() == 4
  }
}
