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
package org.sonatype.nexus.blobstore;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;

public class DateBasedHelperTest
{
  public static final OffsetDateTime NOW = OffsetDateTime.parse("2024-10-25T14:30:30Z");

  @Test
  public void testGeneratePrefixesMinutesLess30() {
    Duration duration = Duration.ofMinutes(1);
    List<String> prefixes = DateBasedHelper.generatePrefixes(NOW.minus(duration), NOW);
    assertThat(prefixes, containsInAnyOrder("2024/10/25/14/29", "2024/10/25/14/30"));

    duration = Duration.ofMinutes(5);
    prefixes = DateBasedHelper.generatePrefixes(NOW.minus(duration), NOW);
    assertThat(prefixes,
        containsInAnyOrder("2024/10/25/14/30", "2024/10/25/14/29", "2024/10/25/14/28", "2024/10/25/14/27",
            "2024/10/25/14/26", "2024/10/25/14/25"));
  }

  @Test
  public void testGeneratePrefixesMinutesOver30() {
    Duration duration = Duration.ofMinutes(31);
    List<String> prefixes = DateBasedHelper.generatePrefixes(NOW.minus(duration), NOW);
    assertThat(prefixes, containsInAnyOrder("2024/10/25/14"));
  }

  @Test
  public void testGeneratePrefixesHoursLess24() {
    Duration duration = Duration.ofHours(3);
    List<String> prefixes = DateBasedHelper.generatePrefixes(NOW.minus(duration), NOW);
    assertThat(prefixes, containsInAnyOrder("2024/10/25/14", "2024/10/25/13", "2024/10/25/11", "2024/10/25/12"));
  }

  @Test
  public void testGeneratePrefixesHoursOver24() {
    Duration duration = Duration.ofHours(25);
    List<String> prefixes = DateBasedHelper.generatePrefixes(NOW.minus(duration), NOW);
    assertThat(prefixes, containsInAnyOrder("2024/10/25", "2024/10/24"));
  }

  @Test
  public void testGeneratePrefixesHoursOne() {
    Duration duration = Duration.ofHours(1);
    // sometimes current hour were not generated as prefix, so define specific value
    OffsetDateTime currentTime = OffsetDateTime.parse("2024-10-25T14:32:30Z");
    List<String> prefixes = DateBasedHelper.generatePrefixes(currentTime.minus(duration), currentTime);
    assertThat(prefixes, containsInAnyOrder("2024/10/25/14", "2024/10/25/13"));
  }

  @Test
  public void testGeneratePrefixesMinutesOne() {
    Duration duration = Duration.ofMinutes(1);
    // sometimes current hour were not generated as prefix, so define specific value
    OffsetDateTime currentTime = OffsetDateTime.parse("2024-10-25T14:32:30Z");
    List<String> prefixes = DateBasedHelper.generatePrefixes(currentTime.minus(duration), currentTime);
    assertThat(prefixes, containsInAnyOrder("2024/10/25/14/32", "2024/10/25/14/31"));
  }

  @Test
  public void testGeneratePrefixesDaysOne() {
    Duration duration = Duration.ofDays(1);
    // sometimes current hour were not generated as prefix, so define specific value
    OffsetDateTime currentTime = OffsetDateTime.parse("2024-10-25T14:32:30Z");
    List<String> prefixes = DateBasedHelper.generatePrefixes(currentTime.minus(duration), currentTime);
    assertThat(prefixes, containsInAnyOrder("2024/10/25", "2024/10/24"));
  }
}
