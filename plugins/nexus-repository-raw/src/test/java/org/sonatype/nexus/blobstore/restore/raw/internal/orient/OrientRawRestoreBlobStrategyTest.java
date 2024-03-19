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
package org.sonatype.nexus.blobstore.restore.raw.internal.orient;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.raw.RawContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.common.hash.HashCode;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA512;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

public class OrientRawRestoreBlobStrategyTest
    extends TestSupport
{
  private static final String TEST_BLOB_STORE_NAME = "test";

  private static final String REPOSITORY_NAME = "theRepository";

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
  private BlobAttributes blobAttributes;

  @Mock
  private BlobStore blobStore;

  @Mock
  private BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  private BlobId blobId;

  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private RawContentFacet rawContentFacet;

  @Mock
  private Bucket bucket;

  @Mock
  private Asset asset;

  @Mock
  private Component component;

  @Mock
  private StorageTx storageTx;

  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private EntityId entityId;

  @Mock
  private EntityMetadata entityMetadata;

  @Mock
  private BlobMetrics blobMetrics;

  private OrientRawRestoreBlobStrategy underTest;

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
    when(storageFacet.blobStore()).thenReturn(blobStore);

    when(rawContentFacet.assetExists(BLOB_NAME)).thenReturn(!EXISTS);

    when(blob.getMetrics()).thenReturn(blobMetrics);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStore.getBlobAttributes(nullable(BlobId.class))).thenReturn(blobAttributes);
    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME);

    when(storageTx.findBucket(repository)).thenReturn(bucket);
    when(storageTx.findAssetWithProperty(eq(P_NAME), anyString(), any(Bucket.class))).thenReturn(asset);
    when(storageTx.findComponents(any(), any())).thenReturn(Collections.singletonList(component));

    when(asset.componentId()).thenReturn(entityId);
    when(component.getEntityMetadata()).thenReturn(entityMetadata);
    when(entityMetadata.getId()).thenReturn(entityId);

    underTest = Guice.createInjector(new TransactionModule(), new AbstractModule() {
      @Override
      protected void configure() {
        bind(NodeAccess.class).toInstance(nodeAccess);
        bind(RepositoryManager.class).toInstance(repositoryManager);
        bind(DryRunPrefix.class).toInstance(DRY_RUN_PREFIX);
      }
    }).getInstance(OrientRawRestoreBlobStrategy.class);
  }

  @Test
  public void testRestoreWhenNoStorageFacet() {
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.empty());

    underTest.restore(properties, blob, blobStore, !DRY_RUN);
    verifyNoMoreInteractions(rawContentFacet);
  }

  @Test
  public void testRestoreWhenNoRawContentFacet() {
    when(repository.optionalFacet(RawContentFacet.class)).thenReturn(Optional.empty());

    underTest.restore(properties, blob, blobStore, !DRY_RUN);
    verifyNoMoreInteractions(rawContentFacet);
  }

  @Ignore("mpiggott - not convinced this test every verified what it claimed, disabling temporarily for Mockito")
  @Test
  public void testRestoreWhenAssetExists() {
    when(rawContentFacet.assetExists(BLOB_NAME)).thenReturn(EXISTS);

    underTest.restore(properties, blob, blobStore, !DRY_RUN);
    verify(rawContentFacet).assetExists(BLOB_NAME);
    verifyNoMoreInteractions(rawContentFacet);
  }

  @Test
  public void testRestoreWhenDryRun() {
    underTest.restore(properties, blob, blobStore, DRY_RUN);
    verify(rawContentFacet).assetExists(BLOB_NAME);
    verifyNoMoreInteractions(rawContentFacet);
  }

  @Test
  public void testRestoreCreatesAssetFromBlobWithExpectedHashes() throws Exception {
    ArgumentCaptor<AssetBlob> assetBlobCaptor = ArgumentCaptor.forClass(AssetBlob.class);

    Map<HashAlgorithm, HashCode> expectedHashes = Stream.of(SHA1, MD5, SHA256, SHA512)
        .collect(toMap(identity(), algorithm -> algorithm.function().hashBytes(BLOB_BYTES)));

    underTest.restore(properties, blob, blobStore, !DRY_RUN);

    verify(rawContentFacet).put(eq(BLOB_NAME), assetBlobCaptor.capture(), eq(NO_CONTENT_ATTRIBUTES));
    assertThat(assetBlobCaptor.getValue().getHashes(), equalTo(expectedHashes));
    assertThat(assetBlobCaptor.getValue().getContentType(), equalTo(CONTENT_TYPE));
  }

  @Test
  public void shouldSkipDeletedBlob() throws Exception {
    when(blobAttributes.isDeleted()).thenReturn(true);
    underTest.restore(properties, blob, blobStore, false);
    verifyNoMoreInteractions(rawContentFacet);
  }

  @Test
  public void shouldSkipOlderBlob() throws Exception {
    when(rawContentFacet.assetExists(BLOB_NAME)).thenReturn(EXISTS);
    mockBlobCreated(DateTime.now());
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now().minusDays(1));

    underTest.restore(properties, blob, blobStore, false);

    verify(asset, never()).blobCreated();
    verify(rawContentFacet).assetExists(any());
    verifyNoMoreInteractions(rawContentFacet);
  }

  @Test
  public void shouldRestoreMoreRecentBlob() throws Exception {
    when(rawContentFacet.assetExists(BLOB_NAME)).thenReturn(EXISTS);
    mockBlobCreated(DateTime.now().minusDays(1));

    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now());
    underTest.restore(properties, blob, blobStore, false);

    verify(asset, never()).blobCreated();
    verify(rawContentFacet).assetExists(any());
    verify(rawContentFacet).put(eq(BLOB_NAME), any(), eq(NO_CONTENT_ATTRIBUTES));
    verifyNoMoreInteractions(rawContentFacet);
  }

  private void mockBlobCreated(final DateTime date) {
    BlobRef ref = mock(BlobRef.class);
    when(asset.blobRef()).thenReturn(ref);
    Blob existingBlob = mock(Blob.class);
    BlobMetrics metrics = mock(BlobMetrics.class);
    when(existingBlob.getMetrics()).thenReturn(metrics);
    when(metrics.getCreationTime()).thenReturn(date);
    when(storageTx.getBlob(ref)).thenReturn(existingBlob);
  }
}
