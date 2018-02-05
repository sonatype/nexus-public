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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Schedule that repeats on same days of a month repeatedly.
 *
 * @see ScheduleFactory#monthly(Date, Set)
 */
public class Monthly
    extends Schedule
{
  public static final String TYPE = "monthly";

  /**
   * Representation of a calender day.
   */
  public static final class CalendarDay
      implements Comparable<CalendarDay>
  {
    /**
     * A special "sentinel" value for a CalendarDay that marks the "last day in the month".
     *
     * Normal days would not be suited for that, nor 30, nor 31, nor 28 nor 29 would work.
     */
    private static final int LAST_DAY_OF_MONTH = 999;

    private static final CalendarDay LAST_DAY = new CalendarDay(LAST_DAY_OF_MONTH);

    public static CalendarDay day(final int day) {
      return new CalendarDay(day);
    }

    public static Set<CalendarDay> days(final int... days) {
      Set<CalendarDay> result = new HashSet<>();
      for (int day : days) {
        result.add(day(day));
      }
      return result;
    }

    public static CalendarDay lastDay() {
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

    /**
     * Function to convert {@link CalendarDay} to string.
     */
    public static final Function<CalendarDay, String> dayToString = input -> Integer.toString(input.getDay());

    /**
     * Function to convert string to {@link CalendarDay}.
     */
    public static final Function<String, CalendarDay> stringToDay = input -> new CalendarDay(Integer.parseInt(input));
  }

  public Monthly(final Date startAt, final Set<CalendarDay> daysToRun) {
    super(TYPE);
    set(SCHEDULE_START_AT, dateToString(startAt));
    set(SCHEDULE_DAYS_TO_RUN, setToCsv(daysToRun, CalendarDay.dayToString));
  }

  public Date getStartAt() {
    return stringToDate(get(SCHEDULE_START_AT));
  }

  public Set<CalendarDay> getDaysToRun() {
    return csvToSet(get(SCHEDULE_DAYS_TO_RUN), CalendarDay.stringToDay);
  }
}
