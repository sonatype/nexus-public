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
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Schedule support class.
 */
public abstract class Schedule
{
  protected final Map<String, String> properties;

  public Schedule(final String type) {
    checkNotNull(type);
    this.properties = Maps.newHashMap();
    this.properties.put("schedule.type", type);
  }

  public String getType() {
    return properties.get("schedule.type");
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

  // ==

  /**
   * Helper method to set {@link Date} types, handles conversion to string automatically.
   */
  public static String dateToString(final Date date) {
    return new DateTime(date).toString();
  }

  /**
   * Helper method to get {@link Date} types, handles conversion from string automatically.
   */
  public static Date stringToDate(final String string) {
    return DateTime.parse(string).toDate();
  }

  /**
   * Helper method to set {@link Set} types, handles conversion to CSV with provided function. Ordering of set
   * elements in produced CSV string is enforced to their natural ordering.
   */
  public static <T extends Comparable<T>> String setToCsv(final Set<T> set, final Function<T, String> func) {
    return Joiner.on(',').join(Collections2.transform(Sets.newTreeSet(set), func));
  }

  /**
   * Helper method to get {@link Set} types, handles conversion from CSV with provided function. Ordering of
   * returned set element does not reflect CSV ordering, elements are (re)sorted by their natural ordering.
   */
  public static <T extends Comparable<T>> Set<T> csvToSet(String value, final Function<String, T> func) {
    return Sets.newTreeSet(Collections2.transform(Splitter.on(',').splitToList(value), func));
  }
}
