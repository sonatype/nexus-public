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
package org.sonatype.nexus.blobstore;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to build date-based prefix based on the given date range. For more details, see
 * {@link DateBasedLocationStrategy}
 */
public class DateBasedHelper
{
  /**
   * Returns the date path prefix based on the given date range.
   *
   * @param fromDateTime the start date
   * @param toDateTime the end date
   * @return the date path prefix
   */
  public static String getDatePathPrefix(final OffsetDateTime fromDateTime, final OffsetDateTime toDateTime) {
    StringBuilder datePathPrefix = new StringBuilder();
    if (fromDateTime.getYear() == toDateTime.getYear()) {
      datePathPrefix.append("yyyy").append("/");
      if (fromDateTime.getMonth().getValue() == toDateTime.getMonth().getValue()) {
        datePathPrefix.append("MM").append("/");
        if (fromDateTime.getDayOfMonth() == toDateTime.getDayOfMonth()) {
          datePathPrefix.append("dd").append("/");
          if (fromDateTime.getHour() == toDateTime.getHour()) {
            datePathPrefix.append("HH").append("/");
            if (fromDateTime.getMinute() == toDateTime.getMinute()) {
              datePathPrefix.append("mm").append("/");
            }
          }
        }
      }
    }
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(datePathPrefix.toString());

    return toDateTime.format(dateTimeFormatter);
  }

  public static List<String> generatePrefixes(final OffsetDateTime from, final OffsetDateTime to) {
    List<String> prefixes = new ArrayList<>();
    OffsetDateTime startTime = from.truncatedTo(ChronoUnit.MINUTES).withOffsetSameInstant(ZoneOffset.UTC);
    OffsetDateTime endTime = to.truncatedTo(ChronoUnit.MINUTES).withOffsetSameInstant(ZoneOffset.UTC);

    Duration duration = Duration.between(startTime, endTime);
    if (duration.toDays() > 0) {
      while (isApplicable(startTime, endTime, ChronoUnit.DAYS)) {
        prefixes.add(startTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))); // Only days
        startTime = startTime.plusDays(1);
      }
    }
    else if (duration.toHours() > 0) {
      while (isApplicable(startTime, endTime, ChronoUnit.HOURS)) {
        prefixes.add(startTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH")));
        startTime = startTime.plusHours(1);
      }
    }
    else {
      while (isApplicable(startTime, endTime, ChronoUnit.MINUTES)) {
        // Check if we need to round up minutes to the next hour
        if (duration.toMinutes() > 30) {
          // Add the remaining minutes as an hour if > 30 minutes
          startTime = startTime.plusHours(1);
          prefixes.add(startTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH")));
          break;
        }
        else {
          prefixes.add(startTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm")));
          startTime = startTime.plusMinutes(1);
        }
      }
    }

    return prefixes;
  }

  private static boolean isApplicable(
      final OffsetDateTime startTime,
      final OffsetDateTime endTime,
      final ChronoUnit granularity)
  {

    if (startTime.truncatedTo(granularity).equals(endTime.truncatedTo(granularity))) {
      return true;
    }
    return startTime.isBefore(endTime) || startTime.equals(endTime);
  }
}
