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
package org.sonatype.nexus.repository.content.store.internal.migration;

import java.util.UUID;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.ContinuationAware;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;
import org.sonatype.nexus.repository.content.store.AssetBlobData;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTaskDescriptor.CONTENT_STORE_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTaskDescriptor.FORMAT_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTaskDescriptor.TYPE_ID;

/**
 * Tests for {@link AssetBlobRefMigrationTask}
 */
public class AssetBlobRefMigrationTaskTest
    extends TestSupport
{
  private static final int READ_ASSETS_BATCH_SIZE = 100;

  @Mock
  private FormatStoreManager formatStoreManager;

  @Mock
  private AssetBlobStore<?> assetBlobStore;

  @Before
  public void setup() {
    ContinuationArrayList<AssetBlobData> firstPage = new ContinuationArrayList<>();
    firstPage.add(newNotMigratedAssetBlobData());
    firstPage.add(newNotMigratedAssetBlobData());
    firstPage.add(newNotMigratedAssetBlobData());
    when(((ContinuationAware) firstPage.get(firstPage.size() - 1)).nextContinuationToken()).thenReturn("NEXT");

    ContinuationArrayList<AssetBlobData> lastPage = new ContinuationArrayList<>();
    lastPage.add(newNotMigratedAssetBlobData());
    lastPage.add(newNotMigratedAssetBlobData());
    when(lastPage.get(lastPage.size() - 1).nextContinuationToken()).thenReturn("EOL");

    ContinuationArrayList<AssetBlobData> emptyPage = new ContinuationArrayList<>();

    when(assetBlobStore.notMigratedAssetBlobRefsExists()).thenReturn(true);

    when(assetBlobStore.browseAssetsWithLegacyBlobRef(READ_ASSETS_BATCH_SIZE, null))
        .thenReturn((Continuation) firstPage);
    when(assetBlobStore.browseAssetsWithLegacyBlobRef(READ_ASSETS_BATCH_SIZE, "NEXT"))
        .thenReturn((Continuation) lastPage);
    when(assetBlobStore.browseAssetsWithLegacyBlobRef(READ_ASSETS_BATCH_SIZE, "EOL"))
        .thenReturn((Continuation) emptyPage);

    when(assetBlobStore.updateBlobRefs(any())).thenReturn(true);

    when(formatStoreManager.assetBlobStore("content")).thenReturn(assetBlobStore);
  }

  @Test
  public void testAssetBlobRefMigration() throws Exception {
    AssetBlobRefMigrationTask underTest = new AssetBlobRefMigrationTask(ImmutableMap.of("raw", formatStoreManager),
        READ_ASSETS_BATCH_SIZE);

    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString(FORMAT_FIELD_ID, "raw");
    taskConfiguration.setString(CONTENT_STORE_FIELD_ID, "content");
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(TYPE_ID);
    underTest.configure(taskConfiguration);

    underTest.execute();

    verify(assetBlobStore).browseAssetsWithLegacyBlobRef(READ_ASSETS_BATCH_SIZE, null);
    verify(assetBlobStore).browseAssetsWithLegacyBlobRef(READ_ASSETS_BATCH_SIZE, "NEXT");
    verify(assetBlobStore).browseAssetsWithLegacyBlobRef(READ_ASSETS_BATCH_SIZE, "EOL");

    verify(assetBlobStore, times(2)).updateBlobRefs(any());
  }

  private AssetBlobData newNotMigratedAssetBlobData() {
    return mock(AssetBlobData.class);
  }
}
