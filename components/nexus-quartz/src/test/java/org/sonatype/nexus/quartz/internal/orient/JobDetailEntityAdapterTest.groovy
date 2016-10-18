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
package org.sonatype.nexus.quartz.internal.orient

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.entity.EntityHelper
import org.sonatype.nexus.orient.entity.AttachedEntityHelper
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule
import org.sonatype.nexus.quartz.internal.task.QuartzTaskJob

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.quartz.JobDataMap
import org.quartz.impl.JobDetailImpl

/**
 * Tests for {@link JobDetailEntityAdapter}.
 */
class JobDetailEntityAdapterTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory('test')

  private JobDetailEntityAdapter underTest

  @Before
  void setUp() {
    underTest = new JobDetailEntityAdapter()

    database.instance.connect().withCloseable {db ->
      underTest.register(db)
    }
  }

  @Test
  void 'add JobDetail'() {
    def jobDetail1 = new JobDetailImpl(
        name: 'test-name',
        group: 'test-group',
        description: 'test-description',
        durability: true,
        requestsRecovery: false,
        jobClass: QuartzTaskJob.class,
        jobDataMap: new JobDataMap([
            'foo': 1
        ])
    )
    println jobDetail1

    def entity1 = new JobDetailEntity(jobDetail1)
    println entity1

    // verify setting value populates some entity bits
    assert entity1.name == jobDetail1.name
    assert entity1.group == jobDetail1.group
    assert entity1.jobType == jobDetail1.jobClass.name

    // add the entity
    database.instance.acquire().withCloseable {db ->
      underTest.addEntity(db, entity1)

      def doc = AttachedEntityHelper.document(entity1)
      println doc.toJSON("prettyPrint")
    }

    // extract the id
    def id1 = EntityHelper.id(entity1)
    println id1

    // lookup the entity and verify its contents
    database.instance.acquire().withCloseable {db ->
      def entity2 = underTest.read(db, id1)
      println entity2

      assert entity2.name == entity1.name
      assert entity2.group == entity1.group
      assert entity2.jobType == entity1.jobType
      assert entity2.value != null
      assert entity2.value instanceof JobDetailImpl

      JobDetailImpl jobDetail2 = (JobDetailImpl)entity2.value
      println jobDetail2

      assert jobDetail2.key == jobDetail1.key
      assert jobDetail2.name == jobDetail1.name
      assert jobDetail2.group == jobDetail1.group
      assert jobDetail2.description == jobDetail1.description
      assert jobDetail2.durable == jobDetail1.durable
      assert jobDetail2.requestsRecovery() == jobDetail1.requestsRecovery()
      assert jobDetail2.jobClass == jobDetail1.jobClass
      assert jobDetail2.jobDataMap.size() == 1
      assert jobDetail2.jobDataMap.get('foo') == 1
    }
  }

  // TODO: Add tests for readyByKey, existsByKey, deleteByKey, browseWithPredicate and deleteAll
}
