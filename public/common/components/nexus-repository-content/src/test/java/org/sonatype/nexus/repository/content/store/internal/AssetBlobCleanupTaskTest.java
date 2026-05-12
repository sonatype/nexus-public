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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;
import org.sonatype.nexus.repository.content.store.AssetBlobData;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.scheduling.RecoveryModeService;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.CaptureLogsFor;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.TestLogAccessor;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.event.Level;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTask.BATCH_SIZE;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTask.BLOB_CREATED_DELAY_MINUTE;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTaskDescriptor.CONTENT_STORE_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTaskDescriptor.FORMAT_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTaskDescriptor.TYPE_ID;
import static org.sonatype.nexus.testcommon.matchers.NexusMatchers.formattedMessage;
import static org.sonatype.nexus.testcommon.matchers.NexusMatchers.logLevel;

/**
 * Test {@link AssetBlobCleanupTask}.
 */
@ExtendWith({LoggingExtension.class, MockitoExtension.class})
public class AssetBlobCleanupTaskTest
{
  @CaptureLogsFor(value = AssetBlobCleanupTask.class, level = Level.WARN)
  TestLogAccessor logs;

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

  @Mock
  private RecoveryModeService recoveryModeService;

  ContinuationArrayList<AssetBlobData> firstPage;

  private MockedStatic<QualifierUtil> mockedStatic;

  @SuppressWarnings({"unchecked", "rawtypes"})
  @BeforeEach
  void setUp() {
    mockedStatic = mockStatic(QualifierUtil.class);
    lenient().when(blobRefMissingStore.getStore()).thenReturn("missing");
    lenient().when(blobRefBecomesUsed.getStore()).thenReturn("default");
    lenient().when(blobRefBecomesUsed.getBlobId()).thenReturn(mock(BlobId.class));

    firstPage = new ContinuationArrayList<>();
    firstPage.add(newAssetBlob());
    firstPage.add(newAssetBlob());
    firstPage.add(newAssetBlob(blobRefMissingStore));
    firstPage.add(newAssetBlob());
    firstPage.add(newAssetBlob());
    lenient().when(firstPage.get(firstPage.size() - 1).nextContinuationToken()).thenReturn("NEXT");

    ContinuationArrayList<AssetBlobData> lastPage = new ContinuationArrayList<>();
    lastPage.add(newAssetBlob());
    lastPage.add(newAssetBlob(blobRefBecomesUsed));
    lastPage.add(newAssetBlob());
    lenient().when(lastPage.get(lastPage.size() - 1).nextContinuationToken()).thenReturn("EOL");

    Continuation<AssetBlobData> emptyPage = new ContinuationArrayList<>();

    lenient().when(assetBlobStore.browseUnusedAssetBlobs(BATCH_SIZE, BLOB_CREATED_DELAY_MINUTE,
        null)).thenReturn((Continuation) firstPage);
    lenient().when(assetBlobStore.browseUnusedAssetBlobs(BATCH_SIZE, BLOB_CREATED_DELAY_MINUTE,
        "NEXT")).thenReturn((Continuation) lastPage);
    lenient().when(assetBlobStore.browseUnusedAssetBlobs(BATCH_SIZE, BLOB_CREATED_DELAY_MINUTE,
        "EOL")).thenReturn((Continuation) emptyPage);

    lenient().when(assetBlobStore.deleteAssetBlob(any())).thenReturn(true);
    lenient().when(assetBlobStore.deleteAssetBlobBatch(any())).thenReturn(4);

    lenient().when(formatStoreManager.assetBlobStore("content")).thenReturn(assetBlobStore);

    lenient().when(blobStoreManager.get("default")).thenReturn(blobStore);
    lenient().when(blobStore.delete(any(), any())).thenReturn(true);
    lenient().when(QualifierUtil.buildQualifierBeanMap(anyList()))
        .thenReturn(ImmutableMap.of("raw", formatStoreManager));
  }

  @AfterEach
  void tearDown() {
    mockedStatic.close();
  }

  @Test
  void testUnusedBlobsAreDeleted() throws Exception {
    setBatchDeleteIgnoreFinalField("raw");
    AssetBlobCleanupTask task =
        new AssetBlobCleanupTask(List.of(formatStoreManager), blobStoreManager, recoveryModeService);

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
    inOrder.verify(assetBlobStore)
        .browseUnusedAssetBlobs(BATCH_SIZE, BLOB_CREATED_DELAY_MINUTE, null);
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    inOrder.verify(blobStore).delete(blobRefCaptor.getValue().getBlobId(), EXPECTED_REASON);
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    inOrder.verify(blobStore).delete(blobRefCaptor.getValue().getBlobId(), EXPECTED_REASON);
    // blob with missing blob store - delete database record only, not physical blob (NEXUS-37039 fix)
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefMissingStore);
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    inOrder.verify(blobStore).delete(blobRefCaptor.getValue().getBlobId(), EXPECTED_REASON);
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    inOrder.verify(blobStore).delete(blobRefCaptor.getValue().getBlobId(), EXPECTED_REASON);

