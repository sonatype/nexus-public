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
package org.sonatype.nexus.repository.npm.internal.search.legacy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

import javax.inject.Inject;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.npm.internal.NpmFacetUtils;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.AssetManager;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.transaction.TransactionModule;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Supplier;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.orientechnologies.common.concur.lock.OModificationOperationProhibitedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.orient.testsupport.OrientExceptionMocker.mockOrientException;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.REPOSITORY_ROOT_ASSET;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

public class NpmSearchIndexFacetCachingTest
    extends TestSupport
{
  static class NpmSearchIndexFacetCachingImpl
      extends NpmSearchIndexFacetCaching
  {
    @Inject
    public NpmSearchIndexFacetCachingImpl(final EventManager eventManager, final AssetManager assetManager) {
      super(eventManager, assetManager);
    }

    @Override
    protected Content buildIndex(StorageTx tx, Path path) throws IOException {
      return new Content(new StringPayload("{}", ContentTypes.APPLICATION_JSON));
    }
  }

  @Mock
  private EventManager eventManager;

  @Mock
  private Repository repository;

  @Mock
  private StorageTx storageTx;

  @Mock
  private Bucket bucket;

  @Spy
  private Asset asset;

  private NestedAttributesMap assetAttributes;

  @Mock
  private BlobRef blobRef;

  @Mock
  private Blob blob;

  @Mock
  private AssetBlob assetBlob;

  @Mock
  private StorageFacet storageFacet;

  private NpmSearchIndexFacetCaching underTest;

  private Exception frozenException;

  private Supplier<StorageTx> supplierStorageTx = () -> storageTx;

  @Before
  public void setUp() throws Exception {
    when(repository.getName()).thenReturn("npm-test");
    when(repository.getFormat()).thenReturn(new NpmFormat());

    underTest = Guice.createInjector(new TransactionModule(), new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(EventManager.class).toInstance(eventManager);
      }
    }).getInstance(NpmSearchIndexFacetCachingImpl.class);
    underTest.attach(repository);

    frozenException = mockOrientException(OModificationOperationProhibitedException.class);

    assetAttributes = new NestedAttributesMap(MetadataNodeEntityAdapter.P_ATTRIBUTES, new HashMap<>());
    assetAttributes.child(Asset.CHECKSUM).set(HashAlgorithm.SHA1.name(), "1234567890123456789012345678901234567890");

    doReturn(blobRef).when(asset).requireBlobRef();
    doReturn(NpmFormat.NAME).when(asset).format();
    doReturn(assetAttributes).when(asset).attributes();
    asset.contentType(ContentTypes.APPLICATION_JSON);

    when(assetBlob.getBlob()).thenReturn(blob);

    when(storageTx.findBucket(repository)).thenReturn(bucket);
    when(storageTx.requireBlob(blobRef)).thenReturn(blob);
    UnitOfWork.begin(() -> storageTx);
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  public void testSearchIndex_FrozenDatabase_CachedIndex() throws Exception {
    when(storageTx.findAssetWithProperty(MetadataNodeEntityAdapter.P_NAME, NpmFacetUtils.REPOSITORY_ROOT_ASSET, bucket))
        .thenReturn(asset);
    doThrow(frozenException).when(storageTx).commit();
    assertThat(underTest.searchIndex(null), is(notNullValue()));
    verify(storageTx).commit();
  }

  @Test(expected = OModificationOperationProhibitedException.class)
  public void testSearchIndex_FrozenDatabase_NoCachedIndex() throws Exception {
    when(storageTx.createAsset(bucket, new NpmFormat())).thenReturn(asset);
    when(storageTx.createBlob(eq(NpmFacetUtils.REPOSITORY_ROOT_ASSET), any(), any(), any(), any(), anyBoolean()))
        .thenReturn(assetBlob);
    doThrow(frozenException).when(storageTx).commit();
    underTest.searchIndex(null);
  }

  @Test
  public void whenInvalidateCacheSearchIndex_Expect_Asset_Removed() {
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.txSupplier()).thenReturn(supplierStorageTx);
    when(storageTx.findAssetWithProperty(P_NAME, REPOSITORY_ROOT_ASSET, bucket)).thenReturn(asset);
    when(storageTx.findBucket(repository)).thenReturn(bucket);

    underTest.invalidateCachedSearchIndex();

    verify(storageTx, times(1)).deleteAsset(asset);
    verify(eventManager, times(1)).post(any(NpmSearchIndexInvalidatedEvent.class));
  }
}
