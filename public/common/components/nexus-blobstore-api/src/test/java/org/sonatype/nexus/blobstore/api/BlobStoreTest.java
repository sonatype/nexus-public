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
package org.sonatype.nexus.blobstore.api;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the default methods of {@link BlobStore}.
 */
public class BlobStoreTest
{
  private BlobStore underTest;

  private BlobId blobId;

  private Blob blob;

  private Map<String, String> headers;

  @Before
  public void setUp() {
    underTest = mock(BlobStore.class, CALLS_REAL_METHODS);
    blobId = new BlobId("test-blob-id");
    blob = mock(Blob.class);
    headers = Collections.singletonMap(BlobStore.BLOB_NAME_HEADER, "name");
  }

  @Test
  public void getByBlobRefDelegatesToGetByBlobId() {
    BlobRef blobRef = mock(BlobRef.class);
    when(blobRef.getBlobId()).thenReturn(blobId);
    doReturn(blob).when(underTest).get(blobId);

    Blob result = underTest.get(blobRef);

    assertThat(result, is(sameInstance(blob)));
    verify(underTest).get(blobId);
  }

  @Test
  public void getByBlobRefReturnsNullWhenBlobMissing() {
    BlobRef blobRef = mock(BlobRef.class);
    when(blobRef.getBlobId()).thenReturn(blobId);
    doReturn(null).when(underTest).get(blobId);

    Blob result = underTest.get(blobRef);

    assertThat(result, is(nullValue()));
    verify(underTest).get(blobId);
  }

  @Test
  public void makeBlobPermanentDelegatesToCopy() {
    Blob copied = mock(Blob.class);
    when(blob.getId()).thenReturn(blobId);
    doReturn(copied).when(underTest).copy(blobId, headers);

    Blob result = underTest.makeBlobPermanent(blob, headers);

    assertThat(result, is(sameInstance(copied)));
    verify(underTest).copy(blobId, headers);
  }

  @Test
  public void isGroupableReturnsTrueByDefault() {
    assertThat(underTest.isGroupable(), is(true));
  }

  @Test
  public void isWritableReadsConfigurationWritableTrue() {
    BlobStoreConfiguration configuration = mock(BlobStoreConfiguration.class);
    when(configuration.isWritable()).thenReturn(true);
    doReturn(configuration).when(underTest).getBlobStoreConfiguration();

    assertThat(underTest.isWritable(), is(true));
    verify(underTest).getBlobStoreConfiguration();
    verify(configuration).isWritable();
  }

  @Test
  public void isWritableReadsConfigurationWritableFalse() {
    BlobStoreConfiguration configuration = mock(BlobStoreConfiguration.class);
    when(configuration.isWritable()).thenReturn(false);
    doReturn(configuration).when(underTest).getBlobStoreConfiguration();

    assertThat(underTest.isWritable(), is(false));
    verify(configuration).isWritable();
  }

  @Test
  public void asyncDeleteCompletesWithDeleteHardResult() throws Exception {
    doReturn(true).when(underTest).deleteHard(blobId);

    Future<Boolean> result = underTest.asyncDelete(blobId);

    assertThat(result.isDone(), is(true));
    assertThat(result.get(), is(true));
    verify(underTest).deleteHard(blobId);
  }

  @Test
  public void asyncDeleteCompletesWithFalseWhenNothingDeleted() throws Exception {
    doReturn(false).when(underTest).deleteHard(blobId);

    Future<Boolean> result = underTest.asyncDelete(blobId);

    assertThat(result.get(), is(false));
    assertThat(result.isDone(), is(true));
    verify(underTest).deleteHard(blobId);
  }

  @Test(expected = BlobStoreException.class)
  public void asyncDeletePropagatesExceptionFromDeleteHard() {
    // The default wraps a synchronous deleteHard in a completed future, so a failure in
    // deleteHard propagates synchronously out of asyncDelete rather than being captured in
    // the returned future.
    doThrow(new BlobStoreException("boom", null)).when(underTest).deleteHard(blobId);

    underTest.asyncDelete(blobId);
  }

  @Test
  public void deleteIfTempDelegatesToDeleteHard() {
    when(blob.getId()).thenReturn(blobId);
    doReturn(true).when(underTest).deleteHard(blobId);

    assertThat(underTest.deleteIfTemp(blob), is(true));
    verify(underTest).deleteHard(blobId);
  }

  @Test
  public void deleteIfTempReturnsFalseWhenDeleteHardReturnsFalse() {
    when(blob.getId()).thenReturn(blobId);
    doReturn(false).when(underTest).deleteHard(blobId);

    assertThat(underTest.deleteIfTemp(blob), is(false));
    verify(underTest).deleteHard(blobId);
  }

  @Test
  public void validateCanCreateAndUpdateIsNoOp() throws Exception {
    underTest.validateCanCreateAndUpdate();
  }

  @Test
  public void flushMetricsIsNoOp() throws Exception {
    underTest.flushMetrics();
  }

  @Test
  public void getExternalMetadataByBlobRefDelegatesToBlobId() {
    BlobRef blobRef = mock(BlobRef.class);
    ExternalMetadata metadata = new ExternalMetadata("etag", OffsetDateTime.now());
    when(blobRef.getBlobId()).thenReturn(blobId);
    doReturn(Optional.of(metadata)).when(underTest).getExternalMetadata(blobId);

    Optional<ExternalMetadata> result = underTest.getExternalMetadata(blobRef);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), is(sameInstance(metadata)));
    verify(underTest).getExternalMetadata(blobId);
  }

  @Test
  public void getExternalMetadataByBlobIdReturnsEmptyByDefault() {
    Optional<ExternalMetadata> result = underTest.getExternalMetadata(blobId);

    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void createBlobAttributesIsNoOp() {
    BlobMetrics blobMetrics = mock(BlobMetrics.class);

    underTest.createBlobAttributes(blobId, headers, blobMetrics);

    verifyNoInteractions(blobMetrics);
  }
}
