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
package org.sonatype.nexus.testsuite.content.range;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.sonatype.nexus.client.core.exception.NexusClientResponseException;
import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.testsuite.content.ContentITSupport;

import com.google.common.base.Strings;
import com.google.common.collect.Range;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ContentRangeIT
    extends ContentITSupport
{

  public ContentRangeIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  private static final Charset CHARSET = Charset.forName("UTF-8");

  private static final String TEST_DATA = Strings.repeat("0123456789", 10);

  private Location location;

  @Before
  public void prepare() throws IOException {
    location = new Location("releases", "/some/content/" + testMethodName() + ".txt");
    final File target = File.createTempFile(testMethodName(), "tmp");
    Files.write(TEST_DATA, target, CHARSET);
    content().upload(location, target);
  }

  @Test
  public void validRangesBeginning() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // get 1st word
    content().downloadRange(location, bos, Range.closed(0L, 9L));
    final String data = new String(bos.toByteArray(), CHARSET);
    assertThat(data, equalTo("0123456789"));
  }

  @Test
  public void validRangesBeginningShiftedNine() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // get 1st word
    content().downloadRange(location, bos, Range.closed(1L, 9L));
    final String data = new String(bos.toByteArray(), CHARSET);
    assertThat(data, equalTo("123456789"));
  }

  @Test
  public void validRangesBeginningShiftedTen() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // get 1st word
    content().downloadRange(location, bos, Range.closed(1L, 10L));
    final String data = new String(bos.toByteArray(), CHARSET);
    assertThat(data, equalTo("1234567890"));
  }

  @Test
  public void validRangesMiddle() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // get middle
    content().downloadRange(location, bos, Range.closed(45L, 54L));
    final String data = new String(bos.toByteArray(), CHARSET);
    assertThat(data, equalTo("5678901234"));
  }

  @Test
  public void validRangesMiddleOneByte() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // get middle
    content().downloadRange(location, bos, Range.closed(48L, 48L));
    final String data = new String(bos.toByteArray(), CHARSET);
    assertThat(data, equalTo("8"));
  }

  @Test
  public void validRangesEnd() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // get last word
    content().downloadRange(location, bos, Range.closed(90L, 99L));
    final String data = new String(bos.toByteArray(), CHARSET);
    assertThat(data, equalTo("0123456789"));
  }

  @Test
  public void validRangesSameAsActualContent() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    content().downloadRange(location, bos, Range.closed(0L, 99L));
    final String data = new String(bos.toByteArray(), CHARSET);
    assertThat(data, equalTo(TEST_DATA));
  }

  @Test
  public void invalidRangesNegativeIsIgnoredAndFullFileReturned() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    content().downloadRange(location, bos, Range.closed(-1L, 99L));
    final String data = new String(bos.toByteArray(), CHARSET);
    assertThat(data, equalTo(TEST_DATA));
  }

  @Test(expected=NexusClientResponseException.class)
  public void invalidRangesTooBig() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // results in 416 - Requested Range Not Satisfiable
    content().downloadRange(location, bos, Range.closed(0L, 1000000L));
  }

  @Test(expected=NexusClientResponseException.class)
  public void invalidRangesBeginsAfterFileEnd() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // results in 416 - Requested Range Not Satisfiable
    content().downloadRange(location, bos, Range.closed(1000L, 1000000L));
  }
}
