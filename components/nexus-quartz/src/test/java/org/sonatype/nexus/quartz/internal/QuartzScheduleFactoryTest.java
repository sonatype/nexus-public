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
package org.sonatype.nexus.quartz.internal;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class QuartzScheduleFactoryTest
{
  private QuartzScheduleFactory underTest;

  @Before
  public void setUp() {
    underTest = new QuartzScheduleFactory();
  }

  /**
   * Parses cron expression: "seconds minutes hours day-of-month month day-of-week year"
   */
  private void cron(String pattern) {
    System.out.println("Parsing: " + pattern);
    underTest.cron(new Date(), pattern);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadOneMonthInsteadOfDay() {
    cron("0 0 0 ? * JAN");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadOneDotInDays() {
    cron("0 0 0 ? * FRI.,SAT");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadOneQuestionmarkAmongDays() {
    cron("1 2 4 ?,1,2,3,10,11,12 * ?");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadOneLAtBadPosition() {
    cron("0 0 L L * ?");
  }
}
