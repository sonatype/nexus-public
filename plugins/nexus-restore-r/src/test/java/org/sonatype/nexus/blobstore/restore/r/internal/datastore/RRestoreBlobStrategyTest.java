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
package org.sonatype.nexus.blobstore.restore.r.internal.datastore;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.r.datastore.RContentFacet;

import org.joda.time.DateTime;
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

public class RRestoreBlobStrategyTest
    extends TestSupport
{
  private static final String TEST_BLOB_STORE_NAME = "test";

  private static final String ARCHIVE_PATH = "src/contrib/curl_4.2.tar.gz";

  private static final String METADATA_PATH = "src/contrib/PACKAGES.gz";

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  private RestoreBlobData restoreBlobData;

  @Mock
  BlobStore blobStore;

  @Mock
  Blob blob;

  @Mock
  Repository repository;

  @Mock
  RContentFacet rContentFacet;

  @Mock
  BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  private BlobId blobId;

  @Mock
  private BlobAttributes blobAttributes;

  @Mock
  ContentFacet contentFacet;

  @Mock
  FluentAssets assets;

  @Mock
  FluentAssetBuilder fluentAssetBuilder;

  @Mock
  BlobMetrics blobMetrics;

  @Mock
  FluentAsset fluentAsset;

  @Mock
  private FluentComponent fluentComponent;

  @Mock
  private AssetBlob assetBlob;

  private Properties properties = new Properties();

  private RRestoreBlobStrategy restoreBlobStrategy;

  @Before
  public void setup() {
    restoreBlobStrategy = new RRestoreBlobStrategy(new DryRunPrefix("dryrun"), repositoryManager);

    when(repositoryManager.get(anyString())).thenReturn(repository);
    when(repository.optionalFacet(RContentFacet.class)).thenReturn(Optional.of(rContentFacet));

    when(restoreBlobData.getRepository()).thenReturn(repository);

    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);

    when(blob.getId()).thenReturn(blobId);
    when(blobAttributes.isDeleted()).thenReturn(false);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(assets);
    when(assets.path(anyString())).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.find()).thenReturn(Optional.empty());
    when(blob.getMetrics()).thenReturn(blobMetrics);

    when(fluentAsset.component()).thenReturn(Optional.of(fluentComponent));
    when(fluentAsset.blob()).thenReturn(Optional.of(assetBlob));

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
    RestoreBlobData restoreData = restoreBlobStrategy.createRestoreData(properties, blob, blobStore);
    assertThat(restoreData.getBlob(), is(blob));
    assertThat(restoreData.getRepository(), is(repository));
    assertThat(restoreData.getBlobStore(), is(blobStore));
  }

  @Test
  public void testCanAttemptRestore() {
    RestoreBlobData restoreBlobData = restoreBlobStrategy.createRestoreData(properties, blob, blobStore);
    assertTrue(restoreBlobStrategy.canAttemptRestore(restoreBlobData));
  }

  @Test
  public void testCannotAttemptRestoreNotFoundFacet() {
    when(repository.optionalFacet(RContentFacet.class)).thenReturn(Optional.empty());
    RestoreBlobData restoreBlobData = restoreBlobStrategy.createRestoreData(properties, blob, blobStore);
    assertFalse(restoreBlobStrategy.canAttemptRestore(restoreBlobData));
  }

  @Test
  public void testAppropriatePathIsReturned() {
    RestoreBlobData restoreBlobData = restoreBlobStrategy.createRestoreData(properties, blob, blobStore);
    assertThat(restoreBlobStrategy.getAssetPath(restoreBlobData), is(ARCHIVE_PATH));
  }

  @Test
  public void testPackageIsRestored() throws Exception {
    when(repository.facet(RContentFacet.class)).thenReturn(rContentFacet);
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(rContentFacet).putPackage(any(), eq(ARCHIVE_PATH));
    verifyNoMoreInteractions(rContentFacet);
  }

  @Test
  public void testMetadataIsRestored() throws Exception {
    when(repository.facet(RContentFacet.class)).thenReturn(rContentFacet);
    properties.setProperty("@BlobStore.blob-name", METADATA_PATH);
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(rContentFacet).putMetadata(any(), eq(METADATA_PATH), any());
    verifyNoMoreInteractions(rContentFacet);
  }

  @Test
  public void shouldSkipDeletedBlob() {
    when(blobAttributes.isDeleted()).thenReturn(true);
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verifyNoMoreInteractions(rContentFacet);
  }

  @Test
  public void shouldSkipOlderBlob() {
    when(repository.facet(RContentFacet.class)).thenReturn(rContentFacet);
    when(blobAttributes.isDeleted()).thenReturn(false);
    when(fluentAssetBuilder.find()).thenReturn(Optional.of(fluentAsset));
    when(assetBlob.blobCreated()).thenReturn(OffsetDateTime.now());
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now().minusDays(1));
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verifyNoMoreInteractions(rContentFacet);
  }

  @Test
  public void shouldRestoreMoreRecentBlob() {
    when(repository.facet(RContentFacet.class)).thenReturn(rContentFacet);
    when(fluentAssetBuilder.find()).thenReturn(Optional.of(fluentAsset));
    when(assetBlob.blobCreated()).thenReturn(OffsetDateTime.now().minusDays(1));
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now());

    when(blobAttributes.isDeleted()).thenReturn(false);
    restoreBlobStrategy.restore(properties, blob, blobStore, false);
    verify(rContentFacet).putPackage(any(), eq(ARCHIVE_PATH));
    verifyNoMoreInteractions(rContentFacet);
  }
}
