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
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.orient.HelmRestoreFacet;

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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * @since 3.28
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(UnitOfWork.class)
public class OrientHelmRestoreBlobStrategyTest
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
  HelmAttributes helmAttributes;

  @Mock
  Query query;

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
    when(blob.getMetrics()).thenReturn(blobMetrics);
    when(helmRestoreBlobData.getBlobData()).thenReturn(restoreBlobData);
    when(restoreBlobData.getBlobName()).thenReturn(ARCHIVE_PATH);
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(restoreBlobData.getBlob()).thenReturn(blob);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(storageFacet.blobStore()).thenReturn(blobStore);
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(storageTx.findComponents(any(Query.class), any(Iterable.class))).thenReturn(ImmutableList.of(component));
    when(storageTx.findAssetWithProperty(eq(P_NAME), anyString(), any(Bucket.class))).thenReturn(asset);
    when(component.getEntityMetadata()).thenReturn(entityMetadata);
    when(entityMetadata.getId()).thenReturn(entityId);
    when(asset.componentId()).thenReturn(entityId);
    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME);

    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStore.getBlobAttributes(any(BlobId.class))).thenReturn(blobAttributes);

    mockStatic(UnitOfWork.class);
    when(UnitOfWork.currentTx()).thenReturn(storageTx);

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

  @Test
  public void shouldSkipDeletedBlob() throws Exception {
    when(blobAttributes.isDeleted()).thenReturn(true);
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verifyNoMoreInteractions(helmRestoreFacet);
  }

  @Test
  public void shouldSkipOlderBlob() throws Exception {
    when(helmRestoreFacet.assetExists(eq(ARCHIVE_PATH))).thenReturn(true);
    when(helmRestoreFacet.componentRequired(eq(ARCHIVE_PATH))).thenReturn(true);
    when(helmRestoreFacet.extractComponentAttributesFromArchive(anyString(), any())).thenReturn(helmAttributes);
    when(helmRestoreFacet.getComponentQuery(eq(helmAttributes))).thenReturn(query);
    when(asset.blobCreated()).thenReturn(DateTime.now());
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now().minusDays(1));
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(helmRestoreFacet).assetExists(ARCHIVE_PATH);
    verify(helmRestoreFacet).componentRequired(ARCHIVE_PATH);
    verify(helmRestoreFacet).getComponentQuery(helmAttributes);
    verify(helmRestoreFacet).extractComponentAttributesFromArchive(anyString(), any());
    verifyNoMoreInteractions(helmRestoreFacet);
  }

  @Test
  public void shouldRestoreMoreRecentBlob() throws Exception {
    when(helmRestoreFacet.assetExists(eq(ARCHIVE_PATH))).thenReturn(true);
    when(helmRestoreFacet.componentRequired(eq(ARCHIVE_PATH))).thenReturn(true);
    when(helmRestoreFacet.extractComponentAttributesFromArchive(anyString(), any())).thenReturn(helmAttributes);
    when(helmRestoreFacet.getComponentQuery(eq(helmAttributes))).thenReturn(query);
    when(asset.blobCreated()).thenReturn(DateTime.now().minusDays(1));
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now());
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(helmRestoreFacet).assetExists(ARCHIVE_PATH);
    verify(helmRestoreFacet).componentRequired(ARCHIVE_PATH);
    verify(helmRestoreFacet).getComponentQuery(helmAttributes);
    verify(helmRestoreFacet).extractComponentAttributesFromArchive(anyString(), any());
    verify(helmRestoreFacet).restore(any(AssetBlob.class), eq(ARCHIVE_PATH));
    verifyNoMoreInteractions(helmRestoreFacet);
  }
}
