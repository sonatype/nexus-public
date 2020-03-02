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

import java.util.regex.Pattern;

import org.sonatype.goodies.testsupport.TestSupport;

import org.joda.time.DateTime;
import org.junit.Test;

import static java.util.regex.Pattern.compile;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.repository.date.DateTimeUtils.formatDateTime;

public class DateTimeUtilsTest
    extends TestSupport
{
  private Pattern rfc1123 = compile("[a-zA-Z]{3}, \\d{2} [a-zA-Z]{3} \\d{4} \\d{2}:\\d{2}:\\d{2} [a-zA-Z]{3}");

  @Test
  public void format_DateTime_Uses_RFC1123_Pattern() {
    assertTrue(rfc1123.matcher(formatDateTime(new DateTime())).matches());
  }

  @Test
  public void format_DateTime_Uses_GMT_TimeZone() {
    assertThat(formatDateTime(new DateTime()), endsWith("GMT"));
  }
}
