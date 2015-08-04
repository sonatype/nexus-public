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

import org.sonatype.scheduling.iterators.AbstractCalendarBasedSchedulerIterator;
import org.sonatype.scheduling.iterators.SchedulerIterator;
import org.sonatype.scheduling.schedules.DailySchedule;

public class FewSecondSchedule
    extends DailySchedule
{

  private final int interval;

  public FewSecondSchedule() {
    this(new Date(System.currentTimeMillis() + 500), null, 5);
  }

  public FewSecondSchedule(Date startDate, Date endDate, int interval) {
    super(startDate, endDate);
    this.interval = interval;
  }

  @Override
  protected SchedulerIterator createIterator() {
    return new FewSecondSchedulerIterator();
  }

  class FewSecondSchedulerIterator
      extends AbstractCalendarBasedSchedulerIterator
  {

    public FewSecondSchedulerIterator() {
      super(getStartDate(), getEndDate());
    }

    @Override
    public void stepNext() {
      getCalendar().add(Calendar.SECOND, interval);
    }
  }

}
