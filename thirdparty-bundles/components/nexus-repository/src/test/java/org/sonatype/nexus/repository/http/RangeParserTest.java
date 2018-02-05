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
package org.sonatype.nexus.repository.http;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.collect.Range;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests {@link RangeParser}.
 */
public class RangeParserTest
    extends TestSupport
{
  private RangeParser parser = new RangeParser();

  @Test
  public void first500Bytes() {
    final List<Range<Long>> ranges = parser.parseRangeSpec("bytes=0-499", 1000L);
    assertThat(ranges.size(), is(1));
    assertThat(ranges.get(0), is(Range.closed(0L, 499L)));
  }

  @Test
  public void second500Bytes() {
    final List<Range<Long>> ranges = parser.parseRangeSpec("bytes=500-999", 1000L);
    assertThat(ranges.size(), is(1));
    assertThat(ranges.get(0), is(Range.closed(500L, 999L)));
  }

  @Test
  public void last500Bytes() {
    final List<Range<Long>> ranges = parser.parseRangeSpec("bytes=9500-", 10000L);
    assertThat(ranges.size(), is(1));
    assertThat(ranges.get(0), is(Range.closed(9500L, 9999L)));
  }

  @Test
  public void last500BytesSuffix() {
    final List<Range<Long>> ranges = parser.parseRangeSpec("bytes=-1", 10L);
    assertThat(ranges.size(), is(1));
    assertThat(ranges.get(0), is(Range.closed(9L, 9L)));
  }

  @Test
  public void completeSuffix() {
    final Range<Long> fullContent = parser.parseRangeSpec("bytes=0-", 10L).get(0);
    final Range<Long> fullSuffix = parser.parseRangeSpec("bytes=-10", 10L).get(0);
    assertThat(fullSuffix, is(fullContent));
  }

  @Test
  public void illegalRange() {
    final List<Range<Long>> ranges = parser.parseRangeSpec("bytes=10-", 5L);
    System.err.println(ranges);
    assertThat(ranges, is(nullValue()));
  }

  @Test
  public void suffixTooLarge() {
    final List<Range<Long>> ranges = parser.parseRangeSpec("bytes=-10", 5L);
    System.err.println(ranges);
    assertThat(ranges, is(nullValue()));
  }

  // The RFC allows for ranges that exceed the end of the content to be fulfilled
  @Test
  public void partialOverlapsAreOkay() {
    final List<Range<Long>> ranges = parser.parseRangeSpec("bytes=5-100", 10L);
    assertThat(ranges.get(0), is(Range.closed(5L, 9L)));
  }
}