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

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Representation of a specific unit of time.
 *
 * Supports:
 *
 * <ul>
 * <li>NANOSECONDS</li>
 * <li>MICROSECONDS</li>
 * <li>MILLISECONDS</li>
 * <li>MINUTES</li>
 * <li>HOURS</li>
 * <li>DAYS</li>
 * </ul>
 *
 */
/**
 * Representation of a specific unit of time.
 *
 * @deprecated Use {@link java.time.Duration} instead.
 */
@Deprecated
public final class Time
{
  private final long value;

  // TODO: May want to duplicate TimeUnit so we can add support for > DAYS ?

  private final TimeUnit unit;

  public Time(final long value, final TimeUnit unit) {
    this.value = value;
    this.unit = checkNotNull(unit);
  }

  @Deprecated
  public long getValue() {
    return value;
  }

  /**
   */
  public long value() {
    return value;
  }

  @Deprecated
  public TimeUnit getUnit() {
    return unit;
  }

  /**
   */
  public TimeUnit unit() {
    return unit;
  }

  // TODO: May want to use getNanos() here so that Groovy DSL use ends up like Time.seconds(1).nanos instead of
  // Time.seconds(1).toNanos() ?

  public long toNanos() {
    return unit.toNanos(value);
  }

  /**
   */
  public int toNanosI() {
    return (int) toNanos();
  }

  /**
   */
  public Time asNanos() {
    return nanos(toNanos());
  }

  public long toMicros() {
    return unit.toMicros(value);
  }

  /**
   */
  public int toMicrosI() {
    return (int) toMicros();
  }

  /**
   */
  public Time asMicros() {
    return micros(toMicros());
  }

  public long toMillis() {
    return unit.toMillis(value);
  }

  /**
   */
  public int toMillisI() {
    return (int) toMillis();
  }

  /**
   */
  public Time asMillis() {
    return millis(toMillis());
  }

  public long toSeconds() {
    return unit.toSeconds(value);
  }

  /**
   */
  public int toSecondsI() {
    return (int) toSeconds();
  }

  /**
   */
  public Time asSeconds() {
    return seconds(toSeconds());
  }

  public long toMinutes() {
    return unit.toMinutes(value);
  }

  /**
   */
  public int toMinutesI() {
    return (int) toMinutes();
  }

  /**
   */
  public Time asMinutes() {
    return minutes(toMinutes());
  }

  public long toHours() {
    return unit.toHours(value);
  }

  /**
   */
  public int toHoursI() {
    return (int) toHours();
  }

  /**
   */
  public Time asHours() {
    return hours(toHours());
  }

  public long toDays() {
    return unit.toDays(value);
  }

  /**
   */
  public int toDaysI() {
    return (int) toDays();
  }

  /**
   */
  public Time asDays() {
    return days(toDays());
  }

  public void sleep() throws InterruptedException {
    unit.sleep(value);
  }

  public void wait(final Object obj) throws InterruptedException {
    checkNotNull(obj);
    unit.timedWait(obj, value);
  }

  public void join(final Thread thread) throws InterruptedException {
    checkNotNull(thread);
    unit.timedJoin(thread, value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Time that = (Time) obj;
    return value == that.value && unit == that.unit;
  }

  @Override
  public int hashCode() {
    int result = (int) (value ^ (value >>> 32));
    result = 31 * result + unit.hashCode();
    return result;
  }

  private String unitName() {
    // TODO: i18n support?
    String name = unit.name().toLowerCase();
    if (value == 1) {
      name = name.substring(0, name.length() - 1);
    }
    return name;
  }

  @Override
  public String toString() {
    return String.format("%d %s", value, unitName());
  }

  public static Time time(final long value, final TimeUnit unit) {
    return new Time(value, unit);
  }

  public static Time nanos(final long value) {
    return new Time(value, NANOSECONDS);
  }

  public static Time micros(final long value) {
    return new Time(value, MICROSECONDS);
  }

  public static Time millis(final long value) {
    return new Time(value, MILLISECONDS);
  }

  public static Time seconds(final long value) {
    return new Time(value, SECONDS);
  }

  public static Time minutes(final long value) {
    return new Time(value, MINUTES);
  }

  public static Time hours(final long value) {
    return new Time(value, HOURS);
  }

  public static Time days(final long value) {
    return new Time(value, DAYS);
  }

  //
  // Parsing
  //

  /**
   */
  public static Time parse(final String value) {
    if (value != null) {
      return doParse(value.trim().toLowerCase());
    }
    return null;
  }

  private static class ParseConfig
  {
    final TimeUnit unit;

    final String[] suffixes;

    private ParseConfig(final TimeUnit unit, final String... suffixes) {
      this.unit = unit;
      this.suffixes = suffixes;
    }
  }

  private static final ParseConfig[] PARSE_CONFIGS = {
      new ParseConfig(SECONDS, "seconds", "second", "sec", "s"),
      new ParseConfig(MINUTES, "minutes", "minute", "min", "m"),
      new ParseConfig(HOURS, "hours", "hour", "hr", "h"),
      new ParseConfig(DAYS, "days", "day", "d"),

      // These probably used less, so parse last
      new ParseConfig(MILLISECONDS, "milliseconds", "millisecond", "millis", "ms"),
      new ParseConfig(NANOSECONDS, "nanoseconds", "nanosecond", "nanos", "ns"),
      new ParseConfig(MICROSECONDS, "microseconds", "microsecond", "micros", "us"),
  };

  private static Time doParse(final String value) {
    for (ParseConfig config : PARSE_CONFIGS) {
      Time t = extract(value, config.unit, config.suffixes);
      if (t != null) {
        return t;
      }
    }
    throw new RuntimeException("Unable to parse: " + value);
  }

  private static Time extract(final String value, final TimeUnit unit, final String... suffixes) {
    String number = null, units = null;

    for (int p = 0; p < value.length(); p++) {
      // skip until we find a non-digit
      if (Character.isDigit(value.charAt(p))) {
        continue;
      }
      // split number and units suffix string
      number = value.substring(0, p);
      units = value.substring(p, value.length()).trim();
      break;
    }

    // if decoded units, check if its one of the supported suffixes
    if (units != null) {
      for (String suffix : suffixes) {
        if (suffix.equals(units)) {
          long n = Long.parseLong(number.trim());
          return new Time(n, unit);
        }
      }
    }

    // else can not extract
    return null;
  }
}
