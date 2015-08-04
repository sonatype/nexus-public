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
package org.sonatype.scheduling.iterators;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

public class WeeklySchedulerIterator
    extends AbstractCalendarBasedSchedulerIterator
{
  private final Set<Integer> weekdaysToRun;

  public WeeklySchedulerIterator(Date startingDate, Date endingDate, Set<Integer> weekdaysToRun) {
    super(startingDate, endingDate);

    this.weekdaysToRun = weekdaysToRun;
  }

  public void stepNext() {
    if (weekdaysToRun == null || weekdaysToRun.isEmpty()) {
      getCalendar().add(Calendar.WEEK_OF_YEAR, 1);
    }
    else {
      getCalendar().add(Calendar.DAY_OF_WEEK, 1);

      while (!weekdaysToRun.contains(getCalendar().get(Calendar.DAY_OF_WEEK))) {
        getCalendar().add(Calendar.DAY_OF_WEEK, 1);
      }
    }
  }
}
