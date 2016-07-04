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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.file.internal.BlobStoreMetricsStore;
import org.sonatype.nexus.blobstore.file.internal.BlobStoreMetricsStoreImpl;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.io.DirectoryHelper;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER;

/**
 * {@link FileBlobStore} integration tests.
 */
public class FileBlobStoreIT
    extends TestSupport
{
  public static final int TEST_DATA_LENGTH = 10;

  public static final ImmutableMap<String, String> TEST_HEADERS = ImmutableMap.of(
      CREATED_BY_HEADER, "test",
      BLOB_NAME_HEADER, "test/randomData.bin"
  );

  private FileBlobStore underTest;

  private Path blobStoreDirectory;

  private PeriodicJobServiceImpl jobService;

  private BlobStoreMetricsStore metricsStore;

  @Before
  public void setUp() throws Exception {
    ApplicationDirectories applicationDirectories = mock(ApplicationDirectories.class);
    blobStoreDirectory = util.createTempDir().toPath();
    when(applicationDirectories.getWorkDirectory(anyString())).thenReturn(blobStoreDirectory.toFile());

    jobService = new PeriodicJobServiceImpl();
    jobService.start();

    metricsStore = new BlobStoreMetricsStoreImpl(jobService);

    final BlobStoreConfiguration config = new BlobStoreConfiguration();
    config.attributes(FileBlobStore.CONFIG_KEY).set(FileBlobStore.PATH_KEY, blobStoreDirectory.toString());
    underTest = new FileBlobStore(new VolumeChapterLocationStrategy(),
        new SimpleFileOperations(),
        applicationDirectories,
        metricsStore);
    underTest.init(config);
    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    if (underTest != null) {
      underTest.stop();
    }
    if (jobService != null) {
      jobService.stop();
    }
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

  @Test
  public void hardLinkIngestion() throws Exception {

    final byte[] content = testData();
    final HashCode sha1 = Hashing.sha1().hashBytes(content);
    final Path sourceFile = testFile(content);

    // Attempt to make a hard link to the file we're importing
    final Blob blob = underTest.create(sourceFile, TEST_HEADERS, content.length, sha1);

    // Now append some telltale bytes to the end of the original file
    final byte[] appendMe = new byte[100];
    Arrays.fill(appendMe, (byte) 3);
    Files.write(sourceFile, appendMe, StandardOpenOption.APPEND);

    // Now see if the blob was modified. This isn't a real use case, but checks the hard link.
    assertThat("blob must have been modified", extractContent(blob), is(equalTo(extractContent(sourceFile))));
  }

  private byte[] testData() {
    final byte[] content = new byte[TEST_DATA_LENGTH];
    new Random().nextBytes(content);
    return content;
  }

  private Path testFile(byte[] content) throws IOException {
    Path tempFile = util.createTempFile().toPath();
    DirectoryHelper.mkdir(tempFile.getParent());
    Files.write(tempFile, content);
    return tempFile;
  }

  private byte[] extractContent(final Blob blob) throws IOException {
    try (InputStream inputStream = blob.getInputStream()) {
      return ByteStreams.toByteArray(inputStream);
    }
  }

  private byte[] extractContent(final Path path) throws IOException {
    try (InputStream input = Files.newInputStream(path)) {
      return ByteStreams.toByteArray(input);
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

  @Test
  public void blobstoreRemovalPreservesExternalFiles() throws Exception {
    final byte[] content = new byte[TEST_DATA_LENGTH];
    new Random().nextBytes(content);

    for (int i = 0; i < 100; i++) {
      underTest.create(new ByteArrayInputStream(content), TEST_HEADERS);
    }

    assertThat(Files.exists(this.blobStoreDirectory), is(true));

    underTest.stop();
    underTest.remove();

    assertThat(Files.exists(this.blobStoreDirectory), is(true));

    underTest = null; // The store is stopped, no cleanup required
  }


  @Test
  public void blobstoreRemovalDeletesInternalFiles() throws Exception {

    assertThat(Files.exists(this.blobStoreDirectory), is(true));

    underTest.stop();
    underTest.remove();

    assertThat(Files.exists(this.blobStoreDirectory), is(false));

    underTest = null; // The store is stopped, no cleanup required
  }
}
