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

import java.nio.file.FileSystemException;
import java.nio.file.Path;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.file.FileBlobStore.FileBlob;
import org.sonatype.nexus.blobstore.file.internal.BlobStoreMetricsStore;
import org.sonatype.nexus.common.app.ApplicationDirectories;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
  private LocationStrategy locationStrategy;

  @Mock
  private FileOperations fileOperations;

  @Mock
  private ApplicationDirectories appDirs;

  @Mock
  private BlobStoreMetricsStore metrics;

  @Mock
  private LoadingCache loadingCache;

  public static final ImmutableMap<String, String> TEST_HEADERS = ImmutableMap.of(
      CREATED_BY_HEADER, "test",
      BLOB_NAME_HEADER, "test/randomData.bin"
  );

  private FileBlobStore underTest;

  @Before
  public void initBlobStore() {
    when(appDirs.getWorkDirectory(any())).thenReturn(util.createTempDir());


    underTest = new FileBlobStore(util.createTempDir().toPath(),
        locationStrategy, fileOperations, metrics, new BlobStoreConfiguration(),
        appDirs);
    when(loadingCache.getUnchecked(any())).thenReturn(underTest.new FileBlob(new BlobId("fakeid")));
    underTest.setLiveBlobs(loadingCache);
  }

  @Test(expected = BlobStoreException.class)
  public void impossibleHardLinkThrowsBlobStoreException() throws Exception {

    Path path = util.createTempFile().toPath();

    doThrow(new FileSystemException(null)).when(fileOperations).hardLink(any(), any());

    underTest.create(path, TEST_HEADERS);
  }
}