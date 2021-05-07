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
package org.sonatype.nexus.blobstore.restore.pypi.internal.orient;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.Properties;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.pypi.orient.PyPiFacet;
import org.sonatype.nexus.repository.pypi.orient.repair.OrientPyPiRepairIndexComponent;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

@RunWith(PowerMockRunner.class)
@PrepareForTest(UnitOfWork.class)
public class OrientPyPiRestoreBlobStrategyTest
{
  private static final String TEST_BLOB_STORE_NAME = "test";

  public static final String PACKAGE_PATH = "packages/sampleproject/1.2.0/sampleproject-1.2.0.tar.gz";

  public static final String INDEX_PATH = "simple/peppercorn/";

  OrientPyPiRestoreBlobStrategy underTest;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  Blob blob;

  @Mock
  BlobAttributes blobAttributes;

  @Mock
  BlobMetrics blobMetrics;

  @Mock
  Component component;

  @Mock
  EntityMetadata entityMetadata;

  @Mock
  EntityId entityId;

  @Mock
  Asset asset;

  @Mock
  Query query;

  @Mock
  Repository repository;

  @Mock
  StorageFacet storageFacet;

  @Mock
  PyPiFacet pyPiFacet;

  @Mock
  StorageTx storageTx;

  @Mock
  Bucket bucket;

  @Mock
  BlobStore blobStore;

  @Mock
  BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  OrientPyPiRepairIndexComponent pyPiRepairIndexComponent;

  @Mock
  PyPiRestoreBlobDataFactory pyPiRestoreBlobDataFactory;

  @Mock
  PyPiRestoreBlobData pyPiRestoreBlobData;

  @Mock
  private RestoreBlobData restoreBlobData;

  Properties packageProps = new Properties();

  Properties indexProps = new Properties();

  byte[] blobBytes = "blobbytes".getBytes();

  @Before
  public void setup() {
    underTest = new OrientPyPiRestoreBlobStrategy(nodeAccess,
        repositoryManager,
        blobStoreManager,
        new DryRunPrefix("dryrun"),
        pyPiRepairIndexComponent,
        pyPiRestoreBlobDataFactory);

    packageProps.setProperty("@BlobStore.created-by", "admin");
    packageProps.setProperty("size", "5674");
    packageProps.setProperty("@Bucket.repo-name", "pypi-hosted");
    packageProps.setProperty("creationTime", "1533220056556");
    packageProps.setProperty("@BlobStore.created-by-ip", "127.0.0.1");
    packageProps.setProperty("@BlobStore.content-type", "application/x-gzip");
    packageProps.setProperty("@BlobStore.blob-name", PACKAGE_PATH);
    packageProps.setProperty("sha1", "c402bbe79807576de00c1fa61fa037a27ee6b62b");

    indexProps.setProperty("@BlobStore.created-by", "anonymous");
    indexProps.setProperty("size", "1330");
    indexProps.setProperty("@Bucket.repo-name", "pypi-proxy");
    indexProps.setProperty("creationTime", "1533220387218");
    indexProps.setProperty("@BlobStore.created-by-ip", "127.0.0.1");
    indexProps.setProperty("@BlobStore.content-type", "text/html");
    indexProps.setProperty("@BlobStore.blob-name", INDEX_PATH);
    indexProps.setProperty("sha1", "0088eb478752a810f48f04d3cf9f46d2924e334a");

    when(repositoryManager.get(anyString())).thenReturn(repository);

    when(repository.facet(PyPiFacet.class)).thenReturn(pyPiFacet);
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.of(storageFacet));
    when(repository.optionalFacet(PyPiFacet.class)).thenReturn(Optional.of(pyPiFacet));

    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(storageFacet.blobStore()).thenReturn(blobStore);

    when(storageTx.findBucket(repository)).thenReturn(bucket);
    when(storageTx.findComponents(any(Query.class), any(Iterable.class))).thenReturn(ImmutableList.of(component));
    when(storageTx.findAssetWithProperty(eq(P_NAME), eq(PACKAGE_PATH), any(Bucket.class))).thenReturn(asset);

    when(component.getEntityMetadata()).thenReturn(entityMetadata);

    when(entityMetadata.getId()).thenReturn(entityId);

