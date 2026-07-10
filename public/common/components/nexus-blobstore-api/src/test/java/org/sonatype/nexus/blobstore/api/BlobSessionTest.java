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

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for the default methods of {@link BlobSession}.
 */
public class BlobSessionTest
{
  private BlobSession<?> underTest;

  private InputStream blobData;

  private Map<String, String> headers;

  private BlobId blobId;

  private Blob blob;

  @Before
  public void setUp() {
    underTest = mock(BlobSession.class, CALLS_REAL_METHODS);
    blobData = mock(InputStream.class);
    headers = Collections.singletonMap(BlobStore.BLOB_NAME_HEADER, "name");
    blobId = new BlobId("test-blob-id");
    blob = mock(Blob.class);
  }

  @Test
  public void createWithoutBlobIdDelegatesToCreateWithNullBlobId() {
    doReturn(blob).when(underTest).create(blobData, headers, null);

    Blob result = underTest.create(blobData, headers);

    assertThat(result, is(sameInstance(blob)));
    verify(underTest).create(blobData, headers, null);
  }

  @Test
  public void getDelegatesToGetWithIncludeDeletedFalse() {
    doReturn(blob).when(underTest).get(blobId, false);

    Blob result = underTest.get(blobId);

    assertThat(result, is(sameInstance(blob)));
    verify(underTest).get(blobId, false);
  }

  @Test
  public void getReturnsNullWhenBlobMissing() {
    doReturn(null).when(underTest).get(blobId, false);

    Blob result = underTest.get(blobId);

    assertThat(result, is(nullValue()));
    verify(underTest).get(blobId, false);
  }
}
