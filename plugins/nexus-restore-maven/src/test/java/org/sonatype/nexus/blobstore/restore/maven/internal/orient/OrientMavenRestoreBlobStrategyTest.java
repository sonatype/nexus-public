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
package org.sonatype.nexus.blobstore.restore.maven.internal.orient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
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
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.maven.OrientMavenFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

public class OrientMavenRestoreBlobStrategyTest
    extends TestSupport
{
  private static final String TEST_BLOB_STORE_NAME = "test";

  OrientMavenRestoreBlobStrategy underTest;

  @Mock
  Maven2MavenPathParser maven2MavenPathParser;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  MavenPath mavenPath;

  @Mock
  Coordinates coordinates;

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
  BlobStore blobStore;

  @Mock
  BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  Repository repository;

  @Mock
  StorageFacet storageFacet;

  @Mock
  StorageTx storageTx;

  @Mock
  Bucket bucket;

  @Mock
  OrientMavenFacet mavenFacet;

  @Mock
  DryRunPrefix dryRunPrefix;

  Properties properties = new Properties();

  byte[] blobBytes = "blobbytes".getBytes();

  @Before
  public void setup() throws IOException {
    underTest = Guice.createInjector(new TransactionModule(), new AbstractModule() {
      @Override
      protected void configure() {
        bind(NodeAccess.class).toInstance(nodeAccess);
        bind(RepositoryManager.class).toInstance(repositoryManager);
        bind(BlobStoreManager.class).toInstance(blobStoreManager);
        bind(DryRunPrefix.class).toInstance(new DryRunPrefix("dryrun"));
        bind(MavenPathParser.class).toInstance(maven2MavenPathParser);
      }
    }).getInstance(OrientMavenRestoreBlobStrategy.class);

    properties.setProperty("@BlobStore.blob-name", "org/codehaus/plexus/plexus/3.1/plexus-3.1.pom");
    properties.setProperty("@Bucket.repo-name", "test-repo");
    properties.setProperty("size", "1000");
    properties.setProperty("@BlobStore.content-type", "application/xml");
    properties.setProperty("sha1", "b64de86ceaa4f0e4d8ccc44a26c562c6fb7fb230");

    when(repositoryManager.get("test-repo")).thenReturn(repository);
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.of(storageFacet));
    when(repository.optionalFacet(OrientMavenFacet.class)).thenReturn(Optional.of(mavenFacet));

    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(storageFacet.blobStore()).thenReturn(blobStore);

    when(storageTx.findBucket(repository)).thenReturn(bucket);
    when(storageTx.findComponents(nullable(Query.class), nullable(Iterable.class))).thenReturn(ImmutableList.of(component));
    when(storageTx.findAssetWithProperty(eq(P_NAME), nullable(String.class), nullable(Bucket.class))).thenReturn(asset);

    when(component.getEntityMetadata()).thenReturn(entityMetadata);

    when(entityMetadata.getId()).thenReturn(entityId);

    when(asset.componentId()).thenReturn(entityId);

    when(blob.getId()).thenReturn(new BlobId("test"));
    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream(blobBytes));
    when(blob.getMetrics()).thenReturn(blobMetrics);

    when(maven2MavenPathParser.parsePath("org/codehaus/plexus/plexus/3.1/plexus-3.1.pom"))
        .thenReturn(mavenPath);

    when(mavenPath.getCoordinates()).thenReturn(coordinates);
    when(coordinates.getGroupId()).thenReturn("org.codehaus.plexus");
    when(coordinates.getArtifactId()).thenReturn("plexus");
    when(coordinates.getVersion()).thenReturn("3.1");

    when(nodeAccess.getId()).thenReturn("node");


    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME);

    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStore.getBlobAttributes(nullable(BlobId.class))).thenReturn(blobAttributes);

    when(blobAttributes.isDeleted()).thenReturn(false);

    when(repository.facet(OrientMavenFacet.class)).thenReturn(mavenFacet);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testRestore() throws Exception {
    underTest.restore(properties, blob, blobStore);
    verify(mavenFacet).get(mavenPath);
    verify(mavenFacet).put(eq(mavenPath), any(), eq(null));
    verifyNoMoreInteractions(mavenFacet);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testRestoreSkipNotFacet() {
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.empty());
    underTest.restore(properties, blob, blobStore);
    verifyNoMoreInteractions(mavenFacet);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testRestoreSkipExistingContent() throws Exception {
    when(mavenFacet.get(mavenPath)).thenReturn(mock(Content.class));
    underTest.restore(properties, blob, blobStore);
    verify(mavenFacet).get(mavenPath);
    verifyNoMoreInteractions(mavenFacet);
  }

  @Test
  public void testRestoreDryRun() throws Exception {
    underTest.restore(properties, blob, blobStore, true);
    verify(mavenFacet).get(mavenPath);
    verifyNoMoreInteractions(mavenFacet);
  }

  @Test
  public void testCorrectChecksums() throws Exception {
    Map<HashAlgorithm, HashCode> expectedHashes = Maps.newHashMap();
    expectedHashes.put(HashAlgorithm.MD5, HashAlgorithm.MD5.function().hashBytes(blobBytes));
    expectedHashes.put(HashAlgorithm.SHA1, HashAlgorithm.SHA1.function().hashBytes(blobBytes));
    expectedHashes.put(HashAlgorithm.SHA256, HashAlgorithm.SHA256.function().hashBytes(blobBytes));
    expectedHashes.put(HashAlgorithm.SHA512, HashAlgorithm.SHA512.function().hashBytes(blobBytes));
    ArgumentCaptor<AssetBlob> assetBlobCaptor = ArgumentCaptor.forClass(AssetBlob.class);

    underTest.restore(properties, blob, blobStore, false);
    verify(mavenFacet).get(mavenPath);
    verify(mavenFacet).put(eq(mavenPath), assetBlobCaptor.capture(), eq(null));

    assertEquals("asset hashes do not match blob", expectedHashes, assetBlobCaptor.getValue().getHashes());
  }

  @Test
  public void shouldSkipDeletedBlob() throws Exception {
    when(blobAttributes.isDeleted()).thenReturn(true);
    underTest.restore(properties, blob, blobStore, false);
    verifyNoMoreInteractions(mavenFacet);
  }

  @Test
  public void shouldSkipOlderBlob() throws Exception {
    when(mavenFacet.get(mavenPath)).thenReturn(mock(Content.class));
    when(asset.blobCreated()).thenReturn(DateTime.now());
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now().minusDays(1));
    underTest.restore(properties, blob, blobStore, false);
    verify(mavenFacet).get(mavenPath);
    verifyNoMoreInteractions(mavenFacet);
  }

  @Test
  public void shouldRestoreMoreRecentBlob() throws Exception {
    when(mavenFacet.get(mavenPath)).thenReturn(mock(Content.class));
    when(asset.blobCreated()).thenReturn(DateTime.now().minusDays(1));
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now());
    underTest.restore(properties, blob, blobStore, false);
    verify(mavenFacet).get(mavenPath);
    verify(mavenFacet).put(eq(mavenPath), any(), eq(null));
    verifyNoMoreInteractions(mavenFacet);
  }
}
