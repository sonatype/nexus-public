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
package org.sonatype.nexus.common.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Helper for {@link DateTime} and {@link Date} types.
 *
 * @since 3.0
 */
public class DateHelper
{
  private DateHelper() {
    // empty
  }

  /**
   * Copy given date.
   */
  @Nullable
  public static Date copy(@Nullable final Date date) {
    if (date != null) {
      return new Date(date.getTime());
    }
    return null;
  }

  /**
   * Converts input {@link Date} to {@link DateTime}, or {@code null} if input is {@code null}.
   */
  @Nullable
  public static DateTime toDateTime(@Nullable final Date date) {
    if (date == null) {
      return null;
    }
    return new DateTime(date.getTime());
  }

  /**
   * Converts input {@link DateTime} to {@link Date}, or {@code null} if input is {@code null}.
   */
  @Nullable
  public static Date toDate(@Nullable final DateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.toDate();
  }

  /**
   * Converts input {@link Long} to {@link Date}, or {@code null} if input is {@code null}.
   */
  @Nullable
  public static Date toDate(@Nullable final Long timestamp) {
    if (timestamp == null || timestamp <= 0) {
      return null;
    }
    Instant instant = Instant.ofEpochMilli(timestamp);
    if (instant.equals(Instant.EPOCH)) {
      return null;
    }
    return new Date(timestamp);
  }

  /**
   * Converts input {@link OffsetDateTime} to {@link Date}, or {@code null} if input is {@code null}.
   */
  @Nullable
  public static Date toDate(@Nullable final OffsetDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return new Date(dateTime.toInstant().toEpochMilli());
  }

  /**
   * @since 3.25
   */
  public static DateTime toDateTime(final OffsetDateTime offsetDateTime) {
    return new DateTime(offsetDateTime.toInstant().toEpochMilli(), DateTimeZone.UTC);
  }

  /**
   * @since 3.25
   */
  public static OffsetDateTime toOffsetDateTime(final DateTime dateTime) {
    return Instant.ofEpochMilli(dateTime.getMillis()).atOffset(ZoneOffset.UTC);
  }

  public static OffsetDateTime toOffsetDateTime(final Date date) {
    return Instant.ofEpochMilli(date.getTime()).atOffset(ZoneOffset.UTC);
  }

  /**
   * @since 3.24
   */
  public static Duration toJavaDuration(final org.joda.time.Duration jodaDuration) {
    return Duration.ofMillis(jodaDuration.getMillis());
  }

  /**
   * @since 3.24
   */
  public static org.joda.time.Duration toJodaDuration(final Duration javaDuration) {
    return org.joda.time.Duration.millis(javaDuration.toMillis());
  }

  /**
   * Converts the Date to a LocalDate (a date, without time). The returned LocalDate will represent the Date of the
   * provided value without any timezone offset.
   *
   * @since 3.38
   */
  public static LocalDate toLocalDate(@Nullable final Date date) {
    if (date == null) {
      return null;
    }

    org.joda.time.LocalDate jodaDate = new org.joda.time.LocalDate(date);
    return LocalDate.of(jodaDate.getYear(), jodaDate.getMonthOfYear(), jodaDate.getDayOfMonth());
  }

  /**
   * Converts the Date to a LocalDate (a date, without time). The returned LocalDate will represent the Date of the
   * provided value in UTC.
   *
   * @since 3.38
   */
  public static Date atStartOfDay(@Nullable final LocalDate date) {
    if (date == null) {
      return null;
    }
    return Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant());
  }

  /**
   * Converts an Optional<OffsetDateTime> to a Date.
   */
  public static Date optionalOffsetToDate(final Optional<OffsetDateTime> offsetDateTime) {
    return offsetDateTime.map(OffsetDateTime::toInstant)
        .map(Date::from)
        .orElse(null);
  }

  /**
   * Converts an OffsetDateTime to a Date.
   */
  public static Date offsetToDate(final OffsetDateTime offsetDateTime) {
    return Date.from(offsetDateTime.toInstant());
  }

  /**
   * Receives a list of dates and returns the oldest one. If the list is null or empty, returns null.
   */
  public static Date oldestDateFromLongs(final List<Long> dates) {
    if (dates == null || dates.isEmpty()) {
      return null;
    }
    List<Date> dateList = dates.stream().map(DateHelper::toDate).collect(Collectors.toList());
    return oldestDate(dateList);
  }

  /**
   * Receives a list of dates and returns the oldest one. If the list is null or empty, returns null.
   */
  public static Date oldestDate(final List<Date> dates) {
    if (dates == null || dates.isEmpty()) {
      return null;
    }
    return dates.stream().filter(date -> date != null).min(Date::compareTo).orElse(null);
  }

  /**
   * Receives two dates and returns the number of days elapsed between them.
   */
  public static int daysElapsed(final Date date1, final Date date2) {
    if (date1 == null || date2 == null) {
      return 0;
    }
    return (int) ((date2.getTime() - date1.getTime()) / DateUtils.MILLIS_PER_DAY);
  }

  /**
   * Formats an Instant to a String in ISO-8601 format.
   */
  public static String isoFormatter(Instant instant) {
    if (instant == null) {
      return null;
    }
    LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }
}