    when(asset.componentId()).thenReturn(entityId);

    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream(blobBytes));
    when(blob.getMetrics()).thenReturn(blobMetrics);

    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME);

    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStore.getBlobAttributes(any(BlobId.class))).thenReturn(blobAttributes);

    when(blobAttributes.isDeleted()).thenReturn(false);

    mockStatic(UnitOfWork.class);
    when(UnitOfWork.currentTx()).thenReturn(storageTx);

    when(restoreBlobData.getBlob()).thenReturn(blob);
    when(restoreBlobData.getRepository()).thenReturn(repository);

    when(pyPiRestoreBlobDataFactory.create(any())).thenReturn(pyPiRestoreBlobData);
    when(pyPiRestoreBlobData.getBlobData()).thenReturn(restoreBlobData);
    when(pyPiRestoreBlobData.getVersion()).thenReturn("1.2.0");
  }

  @Test
  public void testPackageRestore() throws Exception {
    when(pyPiRestoreBlobData.getBlobData().getBlobName()).thenReturn(PACKAGE_PATH);

    underTest.restore(packageProps, blob, blobStore, false);

    verify(pyPiFacet).assetExists(PACKAGE_PATH);
    verify(pyPiFacet).put(eq(PACKAGE_PATH), any(AssetBlob.class));

    verifyNoMoreInteractions(pyPiFacet);
  }

  @Test
  public void testIndexRestore() throws Exception {
    when(pyPiRestoreBlobData.getBlobData().getBlobName()).thenReturn(INDEX_PATH);

    underTest.restore(indexProps, blob, blobStore, false);

    verify(pyPiFacet).assetExists(INDEX_PATH);
    verify(pyPiFacet).put(eq(INDEX_PATH), any(AssetBlob.class));

    verifyNoMoreInteractions(pyPiFacet);
  }

  @Test
  public void testRestoreSkipNotFacet() {
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.empty());

    underTest.restore(indexProps, blob, blobStore, false);

    verifyNoMoreInteractions(pyPiFacet);
  }

  @Test
  public void testRestoreSkipExistingPackage() {
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(pyPiRestoreBlobData.getBlobData().getBlobName()).thenReturn(PACKAGE_PATH);
    when(pyPiFacet.assetExists(PACKAGE_PATH)).thenReturn(true);

    underTest.restore(packageProps, blob, blobStore, false);

    verify(pyPiFacet).assetExists(PACKAGE_PATH);

    verifyNoMoreInteractions(pyPiFacet);
  }

  @Test
  public void testCorrectChecksums() {
    assertThat(underTest.getHashAlgorithms(), equalTo(ImmutableList.of(SHA1, SHA256, MD5)));
  }

  @Test
  public void testRestoreRepairIfAssetsUpdated() {
    underTest.after(true, repository);

    verify(pyPiRepairIndexComponent).repairRepository(repository);
    verifyNoMoreInteractions(pyPiRepairIndexComponent);
  }

  @Test
  public void testRestoreDoesNotRepairIfAssetsNotUpdated() {
    underTest.after(false, repository);

    verifyZeroInteractions(pyPiRepairIndexComponent);
  }

  @Test
  public void blobDataIsCreated() {
    when(pyPiRestoreBlobDataFactory.create(restoreBlobData)).thenReturn(pyPiRestoreBlobData);

    assertThat(underTest.createRestoreData(restoreBlobData), is(pyPiRestoreBlobData));
    verify(pyPiRestoreBlobDataFactory).create(restoreBlobData);
    verifyNoMoreInteractions(pyPiRestoreBlobDataFactory, restoreBlobData);
  }

  @Test
  public void shouldSkipDeletedBlob() throws Exception {
    when(blobAttributes.isDeleted()).thenReturn(true);
    underTest.restore(packageProps, blob, blobStore, false);
    verifyNoMoreInteractions(pyPiFacet);
  }

  @Test
  public void shouldSkipOlderBlob() throws Exception {
    when(pyPiFacet.assetExists(PACKAGE_PATH)).thenReturn(true);
    when(pyPiRestoreBlobData.getBlobData().getBlobName()).thenReturn(PACKAGE_PATH);
    when(asset.blobCreated()).thenReturn(DateTime.now());
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now().minusDays(1));
    underTest.restore(packageProps, blob, blobStore, false);
    verify(pyPiFacet).assetExists(PACKAGE_PATH);
    verifyNoMoreInteractions(pyPiFacet);
  }

  @Test
  public void shouldRestoreMoreRecentBlob() throws Exception {
    when(pyPiFacet.assetExists(PACKAGE_PATH)).thenReturn(true);
    when(pyPiRestoreBlobData.getBlobData().getBlobName()).thenReturn(PACKAGE_PATH);
    when(asset.blobCreated()).thenReturn(DateTime.now().minusDays(1));
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now());
    underTest.restore(packageProps, blob, blobStore, false);
    verify(pyPiFacet).assetExists(PACKAGE_PATH);
    verify(pyPiFacet).put(eq(PACKAGE_PATH), any(AssetBlob.class));
    verifyNoMoreInteractions(pyPiFacet);
  }
}
