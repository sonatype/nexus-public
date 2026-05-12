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
package org.sonatype.nexus.repository.content.store;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.HeavyBlobRef;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests for {@link AssetBlobData#blobRef()} conditional logic that returns
 * {@link HeavyBlobRef} when sha1 checksum and blobCreated are both present.
 */
public class AssetBlobDataTest
{
  private static final OffsetDateTime TIME = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

  private AssetBlobData underTest;

  @Before
  public void setUp() {
    underTest = new AssetBlobData();
  }

  @Test
  public void testBlobRefWithSha1ReturnsHeavyBlobRef() {
    BlobRef blobRef = new BlobRef("node", "store", "blob-id");
    Map<String, String> checksums = new HashMap<>();
    checksums.put("sha1", "abc123sha1hash");

    underTest.setBlobRef(blobRef);
    underTest.setChecksums(checksums);
    underTest.setBlobCreated(TIME);
    underTest.setBlobSize(512L);

    assertThat(underTest.blobRef(), instanceOf(HeavyBlobRef.class));
  }

  @Test
  public void testBlobRefWithoutSha1ReturnsRegularBlobRef() {
    BlobRef blobRef = new BlobRef("node", "store", "blob-id");
    Map<String, String> checksums = new HashMap<>();
    checksums.put("MD5", "abc123md5hash");

    underTest.setBlobRef(blobRef);
    underTest.setChecksums(checksums);
    underTest.setBlobCreated(TIME);

    assertThat(underTest.blobRef(), not(instanceOf(HeavyBlobRef.class)));
  }

  @Test
  public void testBlobRefWithNullChecksumsReturnsRegularBlobRef() {
    BlobRef blobRef = new BlobRef("node", "store", "blob-id");
    underTest.setBlobRef(blobRef);

    assertThat(underTest.blobRef(), not(instanceOf(HeavyBlobRef.class)));
    assertThat(underTest.blobRef(), is(blobRef));
  }

  @Test
  public void testBlobRefWithSha1ButNullBlobCreatedReturnsRegularBlobRef() {
    BlobRef blobRef = new BlobRef("node", "store", "blob-id");
    Map<String, String> checksums = new HashMap<>();
    checksums.put("sha1", "abc123sha1hash");

    underTest.setBlobRef(blobRef);
    underTest.setChecksums(checksums);

    assertThat(underTest.blobRef(), not(instanceOf(HeavyBlobRef.class)));
  }
}
