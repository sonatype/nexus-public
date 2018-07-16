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
package org.sonatype.nexus.repository.browse.internal;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetStore;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketStore;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexCursor;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.goodies.common.Time.seconds;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.I_BUCKET_COMPONENT_NAME;

public class RebuildBrowseNodesTaskTest
    extends TestSupport
{
  static final String REPOSITORY_NAME = "repository-name";

  private static final int REBUILD_PAGE_SIZE = 2;

  private RebuildBrowseNodesTask underTest;

  @Mock
  private AssetStore assetStore;

  @Mock
  private BucketStore bucketStore;

  @Mock
  private BrowseNodeManager browseNodeManager;

  @Mock
  private Repository repository;

  @Mock
  private Configuration config;

  @Mock
  private Bucket bucket;

  @Mock
  private AttachedEntityMetadata bucketMetadata;

  @Mock
  private ODocument bucketDocument;

  private ORID bucketRid = new ORecordId("#2:1");

  @Mock
  private OIndex bucketComponentIndex;

  @Mock
  private OIndexCursor cursor;

  @Before
  public void setUp() {
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getConfiguration()).thenReturn(config);
    when(config.isOnline()).thenReturn(true);
    when(bucketStore.read(repository.getName())).thenReturn(bucket);
    when(bucket.getEntityMetadata()).thenReturn(bucketMetadata);
    when(bucketMetadata.getDocument()).thenReturn(bucketDocument);
    when(bucketDocument.getIdentity()).thenReturn(bucketRid);
    when(assetStore.getIndex(I_BUCKET_COMPONENT_NAME)).thenReturn(bucketComponentIndex);
    when(bucketComponentIndex.cursor()).thenReturn(cursor);

    underTest = new RebuildBrowseNodesTask(
        assetStore,
        bucketStore,
        browseNodeManager,
        new BrowseNodeConfiguration(true, REBUILD_PAGE_SIZE, 1000, 10_000, 10_000, seconds(0))
    );
  }

  @Test
  public void executeWithOfflineRepository() {
    ORID bucketId = AttachedEntityHelper.id(bucket);

    Asset asset1 = createMockAsset("#1:1");
    Asset asset2 = createMockAsset("#1:2");
    Asset asset3 = createMockAsset("#2:1");

    List<Entry<Object, EntityId>> page = asList(
        createIndexEntry(bucketId, EntityHelper.id(asset1)),
        createIndexEntry(bucketId, EntityHelper.id(asset2)),
        createIndexEntry(bucketId, EntityHelper.id(asset3)));

    when(config.isOnline()).thenReturn(false);

    when(assetStore.countAssets(any())).thenReturn(3L);
    when(assetStore.getNextPage(any(), eq(REBUILD_PAGE_SIZE))).thenReturn(page, emptyList());
    when(assetStore.getById(EntityHelper.id(asset1))).thenReturn(asset1);
    when(assetStore.getById(EntityHelper.id(asset2))).thenReturn(asset2);
    when(assetStore.getById(EntityHelper.id(asset3))).thenReturn(asset3);

    underTest.execute(repository);

    verify(assetStore, times(2)).getNextPage(any(), eq(REBUILD_PAGE_SIZE));

    verify(browseNodeManager).createFromAssets(eq(repository), eq(asList(asset1, asset2, asset3)));
  }

  @Test
  public void executeTruncatesNodesForNoAssets() {
    when(assetStore.countAssets(any())).thenReturn(0L);

    underTest.execute(repository);

    verify(browseNodeManager).deleteByRepository(repository.getName());
    verify(assetStore).countAssets(any());
    verifyNoMoreInteractions(assetStore);
    verifyNoMoreInteractions(browseNodeManager);
  }

  @Test
  public void executeProcessesAllAssetsInPages() {
    ORID bucketId = AttachedEntityHelper.id(bucket);

    Asset asset1 = createMockAsset("#1:1");
    Asset asset2 = createMockAsset("#1:2");
    Asset asset3 = createMockAsset("#2:1");

    List<Entry<Object, EntityId>> page1 = asList(
        createIndexEntry(bucketId, EntityHelper.id(asset1)),
        createIndexEntry(bucketId, EntityHelper.id(asset2)));
    List<Entry<Object, EntityId>> page2 = asList(
        createIndexEntry(bucketId, EntityHelper.id(asset3)));
    List<Entry<Object, EntityId>> page3 = emptyList();

    when(assetStore.countAssets(any())).thenReturn(3L);
    when(assetStore.getNextPage(any(), eq(REBUILD_PAGE_SIZE))).thenReturn(page1, page2, page3);
    when(assetStore.getById(EntityHelper.id(asset1))).thenReturn(asset1);
    when(assetStore.getById(EntityHelper.id(asset2))).thenReturn(asset2);
    when(assetStore.getById(EntityHelper.id(asset3))).thenReturn(asset3);

    underTest.execute(repository);

    verify(assetStore, times(3)).getNextPage(any(), eq(REBUILD_PAGE_SIZE));

    verify(browseNodeManager).createFromAssets(eq(repository), eq(asList(asset1, asset2)));
    verify(browseNodeManager).createFromAssets(eq(repository), eq(asList(asset3)));
  }

  private Asset createMockAsset(final String id) {
    return createMockAsset(id, null);
  }

  private Asset createMockAsset(final String id, final String componentId) {
    Asset asset = mock(Asset.class);
    when(asset.getEntityMetadata()).thenReturn(mock(EntityMetadata.class));

    EntityId entityId = new DetachedEntityId(id);
    when(asset.getEntityMetadata().getId()).thenReturn(entityId);

    if (componentId != null) {
      EntityId componentEntityId = new DetachedEntityId(componentId);
      when(asset.componentId()).thenReturn(componentEntityId);
    }

    return asset;
  }

  private Entry<Object, EntityId> createIndexEntry(final ORID bucketId, final EntityId id) {
    return new SimpleEntry<>(new OCompositeKey(bucketId), id);
  }
}
