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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.sonatype.nexus.blobstore.api.tasks.BlobStoreConsumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link BlobStoreTaskServiceImpl}.
 */
public class BlobStoreTaskServiceImplTest
{
  private static final String BLOB_STORE_NAME = "test-store";

  private static final String OTHER_BLOB_STORE_NAME = "other-store";

  private BlobStoreConsumer consumerOne;

  private BlobStoreConsumer consumerTwo;

  @Before
  public void setUp() {
    consumerOne = mock(BlobStoreConsumer.class);
    consumerTwo = mock(BlobStoreConsumer.class);
  }

  @Test
  public void constructorRetainsProvidedConsumersForUse() {
    when(consumerOne.isBlobStoreInUse(BLOB_STORE_NAME)).thenReturn(true);

    BlobStoreTaskServiceImpl underTest =
        new BlobStoreTaskServiceImpl(Arrays.asList(consumerOne, consumerTwo));

    // The constructor-supplied consumers must be the ones consulted. Asserted behaviorally
    // (rather than by reading the package-private blobStoreConsumers field) so the test is
    // not coupled to the implementation field name.
    assertThat(underTest.isAnyTaskInUseForBlobStore(BLOB_STORE_NAME), is(true));
    verify(consumerOne).isBlobStoreInUse(BLOB_STORE_NAME);
  }

  @Test
  public void isAnyTaskInUseReturnsTrueWhenAnyConsumerInUse() {
    when(consumerOne.isBlobStoreInUse(BLOB_STORE_NAME)).thenReturn(false);
    when(consumerTwo.isBlobStoreInUse(BLOB_STORE_NAME)).thenReturn(true);
    BlobStoreTaskServiceImpl underTest =
        new BlobStoreTaskServiceImpl(Arrays.asList(consumerOne, consumerTwo));

    assertThat(underTest.isAnyTaskInUseForBlobStore(BLOB_STORE_NAME), is(true));

    // anyMatch must keep evaluating past a false result, so both consumers are consulted with the requested name
    verify(consumerOne).isBlobStoreInUse(BLOB_STORE_NAME);
    verify(consumerTwo).isBlobStoreInUse(BLOB_STORE_NAME);
  }

  @Test
  public void isAnyTaskInUseShortCircuitsAfterFirstMatchingConsumer() {
    when(consumerOne.isBlobStoreInUse(BLOB_STORE_NAME)).thenReturn(true);
    BlobStoreTaskServiceImpl underTest =
        new BlobStoreTaskServiceImpl(Arrays.asList(consumerOne, consumerTwo));

    assertThat(underTest.isAnyTaskInUseForBlobStore(BLOB_STORE_NAME), is(true));

    // anyMatch short-circuits on the first true, so the later consumer must never be queried
    verify(consumerOne).isBlobStoreInUse(BLOB_STORE_NAME);
    verifyNoInteractions(consumerTwo);
  }

  @Test
  public void isAnyTaskInUseReturnsFalseWhenNoConsumerInUse() {
    when(consumerOne.isBlobStoreInUse(BLOB_STORE_NAME)).thenReturn(false);
    when(consumerTwo.isBlobStoreInUse(BLOB_STORE_NAME)).thenReturn(false);
    BlobStoreTaskServiceImpl underTest =
        new BlobStoreTaskServiceImpl(Arrays.asList(consumerOne, consumerTwo));

    assertThat(underTest.isAnyTaskInUseForBlobStore(BLOB_STORE_NAME), is(false));

    // when nothing matches, every consumer must be consulted with the requested name
    verify(consumerOne).isBlobStoreInUse(BLOB_STORE_NAME);
    verify(consumerTwo).isBlobStoreInUse(BLOB_STORE_NAME);
  }

  @Test
  public void isAnyTaskInUseReturnsFalseForEmptyConsumerList() {
    BlobStoreTaskServiceImpl underTest =
        new BlobStoreTaskServiceImpl(Collections.emptyList());

    assertThat(underTest.isAnyTaskInUseForBlobStore(BLOB_STORE_NAME), is(false));
  }

  @Test
  public void isAnyTaskInUseForwardsTheRequestedBlobStoreName() {
    when(consumerOne.isBlobStoreInUse(OTHER_BLOB_STORE_NAME)).thenReturn(false);
    when(consumerTwo.isBlobStoreInUse(OTHER_BLOB_STORE_NAME)).thenReturn(false);
    BlobStoreTaskServiceImpl underTest =
        new BlobStoreTaskServiceImpl(Arrays.asList(consumerOne, consumerTwo));

    underTest.isAnyTaskInUseForBlobStore(OTHER_BLOB_STORE_NAME);

    // the method argument (not a hardcoded constant) must be forwarded to each consumer
    verify(consumerOne).isBlobStoreInUse(OTHER_BLOB_STORE_NAME);
    verify(consumerTwo).isBlobStoreInUse(OTHER_BLOB_STORE_NAME);
  }

  @Test
  public void countTasksInUseReturnsZeroForEmptyConsumerList() {
    BlobStoreTaskServiceImpl underTest =
        new BlobStoreTaskServiceImpl(Collections.emptyList());

    assertThat(underTest.countTasksInUseForBlobStore(BLOB_STORE_NAME), is(0));
  }

  @Test
  public void countTasksInUseSumsUsageCountsAcrossConsumers() {
    when(consumerOne.blobStoreUsageCount(BLOB_STORE_NAME)).thenReturn(2);
    when(consumerTwo.blobStoreUsageCount(BLOB_STORE_NAME)).thenReturn(3);
    BlobStoreTaskServiceImpl underTest =
        new BlobStoreTaskServiceImpl(Arrays.asList(consumerOne, consumerTwo));

    assertThat(underTest.countTasksInUseForBlobStore(BLOB_STORE_NAME), is(5));

    // every consumer must be consulted exactly once with the requested name and its count summed
    verify(consumerOne).blobStoreUsageCount(BLOB_STORE_NAME);
    verify(consumerTwo).blobStoreUsageCount(BLOB_STORE_NAME);
  }

  @Test
  public void countTasksInUseSumsContributionsIncludingZero() {
    when(consumerOne.blobStoreUsageCount(BLOB_STORE_NAME)).thenReturn(0);
    when(consumerTwo.blobStoreUsageCount(BLOB_STORE_NAME)).thenReturn(7);
    BlobStoreTaskServiceImpl underTest =
        new BlobStoreTaskServiceImpl(Arrays.asList(consumerOne, consumerTwo));

    assertThat(underTest.countTasksInUseForBlobStore(BLOB_STORE_NAME), is(7));
  }

  @Test
  public void countTasksInUseForwardsTheRequestedBlobStoreName() {
    when(consumerOne.blobStoreUsageCount(OTHER_BLOB_STORE_NAME)).thenReturn(1);
    when(consumerTwo.blobStoreUsageCount(OTHER_BLOB_STORE_NAME)).thenReturn(4);
    BlobStoreTaskServiceImpl underTest =
        new BlobStoreTaskServiceImpl(Arrays.asList(consumerOne, consumerTwo));

    assertThat(underTest.countTasksInUseForBlobStore(OTHER_BLOB_STORE_NAME), is(5));

    // the method argument (not a hardcoded constant) must be forwarded to each consumer
    verify(consumerOne).blobStoreUsageCount(OTHER_BLOB_STORE_NAME);
    verify(consumerTwo).blobStoreUsageCount(OTHER_BLOB_STORE_NAME);
  }
}
