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
package org.sonatype.nexus.blobstore.api.softdeleted;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link BlobLocationUpdate}.
 */
public class BlobLocationUpdateTest
{
  private static final String BLOB_ID = "a8f3f56f-e895-4b6e-984a-1cf1f5107d36";

  private static final String STORE_NAME = "target-store";

  private static final OffsetDateTime DATE_PATH_REF =
      OffsetDateTime.of(2024, 1, 1, 10, 30, 45, 0, ZoneOffset.UTC);

  @Test
  public void testAccessors() {
    BlobLocationUpdate underTest = new BlobLocationUpdate(BLOB_ID, STORE_NAME, DATE_PATH_REF);

    assertThat(underTest.blobId(), is(BLOB_ID));
    assertThat(underTest.newSourceBlobStoreName(), is(STORE_NAME));
    assertThat(underTest.newDatePathRef(), is(DATE_PATH_REF));
  }

  @Test
  public void testAcceptsNullValues() {
    BlobLocationUpdate underTest = new BlobLocationUpdate(null, null, null);

    assertThat(underTest.blobId(), is(nullValue()));
    assertThat(underTest.newSourceBlobStoreName(), is(nullValue()));
    assertThat(underTest.newDatePathRef(), is(nullValue()));
  }

  @Test
  public void testEqualsAndHashCode() {
    BlobLocationUpdate first = new BlobLocationUpdate(BLOB_ID, STORE_NAME, DATE_PATH_REF);
    BlobLocationUpdate second = new BlobLocationUpdate(BLOB_ID, STORE_NAME, DATE_PATH_REF);

    assertThat(first, is(equalTo(second)));
    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void testNotEqualWhenBlobIdDiffers() {
    BlobLocationUpdate first = new BlobLocationUpdate(BLOB_ID, STORE_NAME, DATE_PATH_REF);
    BlobLocationUpdate other = new BlobLocationUpdate("different-id", STORE_NAME, DATE_PATH_REF);

    assertThat(first, is(not(equalTo(other))));
  }

  @Test
  public void testNotEqualWhenStoreNameDiffers() {
    BlobLocationUpdate first = new BlobLocationUpdate(BLOB_ID, STORE_NAME, DATE_PATH_REF);
    BlobLocationUpdate other = new BlobLocationUpdate(BLOB_ID, "different-store", DATE_PATH_REF);

    assertThat(first, is(not(equalTo(other))));
  }

  @Test
  public void testNotEqualWhenDatePathRefDiffers() {
    BlobLocationUpdate first = new BlobLocationUpdate(BLOB_ID, STORE_NAME, DATE_PATH_REF);
    BlobLocationUpdate other = new BlobLocationUpdate(BLOB_ID, STORE_NAME, DATE_PATH_REF.plusSeconds(1));

    assertThat(first, is(not(equalTo(other))));
  }

  @Test
  public void testNotEqualToNullOrOtherType() {
    BlobLocationUpdate underTest = new BlobLocationUpdate(BLOB_ID, STORE_NAME, DATE_PATH_REF);

    assertThat(underTest.equals(null), is(false));
    assertThat(underTest.equals("not-an-update"), is(false));
  }

  @Test
  public void testToString() {
    BlobLocationUpdate underTest = new BlobLocationUpdate(BLOB_ID, STORE_NAME, DATE_PATH_REF);

    assertThat(underTest.toString(),
        is("BlobLocationUpdate[blobId=" + BLOB_ID + ", newSourceBlobStoreName=" + STORE_NAME +
            ", newDatePathRef=" + DATE_PATH_REF + "]"));
  }
}
