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
package org.sonatype.nexus.blobstore.restore.helm.internal.orient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.orient.HelmRestoreFacet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;

/**
 * @since 3.28
 */
public class OrientHelmRestoreBlobStrategyTest
    extends TestSupport
{
  private static final String TEST_BLOB_STORE_NAME = "test";

  private static final String ARCHIVE_PATH = "src/contrib/mongodb.tgz";

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  Repository repository;

  @Mock
  StorageFacet storageFacet;

  @Mock
  HelmRestoreFacet helmRestoreFacet;

  @Mock
  HelmRestoreBlobData helmRestoreBlobData;

  @Mock
  private RestoreBlobData restoreBlobData;

  @Mock
  Blob blob;

  @Mock
  BlobStore blobStore;

  @Mock
  BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  StorageTx storageTx;

  private byte[] blobBytes = "blobbytes".getBytes();

  private Properties properties = new Properties();

  private OrientHelmRestoreBlobStrategy restoreBlobStrategy;

  @Before
  public void setup() throws IOException {
    restoreBlobStrategy = new OrientHelmRestoreBlobStrategy(nodeAccess, repositoryManager, blobStoreManager, new DryRunPrefix("dryrun"));

    when(repositoryManager.get(anyString())).thenReturn(repository);
    when(repository.facet(HelmRestoreFacet.class)).thenReturn(helmRestoreFacet);
    when(repository.optionalFacet(HelmRestoreFacet.class)).thenReturn(Optional.of(helmRestoreFacet));
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.of(storageFacet));
    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream(blobBytes));
    when(helmRestoreBlobData.getBlobData()).thenReturn(restoreBlobData);
    when(restoreBlobData.getBlobName()).thenReturn(ARCHIVE_PATH);
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(restoreBlobData.getBlob()).thenReturn(blob);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(restoreBlobData.getRepository()).thenReturn(repository);

    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME);

    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);

    properties.setProperty("@BlobStore.created-by", "anonymous");
    properties.setProperty("size", "1330");
    properties.setProperty("@Bucket.repo-name", "helm-proxy");
    properties.setProperty("creationTime", "1533220387218");
    properties.setProperty("@BlobStore.created-by-ip", "127.0.0.1");
    properties.setProperty("@BlobStore.content-type", "text/html");
    properties.setProperty("@BlobStore.blob-name", ARCHIVE_PATH);
    properties.setProperty("sha1", "0088eb478752a810f48f04d3cf9f46d2924e334a");
  }

  @Test
  public void testBlobDataIsCreated() {
    assertThat(restoreBlobStrategy.createRestoreData(restoreBlobData).getBlobData(), is(restoreBlobData));
  }

  @Test(expected = IllegalStateException.class)
  public void testIfBlobDataNameIsEmptyExceptionIsThrown() {
    when(helmRestoreBlobData.getBlobData().getBlobName()).thenReturn("");
    restoreBlobStrategy.createRestoreData(restoreBlobData);
  }

  @Test
  public void testCorrectHashAlgorithmsAreSupported() {
    assertThat(restoreBlobStrategy.getHashAlgorithms(), containsInAnyOrder(SHA1, SHA256));
  }

  @Test
  public void testAppropriatePathIsReturned() {
    assertThat(restoreBlobStrategy.getAssetPath(helmRestoreBlobData), is(ARCHIVE_PATH));
  }

  @Test
  public void testPackageIsRestored() throws Exception {
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(helmRestoreFacet).assetExists(ARCHIVE_PATH);
    verify(helmRestoreFacet).restore(any(AssetBlob.class), eq(ARCHIVE_PATH));
    verifyNoMoreInteractions(helmRestoreFacet);
  }

  @Test
  public void testRestoreIsSkipIfPackageExists() {
    when(helmRestoreFacet.assetExists(ARCHIVE_PATH)).thenReturn(true);
    restoreBlobStrategy.restore(properties, blob, blobStore, false);

    verify(helmRestoreFacet).assetExists(ARCHIVE_PATH);
    verify(helmRestoreFacet).componentRequired(ARCHIVE_PATH);
    verifyNoMoreInteractions(helmRestoreFacet);
  }

  @Test
  public void testComponentIsRequiredForGz() {
    boolean expected = true;
    when(helmRestoreFacet.componentRequired(ARCHIVE_PATH)).thenReturn(expected);
    assertThat(restoreBlobStrategy.componentRequired(helmRestoreBlobData), is(expected));
    verify(helmRestoreFacet).componentRequired(ARCHIVE_PATH);
    verifyNoMoreInteractions(helmRestoreFacet);
  }

  @Test
  public void testComponentQuery() throws IOException
  {
    restoreBlobStrategy.getComponentQuery(helmRestoreBlobData);
    verify(helmRestoreFacet, times(1)).getComponentQuery(any(HelmAttributes.class));
  }
}
