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
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.sonatype.nexus.blobstore.BlobIdLocationResolver;
import org.sonatype.nexus.blobstore.BlobStoreReconciliationLogger;
import org.sonatype.nexus.blobstore.DateBasedLocationStrategy;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.file.internal.FileOperations;
import org.sonatype.nexus.blobstore.file.internal.SimpleFileOperations;
import org.sonatype.nexus.blobstore.file.internal.datastore.metrics.DatastoreFileBlobStoreMetricsService;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaUsageChecker;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.DirectoryHelper;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.squareup.tape.QueueFile;
import org.joda.time.DateTime;
import org.junit.After;
import org.slf4j.LoggerFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.sonatype.nexus.blobstore.BlobStoreSupport.CONTENT_PREFIX;
import static org.sonatype.nexus.blobstore.DirectPathLocationStrategy.DIRECT_PATH_ROOT;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER;

/**
 * Tests {@link FileBlobStore}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FileBlobStoreTest

{
  private static final byte[] VALID_BLOB_STORE_PROPERTIES = ("@BlobStore.created-by = admin\n" +
      "size = 40\n" +
      "@Bucket.repo-name = maven-releases\n" +
      "creationTime = 1486679665325\n" +
      "@BlobStore.blob-name = com/sonatype/training/nxs301/03-implicit-staging/maven-metadata.xml.sha1\n" +
      "@BlobStore.content-type = text/plain\n" +
      "sha1 = cbd5bce1c926e6b55b6b4037ce691b8f9e5dea0f").getBytes(StandardCharsets.ISO_8859_1);

  private static final byte[] EMPTY_BLOB_STORE_PROPERTIES = "".getBytes(StandardCharsets.ISO_8859_1);

  private static final String RECONCILIATION = "reconciliation";

  private AtomicBoolean cancelled = new AtomicBoolean(false);

  private final DirectoryHelper directoryHelper = new DirectoryHelper();

  @Mock
  private BlobIdLocationResolver blobIdLocationResolver;

  @Spy
  private FileOperations fileOperations = new SimpleFileOperations(directoryHelper);

  @Mock
  private ApplicationDirectories appDirs;

  @Mock
  private DatastoreFileBlobStoreMetricsService metrics;

  @Mock
  private OperationMetrics operationMetrics;

  @Mock
  private BlobStoreQuotaUsageChecker blobStoreQuotaUsageChecker;

  @Mock
  private LoadingCache loadingCache;

  @Mock
  private BlobStoreUsageChecker blobStoreUsageChecker;

  @Mock
  private FileBlobDeletionIndex fileBlobDeletionIndex;

  @Mock
  private FileBlobAttributes attributes;

  @Mock
  FileBlobAttributes newBlobAttributes;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  DryRunPrefix dryRunPrefix;

  @Mock
  BlobStoreReconciliationLogger reconciliationLogger;

  public static final ImmutableMap<String, String> TEST_HEADERS = ImmutableMap.of(
      CREATED_BY_HEADER, "test",
      BLOB_NAME_HEADER, "test/randomData.bin");

  private FileBlobStore underTest;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Path fullPath;

  private Path directFullPath;

  @Before
  public void initBlobStore() throws Exception {
    CancelableHelper.set(cancelled);
    when(nodeAccess.getId()).thenReturn("test");
    when(dryRunPrefix.get()).thenReturn("");
    when(appDirs.getWorkDirectory(any())).thenReturn(temporaryFolder.newFolder());
    when(attributes.isDeleted()).thenReturn(true);

    Properties properties = new Properties();
    properties.put(HEADER_PREFIX + BLOB_NAME_HEADER, "blobName");
    when(attributes.getProperties()).thenReturn(properties);

    BlobStoreConfiguration configuration = new MockBlobStoreConfiguration();

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> fileMap = new HashMap<>();
    fileMap.put("path", temporaryFolder.getRoot().toPath());
    attributes.put("file", fileMap);

    configuration.setAttributes(attributes);

    underTest = new FileBlobStore(temporaryFolder.newFolder().toPath(), blobIdLocationResolver, fileOperations, appDirs,
        directoryHelper, metrics, configuration, nodeAccess, dryRunPrefix, reconciliationLogger, 0L,
        blobStoreQuotaUsageChecker, fileBlobDeletionIndex);

    when(loadingCache.getUnchecked(any())).thenReturn(underTest.new FileBlob(new BlobId("fakeid")));

    underTest.init(configuration);
    underTest.setLiveBlobs(loadingCache);

    fullPath = underTest.getAbsoluteBlobDir()
        .resolve(CONTENT_PREFIX)
        .resolve("vol-03")
        .resolve("chap-44");
    Files.createDirectories(fullPath);

    directFullPath = underTest.getAbsoluteBlobDir().resolve(CONTENT_PREFIX).resolve("directpath");
    Files.createDirectories(directFullPath);

    when(blobIdLocationResolver.getLocation(any(BlobId.class))).thenAnswer(invocation -> {
      BlobId blobId = (BlobId) invocation.getArguments()[0];
      if (blobId == null) {
        return null;
      }
      if (blobId.getBlobCreatedRef() != null) {
        return new DateBasedLocationStrategy().location(blobId);
      }
      return fullPath.resolve(blobId.asUniqueString()).toString();
    });
    when(blobIdLocationResolver.fromHeaders(any()))
        .thenAnswer(invocation -> new BlobId(UUID.randomUUID().toString()));

    Map<OperationType, OperationMetrics> operationMetricsMap = mock(Map.class);
    when(operationMetricsMap.get(any())).thenReturn(operationMetrics);
    when(metrics.getOperationMetricsDelta()).thenReturn(operationMetricsMap);

    underTest.start();
  }

  @After
  public void tearDown() {
    CancelableHelper.remove();
  }

  @Test
  public void impossibleHardLinkThrowsBlobStoreException() throws Exception {

    Path path = temporaryFolder.newFile().toPath();

    doThrow(new FileSystemException(null)).when(fileOperations).hardLink(any(), any());

    assertThrows(BlobStoreException.class,
        () -> underTest.create(path, TEST_HEADERS, 0, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709")));

    verifyNoInteractions(reconciliationLogger);
  }

  @Test
  public void hardLinkWithPrecalculatedInformation() throws Exception {

    long size = 100L;
    HashCode sha1 = HashCode.fromString("356a192b7913b04c54574d18c28d46e6395428ab");
    Path path = temporaryFolder.newFile().toPath();

    Blob blob = underTest.create(path, TEST_HEADERS, size, sha1);

    assertThat(blob.getMetrics().getContentSize(), is(size));
    assertThat(blob.getMetrics().getSha1Hash(), is("356a192b7913b04c54574d18c28d46e6395428ab"));
    verify(reconciliationLogger).logBlobCreated(eq(underTest.getAbsoluteBlobDir().resolve(RECONCILIATION)), any());
  }

  @Test
  public void blobIdCollisionCausesRetry() throws Exception {
    long size = 100L;
    HashCode sha1 = HashCode.fromString("356a192b7913b04c54574d18c28d46e6395428ab");
    Path path = temporaryFolder.newFile().toPath();

    doReturn(true, true, true, false).when(fileOperations).exists(any());

    underTest.create(path, TEST_HEADERS, size, sha1);

    verify(fileOperations, times(4)).exists(any());
    verify(reconciliationLogger, times(1))
        .logBlobCreated(eq(underTest.getAbsoluteBlobDir().resolve(RECONCILIATION)), any());
  }

  @Test
  public void blobIdCollisionThrowsExceptionOnRetryLimit() throws Exception {

    long size = 100L;
    HashCode sha1 = HashCode.fromString("356a192b7913b04c54574d18c28d46e6395428ab");

    Path path = temporaryFolder.newFile().toPath();

    doReturn(true).when(fileOperations).exists(any());

    assertThrows(BlobStoreException.class, () -> underTest.create(path, TEST_HEADERS, size, sha1));
    verify(fileOperations, times(FileBlobStore.MAX_COLLISION_RETRIES + 1)).exists(any());
  }

  byte[] deletedBlobStoreProperties = ("deleted = true\n" +
      "@BlobStore.created-by = admin\n" +
      "size = 40\n" +
      "@Bucket.repo-name = maven-releases\n" +
      "creationTime = 1486679665325\n" +
      "@BlobStore.blob-name = com/sonatype/training/nxs301/03-implicit-staging/maven-metadata.xml.sha1\n" +
      "@BlobStore.content-type = text/plain\n" +
      "sha1 = cbd5bce1c926e6b55b6b4037ce691b8f9e5dea0f").getBytes(StandardCharsets.ISO_8859_1);

  @Test
  public void testDoCompact_RebuildMetadataNeeded() throws Exception {
    when(nodeAccess.isOldestNode()).thenReturn(true);

    write(fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.properties"),
        deletedBlobStoreProperties);

    checkDeletionsIndex(true);
    setRebuildMetadataToTrue();

    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    checkDeletionsIndex(true);

    verify(blobStoreUsageChecker, atLeastOnce()).test(any(), any(), any());
  }

  @Test
  public void testDoCompact_RebuildMetadataNeeded_NotOldestNode() throws Exception {
    when(nodeAccess.isOldestNode()).thenReturn(false);

    checkDeletionsIndex(true);
    setRebuildMetadataToTrue();

    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    checkDeletionsIndex(true);

    verify(blobStoreUsageChecker, never()).test(any(), any(), any());
  }

  @Test
  public void testDoCompact_clearsDirectPathEmptyDirectories() throws Exception {
    when(nodeAccess.isOldestNode()).thenReturn(true);

    Path fileInSubdir1 = directFullPath.resolve("subdir").resolve("somefile.txt");
    fileInSubdir1.toFile().getParentFile().mkdirs();
    write(fileInSubdir1, "somefile".getBytes(UTF_8));

    Path subdir2 = directFullPath.resolve("subdir2");
    subdir2.toFile().mkdirs();

    assertThat(fileInSubdir1.toFile().exists(), is(true));
    assertThat(subdir2.toFile().exists(), is(true));

    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    assertThat(fileInSubdir1.toFile().exists(), is(true));
    assertThat(subdir2.toFile().exists(), is(false));
  }

  @Test
  public void testDoDeleteHard() throws Exception {
    BlobId blobId = new BlobId("0515c8b9-0de0-49d4-bcf0-7738c40c9c5e");
    Path bytesPath = underTest.getAbsoluteBlobDir()
        .resolve(CONTENT_PREFIX)
        .resolve("vol-03")
        .resolve("chap-44")
        .resolve("0515c8b9-0de0-49d4-bcf0-7738c40c9c5e.bytes");
    bytesPath.toFile().getParentFile().mkdirs();
    Path written = write(bytesPath, "hello".getBytes(StandardCharsets.UTF_8));
    assertThat(written.toFile().exists(), is(true));
    // oddly FileOperations is a mock here; we need to provide a real delete for this test
    doAnswer(invocationOnMock -> {
      Files.delete(bytesPath);
      return true;
    }).when(fileOperations).delete(bytesPath);

    Path propertiesPath = underTest.getAbsoluteBlobDir()
        .resolve(CONTENT_PREFIX)
        .resolve("vol-03")
        .resolve("chap-44")
        .resolve("0515c8b9-0de0-49d4-bcf0-7738c40c9c5e.properties");

    Map<String, String> properties = new HashMap<>();
    properties.put("sha1", "a5aa215f17898e21986cb19d4b72f6bebf86c4bd");
    properties.put("BlobStore.blob-name", "/content/foo/tree.txt");
    properties.put("BlobStore.created-by", "admin");
    properties.put("size", "5");
    properties.put("creationTime", "1736870404222");
    properties.put("Bucket.repo-name", "raw");
    BlobMetrics blobMetrics =
        new BlobMetrics(new DateTime(1736870404222L), "a5aa215f17898e21986cb19d4b72f6bebf86c4bd", 5);
    FileBlobAttributes attributes = new FileBlobAttributes(propertiesPath, properties, blobMetrics);
    attributes.store();

    // oddly FileOperations is a mock here; we need to provide a real delete for this test
    doAnswer(invocationOnMock -> {
      Files.delete(propertiesPath);
      return true;
    }).when(fileOperations).delete(propertiesPath);

    assertThat(propertiesPath.toFile().exists(), is(true));
    boolean deleted = underTest.doDeleteHard(blobId);
    assertThat(deleted, is(true));
    assertThat(propertiesPath.toFile().exists(), is(false));
  }

  @Test
  public void testUndelete_AttributesNotDeleted() throws Exception {
    when(attributes.isDeleted()).thenReturn(false);

    boolean result = underTest.undelete(blobStoreUsageChecker, new BlobId("fakeid"), attributes, false);
    assertThat(result, is(false));
    verify(blobStoreUsageChecker, never()).test(eq(underTest), any(BlobId.class), anyString());
  }

  @Test
  public void testUndelete_CheckerNull() throws Exception {
    boolean result = underTest.undelete(null, new BlobId("fakeid"), attributes, false);
    assertThat(result, is(false));
  }

  @Test
  public void testUndelete_CheckInUse() throws Exception {
    when(blobStoreUsageChecker.test(eq(underTest), any(BlobId.class), anyString())).thenReturn(true);

    boolean result = underTest.undelete(blobStoreUsageChecker, new BlobId("fakeid"), attributes, false);
    assertThat(result, is(true));
    verify(attributes).setDeleted(false);
    verify(attributes).setDeletedReason(null);
    verify(attributes).store();
  }

  @Test
  public void testUndelete_CheckInUse_DryRun() throws Exception {
    when(blobStoreUsageChecker.test(eq(underTest), any(BlobId.class), anyString())).thenReturn(true);

    boolean result = underTest.undelete(blobStoreUsageChecker, new BlobId("fakeid"), attributes, true);
    assertThat(result, is(true));
    verify(attributes).getProperties();
    verify(attributes).isDeleted();
    verify(attributes).getDeletedReason();
    verify(attributes).getOriginalLocation();
    verifyNoMoreInteractions(attributes);
  }

  private void setRebuildMetadataToTrue() throws IOException {
    PropertiesFile metadataPropertiesFile = new PropertiesFile(
        underTest.getAbsoluteBlobDir().resolve(FileBlobStore.METADATA_FILENAME).toFile());
    metadataPropertiesFile.setProperty(FileBlobStore.REBUILD_DELETED_BLOB_INDEX_KEY, "true");
    metadataPropertiesFile.store();
  }

  private void checkDeletionsIndex(final boolean expectEmpty) throws IOException {
    QueueFile queueFile = new QueueFile(underTest.getAbsoluteBlobDir().resolve("test-deletions.index").toFile());
    assertThat(queueFile.isEmpty(), is(expectEmpty));
    queueFile.close();
  }

  byte[] deletedBlobStorePropertiesNoBlobName = ("deleted = true\n" +
      "@BlobStore.created-by = admin\n" +
      "size = 40\n" +
      "@Bucket.repo-name = maven-releases\n" +
      "creationTime = 1486679665325\n" +
      "@BlobStore.content-type = text/plain\n" +
      "sha1 = cbd5bce1c926e6b55b6b4037ce691b8f9e5dea0f").getBytes(StandardCharsets.ISO_8859_1);

  @Test
  public void testCompactCorruptAttributes() throws Exception {
    when(nodeAccess.isOldestNode()).thenReturn(true);

    write(fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.properties"),
        deletedBlobStorePropertiesNoBlobName);

    setRebuildMetadataToTrue();

    underTest.compact(null, Duration.ofDays(100));

    verify(fileOperations, times(2)).delete(any());
  }

  @Test
  public void testCompactIsCancelable() throws Exception {
    when(nodeAccess.isOldestNode()).thenReturn(true);

    write(fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.properties"),
        deletedBlobStoreProperties);

    setRebuildMetadataToTrue();
    cancelled.set(true);

    Duration ago = Duration.ofDays(100);
    assertThrows(TaskInterruptedException.class, () -> underTest.compact(null, ago));

    verify(fileOperations, never()).delete(any());
  }

  @Test
  public void testDeleteWithCorruptAttributes() throws Exception {
    when(nodeAccess.isOldestNode()).thenReturn(true);

    Path bytesPath = fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.bytes");
    Path propertiesPath = fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.properties");
    write(propertiesPath, EMPTY_BLOB_STORE_PROPERTIES);

    underTest.delete(new BlobId("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7"), "deleting");

    verify(fileOperations).delete(propertiesPath);
    verify(fileOperations).delete(bytesPath);
  }

  /**
   * This test guarantees we are returning unix-style paths for {@link BlobId}s returned by
   * {@link FileBlobStore#getDirectPathBlobIdStream(String)}. This test would fail on Windows if
   * {@link FileBlobStore#toBlobName(Path)} wasn't implemented correctly.
   */
  @Test
  public void toBlobName() {
    // /full/path/on/disk/to/content/directpath/some/direct/path/file.txt.properties
    Path absolute = underTest.getContentDir().resolve(DIRECT_PATH_ROOT).resolve("some/direct/path/file.txt.properties");
    assertThat(underTest.toBlobName(absolute), is("some/direct/path/file.txt"));
  }

  @Test
  public void toBlobNamePropertiesSuffix() {
    // /full/path/on/disk/to/content/directpath/some/direct/path/file.properties.properties
    Path absolute =
        underTest.getContentDir().resolve(DIRECT_PATH_ROOT).resolve("some/direct/path/file.properties.properties");
    assertThat(underTest.toBlobName(absolute), is("some/direct/path/file.properties"));
  }

  @Test
  public void getBlobAttributes() throws Exception {
    Path propertiesPath = fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.properties");
    write(propertiesPath, VALID_BLOB_STORE_PROPERTIES);

    assertNotNull(underTest.getBlobAttributes(new BlobId("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7")));
  }

  @Test
  public void getBlobAttributesReturnsNullWhenPropertiesFileIsNonExistent() {
    assertNull(underTest.getBlobAttributes(new BlobId("non-existent-blob")));
  }

  @Test
  public void getBlobAttributesReturnsNullWhenExceptionIsThrown() throws Exception {
    Path propertiesPath = underTest.getAbsoluteBlobDir().resolve(CONTENT_PREFIX).resolve("test-blob.properties");
    write(propertiesPath, EMPTY_BLOB_STORE_PROPERTIES);

    assertNull(underTest.getBlobAttributes(new BlobId("test-blob")));
  }

  @Test
  public void testBytesExists() throws Exception {
    Path bytesPath = fullPath.resolve("test-blob.bytes");
    write(bytesPath, "some bytes content".getBytes());
    when(fileOperations.exists(bytesPath)).thenReturn(true);

    assertThat(bytesPath.toFile().exists(), is(true));

    assertThat(underTest.bytesExists(new BlobId("test-blob")), is(true));
  }

  @Test
  public void testCreateBlobAttributes() {
    BlobId blobId = new BlobId("fakeid", UTC.now());
    final DateTime creationTime = new DateTime();
    String sha1 = "356a192b7913b04c54574d18c28d46e6395428ab";
    long size = 10L;

    BlobMetrics blobMetrics = new BlobMetrics(creationTime, sha1, size);
    underTest.createBlobAttributes(blobId, TEST_HEADERS, blobMetrics);

    BlobAttributes blobAttributes = underTest.getBlobAttributes(blobId);
    assertNotNull(blobAttributes);

    // test headers were written
    Map<String, String> headers = blobAttributes.getHeaders();
    TEST_HEADERS.forEach((header, value) -> assertThat(headers.get(header), is(value)));

    // test metrics were written
    BlobMetrics metrics = blobAttributes.getMetrics();
    assertThat(metrics.getContentSize(), is(size));
    assertThat(metrics.getSha1Hash(), is(sha1));
    assertThat(metrics.getCreationTime(), is(creationTime));
  }

  /*
   * Verify the behaviour of blobs which are stored under the volume-chaptor paths
   */
  @Test
  public void testGetBlobIdUpdatedSinceStream_volume() throws InterruptedException {
    OffsetDateTime fromDateTime = OffsetDateTime.now().minusMinutes(1);

    List<BlobId> expected = new ArrayList<>();
    expected.add(underTest.create(new ByteArrayInputStream(new byte[0]), TEST_HEADERS).getId());
    expected.add(underTest.create(new ByteArrayInputStream(new byte[0]), TEST_HEADERS).getId());
    // we expect to visit both property and bytes files
    expected.addAll(expected);

    OffsetDateTime toDateTime = OffsetDateTime.now();
    // Sleep to prevent a race condition where the file operation occurs in the same ms as the date we created
    Thread.sleep(2);
    underTest.create(new ByteArrayInputStream(new byte[0]), TEST_HEADERS);

    try (Stream<BlobId> blobIds = underTest.getBlobIdUpdatedSinceStream("vol-", fromDateTime, toDateTime)) {
      List<BlobId> actual = blobIds.toList();
      assertThat(actual, containsInAnyOrder(expected.toArray()));
    }
  }

  /*
   * Verify the behaviour of blobs which are stored using date-based paths
   */
  @Test
  public void testGetBlobIdUpdatedSinceStream_dateBased() {
    OffsetDateTime fromDateTime = LocalDateTime.now().atOffset(ZoneOffset.UTC);
    when(blobIdLocationResolver.fromHeaders(any()))
        .thenReturn(new BlobId(UUID.randomUUID().toString(), fromDateTime.plusMinutes(1)),
            new BlobId(UUID.randomUUID().toString(), fromDateTime.plusMinutes(2)));

    List<BlobId> expected = new ArrayList<>();
    expected.add(underTest.create(new ByteArrayInputStream(new byte[0]), TEST_HEADERS).getId());
    expected.add(underTest.create(new ByteArrayInputStream(new byte[0]), TEST_HEADERS).getId());
    // we expect to visit both property and bytes files
    expected.addAll(expected);

    OffsetDateTime toDateTime = fromDateTime.plusDays(1);

    when(blobIdLocationResolver.fromHeaders(any()))
        .thenAnswer(invocation -> new BlobId(UUID.randomUUID().toString(), toDateTime.plusYears(1)));
    underTest.create(new ByteArrayInputStream(new byte[0]), TEST_HEADERS);

    String pattern = fromDateTime.format(DateTimeFormatter.ofPattern("yyyy"));

    try (Stream<BlobId> blobIds = underTest.getBlobIdUpdatedSinceStream(pattern, fromDateTime, toDateTime)) {
      List<BlobId> actual = blobIds.toList();
      assertThat(actual, containsInAnyOrder(expected.toArray()));
    }
  }

  @Test
  public void testDoDeleteWithDateBasedLayoutEnabled() throws Exception {
    TestFileBlobStore underTest = createFixture();

    BlobId blobId = new BlobId("test-blob-id");
    Path path = underTest.attributePath(new BlobId(blobId.asUniqueString(), UTC.now()));
    when(attributes.isDeleted()).thenReturn(false);
    when(underTest.getFileBlobAttributes(blobId)).thenReturn(attributes);
    when(underTest.getFileBlobAttributes(path)).thenReturn(newBlobAttributes);

    boolean result = underTest.doDelete(blobId, "test-reason");

    assertTrue(result);
    assertAttributes(blobId);

    verify(attributes).setDeletedDateTime(any());
    verify(attributes).setSoftDeletedLocation(anyString());
    verify(newBlobAttributes).updateFrom(attributes);
    verify(newBlobAttributes).setOriginalLocation(anyString());
    verify(newBlobAttributes).store();
  }

  @Test
  public void testIsOwner() throws Exception {
    assertThat(underTest.isOwner(mock(Blob.class)), is(false));

    BlobStoreConfiguration configuration = new MockBlobStoreConfiguration();
    Map<String, Object> file = new HashMap<>();
    file.put("path", temporaryFolder.newFolder().getAbsolutePath());
    configuration.setAttributes(Map.of("file", file));

    Blob blob = createBlobStore(configuration).create(new ByteArrayInputStream(new byte[0]),
        Map.of(BLOB_NAME_HEADER, "foo", CREATED_BY_HEADER, "jsmith"));
    assertThat("Blob owned by different instance should return false", underTest.isOwner(blob), is(false));

    blob = underTest.create(new ByteArrayInputStream(new byte[0]),
        Map.of(BLOB_NAME_HEADER, "foo", CREATED_BY_HEADER, "jsmith"));
    assertThat(underTest.isOwner(blob), is(true));
  }

  private TestFileBlobStore createFixture() throws IOException {
    BlobStoreConfiguration configuration = new MockBlobStoreConfiguration();

    Map<String, Map<String, Object>> attributes1 = new HashMap<>();
    Map<String, Object> fileMap = new HashMap<>();
    fileMap.put("path", temporaryFolder.getRoot().toPath());
    attributes1.put("file", fileMap);

    configuration.setAttributes(attributes1);

    TestFileBlobStore underTest = spy(new TestFileBlobStore(
        temporaryFolder.newFolder().toPath(), blobIdLocationResolver, fileOperations, appDirs, directoryHelper, metrics,
        configuration, nodeAccess, dryRunPrefix, reconciliationLogger, 0L, blobStoreQuotaUsageChecker,
        fileBlobDeletionIndex));

    underTest.init(configuration);
    try {
      underTest.start();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    underTest.setLiveBlobs(loadingCache);
    return underTest;
  }

  private void assertAttributes(final BlobId blobId) throws Exception {
    verify(attributes).setDeleted(true);
    verify(attributes).setDeletedReason("test-reason");
    verify(attributes).store();
    verify(fileBlobDeletionIndex).createRecord(blobId);
  }

  private FileBlobStore createBlobStore(final BlobStoreConfiguration configuration) throws Exception {
    FileBlobStore blobstore = new FileBlobStore(temporaryFolder.newFolder().toPath(), blobIdLocationResolver,
        fileOperations, appDirs, directoryHelper, metrics, configuration, nodeAccess, dryRunPrefix,
        reconciliationLogger, 0L, blobStoreQuotaUsageChecker, fileBlobDeletionIndex);
    blobstore.init(configuration);
    blobstore.setLiveBlobs(loadingCache);
    blobstore.start();

    return blobstore;
  }

  // test class to provide isReconcilePlanEnabled() method
  private class TestFileBlobStore
      extends FileBlobStore
  {
    public TestFileBlobStore(
        final Path root,
        final BlobIdLocationResolver blobIdLocationResolver,
        final FileOperations fileOperations,
        final ApplicationDirectories appDirs,
        final DirectoryHelper directoryHelper,
        final DatastoreFileBlobStoreMetricsService metrics,
        final BlobStoreConfiguration configuration,
        final NodeAccess nodeAccess,
        final DryRunPrefix dryRunPrefix,
        final BlobStoreReconciliationLogger reconciliationLogger,
        final long blobStoreQuota,
        final BlobStoreQuotaUsageChecker blobStoreQuotaUsageChecker,
        final FileBlobDeletionIndex fileBlobDeletionIndex)
    {
      super(root, blobIdLocationResolver, fileOperations, appDirs, directoryHelper, metrics, configuration, nodeAccess,
          dryRunPrefix, reconciliationLogger, blobStoreQuota, blobStoreQuotaUsageChecker, fileBlobDeletionIndex);
    }
  }

  @Test
  public void testGet_RetriesForSoftDeletedBlobWhenIncludeDeletedIsTrue() throws Exception {
    // Given
    BlobId blobId = new BlobId("soft-deleted-blob");

    // Create dummy file for this blob ID so Files.exists() check passes
    Path blobPath = fullPath.resolve(blobId.asUniqueString() + ".bytes");
    Files.createDirectories(blobPath.getParent());
    Files.write(blobPath, "dummy content".getBytes());

    // Mock a stale blob that fails refresh on first attempt, then succeeds
    FileBlobStore.FileBlob staleBlob = mock(FileBlobStore.FileBlob.class);
    when(staleBlob.isStale()).thenReturn(true);

    // Mock a fresh blob that succeeds
    FileBlobStore.FileBlob freshBlob = mock(FileBlobStore.FileBlob.class);
    when(freshBlob.isStale()).thenReturn(false);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    when(freshBlob.getMetrics()).thenReturn(mockMetrics);

    when(loadingCache.getUnchecked(blobId))
        .thenReturn(staleBlob)
        .thenReturn(freshBlob);

    // Create spy and set up retry configuration and cache
    FileBlobStore spyUnderTest = spy(underTest);
    spyUnderTest.setRetryConfiguration(2, 50);

    // Mock refreshBlob to return null on first attempt (failure), then fresh blob (success)
    doReturn(null, freshBlob).when(spyUnderTest).refreshBlob(staleBlob, blobId, true);

    spyUnderTest.setLiveBlobs(loadingCache);

    // When - includeDeleted is true, so it should retry even for soft-deleted blobs
    Blob result = spyUnderTest.get(blobId, true);

    // Then
    assertThat(result, is(freshBlob));
    verify(loadingCache, times(2)).getUnchecked(blobId);
  }

  @Test
  public void testGet_ExhaustsAllRetries() throws Exception {
    // Given
    underTest.setRetryConfiguration(3, 25);
    BlobId blobId = new BlobId("exhausted-retry-blob");

    // Create dummy file for this blob ID so Files.exists() check passes
    Path blobPath = fullPath.resolve(blobId.asUniqueString() + ".bytes");
    Files.createDirectories(blobPath.getParent());
    Files.write(blobPath, "dummy content".getBytes());

    // Mock a stale blob that consistently fails to refresh
    FileBlobStore.FileBlob staleBlob = mock(FileBlobStore.FileBlob.class);
    when(staleBlob.isStale()).thenReturn(true);

    when(loadingCache.getUnchecked(blobId)).thenReturn(staleBlob);

    // Mock the refresh operation to always return null (simulating persistent failure)
    FileBlobStore spyUnderTest = spy(underTest);
    doReturn(null).when(spyUnderTest).refreshBlob(staleBlob, blobId, false);

    // Set the mocked cache on the spy
    spyUnderTest.setLiveBlobs(loadingCache);

    // When - all attempts return null, should exhaust retries
    Blob result = spyUnderTest.get(blobId, false);

    // Then
    assertThat(result, is(nullValue()));
    verify(loadingCache, times(4)).getUnchecked(blobId);
  }

  @Test
  public void testGet_IOExceptionDuringBlobAttributesLoad() throws Exception {
    // Given
    BlobId blobId = new BlobId("io-exception-blob");

    // Create dummy file for this blob ID so Files.exists() check passes
    Path blobPath = fullPath.resolve(blobId.asUniqueString() + ".bytes");
    Files.createDirectories(blobPath.getParent());
    Files.write(blobPath, "dummy content".getBytes());

    // Mock a stale blob that fails refresh on first attempt, then succeeds
    FileBlobStore.FileBlob staleBlob = mock(FileBlobStore.FileBlob.class);
    when(staleBlob.isStale()).thenReturn(true);

    // Mock a fresh blob that succeeds
    FileBlobStore.FileBlob freshBlob = mock(FileBlobStore.FileBlob.class);
    when(freshBlob.isStale()).thenReturn(false);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    when(freshBlob.getMetrics()).thenReturn(mockMetrics);

    when(loadingCache.getUnchecked(blobId))
        .thenReturn(staleBlob)
        .thenReturn(freshBlob);

    // Create spy and set up retry configuration and cache
    FileBlobStore spyUnderTest = spy(underTest);
    spyUnderTest.setRetryConfiguration(2, 50);

    // Mock refreshBlob to return null on first attempt (failure), then fresh blob (success)
    doReturn(null, freshBlob).when(spyUnderTest).refreshBlob(staleBlob, blobId, false);

    spyUnderTest.setLiveBlobs(loadingCache);

    // When - IOException during attribute loading should trigger retries
    Blob result = spyUnderTest.get(blobId, false);

    // Then - should retry since IOException means we can't determine if it's soft-deleted
    assertThat(result, is(freshBlob));
    verify(loadingCache, times(2)).getUnchecked(blobId);
  }

  @Test
  public void testGet_FirstAttemptSucceedsNoRetries() throws Exception {
    // Given
    BlobId blobId = new BlobId("first-success-blob");

    // Create dummy file for this blob ID so Files.exists() check passes
    Path blobPath = fullPath.resolve(blobId.asUniqueString() + ".bytes");
    Files.createDirectories(blobPath.getParent());
    Files.write(blobPath, "dummy content".getBytes());

    // Mock a non-stale blob that will succeed immediately
    FileBlobStore.FileBlob fileBlob = mock(FileBlobStore.FileBlob.class);
    when(fileBlob.isStale()).thenReturn(false);
    BlobMetrics mockMetrics = mock(BlobMetrics.class);
    when(fileBlob.getMetrics()).thenReturn(mockMetrics);

    final LoadingCache<BlobId, org.sonatype.nexus.blobstore.BlobSupport> liveBlobsMock = mock(LoadingCache.class);
    when(liveBlobsMock.getUnchecked(blobId)).thenReturn(fileBlob);

    // Create spy and set up retry configuration and cache
    FileBlobStore spyUnderTest = spy(underTest);
    spyUnderTest.setRetryConfiguration(2, 100);
    spyUnderTest.setLiveBlobs(liveBlobsMock);

    // When
    Blob result = spyUnderTest.get(blobId, false);

    // Then - should succeed immediately without any retries
    assertThat(result, is(fileBlob));
    verify(liveBlobsMock, times(1)).getUnchecked(blobId);
  }

  @Test
  public void testSetBlobAttributes_NullCheck_ReturnsEarly() throws Exception {
    BlobId blobId = new BlobId("null-blob");
    BlobAttributes blobAttributes = mock(BlobAttributes.class);

    underTest.setBlobAttributes(blobId, blobAttributes);

    verify(blobAttributes, never()).getProperties();
  }

  @Test
  public void testSetBlobAttributes_SuccessfulFirstAttempt() throws Exception {
    BlobId blobId = new BlobId("success-blob");
    BlobAttributes blobAttributes = mock(BlobAttributes.class);
    FileBlobAttributes fileBlobAttributes = mock(FileBlobAttributes.class);

    FileBlobStore spyUnderTest = spy(underTest);
    when(spyUnderTest.getFileBlobAttributes(blobId)).thenReturn(fileBlobAttributes);

    spyUnderTest.setBlobAttributes(blobId, blobAttributes);

    verify(fileBlobAttributes, times(1)).updateFrom(blobAttributes);
    verify(fileBlobAttributes, times(1)).store();
  }

  @Test
  public void testSetBlobAttributes_RetriesOnException() throws Exception {
    BlobId blobId = new BlobId("retry-blob");
    BlobAttributes blobAttributes = mock(BlobAttributes.class);
    FileBlobAttributes fileBlobAttributes = mock(FileBlobAttributes.class);

    FileBlobStore spyUnderTest = spy(underTest);
    when(spyUnderTest.getFileBlobAttributes(blobId)).thenReturn(fileBlobAttributes);
    doThrow(new IOException("Concurrent write")).doNothing().when(fileBlobAttributes).store();

    spyUnderTest.setBlobAttributes(blobId, blobAttributes);

    verify(fileBlobAttributes, times(2)).updateFrom(blobAttributes);
    verify(fileBlobAttributes, times(2)).store();
  }

  @Test
  public void testSetBlobAttributes_ExhaustsAllRetries() throws Exception {
    BlobId blobId = new BlobId("exhausted-retry-blob");
    BlobAttributes blobAttributes = mock(BlobAttributes.class);
    FileBlobAttributes fileBlobAttributes = mock(FileBlobAttributes.class);

    FileBlobStore spyUnderTest = spy(underTest);
    when(spyUnderTest.getFileBlobAttributes(blobId)).thenReturn(fileBlobAttributes);
    doThrow(new IOException("Persistent failure")).when(fileBlobAttributes).store();

    try {
      spyUnderTest.setBlobAttributes(blobId, blobAttributes);
      fail("Expected BlobStoreException");
    }
    catch (BlobStoreException e) {
      assertTrue(e.getMessage().contains("Unable to set BlobAttributes after retries"));
    }

    verify(fileBlobAttributes, times(3)).updateFrom(blobAttributes);
    verify(fileBlobAttributes, times(3)).store();
  }

  @Test
  public void testSetBlobAttributes_NullDuringRetry() throws Exception {
    BlobId blobId = new BlobId("null-during-retry-blob");
    BlobAttributes blobAttributes = mock(BlobAttributes.class);
    FileBlobAttributes fileBlobAttributes = mock(FileBlobAttributes.class);

    FileBlobStore spyUnderTest = spy(underTest);
    when(spyUnderTest.getFileBlobAttributes(blobId))
        .thenReturn(fileBlobAttributes)
        .thenReturn(fileBlobAttributes)
        .thenReturn(null);
    doThrow(new IOException("Concurrent write")).when(fileBlobAttributes).store();

    spyUnderTest.setBlobAttributes(blobId, blobAttributes);

    verify(fileBlobAttributes, times(1)).updateFrom(blobAttributes);
    verify(fileBlobAttributes, times(1)).store();
  }

  @Test
  public void testSetBlobAttributes_HandlesNoSuchFileExceptionGracefully() throws Exception {
    // Test for NEXUS-51247: NoSuchFileException during concurrent writes should not log ERROR
    BlobId blobId = new BlobId("concurrent-write-blob");
    BlobAttributes blobAttributes = mock(BlobAttributes.class);
    FileBlobAttributes fileBlobAttributes = mock(FileBlobAttributes.class);

    FileBlobStore spyUnderTest = spy(underTest);
    when(spyUnderTest.getFileBlobAttributes(blobId)).thenReturn(fileBlobAttributes);
    doThrow(new java.nio.file.NoSuchFileException("blob.properties")).when(fileBlobAttributes).store();

    // Should not throw exception, should return gracefully
    spyUnderTest.setBlobAttributes(blobId, blobAttributes);

    // Verify it attempted to update but didn't retry (returned early due to race condition)
    verify(fileBlobAttributes, times(1)).updateFrom(blobAttributes);
    verify(fileBlobAttributes, times(1)).store();
  }

  @Test
  public void testGetBlobAttributesWithException_FileNotFoundReturnNullNotError() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(FileBlobStore.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
    try {
      BlobId blobId = new BlobId("concurrent-read-blob");
      FileBlobAttributes fileBlobAttributes = mock(FileBlobAttributes.class);
      doThrow(new java.io.FileNotFoundException("blob.properties (No such file or directory)"))
          .when(fileBlobAttributes)
          .load();

      FileBlobStore spyUnderTest = spy(underTest);
      doReturn(fileBlobAttributes).when(spyUnderTest).newFileBlobAttributes(any());

      BlobAttributes result = spyUnderTest.getBlobAttributesWithException(blobId);

      assertNull(result);
      verify(fileBlobAttributes, times(1)).load();
      assertThat(listAppender.list.stream().anyMatch(e -> e.getLevel() == Level.ERROR), is(false));
    }
    finally {
      logger.detachAppender(listAppender);
    }
  }

  @Test
  public void testGetBlobAttributesWithException_NoSuchFileReturnNullNotError() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(FileBlobStore.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
    try {
      BlobId blobId = new BlobId("concurrent-read-no-such-file");
      FileBlobAttributes fileBlobAttributes = mock(FileBlobAttributes.class);
      doThrow(new java.nio.file.NoSuchFileException("blob.properties"))
          .when(fileBlobAttributes)
          .load();

      FileBlobStore spyUnderTest = spy(underTest);
      doReturn(fileBlobAttributes).when(spyUnderTest).newFileBlobAttributes(any());

      BlobAttributes result = spyUnderTest.getBlobAttributesWithException(blobId);

      assertNull(result);
      verify(fileBlobAttributes, times(1)).load();
      assertThat(listAppender.list.stream().anyMatch(e -> e.getLevel() == Level.ERROR), is(false));
    }
    finally {
      logger.detachAppender(listAppender);
    }
  }

  @Test
  public void testGetBlobAttributesWithException_OtherIOExceptionThrowsBlobStoreException() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(FileBlobStore.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
    try {
      BlobId blobId = new BlobId("corrupt-blob");
      FileBlobAttributes fileBlobAttributes = mock(FileBlobAttributes.class);
      doThrow(new IOException("Disk read error"))
          .when(fileBlobAttributes)
          .load();

      FileBlobStore spyUnderTest = spy(underTest);
      doReturn(fileBlobAttributes).when(spyUnderTest).newFileBlobAttributes(any());

      assertThrows(BlobStoreException.class, () -> spyUnderTest.getBlobAttributesWithException(blobId));
      assertThat(listAppender.list.stream().anyMatch(e -> e.getLevel() == Level.ERROR), is(true));
    }
    finally {
      logger.detachAppender(listAppender);
    }
  }

  @Test
  public void testPruneEmptyContentDirectories_RemovesNestedEmptyDirectories() throws Exception {
    // Create nested empty directories in content area
    Path contentDir = underTest.getContentDir();
    Path level1 = contentDir.resolve("2024/01");
    Path level2 = level1.resolve("15/10");
    Path level3 = level2.resolve("30");
    Files.createDirectories(level3);

    // Create another set of nested empty directories
    Path anotherLevel1 = contentDir.resolve("2024/02");
    Path anotherLevel2 = anotherLevel1.resolve("20/14");
    Files.createDirectories(anotherLevel2);

    // Verify directories were created
    assertThat(Files.exists(level3), is(true));
    assertThat(Files.exists(anotherLevel2), is(true));

    // Run compact which should trigger empty directory cleanup
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    // Verify all empty nested directories were removed
    assertThat(Files.exists(level3), is(false));
    assertThat(Files.exists(level2), is(false));
    assertThat(Files.exists(level1), is(false));
    assertThat(Files.exists(anotherLevel2), is(false));
    assertThat(Files.exists(anotherLevel1), is(false));
  }

  @Test
  public void testPruneEmptyContentDirectories_SkipsSpecialDirectoryRoots() throws Exception {
    Path contentDir = underTest.getContentDir();

    // Create TMP directory with empty subdirectory
    Path tmpDir = contentDir.resolve(FileBlobStore.TMP);
    Path tmpSubdir = tmpDir.resolve("subdir");
    Files.createDirectories(tmpSubdir);

    // Create directpath directory with empty subdirectory
    Path directpathDir = contentDir.resolve(DIRECT_PATH_ROOT);
    Path directpathSubdir = directpathDir.resolve("subdir");
    Files.createDirectories(directpathSubdir);

    // Verify directories were created
    assertThat(Files.exists(tmpSubdir), is(true));
    assertThat(Files.exists(directpathSubdir), is(true));

    // Run compact which should trigger empty directory cleanup
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    // Verify special directory roots were preserved (not deleted)
    assertThat(Files.exists(tmpDir), is(true));
    assertThat(Files.exists(directpathDir), is(true));

    // But their empty subdirectories should be cleaned up
    assertThat(Files.exists(tmpSubdir), is(false));
    assertThat(Files.exists(directpathSubdir), is(false));
  }

  @Test
  public void testPruneEmptyContentDirectories_HandlesNonEmptyDirectories() throws Exception {
    // Create nested directories with a file at the deepest level
    Path contentDir = underTest.getContentDir();
    Path level1 = contentDir.resolve("2024/01");
    Path level2 = level1.resolve("15/10");
    Path level3 = level2.resolve("30");
    Files.createDirectories(level3);

    // Add a file in the deepest directory
    Path file = level3.resolve("blob.bytes");
    write(file, "test content".getBytes(UTF_8));

    // Create empty parent directory at same level
    Path emptyLevel1 = contentDir.resolve("2024/02");
    Path emptyLevel2 = emptyLevel1.resolve("20");
    Files.createDirectories(emptyLevel2);

    // Verify everything was created
    assertThat(Files.exists(file), is(true));
    assertThat(Files.exists(level3), is(true));
    assertThat(Files.exists(emptyLevel2), is(true));

    // Run compact which should trigger empty directory cleanup
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    // Verify non-empty directory structure is preserved
    assertThat(Files.exists(file), is(true));
    assertThat(Files.exists(level3), is(true));
    assertThat(Files.exists(level2), is(true));
    assertThat(Files.exists(level1), is(true));

    // Verify empty directories were removed
    assertThat(Files.exists(emptyLevel2), is(false));
    assertThat(Files.exists(emptyLevel1), is(false));
  }

  @Test
  public void testPruneEmptyContentDirectories_BottomUpRecursion() throws Exception {
    // Create a structure where child is deleted first, making parent deletable
    Path contentDir = underTest.getContentDir();
    Path parent = contentDir.resolve("2024/03");
    Path child1 = parent.resolve("10");
    Path child2 = parent.resolve("20");
    Path grandchild = child1.resolve("15");
    Files.createDirectories(grandchild);
    Files.createDirectories(child2);

    // Verify all directories were created
    assertThat(Files.exists(grandchild), is(true));
    assertThat(Files.exists(child1), is(true));
    assertThat(Files.exists(child2), is(true));
    assertThat(Files.exists(parent), is(true));

    // Run compact which should trigger empty directory cleanup
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    // Verify bottom-up deletion: grandchild deleted first, then child, then parent
    assertThat(Files.exists(grandchild), is(false));
    assertThat(Files.exists(child1), is(false));
    assertThat(Files.exists(child2), is(false));
    assertThat(Files.exists(parent), is(false));
  }

  @Test
  public void testPruneEmptyContentDirectories_MultipleEmptyDirectoriesAtSameLevel() throws Exception {
    // Create multiple empty directories at the same level
    Path contentDir = underTest.getContentDir();
    Path dir1 = contentDir.resolve("2024/04/01");
    Path dir2 = contentDir.resolve("2024/04/02");
    Path dir3 = contentDir.resolve("2024/04/03");
    Files.createDirectories(dir1);
    Files.createDirectories(dir2);
    Files.createDirectories(dir3);

    // Verify directories were created
    assertThat(Files.exists(dir1), is(true));
    assertThat(Files.exists(dir2), is(true));
    assertThat(Files.exists(dir3), is(true));

    // Run compact which should trigger empty directory cleanup
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    // Verify all empty directories at the same level were removed
    assertThat(Files.exists(dir1), is(false));
    assertThat(Files.exists(dir2), is(false));
    assertThat(Files.exists(dir3), is(false));
    assertThat(Files.exists(contentDir.resolve("2024/04")), is(false));
  }

  @Test
  public void testPruneEmptyContentDirectories_MixedEmptyAndNonEmptyDirectories() throws Exception {
    // Create a complex structure with both empty and non-empty directories
    Path contentDir = underTest.getContentDir();

    // Empty branch
    Path emptyBranch = contentDir.resolve("2024/05/01/10");
    Files.createDirectories(emptyBranch);

    // Non-empty branch
    Path nonEmptyBranch = contentDir.resolve("2024/05/02/11");
    Files.createDirectories(nonEmptyBranch);
    Path file = nonEmptyBranch.resolve("data.bytes");
    write(file, "content".getBytes(UTF_8));

    // Another empty branch under same parent
    Path anotherEmptyBranch = contentDir.resolve("2024/05/03/12");
    Files.createDirectories(anotherEmptyBranch);

    // Verify setup
    assertThat(Files.exists(emptyBranch), is(true));
    assertThat(Files.exists(file), is(true));
    assertThat(Files.exists(anotherEmptyBranch), is(true));

    // Run compact which should trigger empty directory cleanup
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    // Verify empty branches removed, non-empty preserved
    assertThat(Files.exists(emptyBranch), is(false));
    assertThat(Files.exists(anotherEmptyBranch), is(false));
    assertThat(Files.exists(file), is(true));
    assertThat(Files.exists(nonEmptyBranch), is(true));

    // Shared parent should still exist because it has non-empty children
    assertThat(Files.exists(contentDir.resolve("2024/05")), is(true));
  }

  @Test
  public void testPruneEmptyContentDirectories_ContentDirItselfNotDeleted() throws Exception {
    // Create and delete a single empty subdirectory
    Path contentDir = underTest.getContentDir();
    Path subdir = contentDir.resolve("test-subdir");
    Files.createDirectories(subdir);

    assertThat(Files.exists(subdir), is(true));
    assertThat(Files.exists(contentDir), is(true));

    // Run compact which should trigger empty directory cleanup
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    // Subdirectory should be deleted, but content dir itself should remain
    assertThat(Files.exists(subdir), is(false));
    assertThat(Files.exists(contentDir), is(true));
  }

  @Test
  public void testPruneEmptyContentDirectories_DeepNesting() throws Exception {
    // Create deeply nested empty directory structure (date-based can have up to 5 levels)
    Path contentDir = underTest.getContentDir();
    Path deepPath = contentDir.resolve("2024/06/15/14/45/extra/deeper");
    Files.createDirectories(deepPath);

    assertThat(Files.exists(deepPath), is(true));

    // Run compact which should trigger empty directory cleanup
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    // All levels should be cleaned up
    assertThat(Files.exists(deepPath), is(false));
    assertThat(Files.exists(contentDir.resolve("2024/06/15/14/45/extra")), is(false));
    assertThat(Files.exists(contentDir.resolve("2024/06/15/14/45")), is(false));
    assertThat(Files.exists(contentDir.resolve("2024/06/15/14")), is(false));
  }

  @Test
  public void testPruneEmptyContentDirectories_IgnoresSymlinks() throws Exception {
    Path contentDir = underTest.getContentDir();

    // Create a real empty directory inside content dir
    Path realDir = contentDir.resolve("2024/07/real-dir");
    Files.createDirectories(realDir);

    // Create a directory outside contentDir
    Path outsideDir = Files.createTempDirectory("outside-content");
    Path outsideSubdir = outsideDir.resolve("subdir");
    Files.createDirectories(outsideSubdir);

    // Create a symlink from contentDir pointing to outside directory
    Path symlink = contentDir.resolve("2024/07/symlink-dir");
    Files.createSymbolicLink(symlink, outsideDir);

    // Verify setup
    assertThat(Files.exists(realDir), is(true));
    assertThat(Files.exists(symlink, LinkOption.NOFOLLOW_LINKS), is(true));
    assertThat(Files.isSymbolicLink(symlink), is(true));
    assertThat(Files.exists(outsideDir), is(true));
    assertThat(Files.exists(outsideSubdir), is(true));

    // Run compact which should trigger empty directory cleanup
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    // Verify symlink was NOT followed or deleted (preserved as-is)
    assertThat("Symlink should be preserved", Files.exists(symlink, LinkOption.NOFOLLOW_LINKS), is(true));
    assertThat("Symlink should still be a symlink", Files.isSymbolicLink(symlink), is(true));

    // Verify outside directory was NOT touched (not deleted)
    assertThat("Outside directory should be untouched", Files.exists(outsideDir), is(true));
    assertThat("Outside subdirectory should be untouched", Files.exists(outsideSubdir), is(true));

    // Verify real empty directory was properly deleted
    assertThat("Real empty directory should be deleted", Files.exists(realDir), is(false));

    // Cleanup
    Files.delete(symlink);
    Files.delete(outsideSubdir);
    Files.delete(outsideDir);
  }

  @Test
  public void testPruneEmptyContentDirectories_PathBoundaryValidation() throws Exception {
    Path contentDir = underTest.getContentDir();

    // Create a directory inside content area
    Path insideDir = contentDir.resolve("2024/08/inside");
    Files.createDirectories(insideDir);

    // Create a directory outside content area that we'll try to reference via symlink
    Path outsideDir = Files.createTempDirectory("boundary-test-outside");
    Path outsideNested = outsideDir.resolve("nested/deep");
    Files.createDirectories(outsideNested);

    // Create symlink that escapes content directory
    Path escapingSymlink = contentDir.resolve("2024/08/escaping-link");
    Files.createSymbolicLink(escapingSymlink, outsideDir);

    // Verify setup
    assertThat(Files.exists(insideDir), is(true));
    assertThat(Files.exists(escapingSymlink, LinkOption.NOFOLLOW_LINKS), is(true));
    assertThat(Files.exists(outsideNested), is(true));

    // Run compact which should trigger empty directory cleanup with boundary checks
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    // Verify symlink was skipped (not followed)
    assertThat("Escaping symlink should be preserved",
        Files.exists(escapingSymlink, LinkOption.NOFOLLOW_LINKS), is(true));

    // Verify outside directory structure was NOT touched by cleanup
    assertThat("Outside directory should be untouched", Files.exists(outsideDir), is(true));
    assertThat("Outside nested directory should be untouched", Files.exists(outsideNested), is(true));

    // Verify inside empty directory was properly deleted
    assertThat("Inside empty directory should be deleted", Files.exists(insideDir), is(false));

    // Cleanup
    Files.delete(escapingSymlink);
    Files.delete(outsideNested);
    Files.delete(outsideNested.getParent());
    Files.delete(outsideDir);
  }

  @Test
  public void testPruneEmptyContentDirectories_ThreadSafety() throws Exception {
    // Create empty directories that will be cleaned
    Path contentDir = underTest.getContentDir();
    Path dir1 = contentDir.resolve("2024/09/thread1");
    Path dir2 = contentDir.resolve("2024/09/thread2");
    Path dir3 = contentDir.resolve("2024/09/thread3");
    Files.createDirectories(dir1);
    Files.createDirectories(dir2);
    Files.createDirectories(dir3);

    when(nodeAccess.isOldestNode()).thenReturn(true);

    // Run compact concurrently from multiple threads
    ExecutorService executor = Executors.newFixedThreadPool(3);
    CountDownLatch latch = new CountDownLatch(3);
    AtomicReference<Exception> exception = new AtomicReference<>();

    for (int i = 0; i < 3; i++) {
      executor.submit(() -> {
        try {
          underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));
        }
        catch (Exception e) {
          exception.set(e);
        }
        finally {
          latch.countDown();
        }
      });
    }

    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    // Verify no exceptions occurred during concurrent execution
    assertThat("Concurrent execution should not throw exceptions", exception.get(), is(nullValue()));

    // Verify directories were cleaned up
    assertThat(Files.exists(dir1), is(false));
    assertThat(Files.exists(dir2), is(false));
    assertThat(Files.exists(dir3), is(false));
  }

  @Test
  public void testPruneEmptyContentDirectories_BrokenSymlink() throws Exception {
    Path contentDir = underTest.getContentDir();

    // Create a real empty directory
    Path realDir = contentDir.resolve("2024/10/real-dir");
    Files.createDirectories(realDir);

    // Create a symlink pointing to non-existent target (broken symlink)
    Path brokenSymlink = contentDir.resolve("2024/10/broken-link");
    Path nonExistentTarget = Paths.get("/tmp/does-not-exist-" + UUID.randomUUID());
    Files.createSymbolicLink(brokenSymlink, nonExistentTarget);

    // Verify setup
    assertThat("Broken symlink should exist", Files.exists(brokenSymlink, LinkOption.NOFOLLOW_LINKS), is(true));
    assertThat("Broken symlink is a symlink", Files.isSymbolicLink(brokenSymlink), is(true));
    assertThat("Target should not exist", Files.exists(nonExistentTarget), is(false));

    // Run compact which should handle broken symlink gracefully
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    // Verify broken symlink was preserved (not deleted, not followed)
    assertThat("Broken symlink should be preserved", Files.exists(brokenSymlink, LinkOption.NOFOLLOW_LINKS), is(true));

    // Verify real empty directory was deleted
    assertThat("Real empty directory should be deleted", Files.exists(realDir), is(false));

    // Cleanup
    Files.delete(brokenSymlink);
  }

  @Test
  public void testPruneEmptyContentDirectories_HandlesDirectoryBecomingNonEmpty() throws Exception {
    Path contentDir = underTest.getContentDir();
    Path testDir = contentDir.resolve("2024/11/race-test");
    Files.createDirectories(testDir);

    when(nodeAccess.isOldestNode()).thenReturn(true);

    // Start compact in background
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<?> compactFuture = executor.submit(() -> {
      try {
        underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    // Concurrently add a file to the directory (if it still exists)
    Thread.sleep(50); // Small delay to let compact start
    Path fileInDir = testDir.resolve("concurrent-file.txt");

    // Only write if directory still exists (race condition simulation)
    boolean fileWritten = false;
    if (Files.exists(testDir)) {
      try {
        Files.write(fileInDir, "content".getBytes(UTF_8));
        fileWritten = true;
      }
      catch (Exception e) {
        // Directory was deleted between check and write - this is expected in race condition
      }
    }

    // Wait for compact to complete
    compactFuture.get(5, TimeUnit.SECONDS);
    executor.shutdown();

    // If file was written, directory should still exist; otherwise it should be deleted
    if (fileWritten) {
      assertThat("Directory with file should not be deleted", Files.exists(testDir), is(true));
      assertThat("File should exist", Files.exists(fileInDir), is(true));
      // Cleanup
      Files.delete(fileInDir);
      Files.delete(testDir);
    }
    else {
      // Directory was deleted before file could be written - atomic delete worked correctly
      assertThat("Directory should be deleted if file wasn't written", Files.exists(testDir), is(false));
    }
  }

  @Test
  public void testPruneEmptyContentDirectories_SymlinkChain() throws Exception {
    Path contentDir = underTest.getContentDir();

    // Create real directory and empty subdirectory
    Path realDir = contentDir.resolve("2024/12/real");
    Files.createDirectories(realDir);

    // Create chain: symlink1 -> symlink2 -> outside directory
    Path outsideDir = Files.createTempDirectory("symlink-chain-test");
    Path symlink2 = Files.createTempFile("symlink2-", "");
    Files.delete(symlink2);
    Files.createSymbolicLink(symlink2, outsideDir);

    Path symlink1 = contentDir.resolve("2024/12/symlink-chain");
    Files.createSymbolicLink(symlink1, symlink2);

    // Verify setup
    assertThat("Symlink1 exists", Files.isSymbolicLink(symlink1), is(true));
    assertThat("Symlink2 exists", Files.isSymbolicLink(symlink2), is(true));

    // Run compact
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doCompact(blobStoreUsageChecker, Duration.ofDays(100));

    // Verify symlink chain was not followed
    assertThat("Symlink1 should be preserved", Files.exists(symlink1, LinkOption.NOFOLLOW_LINKS), is(true));
    assertThat("Outside directory should be untouched", Files.exists(outsideDir), is(true));

    // Real empty directory should be deleted
    assertThat("Real directory should be deleted", Files.exists(realDir), is(false));

    // Cleanup
    Files.delete(symlink1);
    Files.delete(symlink2);
    Files.delete(outsideDir);
  }

  @Test
  public void testCreateWithAssignedBlobIdOverridesTimestamp() {
    // Create a BlobId with an old timestamp (2 hours ago)
    OffsetDateTime oldTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2);
    BlobId assignedBlobId = new BlobId("12345678-1234-1234-1234-123456789abc", oldTimestamp);

    // Create blob with assigned BlobId
    Blob blob = underTest.create(new ByteArrayInputStream("test content".getBytes()), TEST_HEADERS, assignedBlobId);

    // Assert that the blob was created
    assertThat(blob, is(notNullValue()));

    // Assert that the UUID string is preserved
    assertThat(blob.getId().asUniqueString(), is("12345678-1234-1234-1234-123456789abc"));

    // Assert that the timestamp was overridden to current time (not the old timestamp)
    assertThat(blob.getId().getBlobCreatedRef(), is(notNullValue()));
    assertThat(blob.getId().getBlobCreatedRef(), is(not(oldTimestamp)));

    // Assert that the new timestamp is recent (within last minute)
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(blob.getId().getBlobCreatedRef().isAfter(now.minusMinutes(1)), is(true));
    assertThat(blob.getId().getBlobCreatedRef().isBefore(now.plusSeconds(5)), is(true));
  }

  @Test
  public void testGet_blobNotInFileStore_returnsNull() throws Exception {
    BlobId blobId = new BlobId(UUID.randomUUID().toString());

    Blob result = underTest.get(blobId, false);

    // Verify null is returned (triggers fallback in FluentAssetImpl)
    assertThat(result, is(nullValue()));
  }

  /**
   * Test that get() works normally when blob exists in file blob store.
   */
  @Test
  public void testGet_blobExistsInFileStore_returnsBlob() throws Exception {
    // Create a real blob
    byte[] content = "test content".getBytes(UTF_8);
    Map<String, String> headers = ImmutableMap.of(
        BLOB_NAME_HEADER, "test",
        CREATED_BY_HEADER, "test");

    Blob createdBlob = underTest.create(new ByteArrayInputStream(content), headers);
    BlobId blobId = createdBlob.getId();

    Blob result = underTest.get(blobId, false);

    // Verify blob is returned (not null)
    assertThat(result, is(notNullValue()));
    assertThat(result.getId(), is(blobId));
    assertThat(result.getInputStream(), is(notNullValue()));
    assertThat(result.getMetrics(), is(notNullValue()));
    assertThat(result.getMetrics().getContentSize(), is((long) content.length));
  }

  /**
   * NEXUS-51514: a single blob deletion failure during compaction must not abort the entire run.
   */
  @Test
  public void testCompact_continuesPastSingleDeleteFailure_NEXUS_51514() throws Exception {
    BlobId blobId1 = new BlobId(UUID.randomUUID().toString());
    BlobId blobId2 = new BlobId(UUID.randomUUID().toString());
    BlobId blobId3 = new BlobId(UUID.randomUUID().toString());

    // Create .bytes files so deleteHard returns true for blob1 and blob3
    Path blob1Bytes = fullPath.resolve(blobId1.asUniqueString() + ".bytes");
    Path blob3Bytes = fullPath.resolve(blobId3.asUniqueString() + ".bytes");
    Files.createFile(blob1Bytes);
    Files.createFile(blob3Bytes);

    // blob2: create files and make the .bytes deletion throw
    Path blob2Bytes = fullPath.resolve(blobId2.asUniqueString() + ".bytes");
    Files.createFile(blob2Bytes);
    doThrow(new IOException("Permission denied")).when(fileOperations).delete(blob2Bytes);

    when(fileBlobDeletionIndex.count(any(OffsetDateTime.class))).thenReturn(3);
    when(fileBlobDeletionIndex.getRecordsBefore(any(OffsetDateTime.class)))
        .thenReturn(Stream.of(blobId1, blobId2, blobId3));

    underTest.compact(null, Duration.ZERO);

    // blob1 and blob3 were hard-deleted successfully
    verify(fileBlobDeletionIndex).deleteRecord(blobId1);
    verify(fileBlobDeletionIndex).deleteRecord(blobId3);

    // blob2 failed — its record stays in the index for retry
    verify(fileBlobDeletionIndex, never()).deleteRecord(blobId2);
  }

  @Test
  public void testCompact_taskCancellationPropagatesDuringBlobDeletion() throws Exception {
    BlobId blobId = new BlobId(UUID.randomUUID().toString());

    when(fileBlobDeletionIndex.count(any(OffsetDateTime.class))).thenReturn(1);
    when(fileBlobDeletionIndex.getRecordsBefore(any(OffsetDateTime.class)))
        .thenReturn(Stream.of(blobId));

    cancelled.set(true);

    assertThrows(TaskInterruptedException.class,
        () -> underTest.compact(null, Duration.ZERO));
  }

}
