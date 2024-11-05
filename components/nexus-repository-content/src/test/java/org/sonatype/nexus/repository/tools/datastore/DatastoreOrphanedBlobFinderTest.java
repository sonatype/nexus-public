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
package org.sonatype.nexus.repository.tools.datastore;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Named;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.tools.OrphanedBlobFinder;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;

@Named
public class DatastoreOrphanedBlobFinderTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "repository";

  private static final String ASSET_NAME = "asset";

  private static final String USED_BLOB_ID = "e0acbed6-3e72-4e5f-9446-f542094ab73c";

  private static final String ORPHANED_BLOB_ID = "924c7ff1-58eb-4571-b68a-76f6a754a8bf";

  private static final String BLOB_STORE_NAME = "blobStore";

  private static final OffsetDateTime BLOB_CREATED_REF = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private FileBlobStore blobStore;

  @Mock
  private FileBlobStore blobStore2;

  @Mock
  private BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private Consumer<BlobId> orphanedBlobHandler;

  private OrphanedBlobFinder underTest;

  @Before
  public void setup() throws Exception {
    setupRepository(repository);

    when(blobStoreManager.get(BLOB_STORE_NAME)).thenReturn(blobStore);

    underTest = new DatastoreOrphanedBlobFinder(repositoryManager, blobStoreManager);
  }

  @Test
  public void detectOrphanedBlobsByRepository() {
    setupOrphanedBlob();

    underTest.detect(repository, orphanedBlobHandler);

    verify(orphanedBlobHandler).accept(new BlobId(ORPHANED_BLOB_ID, BLOB_CREATED_REF));
  }

  @Test
  public void deleteOrphanedBlobs() {
    setupOrphanedBlob();

    underTest.delete(repository);

    verify(blobStore).deleteHard(new BlobId(ORPHANED_BLOB_ID, BLOB_CREATED_REF));
  }

  @Test
  public void ignoreSoftDeletedBlobs() {
    setupOrphanedBlob(blobStore, true);

    underTest.delete(repository);

    verify(blobStore, never()).deleteHard(new BlobId(ORPHANED_BLOB_ID, BLOB_CREATED_REF));
  }

  @Test
  public void deleteOrphanedBlobsForAllBlobStores() {
    setupOrphanedBlob(blobStore, false);
    setupOrphanedBlob(blobStore2, false);

    when(blobStoreManager.browse()).thenReturn(ImmutableList.of(blobStore, blobStore2));

    underTest.delete();

    verify(blobStore).deleteHard(new BlobId(ORPHANED_BLOB_ID, BLOB_CREATED_REF));
    verify(blobStore2).deleteHard(new BlobId(ORPHANED_BLOB_ID, BLOB_CREATED_REF));
  }

  @Test
  public void deleteBlobIfRepositoryNotFound() {
    setupOrphanedBlob();

    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(null);

    underTest.delete(repository);

    verify(blobStore).deleteHard(new BlobId(ORPHANED_BLOB_ID, BLOB_CREATED_REF));
    verify(blobStore).deleteHard(new BlobId(USED_BLOB_ID, BLOB_CREATED_REF));
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwExceptionWhenRepositoryConfigurationAttributesNull() {
    Configuration config = repository.getConfiguration();
    when(config.getAttributes()).thenReturn(null);

    underTest.detect(repository, orphanedBlobHandler);
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwExceptionWhenRepositoryStorageConfigurationNull() {
    Configuration config = repository.getConfiguration();
    when(config.getAttributes()).thenReturn(emptyMap());

    underTest.detect(repository, orphanedBlobHandler);
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwExceptionWhenBlobStoreNameBlank() {
    Configuration config = repository.getConfiguration();
    when(config.getAttributes()).thenReturn(of("storage", of("blobStoreName", "")));

    underTest.detect(repository, orphanedBlobHandler);
  }

  private void setupOrphanedBlob() {
    setupOrphanedBlob(blobStore, false);
  }

  private void setupOrphanedBlob(final BlobStore blobStore, final boolean deleted) {
    when(blobStore.getBlobIdStream())
        .thenAnswer(i -> Stream.of(new BlobId(ORPHANED_BLOB_ID, BLOB_CREATED_REF), new BlobId(USED_BLOB_ID, BLOB_CREATED_REF)));
    when(blobStore.getBlobAttributes(new BlobId(ORPHANED_BLOB_ID, BLOB_CREATED_REF)))
        .thenReturn(new TestBlobAttributes(deleted));
    when(blobStore.getBlobAttributes(new BlobId(USED_BLOB_ID, BLOB_CREATED_REF))).thenReturn(new TestBlobAttributes(deleted));

    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);

    FluentAsset asset = buildAssetWithBlobId(USED_BLOB_ID);

    setupTransactionToFindAsset(asset);
  }

  private FluentAsset buildAssetWithBlobId(final String usedBlob) {
    FluentAsset asset = mock(FluentAsset.class);
    BlobRef blobRef = new BlobRef("node", "store", usedBlob);

    AssetBlob assetBlob = mock(AssetBlob.class);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(blobRef);

    return asset;
  }

  private void setupTransactionToFindAsset(final FluentAsset asset) {
    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);

    FluentAssets fluentAssets = mock(FluentAssets.class);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    FluentAssetBuilder builder = mock(FluentAssetBuilder.class);
    when(fluentAssets.path(ASSET_NAME)).thenReturn(builder);
    when(builder.find()).thenReturn(Optional.of(asset));
  }

  private void setupRepository(final Repository repository) {
    Configuration repositoryConfiguration = mock(Configuration.class);
    when(repositoryConfiguration.getAttributes()).thenReturn(
        of("storage", of("blobStoreName", BLOB_STORE_NAME))
    );
    when(repository.getConfiguration()).thenReturn(repositoryConfiguration);
  }

  private static class TestBlobAttributes
      implements BlobAttributes
  {
    private final boolean deleted;

    TestBlobAttributes(final boolean deleted) {
      this.deleted = deleted;
    }

    @Override
    public Map<String, String> getHeaders() {
      return of(REPO_NAME_HEADER, REPOSITORY_NAME, BLOB_NAME_HEADER, ASSET_NAME);
    }

    @Override
    public BlobMetrics getMetrics() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isDeleted() {
      return deleted;
    }

    @Override
    public void setDeleted(final boolean deleted) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setDeletedReason(final String deletedReason) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getDeletedReason() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setDeletedDateTime(final DateTime deletedDateTime) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public DateTime getDeletedDateTime() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Properties getProperties() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void updateFrom(final BlobAttributes blobAttributes) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void store() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void writeProperties() {
      throw new UnsupportedOperationException("Not implemented");
    }
  }
}
