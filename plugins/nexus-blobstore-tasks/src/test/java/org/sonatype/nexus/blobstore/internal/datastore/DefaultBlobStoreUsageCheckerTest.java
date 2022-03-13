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
package org.sonatype.nexus.blobstore.internal.datastore;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.test.util.Whitebox;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;

public class DefaultBlobStoreUsageCheckerTest
    extends TestSupport
{
  private static final String REPO_NAME = "repoName";

  private static final String NODE_ID = "repoName";

  private static final String DEFAULT = "default";

  private static final String NOT_DEFAULT = "notADefault";

  private static final String BLOB_ID_STRING = "86e20baa-0bca-4915-a7dc-9a4f34e72321";

  private static final BlobId BLOB_ID = new BlobId(BLOB_ID_STRING);

  private static final String BLOB_NAME = "/fake/blob.name";

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  BlobStore blobStore;

  @Mock
  Repository repository;

  @Mock
  BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  AssetBlobStore assetBlobStore;

  @Mock
  Blob blob;

  @Mock
  AssetBlob assetBlob;

  @Mock
  ContentFacetSupport contentFacet;

  @Mock
  ContentFacetStores contentFacetStores;

  DefaultBlobStoreUsageChecker underTest;

  @Before
  public void setUp() {
    Whitebox.setInternalState(contentFacetStores, "assetBlobStore", assetBlobStore);

    when(contentFacet.stores()).thenReturn(contentFacetStores);
    when(contentFacet.nodeName()).thenReturn(NODE_ID);

    BlobRef blobRef = new BlobRef(NODE_ID, DEFAULT, BLOB_ID.asUniqueString());

    when(blobStoreConfiguration.getName()).thenReturn(DEFAULT);

    when(blobStore.get(BLOB_ID)).thenReturn(blob);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);

    Map<String, String> headers = new HashMap<>();
    headers.put(REPO_NAME_HEADER, REPO_NAME);
    when(blob.getHeaders()).thenReturn(headers);

    when(repositoryManager.get(REPO_NAME)).thenReturn(repository);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);

    when(assetBlobStore.readAssetBlob(any())).thenReturn(empty());
    when(assetBlobStore.readAssetBlob(eq(blobRef))).thenReturn(of(assetBlob));

    underTest = new DefaultBlobStoreUsageChecker(repositoryManager);
  }

  @Test
  public void blobIsReferenced() {
    assertThat(underTest.test(blobStore, BLOB_ID, BLOB_NAME), equalTo(true));
  }

  @Test
  public void blobIdDoesNotMatch() {
    assertThat(underTest.test(blobStore, new BlobId("0"), BLOB_NAME), equalTo(false));
  }

  @Test
  public void blobStoreNameDoesNotMatch() {
    when(blobStoreConfiguration.getName()).thenReturn(NOT_DEFAULT);

    assertThat(underTest.test(blobStore, BLOB_ID, BLOB_NAME), equalTo(false));
  }
}
