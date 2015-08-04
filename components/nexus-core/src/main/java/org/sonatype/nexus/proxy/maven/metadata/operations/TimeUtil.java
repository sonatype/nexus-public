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
package org.sonatype.nexus.proxy.maven.metadata.operations;

import java.text.ParseException;
import java.util.Date;

/**
 * @author Oleg Gusakov
 * @version $Id: TimeUtil.java 762963 2009-04-07 21:01:07Z ogusakov $
 */
public class TimeUtil
{
  public static final java.util.TimeZone TS_TZ = java.util.TimeZone.getTimeZone("UTC");

  public static final java.text.DateFormat TS_FORMAT = new java.text.SimpleDateFormat("yyyyMMddHHmmss");

  static {
    TS_FORMAT.setTimeZone(TS_TZ);
  }

  /**
   * @return current UTC timestamp by yyyyMMddHHmmss mask
   */
  public static String getUTCTimestamp() {
    return getUTCTimestamp(new Date());
  }

  /**
   * @return current date converted to UTC timestamp by yyyyMMddHHmmss mask
   */
  public static String getUTCTimestamp(Date date) {
    return TS_FORMAT.format(date);
  }

  /**
   * convert timestamp to millis
   *
   * @param ts timestamp to convert. Presumed to be a string of form yyyyMMddHHmmss
   * @return millis, corresponding to the supplied TS
   * @throws ParseException is long does not follow the format
   */
  public static long toMillis(String ts)
      throws ParseException
  {
    Date dts = toDate(ts);

    return dts.getTime();
  }

  static Date toDate(String ts)
      throws ParseException
  {
    Date dts = TS_FORMAT.parse(ts);
    return dts;
  }

  public static void main(String[] args)
      throws Exception
  {
    if (args == null || args.length < 0) {
      return;
    }

    if ("-t".equals(args[0])) {
      System.out.println(args[1] + " => " + new Date(toMillis(args[1])));
      return;
    }
  }

}
