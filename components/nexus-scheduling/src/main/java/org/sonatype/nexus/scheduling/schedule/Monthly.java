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
package org.sonatype.nexus.scheduling.schedule;

import java.util.Date;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Schedule that repeats on same days of a month repeatedly.
 */
public class Monthly
    extends Schedule
{
  public static final class CalendarDay
      implements Comparable<CalendarDay>
  {
    /**
     * A special "sentinel" value for a CalendarDay that marks the "last day in the month", as the normal days
     * would not be suited for that, nor 30, nor 31, nor 28 nor 29 would work.
     */
    private static final int LAST_DAY_OF_MONTH = 999;

    private static final CalendarDay LAST_DAY = new CalendarDay(LAST_DAY_OF_MONTH);

    public static final CalendarDay day(final int day) {
      return new CalendarDay(day);
    }

    public static final Set<CalendarDay> days(final int... days) {
      final Set<CalendarDay> result = Sets.newHashSet();
      for (int day : days) {
        result.add(day(day));
      }
      return result;
    }

    public static final CalendarDay lastDay() {
      return LAST_DAY;
    }

    private final int day;

    private CalendarDay(final int day) {
      checkArgument((day >= 1 && day <= 31) || day == LAST_DAY_OF_MONTH, "Invalid calendar day %s", day);
      this.day = day;
    }

    public int getDay() {
      return day;
    }

    public boolean isLastDayOfMonth() {
      return day == LAST_DAY_OF_MONTH;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "day=" + (day == LAST_DAY_OF_MONTH ? "last" : day) +
          '}';
    }

    @Override
    public int compareTo(final CalendarDay other) {
      return this.day - other.day;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CalendarDay that = (CalendarDay) o;
      if (day != that.day) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      return day;
    }

    // ==

    public static final Function<CalendarDay, String> toString = new Function<CalendarDay, String>()
    {
      @Override
      public String apply(final CalendarDay input) {
        return Integer.toString(input.getDay());
      }
    };

    public static final Function<String, CalendarDay> toCalendarDay = new Function<String, CalendarDay>()
    {
      @Override
      public CalendarDay apply(final String input) {
        return new CalendarDay(Integer.parseInt(input));
      }
    };
  }

  public Monthly(final Date startAt, final Set<CalendarDay> daysToRun) {
    super("monthly");
    checkNotNull(startAt);
    checkNotNull(daysToRun);
    checkArgument(!daysToRun.isEmpty(), "No days of month set to run");
    properties.put("schedule.startAt", dateToString(startAt));
    properties.put("schedule.daysToRun", setToCsv(daysToRun, CalendarDay.toString));
  }

  public Date getStartAt() {
    return stringToDate(properties.get("schedule.startAt"));
  }

  public Set<CalendarDay> getDaysToRun() {
    return csvToSet(properties.get("schedule.daysToRun"), CalendarDay.toCalendarDay);
  }
}
