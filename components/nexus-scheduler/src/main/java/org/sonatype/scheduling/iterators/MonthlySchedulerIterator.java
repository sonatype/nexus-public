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

public class MonthlySchedulerIterator
    extends AbstractCalendarBasedSchedulerIterator
{
  public static final Integer LAST_DAY_OF_MONTH = new Integer(999);

  private final Set<Integer> monthdaysToRun;

  public MonthlySchedulerIterator(Date startingDate, Date endingDate, Set<Integer> monthdaysToRun) {
    super(startingDate, endingDate);

    this.monthdaysToRun = monthdaysToRun;
  }

  public void stepNext() {
    if (monthdaysToRun == null || monthdaysToRun.isEmpty()) {
      getCalendar().add(Calendar.MONTH, 1);
    }
    else {
      getCalendar().add(Calendar.DAY_OF_MONTH, 1);

      // step over the days not in when to run
      while (!monthdaysToRun.contains(getCalendar().get(Calendar.DAY_OF_MONTH))) {
        // first check to see if we are on the last day of the month
        if (monthdaysToRun.contains(LAST_DAY_OF_MONTH)) {
          Calendar cal = (Calendar) getCalendar().clone();

          cal.add(Calendar.DAY_OF_MONTH, 1);

          if (cal.get(Calendar.DAY_OF_MONTH) == 1) {
            break;
          }
        }

        getCalendar().add(Calendar.DAY_OF_MONTH, 1);
      }
    }
  }
}
