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
package org.sonatype.nexus.blobstore.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests for {@link BlobMetrics}.
 */
public class BlobMetricsTest
{
  private static final String SHA1_HASH = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

  private static final long CONTENT_SIZE = 1024L;

  private static final DateTime CREATION_TIME = new DateTime(2024, 1, 1, 10, 30, 45, DateTimeZone.UTC);

  private static final OffsetDateTime LAST_DOWNLOADED = OffsetDateTime.of(2024, 2, 2, 11, 31, 46, 0, ZoneOffset.UTC);

  private BlobMetrics underTest;

  @Before
  public void setUp() {
    underTest = new BlobMetrics(CREATION_TIME, SHA1_HASH, CONTENT_SIZE);
  }

  @Test
  public void testGetCreationTime() {
    assertThat(underTest.getCreationTime(), is(sameInstance(CREATION_TIME)));
  }

  @Test
  public void testGetSha1Hash() {
    assertThat(underTest.getSha1Hash(), is(SHA1_HASH));
  }

  @Test
  public void testGetContentSize() {
    assertThat(underTest.getContentSize(), is(CONTENT_SIZE));
  }

  @Test
  public void testLastDownloadedDefaultsToNull() {
    assertThat(underTest.getLastDownloaded(), is(nullValue()));
  }

  @Test
  public void testSetLastDownloaded() {
    underTest.setLastDownloaded(LAST_DOWNLOADED);

    assertThat(underTest.getLastDownloaded(), is(sameInstance(LAST_DOWNLOADED)));
  }

  @Test
  public void testSetLastDownloadedToNull() {
    underTest.setLastDownloaded(LAST_DOWNLOADED);
    underTest.setLastDownloaded(null);

    assertThat(underTest.getLastDownloaded(), is(nullValue()));
  }

  @Test
  public void testConstructorAcceptsNullValues() {
    BlobMetrics metrics = new BlobMetrics(null, null, 0L);

    assertThat(metrics.getCreationTime(), is(nullValue()));
    assertThat(metrics.getSha1Hash(), is(nullValue()));
    assertThat(metrics.getContentSize(), is(0L));
    assertThat(metrics.getLastDownloaded(), is(nullValue()));
  }

  @Test
  public void testToStringWithoutLastDownloaded() {
    String expected = "BlobMetrics{creationTime=" + CREATION_TIME +
        ", sha1Hash='" + SHA1_HASH + '\'' +
        ", contentSize=" + CONTENT_SIZE +
        ", lastDownloaded=null}";

    assertThat(underTest.toString(), is(expected));
  }

  @Test
  public void testToStringWithLastDownloaded() {
    underTest.setLastDownloaded(LAST_DOWNLOADED);

    String expected = "BlobMetrics{creationTime=" + CREATION_TIME +
        ", sha1Hash='" + SHA1_HASH + '\'' +
        ", contentSize=" + CONTENT_SIZE +
        ", lastDownloaded=" + LAST_DOWNLOADED + '}';

    assertThat(underTest.toString(), is(expected));
  }

  @Test
  public void testIsSerializable() throws Exception {
    underTest.setLastDownloaded(LAST_DOWNLOADED);

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(underTest);
    }

    BlobMetrics result;
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      result = (BlobMetrics) in.readObject();
    }

    assertThat(result.getCreationTime(), is(CREATION_TIME));
    assertThat(result.getSha1Hash(), is(SHA1_HASH));
    assertThat(result.getContentSize(), is(CONTENT_SIZE));
    assertThat(result.getLastDownloaded(), is(LAST_DOWNLOADED));
  }
}
