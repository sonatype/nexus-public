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
package org.sonatype.nexus.blobstore.restore.r.internal.orient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
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
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.r.orient.OrientRRestoreFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

public class OrientRRestoreBlobStrategyTest
    extends TestSupport
{
  private static final String TEST_BLOB_STORE_NAME = "test";

  private static final String ARCHIVE_PATH = "src/contrib/curl_4.2.tar.gz";

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
  OrientRRestoreFacet rRestoreFacet;

  @Mock
  OrientRRestoreBlobData rRestoreBlobData;

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
  Query query;

  @Mock
  BlobStore blobStore;

  @Mock
  private BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  StorageTx storageTx;

  private byte[] blobBytes = "blobbytes".getBytes();

  private Properties properties = new Properties();

  private OrientRRestoreBlobStrategy restoreBlobStrategy;

  @Before
  public void setup() {
    restoreBlobStrategy  = Guice.createInjector(new TransactionModule(), new AbstractModule() {
      @Override
      protected void configure() {
        bind(NodeAccess.class).toInstance(nodeAccess);
        bind(RepositoryManager.class).toInstance(repositoryManager);
        bind(BlobStoreManager.class).toInstance(blobStoreManager);
        bind(DryRunPrefix.class).toInstance(new DryRunPrefix("dryrun"));
      }
    }).getInstance(OrientRRestoreBlobStrategy.class);

    when(repositoryManager.get(anyString())).thenReturn(repository);
    when(repository.facet(OrientRRestoreFacet.class)).thenReturn(rRestoreFacet);
    when(repository.optionalFacet(OrientRRestoreFacet.class)).thenReturn(Optional.of(rRestoreFacet));
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.of(storageFacet));
    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream(blobBytes));
    when(blob.getMetrics()).thenReturn(blobMetrics);
    when(rRestoreBlobData.getBlobData()).thenReturn(restoreBlobData);
    when(restoreBlobData.getBlobName()).thenReturn(ARCHIVE_PATH);
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(restoreBlobData.getBlob()).thenReturn(blob);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(storageFacet.blobStore()).thenReturn(blobStore);
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(storageTx.findComponents(eq(query), any(Iterable.class))).thenReturn(ImmutableList.of(component));
    when(storageTx.findAssetWithProperty(eq(P_NAME), eq(ARCHIVE_PATH), any(Bucket.class))).thenReturn(asset);
    when(component.getEntityMetadata()).thenReturn(entityMetadata);
    when(entityMetadata.getId()).thenReturn(entityId);
    when(asset.componentId()).thenReturn(entityId);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStore.getBlobAttributes(any(BlobId.class))).thenReturn(blobAttributes);
    when(blobAttributes.isDeleted()).thenReturn(false);
    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME);
    when(rRestoreFacet.componentRequired(ARCHIVE_PATH)).thenReturn(true);
    when(rRestoreFacet.getComponentQuery(anyMap())).thenReturn(query);
    when(rRestoreFacet.extractComponentAttributesFromArchive(anyString(), any())).thenReturn(Collections.emptyMap());

    properties.setProperty("@BlobStore.created-by", "anonymous");
    properties.setProperty("size", "1330");
    properties.setProperty("@Bucket.repo-name", "r-proxy");
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
    when(rRestoreBlobData.getBlobData().getBlobName()).thenReturn("");
    restoreBlobStrategy.createRestoreData(restoreBlobData);
  }

  @Test
  public void testCorrectHashAlgorithmsAreSupported() {
    assertThat(restoreBlobStrategy.getHashAlgorithms(), containsInAnyOrder(SHA1));
  }

  @Test
  public void testAppropriatePathIsReturned() {
    assertThat(restoreBlobStrategy.getAssetPath(rRestoreBlobData), is(ARCHIVE_PATH));
  }

  @Test
  public void testPackageIsRestored() throws Exception {
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(rRestoreFacet).assetExists(ARCHIVE_PATH);
    verify(rRestoreFacet).restore(any(AssetBlob.class), eq(ARCHIVE_PATH));
    verifyNoMoreInteractions(rRestoreFacet);
  }

  @Test
  public void testRestoreIsSkipIfPackageExists() {
    when(rRestoreFacet.assetExists(ARCHIVE_PATH)).thenReturn(true);
    restoreBlobStrategy.restore(properties, blob, blobStore, false);

    verify(rRestoreFacet).assetExists(ARCHIVE_PATH);
    verify(rRestoreFacet).componentRequired(ARCHIVE_PATH);
    verify(rRestoreFacet).getComponentQuery(anyMap());
    verify(rRestoreFacet).extractComponentAttributesFromArchive(anyString(), any());
    verifyNoMoreInteractions(rRestoreFacet);
  }

  @Test
  public void testComponentIsRequiredForGz() {
    boolean expected = true;
    when(rRestoreFacet.componentRequired(ARCHIVE_PATH)).thenReturn(expected);
    assertThat(restoreBlobStrategy.componentRequired(rRestoreBlobData), is(expected));
    verify(rRestoreFacet).componentRequired(ARCHIVE_PATH);
    verifyNoMoreInteractions(rRestoreFacet);
  }

  @Test
  public void testComponentQuery() throws IOException
  {
    restoreBlobStrategy.getComponentQuery(rRestoreBlobData);
    verify(rRestoreFacet, times(1)).getComponentQuery(anyMapOf(String.class, String.class));
  }

  @Test
  public void shouldSkipDeletedBlob() throws Exception {
    when(rRestoreFacet.assetExists(ARCHIVE_PATH)).thenReturn(true);
    when(blobAttributes.isDeleted()).thenReturn(true);
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verifyNoMoreInteractions(rRestoreFacet);
  }

  @Test
  public void shouldSkipOlderBlob() throws Exception {
    when(rRestoreFacet.assetExists(ARCHIVE_PATH)).thenReturn(true);
    when(asset.blobCreated()).thenReturn(DateTime.now());
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now().minusDays(1));
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(rRestoreFacet).assetExists(ARCHIVE_PATH);
    verify(rRestoreFacet).componentRequired(ARCHIVE_PATH);
    verify(rRestoreFacet).getComponentQuery(anyMap());
    verify(rRestoreFacet).extractComponentAttributesFromArchive(anyString(), any());
    verifyNoMoreInteractions(rRestoreFacet);  }

  @Test
  public void shouldRestoreMoreRecentBlob() throws Exception {
    when(rRestoreFacet.assetExists(ARCHIVE_PATH)).thenReturn(true);
    when(asset.blobCreated()).thenReturn(DateTime.now().minusDays(1));
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now());
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(rRestoreFacet).assetExists(ARCHIVE_PATH);
    verify(rRestoreFacet).componentRequired(ARCHIVE_PATH);
    verify(rRestoreFacet).getComponentQuery(anyMap());
    verify(rRestoreFacet).restore(any(AssetBlob.class), eq(ARCHIVE_PATH));
    verify(rRestoreFacet).extractComponentAttributesFromArchive(anyString(), any());
    verifyNoMoreInteractions(rRestoreFacet);
  }
}
