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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests for {@link HeavyBlobRef}.
 */
public class HeavyBlobRefTest
{
  private static final String STORE_NAME = "test-store";

  private static final String NODE_ID = "ab761d55-5d9c22b6-3f38315a-75b3db34-0922a4d5";

  private static final String BLOB_ID = "a8f3f56f-e895-4b6e-984a-1cf1f5107d36";

  private static final OffsetDateTime DATE_BASED_REF =
      OffsetDateTime.of(2024, 1, 1, 10, 30, 45, 0, ZoneOffset.UTC);

  private static final BlobMetrics METRICS = new BlobMetrics(null, "sha1", 1024L);

  @Test
  public void testCopiesAllFieldsFromBlobRef() {
    BlobRef blobRef = new BlobRef(NODE_ID, STORE_NAME, BLOB_ID, DATE_BASED_REF);

    HeavyBlobRef underTest = new HeavyBlobRef(blobRef, METRICS);

    assertThat(underTest.getNode(), is(NODE_ID));
    assertThat(underTest.getStore(), is(STORE_NAME));
    assertThat(underTest.getBlob(), is(BLOB_ID));
    assertThat(underTest.getDateBasedRef(), is(DATE_BASED_REF));
  }

  @Test
  public void testGetMetrics() {
    BlobRef blobRef = new BlobRef(STORE_NAME, BLOB_ID);

    HeavyBlobRef underTest = new HeavyBlobRef(blobRef, METRICS);

    assertThat(underTest.getMetrics(), is(sameInstance(METRICS)));
  }

  @Test
  public void testPreservesNullNodeAndDateBasedRef() {
    BlobRef blobRef = new BlobRef(STORE_NAME, BLOB_ID);

    HeavyBlobRef underTest = new HeavyBlobRef(blobRef, METRICS);

    assertThat(underTest.getNode(), is(nullValue()));
    assertThat(underTest.getDateBasedRef(), is(nullValue()));
    assertThat(underTest.getStore(), is(STORE_NAME));
    assertThat(underTest.getBlob(), is(BLOB_ID));
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullBlobRef() {
    new HeavyBlobRef(null, METRICS);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullMetrics() {
    new HeavyBlobRef(new BlobRef(STORE_NAME, BLOB_ID), null);
  }

  @Test
  public void testIsABlobRef() {
    HeavyBlobRef underTest = new HeavyBlobRef(new BlobRef(STORE_NAME, BLOB_ID), METRICS);

    assertThat(underTest, is(instanceOf(BlobRef.class)));
  }

  @Test
  public void testGetMetricsReturnsSameInstanceOnRepeatedCalls() {
    HeavyBlobRef underTest = new HeavyBlobRef(new BlobRef(STORE_NAME, BLOB_ID), METRICS);

    assertThat(underTest.getMetrics(), is(sameInstance(underTest.getMetrics())));
  }

  @Test
  public void testPreservesNullNodeWithNonNullDateBasedRef() {
    BlobRef blobRef = new BlobRef(null, STORE_NAME, BLOB_ID, DATE_BASED_REF);

    HeavyBlobRef underTest = new HeavyBlobRef(blobRef, METRICS);

    assertThat(underTest.getNode(), is(nullValue()));
    assertThat(underTest.getDateBasedRef(), is(DATE_BASED_REF));
    assertThat(underTest.getStore(), is(STORE_NAME));
    assertThat(underTest.getBlob(), is(BLOB_ID));
  }
}
