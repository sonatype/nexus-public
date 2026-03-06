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
package org.sonatype.nexus.blobstore.internal.softdeleted;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.softdeleted.BlobLocationUpdate;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlobsStore;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.CaptureLogsFor;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.TestLogAccessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.event.Level;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.testcommon.matchers.NexusMatchers.formattedMessage;
import static org.sonatype.nexus.testcommon.matchers.NexusMatchers.logLevel;

@ExtendWith(LoggingExtension.class)
class SoftDeletedBlobMoveServiceImplTest
    extends Test5Support
{
  private static final String OLD_BLOB_STORE = "oldBlobStore";

  @Mock
  private SoftDeletedBlobsStore softDeletedBlobsStore;

  @Captor
  private ArgumentCaptor<List<BlobLocationUpdate>> updatesCaptor;

  @CaptureLogsFor(value = SoftDeletedBlobMoveServiceImpl.class, level = Level.DEBUG)
  TestLogAccessor logs;

  private SoftDeletedBlobMoveServiceImpl underTest;

  @BeforeEach
  void setUp() {
    underTest = new SoftDeletedBlobMoveServiceImpl(softDeletedBlobsStore, 1000);
  }

  @Test
  void testUpdateMovedBlobRecords_emptyMap() {
    Map<BlobId, String> emptyMap = new HashMap<>();

    underTest.updateMovedBlobRecords(emptyMap, OLD_BLOB_STORE);

    verify(softDeletedBlobsStore, never()).batchUpdateBlobLocation(anyList(), anyString());

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.INFO),
        formattedMessage(is("No blobs were moved, skipping soft_deleted_blobs update")))));
  }

  @Test
  void testUpdateMovedBlobRecords_partialFailure() {
    OffsetDateTime baseTime = UTC.now();
    Map<BlobId, String> movedBlobs = new HashMap<>();
    Map<String, BlobLocationUpdate> expectedUpdates = new HashMap<>();

    for (int i = 0; i < 2001; i++) {
      OffsetDateTime datePathRef = baseTime.plusSeconds(i);
      BlobId blobId = new BlobId("blob-" + i, datePathRef);
      String targetStore = "targetStore-" + i;
      movedBlobs.put(blobId, targetStore);
      expectedUpdates.put("blob-" + i, new BlobLocationUpdate("blob-" + i, targetStore, datePathRef));
    }

    when(softDeletedBlobsStore.batchUpdateBlobLocation(anyList(), eq(OLD_BLOB_STORE)))
        .thenReturn(1000)
        .thenThrow(new RuntimeException("Database error"))
        .thenReturn(1);

    underTest.updateMovedBlobRecords(movedBlobs, OLD_BLOB_STORE);

    verify(softDeletedBlobsStore, times(3)).batchUpdateBlobLocation(updatesCaptor.capture(), eq(OLD_BLOB_STORE));

    List<List<BlobLocationUpdate>> allBatches = updatesCaptor.getAllValues();
    assertThat(allBatches, hasSize(3));
    assertThat(allBatches.get(0), hasSize(1000));
    assertThat(allBatches.get(1), hasSize(1000));
    assertThat(allBatches.get(2), hasSize(1));

    for (List<BlobLocationUpdate> batch : allBatches) {
      for (BlobLocationUpdate actualUpdate : batch) {
        BlobLocationUpdate expected = expectedUpdates.get(actualUpdate.blobId());
        assertThat("newSourceBlobStoreName mismatch for " + actualUpdate.blobId(),
            actualUpdate.newSourceBlobStoreName(), is(expected.newSourceBlobStoreName()));
        assertThat("newDatePathRef mismatch for " + actualUpdate.blobId(),
            actualUpdate.newDatePathRef(), is(expected.newDatePathRef()));
      }
    }

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.WARN),
        formattedMessage(is("Failed to batch update soft_deleted_blobs records from oldBlobStore: Database error")))));

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.INFO),
        formattedMessage(is("Updated 1001 soft_deleted_blobs records from oldBlobStore (0 not found, 1000 failed)")))));

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.WARN),
        formattedMessage(is(
            "Failed to update 1000 soft_deleted_blobs records - these blobs may not be cleaned up by Compact task")))));
  }

  @Test
  void testUpdateMovedBlobRecords_logsCorrectCounts() {
    OffsetDateTime baseTime = UTC.now();
    Map<BlobId, String> movedBlobs = new HashMap<>();

    for (int i = 0; i < 5; i++) {
      BlobId blobId = new BlobId("blob-" + i, baseTime.plusSeconds(i));
      movedBlobs.put(blobId, "targetStore");
    }

    when(softDeletedBlobsStore.batchUpdateBlobLocation(anyList(), eq(OLD_BLOB_STORE))).thenReturn(3);

    underTest.updateMovedBlobRecords(movedBlobs, OLD_BLOB_STORE);

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.INFO),
        formattedMessage(is("Updating soft_deleted_blobs records for 5 moved blobs from oldBlobStore")))));

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.INFO),
        formattedMessage(is("Updated 3 soft_deleted_blobs records from oldBlobStore (2 not found, 0 failed)")))));
  }

  @Test
  void testUpdateMovedBlobRecords_batchProcessing() {
    OffsetDateTime baseTime = UTC.now();
    Map<BlobId, String> movedBlobs = new HashMap<>();
    Map<String, BlobLocationUpdate> expectedUpdates = new HashMap<>();

    for (int i = 0; i < 2500; i++) {
      OffsetDateTime datePathRef = baseTime.plusSeconds(i);
      BlobId blobId = new BlobId("blob-" + i, datePathRef);
      String targetStore = "targetStore-" + i;
      movedBlobs.put(blobId, targetStore);
      expectedUpdates.put("blob-" + i, new BlobLocationUpdate("blob-" + i, targetStore, datePathRef));
    }

    when(softDeletedBlobsStore.batchUpdateBlobLocation(anyList(), eq(OLD_BLOB_STORE)))
        .thenAnswer(invocation -> {
          List<BlobLocationUpdate> batch = invocation.getArgument(0);
          return batch.size();
        });

    underTest.updateMovedBlobRecords(movedBlobs, OLD_BLOB_STORE);

    verify(softDeletedBlobsStore, times(3)).batchUpdateBlobLocation(updatesCaptor.capture(), eq(OLD_BLOB_STORE));

    List<List<BlobLocationUpdate>> allBatches = updatesCaptor.getAllValues();
    assertThat(allBatches, hasSize(3));
    assertThat(allBatches.get(0), hasSize(1000));
    assertThat(allBatches.get(1), hasSize(1000));
    assertThat(allBatches.get(2), hasSize(500));

    for (List<BlobLocationUpdate> batch : allBatches) {
      for (BlobLocationUpdate actualUpdate : batch) {
        BlobLocationUpdate expected = expectedUpdates.get(actualUpdate.blobId());
        assertThat("newSourceBlobStoreName mismatch for " + actualUpdate.blobId(),
            actualUpdate.newSourceBlobStoreName(), is(expected.newSourceBlobStoreName()));
        assertThat("newDatePathRef mismatch for " + actualUpdate.blobId(),
            actualUpdate.newDatePathRef(), is(expected.newDatePathRef()));
      }
    }

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.INFO),
        formattedMessage(is("Updated 2500 soft_deleted_blobs records from oldBlobStore (0 not found, 0 failed)")))));
  }

  @Test
  void testUpdateMovedBlobRecords_handlesNullDatePathRef() {
    // BlobId with null datePathRef (legacy blobs)
    BlobId blobId = new BlobId("blob-1", null);
    String newBlobStore = "newBlobStore";

    Map<BlobId, String> movedBlobs = new HashMap<>();
    movedBlobs.put(blobId, newBlobStore);

    when(softDeletedBlobsStore.batchUpdateBlobLocation(anyList(), eq(OLD_BLOB_STORE))).thenReturn(1);

    underTest.updateMovedBlobRecords(movedBlobs, OLD_BLOB_STORE);

    verify(softDeletedBlobsStore).batchUpdateBlobLocation(updatesCaptor.capture(), eq(OLD_BLOB_STORE));

    List<BlobLocationUpdate> capturedUpdates = updatesCaptor.getValue();
    assertThat(capturedUpdates, hasSize(1));

    BlobLocationUpdate update = capturedUpdates.get(0);
    assertThat(update.blobId(), is("blob-1"));
    assertThat(update.newSourceBlobStoreName(), is(newBlobStore));
    assertThat(update.newDatePathRef(), nullValue());
  }
}
