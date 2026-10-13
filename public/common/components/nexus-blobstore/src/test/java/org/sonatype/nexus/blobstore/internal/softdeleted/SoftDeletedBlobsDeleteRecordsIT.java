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

import java.util.List;
import java.util.stream.IntStream;

import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlob;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class SoftDeletedBlobsDeleteRecordsIT
{
  private static final String BLOB_STORE = "test-bs";

  @DataSessionConfiguration(daos = {SoftDeletedBlobsDAO.class})
  TestDataSessionSupplier supplier;

  @DatabaseTest
  void deleteRecords_removesExactMatchesLeavingOthersIntact() {
    insert("a", "b", "c", "d");

    supplier.callDAO(SoftDeletedBlobsDAO.class,
        dao -> dao.deleteRecords(BLOB_STORE, List.of("b", "d")));

    List<String> remaining = supplier.withDAO(SoftDeletedBlobsDAO.class,
        dao -> dao.readRecords(null, 100, BLOB_STORE)
            .stream()
            .map(SoftDeletedBlob::getBlobId)
            .collect(toList()));
    assertThat(remaining).containsExactlyInAnyOrder("a", "c");
  }

  @DatabaseTest
  void deleteRecords_singleId_removesOneRow() {
    insert("x", "y");

    supplier.callDAO(SoftDeletedBlobsDAO.class,
        dao -> dao.deleteRecords(BLOB_STORE, List.of("x")));

    assertThat(count()).isEqualTo(1);
  }

  @DatabaseTest
  void deleteRecords_allIds_emptiesTable() {
    insert("p", "q", "r");

    supplier.callDAO(SoftDeletedBlobsDAO.class,
        dao -> dao.deleteRecords(BLOB_STORE, List.of("p", "q", "r")));

    assertThat(count()).isEqualTo(0);
  }

  @DatabaseTest
  void deleteRecords_largeBatch_deletesAll() {
    List<String> ids = IntStream.range(0, 500).mapToObj(i -> "blob-" + i).collect(toList());
    ids.forEach(id -> supplier.callDAO(SoftDeletedBlobsDAO.class,
        dao -> dao.createRecord(BLOB_STORE, id, null)));

    supplier.callDAO(SoftDeletedBlobsDAO.class,
        dao -> dao.deleteRecords(BLOB_STORE, ids));

    assertThat(count()).isEqualTo(0);
  }

  @DatabaseTest
  void deleteRecords_nonExistentIds_isNoOp() {
    insert("keep");

    supplier.callDAO(SoftDeletedBlobsDAO.class,
        dao -> dao.deleteRecords(BLOB_STORE, List.of("missing-1", "missing-2")));

    assertThat(count()).isEqualTo(1);
  }

  @DatabaseTest
  void deleteRecords_doesNotTouchOtherBlobStores() {
    supplier.callDAO(SoftDeletedBlobsDAO.class,
        dao -> dao.createRecord("other-bs", "shared-id", null));
    insert("shared-id");

    supplier.callDAO(SoftDeletedBlobsDAO.class,
        dao -> dao.deleteRecords(BLOB_STORE, List.of("shared-id")));

    Integer otherCount = supplier.withDAO(SoftDeletedBlobsDAO.class, dao -> dao.count("other-bs"));
    assertThat(otherCount).isEqualTo(1);
    assertThat(count()).isEqualTo(0);
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private void insert(final String... ids) {
    for (String id : ids) {
      supplier.callDAO(SoftDeletedBlobsDAO.class,
          dao -> dao.createRecord(BLOB_STORE, id, null));
    }
  }

  private Integer count() {
    return supplier.withDAO(SoftDeletedBlobsDAO.class, dao -> dao.count(BLOB_STORE));
  }
}
