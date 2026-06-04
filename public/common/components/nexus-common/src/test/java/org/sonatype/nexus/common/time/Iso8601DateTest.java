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

import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link Iso8601Date}.
 */
public class Iso8601DateTest
{
  @Test
  public void testFormatParse() throws Exception {
    Date date1 = new Date();

    String formatted = Iso8601Date.format(date1);
    assertNotNull(formatted);

    Date date2 = Iso8601Date.parse(formatted);
    assertNotNull(date2);

    assertEquals(date1.getTime(), date2.getTime());
  }

  @Test
  public void testFormatParseExpected() throws Exception {
    String formatted1 = "2016-10-01T20:00:00.123Z";

    Date date = Iso8601Date.parse(formatted1);
    assertNotNull(date);

    TimeZone savedTimeZone = TimeZone.getDefault();
    TimeZone zuluTimeZone = TimeZone.getTimeZone("Etc/UTC");

    String formatted2 = null;

    try {
      TimeZone.setDefault(zuluTimeZone);
      formatted2 = Iso8601Date.format(date);
    }
    finally {
      TimeZone.setDefault(savedTimeZone);
    }

    assertNotNull(formatted2);

    assertEquals(formatted1, formatted2);
  }
}
