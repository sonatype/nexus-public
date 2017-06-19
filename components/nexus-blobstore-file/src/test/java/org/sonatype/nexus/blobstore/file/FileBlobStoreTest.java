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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.LocationStrategy;
import org.sonatype.nexus.blobstore.TemporaryLocationStrategy;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.file.internal.BlobStoreMetricsStore;
import org.sonatype.nexus.blobstore.file.internal.FileOperations;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.property.PropertiesFile;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.squareup.tape.QueueFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static java.nio.file.Files.write;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER;

/**
 * Tests {@link FileBlobStore}.
 */
public class FileBlobStoreTest
    extends TestSupport
{
  @Mock
  private LocationStrategy permanentLocationStrategy;

  @Mock
  private TemporaryLocationStrategy temporaryLocationStrategy;

  @Mock
  private FileOperations fileOperations;

  @Mock
  private ApplicationDirectories appDirs;

  @Mock
  private BlobStoreMetricsStore metrics;

  @Mock
  private LoadingCache loadingCache;

  @Mock
  NodeAccess nodeAccess;

  public static final ImmutableMap<String, String> TEST_HEADERS = ImmutableMap.of(
      CREATED_BY_HEADER, "test",
      BLOB_NAME_HEADER, "test/randomData.bin"
  );

  private FileBlobStore underTest;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();


  @Before
  public void initBlobStore() {
    when(nodeAccess.getId()).thenReturn("test");
    when(appDirs.getWorkDirectory(any())).thenReturn(util.createTempDir());

    BlobStoreConfiguration configuration = new BlobStoreConfiguration();

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> fileMap = new HashMap<>();
    fileMap.put("path", temporaryFolder.getRoot().toPath());
    attributes.put("file", fileMap);

    configuration.setAttributes(attributes);

    underTest = new FileBlobStore(util.createTempDir().toPath(),
        permanentLocationStrategy, temporaryLocationStrategy, fileOperations, metrics, configuration,
        appDirs, nodeAccess);

    when(loadingCache.getUnchecked(any())).thenReturn(underTest.new FileBlob(new BlobId("fakeid")));

    underTest.init(configuration);
    underTest.setLiveBlobs(loadingCache);
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

    write(underTest.getAbsoluteBlobDir().resolve("content").resolve("test-blob.properties"),
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

    write(underTest.getAbsoluteBlobDir().resolve("content").resolve("test-blob.properties"),
        deletedBlobStoreProperties);

    checkDeletionsIndex(true);

    setRebuildMetadataToTrue();

    underTest.maybeRebuildDeletedBlobIndex();

    checkDeletionsIndex(true);
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
}
