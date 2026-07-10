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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link ExternalMetadata}.
 */
public class ExternalMetadataTest
{
  private static final String ETAG = "\"d41d8cd98f00b204e9800998ecf8427e\"";

  private static final OffsetDateTime LAST_MODIFIED =
      OffsetDateTime.of(2024, 1, 1, 10, 30, 45, 0, ZoneOffset.UTC);

  @Test
  public void testAccessors() {
    ExternalMetadata underTest = new ExternalMetadata(ETAG, LAST_MODIFIED);

    assertThat(underTest.etag(), is(ETAG));
    assertThat(underTest.lastModified(), is(LAST_MODIFIED));
  }

  @Test
  public void testAcceptsNullValues() {
    ExternalMetadata underTest = new ExternalMetadata(null, null);

    assertThat(underTest.etag(), is(nullValue()));
    assertThat(underTest.lastModified(), is(nullValue()));
  }

  @Test
  public void testEqualsAndHashCode() {
    ExternalMetadata first = new ExternalMetadata(ETAG, LAST_MODIFIED);
    ExternalMetadata second = new ExternalMetadata(ETAG, LAST_MODIFIED);

    assertThat(first, is(equalTo(second)));
    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void testNotEqualWhenEtagDiffers() {
    ExternalMetadata first = new ExternalMetadata(ETAG, LAST_MODIFIED);
    ExternalMetadata other = new ExternalMetadata("other-etag", LAST_MODIFIED);

    assertThat(first, is(not(equalTo(other))));
  }

  @Test
  public void testNotEqualWhenLastModifiedDiffers() {
    ExternalMetadata first = new ExternalMetadata(ETAG, LAST_MODIFIED);
    ExternalMetadata other = new ExternalMetadata(ETAG, LAST_MODIFIED.plusSeconds(1));

    assertThat(first, is(not(equalTo(other))));
  }

  @Test
  public void testNotEqualToNullOrOtherType() {
    ExternalMetadata underTest = new ExternalMetadata(ETAG, LAST_MODIFIED);

    assertThat(underTest.equals(null), is(false));
    assertThat(underTest.equals("not-metadata"), is(false));
  }

  @Test
  public void testToString() {
    ExternalMetadata underTest = new ExternalMetadata(ETAG, LAST_MODIFIED);

    assertThat(underTest.toString(),
        is("ExternalMetadata[etag=" + ETAG + ", lastModified=" + LAST_MODIFIED + "]"));
  }
}
