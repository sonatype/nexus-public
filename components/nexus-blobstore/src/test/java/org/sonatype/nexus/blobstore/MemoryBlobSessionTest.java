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
package org.sonatype.nexus.blobstore;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobSession;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import com.google.common.hash.HashCode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.transaction.Transactional.DEFAULT_REASON;

/**
 * Tests for {@link MemoryBlobSession}.
 */
public class MemoryBlobSessionTest
    extends TestSupport
{
  private static final long TEST_BLOB_SIZE = 1024;

  private static final HashCode TEST_BLOB_HASH = HashCode.fromInt(1234567890);

  private static final BlobId EXISTING_BLOB_ID = new BlobId("existing-blob");

  private static final BlobId RESTORED_BLOB_ID = new BlobId("restored-blob");

  private static final BlobId COPIED_BLOB_ID = new BlobId("copied-blob");

  private static final String TEST_COMMIT_REASON = "committing " + DEFAULT_REASON;

  private static final String TEST_ROLLBACK_REASON = "rolling back " + DEFAULT_REASON;

  @Mock
  private BlobStore blobStore;

  @Mock
  private InputStream blobData;

  @Mock
  private Path sourceFile;

  @Mock
  private Map<String, String> headers;

  private int blobIdSequence = 1;

  @Before
  public void setUp() {
    Blob restoredBlob = mockBlob(RESTORED_BLOB_ID);
    Blob copiedBlob = mockBlob(COPIED_BLOB_ID);

    when(blobStore.create(blobData, headers, null)).thenAnswer(this::newBlob);
    when(blobStore.create(blobData, headers, RESTORED_BLOB_ID)).thenReturn(restoredBlob);
    when(blobStore.create(sourceFile, headers, TEST_BLOB_SIZE, TEST_BLOB_HASH)).thenAnswer(this::newBlob);
    when(blobStore.copy(EXISTING_BLOB_ID, headers)).thenReturn(copiedBlob);

    when(blobStore.exists(any())).thenReturn(true);
    when(blobStore.get(any())).thenAnswer(this::getBlob);

    BlobStoreConfiguration storeConfiguration = mock(BlobStoreConfiguration.class);
    when(storeConfiguration.getName()).thenReturn("test-blob-store");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(storeConfiguration);
  }

  @Test
  public void commitAppliesPendingDeletes() throws Exception {
    try (BlobSession<?> session = new MemoryBlobSession(blobStore)) {
      exerciseBlobSession(session);

      InOrder order = inOrder(blobStore);
      order.verify(blobStore).create(blobData, headers, null);
      order.verify(blobStore).create(blobData, headers, RESTORED_BLOB_ID);
      order.verify(blobStore).create(sourceFile, headers, TEST_BLOB_SIZE, TEST_BLOB_HASH);
      order.verify(blobStore).copy(EXISTING_BLOB_ID, headers);

      session.getTransaction().commit();
    }

    // these deletes may be applied in any order
    verify(blobStore).delete(EXISTING_BLOB_ID, TEST_COMMIT_REASON);
    verify(blobStore).delete(new BlobId("new-blob-1"), TEST_COMMIT_REASON);

    verifyNoMoreInteractions(blobStore);
  }

  @Test
  public void rollbackDeletesUncommittedBlobs() throws Exception {
    try (BlobSession<?> session = new MemoryBlobSession(blobStore)) {
      exerciseBlobSession(session);

      InOrder order = inOrder(blobStore);
      order.verify(blobStore).create(blobData, headers, null);
      order.verify(blobStore).create(blobData, headers, RESTORED_BLOB_ID);
      order.verify(blobStore).create(sourceFile, headers, TEST_BLOB_SIZE, TEST_BLOB_HASH);
      order.verify(blobStore).copy(EXISTING_BLOB_ID, headers);

      session.getTransaction().rollback();
    }

    // these deletes may be applied in any order
    verify(blobStore).delete(new BlobId("new-blob-1"), TEST_ROLLBACK_REASON);
    verify(blobStore).delete(RESTORED_BLOB_ID, TEST_ROLLBACK_REASON);
    verify(blobStore).delete(new BlobId("new-blob-2"), TEST_ROLLBACK_REASON);
    verify(blobStore).delete(COPIED_BLOB_ID, TEST_ROLLBACK_REASON);

    verifyNoMoreInteractions(blobStore);
  }

  @Test
  public void uncommittedChangesRolledBackOnClose() throws Exception {
    try (BlobSession<?> session = new MemoryBlobSession(blobStore)) {

      exerciseBlobSession(session);

      InOrder order = inOrder(blobStore);
      order.verify(blobStore).create(blobData, headers, null);
      order.verify(blobStore).create(blobData, headers, RESTORED_BLOB_ID);
      order.verify(blobStore).create(sourceFile, headers, TEST_BLOB_SIZE, TEST_BLOB_HASH);
      order.verify(blobStore).copy(EXISTING_BLOB_ID, headers);
    }

    // these deletes may be applied in any order
    verify(blobStore).delete(new BlobId("new-blob-1"), TEST_ROLLBACK_REASON);
    verify(blobStore).delete(RESTORED_BLOB_ID, TEST_ROLLBACK_REASON);
    verify(blobStore).delete(new BlobId("new-blob-2"), TEST_ROLLBACK_REASON);
    verify(blobStore).delete(COPIED_BLOB_ID, TEST_ROLLBACK_REASON);

    verifyNoMoreInteractions(blobStore);
  }

  @Test
  public void getRespectsPendingDeletes() throws Exception {
    try (BlobSession<?> session = new MemoryBlobSession(blobStore)) {
      BlobId testId = session.create(blobData, headers).getId();

      assertThat(session.exists(testId), is(true));
      assertThat(session.get(testId).getId(), is(testId));
      assertThat(session.get(testId, false).getId(), is(testId));
      assertThat(session.get(testId, true).getId(), is(testId));

      session.delete(testId);

      assertThat(session.exists(testId), is(true)); // 'exists' doesn't take soft-deletes into account
      assertThat(session.get(testId), is(nullValue()));
      assertThat(session.get(testId, false), is(nullValue()));
      assertThat(session.get(testId, true).getId(), is(testId));
    }
  }

  @Test
  public void commitNeverFails() throws Exception {
    // use Error to make sure we're also catching serious errors
    when(blobStore.delete(any(), any())).thenThrow(new Error("Simulated store error"));
    try (BlobSession<?> session = new MemoryBlobSession(blobStore)) {
      session.delete(EXISTING_BLOB_ID);
      session.getTransaction().commit();
    }
    catch (Throwable t) {
      //explictly having an assertion pleases sonar
      fail("transaction commit not reached");
    }

    // now make even accessing the store config fail badly
    when(blobStore.getBlobStoreConfiguration()).thenThrow(new Error("Simulated config error"));
    try (BlobSession<?> session = new MemoryBlobSession(blobStore)) {
      session.delete(EXISTING_BLOB_ID);
      session.getTransaction().commit();
    }
    catch (Throwable t) {
      //explictly having an assertion pleases sonar
      fail("transaction commit not reached");
    }
  }

  @Test
  public void rollbackNeverFails() throws Exception {
    // use Error to make sure we're also catching serious errors
    when(blobStore.delete(any(), any())).thenThrow(new Error("Simulated store error"));
    try (BlobSession<?> session = new MemoryBlobSession(blobStore)) {
      session.create(blobData, headers);
      session.getTransaction().rollback();
    }
    catch (Throwable t) {
      //explictly having an assertion pleases sonar
      fail("rollback may have failed");
    }

    // now make even accessing the store config fail badly
    when(blobStore.getBlobStoreConfiguration()).thenThrow(new Error("Simulated config error"));
    try (BlobSession<?> session = new MemoryBlobSession(blobStore)) {
      session.create(blobData, headers);
      session.getTransaction().rollback();
    }
    catch (Throwable t) {
      //explictly having an assertion pleases sonar
      fail("rollback may have failed");
    }
  }

  private Blob newBlob(final InvocationOnMock unused) {
    return mockBlob(new BlobId("new-blob-" + (blobIdSequence++)));
  }

  private Blob getBlob(final InvocationOnMock invocation) {
    return mockBlob((BlobId) invocation.getArguments()[0]);
  }

  private Blob mockBlob(final BlobId blobId) {
    Blob blob = mock(Blob.class);
    when(blob.getId()).thenReturn(blobId);
    return blob;
  }

  private void exerciseBlobSession(final BlobSession<?> session) {
    Blob newBlob = session.create(blobData, headers);
    session.create(blobData, headers, RESTORED_BLOB_ID);
    session.create(sourceFile, headers, TEST_BLOB_SIZE, TEST_BLOB_HASH);
    session.copy(EXISTING_BLOB_ID, headers);
    session.delete(EXISTING_BLOB_ID);
    session.delete(newBlob.getId()); // go back and delete the first blob we created in this session
  }
}
