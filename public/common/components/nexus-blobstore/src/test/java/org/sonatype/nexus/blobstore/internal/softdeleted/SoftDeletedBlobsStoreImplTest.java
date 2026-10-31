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

import java.util.stream.Collectors;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@ExtendWith(MockitoExtension.class)
class SoftDeletedBlobsStoreImplTest
{
  private static final String BLOB_STORE = "test-bs";

  @Mock
  private DataSessionSupplier sessionSupplier;

  @SuppressWarnings("rawtypes")
  @Mock
  private DataSession dataSession;

  @Mock
  private Transaction transaction;

  @Mock
  private SoftDeletedBlobsDAO dao;

  private SoftDeletedBlobsStoreImpl underTest;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    lenient().when(sessionSupplier.openSession(DEFAULT_DATASTORE_NAME)).thenReturn(dataSession);
    lenient().when(dataSession.access(SoftDeletedBlobsDAO.class)).thenReturn(dao);
    lenient().when(dataSession.getTransaction()).thenReturn(transaction);
    UnitOfWork.beginBatch(dataSession);
    underTest = new SoftDeletedBlobsStoreImpl(sessionSupplier);
  }

  @AfterEach
  void tearDown() {
    UnitOfWork.end();
  }

  @Test
  void deleteRecords_nullCollection_isNoOp() {
    underTest.deleteRecords(BLOB_STORE, null);
    verify(dao, never()).deleteRecords(any(), any());
  }

  @Test
  void deleteRecords_emptyCollection_isNoOp() {
    underTest.deleteRecords(BLOB_STORE, List.of());
    verify(dao, never()).deleteRecords(any(), any());
  }

  @Test
  void deleteRecords_oversizeBatch_throwsBeforeHittingDao() {
    List<BlobId> tooMany = IntStream.range(0, 1001)
        .mapToObj(i -> new BlobId("b" + i))
        .collect(Collectors.toList());

    assertThatThrownBy(() -> underTest.deleteRecords(BLOB_STORE, tooMany))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be <= 1000");

    verify(dao, never()).deleteRecords(any(), any());
  }

  @Test
  void deleteRecords_happyPath_passesStringIdsToDao() {
    List<BlobId> ids = List.of(new BlobId("alpha"), new BlobId("beta"));
    underTest.deleteRecords(BLOB_STORE, ids);

    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass((Class) Collection.class);
    verify(dao).deleteRecords(eq(BLOB_STORE), captor.capture());
    assertThat(captor.getValue()).containsExactly("alpha", "beta");
  }
}
