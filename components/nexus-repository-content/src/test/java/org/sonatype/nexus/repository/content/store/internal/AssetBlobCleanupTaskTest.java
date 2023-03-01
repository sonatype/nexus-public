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
package org.sonatype.nexus.repository.content.store.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.store.AssetBlobData;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.BlobRefTypeHandler;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTask.BATCH_SIZE;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTaskDescriptor.CONTENT_STORE_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTaskDescriptor.FORMAT_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTaskDescriptor.TYPE_ID;

/**
 * Test {@link AssetBlobCleanupTask}.
 */
public class AssetBlobCleanupTaskTest
    extends TestSupport
{
  private static final String EXPECTED_REASON = "Removing unused asset blob";

  @Mock
  private FormatStoreManager formatStoreManager;

  @Mock
  private AssetBlobStore<?> assetBlobStore;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStore blobStore;

  @Mock
  private BlobRef blobRefMissingStore;

  @Mock
  private BlobRef blobRefBecomesUsed;

  ContinuationArrayList<AssetBlobData> firstPage;

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Before
  public void setUp() {
    when(blobRefMissingStore.getStore()).thenReturn("missing");
    when(blobRefBecomesUsed.getStore()).thenReturn("default");
    when(blobRefBecomesUsed.getBlobId()).thenReturn(mock(BlobId.class));

    firstPage = new ContinuationArrayList<>();
    firstPage.add(newAssetBlob());
    firstPage.add(newAssetBlob());
    firstPage.add(newAssetBlob(blobRefMissingStore));
    firstPage.add(newAssetBlob());
    firstPage.add(newAssetBlob());
    when(firstPage.get(firstPage.size() - 1).nextContinuationToken()).thenReturn("NEXT");

    ContinuationArrayList<AssetBlobData> lastPage = new ContinuationArrayList<>();
    lastPage.add(newAssetBlob());
    lastPage.add(newAssetBlob(blobRefBecomesUsed));
    lastPage.add(newAssetBlob());
    when(lastPage.get(lastPage.size() - 1).nextContinuationToken()).thenReturn("EOL");

    Continuation<AssetBlobData> emptyPage = new ContinuationArrayList<>();

    when(assetBlobStore.browseUnusedAssetBlobs(BATCH_SIZE, null)).thenReturn((Continuation) firstPage);
    when(assetBlobStore.browseUnusedAssetBlobs(BATCH_SIZE, "NEXT")).thenReturn((Continuation) lastPage);
    when(assetBlobStore.browseUnusedAssetBlobs(BATCH_SIZE, "EOL")).thenReturn((Continuation) emptyPage);

    when(assetBlobStore.deleteAssetBlob(any())).thenReturn(true);
    when(assetBlobStore.deleteAssetBlobBatch(any())).thenReturn(true);

    when(formatStoreManager.assetBlobStore("content")).thenReturn(assetBlobStore);

    when(blobStoreManager.get("default")).thenReturn(blobStore);
    when(blobStore.delete(any(), any())).thenReturn(true);
  }

  @Test
  public void testUnusedBlobsAreDeleted() throws Exception {
    AssetBlobCleanupTask task = new AssetBlobCleanupTask(ImmutableMap.of("raw", formatStoreManager), blobStoreManager);

    setBatchDeleteIgnoreFinalField("raw");

    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString(FORMAT_FIELD_ID, "raw");
    taskConfiguration.setString(CONTENT_STORE_FIELD_ID, "content");
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(TYPE_ID);
    task.configure(taskConfiguration);

    ArgumentCaptor<BlobRef> blobRefCaptor = forClass(BlobRef.class);
    InOrder inOrder = Mockito.inOrder(assetBlobStore, blobStore);

    task.execute();

    // first page
    inOrder.verify(assetBlobStore).browseUnusedAssetBlobs(BATCH_SIZE, null);
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    inOrder.verify(blobStore).delete(blobRefCaptor.getValue().getBlobId(), EXPECTED_REASON);
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    inOrder.verify(blobStore).delete(blobRefCaptor.getValue().getBlobId(), EXPECTED_REASON);
    // blob with missing blob store won't end up calling either asset-blob or blob delete
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    inOrder.verify(blobStore).delete(blobRefCaptor.getValue().getBlobId(), EXPECTED_REASON);
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    inOrder.verify(blobStore).delete(blobRefCaptor.getValue().getBlobId(), EXPECTED_REASON);

    // mimic scenario when an unused blob becomes used again, so delete is rejected
    when(assetBlobStore.deleteAssetBlob(blobRefBecomesUsed)).thenReturn(false);

    // last page
    inOrder.verify(assetBlobStore).browseUnusedAssetBlobs(BATCH_SIZE, "NEXT");
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    inOrder.verify(blobStore).delete(blobRefCaptor.getValue().getBlobId(), EXPECTED_REASON);
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    // unused blob has become used again so won't go on to delete the actual blob
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    inOrder.verify(blobStore).delete(blobRefCaptor.getValue().getBlobId(), EXPECTED_REASON);

    inOrder.verify(assetBlobStore).browseUnusedAssetBlobs(BATCH_SIZE, "EOL");

    inOrder.verifyNoMoreInteractions();

    setBatchDeleteIgnoreFinalField(null);
  }

  @Test
  public void testUnusedBlobsAreDeletedBatch() throws Exception {
    AssetBlobCleanupTask task = new AssetBlobCleanupTask(ImmutableMap.of("raw", formatStoreManager), blobStoreManager);

    setBatchDeleteIgnoreFinalField(null);

    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString(FORMAT_FIELD_ID, "raw");
    taskConfiguration.setString(CONTENT_STORE_FIELD_ID, "content");
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(TYPE_ID);
    task.configure(taskConfiguration);

    ArgumentCaptor<String[]> blobRefIdCaptor = forClass(String[].class);
    InOrder inOrder = Mockito.inOrder(assetBlobStore);

    task.execute();

    // first page
    inOrder.verify(assetBlobStore).browseUnusedAssetBlobs(BATCH_SIZE, null);
    inOrder.verify(assetBlobStore).deleteAssetBlobBatch(blobRefIdCaptor.capture());
    when(assetBlobStore.deleteAssetBlobBatch(
        new String[]{blobRefBecomesUsed.getBlobId().toString()})).thenReturn(false);
    inOrder.verify(assetBlobStore).browseUnusedAssetBlobs(BATCH_SIZE, "NEXT");
    inOrder.verify(assetBlobStore).deleteAssetBlobBatch(blobRefIdCaptor.capture());
    inOrder.verify(assetBlobStore).browseUnusedAssetBlobs(BATCH_SIZE, "EOL");

    inOrder.verifyNoMoreInteractions();
  }

  private void setBatchDeleteIgnoreFinalField(String batchDeleteFormats)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = AssetBlobCleanupTask.class.getDeclaredField("BATCH_DELETE_IGNORE_FORMATS");
    field.setAccessible(true);
    Field modifiers = Field.class.getDeclaredField("modifiers");
    modifiers.setAccessible(true);
    modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    field.set(null, batchDeleteFormats);
  }

  private AssetBlobData newAssetBlob() {
    BlobRef blobRef = mock(BlobRef.class);
    when(blobRef.getStore()).thenReturn("default");
    when(blobRef.getBlobId()).thenReturn(mock(BlobId.class));
    return newAssetBlob(blobRef);
  }

  private AssetBlobData newAssetBlob(final BlobRef blobRef) {
    AssetBlobData assetBlob = mock(AssetBlobData.class);
    when(assetBlob.blobRef()).thenReturn(blobRef);
    return assetBlob;
  }
}
