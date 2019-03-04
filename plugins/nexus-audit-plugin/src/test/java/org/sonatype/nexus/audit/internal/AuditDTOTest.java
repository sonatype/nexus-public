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
package org.sonatype.nexus.audit.internal;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.nexus.audit.AuditData;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.joda.time.DateTime.parse;
import static org.joda.time.format.DateTimeFormat.forPattern;
import static org.junit.Assert.assertTrue;

public class AuditDTOTest
{
  private static final long TEST_TIMESTAMP = 1549375691779L;

  private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("(\\{\"timestamp\":\")([^\"]*)(\".*)");

  private AuditDTO underTest;

  @Test
  public void testToString_noData() {
    underTest = new AuditDTO();
    assertThat(underTest.toString(), is("{}"));
  }

  @Test
  public void testToString_withData() {
    underTest = new AuditDTO(makeAuditData());

    Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(underTest.toString());
    assertTrue(timestampMatcher.matches());
    String timestamp = timestampMatcher.group(2);

    assertThat(parse(timestamp, forPattern("yyyy-MM-dd HH:mm:ss,SSSZ")).getMillis(), is(TEST_TIMESTAMP));

    assertThat(timestampMatcher.replaceAll("$1<TIMESTAMP>$3"), is(
        "{\"timestamp\":\"<TIMESTAMP>\",\"nodeId\":\"testnodeid\",\"initiator\":\"testinitiator\",\"domain\":\"testdomain\",\"type\":\"testtype\",\"context\":\"testcontext\",\"attributes\":{\"testattribute1\":\"testvalue1\",\"testattribute2\":\"testvalue2\"}}"));
  }

  private static AuditData makeAuditData() {
    AuditData data = new AuditData();
    data.setDomain("testdomain");
    data.setType("testtype");
    data.setContext("testcontext");
    data.setInitiator("testinitiator");
    data.setNodeId("testnodeid");
    data.setTimestamp(new Date(TEST_TIMESTAMP));
    data.getAttributes().put("testattribute1", "testvalue1");
    data.getAttributes().put("testattribute2", "testvalue2");
    return data;
  }
}
