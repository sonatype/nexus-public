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
package org.sonatype.nexus.blobstore.restore.apt.orient.internal;

import java.io.ByteArrayInputStream;
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
import org.sonatype.nexus.blobstore.restore.apt.internal.orient.AptRestoreBlobData;
import org.sonatype.nexus.blobstore.restore.apt.internal.orient.OrientAptRestoreBlobStrategy;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.orient.AptRestoreFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

public class OrientAptRestoreBlobStrategyTest
    extends TestSupport
{
  OrientAptRestoreBlobStrategy underTest;

  private static final String TEST_BLOB_STORE_NAME = "test";

  private static final String PACKAGE_PATH = "pool/main/n/nano/nano_2.9.3-2_amd64.deb";

  private static final String RELEASE_FILE_PATH = "metadata/Release";

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  Repository repository;

  @Mock
  StorageFacet storageFacet;

  @Mock
  AptRestoreFacet aptRestoreFacet;

  @Mock
  AptRestoreBlobData aptRestoreBlobData;

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
  BlobStore blobStore;

  @Mock
  StorageTx storageTx;

  byte[] blobBytes = "blobbytes".getBytes();

  Properties properties = new Properties();

  @Before
  public void setup() {
    underTest = Guice.createInjector(new TransactionModule(), new AbstractModule() {
      @Override
      protected void configure() {
        bind(NodeAccess.class).toInstance(nodeAccess);
        bind(RepositoryManager.class).toInstance(repositoryManager);
        bind(BlobStoreManager.class).toInstance(blobStoreManager);
        bind(DryRunPrefix.class).toInstance(new DryRunPrefix("dryrun"));
      }
    }).getInstance(OrientAptRestoreBlobStrategy.class);

    when(repositoryManager.get(nullable(String.class))).thenReturn(repository);
    when(repository.facet(AptRestoreFacet.class)).thenReturn(aptRestoreFacet);
    when(repository.optionalFacet(AptRestoreFacet.class)).thenReturn(Optional.of(aptRestoreFacet));
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.of(storageFacet));
    when(aptRestoreBlobData.getBlobData()).thenReturn(restoreBlobData);
    when(aptRestoreBlobData.getBlobData().getBlobName()).thenReturn(PACKAGE_PATH);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(storageFacet.blobStore()).thenReturn(blobStore);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStore.getBlobAttributes(nullable(BlobId.class))).thenReturn(blobAttributes);
    when(blobAttributes.isDeleted()).thenReturn(false);
    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream(blobBytes));
    when(blob.getMetrics()).thenReturn(blobMetrics);
    when(blobStoreManager.get(TEST_BLOB_STORE_NAME)).thenReturn(blobStore);
    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(restoreBlobData.getBlob()).thenReturn(blob);
    when(storageTx.findAssetWithProperty(eq(P_NAME), eq(PACKAGE_PATH), nullable(Bucket.class))).thenReturn(asset);

    properties.setProperty("@BlobStore.created-by", "anonymous");
    properties.setProperty("size", "1330");
    properties.setProperty("@Bucket.repo-name", "apt-proxy");
    properties.setProperty("creationTime", "1533220387218");
    properties.setProperty("@BlobStore.created-by-ip", "127.0.0.1");
    properties.setProperty("@BlobStore.content-type", "text/html");
    properties.setProperty("@BlobStore.blob-name", PACKAGE_PATH);
    properties.setProperty("sha1", "0088eb478752a810f48f04d3cf9f46d2924e334a");
  }

  @Test
  public void testBlobDataIsCreated() {
    assertThat(underTest.createRestoreData(restoreBlobData).getBlobData(), is(restoreBlobData));
  }

  @Test(expected = IllegalStateException.class)
  public void testIfBlobDataNameIsEmpty_ExceptionIsThrown() {
    when(aptRestoreBlobData.getBlobData().getBlobName()).thenReturn("");
    underTest.createRestoreData(restoreBlobData);
  }

  @Test
  public void testCorrectHashAlgorithmsAreSupported() {
    assertThat(underTest.getHashAlgorithms(), containsInAnyOrder(SHA1, SHA256, MD5));
  }

  @Test
  public void testAppropriatePathIsReturned() {
    assertThat(underTest.getAssetPath(aptRestoreBlobData), is(PACKAGE_PATH));
  }

  @Test
  public void testPackageIsRestored() throws Exception {
    underTest.restore(properties, blob, blobStore, false);
    verify(aptRestoreFacet).assetExists(PACKAGE_PATH);
    verify(aptRestoreFacet).restore(nullable(AssetBlob.class), eq(PACKAGE_PATH));
    verifyNoMoreInteractions(aptRestoreFacet);
  }

  @Test
  public void testRestoreIsSkip_IfPackageExists() {
    when(aptRestoreFacet.assetExists(PACKAGE_PATH)).thenReturn(true);
    underTest.restore(properties, blob, blobStore, false);

    verify(aptRestoreFacet).assetExists(PACKAGE_PATH);
    verify(aptRestoreFacet).componentRequired(PACKAGE_PATH);
    verifyNoMoreInteractions(aptRestoreFacet);
  }

  @Test
  public void testComponentIsRequiredForDeb() {
    boolean expected = true;
    when(aptRestoreFacet.componentRequired(PACKAGE_PATH)).thenReturn(expected);
    assertThat(underTest.componentRequired(aptRestoreBlobData), is(expected));
    verify(aptRestoreFacet).componentRequired(PACKAGE_PATH);
    verifyNoMoreInteractions(aptRestoreFacet);
  }

  @Test
  public void testComponentIsNotRequiredForRelease() {
    boolean expected = false;
    when(aptRestoreFacet.componentRequired(RELEASE_FILE_PATH)).thenReturn(expected);
    assertThat(underTest.componentRequired(aptRestoreBlobData), is(expected));
    verify(aptRestoreFacet).componentRequired(PACKAGE_PATH);
    verifyNoMoreInteractions(aptRestoreFacet);
  }

  @Test
  public void shouldSkipDeletedBlob() throws Exception {
    when(blobAttributes.isDeleted()).thenReturn(true);
    underTest.restore(properties, blob, blobStore, false);
    verifyNoMoreInteractions(aptRestoreFacet);
  }

  @Test
  public void shouldSkipOlderBlob() throws Exception {
    when(aptRestoreFacet.assetExists(PACKAGE_PATH)).thenReturn(true);
    when(asset.blobCreated()).thenReturn(DateTime.now());
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now().minusDays(1));
    underTest.restore(properties, blob, blobStore, false);
    verify(aptRestoreFacet).assetExists(PACKAGE_PATH);
    verify(aptRestoreFacet).componentRequired(PACKAGE_PATH);
    verifyNoMoreInteractions(aptRestoreFacet);
  }

  @Test
  public void shouldRestoreMoreRecentBlob() throws Exception {
    when(aptRestoreFacet.assetExists(PACKAGE_PATH)).thenReturn(true);
    when(asset.blobCreated()).thenReturn(DateTime.now().minusDays(1));
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now());
    underTest.restore(properties, blob, blobStore, false);
    verify(aptRestoreFacet).assetExists(PACKAGE_PATH);
    verify(aptRestoreFacet).componentRequired(PACKAGE_PATH);
    verify(aptRestoreFacet).restore(nullable(AssetBlob.class), eq(PACKAGE_PATH));
    verifyNoMoreInteractions(aptRestoreFacet);
  }
}
