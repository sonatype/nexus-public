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
package org.sonatype.nexus.repository.date;

import java.time.LocalDateTime;

import static org.sonatype.nexus.scheduling.schedule.Weekly.Weekday.SAT;
import static org.sonatype.nexus.scheduling.schedule.Weekly.Weekday.SUN;

/**
 * Utils for the nexus repository allowing perform specific timezone operations.
 *
 * @since 3.19
 */
public class TimeZoneUtils
{
  private TimeZoneUtils() {
    // empty
  }

  /**
   * The aim of this method to handle days of the week in case client and server are in different dates.
   * e.g. server date is (20/07/2019) and client date is (21/07/2019) and specific day of the week is specified.
   *
   * @param weekDayNumber it is ordinal number from enum Weekly.Weekday. First is SUN(0) e.g. SUN(0), MON(1), ...
   * @param clientDate    Represent the client Date
   * @param serverDate    Represent the client Date but in server TimeZone.
   * @return Returns transformed day of the week (one day back/forward/without changes) according client and server timezone.
   */
  public static int shiftWeekDay(final int weekDayNumber,
                                 final LocalDateTime clientDate,
                                 final LocalDateTime serverDate)
  {
    final int compare = clientDate.toLocalDate().compareTo(serverDate.toLocalDate());
    if (compare > 0) {
      return weekDayNumber - 1 < SUN.ordinal() ? SAT.ordinal() : weekDayNumber - 1;
    }
    else if (compare < 0) {
      return weekDayNumber + 1 > SAT.ordinal() ? SUN.ordinal() : weekDayNumber + 1;
    }
    return weekDayNumber;
  }

  /**
   * @param monthDay   Represent ordinal number of the month day. 1, 2, 3 ... 31.
   * @param clientDate Represent the client Date
   * @param serverDate Represent the client Date but in server TimeZone.
   * @return Returns transformed day of the month (one day back/forward/without changes) according client and server timezone.
   */
  public static int shiftMonthDay(final int monthDay,
                                  final LocalDateTime clientDate,
                                  final LocalDateTime serverDate)
  {
    final int lengthOfMonth = serverDate.toLocalDate().lengthOfMonth();
    final int compare = clientDate.toLocalDate().compareTo(serverDate.toLocalDate());
    if (compare > 0) {
      return monthDay - 1 < 1 ? lengthOfMonth : monthDay - 1;
    }
    else if (compare < 0) {
      return monthDay + 1 > lengthOfMonth ? 1 : monthDay + 1;
    }
    return monthDay;
  }
}
