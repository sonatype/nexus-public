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
package org.sonatype.nexus.blobstore.restore.pypi.internal;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.pypi.PyPiFacet;
import org.sonatype.nexus.repository.pypi.repair.PyPiRepairIndexComponent;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

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
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;

public class PyPiRestoreBlobStrategyTest
    extends TestSupport
{

  private static final String TEST_BLOB_STORE_NAME = "test";

  public static final String PACKAGE_PATH = "packages/sampleproject/1.2.0/sampleproject-1.2.0.tar.gz";

  public static final String INDEX_PATH = "simple/peppercorn/";

  PyPiRestoreBlobStrategy underTest;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  Blob blob;

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
  PyPiRepairIndexComponent pyPiRepairIndexComponent;

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
    underTest = new PyPiRestoreBlobStrategy(nodeAccess,
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

    when(storageTx.findBucket(repository)).thenReturn(bucket);

    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream(blobBytes));

    when(blobStoreManager.get(TEST_BLOB_STORE_NAME)).thenReturn(blobStore);

    when(pyPiRestoreBlobDataFactory.create(any())).thenReturn(pyPiRestoreBlobData);
    when(pyPiRestoreBlobData.getBlobData()).thenReturn(restoreBlobData);
  }

  @Test
  public void testPackageRestore() throws Exception {
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(pyPiRestoreBlobData.getBlobData().getBlobName()).thenReturn(PACKAGE_PATH);

    underTest.restore(packageProps, blob, TEST_BLOB_STORE_NAME, false);

    verify(pyPiFacet).assetExists(PACKAGE_PATH);
    verify(pyPiFacet).put(eq(PACKAGE_PATH), any(AssetBlob.class));

    verifyNoMoreInteractions(pyPiFacet);
  }

  @Test
  public void testIndexRestore() throws Exception {
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(pyPiRestoreBlobData.getBlobData().getBlobName()).thenReturn(INDEX_PATH);

    underTest.restore(indexProps, blob, TEST_BLOB_STORE_NAME, false);

    verify(pyPiFacet).assetExists(INDEX_PATH);
    verify(pyPiFacet).put(eq(INDEX_PATH), any(AssetBlob.class));

    verifyNoMoreInteractions(pyPiFacet);
  }

  @Test
  public void testRestoreSkipNotFacet() {
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.empty());

    underTest.restore(indexProps, blob, TEST_BLOB_STORE_NAME, false);

    verifyNoMoreInteractions(pyPiFacet);
  }

  @Test
  public void testRestoreSkipExistingPackage() {
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(pyPiRestoreBlobData.getBlobData().getBlobName()).thenReturn(PACKAGE_PATH);
    when(pyPiFacet.assetExists(PACKAGE_PATH)).thenReturn(true);

    underTest.restore(packageProps, blob, TEST_BLOB_STORE_NAME, false);

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
}
