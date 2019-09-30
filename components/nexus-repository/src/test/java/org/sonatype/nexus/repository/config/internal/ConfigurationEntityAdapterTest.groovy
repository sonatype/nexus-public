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
package org.sonatype.nexus.repository.config.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.orient.HexRecordIdObfuscator
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.routing.RoutingRulesConfiguration
import org.sonatype.nexus.repository.routing.internal.RoutingRuleEntityAdapter
import org.sonatype.nexus.security.PasswordHelper

import com.google.common.collect.Iterables
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * Tests for {@link ConfigurationEntityAdapter}.
 */
class ConfigurationEntityAdapterTest
  extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory('test')

  @Mock
  private PasswordHelper passwordHelper

  @Mock
  private RoutingRulesConfiguration routingRulesConfiguration

  @Mock private RoutingRuleEntityAdapter routingRuleEntityAdapter

  private ConfigurationEntityAdapter underTest

  @Before
  void setUp() {
    when(passwordHelper.encrypt('s1mpl3')).thenReturn('******')
    when(passwordHelper.tryDecrypt('******')).thenReturn('s1mpl3')
    when(routingRulesConfiguration.isEnabled()).thenReturn(true)

    underTest = new ConfigurationEntityAdapter(passwordHelper, routingRulesConfiguration, routingRuleEntityAdapter)
    underTest.enableObfuscation(new HexRecordIdObfuscator())
  }

  @After
  void tearDown() {
    underTest = null
  }

  @Test
  void 'register schema'() {
    database.instance.connect().withCloseable { db ->
      underTest.register(db)
    }
  }

  @Test
  void 'add simple entity'() {
    database.instance.connect().withCloseable { db ->
      underTest.register(db)

      def config = new Configuration(repositoryName: 'bar', recipeName: 'foo')
      config.attributes('baz').set('a', 'b')

      underTest.addEntity(db, config)
    }
  }

  @Test(expected = ORecordDuplicatedException)
  void 'index on name is case-insensitive'() {
    database.instance.connect().withCloseable { db ->
      underTest.register(db)

      def config = new Configuration(repositoryName: 'bar', recipeName: 'foo', attributes: [:])
      underTest.addEntity(db, config)
      def conflictingConfig = new Configuration(repositoryName: config.repositoryName.capitalize(),
          recipeName: config.recipeName, attributes: config.attributes)
      underTest.addEntity(db, conflictingConfig)
    }
  }

  @Test
  void 'sensitive entity'() {
    database.instance.connect().withCloseable { db ->
      underTest.register(db)

      def config = new Configuration(repositoryName: 'bar', recipeName: 'foo')
      config.attributes('baz').set('password', 's1mpl3')

      def document = underTest.addEntity(db, config)

      assert document.field('attributes.baz.password') == '******'

      def entity = underTest.readEntity(document)

      assert entity.attributes['baz']['password'] == 's1mpl3'
    }
  }

  @Test
  void 'can modify group membership outside of transaction'() {
    database.instance.connect().withCloseable { db ->
      underTest.register(db)
    }

    database.instance.acquire().withCloseable { db ->
      db.begin()

      def config = new Configuration(repositoryName: 'bar', recipeName: 'foo')
      config.attributes('group').set('memberNames', [] as Collection)
      underTest.addEntity(db, config)

      db.commit()
    }

    Configuration config
    database.instance.acquire().withCloseable { db ->
      db.begin()

      config = Iterables.getFirst(underTest.browse(db), null)
      config.attributes('group').get('memberNames', Collection.class).add('testRepo')
      underTest.editEntity(db, config)

      db.commit()
    }

    /*
     * Check the group membership can be modified outside of the transaction.
     *
     * This used to fail because the membership sequence was left backed by an
     * OTrackedList which expected to find a live DB context whenever the list
     * was mutated. We now wrap the config's attributes so any tracked entries
     * will automatically detach when this isn't the case.
     */

    config.attributes('group').get('memberNames', Collection.class).remove('testRepo')
  }

  // FIXME: Below use protected bits to test, not easy to expose for testing w/o exposing too much api in impls
  // FIXME: Groovy may or may not ignore access modifiers in the future so should sort out how to better test

//  @Test
//  void 'read simple entity'() {
//    def db = database.instance.connect()
//    try {
//      underTest.register(db)
//
//      def config1 = new Configuration()
//      config1.recipeName = 'foo'
//      config1.repositoryName = 'bar'
//      def attr1 = config1.attributes('baz')
//      attr1.set('a', 'b')
//
//      def doc = underTest.add(db, config1)
//      log doc.toJSON()
//
//      def config2 = underTest.readEntity(doc)
//      assert config2.recipeName == 'foo'
//      assert config2.repositoryName == 'bar'
//      assert config2.attributes != null
//      assert config2.attributes.size() == 1
//      log config2.attributes.getClass()
//
//      def attr2 = config2.attributes('baz')
//      assert attr2 != null
//      assert attr2.get('a') == 'b'
//    }
//    finally {
//      db.close()
//    }
//  }

//  @Test
//  void 'read detached entity'() {
//    def detached
//    def db = database.instance.connect()
//    try {
//      underTest.register(db)
//
//      def config1 = new Configuration()
//      config1.recipeName = 'foo'
//      config1.repositoryName = 'bar'
//      def attr1 = config1.attributes('baz')
//      attr1.set('a', 'b')
//
//      def doc = underTest.add(db, config1)
//      detached = underTest.readEntity(doc)
//    }
//    finally {
//      db.close()
//    }
//
//    def attr2 = detached.attributes('baz')
//    assert attr2 != null
//    assert attr2.get('a') == 'b'
//
//    def attr3 = detached.attributes('more')
//    assert attr3 != null
//    attr3.set('a', 'b')
//  }
}
