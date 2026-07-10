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
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests for {@link BlobId}.
 */
public class BlobIdTest
{
  private static final String BLOB_ID = "a8f3f56f-e895-4b6e-984a-1cf1f5107d36";

  private static final String BLOBSTORE_PATH = "content/vol-01/chap-02";

  private static final OffsetDateTime DATE_CREATED = OffsetDateTime.of(2024, 1, 1, 10, 30, 45, 0, ZoneOffset.UTC);

  @Test
  public void testDeprecatedSingleArgConstructor() {
    BlobId blobId = new BlobId(BLOB_ID);

    assertThat(blobId.asUniqueString(), is(equalTo(BLOB_ID)));
    assertThat(blobId.getBlobCreatedRef(), is(nullValue()));
    assertThat(blobId.getBlobstorePath(), is(nullValue()));
  }

  @Test
  public void testTwoArgConstructor() {
    BlobId blobId = new BlobId(BLOB_ID, DATE_CREATED);

    assertThat(blobId.asUniqueString(), is(equalTo(BLOB_ID)));
    assertThat(blobId.getBlobCreatedRef(), is(equalTo(DATE_CREATED)));
    assertThat(blobId.getBlobstorePath(), is(nullValue()));
  }

  @Test
  public void testTwoArgConstructorWithNullDate() {
    BlobId blobId = new BlobId(BLOB_ID, null);

    assertThat(blobId.asUniqueString(), is(equalTo(BLOB_ID)));
    assertThat(blobId.getBlobCreatedRef(), is(nullValue()));
    assertThat(blobId.getBlobstorePath(), is(nullValue()));
  }

  @Test
  public void testThreeArgConstructor() {
    BlobId blobId = new BlobId(BLOB_ID, DATE_CREATED, BLOBSTORE_PATH);

    assertThat(blobId.asUniqueString(), is(equalTo(BLOB_ID)));
    assertThat(blobId.getBlobCreatedRef(), is(equalTo(DATE_CREATED)));
    assertThat(blobId.getBlobstorePath(), is(equalTo(BLOBSTORE_PATH)));
  }

  @Test
  public void testThreeArgConstructorWithNullableArgs() {
    BlobId blobId = new BlobId(BLOB_ID, null, null);

    assertThat(blobId.asUniqueString(), is(equalTo(BLOB_ID)));
    assertThat(blobId.getBlobCreatedRef(), is(nullValue()));
    assertThat(blobId.getBlobstorePath(), is(nullValue()));
  }

  @Test
  public void testThreeArgConstructorWithPathButNoDate() {
    BlobId blobId = new BlobId(BLOB_ID, null, BLOBSTORE_PATH);

    assertThat(blobId.asUniqueString(), is(equalTo(BLOB_ID)));
    assertThat(blobId.getBlobCreatedRef(), is(nullValue()));
    assertThat(blobId.getBlobstorePath(), is(equalTo(BLOBSTORE_PATH)));
  }

  @Test(expected = NullPointerException.class)
  public void testDeprecatedConstructorRejectsNullId() {
    new BlobId(null);
  }

  @Test(expected = NullPointerException.class)
  public void testTwoArgConstructorRejectsNullId() {
    new BlobId(null, DATE_CREATED);
  }

  @Test(expected = NullPointerException.class)
  public void testThreeArgConstructorRejectsNullId() {
    new BlobId(null, DATE_CREATED, BLOBSTORE_PATH);
  }

  @Test
  public void testToStringReturnsId() {
    BlobId blobId = new BlobId(BLOB_ID, DATE_CREATED, BLOBSTORE_PATH);

    assertThat(blobId.toString(), is(equalTo(BLOB_ID)));
  }

  @Test
  public void testEqualsSameInstance() {
    BlobId blobId = new BlobId(BLOB_ID);

    assertThat(blobId, is(sameInstance(blobId)));
    assertThat(blobId.equals(blobId), is(true));
  }

  @Test
  public void testEqualsEquivalentInstance() {
    BlobId blobId = new BlobId(BLOB_ID, DATE_CREATED, BLOBSTORE_PATH);
    BlobId other = new BlobId(BLOB_ID, null, null);

    // equality is based solely on the id, regardless of date or path
    assertThat(blobId, is(not(sameInstance(other))));
    assertThat(blobId, is(equalTo(other)));
    assertThat(other, is(equalTo(blobId)));
  }

  @Test
  public void testEqualsDifferentId() {
    BlobId blobId = new BlobId(BLOB_ID);
    BlobId other = new BlobId("different-id");

    assertThat(blobId, is(not(equalTo(other))));
  }

  @Test
  public void testEqualsNull() {
    BlobId blobId = new BlobId(BLOB_ID);

    assertThat(blobId.equals(null), is(false));
  }

  @Test
  public void testEqualsDifferentType() {
    BlobId blobId = new BlobId(BLOB_ID);

    assertThat(blobId.equals(BLOB_ID), is(false));
  }

  @Test
  public void testHashCodeMatchesIdHashCode() {
    BlobId blobId = new BlobId(BLOB_ID, DATE_CREATED, BLOBSTORE_PATH);

    assertThat(blobId.hashCode(), is(equalTo(BLOB_ID.hashCode())));
  }

  @Test
  public void testHashCodeConsistentForEqualInstances() {
    BlobId blobId = new BlobId(BLOB_ID, DATE_CREATED);
    BlobId other = new BlobId(BLOB_ID);

    assertThat(blobId.hashCode(), is(equalTo(other.hashCode())));
  }

  @Test
  public void testCompareToEqual() {
    BlobId blobId = new BlobId(BLOB_ID);
    BlobId other = new BlobId(BLOB_ID);

    assertThat(blobId.compareTo(other), is(0));
  }

  @Test
  public void testCompareToLessThan() {
    BlobId blobId = new BlobId("aaa");
    BlobId other = new BlobId("bbb");

    assertThat(blobId.compareTo(other), is(lessThan(0)));
  }

  @Test
  public void testCompareToGreaterThan() {
    BlobId blobId = new BlobId("bbb");
    BlobId other = new BlobId("aaa");

    assertThat(blobId.compareTo(other), is(greaterThan(0)));
  }

  @Test
  public void testCompareToIgnoresDateAndPath() {
    BlobId blobId = new BlobId(BLOB_ID, DATE_CREATED, BLOBSTORE_PATH);
    BlobId other = new BlobId(BLOB_ID, null, null);

    assertThat(blobId.compareTo(other), is(0));
    assertThat(other.compareTo(blobId), is(0));
  }
}
