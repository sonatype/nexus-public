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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper for working with <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO 8601<a/> dates.
 *
 */
public class Iso8601Date
{
  public static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"; // NON-NLS
  // public static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

  /**
   */
  private Iso8601Date() {
  }

  private static DateFormat getFormat() {
    return new SimpleDateFormat(PATTERN);
  }

  public static Date parse(final String value) throws ParseException {
    checkNotNull(value);
    return getFormat().parse(value);
  }

  public static String format(final Date date) {
    checkNotNull(date);
    return getFormat().format(date);
  }
}
