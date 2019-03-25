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

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import static java.util.Locale.US;
import static org.joda.time.DateTimeZone.forID;
import static org.joda.time.format.DateTimeFormat.forPattern;

/**
 * Utils for the nexus repository allowing simplified access to Date and Time methods for easy reuse.
 *
 * @since 3.next
 */
public class DateTimeUtils
{
  /**
   * Date format pattern used to parse HTTP date headers in RFC 1123 format.
   */
  private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

  public static final DateTimeFormatter FORMATTER_RFC1123 = forPattern(PATTERN_RFC1123)
      .withZone(forID("GMT"))
      .withLocale(US);

  private DateTimeUtils() {
  }

  /**
   * Formats the given {@link DateTime} according to the RFC 1123 pattern.
   *
   * @param dateTime The date to format.
   * @return An RFC 1123 formatted date string.
   * @see #PATTERN_RFC1123
   */
  public static String formatDateTime(final DateTime dateTime) {
    return FORMATTER_RFC1123.print(dateTime);
  }
}
