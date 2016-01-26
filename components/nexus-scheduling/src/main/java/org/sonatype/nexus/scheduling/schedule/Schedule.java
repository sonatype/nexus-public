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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Schedule.
 */
public abstract class Schedule
{
  public static final String SCHEDULE_TYPE = "schedule.type";

  public static final String SCHEDULE_START_AT = "schedule.startAt";

  public static final String SCHEDULE_DAYS_TO_RUN = "schedule.daysToRun";

  private final Map<String, String> properties = new HashMap<>();

  protected Schedule(final String type) {
    checkNotNull(type);
    set(SCHEDULE_TYPE, type);
  }

  public String getType() {
    return properties.get(SCHEDULE_TYPE);
  }

  /**
   * Get a schedule property.
   */
  protected String get(final String name) {
    return properties.get(name);
  }

  /**
   * Set a schedule property.
   */
  protected void set(final String name, final String value) {
    properties.put(name, value);
  }

  public Map<String, String> asMap() {
    return Collections.unmodifiableMap(properties);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "properties=" + properties +
        '}';
  }

  //
  // Helpers
  //

  /**
   * Convert a date into a string.
   */
  public static String dateToString(final Date date) {
    return new DateTime(date).toString();
  }

  /**
   * Convert a string into a date.
   */
  public static Date stringToDate(final String string) {
    return DateTime.parse(string).toDate();
  }

  /**
   * Convert a set into a CSV formatted string using given mapping function.
   *
   * Ordering of set elements in produced CSV string is enforced to their natural ordering.
   */
  public static <T extends Comparable<T>> String setToCsv(final Set<T> set, final Function<T, String> function) {
    return new TreeSet<>(set).stream()
        .map(function)
        .collect(Collectors.joining(","));
  }

  /**
   * Convert a CSV formatted string into a set using given mapping function.
   *
   * Ordering of returned set element does not reflect CSV ordering, elements are (re)sorted by their natural ordering.
   */
  public static <T extends Comparable<T>> Set<T> csvToSet(final String csv, final Function<String, T> function) {
    return Splitter.on(',').splitToList(csv).stream()
        .map(function)
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