    // mimic scenario when an unused blob becomes used again, so delete is rejected
    lenient().when(assetBlobStore.deleteAssetBlob(blobRefBecomesUsed)).thenReturn(false);

    // last page
    inOrder.verify(assetBlobStore)
        .browseUnusedAssetBlobs(BATCH_SIZE, BLOB_CREATED_DELAY_MINUTE, "NEXT");
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    inOrder.verify(blobStore).delete(blobRefCaptor.getValue().getBlobId(), EXPECTED_REASON);
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    // unused blob has become used again so won't go on to delete the actual blob
    inOrder.verify(assetBlobStore).deleteAssetBlob(blobRefCaptor.capture());
    inOrder.verify(blobStore).delete(blobRefCaptor.getValue().getBlobId(), EXPECTED_REASON);

    inOrder.verify(assetBlobStore)
        .browseUnusedAssetBlobs(BATCH_SIZE, BLOB_CREATED_DELAY_MINUTE, "EOL");
    verify(recoveryModeService).ensureNotInRecoveryMode(any());
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testDefaultBatchSize() throws Exception {
    setBatchDeleteIgnoreFinalField("raw");
    AssetBlobCleanupTask task =
        new AssetBlobCleanupTask(List.of(formatStoreManager), blobStoreManager, recoveryModeService);

    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString(FORMAT_FIELD_ID, "raw");
    taskConfiguration.setString(CONTENT_STORE_FIELD_ID, "content");
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(TYPE_ID);
    task.configure(taskConfiguration);

    task.execute();

    int expectedBatchSize = 1000;
    verify(assetBlobStore).browseUnusedAssetBlobs(expectedBatchSize, BLOB_CREATED_DELAY_MINUTE, null);
    verify(recoveryModeService).ensureNotInRecoveryMode(any());
  }

  @Test
  void testExecutorServiceShutdown() throws Exception {
    setBatchDeleteIgnoreFinalField(null);
    AssetBlobCleanupTask task =
        spy(new AssetBlobCleanupTask(List.of(formatStoreManager), blobStoreManager, recoveryModeService));

    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString(FORMAT_FIELD_ID, "raw");
    taskConfiguration.setString(CONTENT_STORE_FIELD_ID, "content");
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(TYPE_ID);
    task.configure(taskConfiguration);

    doNothing().when(task).initBatchDeleteIfEnabled(anyString());
    doReturn(1).when(task).deleteUnusedAssetBlobsBatch(any(), anyString(), anyString());

    ExecutorService batchDeleteExecutorService = mock(ExecutorService.class);
    when(batchDeleteExecutorService.isShutdown()).thenReturn(false);

    Field field = task.getClass().getDeclaredField("batchDeleteExecutorService");
    field.setAccessible(true);
    field.set(task, batchDeleteExecutorService);

    task.execute();

    verify(recoveryModeService).ensureNotInRecoveryMode(any());
    verify(batchDeleteExecutorService).isShutdown();
    verify(batchDeleteExecutorService).shutdown();
  }

