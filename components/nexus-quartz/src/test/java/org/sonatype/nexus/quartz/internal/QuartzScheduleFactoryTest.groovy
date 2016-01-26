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
package org.sonatype.nexus.quartz.internal

import org.sonatype.goodies.testsupport.TestSupport

import org.junit.Before
import org.junit.Test

/**
 * Tests for {@link QuartzScheduleFactory}.
 */
public class QuartzScheduleFactoryTest
    extends TestSupport
{
  private QuartzScheduleFactory underTest

  @Before
  public void setUp() throws Exception {
    underTest = new QuartzScheduleFactory()
  }

  /**
   * Parses cron expression: "seconds minutes hours day-of-month month day-of-week year"
   */
  private void cron(String pattern) {
    log "Parsing: $pattern"
    underTest.cron(new Date(), pattern)
  }

  @Test
  void 'Fire at 12pm (noon) every day'() {
    cron '0 0 12 * * ?'
  }

  @Test
  void 'Fire at 10:15am every day'() {
    cron '0 15 10 ? * *'
    cron '0 15 10 * * ?'
    cron '0 15 10 * * ? *'
  }

  @Test
  void 'Fire at 10:15am every day during the year 2005'() {
    cron '0 15 10 * * ? 2005'
  }

  @Test
  void 'Fire every minute starting at 2pm and ending at 2:59pm, every day'() {
    cron '0 * 14 * * ?'
  }

  @Test
  void 'Fire every 5 minutes starting at 2pm and ending at 2:55pm, every day'() {
    cron '0 0/5 14 * * ?'
  }

  @Test
  void 'Fire every 5 minutes starting at 2pm and ending at 2:55pm, AND fire every 5 minutes starting at 6pm and ending at 6:55pm, every day'() {
    cron '0 0/5 14,18 * * ?'
  }

  @Test
  void 'Fire every minute starting at 2pm and ending at 2:05pm, every day'() {
    cron '0 0-5 14 * * ?'
  }

  @Test
  void 'Fire at 2:10pm and at 2:44pm every Wednesday in the month of March'() {
    cron '0 10,44 14 ? 3 WED'
  }

  @Test
  void 'Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday'() {
    cron '0 15 10 ? * MON-FRI'
  }

  @Test
  void 'Fire at 10:15am on the 15th day of every month'() {
    cron '0 15 10 15 * ?'
  }

  @Test
  void 'Fire at 10:15am on the last day of every month'() {
    cron '0 15 10 L * ?'
  }

  @Test
  void 'Fire at 10:15am on the last Friday of every month'() {
    cron '0 15 10 ? * 6L'
  }

  @Test
  void 'Fire at 10:15am on every last friday of every month during the years 2002, 2003, 2004 and 2005'() {
    cron '0 15 10 ? * 6L 2002-2005'
  }

  @Test
  void 'Fire at 10:15am on the third Friday of every month'() {
    cron '0 15 10 ? * 6#3'
  }

  @Test
  void 'fire every 5 minutes'() {
    cron '0 0/5 * * * ?'
  }

  @Test
  void goodOnes() {
    cron '0 0 0 ? * SAT'
    cron '0 0 0 ? * FRI,SAT'
    cron '1 2 4 1,2,3,10,11,12 * ?'
  }

  @Test(expected = IllegalArgumentException.class)
  void badOneMonthInsteadOfDay() {
    cron '0 0 0 ? * JAN'
  }

  @Test(expected = IllegalArgumentException.class)
  void badOneDotInDays() {
    cron '0 0 0 ? * FRI.,SAT'
  }

  @Test(expected = IllegalArgumentException.class)
  void badOneQuestionmarkAmongDays() {
    cron '1 2 4 ?,1,2,3,10,11,12 * ?'
  }

  @Test(expected = IllegalArgumentException.class)
  void badOneLAtBadPosition() {
    cron '0 0 L L * ?'
  }
}
