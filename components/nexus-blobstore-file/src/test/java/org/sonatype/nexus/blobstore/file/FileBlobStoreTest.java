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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.file.FileOperations.StreamMetrics;
import org.sonatype.nexus.common.collect.AutoClosableIterable;

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FileBlobStore}.
 */
public class FileBlobStoreTest
    extends TestSupport
{
  private LocationStrategy locationStrategy;

  private FileOperations fileOps;

  private FileBlobMetadataStore metadataStore;

  private Path root;

  private FileBlobStore underTest;

  @Before
  public void setUp() throws Exception {
    locationStrategy = mock(LocationStrategy.class);
    fileOps = mock(FileOperations.class);
    metadataStore = mock(FileBlobMetadataStore.class);

    root = util.createTempDir().toPath();
    underTest = new FileBlobStore(root, locationStrategy, fileOps, metadataStore, new BlobStoreConfiguration());
    underTest.start();
  }

  @After
  public void shutdown() throws Exception {
    underTest.stop();
  }

  @Test(expected = IllegalArgumentException.class)
  public void createRequiresHeaders() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[100]);
    final HashMap<String, String> headers = new HashMap<>();
    underTest.create(inputStream, headers);
  }

  @Test
  public void successfulCreation() throws Exception {
    final BlobId fakeId = new BlobId("testId");
    final long contentSize = 200L;
    final String fakeSHA1 = "3757y5abc234cfgg";
    final InputStream inputStream = mock(InputStream.class);
    final ImmutableMap<String, String> headers = ImmutableMap.of(
        BlobStore.BLOB_NAME_HEADER, "my blob",
        BlobStore.CREATED_BY_HEADER, "John did this"
    );

    when(metadataStore.add(any(FileBlobMetadata.class))).thenReturn(fakeId);
    when(locationStrategy.location(fakeId)).thenReturn("fakePath");
    final Path fakePath = root.resolve("fakePath" + FileBlobStore.BLOB_CONTENT_SUFFIX);
    when(fileOps.create(fakePath, inputStream)).thenReturn(new StreamMetrics(contentSize, fakeSHA1));

    final Blob blob = underTest.create(inputStream, headers);

    final BlobMetrics metrics = blob.getMetrics();

    assertThat(metrics.getSHA1Hash(), is(equalTo(fakeSHA1)));
    assertThat(metrics.getContentSize(), is(equalTo(contentSize)));

    assertTrue("Creation time should be very recent",
        metrics.getCreationTime().isAfter(new DateTime().minusSeconds(2)));
  }

  @Test
  public void getExistingBlob() throws Exception {
    final BlobId fakeId = new BlobId("fakeId");
    final FileBlobMetadata metadata = mock(FileBlobMetadata.class);
    when(metadataStore.get(fakeId)).thenReturn(metadata);
    when(metadata.isAlive()).thenReturn(true);
    when(metadata.getMetrics()).thenReturn(mock(BlobMetrics.class));

    when(locationStrategy.location(fakeId)).thenReturn("fakePath");
    final Path fakePath = root.resolve("fakePath" + FileBlobStore.BLOB_CONTENT_SUFFIX);
    when(fileOps.exists(fakePath)).thenReturn(true);

    when(fileOps.openInputStream(fakePath)).thenReturn(mock(InputStream.class));

    final Blob blob = underTest.get(fakeId);
    assertThat(blob, notNullValue());
    assertThat(blob.getId(), is(equalTo(fakeId)));
  }

  @Test
  public void deletingMarksAsDeleted() {
    final BlobId fakeId = new BlobId("fakeId");
    final FileBlobMetadata metadata = mock(FileBlobMetadata.class);
    when(metadataStore.get(fakeId)).thenReturn(metadata);

    // The blob isn't already deleted
    when(metadata.isAlive()).thenReturn(true);

    final boolean deleted = underTest.delete(fakeId);
    assertThat(deleted, is(equalTo(true)));

    verify(metadata).setBlobState(FileBlobState.MARKED_FOR_DELETION);
  }

  @Test
  public void secondDeletionRedundant() {
    final BlobId fakeId = new BlobId("testId");
    final FileBlobMetadata metadata = mock(FileBlobMetadata.class);
    when(metadataStore.get(fakeId)).thenReturn(metadata);
    when(metadata.isAlive()).thenReturn(false);

    final boolean deleted = underTest.delete(fakeId);
    assertThat(deleted, is(equalTo(false)));
  }

  @Test
  public void iteratorIsOnlyForAliveBlobs() {
    final AutoClosableIterable<BlobId> iterator = underTest.iterator();
    verify(metadataStore).findWithState(FileBlobState.ALIVE);
  }
}
