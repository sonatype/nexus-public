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
package org.sonatype.nexus.blobstore.restore.datastore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.handlers.LastDownloadedAttributeHandler;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.google.common.base.Throwables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;

@ExtendWith(MockitoExtension.class)
class BaseRestoreBlobStrategyTest

{
  private static final String BLOB_STORE_NAME = "test-blobstore";

  private static final String REPO_NAME = "test-repo";

  private static final String BLOB_NAME = "/test/blob.jar";

  private static final String ASSET_PATH = "/test/blob.jar";

  @Mock
  private DryRunPrefix dryRunPrefix;

  @Mock
  private LastDownloadedAttributeHandler lastDownloadedAttributeHandler;

  @Mock
  private Blob blob;

  @Mock
  private BlobStore blobStore;

  @Mock
  private BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  private Repository repository;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private BlobAttributes blobAttributes;

  @Mock
  private BlobId blobId;

  @Mock
  private BlobMetrics blobMetrics;

  @Mock
  private Format format;

  private Properties properties;

  private TestRestoreBlobStrategy underTest;

  @BeforeEach
  void setup() {
    underTest = new TestRestoreBlobStrategy(dryRunPrefix);
    underTest.injectDependencies(lastDownloadedAttributeHandler);

    properties = new Properties();
    properties.setProperty(HEADER_PREFIX + REPO_NAME_HEADER, REPO_NAME);
    properties.setProperty(HEADER_PREFIX + BLOB_NAME_HEADER, BLOB_NAME);

    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStoreConfiguration.getName()).thenReturn(BLOB_STORE_NAME);
    when(blob.getId()).thenReturn(blobId);
    when(repository.getName()).thenReturn(REPO_NAME);
    when(repositoryManager.get(REPO_NAME)).thenReturn(repository);
    when(blobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);
    when(blobAttributes.isDeleted()).thenReturn(false);
    when(lastDownloadedAttributeHandler.readLastDownloadedAttribute(anyString(), any(Blob.class))).thenReturn(null);
  }

  @Test
  void testRestore_propagatesIOException() {
    FluentAssets fluentAssets = mock(FluentAssets.class);
    FluentAssetBuilder fluentAssetBuilder = mock();

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.path(ASSET_PATH)).thenReturn(fluentAssetBuilder);

    underTest.exception = new IOException("Test IO exception");

    UncheckedIOException exception = assertThrows(
        UncheckedIOException.class,
        () -> underTest.restore(properties, blob, blobStore, false));

    assertThat(exception.getCause(), instanceOf(IOException.class));
    assertThat(exception.getCause().getMessage(), is("Test IO exception"));
  }

  @Test
  void testRestore_propagatesNPE() {
    FluentAssets fluentAssets = mock(FluentAssets.class);
    FluentAssetBuilder fluentAssetBuilder = mock();

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.path(ASSET_PATH)).thenReturn(fluentAssetBuilder);

    underTest.exception = new NullPointerException("NPE");

    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> underTest.restore(properties, blob, blobStore, false));

    assertThat(exception.getMessage(), is("NPE"));
  }

  /**
   * Concrete test implementation of BaseRestoreBlobStrategy for testing purposes
   */
  private class TestRestoreBlobStrategy
      extends BaseRestoreBlobStrategy<DataStoreRestoreBlobData>
  {
    Exception exception;

    protected TestRestoreBlobStrategy(final DryRunPrefix dryRunPrefix) {
      super(dryRunPrefix);
    }

    @Override
    protected boolean canAttemptRestore(@Nonnull final DataStoreRestoreBlobData data) {
      return true;
    }

    @Override
    protected void createAssetFromBlob(final Blob assetBlob, final DataStoreRestoreBlobData data) throws IOException {
      Throwables.propagateIfPossible(exception, IOException.class);
    }

    @Override
    protected String getAssetPath(@Nonnull final DataStoreRestoreBlobData data) {
      return ASSET_PATH;
    }

    @Override
    protected DataStoreRestoreBlobData createRestoreData(
        final Properties properties,
        final Blob blob,
        final BlobStore blobStore)
    {
      return new DataStoreRestoreBlobData(blob, properties, blobStore, repositoryManager);
    }

    @Override
    protected boolean isComponentRequired(final DataStoreRestoreBlobData data) {
      return false;
    }

    @Override
    public void after(final boolean updateAssets, final Repository repository) {

    }
  }
}
