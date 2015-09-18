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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Schedule that repeats on same days of a week repeatedly.
 */
public class Weekly
    extends Schedule
{
  public static enum Weekday
  {
    SUN, MON, TUE, WED, THU, FRI, SAT;

    public static final Function<Weekday, String> toString = new Function<Weekday, String>()
    {
      @Override
      public String apply(final Weekday input) {
        return input.name();
      }
    };

    public static final Function<String, Weekday> toWeekday = new Function<String, Weekday>()
    {
      @Override
      public Weekday apply(final String input) {
        return Weekday.valueOf(input);
      }
    };
  }

  public Weekly(final Date startAt, final Set<Weekday> daysToRun) {
    super("weekly");
    checkNotNull(startAt);
    checkNotNull(daysToRun);
    checkArgument(!daysToRun.isEmpty(), "No days of week set to run");
    properties.put("schedule.startAt", dateToString(startAt));
    properties.put("schedule.daysToRun", setToCsv(daysToRun, Weekday.toString));
  }

  public Date getStartAt() {
    return stringToDate(properties.get("schedule.startAt"));
  }

  public Set<Weekday> getDaysToRun() {
    return csvToSet(properties.get("schedule.daysToRun"), Weekday.toWeekday);
  }
}