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

package org.sonatype.nexus.repository.routing.internal

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.repository.routing.RoutingRule
import org.sonatype.nexus.testdb.DataSessionRule

import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static org.sonatype.nexus.datastore.api.DataStoreManager.CONFIG_DATASTORE_NAME
import static org.sonatype.nexus.repository.routing.RoutingMode.ALLOW
import static org.sonatype.nexus.repository.routing.RoutingMode.BLOCK

@Category(SQLTestGroup.class)
class RoutingRuleDAOTest
    extends Specification
{
  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(RoutingRuleDAO)

  DataSession session

  RoutingRuleDAO dao

  void setup() {
    session = sessionRule.openSession(CONFIG_DATASTORE_NAME)
    dao = session.access(RoutingRuleDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'create read update delete'() {
    given: 'a routing rule'
      def routingRule = new RoutingRuleData(name: 'foo', mode: ALLOW,
          description: 'desc',
          matchers: ['a', 'b', 'c'])
    when: 'the routing rule is stored'
      dao.create(routingRule)
    and: 'it is read back'
      def read = dao.read(routingRule.id).orElse(null)
    then: 'the read value matches the original'
      read.id == routingRule.id
      read.name == routingRule.name
      read.mode == routingRule.mode
      read.description == routingRule.description
      read.matchers == routingRule.matchers
    when: 'the routing rule is found by name'
      read = dao.readByName(routingRule.name).get()
    then: 'the found value matches the original'
      read.name == routingRule.name
      read.mode == routingRule.mode
      read.description == routingRule.description
      read.matchers == routingRule.matchers
    when: 'it is updated'
      routingRule.name = 'foo2'
      routingRule.mode = BLOCK
      routingRule.description = 'desc2'
      routingRule.matchers = ['x', 'y', 'z']
      dao.update(routingRule)
    and: 'it is read back'
      def update = dao.read(routingRule.id).orElse(null)
    then: 'the read value matches the update'
      update.name == routingRule.name
      update.mode == routingRule.mode
      update.description == routingRule.description
      update.matchers == routingRule.matchers
    when: 'the updated routing rule is found by name'
      read = dao.readByName(routingRule.name).get()
    then: 'the found value matches the original'
      read.name == routingRule.name
      read.mode == routingRule.mode
      read.description == routingRule.description
      read.matchers == routingRule.matchers
    when: 'it is deleted'
      dao.delete(routingRule.id)
    then: 'no routingRule is found by that name'
      !dao.read(routingRule.id).isPresent()
    when: 'several rules are created'
      (1..5).each {
        dao.create(new RoutingRuleData(
            name: "foo${it}",
            mode: ALLOW,
            matchers: ['a']
        ))
      }
    then: 'browsing finds them all'
      List<RoutingRule> items = dao.browse()
      items.size() == 5
    when: 'a rule with the same name is created'
      dao.create(new RoutingRuleData(name: 'food', mode: ALLOW))
      dao.create(new RoutingRuleData(name: 'food', mode: ALLOW))
    then: 'a exception is thrown'
      thrown Exception
  }

}
