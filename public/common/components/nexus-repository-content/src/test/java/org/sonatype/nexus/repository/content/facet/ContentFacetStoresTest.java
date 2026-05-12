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
package org.sonatype.nexus.repository.content.facet;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ContentFacetStoresTest
{
  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStore blobStore;

  @Mock
  private FormatStoreManager formatStoreManager;

  @Before
  public void setUp() {
    when(blobStoreManager.get(anyString())).thenReturn(blobStore);
  }

  @Test
  public void testStoreCreation() {
    ContentFacetStores underTest = new ContentFacetStores(
        blobStoreManager, "default", formatStoreManager, "testStoreName");

    assertThat(underTest.blobStoreName, is("default"));
    assertThat(underTest.contentStoreName, is("testStoreName"));
    assertThat(underTest.blobStoreProvider, is(notNullValue()));
  }

  @Test
  public void testBlobStoreProviderLazyLoads() {
    ContentFacetStores underTest = new ContentFacetStores(
        blobStoreManager, "my-store", formatStoreManager, "content");

    BlobStore result = underTest.blobStoreProvider.get();
    assertThat(result, is(blobStore));
  }

  @Test
  public void testAssetStoreAccessor() {
    ContentFacetStores underTest = new ContentFacetStores(
        blobStoreManager, "default", formatStoreManager, "testStoreName");

    assertThat(underTest.assetStore(), is(underTest.assetStore));
  }

  @Test
  public void testAssetBlobStoreAccessor() {
    ContentFacetStores underTest = new ContentFacetStores(
        blobStoreManager, "default", formatStoreManager, "testStoreName");

    assertThat(underTest.assetBlobStore(), is(underTest.assetBlobStore));
  }
}
