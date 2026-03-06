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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.softdeleted.BlobLocationUpdate;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlob;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class SoftDeletedBlobsDAOTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(SoftDeletedBlobsDAO.class);

  private DataSession<?> session;

  private SoftDeletedBlobsDAO dao;

  private static final String FAKE_BLOB_STORE_NAME = "fakeBlobStore";

  @Before
  public void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(SoftDeletedBlobsDAO.class);
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void testDAOOperations() {
    int limit = 100;
    Continuation<SoftDeletedBlob> emptyData = dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME);
    assertThat(emptyData.isEmpty(), is(true));

    dao.createRecord(FAKE_BLOB_STORE_NAME, "blobID", UTC.now());
    Optional<SoftDeletedBlob> initialBlobID = dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME).stream().findFirst();

    assertThat(initialBlobID.isPresent(), is(true));
    assertThat(initialBlobID.get().getBlobId(), is("blobID"));

    dao.deleteRecord(FAKE_BLOB_STORE_NAME, "blobID");
    Continuation<SoftDeletedBlob> newBlobs = dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME);

    assertThat(newBlobs.isEmpty(), is(true));

    dao.createRecord(FAKE_BLOB_STORE_NAME, "blob1", UTC.now());
    dao.createRecord(FAKE_BLOB_STORE_NAME, "blob2", UTC.now());
    dao.createRecord(FAKE_BLOB_STORE_NAME, "blob3", UTC.now());

    assertThat(dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME).size(), is(3));

    dao.deleteAllRecords(FAKE_BLOB_STORE_NAME, "100");

    assertThat(dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME).size(), is(0));
  }

  @Test
  public void testBatchUpdateBlobLocation() {
    int batchSize = 1000;
    String oldBlobStoreName = "oldBlobStore";
    OffsetDateTime baseTime = UTC.now().truncatedTo(ChronoUnit.SECONDS);

    // Create 1000 records with old date_path_ref
    for (int i = 0; i < batchSize; i++) {
      dao.createRecord(oldBlobStoreName, "blob-" + i, baseTime.minusDays(30));
    }

    // Verify initial state
    assertThat(dao.count(oldBlobStoreName), is(batchSize));

    // Create updates with unique date_path_ref and unique target blob store for each blob
    List<BlobLocationUpdate> updates = new ArrayList<>();
    Map<String, BlobLocationUpdate> targetBlobStoresWithBlobLocation = new HashMap<>();
    for (int i = 0; i < batchSize; i++) {
      String blobId = "blob-" + i;
      OffsetDateTime uniqueDatePathRef = baseTime.plusSeconds(i);
      String targetBlobStore = "targetStore-" + i;
      BlobLocationUpdate blobLocationUpdate = new BlobLocationUpdate(blobId, targetBlobStore, uniqueDatePathRef);
      updates.add(blobLocationUpdate);
      targetBlobStoresWithBlobLocation.put(blobId, blobLocationUpdate);
    }

    // Execute batch update
    int updated = dao.batchUpdateBlobLocation(updates, oldBlobStoreName);
    assertThat(updated, is(batchSize));

    // Verify old blob store is empty
    assertThat(dao.count(oldBlobStoreName), is(0));

    // Verify each blob has its unique date_path_ref and unique blob store name
    for (int i = 0; i < batchSize; i++) {
      String targetStore = "targetStore-" + i;
      Continuation<SoftDeletedBlob> records = dao.readRecords(null, 1, targetStore);
      assertThat("Expected exactly 1 blob in " + targetStore, records.size(), is(1));

      SoftDeletedBlob record = records.stream().findFirst().orElseThrow();
      String blobId = record.getBlobId();

      OffsetDateTime expectedDatePathRef = targetBlobStoresWithBlobLocation.get(blobId).newDatePathRef();
      assertThat("date_path_ref mismatch for " + blobId,
          record.getDatePathRef(), is(expectedDatePathRef));

      String expectedBlobStoreName = targetBlobStoresWithBlobLocation.get(blobId).newSourceBlobStoreName();
      assertThat("source_blob_store_name mismatch for " + blobId,
          record.getSourceBlobStoreName(), is(expectedBlobStoreName));
    }
  }
}
