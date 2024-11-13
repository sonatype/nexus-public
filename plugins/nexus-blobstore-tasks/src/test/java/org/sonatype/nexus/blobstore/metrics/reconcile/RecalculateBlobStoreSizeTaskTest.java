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
package org.sonatype.nexus.blobstore.metrics.reconcile;

import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Stream;

import org.sonatype.goodies.common.MultipleFailures.MultipleFailuresException;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.BlobAttributesSupport;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsService;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.common.BlobStoreTaskSupport.ALL;
import static org.sonatype.nexus.blobstore.common.BlobStoreTaskSupport.BLOBSTORE_NAME_FIELD_ID;

public class RecalculateBlobStoreSizeTaskTest
    extends TestSupport
{
  @Mock
  private BlobStoreManager blobStoreManager;

  private RecalculateBlobStoreSizeTask underTest;

  @Before
  public void setUp() {
    underTest = spy(new RecalculateBlobStoreSizeTask(blobStoreManager));
  }

  @Test
  public void testTaskWorksAsExpectedWithSingleBlobStore() throws Exception {
    Pair<BlobStore, BlobStoreMetricsService> mocks = mockBlobStore("single-blobstore", 10, false);

    TaskConfiguration configuration = buildTaskConfiguration("test-single-blobstore", "single-blobstore");

    underTest.configure(configuration);
    underTest.call();

    verify(underTest, times(1)).execute(mocks.getLeft());
    verify(mocks.getLeft(), times(10)).getBlobAttributes(any(BlobId.class));
    verify(mocks.getRight(), times(10)).recordAddition(anyLong());
  }

  @Test
  public void testTaskWorksAsExpectedWithAllBlobStores() throws Exception {
    Pair<BlobStore, BlobStoreMetricsService> blobstore1Mocks = mockBlobStore("test-blobstore-1", 10, false);
    Pair<BlobStore, BlobStoreMetricsService> blobstore2Mocks = mockBlobStore("test-blobstore-2", 25, false);
    Pair<BlobStore, BlobStoreMetricsService> blobstore3Mocks = mockBlobStore("test-blobstore-3", 12, false);

    TaskConfiguration configuration = buildTaskConfiguration("test-all-blobstores", ALL);

    when(blobStoreManager.browse()).thenReturn(
        ImmutableList.of(blobstore1Mocks.getLeft(), blobstore2Mocks.getLeft(), blobstore3Mocks.getLeft()));

    underTest.configure(configuration);
    underTest.call();

    verify(underTest, times(3)).execute(any(BlobStore.class));
    verify(blobstore1Mocks.getRight(), times(10)).recordAddition(anyLong());
    verify(blobstore2Mocks.getRight(), times(25)).recordAddition(anyLong());
    verify(blobstore3Mocks.getRight(), times(12)).recordAddition(anyLong());
  }

  @Test
  public void testTaskPropagateFailuresAsExpected() {
    Pair<BlobStore, BlobStoreMetricsService> unavailableMocks = mockBlobStore("unavailable-blobstore", 3, true);
    Pair<BlobStore, BlobStoreMetricsService> available1Mocks = mockBlobStore("available-blobstore-1", 56, false);
    Pair<BlobStore, BlobStoreMetricsService> available2Mocks = mockBlobStore("available-blobstore-2", 23, false);

    TaskConfiguration configuration = buildTaskConfiguration("test-multiple-failures", ALL);

    when(blobStoreManager.browse()).thenReturn(
        ImmutableList.of(unavailableMocks.getLeft(), available1Mocks.getLeft(), available2Mocks.getLeft()));

    underTest.configure(configuration);
    assertThrows(MultipleFailuresException.class, () -> underTest.call());
    verify(underTest, times(3)).execute(any(BlobStore.class));
    verify(unavailableMocks.getRight(), never()).recordAddition(anyLong());
    verify(available1Mocks.getRight(), times(56)).recordAddition(anyLong());
    verify(available2Mocks.getRight(), times(23)).recordAddition(anyLong());
  }

  private Pair<BlobStore, BlobStoreMetricsService> mockBlobStore(
      final String blobstoreName,
      final int blobsCount,
      final boolean throwException)
  {
    BlobStoreMetricsService metricsService = mock(BlobStoreMetricsService.class);
    BlobStoreConfiguration configuration = mock(BlobStoreConfiguration.class);
    BlobStore blobStore = mock(BlobStore.class);
    when(blobStoreManager.get(blobstoreName)).thenReturn(blobStore);
    when(configuration.getName()).thenReturn(blobstoreName);
    when(configuration.getType()).thenReturn(FileBlobStore.TYPE);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);
    when(blobStore.getMetricsService()).thenReturn(metricsService);

    if (throwException) {
      when(blobStore.getBlobIdStream()).thenThrow(new IllegalStateException("unavailable blobstore"));
    }
    else {
      when(blobStore.getBlobIdStream()).thenAnswer((invocation) -> Stream.iterate(0, n -> n + 1)
          .limit(blobsCount)
          .map((n) -> new BlobId(n.toString())));
    }

    when(blobStore.getBlobAttributes(any())).thenAnswer((invocation) -> {
      Random random = new Random();

      Map<String, String> headers = ImmutableMap.of();
      BlobMetrics metrics = new BlobMetrics(DateTime.now().minusHours(1), "hash", random.nextInt(100));
      return new TestBlobAttributes(headers, metrics);
    });

    return Pair.of(blobStore, metricsService);
  }

  private TaskConfiguration buildTaskConfiguration(final String taskName, final String blobStoreField) {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setId(taskName);
    taskConfiguration.setTypeId(RecalculateBlobStoreSizeTaskDescriptor.TYPE_ID);
    taskConfiguration.setString(".name", taskName);
    taskConfiguration.setString(BLOBSTORE_NAME_FIELD_ID, blobStoreField);

    return taskConfiguration;
  }

  private static class TestBlobAttributes
      extends BlobAttributesSupport<Properties>
  {
    public Properties properties;

    public TestBlobAttributes(final Map<String, String> headers, final BlobMetrics blobMetrics) {
      super(new Properties(), headers, blobMetrics);
      this.properties = propertiesFile;
    }

    @Override
    public void store() {
      writeTo(properties);
    }

    @Override
    public void writeProperties() {
      writeTo(propertiesFile);
    }
  }
}
