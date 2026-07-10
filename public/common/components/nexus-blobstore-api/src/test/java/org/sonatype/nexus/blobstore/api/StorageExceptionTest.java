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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests for {@link StorageException}.
 */
public class StorageExceptionTest
{
  private static final String PATH = "some/blob/path";

  @Test
  public void testNotFound() {
    StorageException exception = StorageException.notFound(PATH);

    assertThat(exception.code(), is(StorageException.ErrorCode.NOT_FOUND));
    assertThat(exception.path(), is(PATH));
    assertThat(exception.getMessage(), is("Missing path " + PATH));
    assertThat(exception.getCause(), is(nullValue()));
  }

  @Test
  public void testTimeout() {
    Throwable cause = new RuntimeException("boom");
    StorageException exception = StorageException.timeout(PATH, cause);

    assertThat(exception.code(), is(StorageException.ErrorCode.TIMEOUT));
    assertThat(exception.path(), is(PATH));
    assertThat(exception.getMessage(), is("Timeout while retrieving " + PATH));
    assertThat(exception.getCause(), is(sameInstance(cause)));
  }

  @Test
  public void testTimeoutWithNullCause() {
    StorageException exception = StorageException.timeout(PATH, null);

    assertThat(exception.code(), is(StorageException.ErrorCode.TIMEOUT));
    assertThat(exception.path(), is(PATH));
    assertThat(exception.getMessage(), is("Timeout while retrieving " + PATH));
    assertThat(exception.getCause(), is(nullValue()));
  }

  @Test
  public void testNotFoundWithNullPath() {
    StorageException exception = StorageException.notFound(null);

    assertThat(exception.code(), is(StorageException.ErrorCode.NOT_FOUND));
    assertThat(exception.path(), is(nullValue()));
    assertThat(exception.getMessage(), is("Missing path null"));
    assertThat(exception.getCause(), is(nullValue()));
  }

  @Test
  public void testTimeoutWithNullPath() {
    Throwable cause = new RuntimeException("boom");
    StorageException exception = StorageException.timeout(null, cause);

    assertThat(exception.code(), is(StorageException.ErrorCode.TIMEOUT));
    assertThat(exception.path(), is(nullValue()));
    assertThat(exception.getMessage(), is("Timeout while retrieving null"));
    assertThat(exception.getCause(), is(sameInstance(cause)));
  }

  @Test
  public void testInheritsFromRuntimeException() {
    StorageException exception = StorageException.notFound(PATH);

    assertThat(exception instanceof RuntimeException, is(true));
  }

  @Test
  public void testErrorCodeValues() {
    assertThat(StorageException.ErrorCode.values(),
        arrayContaining(StorageException.ErrorCode.NOT_FOUND, StorageException.ErrorCode.TIMEOUT));
  }

  @Test
  public void testErrorCodeValueOf() {
    assertThat(StorageException.ErrorCode.valueOf("NOT_FOUND"), is(StorageException.ErrorCode.NOT_FOUND));
    assertThat(StorageException.ErrorCode.valueOf("TIMEOUT"), is(StorageException.ErrorCode.TIMEOUT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testErrorCodeValueOfInvalid() {
    StorageException.ErrorCode.valueOf("DOES_NOT_EXIST");
  }

  @Test(expected = NullPointerException.class)
  public void testErrorCodeValueOfNull() {
    StorageException.ErrorCode.valueOf(null);
  }

  @Test
  public void testErrorCodeMessageNotFound() {
    assertThat(StorageException.ErrorCode.NOT_FOUND.message(PATH), is("Missing path " + PATH));
  }

  @Test
  public void testErrorCodeMessageTimeout() {
    assertThat(StorageException.ErrorCode.TIMEOUT.message(PATH), is("Timeout while retrieving " + PATH));
  }

  @Test
  public void testErrorCodeMessageWithNullPath() {
    assertThat(StorageException.ErrorCode.NOT_FOUND.message(null), is("Missing path null"));
    assertThat(StorageException.ErrorCode.TIMEOUT.message(null), is("Timeout while retrieving null"));
  }
}
