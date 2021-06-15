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
package org.sonatype.nexus.blobstore.restore.p2.orient.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.blobstore.restore.p2.internal.P2RestoreBlobData;
import org.sonatype.nexus.blobstore.restore.p2.orient.internal.P2RestoreBlobStrategy;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.p2.orient.P2RestoreFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.singletonList;
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
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * @since 0.next
 */
public class P2RestoreBlobStrategyTest
    extends TestSupport
{
  private static final String PACKAGE_PATH = "https/download.eclipse.org/releases/2019-12/201912181000/plugins/org.eclipse.core.resources_3.13.600.v20191122-2104.jar";

  private static final String TEST_BLOB_STORE_NAME = "test";

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
  P2RestoreFacet p2RestoreFacet;

  @Mock
  P2RestoreBlobData p2RestoreBlobData;

  @Mock
  private RestoreBlobData restoreBlobData;

  @Mock
  Blob blob;

  @Mock
  BlobAttributes blobAttributes;

  @Mock
  BlobMetrics blobMetrics;

  @Mock
  Asset asset;

  @Mock
  Component component;

  @Mock
  EntityMetadata entityMetadata;

  @Mock
  EntityId entityId;

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

  private P2RestoreBlobStrategy restoreBlobStrategy;

  @Before
  public void setup() throws IOException {
    restoreBlobStrategy = Guice.createInjector(new TransactionModule(), new AbstractModule() {
      @Override
      protected void configure() {
        bind(NodeAccess.class).toInstance(nodeAccess);
        bind(RepositoryManager.class).toInstance(repositoryManager);
        bind(BlobStoreManager.class).toInstance(blobStoreManager);
        bind(DryRunPrefix.class).toInstance(new DryRunPrefix("dryrun"));
      }
    }).getInstance(P2RestoreBlobStrategy.class);

    when(repositoryManager.get(anyString())).thenReturn(repository);
    when(repository.facet(P2RestoreFacet.class)).thenReturn(p2RestoreFacet);
    when(repository.optionalFacet(P2RestoreFacet.class)).thenReturn(Optional.of(p2RestoreFacet));
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.of(storageFacet));
    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream(blobBytes));
    when(blob.getMetrics()).thenReturn(blobMetrics);
    when(p2RestoreBlobData.getBlobData()).thenReturn(restoreBlobData);
    when(restoreBlobData.getBlobName()).thenReturn(PACKAGE_PATH);
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(restoreBlobData.getBlob()).thenReturn(blob);
    when(restoreBlobData.getBlobStore()).thenReturn(blobStore);
    when(restoreBlobData.getRepository()).thenReturn(repository);

    when(p2RestoreFacet.getComponentQuery(eq(blob), anyString(), anyString())).thenReturn(query);

    when(storageTx.findComponents(eq(query), any(Iterable.class))).thenReturn(singletonList(component));
    when(storageTx.findAssetWithProperty(eq(P_NAME), eq(PACKAGE_PATH), any(Bucket.class))).thenReturn(asset);

    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(storageFacet.blobStore()).thenReturn(blobStore);

    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStore.getBlobAttributes(any(BlobId.class))).thenReturn(blobAttributes);

    when(blobAttributes.isDeleted()).thenReturn(false);

    when(asset.componentId()).thenReturn(entityId);

    when(component.getEntityMetadata()).thenReturn(entityMetadata);

    when(entityMetadata.getId()).thenReturn(entityId);

    when(blobStoreManager.get(TEST_BLOB_STORE_NAME)).thenReturn(blobStore);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME);

    properties.setProperty("@BlobStore.created-by", "anonymous");
    properties.setProperty("size", "894185");
    properties.setProperty("@Bucket.repo-name", "p2-proxy");
    properties.setProperty("creationTime", "1577179311620");
    properties.setProperty("@BlobStore.created-by-ip", "127.0.0.1");
    properties.setProperty("@BlobStore.content-type", "application/java-archive");
    properties.setProperty("@BlobStore.blob-name", PACKAGE_PATH);
    properties.setProperty("sha1", "ac7306bee8742701a1e81a702685a55c17b07e4a");
    properties.setProperty("sha256", "c96079af9c3b1506894adf617f1db4710974dc947502b8b0f938616a520d35e6");
    properties.setProperty("sha512",
        "97c2358a587af72b43095dc6525bf32b34a7391f7d2a4af8087640b5cecba0ef2cd2965e9a749742341a0dc57ebfcd4c7766dc53580def74adf19a58dd2e980d");
    properties.setProperty("md5", "b20040eb2bfd202bbc5e734885e71549");
  }

  @Test
  public void testBlobDataIsCreated() {
    assertThat(restoreBlobStrategy.createRestoreData(restoreBlobData).getBlobData(), is(restoreBlobData));
  }

  @Test(expected = IllegalStateException.class)
  public void testIfBlobDataNameIsEmptyExceptionIsThrown() {
    when(p2RestoreBlobData.getBlobData().getBlobName()).thenReturn("");
    restoreBlobStrategy.createRestoreData(restoreBlobData);
  }

  @Test
  public void testCorrectHashAlgorithmsAreSupported() {
    assertThat(restoreBlobStrategy.getHashAlgorithms(),
        containsInAnyOrder(HashAlgorithm.ALL_HASH_ALGORITHMS.values().toArray()));
  }

  @Test
  public void testAppropriatePathIsReturned() {
    assertThat(restoreBlobStrategy.getAssetPath(p2RestoreBlobData), is(PACKAGE_PATH));
  }

  @Test
  public void testPackageIsRestored() throws IOException {
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(p2RestoreFacet).assetExists(PACKAGE_PATH);
    verify(p2RestoreFacet).restore(any(AssetBlob.class), eq(PACKAGE_PATH));
    verifyNoMoreInteractions(p2RestoreFacet);
  }

  @Test
  public void testRestoreIsSkipIfPackageExists() {
    when(p2RestoreFacet.assetExists(PACKAGE_PATH)).thenReturn(true);
    restoreBlobStrategy.restore(properties, blob, blobStore, false);

    verify(p2RestoreFacet).assetExists(PACKAGE_PATH);
    verify(p2RestoreFacet).componentRequired(PACKAGE_PATH);
    verifyNoMoreInteractions(p2RestoreFacet);
  }

  @Test
  public void testComponentIsRequiredForGz() {
    boolean expected = true;
    when(p2RestoreFacet.componentRequired(PACKAGE_PATH)).thenReturn(expected);
    assertThat(restoreBlobStrategy.componentRequired(p2RestoreBlobData), is(expected));
    verify(p2RestoreFacet).componentRequired(PACKAGE_PATH);
    verifyNoMoreInteractions(p2RestoreFacet);
  }

  @Test
  public void testComponentQuery() throws IOException
  {
    restoreBlobStrategy.getComponentQuery(p2RestoreBlobData);
    verify(p2RestoreFacet, times(1)).getComponentQuery(any(), any(), any());
  }

  @Test
  public void shouldSkipDeletedBlob() throws Exception {
    when(blobAttributes.isDeleted()).thenReturn(true);
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verifyNoMoreInteractions(p2RestoreFacet);
  }

  @Test
  public void shouldSkipOlderBlob() throws Exception {
    when(p2RestoreFacet.assetExists(PACKAGE_PATH)).thenReturn(true);
    when(p2RestoreFacet.componentRequired(PACKAGE_PATH)).thenReturn(true);
    when(asset.blobCreated()).thenReturn(DateTime.now());
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now().minusDays(1));
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(p2RestoreFacet).assetExists(PACKAGE_PATH);
    verify(p2RestoreFacet).componentRequired(PACKAGE_PATH);
    verify(p2RestoreFacet).getComponentQuery(eq(blob), anyString(), anyString());
    verifyNoMoreInteractions(p2RestoreFacet);
  }

  @Test
  public void shouldRestoreMoreRecentBlob() throws Exception {
    when(p2RestoreFacet.assetExists(PACKAGE_PATH)).thenReturn(true);
    when(p2RestoreFacet.componentRequired(PACKAGE_PATH)).thenReturn(true);
    when(asset.blobCreated()).thenReturn(DateTime.now().minusDays(1));
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now());
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(p2RestoreFacet).assetExists(PACKAGE_PATH);
    verify(p2RestoreFacet).componentRequired(PACKAGE_PATH);
    verify(p2RestoreFacet).getComponentQuery(any(Blob.class), anyString(), anyString());
    verify(p2RestoreFacet).restore(any(AssetBlob.class), eq(PACKAGE_PATH));
    verifyNoMoreInteractions(p2RestoreFacet);
  }
}
