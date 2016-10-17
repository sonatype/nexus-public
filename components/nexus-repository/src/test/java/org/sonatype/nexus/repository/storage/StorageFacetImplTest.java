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
import java.io.InputStream;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.storage.StorageFacetImpl.Config;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.security.ClientInfoProvider;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import static com.google.common.hash.HashCode.fromString;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

/**
 * Unit tests for {@link StorageFacetImpl}.
 */
public class StorageFacetImplTest
    extends TestSupport
{
  private static final String NODE_ID = "testNodeId";

  private static final String BLOB_STORE_NAME = "testBlobStore";

  private static final String BLOB_ID = "testBlobId";

  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  private BlobStore blobStore;

  @Mock
  private BlobId blobId;

  @Mock
  private BlobMetrics blobMetrics;

  @Mock
  private Blob blob;

  @Mock
  private DatabaseInstance databaseInstance;

  @Mock
  private BucketEntityAdapter bucketEntityAdapter;

  @Mock
  private ComponentEntityAdapter componentEntityAdapter;

  @Mock
  private AssetEntityAdapter assetEntityAdapter;

  @Mock
  private ClientInfoProvider clientInfoProvider;

  @Mock
  private ContentValidatorSelector contentValidatorSelector;

  @Mock
  private MimeRulesSourceSelector mimeRulesSourceSelector;

  @Mock
  private Configuration configuration;

  @Mock
  private ConfigurationFacet configurationFacet;

  @Mock
  private Repository repository;

  @Mock
  private Payload payload;

  private StorageFacetImpl underTest;

  @Before
  public void setUp() throws Exception {
    Config config = new Config();
    config.blobStoreName = BLOB_STORE_NAME;
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStoreConfiguration.getName()).thenReturn(BLOB_STORE_NAME);
    when(repository.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
    when(configurationFacet
        .readSection(any(Configuration.class), eq(StorageFacetImpl.CONFIG_KEY), eq(StorageFacetImpl.Config.class)))
        .thenReturn(config);
    when(nodeAccess.getId()).thenReturn(NODE_ID);
    when(blob.getId()).thenReturn(blobId);
    when(blob.getMetrics()).thenReturn(blobMetrics);
    when(blobId.asUniqueString()).thenReturn(BLOB_ID);
    underTest = new StorageFacetImpl(
        nodeAccess,
        blobStoreManager,
        () -> databaseInstance,
        bucketEntityAdapter,
        componentEntityAdapter,
        assetEntityAdapter,
        clientInfoProvider,
        contentValidatorSelector,
        mimeRulesSourceSelector
    );
    underTest.attach(repository);
  }

  @Test
  public void createTempBlobFromInputStream() throws Exception {
    byte[] contents = "hello, world".getBytes(Charsets.UTF_8);
    underTest.doConfigure(configuration);
    when(blobStoreManager.get(BLOB_STORE_NAME)).thenReturn(blobStore);
    when(blobStore.create(any(InputStream.class), Matchers.<Map<String, String>>any())).thenAnswer(
        invocationOnMock -> {
          ByteStreams.toByteArray((InputStream) invocationOnMock.getArguments()[0]);
          return blob;
        });
    when(blobMetrics.getContentSize()).thenReturn((long) contents.length);
    try (ByteArrayInputStream in = new ByteArrayInputStream(contents)) {
      try (TempBlob tempBlob = underTest.createTempBlob(in, singletonList(SHA1))) {
        assertThat(tempBlob.getHashes(), hasEntry(SHA1, fromString("b7e23ec29af22b0b4e41da31e868d57226121c84")));
        assertThat(tempBlob.getHashesVerified(), is(true));
        assertThat(tempBlob.getBlob(), is(blob));
      }
      verify(blobStore).deleteHard(blobId);
    }
  }

  @Test
  public void createTempBlobFromPayload() throws Exception {
    byte[] contents = "hello, world".getBytes(Charsets.UTF_8);
    underTest.doConfigure(configuration);
    when(blobStoreManager.get(BLOB_STORE_NAME)).thenReturn(blobStore);
    when(blobStore.create(any(InputStream.class), Matchers.<Map<String, String>>any())).thenAnswer(
        invocationOnMock -> {
          ByteStreams.toByteArray((InputStream) invocationOnMock.getArguments()[0]);
          return blob;
        });
    when(blobMetrics.getContentSize()).thenReturn((long) contents.length);
    when(payload.openInputStream()).thenAnswer(invocationOnMock -> new ByteArrayInputStream(contents));
    try (TempBlob tempBlob = underTest.createTempBlob(payload, singletonList(SHA1))) {
      assertThat(tempBlob.getHashes(), hasEntry(SHA1, fromString("b7e23ec29af22b0b4e41da31e868d57226121c84")));
      assertThat(tempBlob.getHashesVerified(), is(true));
      assertThat(tempBlob.getBlob(), is(blob));
    }
    verify(blobStore).deleteHard(blobId);
  }
}
