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
package org.sonatype.nexus.blobstore.restore.helm.internal;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.repository.helm.datastore.HelmRestoreFacet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @since 3.next
 */
public class HelmRestoreBlobStrategyTest
    extends TestSupport
{
  private static final String TEST_BLOB_STORE_NAME = "test";

  private static final String ARCHIVE_PATH = "mongodb-7.8.10.tgz";

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  Repository repository;

  @Mock
  HelmRestoreFacet helmRestoreFacet;

  @Mock
  ContentFacet contentFacet;

  @Mock
  FluentAssets assets;

  @Mock
  FluentAssetBuilder fluentAssetBuilder;

  @Mock
  Blob blob;

  @Mock
  BlobStore blobStore;

  @Mock
  BlobStoreConfiguration blobStoreConfiguration;

  private Properties properties = new Properties();

  private HelmRestoreBlobStrategy restoreBlobStrategy;

  @Before
  public void setup() throws IOException {
    restoreBlobStrategy = new HelmRestoreBlobStrategy(new DryRunPrefix("dryrun"), repositoryManager);

    when(repositoryManager.get(anyString())).thenReturn(repository);
    when(repository.facet(HelmRestoreFacet.class)).thenReturn(helmRestoreFacet);
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.optionalFacet(HelmRestoreFacet.class)).thenReturn(Optional.of(helmRestoreFacet));
    when(helmRestoreFacet.isRestorable(any())).thenReturn(true);

    when(contentFacet.assets()).thenReturn(assets);
    when(assets.path(anyString())).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.find()).thenReturn(Optional.empty());

    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);

    properties.setProperty("@BlobStore.created-by", "anonymous");
    properties.setProperty("size", "24900");
    properties.setProperty("@Bucket.repo-name", "helm-proxy");
    properties.setProperty("creationTime", "1533220387218");
    properties.setProperty("@BlobStore.created-by-ip", "127.0.0.1");
    properties.setProperty("@BlobStore.content-type", "application/x-tgz");
    properties.setProperty("@BlobStore.blob-name", ARCHIVE_PATH);
  }

  @Test
  public void testCanAttemptRestore() {
    HelmRestoreBlobData restoreBlobData = restoreBlobStrategy.createRestoreData(properties, blob, blobStore);
    assertTrue(restoreBlobStrategy.canAttemptRestore(restoreBlobData));
  }

  @Test
  public void testCannotAttemptRestoreNotFoundFacet() {
    when(repository.optionalFacet(HelmRestoreFacet.class)).thenReturn(Optional.empty());
    HelmRestoreBlobData restoreBlobData = restoreBlobStrategy.createRestoreData(properties, blob, blobStore);
    assertFalse(restoreBlobStrategy.canAttemptRestore(restoreBlobData));
  }

  @Test
  public void testCannotAttemptRestorePackageIsNotRestorable() {
    when(helmRestoreFacet.isRestorable(any())).thenReturn(false);
    HelmRestoreBlobData restoreBlobData = restoreBlobStrategy.createRestoreData(properties, blob, blobStore);
    assertFalse(restoreBlobStrategy.canAttemptRestore(restoreBlobData));
  }

  @Test
  public void testBlobDataIsCreated() {
    HelmRestoreBlobData restoreBlobData = restoreBlobStrategy.createRestoreData(properties, blob, blobStore);
    assertThat(restoreBlobData.getBlob(), is(blob));
    assertThat(restoreBlobData.getRepository(), is(repository));
    assertThat(restoreBlobData.getBlobStore(), is(blobStore));
  }

  @Test
  public void testAppropriatePathIsReturned() {
    HelmRestoreBlobData restoreBlobData = restoreBlobStrategy.createRestoreData(properties, blob, blobStore);
    assertThat(restoreBlobStrategy.getAssetPath(restoreBlobData), is(ARCHIVE_PATH));
  }

  @Test
  public void testPackageIsRestored() throws Exception {
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(helmRestoreFacet).restore(any(), eq(ARCHIVE_PATH));
    verify(helmRestoreFacet).isRestorable(eq(ARCHIVE_PATH));
    verifyNoMoreInteractions(helmRestoreFacet);
  }
}
