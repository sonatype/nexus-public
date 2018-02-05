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
import java.util.function.Function;

/**
 * Schedule that repeats on same days of a week repeatedly.
 *
 * @see ScheduleFactory#weekly(Date, Set)
 */
public class Weekly
    extends Schedule
{
  public static final String TYPE = "weekly";

  /**
   * Representation of weekday.
   */
  public enum Weekday
  {
    SUN, MON, TUE, WED, THU, FRI, SAT;

    /**
     * Function to convert {@link Weekday} to strings.
     */
    public static final Function<Weekday, String> dayToString = Enum::name;

    /**
     * Function to convert string to {@link Weekday}.
     */
    public static final Function<String, Weekday> stringToDay = Weekday::valueOf;
  }

  public Weekly(final Date startAt, final Set<Weekday> daysToRun) {
    super(TYPE);
    set(SCHEDULE_START_AT, dateToString(startAt));
    set(SCHEDULE_DAYS_TO_RUN, setToCsv(daysToRun, Weekday.dayToString));
  }

  public Date getStartAt() {
    return stringToDate(get(SCHEDULE_START_AT));
  }

  public Set<Weekday> getDaysToRun() {
    return csvToSet(get(SCHEDULE_DAYS_TO_RUN), Weekday.stringToDay);
  }
}