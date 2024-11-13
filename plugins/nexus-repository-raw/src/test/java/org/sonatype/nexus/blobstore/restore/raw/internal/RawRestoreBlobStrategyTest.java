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
package org.sonatype.nexus.blobstore.restore.raw.internal;

import java.io.IOException;
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
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.content.raw.RawContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.handlers.LastDownloadedAttributeHandler;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;

public class RawRestoreBlobStrategyTest
    extends TestSupport
{
  private static final String TEST_BLOB_STORE_NAME = "test";

  private static final String REPOSITORY_NAME = "theRepository";

  private static final String BLOB_PATH = "/blob/path/end";

  private static final boolean DRY_RUN = true;

  @Mock
  private RawContentFacet rawContentFacet;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository repository;

  @Mock
  private Blob blob;

  @Mock
  private BlobId blobId;

  @Mock
  private BlobAttributes blobAttributes;

  @Mock
  private AssetBlob assetBlob;

  @Mock
  private BlobMetrics blobMetrics;

  @Mock
  private BlobStore blobStore;

  @Mock
  private BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  private FluentAssets assets;

  @Mock
  private FluentAssetBuilder fluentAssetBuilder;

  @Mock
  private FluentAsset asset;

  @Mock
  private FluentComponent component;

  private final DryRunPrefix dryRunPrefix = new DryRunPrefix("DRY RUN");

  private Properties properties;

  private RawRestoreBlobStrategy underTest;

  @Before
  public void setup() {
    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);

    when(repository.optionalFacet(RawContentFacet.class)).thenReturn(of(rawContentFacet));
    when(repository.facet(RawContentFacet.class)).thenReturn(rawContentFacet);
    when(repository.facet(ContentFacet.class)).thenReturn(rawContentFacet);

    when(rawContentFacet.assets()).thenReturn(assets);
    when(assets.path(anyString())).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.find()).thenReturn(Optional.of(asset));

    when(asset.component()).thenReturn(empty());
    when(asset.blob()).thenReturn(Optional.of(assetBlob));

    when(blob.getId()).thenReturn(blobId);
    when(blob.getMetrics()).thenReturn(blobMetrics);

    when(blobAttributes.isDeleted()).thenReturn(false);

    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);

    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME);

    properties = new Properties();
    properties.put(HEADER_PREFIX + BlobStore.REPO_NAME_HEADER, REPOSITORY_NAME);
    properties.put(HEADER_PREFIX + BLOB_NAME_HEADER, BLOB_PATH);
    properties.put(HEADER_PREFIX + CONTENT_TYPE_HEADER, "testContentType");

    underTest = new RawRestoreBlobStrategy(dryRunPrefix, repositoryManager);
    underTest.injectDependencies(mock(LastDownloadedAttributeHandler.class));
  }

  @Test
  public void testRestoreWhenNoContentFacet() {
    when(repository.optionalFacet(RawContentFacet.class)).thenReturn(empty());
    when(repository.facet(RawContentFacet.class)).thenReturn(null);
    when(repository.facet(ContentFacet.class)).thenReturn(null);

    underTest.restore(properties, blob, blobStore, !DRY_RUN);
    verifyNoMoreInteractions(rawContentFacet);
  }

  @Test
  public void testRestoreWhenNoAssetExists() throws IOException {
    when(fluentAssetBuilder.find()).thenReturn(empty());

    underTest.restore(properties, blob, blobStore, !DRY_RUN);
    verify(asset, never()).delete();
    verify(rawContentFacet, times(1)).put(eq(BLOB_PATH), any());
  }

  @Test
  public void testRestoreWhenComponentAndAssetExist() throws IOException {
    when(asset.component()).thenReturn(of(component));

    underTest.restore(properties, blob, blobStore, !DRY_RUN);
    verify(asset,never()).delete();
    verify(rawContentFacet, never()).put(eq(BLOB_PATH), any());
  }

  @Test
  public void testRestoreWhenDryRun() throws IOException {
    underTest.restore(properties, blob, blobStore, DRY_RUN);
    verify(asset, never()).delete();
    verify(rawContentFacet, never()).put(anyString(), any());
  }

  @Test
  public void testRestoreCreatesAsset() throws Exception {
    underTest.restore(properties, blob, blobStore, !DRY_RUN);

    verify(asset, times(1)).delete();
    verify(rawContentFacet, times(1)).put(eq(BLOB_PATH), any());
  }

  @Test
  public void shouldSkipDeletedBlob() throws Exception {
    when(blobAttributes.isDeleted()).thenReturn(true);
    underTest.restore(properties, blob, blobStore, false);
    verifyNoMoreInteractions(rawContentFacet);
    verify(asset, never()).delete();
    verify(rawContentFacet, never()).put(eq(BLOB_PATH), any());
  }

  @Test
  public void shouldSkipOlderBlob() throws Exception {
    when(asset.component()).thenReturn(of(component));
    when(assetBlob.blobCreated()).thenReturn(OffsetDateTime.now());
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now().minusDays(1));
    underTest.restore(properties, blob, blobStore, false);
    verify(asset, never()).delete();
    verify(rawContentFacet, never()).put(eq(BLOB_PATH), any());
  }

  @Test
  public void shouldRestoreMoreRecentBlob() throws Exception {
    when(asset.component()).thenReturn(of(component));
    when(assetBlob.blobCreated()).thenReturn(OffsetDateTime.now().minusDays(1));
    when(blobMetrics.getCreationTime()).thenReturn(DateTime.now());
    underTest.restore(properties, blob, blobStore, false);
    verify(asset, times(1)).delete();
    verify(rawContentFacet, times(1)).put(eq(BLOB_PATH), any());
  }
}
