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
package org.sonatype.nexus.blobstore.restore.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.sonatype.nexus.repository.npm.NpmFacet;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.internal.NpmRestoreBlobStrategy;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.npm.repair.NpmRepairPackageRootComponent;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.google.common.hash.HashCode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class NpmRestoreBlobStrategyTest
    extends TestSupport
{
  private static final String TEST_BLOB_STORE_NAME = "test";

  private static final String TEST_PACKAGE_NAME = "query-string";

  private static final String TEST_TARBALL_NAME = "query-string-1.0.0.tgz";

  NpmRestoreBlobStrategy underTest;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  BlobStore blobStore;

  @Mock
  Blob blob;

  @Mock
  Repository repository;

  @Mock
  StorageFacet storageFacet;

  @Mock
  StorageTx storageTx;

  @Mock
  Bucket bucket;

  @Mock
  NpmFacet npmFacet;
  
  @Mock
  NpmRepairPackageRootComponent npmRepairPackageRootComponent;

  Properties packageProps = new Properties();

  Properties tarballProps = new Properties();

  Properties repoRootProps = new Properties();

  byte[] blobBytes = "blobbytes".getBytes();

  @Before
  public void setup() throws IOException {
    underTest = new NpmRestoreBlobStrategy(nodeAccess, repositoryManager, blobStoreManager, new DryRunPrefix("dryrun"),
        npmRepairPackageRootComponent);

    packageProps.setProperty("@BlobStore.blob-name", TEST_PACKAGE_NAME);
    packageProps.setProperty("@Bucket.repo-name", "test-repo");
    packageProps.setProperty("size", "1000");
    packageProps.setProperty("@BlobStore.content-type", "application/xml");
    packageProps.setProperty("sha1", "b64de86ceaa4f0e4d8ccc44a26c562c6fb7fb230");

    tarballProps.setProperty("@BlobStore.blob-name", TEST_PACKAGE_NAME + "/-/" + TEST_TARBALL_NAME);
    tarballProps.setProperty("@Bucket.repo-name", "test-repo");
    tarballProps.setProperty("size", "2000");
    tarballProps.setProperty("@BlobStore.content-type", "application/x-tgz");
    tarballProps.setProperty("sha1", "244cb02c77ec2e74f78a9bd318218abc9c500a61");

    repoRootProps.setProperty("@BlobStore.blob-name", "-/all");
    repoRootProps.setProperty("@Bucket.repo-name", "test-repo");
    repoRootProps.setProperty("size", "3000");
    repoRootProps.setProperty("@BlobStore.content-type", "application/json");
    repoRootProps.setProperty("sha1", "e4edaa6af69865e35ceb0882ce61e460c07f700c");

    Mockito.when(repositoryManager.get("test-repo")).thenReturn(repository);

    Mockito.when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.of(storageFacet));
    Mockito.when(repository.optionalFacet(NpmFacet.class)).thenReturn(Optional.of(npmFacet));
    Mockito.when(repository.facet(NpmFacet.class)).thenReturn(npmFacet);

    Mockito.when(storageFacet.txSupplier()).thenReturn(() -> storageTx);

    Mockito.when(storageTx.findBucket(repository)).thenReturn(bucket);

    Mockito.when(blob.getId()).thenReturn(new BlobId("test"));
    Mockito.when(blob.getInputStream()).thenReturn(new ByteArrayInputStream(blobBytes));

    Mockito.when(nodeAccess.getId()).thenReturn("node");

    Mockito.when(blobStoreManager.get(TEST_BLOB_STORE_NAME)).thenReturn(blobStore);
  }

  @Test
  public void testCorrectChecksums() throws Exception {
    Map<HashAlgorithm, HashCode> expectedHashes = Collections
        .singletonMap(HashAlgorithm.SHA1, HashAlgorithm.SHA1.function().hashBytes(blobBytes));
    ArgumentCaptor<AssetBlob> assetBlobCaptor = ArgumentCaptor.forClass(AssetBlob.class);

    underTest.restore(packageProps, blob, TEST_BLOB_STORE_NAME, false);

    Mockito.verify(npmFacet).putPackageRoot(Matchers.eq(TEST_PACKAGE_NAME), assetBlobCaptor.capture(), Matchers.eq(null));
    assertEquals("asset hashes do not match blob", expectedHashes, assetBlobCaptor.getValue().getHashes());
  }

  @Test
  public void testPackageRestore() throws Exception {
    underTest.restore(packageProps, blob, TEST_BLOB_STORE_NAME, false);
    Mockito.verify(npmFacet).findPackageRootAsset(Matchers.eq(TEST_PACKAGE_NAME));
    Mockito.verify(npmFacet).putPackageRoot(Matchers.eq(TEST_PACKAGE_NAME), Matchers.any(AssetBlob.class), Matchers.eq(null));
    Mockito.verifyNoMoreInteractions(npmFacet);
  }

  @Test
  public void testTarballRestore() throws Exception {
    underTest.restore(tarballProps, blob, TEST_BLOB_STORE_NAME, false);
    Mockito.verify(npmFacet).findTarballAsset(Matchers.eq(TEST_PACKAGE_NAME), Matchers.eq(TEST_TARBALL_NAME));
    Mockito.verify(npmFacet).putTarball(Matchers.eq(TEST_PACKAGE_NAME), Matchers.eq(TEST_TARBALL_NAME), Matchers.any(AssetBlob.class), Matchers
        .eq(null));
    Mockito.verifyNoMoreInteractions(npmFacet);
  }

  @Test
  public void testRootRestore() throws Exception {
    underTest.restore(repoRootProps, blob, TEST_BLOB_STORE_NAME, false);
    Mockito.verify(npmFacet).findRepositoryRootAsset();
    Mockito.verify(npmFacet).putRepositoryRoot(Matchers.any(AssetBlob.class), Matchers.eq(null));
    Mockito.verifyNoMoreInteractions(npmFacet);
  }

  @Test
  public void testRestoreSkipNotFacet() {
    Mockito.when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.empty());

    underTest.restore(packageProps, blob, TEST_BLOB_STORE_NAME, false);
    Mockito.verifyNoMoreInteractions(npmFacet);
  }

  @Test
  public void testRestoreSkipExistingPackage() throws Exception {
    Mockito.when(npmFacet.findPackageRootAsset(Matchers.eq(TEST_PACKAGE_NAME))).thenReturn(Mockito.mock(Asset.class));

    underTest.restore(packageProps, blob, TEST_BLOB_STORE_NAME, false);
    Mockito.verify(npmFacet).findPackageRootAsset(Matchers.eq(TEST_PACKAGE_NAME));
    Mockito.verify(npmFacet, Mockito.never()).putPackageRoot(Matchers.any(), Matchers.any(), Matchers.any());
    Mockito.verifyNoMoreInteractions(npmFacet);
  }

  @Test
  public void testRestoreDryRun() throws Exception {
    underTest.restore(packageProps, blob, TEST_BLOB_STORE_NAME, true);
    Mockito.verify(npmFacet).findPackageRootAsset(Matchers.eq(TEST_PACKAGE_NAME));
    Mockito.verify(npmFacet, Mockito.never()).putPackageRoot(Matchers.any(), Matchers.any(), Matchers.any());
    Mockito.verifyNoMoreInteractions(npmFacet);
  }

  @Test
  public void runNpmRepairComponentAfter() throws Exception {
    underTest.after(true, repository);
    
    Mockito.verify(npmRepairPackageRootComponent).repairRepository(repository);
  }

  @Test
  public void doNotRunNpmRepairComponentAfterWhenUpdatingAssetsDisabled() throws Exception {
    underTest.after(false, repository);

    Mockito.verify(npmRepairPackageRootComponent, Mockito.never()).repairRepository(repository);
  }
}
