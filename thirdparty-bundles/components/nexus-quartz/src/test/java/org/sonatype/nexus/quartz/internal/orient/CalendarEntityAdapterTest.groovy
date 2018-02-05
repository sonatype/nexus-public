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

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.quartz.impl.calendar.CronCalendar

/**
 * Tests for {@link CalendarEntityAdapter}.
 */
class CalendarEntityAdapterTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory('test')

  private CalendarEntityAdapter underTest

  @Before
  void setUp() {
    underTest = new CalendarEntityAdapter()

    database.instance.connect().withCloseable {db ->
      underTest.register(db)
    }
  }

  @Test
  void 'add CronCalendar'() {
    def calendar1 = new CronCalendar('0 0 10 * * ?')
    println calendar1

    def entity1 = new CalendarEntity(
        name: 'test-calendar',
        value: calendar1
    )
    println entity1

    database.instance.acquire().withCloseable {db ->
      underTest.addEntity(db, entity1)

      def doc = AttachedEntityHelper.document(entity1)
      println doc.toJSON("prettyPrint")
    }

    def id1 = EntityHelper.id(entity1)
    println id1

    database.instance.acquire().withCloseable {db ->
      def entity2 = underTest.read(db, id1)
      println entity2

      assert entity2.name == entity1.name
      assert entity2.value != null
      assert entity2.value instanceof CronCalendar

      CronCalendar calendar2 = (CronCalendar)entity2.value
      println calendar2

      assert calendar2.cronExpression != null
      assert calendar2.cronExpression.cronExpression == calendar1.cronExpression.cronExpression
    }
  }

  @Test
  void 'read CronCalendar by name'() {
    def calendar1 = new CronCalendar('0 0 10 * * ?')
    def entity1 = new CalendarEntity(
        name: 'test-calendar',
        value: calendar1
    )

    database.instance.acquire().withCloseable {db ->
      underTest.addEntity(db, entity1)
    }

    database.instance.acquire().withCloseable {db ->
      def entity2 = underTest.readByName(db, entity1.name)

      assert entity2.name == entity1.name
      assert entity2.value != null
      assert entity2.value instanceof CronCalendar

      CronCalendar calendar2 = (CronCalendar)entity2.value
      assert calendar2.cronExpression != null
      assert calendar2.cronExpression.cronExpression == calendar1.cronExpression.cronExpression
    }
  }

  @Test
  void 'delete CronCalendar by name'() {
    def calendar1 = new CronCalendar('0 0 10 * * ?')
    def entity1 = new CalendarEntity(
        name: 'test-calendar',
        value: calendar1
    )

    database.instance.acquire().withCloseable {db ->
      underTest.addEntity(db, entity1)
    }

    database.instance.acquire().withCloseable {db ->
      boolean deleted = underTest.deleteByName(db, entity1.name)
      assert deleted
    }
  }

  @Test
  void 'list CronCalendar names'() {
    def entity1 = new CalendarEntity(
        name: 'test-calendar1',
        value: new CronCalendar('0 0 10 * * ?')
    )

    def entity2 = new CalendarEntity(
        name: 'test-calendar2',
        value: new CronCalendar('0 0 10 * * ?')
    )

    database.instance.acquire().withCloseable {db ->
      underTest.addEntity(db, entity1)
      underTest.addEntity(db, entity2)
    }

    database.instance.acquire().withCloseable {db ->
      List<String> names = underTest.browseNames(db);
      assert names.size() == 2
      assert names.contains('test-calendar1')
      assert names.contains('test-calendar2')
    }
  }

  // TODO: Verify other default calendar impls for marshalling sanity:
  //    DailyCalendar
  //    MonthlyCalendar
  //    AnnualCalendar
  //    WeeklyCalendar
  //    HolidayCalendar
}
