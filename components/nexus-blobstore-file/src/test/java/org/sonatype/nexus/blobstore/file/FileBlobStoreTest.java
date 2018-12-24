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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.BlobIdLocationResolver;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.file.internal.FileBlobStoreMetricsStore;
import org.sonatype.nexus.blobstore.file.internal.FileOperations;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.squareup.tape.QueueFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static java.nio.file.Files.write;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
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

  private AtomicBoolean cancelled = new AtomicBoolean(false);

  @Mock
  private BlobIdLocationResolver blobIdLocationResolver;

  @Mock
  private FileOperations fileOperations;

  @Mock
  private ApplicationDirectories appDirs;

  @Mock
  private FileBlobStoreMetricsStore metrics;

  @Mock
  private LoadingCache loadingCache;

  @Mock
  private BlobStoreUsageChecker blobStoreUsageChecker;

  @Mock
  private FileBlobAttributes attributes;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  DryRunPrefix dryRunPrefix;

  public static final ImmutableMap<String, String> TEST_HEADERS = ImmutableMap.of(
      CREATED_BY_HEADER, "test",
      BLOB_NAME_HEADER, "test/randomData.bin"
  );

  private FileBlobStore underTest;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Path fullPath;


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

    BlobStoreConfiguration configuration = new BlobStoreConfiguration();

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> fileMap = new HashMap<>();
    fileMap.put("path", temporaryFolder.getRoot().toPath());
    attributes.put("file", fileMap);

    configuration.setAttributes(attributes);

    underTest = new FileBlobStore(util.createTempDir().toPath(),
        blobIdLocationResolver, fileOperations, metrics, configuration,
        appDirs, nodeAccess, dryRunPrefix);

    when(loadingCache.getUnchecked(any())).thenReturn(underTest.new FileBlob(new BlobId("fakeid")));

    underTest.init(configuration);
    underTest.setLiveBlobs(loadingCache);

    fullPath = underTest.getAbsoluteBlobDir()
        .resolve("content").resolve("vol-03").resolve("chap-44");
    Files.createDirectories(fullPath);

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
  }

  @Test
  public void hardLinkWithPrecalculatedInformation() throws Exception {

    long size = 100L;
    HashCode sha1 = HashCode.fromString("356a192b7913b04c54574d18c28d46e6395428ab");

    Path path = util.createTempFile().toPath();

    Blob blob = underTest.create(path, TEST_HEADERS, size, sha1);

    assertThat(blob.getMetrics().getContentSize(), is(size));
    assertThat(blob.getMetrics().getSha1Hash(), is("356a192b7913b04c54574d18c28d46e6395428ab"));
  }

  @Test
  public void blobIdCollisionCausesRetry() throws Exception {

    long size = 100L;
    HashCode sha1 = HashCode.fromString("356a192b7913b04c54574d18c28d46e6395428ab");

    Path path = util.createTempFile().toPath();

    when(fileOperations.exists(any())).thenReturn(true, true, true, false);

    underTest.create(path, TEST_HEADERS, size, sha1);

    verify(fileOperations, times(4)).exists(any());
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
  public void testMaybeRebuildDeletedBlobIndex() throws Exception {
    when(nodeAccess.isOldestNode()).thenReturn(true);
    underTest.doStart();

    write(fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.properties"),
        deletedBlobStoreProperties);

    checkDeletionsIndex(true);

    setRebuildMetadataToTrue();

    underTest.maybeRebuildDeletedBlobIndex();

    checkDeletionsIndex(false);
  }


  @Test
  public void testMaybeRebuildDeletedBlobIndex_NotOldest() throws Exception {
    when(nodeAccess.isOldestNode()).thenReturn(false);
    underTest.doStart();

    write(fullPath.resolve("e27f83a9-dc18-4818-b4ca-ae8a9cb813c7.properties"),
        deletedBlobStoreProperties);

    checkDeletionsIndex(true);

    setRebuildMetadataToTrue();

    underTest.maybeRebuildDeletedBlobIndex();

    checkDeletionsIndex(true);
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

    underTest.compact();

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
      underTest.compact();
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
    Path absolute = underTest.getContentDir().resolve(DIRECT_PATH_ROOT).resolve("some/direct/path/file.properties.properties");
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
    Path propertiesPath = underTest.getAbsoluteBlobDir().resolve("content").resolve("test-blob.properties");
    write(propertiesPath, EMPTY_BLOB_STORE_PROPERTIES);

    assertNull(underTest.getBlobAttributes(new BlobId("test-blob")));
  }
}
