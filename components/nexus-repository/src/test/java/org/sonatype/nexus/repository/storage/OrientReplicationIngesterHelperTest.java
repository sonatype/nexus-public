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
package org.sonatype.nexus.repository.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.replication.ReplicationIngestionException;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrientReplicationIngesterHelperTest
    extends TestSupport
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private Blob blob;

  @Mock
  private BlobId blobId;

  @Mock
  private BlobStore blobStore;

  @Mock
  private ReplicationFacet replicationFacet;

  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private BlobAttributes blobAttributes;

  // Mocking so we can verify the put call
  @Mock
  Map<String, String> headers;

  @Mock
  private MockedStatic<UnitOfWork> unitOfWork;

  private InputStream contentInputStream;

  private OrientReplicationIngesterHelper underTest;

  @Before
  public void setup() {
    contentInputStream = new ByteArrayInputStream("someContent".getBytes());
    when(blob.getInputStream()).thenReturn(contentInputStream);
    when(headers.get("BlobStore.blob-name")).thenReturn("/somePath");
    when(headers.get("BlobStore.content-type")).thenReturn("someContentType");
    when(blobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);
    when(blobStoreManager.get("blobStoreId")).thenReturn(blobStore);
    when(blobAttributes.getHeaders()).thenReturn(headers);
    when(repositoryManager.get("repoName")).thenReturn(repository);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(repository.facet(ReplicationFacet.class)).thenReturn(replicationFacet);
    when(blobId.toString()).thenReturn("blobId");
    when(blob.getId()).thenReturn(blobId);
    underTest = new OrientReplicationIngesterHelper(blobStoreManager, nodeAccess, repositoryManager);
  }

  @Test
  public void testReplicateNullRepo() throws IOException {
    when(repositoryManager.get("repoName")).thenReturn(null);
    ReplicationIngestionException e =
        assertThrows(ReplicationIngestionException.class,
            () -> underTest.replicate("blobStoreId", blob, null, null, "repoName", "blobStoreName"));
    assertThat(e.getMessage(), is("Can't replicate blob blobId, the repository repoName doesn't exist"));
  }

  @Test
  public void testReplicate() throws IOException {
    Map<String, Object> assetAttributes = new HashMap<>();
    Map<String, Object> componentAttributes = new HashMap<>();
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<AssetBlob> assetBlobCaptor = ArgumentCaptor.forClass(AssetBlob.class);
    ArgumentCaptor<NestedAttributesMap> assetAttributesCaptor = ArgumentCaptor.forClass(NestedAttributesMap.class);
    ArgumentCaptor<NestedAttributesMap> componentAttributesCaptor = ArgumentCaptor.forClass(NestedAttributesMap.class);

    underTest.replicate("blobStoreId", blob, assetAttributes, componentAttributes, "repoName", "blobStoreName");

    verify(headers, times(1)).put("Bucket.repo-name", "repoName");
    verify(blobAttributes, times(1)).store();
    verify(replicationFacet).replicate(pathCaptor.capture(), assetBlobCaptor.capture(), assetAttributesCaptor.capture(),
        componentAttributesCaptor.capture());

    assertThat(pathCaptor.getValue(), is("/somePath"));
    assertThat(assetAttributesCaptor.getValue().backing(), is(assetAttributes));
    assertThat(componentAttributesCaptor.getValue().backing(), is(componentAttributes));
  }

  @Test
  public void testDeleteReplicationNullRepo() {
    when(repositoryManager.get("repoName")).thenReturn(null);
    ReplicationIngestionException e =
        assertThrows(ReplicationIngestionException.class,
            () -> underTest.deleteReplication("/path", "repoName"));
    assertThat(e.getMessage(), is("Can't delete blob in path /path as the repository repoName doesn't exist"));
  }

  @Test
  public void testDeleteReplication() {
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    underTest.deleteReplication("/path", "repoName");

    verify(replicationFacet).replicateDelete(pathCaptor.capture());
    assertThat(pathCaptor.getValue(), is("/path"));
  }
}
