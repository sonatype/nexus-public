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
package org.sonatype.nexus.blobstore.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Random;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.file.internal.FileBlobMetadataStoreImpl;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER;

/**
 * {@link FileBlobStore} integration tests.
 */
public class FileBlobStoreIT
    extends TestSupport
{
  public static final int TEST_DATA_LENGTH = 10_000;

  public static final ImmutableMap<String, String> TEST_HEADERS = ImmutableMap.of(
      CREATED_BY_HEADER, "test",
      BLOB_NAME_HEADER, "test/randomData.bin"
  );

  private FileBlobMetadataStore metadataStore;

  private FileBlobStore underTest;

  @Before
  public void setUp() throws Exception {
    Path root = util.createTempDir().toPath();
    Path content = root.resolve("content");
    Path metadata = root.resolve("metadata");

    this.metadataStore = FileBlobMetadataStoreImpl.create(metadata.toFile());
    this.underTest = new FileBlobStore(content, new VolumeChapterLocationStrategy(), new SimpleFileOperations(),
        metadataStore, new BlobStoreConfiguration());
    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    underTest.stop();
  }

  @Test
  public void basicSmokeTest() throws Exception {
    final byte[] content = new byte[TEST_DATA_LENGTH];
    new Random().nextBytes(content);

    final Blob blob = underTest.create(new ByteArrayInputStream(content), TEST_HEADERS);

    final byte[] output = extractContent(blob);
    assertThat("data must survive", content, is(equalTo(output)));

    final BlobMetrics metrics = blob.getMetrics();
    assertThat("size must be calculated correctly", metrics.getContentSize(), is(equalTo((long) TEST_DATA_LENGTH)));

    final BlobStoreMetrics storeMetrics = underTest.getMetrics();
    assertThat(storeMetrics.getBlobCount(), is(equalTo(1L)));

    // FIXME: This is no longer valid
    //assertThat(storeMetrics.getTotalSize(), is(equalTo((long) TEST_DATA_LENGTH)));

    assertThat(storeMetrics.getAvailableSpace(), is(greaterThan(0L)));

    final boolean deleted = underTest.delete(blob.getId());
    assertThat(deleted, is(equalTo(true)));

    final Blob deletedBlob = underTest.get(blob.getId());
    assertThat(deletedBlob, is(nullValue()));

    // Now that we've deleted the blob, there shouldn't be anything left
    final BlobStoreMetrics storeMetrics2 = underTest.getMetrics();

    // FIXME: This is no longer valid
    //assertThat(storeMetrics2.getBlobCount(), is(equalTo(0L)));
    //assertThat(storeMetrics2.getTotalSize(), is(equalTo((long) TEST_DATA_LENGTH)));

    underTest.compact();

    final BlobStoreMetrics storeMetrics3 = underTest.getMetrics();

    // FIXME: This is no longer valid
    //assertThat("compacting should reclaim deleted blobs' space", storeMetrics3.getTotalSize(), is(equalTo(0L)));
  }

  private byte[] extractContent(final Blob blob) throws IOException {
    try (InputStream inputStream = blob.getInputStream()) {
      return ByteStreams.toByteArray(inputStream);
    }
  }

  @Test
  public void hardDeletePreventsGetDespiteOpenStreams() throws Exception {
    final byte[] content = new byte[TEST_DATA_LENGTH];
    new Random().nextBytes(content);

    final Blob blob = underTest.create(new ByteArrayInputStream(content), TEST_HEADERS);

    final InputStream inputStream = blob.getInputStream();

    // Read half the data
    inputStream.read(new byte[content.length / 2]);

    // force delete
    underTest.deleteHard(blob.getId());

    final Blob newBlob = underTest.get(blob.getId());
    assertThat(newBlob, is(nullValue()));
  }
}
