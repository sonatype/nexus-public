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
package org.sonatype.scheduling;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import org.sonatype.scheduling.iterators.WeeklySchedulerIterator;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link WeeklySchedulerIterator}.
 */
public class WeeklyIteratorTest
    extends TestSupport
{

  @Test
  public void testWeeklyIterator()
      throws Exception
  {
    Calendar nearFuture = Calendar.getInstance();
    nearFuture.add(Calendar.MINUTE, 15);

    HashSet<Integer> days = new HashSet<Integer>();

    days.add(1);
    days.add(2);
    days.add(3);
    days.add(4);
    days.add(5);
    days.add(6);
    days.add(7);

    WeeklySchedulerIterator iter = new WeeklySchedulerIterator(nearFuture.getTime(), null, days);

    Date nextDate = iter.next();

    assertTrue(nearFuture.getTime().equals(nextDate));

    // Just validate the next 20 days in a row
    for (int i = 0; i < 20; i++) {
      nextDate = iter.next();

      nearFuture.add(Calendar.DAY_OF_YEAR, 1);

      assertTrue(nearFuture.getTime().equals(nextDate));
    }
  }
}
