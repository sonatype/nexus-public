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
package org.sonatype.nexus.common.guice;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.inject.Named;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeConverter;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Guice {@link TypeConverter} for {@link Duration} instances.
 *
 * @since 3.41
 */
@Named
public class DurationTypeConverter
    extends TypeConverterSupport<Duration>
{
  @Override
  protected Object doConvert(final String value, final TypeLiteral<?> toType) throws Exception {
    if (value != null) {
      return doParse(value.trim().toLowerCase());
    }
    return null;
  }

  private static class ParseConfig
  {
    final ChronoUnit unit;

    final String[] suffixes;

    private ParseConfig(final ChronoUnit unit, final String... suffixes) {
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
      new ParseConfig(MILLIS, "milliseconds", "millisecond", "millis", "ms"),
      new ParseConfig(NANOS, "nanoseconds", "nanosecond", "nanos", "ns"),
      new ParseConfig(MICROS, "microseconds", "microsecond", "micros", "us"),};

  private static Duration doParse(final String value) {
    for (ParseConfig config : PARSE_CONFIGS) {
      Duration t = extract(value, config.unit, config.suffixes);
      if (t != null) {
        return t;
      }
    }
    throw new RuntimeException("Unable to parse: " + value);
  }

  private static Duration extract(final String value, final ChronoUnit unit, final String... suffixes) {
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

          return Duration.of(n, unit);
        }
      }
    }

    // else can not extract
    return null;
  }
}