  @Test
  void testUnusedBlobsAreDeletedBatch() throws Exception {
    setBatchDeleteIgnoreFinalField(null);

    AssetBlobCleanupTask task =
        new AssetBlobCleanupTask(List.of(formatStoreManager), blobStoreManager, recoveryModeService);

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
    inOrder.verify(assetBlobStore)
        .browseUnusedAssetBlobs(BATCH_SIZE, BLOB_CREATED_DELAY_MINUTE, null);
    inOrder.verify(assetBlobStore).deleteAssetBlobBatch(blobRefIdCaptor.capture());
    lenient().when(assetBlobStore.deleteAssetBlobBatch(
        new String[]{blobRefBecomesUsed.getBlobId().toString()})).thenReturn(0);
    inOrder.verify(assetBlobStore)
        .browseUnusedAssetBlobs(BATCH_SIZE, BLOB_CREATED_DELAY_MINUTE, "NEXT");
    inOrder.verify(assetBlobStore).deleteAssetBlobBatch(blobRefIdCaptor.capture());
    inOrder.verify(assetBlobStore)
        .browseUnusedAssetBlobs(BATCH_SIZE, BLOB_CREATED_DELAY_MINUTE, "EOL");
    verify(recoveryModeService).ensureNotInRecoveryMode(any());
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void testExecute_failsWhenRecoveryModeActive() {
    doThrow(
        new IllegalStateException("expected exception"))
            .when(recoveryModeService)
            .ensureNotInRecoveryMode(any());

    AssetBlobCleanupTask task =
        new AssetBlobCleanupTask(List.of(formatStoreManager), blobStoreManager, recoveryModeService);

    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString(FORMAT_FIELD_ID, "raw");
    taskConfiguration.setString(CONTENT_STORE_FIELD_ID, "content");
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(TYPE_ID);
    task.configure(taskConfiguration);

    assertThrows(IllegalStateException.class, task::execute);
    verify(recoveryModeService).ensureNotInRecoveryMode(any());
  }

  @Test
  void testBatchDeleteDatabaseExceptionIsLogged() throws Exception {
    setBatchDeleteIgnoreFinalField(null);

    AssetBlobCleanupTask task =
        new AssetBlobCleanupTask(List.of(formatStoreManager), blobStoreManager, recoveryModeService);

    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString(FORMAT_FIELD_ID, "raw");
    taskConfiguration.setString(CONTENT_STORE_FIELD_ID, "content");
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(TYPE_ID);
    task.configure(taskConfiguration);

    RuntimeException dbException = new RuntimeException("Database connection lost");
    when(assetBlobStore.deleteAssetBlobBatch(any())).thenThrow(dbException);

    task.execute();

    verify(assetBlobStore, times(2)).deleteAssetBlobBatch(any());
    verify(assetBlobStore, times(3)).browseUnusedAssetBlobs(anyInt(), anyInt(), any());

    assertThat(logs.logs(), hasItem(allOf(logLevel(Level.WARN),
        formattedMessage(containsString(
            "Batch processing: 3 blobs deleted from storage, but database deletion failed")))));
  }

  @Test
  void testBatchDeleteTracksSuccessfulDeletionsOnly() throws Exception {
    setBatchDeleteIgnoreFinalField(null);

    AssetBlobCleanupTask task =
        new AssetBlobCleanupTask(List.of(formatStoreManager), blobStoreManager, recoveryModeService);

    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString(FORMAT_FIELD_ID, "raw");
    taskConfiguration.setString(CONTENT_STORE_FIELD_ID, "content");
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(TYPE_ID);
    task.configure(taskConfiguration);

    ArgumentCaptor<String[]> blobRefIdCaptor = forClass(String[].class);

    // Mock database deletion to return fewer records than expected to trigger mismatch warning
    when(assetBlobStore.deleteAssetBlobBatch(any())).thenReturn(2);

    task.execute();

    verify(assetBlobStore, times(2)).deleteAssetBlobBatch(blobRefIdCaptor.capture());

    // Verify only successfully deleted blobs from storage are passed to database delete
    List<String[]> allCapturedBatchArgs = blobRefIdCaptor.getAllValues();
    assertThat("Should have one batch per page", allCapturedBatchArgs.size(), is(2));

    // First page has 5 assets, 1 has missing blob store which is now INCLUDED (NEXUS-37039 fix)
    String[] firstBatch = allCapturedBatchArgs.get(0);
    assertThat("First batch should include blob with missing store", firstBatch.length, is(5));

    // Second page has 3 assets, all with valid blob stores
    String[] secondBatch = allCapturedBatchArgs.get(1);
    assertThat("Second batch should contain all blobs from last page", secondBatch.length, is(3));

    verify(blobStoreManager, atLeast(4)).get("default");
    verify(blobStoreManager, atLeast(1)).get("missing");

    assertThat(logs.logs(), hasItem(allOf(logLevel(Level.WARN),
        formattedMessage(containsString(
            "Batch processing: 5 blobs deleted from storage, but only 2 database records deleted")))));
    assertThat(logs.logs(), hasItem(allOf(logLevel(Level.WARN),
        formattedMessage(containsString("(3 records not found)")))));
  }

  @Test
  void testBatchDeleteLogsExceptionWhenBlobStoreDeletionFails() throws Exception {
    setBatchDeleteIgnoreFinalField(null);

    BlobRef failingBlobRef = mock(BlobRef.class);
    BlobId failingBlobId = mock(BlobId.class);
    when(failingBlobRef.getStore()).thenReturn("default");
    when(failingBlobRef.getBlobId()).thenReturn(failingBlobId);

    firstPage.add(newAssetBlob(failingBlobRef));
    when(firstPage.get(firstPage.size() - 1).nextContinuationToken()).thenReturn("NEXT");

    RuntimeException storageException = new RuntimeException("S3 connection timeout");
    when(blobStore.delete(failingBlobId, EXPECTED_REASON)).thenThrow(storageException);

    when(assetBlobStore.deleteAssetBlobBatch(any())).thenReturn(5);

    AssetBlobCleanupTask task =
        new AssetBlobCleanupTask(List.of(formatStoreManager), blobStoreManager, recoveryModeService);

    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString(FORMAT_FIELD_ID, "raw");
    taskConfiguration.setString(CONTENT_STORE_FIELD_ID, "content");
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(TYPE_ID);
    task.configure(taskConfiguration);

    ArgumentCaptor<String[]> blobRefIdCaptor = forClass(String[].class);

    task.execute();

    verify(assetBlobStore, atLeast(1)).deleteAssetBlobBatch(blobRefIdCaptor.capture());

    // Verify that the exception was logged
    assertThat(logs.logs(), hasItem(allOf(logLevel(Level.WARN),
        formattedMessage(containsString("Failed to delete blob content under")))));

    // Verify the failing blob was NOT included in the database deletion batch
    // (First page has 6 blobs: 4 succeed, 1 missing store included (NEXUS-37039 fix), 1 fails = 5 should be sent)
    String[] firstBatch = blobRefIdCaptor.getAllValues().get(0);
    assertThat("Missing blobstore blob included, failing blob excluded", firstBatch.length, is(5));
  }

  @Test
  void testOrphanedBlobsWithDeletedBlobstoreAreDeleted() throws Exception {
    // Use single-threaded path to test NEXUS-37039 fix
    setBatchDeleteIgnoreFinalField("raw");

    AssetBlobCleanupTask task =
        new AssetBlobCleanupTask(List.of(formatStoreManager), blobStoreManager, recoveryModeService);

    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString(FORMAT_FIELD_ID, "raw");
    taskConfiguration.setString(CONTENT_STORE_FIELD_ID, "content");
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(TYPE_ID);
    task.configure(taskConfiguration);

    task.execute();

    // Verify the blob with missing blobstore (blobRefMissingStore) was processed
    verify(assetBlobStore, atLeast(1)).deleteAssetBlob(blobRefMissingStore);

    // Verify blobStoreManager checked for the missing blobstore
    verify(blobStoreManager, atLeast(1)).get("missing");

    // Verify physical blob deletion was NOT attempted for missing blobstore
    // (blobStore.delete() should only be called for blobs with existing blobstores)
    verify(blobStore, atLeast(4)).delete(any(), any()); // 4 blobs from "default" store, not 5

    // Verify appropriate log message was generated
    assertThat(logs.logs(), hasItem(allOf(logLevel(Level.WARN),
        formattedMessage(containsString("no longer exists")))));
    assertThat(logs.logs(), hasItem(allOf(logLevel(Level.WARN),
        formattedMessage(containsString("deleting orphaned database record")))));

    verify(recoveryModeService).ensureNotInRecoveryMode(any());
  }

  @Test
  void testExecuteWithNullRecoveryModeService() throws Exception {
    setBatchDeleteIgnoreFinalField("raw");
    // Create task with null RecoveryModeService (CORE edition scenario)
    AssetBlobCleanupTask task =
        new AssetBlobCleanupTask(List.of(formatStoreManager), blobStoreManager, null);

    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setString(FORMAT_FIELD_ID, "raw");
    taskConfiguration.setString(CONTENT_STORE_FIELD_ID, "content");
    taskConfiguration.setId(UUID.randomUUID().toString());
    taskConfiguration.setTypeId(TYPE_ID);
    task.configure(taskConfiguration);

    // Execute should complete without throwing
    task.execute();

    // Verify the task executed and deleted blobs as expected
    verify(assetBlobStore).browseUnusedAssetBlobs(BATCH_SIZE, BLOB_CREATED_DELAY_MINUTE, null);
    verify(assetBlobStore, atLeast(1)).deleteAssetBlob(any());
    verify(blobStore, atLeast(1)).delete(any(), anyString());
  }

  private void setBatchDeleteIgnoreFinalField(String batchDeleteFormats) {
    System.setProperty(AssetBlobCleanupTask.PROPERTY_PREFIX + "batchDeleteIgnoreForFormat",
        batchDeleteFormats == null ? "" : batchDeleteFormats);
  }

  private AssetBlobData newAssetBlob() {
    BlobRef blobRef = mock(BlobRef.class);
    lenient().when(blobRef.getStore()).thenReturn("default");
    lenient().when(blobRef.getBlobId()).thenReturn(mock(BlobId.class));
    return newAssetBlob(blobRef);
  }

  private AssetBlobData newAssetBlob(final BlobRef blobRef) {
    AssetBlobData assetBlob = mock(AssetBlobData.class);
    lenient().when(assetBlob.blobRef()).thenReturn(blobRef);
    return assetBlob;
  }
}
