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
import org.sonatype.nexus.common.entity.EntityHelper
import org.sonatype.nexus.orient.HexRecordIdObfuscator
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.routing.RoutingMode
import org.sonatype.nexus.repository.routing.RoutingRule
import org.sonatype.nexus.repository.routing.RoutingRuleStore
import org.sonatype.nexus.repository.routing.RoutingRulesConfiguration
import org.sonatype.nexus.repository.routing.internal.RoutingRuleEntityAdapter
import org.sonatype.nexus.repository.routing.internal.RoutingRuleStoreImpl
import org.sonatype.nexus.security.PasswordHelper

import com.orientechnologies.orient.core.exception.OValidationException
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock

import static org.junit.Assert.fail

/**
 * Tests for {@link ConfigurationStoreImpl}.
 */
class ConfigurationStoreImplTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory('test')

  @Mock
  private PasswordHelper passwordHelper

  private ConfigurationStoreImpl underTest

  private RoutingRuleStore routingRuleStore

  private RoutingRulesConfiguration routingRulesConfiguration = new RoutingRulesConfiguration(true)

  private RoutingRuleEntityAdapter routingRuleEntityAdapter = new RoutingRuleEntityAdapter()

  @Before
  void setUp() {
    def entityAdapter = new ConfigurationEntityAdapter(passwordHelper, routingRulesConfiguration, routingRuleEntityAdapter)
    entityAdapter.enableObfuscation(new HexRecordIdObfuscator())
    routingRuleEntityAdapter.enableObfuscation(new HexRecordIdObfuscator())

    underTest = new ConfigurationStoreImpl(
        database.instanceProvider,
        entityAdapter
    )

    underTest.start()
  }

  @After
  void tearDown() {
    if (underTest) {
      underTest.stop()
      underTest = null
    }
  }

  @Test
  void 'create configuration'() {
    def entity = new Configuration(
        repositoryName: 'foo',
        recipeName: 'bar',
        attributes: [
            'baz': [
                'a': false
            ]
        ]
    )
    log entity
    assert entity.entityMetadata == null

    underTest.create(entity)
    log entity
    assert entity.entityMetadata != null
    log entity.entityMetadata
  }

  @Test
  void 'create configuration with unique repositoryName'() {
    def create = { name ->
      underTest.create(new Configuration(
          repositoryName: name,
          recipeName: 'test'
      ))
    }

    create 'foo'

    // attempting to create with same repositoryName should fail
    try {
      create 'foo'
      fail()
    }
    catch (ORecordDuplicatedException e) {
      // expected
      log e.toString()
    }
  }

  @Test
  void 'create configuration with mandatory fields'() {
    try {
      underTest.create(new Configuration(
          // omit repositoryName
          recipeName: 'test'
      ))
      fail()
    }
    catch (OValidationException e) {
      // expected
      log e.toString()
    }

    try {
      underTest.create(new Configuration(
          repositoryName: 'test'
          // omit recipeName
      ))
      fail()
    }
    catch (OValidationException e) {
      // expected
      log e.toString()
    }
  }

  @Test
  void 'list configurations'() {
    def create = { name ->
      underTest.create(new Configuration(
          repositoryName: name,
          recipeName: 'test'
      ))
    }

    create 'foo'
    create 'bar'

    underTest.list().each { entity ->
      log entity
    }
  }

  @Test
  void 'save routing rule id'() {
    routingRuleStore = new RoutingRuleStoreImpl(database.instanceProvider, routingRuleEntityAdapter,
        routingRulesConfiguration)
    routingRuleStore.start()

    RoutingRule routingRule = routingRuleStore.create(new RoutingRule(
        name: 'test',
        description: '',
        mode: RoutingMode.ALLOW,
        matchers: ['.*']
    ))

    underTest.create(new Configuration(
        repositoryName: 'test',
        recipeName: 'test',
        routingRuleId: EntityHelper.id(routingRule)
    ))

    assert underTest.list().first().routingRuleId == EntityHelper.id(routingRule)
  }
}
