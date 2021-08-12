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
package org.sonatype.nexus.repository.content.replication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.replication.ReplicationIngestionException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

public class ReplicationIngesterHelperImplTest extends TestSupport
{

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private BlobStoreManager blobStoreManager;

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
  private BlobAttributes blobAttributes;

  // Mocking so we can verify the put call
  @Mock
  Map<String, String> headers;

  private ReplicationIngesterHelperImpl underTest;


  @Before
  public void setup() {
    when(headers.get("BlobStore.blob-name")).thenReturn("/somePath");
    when(blobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);
    when(blobStoreManager.get("blobStoreName")).thenReturn(blobStore);
    when(blobAttributes.getHeaders()).thenReturn(headers);
    when(repositoryManager.get("repoName")).thenReturn(repository);
    when(repository.facet(ReplicationFacet.class)).thenReturn(replicationFacet);
    when(blobId.toString()).thenReturn("blobId");
    when(blob.getId()).thenReturn(blobId);
    underTest = new ReplicationIngesterHelperImpl(repositoryManager, blobStoreManager);
  }

  @Test
  public void testReplicateNullRepo() throws IOException {
    when(repositoryManager.get("repoName")).thenReturn(null);
    ReplicationIngestionException e =
        assertThrows(ReplicationIngestionException.class,
            () -> underTest.replicate("blobStoreId", blob, null, null, "repoName", "blobStoreName"));
    assertThat(e.getMessage(), is("Can't replicate blob blobId as the repository repoName doesn't exist"));
  }

  @Test
  public void testReplicate() throws IOException {
    Map<String, Object> assetAttributes = new HashMap<>();
    Map<String, Object> componentAttributes = new HashMap<>();
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Blob> blobCaptor = ArgumentCaptor.forClass(Blob.class);
    ArgumentCaptor<Map> assetAttributesCaptor = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<Map> componentAttributesCaptor = ArgumentCaptor.forClass(Map.class);

    underTest.replicate("blobStoreId", blob, assetAttributes, componentAttributes, "repoName", "blobStoreName");

    verify(headers, times(1)).put("Bucket.repo-name", "repoName");
    verify(blobAttributes, times(1)).store();
    verify(replicationFacet).replicate(pathCaptor.capture(), blobCaptor.capture(), assetAttributesCaptor.capture(),
        componentAttributesCaptor.capture());
    assertThat(pathCaptor.getValue(), is("/somePath"));
    assertThat(blobCaptor.getValue(), is(blob));
    assertThat(assetAttributesCaptor.getValue(), is(assetAttributes));
    assertThat(componentAttributesCaptor.getValue(), is(componentAttributes));
  }

  @Test
  public void testDeleteReplicationNullRepo() {
    when(repositoryManager.get("repoName")).thenReturn(null);
    ReplicationIngestionException e =
        assertThrows(ReplicationIngestionException.class,
            () -> underTest.deleteReplication("/path", "repoName"));
    assertThat(e.getMessage(), is("Can't delete blob in path /path, the repository repoName doesn't exist"));
  }

  @Test
  public void testDeleteReplication() {
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    underTest.deleteReplication("/path", "repoName");

    verify(replicationFacet).replicateDelete(pathCaptor.capture());
    assertThat(pathCaptor.getValue(), is("/path"));
  }
}
