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
package org.sonatype.nexus.blobstore.restore.raw.internal;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.raw.RawContentFacet;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.google.common.hash.HashCode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

public class RawRestoreBlobStrategyTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "theRepository";

  private static final String BLOB_STORE_NAME = "theBlobStore";

  private static final String BLOB_NAME = "theBlob";

  private static final String CONTENT_TYPE = "theContentType";

  private static final boolean DRY_RUN = true;

  private static final DryRunPrefix DRY_RUN_PREFIX = new DryRunPrefix("DRY RUN");

  private static final boolean EXISTS = true;

  private static final byte[] BLOB_BYTES = "blobBytes".getBytes();

  private static final AttributesMap NO_CONTENT_ATTRIBUTES = null;

  private Properties properties = new Properties();

  @Mock
  private Blob blob;

  @Mock
  private BlobId blobId;

  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private RawContentFacet rawContentFacet;

  @Mock
  private StorageTx storageTx;

  @Mock
  private BlobStore blobStore;

  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private BlobStoreManager blobStoreManager;

  private RawRestoreBlobStrategy underTest;

  @Before
  public void setup() throws Exception {
    properties.setProperty("@Bucket.repo-name", REPOSITORY_NAME);
    properties.setProperty("@BlobStore.blob-name", BLOB_NAME);
    properties.setProperty("@BlobStore.content-type", CONTENT_TYPE);

    when(blob.getId()).thenReturn(blobId);
    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream(BLOB_BYTES));

    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.of(storageFacet));
    when(repository.optionalFacet(RawContentFacet.class)).thenReturn(Optional.of(rawContentFacet));
    when(repository.facet(RawContentFacet.class)).thenReturn(rawContentFacet);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);

    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);

    when(rawContentFacet.assetExists(BLOB_NAME)).thenReturn(!EXISTS);

    when(blobStoreManager.get(BLOB_STORE_NAME)).thenReturn(blobStore);

    underTest = new RawRestoreBlobStrategy(nodeAccess, repositoryManager, blobStoreManager, DRY_RUN_PREFIX);
  }

  @Test
  public void testRestoreWhenNoStorageFacet() {
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.empty());

    underTest.restore(properties, blob, BLOB_STORE_NAME, !DRY_RUN);
    verifyNoMoreInteractions(rawContentFacet);
  }

  @Test
  public void testRestoreWhenNoRawContentFacet() {
    when(repository.optionalFacet(RawContentFacet.class)).thenReturn(Optional.empty());

    underTest.restore(properties, blob, BLOB_STORE_NAME, !DRY_RUN);
    verifyNoMoreInteractions(rawContentFacet);
  }

  @Test
  public void testRestoreWhenAssetExists() {
    when(rawContentFacet.assetExists(BLOB_NAME)).thenReturn(EXISTS);

    underTest.restore(properties, blob, BLOB_STORE_NAME, !DRY_RUN);
    verify(rawContentFacet).assetExists(BLOB_NAME);
    verifyNoMoreInteractions(rawContentFacet);
  }

  @Test
  public void testRestoreWhenDryRun() {
    underTest.restore(properties, blob, BLOB_STORE_NAME, DRY_RUN);
    verify(rawContentFacet).assetExists(BLOB_NAME);
    verifyNoMoreInteractions(rawContentFacet);
  }

  @Test
  public void testRestoreCreatesAssetFromBlobWithExpectedHashes() throws Exception {
    ArgumentCaptor<AssetBlob> assetBlobCaptor = ArgumentCaptor.forClass(AssetBlob.class);

    Map<HashAlgorithm, HashCode> expectedHashes = Stream.of(SHA1, MD5)
        .collect(toMap(identity(), algorithm -> algorithm.function().hashBytes(BLOB_BYTES)));

    underTest.restore(properties, blob, BLOB_STORE_NAME, !DRY_RUN);

    verify(rawContentFacet).put(eq(BLOB_NAME), assetBlobCaptor.capture(), eq(NO_CONTENT_ATTRIBUTES));
    assertThat(assetBlobCaptor.getValue().getHashes(), equalTo(expectedHashes));
    assertThat(assetBlobCaptor.getValue().getContentType(), equalTo(CONTENT_TYPE));
  }
}
