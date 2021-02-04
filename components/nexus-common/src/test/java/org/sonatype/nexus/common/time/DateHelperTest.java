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
import java.time.OffsetDateTime;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.common.time.DateHelper.toDateTime;
import static org.sonatype.nexus.common.time.DateHelper.toJavaDuration;
import static org.sonatype.nexus.common.time.DateHelper.toJodaDuration;
import static org.sonatype.nexus.common.time.DateHelper.toOffsetDateTime;

public class DateHelperTest
{
  @Test
  public void toDateTimeTest() {
    OffsetDateTime offsetDateTime = OffsetDateTime.parse("2010-06-30T01:20+00:00");
    DateTime jodaDateTime = new DateTime("2010-06-30T01:20+00:00");
    assertThat(toDateTime(offsetDateTime).toInstant(), equalTo(jodaDateTime.toInstant()));
  }

  @Test
  public void toOffsetDateTimeTest() {
    OffsetDateTime offsetDateTime = OffsetDateTime.parse("2010-06-30T01:20+00:00");
    DateTime jodaDateTime = new DateTime("2010-06-30T01:20+00:00");
    assertThat(toOffsetDateTime(jodaDateTime).toInstant(), equalTo(offsetDateTime.toInstant()));
  }

  @Test
  public void toJavaDurationTest() {
    Duration javaDuration = Duration.ofHours(5);
    org.joda.time.Duration jodaDuration = org.joda.time.Duration.standardHours(5);
    assertThat(toJavaDuration(jodaDuration), equalTo(javaDuration));
  }

  @Test
  public void toJodaDurationTest() {
    Duration javaDuration = Duration.ofHours(5);
    org.joda.time.Duration jodaDuration = org.joda.time.Duration.standardHours(5);
    assertThat(toJodaDuration(javaDuration), equalTo(jodaDuration));
  }
}
