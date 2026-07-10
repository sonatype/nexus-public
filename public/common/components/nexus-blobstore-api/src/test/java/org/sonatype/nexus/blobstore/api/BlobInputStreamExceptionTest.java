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

import java.io.IOException;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests for {@link BlobInputStreamException}.
 */
public class BlobInputStreamExceptionTest
{
  private static final BlobId BLOB_ID = new BlobId("blob-123", null);

  @Test
  public void testCauseAndBlobIdConstructor() {
    IOException cause = new IOException("disk error");
    BlobInputStreamException exception = new BlobInputStreamException(cause, BLOB_ID);

    assertThat(exception.getBlobId(), is(sameInstance(BLOB_ID)));
    assertThat(exception.getCause(), is(sameInstance((Throwable) cause)));
    // super(cause) sets the detail message to cause.toString()
    assertThat(exception.getMessage(),
        is("BlobId: blob-123, java.io.IOException: disk error, Cause: disk error"));
  }

  @Test
  public void testCauseConstructorWithNullBlobId() {
    IOException cause = new IOException("disk error");
    BlobInputStreamException exception = new BlobInputStreamException(cause, null);

    assertThat(exception.getBlobId(), is(nullValue()));
    assertThat(exception.getCause(), is(sameInstance((Throwable) cause)));
    assertThat(exception.getMessage(), is("java.io.IOException: disk error, Cause: disk error"));
  }

  @Test
  public void testCauseWithNullMessage() {
    RuntimeException cause = new RuntimeException();
    BlobInputStreamException exception = new BlobInputStreamException(cause, null);

    assertThat(exception.getBlobId(), is(nullValue()));
    assertThat(exception.getCause(), is(sameInstance((Throwable) cause)));
    // cause has no message, so the "Cause:" segment is omitted
    assertThat(exception.getMessage(), is("java.lang.RuntimeException"));
  }

  @Test
  public void testBlobIdPresentWithCauseLackingMessage() {
    RuntimeException cause = new RuntimeException();
    BlobInputStreamException exception = new BlobInputStreamException(cause, BLOB_ID);

    assertThat(exception.getBlobId(), is(sameInstance(BLOB_ID)));
    assertThat(exception.getCause(), is(sameInstance((Throwable) cause)));
    // blobId is present but the cause has no message, so the "Cause:" segment is omitted
    assertThat(exception.getMessage(), is("BlobId: blob-123, java.lang.RuntimeException"));
  }

  @Test
  public void testInheritsFromBlobStoreException() {
    BlobInputStreamException exception = new BlobInputStreamException(new IOException("oops"), BLOB_ID);

    assertThat(exception instanceof BlobStoreException, is(true));
    assertThat(exception instanceof RuntimeException, is(true));
  }
}
