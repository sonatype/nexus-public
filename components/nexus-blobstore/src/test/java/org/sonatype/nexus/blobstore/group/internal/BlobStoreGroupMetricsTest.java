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
package org.sonatype.nexus.blobstore.group.internal;

import java.util.Arrays;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;

import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BlobStoreGroupMetricsTest
    extends TestSupport
{

  @Test
  public void emptyMetricsIsAvailable() {
    assertThat(new BlobStoreGroupMetrics(emptyList()).isUnavailable(), is(false));
  }

  @Test
  public void metricsWithNoAvailableMemberIsUnavailable() {
    BlobStoreMetrics blobStoreMetrics = mock(BlobStoreMetrics.class);
    BlobStoreMetrics otherBlobStoreMetrics = mock(BlobStoreMetrics.class);
    when(blobStoreMetrics.isUnavailable()).thenReturn(true);
    when(otherBlobStoreMetrics.isUnavailable()).thenReturn(true);
    BlobStoreGroupMetrics groupMetrics =
        new BlobStoreGroupMetrics(Arrays.asList(blobStoreMetrics, otherBlobStoreMetrics));
    verify(blobStoreMetrics).isUnavailable();
    verify(blobStoreMetrics).getAvailableSpaceByFileStore();
    verify(otherBlobStoreMetrics).isUnavailable();
    verify(otherBlobStoreMetrics).getAvailableSpaceByFileStore();
    assertThat(groupMetrics.isUnavailable(), is(true));
  }

  @Test
  public void metricsWithOneAvailableMemberIsAvailable() {
    BlobStoreMetrics blobStoreMetrics = mock(BlobStoreMetrics.class);
    when(blobStoreMetrics.isUnavailable()).thenReturn(true);
    BlobStoreMetrics otherBlobStoreMetrics = mock(BlobStoreMetrics.class);
    BlobStoreGroupMetrics groupMetrics =
        new BlobStoreGroupMetrics(Arrays.asList(blobStoreMetrics, otherBlobStoreMetrics));
    verify(blobStoreMetrics).isUnavailable();
    verify(blobStoreMetrics).getAvailableSpaceByFileStore();
    verify(otherBlobStoreMetrics).isUnavailable();
    verify(otherBlobStoreMetrics).getAvailableSpaceByFileStore();
    assertThat(groupMetrics.isUnavailable(), is(false));
  }
}
