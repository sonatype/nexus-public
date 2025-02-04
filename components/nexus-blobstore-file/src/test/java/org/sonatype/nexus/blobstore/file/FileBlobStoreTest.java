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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.BlobIdLocationResolver;
import org.sonatype.nexus.blobstore.BlobStoreReconciliationLogger;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.file.FileBlobStore.FileBlob;
import org.sonatype.nexus.blobstore.file.internal.FileOperations;
import org.sonatype.nexus.blobstore.file.internal.datastore.metrics.DatastoreFileBlobStoreMetricsService;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaUsageChecker;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.squareup.tape.QueueFile;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
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
public class FileBlobStoreTest
    extends TestSupport
{
  private static final byte[] VALID_BLOB_STORE_PROPERTIES = ("@BlobStore.created-by = admin\n" +
      "size = 40\n" +
      "@Bucket.repo-name = maven-releases\n" +
      "creationTime = 1486679665325\n" +
      "@BlobStore.blob-name = com/sonatype/training/nxs301/03-implicit-staging/maven-metadata.xml.sha1\n" +
      "@BlobStore.content-type = text/plain\n" +
      "sha1 = cbd5bce1c926e6b55b6b4037ce691b8f9e5dea0f").getBytes(StandardCharsets.ISO_8859_1);

  private static final byte[] EMPTY_BLOB_STORE_PROPERTIES = ("").getBytes(StandardCharsets.ISO_8859_1);

  private static final String RECONCILIATION = "reconciliation";

  private AtomicBoolean cancelled = new AtomicBoolean(false);

  @Mock
  private BlobIdLocationResolver blobIdLocationResolver;

  @Mock
  private FileOperations fileOperations;

  @Mock
  private ApplicationDirectories appDirs;

  @Mock
  private DatastoreFileBlobStoreMetricsService metrics;

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
  public void initBlobStore() throws IOException {
    CancelableHelper.set(cancelled);
    when(nodeAccess.getId()).thenReturn("test");
    when(dryRunPrefix.get()).thenReturn("");
    when(appDirs.getWorkDirectory(any())).thenReturn(util.createTempDir());
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

    underTest = new FileBlobStore(util.createTempDir().toPath(), blobIdLocationResolver, fileOperations, metrics,
        configuration, appDirs, nodeAccess, dryRunPrefix, reconciliationLogger, 0L, blobStoreQuotaUsageChecker,
        fileBlobDeletionIndex);

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
      return fullPath.resolve(blobId.asUniqueString()).toString();
    });
    when(blobIdLocationResolver.fromHeaders(any(Map.class)))
        .thenAnswer(invocation -> new BlobId(UUID.randomUUID().toString()));

  }

  @After
  public void tearDown() {
    CancelableHelper.remove();
  }

  @Test(expected = BlobStoreException.class)
  public void impossibleHardLinkThrowsBlobStoreException() throws Exception {

    Path path = util.createTempFile().toPath();

    doThrow(new FileSystemException(null)).when(fileOperations).hardLink(any(), any());

    underTest.create(path, TEST_HEADERS, 0, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    verifyNoInteractions(reconciliationLogger);
  }

  @Test
  public void hardLinkWithPrecalculatedInformation() throws Exception {

    long size = 100L;
    HashCode sha1 = HashCode.fromString("356a192b7913b04c54574d18c28d46e6395428ab");

    Path path = util.createTempFile().toPath();

    Blob blob = underTest.create(path, TEST_HEADERS, size, sha1);

    assertThat(blob.getMetrics().getContentSize(), is(size));
    assertThat(blob.getMetrics().getSha1Hash(), is("356a192b7913b04c54574d18c28d46e6395428ab"));
    verify(reconciliationLogger).logBlobCreated(eq(underTest.getAbsoluteBlobDir().resolve(RECONCILIATION)), any());
  }

  @Test
  public void blobIdCollisionCausesRetry() throws Exception {

    long size = 100L;
    HashCode sha1 = HashCode.fromString("356a192b7913b04c54574d18c28d46e6395428ab");

    Path path = util.createTempFile().toPath();

    when(fileOperations.exists(any())).thenReturn(true, true, true, false);

    underTest.create(path, TEST_HEADERS, size, sha1);

    verify(fileOperations, times(4)).exists(any());
    verify(reconciliationLogger, times(1))
        .logBlobCreated(eq(underTest.getAbsoluteBlobDir().resolve(RECONCILIATION)), any());
  }

  @Test
  public void blobIdCollisionThrowsExceptionOnRetryLimit() throws Exception {

    long size = 100L;
    HashCode sha1 = HashCode.fromString("356a192b7913b04c54574d18c28d46e6395428ab");

    Path path = util.createTempFile().toPath();

    when(fileOperations.exists(any())).thenReturn(true);

    try {
      underTest.create(path, TEST_HEADERS, size, sha1);
      fail("Expected BlobStoreException");
    }
    catch (BlobStoreException e) {
      verify(fileOperations, times(FileBlobStore.MAX_COLLISION_RETRIES + 1)).exists(any());
    }
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
    when(fileOperations.delete(any())).thenReturn(true);
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doStart();

    write(fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.properties"),
        deletedBlobStoreProperties);

    checkDeletionsIndex(true);
    setRebuildMetadataToTrue();

    underTest.doCompact(blobStoreUsageChecker);

    checkDeletionsIndex(true);

    verify(blobStoreUsageChecker, atLeastOnce()).test(any(), any(), any());
  }

  @Test
  public void testDoCompact_RebuildMetadataNeeded_NotOldestNode() throws Exception {
    when(nodeAccess.isOldestNode()).thenReturn(false);
    underTest.doStart();

    checkDeletionsIndex(true);
    setRebuildMetadataToTrue();

    underTest.doCompact(blobStoreUsageChecker);

    checkDeletionsIndex(true);

    verify(blobStoreUsageChecker, never()).test(any(), any(), any());
  }

  @Test
  public void testDoCompact_clearsDirectPathEmptyDirectories() throws Exception {
    when(fileOperations.delete(any())).thenReturn(true);
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doStart();

    Path fileInSubdir1 = directFullPath.resolve("subdir").resolve("somefile.txt");
    fileInSubdir1.toFile().getParentFile().mkdirs();
    write(fileInSubdir1, "somefile".getBytes(UTF_8));

    Path subdir2 = directFullPath.resolve("subdir2");
    subdir2.toFile().mkdirs();

    assertThat(fileInSubdir1.toFile().exists(), is(true));
    assertThat(subdir2.toFile().exists(), is(true));

    underTest.doCompact(blobStoreUsageChecker);

    assertThat(fileInSubdir1.toFile().exists(), is(true));
    assertThat(subdir2.toFile().exists(), is(false));
  }

  @Test
  public void testDoDeleteHard() throws Exception {
    underTest.doStart();

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
  public void testUndelete_AttributesNotDeleted() throws IOException {
    when(attributes.isDeleted()).thenReturn(false);

    boolean result = underTest.undelete(blobStoreUsageChecker, new BlobId("fakeid"), attributes, false);
    assertThat(result, is(false));
    verify(blobStoreUsageChecker, never()).test(eq(underTest), any(BlobId.class), anyString());
  }

  @Test
  public void testUndelete_CheckerNull() throws IOException {
    boolean result = underTest.undelete(null, new BlobId("fakeid"), attributes, false);
    assertThat(result, is(false));
  }

  @Test
  public void testUndelete_CheckInUse() throws IOException {
    when(blobStoreUsageChecker.test(eq(underTest), any(BlobId.class), anyString())).thenReturn(true);

    boolean result = underTest.undelete(blobStoreUsageChecker, new BlobId("fakeid"), attributes, false);
    assertThat(result, is(true));
    verify(attributes).setDeleted(false);
    verify(attributes).setDeletedReason(null);
    verify(attributes).store();
  }

  @Test
  public void testUndelete_CheckInUse_DryRun() throws IOException {
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

  private void checkDeletionsIndex(boolean expectEmpty) throws IOException {
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
    underTest.doStart();

    write(fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.properties"),
        deletedBlobStorePropertiesNoBlobName);

    setRebuildMetadataToTrue();

    underTest.compact(null);

    verify(fileOperations, times(2)).delete(any());
  }

  @Test
  public void testCompactIsCancelable() throws Exception {
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doStart();

    write(fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.properties"),
        deletedBlobStoreProperties);

    setRebuildMetadataToTrue();
    cancelled.set(true);

    try {
      underTest.compact(null);
      fail("Expected exception to be thrown");
    }
    catch (TaskInterruptedException expected) {
    }

    verify(fileOperations, never()).delete(any());
  }

  @Test
  public void testDeleteWithCorruptAttributes() throws Exception {
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doStart();

    Path bytesPath = fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.bytes");
    Path propertiesPath = fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.properties");
    write(propertiesPath, EMPTY_BLOB_STORE_PROPERTIES);

    underTest.delete(new BlobId("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7"), "deleting");

    verify(fileOperations).delete(propertiesPath);
    verify(fileOperations).delete(bytesPath);
  }

  /**
   * This test guarantees we are returning unix-style paths for {@link BlobId}s returned by
   * {@link FileBlobStore#getDirectPathBlobIdStream(String)}.
   * This test would fail on Windows if {@link FileBlobStore#toBlobName(Path)} wasn't implemented correctly.
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
  public void testIsBlobEmpty() throws Exception {
    Path bytesPath = fullPath.resolve("test-blob.bytes");
    write(bytesPath, "some bytes content".getBytes());
    when(fileOperations.isBlobZeroLength(bytesPath)).thenReturn(true);

    assertThat(bytesPath.toFile().exists(), is(true));

    assertThat(underTest.isBlobEmpty(new BlobId("test-blob")), is(true));
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

  @Test
  public void testGetBlobIdUpdatedSinceStream() {
    OffsetDateTime fromDateTime = LocalDate.now().atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
    OffsetDateTime toDateTime = LocalDate.now().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

    ZoneId systemZone = ZoneId.systemDefault();
    LocalDateTime fromSystemTime = fromDateTime.atZoneSameInstant(systemZone).toLocalDateTime();
    LocalDateTime toSystemTime = toDateTime.atZoneSameInstant(systemZone).toLocalDateTime();

    underTest.getBlobIdUpdatedSinceStream("test", fromDateTime, toDateTime, null, 10);
    verify(reconciliationLogger, times(1)).getBlobsCreatedSince(any(),
        eq(fromSystemTime), eq(toSystemTime), anyMap());
  }

  @Test
  public void testDoDeleteWithDateBasedLayoutEnabled() throws Exception {

    TestFileBlobStore underTest = createFixture();

    BlobId blobId = new BlobId("test-blob-id");
    Path path = underTest.attributePath(new BlobId(blobId.asUniqueString(), UTC.now()));
    when(underTest.isDateBasedLayoutEnabled()).thenReturn(true);
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
  public void testDoDeleteWithDateBasedLayoutDisabled() throws Exception {

    TestFileBlobStore underTest = createFixture();

    BlobId blobId = new BlobId("test-blob-id");
    when(underTest.isDateBasedLayoutEnabled()).thenReturn(false);
    when(attributes.isDeleted()).thenReturn(false);
    when(underTest.getFileBlobAttributes(blobId)).thenReturn(attributes);

    boolean result = underTest.doDelete(blobId, "test-reason");

    assertTrue(result);
    assertAttributes(blobId);
    verify(attributes, never()).setDeletedDateTime(any());
    verifyNoInteractions(newBlobAttributes);
  }

  private TestFileBlobStore createFixture() {
    BlobStoreConfiguration configuration = new MockBlobStoreConfiguration();

    Map<String, Map<String, Object>> attributes1 = new HashMap<>();
    Map<String, Object> fileMap = new HashMap<>();
    fileMap.put("path", temporaryFolder.getRoot().toPath());
    attributes1.put("file", fileMap);

    configuration.setAttributes(attributes1);

    TestFileBlobStore underTest = spy(new TestFileBlobStore(
        util.createTempDir().toPath(), blobIdLocationResolver, fileOperations, metrics, configuration, appDirs,
        nodeAccess, dryRunPrefix, reconciliationLogger, 0L, blobStoreQuotaUsageChecker, fileBlobDeletionIndex));

    underTest.init(configuration);
    underTest.setLiveBlobs(loadingCache);
    return underTest;
  }

  private void assertAttributes(final BlobId blobId) throws Exception {
    verify(attributes).setDeleted(true);
    verify(attributes).setDeletedReason("test-reason");
    verify(attributes).store();
    verify(fileBlobDeletionIndex).createRecord(blobId);
  }

  // test class to provide isDateBasedLayoutEnabled() method
  private class TestFileBlobStore
      extends FileBlobStore
  {
    public TestFileBlobStore(
        Path root,
        BlobIdLocationResolver blobIdLocationResolver,
        FileOperations fileOperations,
        DatastoreFileBlobStoreMetricsService metrics,
        BlobStoreConfiguration configuration,
        ApplicationDirectories appDirs,
        NodeAccess nodeAccess,
        DryRunPrefix dryRunPrefix,
        BlobStoreReconciliationLogger reconciliationLogger,
        long blobStoreQuota,
        BlobStoreQuotaUsageChecker blobStoreQuotaUsageChecker,
        FileBlobDeletionIndex fileBlobDeletionIndex)
    {
      super(root, blobIdLocationResolver, fileOperations, metrics, configuration, appDirs, nodeAccess, dryRunPrefix,
          reconciliationLogger, blobStoreQuota, blobStoreQuotaUsageChecker, fileBlobDeletionIndex);
    }

    @Override
    public boolean isDateBasedLayoutEnabled() {
      return false;
    }
  }

}
