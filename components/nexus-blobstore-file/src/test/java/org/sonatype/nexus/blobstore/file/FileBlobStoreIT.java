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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.DefaultBlobIdLocationResolver;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.file.internal.FileBlobStoreMetricsStore;
import org.sonatype.nexus.blobstore.file.internal.SimpleFileOperations;
import org.sonatype.nexus.scheduling.internal.PeriodicJobServiceImpl;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.io.DirectoryHelper;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.tuple.Pair.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.DefaultBlobIdLocationResolver.TEMPORARY_BLOB_ID_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.DIRECT_PATH_BLOB_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.TEMPORARY_BLOB_HEADER;

/**
 * {@link FileBlobStore} integration tests.
 */
public class FileBlobStoreIT
    extends TestSupport
{
  public static final int TEST_DATA_LENGTH = 10;

  private static final int METRICS_FLUSH_TIMEOUT = 5;

  private static final int QUOTA_CHECK_INTERVAL = 5;

  public static final ImmutableMap<String, String> TEST_HEADERS = ImmutableMap.of(
      CREATED_BY_HEADER, "test",
      BLOB_NAME_HEADER, "test/randomData.bin"
  );

  public static final ImmutableMap<String, String> TEMP_HEADERS = ImmutableMap.of(
      CREATED_BY_HEADER, "test",
      BLOB_NAME_HEADER, "test/randomData.bin",
      TEMPORARY_BLOB_HEADER, ""
  );

  private FileBlobStore underTest;

  private Path blobStoreDirectory;

  private Path contentDirectory;

  private FileBlobStoreMetricsStore metricsStore;

  private SimpleFileOperations fileOperations;

  private DefaultBlobIdLocationResolver blobIdResolver;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  DryRunPrefix dryRunPrefix;

  @Mock
  private BlobStoreQuotaService quotaService;

  @Before
  public void setUp() throws Exception {
    when(nodeAccess.getId()).thenReturn(UUID.randomUUID().toString());
    when(dryRunPrefix.get()).thenReturn("");
    ApplicationDirectories applicationDirectories = mock(ApplicationDirectories.class);
    blobStoreDirectory = util.createTempDir().toPath();
    contentDirectory = blobStoreDirectory.resolve("content");
    when(applicationDirectories.getWorkDirectory(anyString())).thenReturn(blobStoreDirectory.toFile());

    fileOperations = spy(new SimpleFileOperations());

    metricsStore = new FileBlobStoreMetricsStore(new PeriodicJobServiceImpl(), nodeAccess, quotaService,
        QUOTA_CHECK_INTERVAL, fileOperations);

    blobIdResolver = new DefaultBlobIdLocationResolver();

    final BlobStoreConfiguration config = new BlobStoreConfiguration();
    config.attributes(FileBlobStore.CONFIG_KEY).set(FileBlobStore.PATH_KEY, blobStoreDirectory.toString());
    underTest = new FileBlobStore(blobIdResolver,
        fileOperations,
        applicationDirectories,
        metricsStore, nodeAccess, dryRunPrefix);
    underTest.init(config);
    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    if (underTest != null) {
      underTest.stop();
    }
  }

  @Test
  public void basicSmokeTest() throws Exception {
    final byte[] content = new byte[TEST_DATA_LENGTH];
    new Random().nextBytes(content);

    final Blob blob = underTest.create(new ByteArrayInputStream(content), TEST_HEADERS);
    verifyMoveOperationsAtomic(blob);

    final byte[] output = extractContent(blob);
    assertThat("data must survive", content, is(equalTo(output)));

    final BlobMetrics metrics = blob.getMetrics();
    assertThat("size must be calculated correctly", metrics.getContentSize(), is(equalTo((long) TEST_DATA_LENGTH)));

    final BlobStoreMetrics storeMetrics = underTest.getMetrics();
    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS).until(() -> underTest.getMetrics().getBlobCount(), is(1L));

    // FIXME: This is no longer valid
    //assertThat(storeMetrics.getTotalSize(), is(equalTo((long) TEST_DATA_LENGTH)));

    assertThat(storeMetrics.getAvailableSpace(), is(greaterThan(0L)));

    final boolean deleted = underTest.delete(blob.getId(), "basicSmokeTest");
    assertThat(deleted, is(equalTo(true)));
    underTest.compact();
    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS).until(() -> underTest.getMetrics().getBlobCount(), is(0L));

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
  public void createAndDeleteBlobWithDirectPathSuccessful() throws IOException {
    final byte[] content = new byte[TEST_DATA_LENGTH];
    new Random().nextBytes(content);

    final Blob blob = underTest.create(new ByteArrayInputStream(content), ImmutableMap.of(
        CREATED_BY_HEADER, "test",
        BLOB_NAME_HEADER, "health-check/repositoryName/bundle.gz",
        DIRECT_PATH_BLOB_HEADER, "true"
    ));
    verifyMoveOperationsAtomic(blob);

    final byte[] output = extractContent(blob);
    assertThat("data must survive", content, is(equalTo(output)));

    final BlobMetrics metrics = blob.getMetrics();
    assertThat("size must be calculated correctly", metrics.getContentSize(), is(equalTo((long) TEST_DATA_LENGTH)));

    final BlobStoreMetrics storeMetrics = underTest.getMetrics();
    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS).until(() -> underTest.getMetrics().getBlobCount(), is(1L));

    assertThat(storeMetrics.getAvailableSpace(), is(greaterThan(0L)));

    final boolean deleted = underTest.delete(blob.getId(), "createAndDeleteBlobWithDirectPathSuccessful");
    assertThat(deleted, is(equalTo(true)));
    underTest.compact();
    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS).until(() -> underTest.getMetrics().getBlobCount(), is(0L));

    final Blob deletedBlob = underTest.get(blob.getId());
    assertThat(deletedBlob, is(nullValue()));
  }

  @Test
  public void getDirectPathBlobIdStreamEmpty() {
    assertThat(underTest.getDirectPathBlobIdStream("nothing").count(), is(0L));
  }

  @Test
  public void testExistsMethodForDirectPathBlob() {
    byte[] content = "hello".getBytes();
    final ImmutableMap<String, String> DIRECT_PATH_HEADERS = ImmutableMap.of(
        CREATED_BY_HEADER, "test",
        BLOB_NAME_HEADER, "health-check/repositoryName/file.txt",
        DIRECT_PATH_BLOB_HEADER, "true"
    );
    BlobId blobId = blobIdResolver.fromHeaders(DIRECT_PATH_HEADERS);
    //At this point the exist test should return false
    assertThat(underTest.exists(blobId), is(false));

    final Blob blob = underTest.create(new ByteArrayInputStream(content), DIRECT_PATH_HEADERS);
    assertThat(blobId.asUniqueString(), is(blob.getId().asUniqueString()));
    //Now the exist test should be true
    assertThat(underTest.exists(blob.getId()), is(true));
  }

  @Test
  public void getDirectPathBlobIdStreamSuccess() throws IOException {
    byte[] content = "hello".getBytes();
    Blob blob = underTest.create(new ByteArrayInputStream(content), ImmutableMap.of(
        CREATED_BY_HEADER, "test",
        BLOB_NAME_HEADER, "health-check/repositoryName/file.txt",
        DIRECT_PATH_BLOB_HEADER, "true"
    ));
    verifyMoveOperationsAtomic(blob);

    assertThat(underTest.getDirectPathBlobIdStream("health-check").count(), is(1L));
    // confirm same result for deeper prefix
    assertThat(underTest.getDirectPathBlobIdStream("health-check/repositoryName").count(), is(1L));
    // confirm same result for the top
    assertThat(underTest.getDirectPathBlobIdStream(".").count(), is(1L));
    assertThat(underTest.getDirectPathBlobIdStream("").count(), is(1L));

    BlobId blobId = underTest.getDirectPathBlobIdStream("health-check").findFirst().get();
    assertThat(blobId, is(blob.getId()));

    // this check is more salient when run on Windows but confirms that direct path BlobIds use unix style paths
    assertThat(blobId.asUniqueString().contains("\\"), is(false));
    assertThat(blobId.asUniqueString().contains("/"), is(true));
  }

  @Test
  public void itWillReturnAllBlobIdsInTheStream() {
    byte[] content = "hello".getBytes();
    Blob regularBlob = underTest.create(new ByteArrayInputStream(content), ImmutableMap.of(
        BLOB_NAME_HEADER, "example",
        CREATED_BY_HEADER, "test"));

    Blob directPathBlob = underTest.create(new ByteArrayInputStream(content), ImmutableMap.of(
        CREATED_BY_HEADER, "test",
        BLOB_NAME_HEADER, "health-check/repositoryName/file.txt",
        DIRECT_PATH_BLOB_HEADER, "true"
    ));

    List<BlobId> blobIds = underTest.getBlobIdStream().collect(Collectors.toList());
    assertThat(blobIds.size(), is(equalTo(2)));
    assertThat(blobIds, containsInAnyOrder(regularBlob.getId(), directPathBlob.getId()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getDirectPathBlobIdStreamPreventsTraversal() {
    underTest.getDirectPathBlobIdStream("../content");
  }

  @Test
  public void overwriteDirectPathBlobSuccessful() throws IOException {
    byte[] content = "hello".getBytes();
    Blob blob = underTest.create(new ByteArrayInputStream(content), ImmutableMap.of(
        CREATED_BY_HEADER, "test",
        BLOB_NAME_HEADER, "health-check/repositoryName/file.txt",
        DIRECT_PATH_BLOB_HEADER, "true"
    ));
    verifyMoveOperationsAtomic(blob);

    byte[] output = extractContent(blob);
    assertThat("data must survive", content, is(equalTo(output)));

    BlobMetrics metrics = blob.getMetrics();
    assertThat("size must be calculated correctly", metrics.getContentSize(), is(equalTo((long) content.length)));

    final BlobStoreMetrics storeMetrics = underTest.getMetrics();
    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS).until(() -> underTest.getMetrics().getBlobCount(), is(1L));

    assertThat(storeMetrics.getAvailableSpace(), is(greaterThan(0L)));

    // now overwrite the blob
    content = "goodbye".getBytes();
    blob = underTest.create(new ByteArrayInputStream(content), ImmutableMap.of(
        CREATED_BY_HEADER, "test",
        BLOB_NAME_HEADER, "health-check/repositoryName/file.txt",
        DIRECT_PATH_BLOB_HEADER, "true"
    ));
    verifyOverwriteOperationsAtomic(blob);

    output = extractContent(blob);
    assertThat("data must have been overwritten", content, is(equalTo(output)));

    metrics = blob.getMetrics();
    assertThat("size must be updated correctly", metrics.getContentSize(), is(equalTo((long) content.length)));

    final boolean deleted = underTest.delete(blob.getId(), " overwriteDirectPathBlobSuccessful");
    assertThat(deleted, is(equalTo(true)));
    underTest.compact();
    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS).until(() -> underTest.getMetrics().getBlobCount(), is(0L));

    final Blob deletedBlob = underTest.get(blob.getId());
    assertThat(deletedBlob, is(nullValue()));
  }

  @Test
  public void testDeleteHardUpdatesMetrics() {
    long initialBlobCount = underTest.getMetrics().getBlobCount();

    final byte[] content = new byte[TEST_DATA_LENGTH];
    final Blob blob = underTest.create(new ByteArrayInputStream(content), TEST_HEADERS);

    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS)
        .until(() -> underTest.getMetrics().getBlobCount(), is(initialBlobCount + 1));

    underTest.deleteHard(blob.getId());

    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS)
        .until(() -> underTest.getMetrics().getBlobCount(), is(initialBlobCount));
  }

  @Test
  public void testSoftDeleteMetricsOnlyUpdateOnCompact() {
    long initialBlobCount = underTest.getMetrics().getBlobCount();
    final byte[] content = new byte[TEST_DATA_LENGTH];

    final Blob blob = underTest.create(new ByteArrayInputStream(content), TEST_HEADERS);
    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS)
        .until(() -> underTest.getMetrics().getBlobCount(), is(initialBlobCount + 1));

    //Standard delete will not update metrics
    underTest.delete(blob.getId(), "testSoftDeleteMetricsOnlyUpdateOnCompact");
    assertThat(underTest.getMetrics().getBlobCount(), is(initialBlobCount + 1));

    //Compact triggers hard delete, so metrics will be updated
    underTest.compact();
    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS)
        .until(() -> underTest.getMetrics().getBlobCount(), is(initialBlobCount));
  }

  @Test
  public void temporaryBlobMoveFallback() throws Exception {
    final byte[] content = new byte[TEST_DATA_LENGTH];
    new Random().nextBytes(content);

    doThrow(new AtomicMoveNotSupportedException("", "", "")).when(fileOperations).moveAtomic(any(), any());

    final Blob blob = underTest.create(new ByteArrayInputStream(content), TEST_HEADERS);
    verifyMoveOperations(blob);

    final byte[] output = extractContent(blob);
    assertThat("data must survive", content, is(equalTo(output)));

    final BlobMetrics metrics = blob.getMetrics();
    assertThat("size must be calculated correctly", metrics.getContentSize(), is(equalTo((long) TEST_DATA_LENGTH)));
  }

  @Test
  public void temporaryBlobMoveFallbackPersists() throws Exception {
    final byte[] content = new byte[TEST_DATA_LENGTH];
    new Random().nextBytes(content);

    doThrow(new AtomicMoveNotSupportedException("", "", "")).when(fileOperations).moveAtomic(any(), any());

    underTest.create(new ByteArrayInputStream(content), TEST_HEADERS);
    underTest.create(new ByteArrayInputStream(content), TEST_HEADERS);

    verify(fileOperations, times(1)).moveAtomic(any(), any());
    verify(fileOperations, times(4)).move(any(), any());
  }

  @Test
  public void hardLinkIngestion() throws Exception {

    final byte[] content = testData();
    final HashCode sha1 = Hashing.sha1().hashBytes(content);
    final Path sourceFile = testFile(content);

    // Attempt to make a hard link to the file we're importing
    final Blob blob = underTest.create(sourceFile, TEST_HEADERS, content.length, sha1);
    assertThat(blob.getId().asUniqueString(), not(startsWith(TEMPORARY_BLOB_ID_PREFIX)));
    assertThat(Files.exists(contentDirectory.resolve(blobIdResolver.getLocation(blob.getId()) +
        FileBlobStore.BLOB_CONTENT_SUFFIX)), is(true));
    assertThat(Files.exists(contentDirectory.resolve(blobIdResolver.getLocation(blob.getId()) +
        FileBlobStore.BLOB_ATTRIBUTE_SUFFIX)), is(true));

    // Now append some telltale bytes to the end of the original file
    final byte[] appendMe = new byte[100];
    Arrays.fill(appendMe, (byte) 3);
    Files.write(sourceFile, appendMe, StandardOpenOption.APPEND);

    // Now see if the blob was modified. This isn't a real use case, but checks the hard link.
    assertThat("blob must have been modified", extractContent(blob), is(equalTo(extractContent(sourceFile))));
  }

  @Test
  public void blobCopyingPreservesBytesUsingHardLink() throws Exception {

    byte[] content = testData();
    HashCode sha1 = Hashing.sha1().hashBytes(content);
    Path sourceFile = testFile(content);

    Blob temp = underTest.create(sourceFile, TEMP_HEADERS, content.length, sha1);

    BlobId copyId = underTest.copy(temp.getId(), TEST_HEADERS).getId();

    underTest.delete(temp.getId(), "blobCopyingPreservesBytesUsingHardLink");

    Blob copy = underTest.get(copyId);

    try (InputStream inputStream = copy.getInputStream()) {
      byte[] copiedBytes = ByteStreams.toByteArray(inputStream);
      assertThat(content, is(copiedBytes));
    }

    assertThat(Files.exists(contentDirectory.resolve(blobIdResolver.getLocation(temp.getId()) +
        FileBlobStore.BLOB_CONTENT_SUFFIX)), is(true));
    assertThat(Files.exists(contentDirectory.resolve(blobIdResolver.getLocation(temp.getId()) +
        FileBlobStore.BLOB_ATTRIBUTE_SUFFIX)), is(true));

    assertThat(Files.exists(contentDirectory.resolve(blobIdResolver.getLocation(copy.getId()) +
        FileBlobStore.BLOB_CONTENT_SUFFIX)), is(true));
    assertThat(Files.exists(contentDirectory.resolve(blobIdResolver.getLocation(copy.getId()) +
        FileBlobStore.BLOB_ATTRIBUTE_SUFFIX)), is(true));

    assertThat(temp.getId().asUniqueString(), startsWith(TEMPORARY_BLOB_ID_PREFIX));
    assertThat(copy.getId().asUniqueString(), not(startsWith(TEMPORARY_BLOB_ID_PREFIX)));
  }

  @Test
  public void blobCopyingPreservesBytesUsingInputStream() throws Exception {

    byte[] content = testData();
    HashCode sha1 = Hashing.sha1().hashBytes(content);
    Path sourceFile = testFile(content);

    Blob temp = underTest.create(sourceFile, TEMP_HEADERS, content.length, sha1);

    doThrow(new IOException()).when(fileOperations).hardLink(any(), any());

    underTest.copy(temp.getId(), TEST_HEADERS).getId();

    BlobId copyId = underTest.copy(temp.getId(), TEST_HEADERS).getId();

    underTest.delete(temp.getId(), "blobCopyingPreservesBytesUsingInputStream");

    Blob copy = underTest.get(copyId);

    try (InputStream inputStream = copy.getInputStream()) {
      byte[] copiedBytes = ByteStreams.toByteArray(inputStream);
      assertThat(content, is(copiedBytes));
    }

    assertThat(Files.exists(contentDirectory.resolve(blobIdResolver.getLocation(temp.getId()) +
        FileBlobStore.BLOB_CONTENT_SUFFIX)), is(true));
    assertThat(Files.exists(contentDirectory.resolve(blobIdResolver.getLocation(temp.getId()) +
        FileBlobStore.BLOB_ATTRIBUTE_SUFFIX)), is(true));

    assertThat(Files.exists(contentDirectory.resolve(blobIdResolver.getLocation(copy.getId()) +
        FileBlobStore.BLOB_CONTENT_SUFFIX)), is(true));
    assertThat(Files.exists(contentDirectory.resolve(blobIdResolver.getLocation(copy.getId()) +
        FileBlobStore.BLOB_ATTRIBUTE_SUFFIX)), is(true));

    assertThat(temp.getId().asUniqueString(), startsWith(TEMPORARY_BLOB_ID_PREFIX));
    assertThat(copy.getId().asUniqueString(), not(startsWith(TEMPORARY_BLOB_ID_PREFIX)));

    verify(fileOperations, times(2)).hardLink(any(), any());
    verify(fileOperations, times(2)).copy(any(), any());
    verify(fileOperations, times(6)).moveAtomic(any(), any());
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

  @Test
  public void blobMoveRetriesOnFileSystemException() throws Exception {
    byte[] content = testData();
    HashCode sha1 = Hashing.sha1().hashBytes(content);
    Path sourceFile = testFile(content);

    doThrow(new FileSystemException("The process cannot access the file because it is being used by another process."))
        .when(fileOperations).moveAtomic(any(), any());

    underTest.create(sourceFile, TEST_HEADERS, content.length, sha1);

    verify(fileOperations, times(2)).copy(any(), any());
    verify(fileOperations, times(2)).delete(any());
  }

  @Test
  public void testSoftDeleteFallsBackToHardDelete() throws Exception {
    byte[] content = new byte[TEST_DATA_LENGTH];
    Blob blob = underTest.create(new ByteArrayInputStream(content), TEST_HEADERS);
    Path bytesPath = contentDirectory.resolve(blobIdResolver.getLocation(blob.getId()) +
        FileBlobStore.BLOB_CONTENT_SUFFIX);
    Path propertiesPath = contentDirectory.resolve(blobIdResolver.getLocation(blob.getId()) +
        FileBlobStore.BLOB_ATTRIBUTE_SUFFIX);

    // truncate blob properties file to simulate corruption
    Files.write(propertiesPath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);

    assertThat(Files.exists(bytesPath), is(true));
    assertThat(Files.exists(propertiesPath), is(true));

    underTest.delete(blob.getId(), "deleting");

    assertThat(Files.exists(bytesPath), is(false));
    assertThat(Files.exists(propertiesPath), is(false));
  }

  private void verifyMoveOperations(Blob blob) throws IOException {
    Pair<Path, Path> paths = verifyBlobPaths(blob);

    verify(fileOperations).move(any(), eq(paths.getLeft()));
    verify(fileOperations).move(any(), eq(paths.getRight()));
  }

  private void verifyMoveOperationsAtomic(Blob blob) throws IOException {
    Pair<Path, Path> paths = verifyBlobPaths(blob);

    verify(fileOperations).moveAtomic(any(), eq(paths.getLeft()));
    verify(fileOperations).moveAtomic(any(), eq(paths.getRight()));
  }

  private void verifyOverwriteOperationsAtomic(Blob blob) throws IOException {
    Pair<Path, Path> paths = verifyBlobPaths(blob);

    verify(fileOperations).overwriteAtomic(any(), eq(paths.getLeft()));
    verify(fileOperations).overwriteAtomic(any(), eq(paths.getRight()));
  }

  /**
   * Verifies the presence of both the blob content and blob attributes on the file system.
   * Returns the {@link Path}s to the blob content (left) and blob attributes (right).
   */
  Pair<Path, Path> verifyBlobPaths(final Blob blob) {
    final Path contentPath = contentDirectory.resolve(blobIdResolver.getLocation(blob.getId()) +
        FileBlobStore.BLOB_CONTENT_SUFFIX);
    final Path attributesPath = contentDirectory.resolve(blobIdResolver.getLocation(blob.getId()) +
        FileBlobStore.BLOB_ATTRIBUTE_SUFFIX);
    assertThat(blob.getId().asUniqueString(), not(startsWith(TEMPORARY_BLOB_ID_PREFIX)));
    assertThat(Files.exists(contentPath), is(true));
    assertThat(Files.exists(attributesPath), is(true));
    return of(contentPath, attributesPath);
  }
}
