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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BlobStoreStartedEvent}.
 */
public class BlobStoreStartedEventTest
{
  private final BlobStore blobStore = mock(BlobStore.class);

  @Test
  public void getBlobStoreReturnsConstructorArgument() {
    BlobStoreStartedEvent underTest = new BlobStoreStartedEvent(blobStore);

    assertThat(underTest.getBlobStore(), is(sameInstance(blobStore)));
  }

  @Test
  public void toStringContainsSimpleNameAndBlobStore() {
    BlobStoreStartedEvent underTest = new BlobStoreStartedEvent(blobStore);

    assertThat(underTest.toString(), is(equalTo("BlobStoreStartedEvent{blobStore=" + blobStore + "}")));
  }

  @Test(expected = NullPointerException.class)
  public void nullBlobStoreIsRejected() {
    new BlobStoreStartedEvent(null);
  }
}
