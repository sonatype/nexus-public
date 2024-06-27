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
package org.sonatype.nexus.repository.content.blobstore.metrics.migration;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsEntity;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsStore;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.content.testsuite.groups.PostgresTestGroup;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.repository.content.blobstore.metrics.BlobStoreMetricsDAO;
import org.sonatype.nexus.repository.content.blobstore.metrics.BlobStoreMetricsStoreImpl;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.transaction.TransactionModule;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Provides;
import org.apache.commons.collections.map.HashedMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * {@link FileBlobStoreMetricsMigrationTask} tests
 */
@Category(PostgresTestGroup.class)
public class FileBlobStoreMetricsMigrationTaskTest
    extends TestSupport
{
  private static final String DEFAULT_BS_NAME = "default-blobstore";

  private static final String TEST_BS_NAME = "test-blobstore";

  private static final String BLOBS_COUNT = "blobs_count";

  private static final String BLOBS_TOTAL_SIZE = "blobs_total_size";

  private static final String BLOBS_AVAILABLE_SPACE = "blobs_available_space";

  private static final String BLOB_SIZE = "blob_size";

  private static final String ERROR_REQUESTS_COUNT = "error_requests_count";

  private static final String SUCCESSFUL_REQUESTS_COUNT = "successful_requests_count";

  private static final String TIME_ON_REQUESTS = "time_on_requests";

  private Map<String, Map<String, Long>> fileMetrics = new HashedMap();

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME).access(BlobStoreMetricsDAO.class);

  private FileBlobStoreMetricsMigrationTask underTest;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStore defaultBlobStore;

  @Mock
  private BlobStoreMetricsReader metricsReader;

  @Mock
  private BlobStore testBlobStore;

  @Mock
  private EventManager eventManager;

  private BlobStoreMetricsStore blobStoreMetricsStore;

  @Before
  public void setup() throws Exception {
    blobStoreMetricsStore = Guice.createInjector(new TransactionModule()
    {
      @Provides
      DataSessionSupplier getDataSessionSupplier() {
        return sessionRule;
      }

      @Provides
      EventManager getEventManager() {
        return eventManager;
      }
    }).getInstance(BlobStoreMetricsStoreImpl.class);

    UnitOfWork.beginBatch(() -> sessionRule.openSession(DataStoreManager.DEFAULT_DATASTORE_NAME));
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  public void testMetricsMigrated() throws Exception {
    setupExpectedTestMetrics();
    setupBlobStores();
    setupBlobStoresMetrics();
    setupBlobStoresMetricsOperations();

    underTest = new FileBlobStoreMetricsMigrationTask(blobStoreManager, blobStoreMetricsStore, metricsReader);
    underTest.execute();

    assertMigratedMetrics(DEFAULT_BS_NAME, blobStoreMetricsStore.get(DEFAULT_BS_NAME));
    assertMigratedMetrics(TEST_BS_NAME, blobStoreMetricsStore.get(TEST_BS_NAME));
  }

  private void assertMigratedMetrics(final String blobStoreName, final BlobStoreMetricsEntity metrics) {
    Map<String, Long> expectedMetrics = fileMetrics.get(blobStoreName);

    assertEquals(metrics.getBlobStoreName(), blobStoreName);
    assertEquals(metrics.getBlobCount(), expectedMetrics.get(BLOBS_COUNT).longValue());
    assertEquals(metrics.getTotalSize(), expectedMetrics.get(BLOBS_TOTAL_SIZE).longValue());

    assertEquals(metrics.getDownloadBlobSize(), expectedMetrics.get(BLOB_SIZE).longValue());
    assertEquals(metrics.getDownloadErrorRequests(), expectedMetrics.get(ERROR_REQUESTS_COUNT).longValue());
    assertEquals(metrics.getDownloadSuccessfulRequests(), expectedMetrics.get(SUCCESSFUL_REQUESTS_COUNT).longValue());
    assertEquals(metrics.getDownloadTimeOnRequests(), expectedMetrics.get(TIME_ON_REQUESTS).longValue());

    assertEquals(metrics.getUploadBlobSize(), expectedMetrics.get(BLOB_SIZE).longValue());
    assertEquals(metrics.getUploadErrorRequests(), expectedMetrics.get(ERROR_REQUESTS_COUNT).longValue());
    assertEquals(metrics.getUploadSuccessfulRequests(), expectedMetrics.get(SUCCESSFUL_REQUESTS_COUNT).longValue());
    assertEquals(metrics.getUploadTimeOnRequests(), expectedMetrics.get(TIME_ON_REQUESTS).longValue());
  }

  private void setupExpectedTestMetrics() {
    fileMetrics.put(DEFAULT_BS_NAME, new HashedMap()
    {{
      put(BLOBS_COUNT, 3L);
      put(BLOBS_TOTAL_SIZE, 12345678L);
      put(BLOBS_AVAILABLE_SPACE, 87654321L);
      put(BLOB_SIZE, 2048L);
      put(ERROR_REQUESTS_COUNT, 2L);
      put(SUCCESSFUL_REQUESTS_COUNT, 5L);
      put(TIME_ON_REQUESTS, 1L);
    }});
    fileMetrics.put(TEST_BS_NAME, new HashedMap()
    {{
      put(BLOBS_COUNT, 6L);
      put(BLOBS_TOTAL_SIZE, 555444466L);
      put(BLOBS_AVAILABLE_SPACE, 87654321L);
      put(BLOB_SIZE, 5566L);
      put(ERROR_REQUESTS_COUNT, 3L);
      put(SUCCESSFUL_REQUESTS_COUNT, 3L);
      put(TIME_ON_REQUESTS, 3L);
    }});
  }

  private void setupBlobStores() {
    BlobStoreConfiguration defaultBlobStoreConfig = mock(BlobStoreConfiguration.class);
    when(defaultBlobStoreConfig.getName()).thenReturn(DEFAULT_BS_NAME);
    when(defaultBlobStore.getBlobStoreConfiguration()).thenReturn(defaultBlobStoreConfig);

    BlobStoreConfiguration testBlobStoreConfig = mock(BlobStoreConfiguration.class);
    when(testBlobStoreConfig.getName()).thenReturn(TEST_BS_NAME);
    when(testBlobStore.getBlobStoreConfiguration()).thenReturn(testBlobStoreConfig);

    when(blobStoreManager.browse()).thenReturn(ImmutableList.of(defaultBlobStore, testBlobStore));
  }

  private void setupBlobStoresMetrics() throws Exception {
    BlobStoreMetrics metricsForDefaultBlobStore = mock(BlobStoreMetrics.class);
    Map<String, Long> expectedDefaultBlobStoreMetrics = fileMetrics.get(DEFAULT_BS_NAME);
    when(metricsForDefaultBlobStore.getBlobCount()).thenReturn(expectedDefaultBlobStoreMetrics.get(BLOBS_COUNT));
    when(metricsForDefaultBlobStore.getTotalSize()).thenReturn(expectedDefaultBlobStoreMetrics.get(BLOBS_TOTAL_SIZE));
    when(metricsForDefaultBlobStore.getAvailableSpace())
        .thenReturn(expectedDefaultBlobStoreMetrics.get(BLOBS_AVAILABLE_SPACE));

    when(metricsReader.readMetrics(defaultBlobStore)).thenReturn(metricsForDefaultBlobStore);

    BlobStoreMetrics metricsForTestBlobStore = mock(BlobStoreMetrics.class);
    Map<String, Long> expectedTestBlobStoreMetrics = fileMetrics.get(TEST_BS_NAME);
    when(metricsForTestBlobStore.getBlobCount()).thenReturn(expectedTestBlobStoreMetrics.get(BLOBS_COUNT));
    when(metricsForTestBlobStore.getTotalSize()).thenReturn(expectedTestBlobStoreMetrics.get(BLOBS_TOTAL_SIZE));
    when(metricsForTestBlobStore.getAvailableSpace()).thenReturn(
        expectedTestBlobStoreMetrics.get(BLOBS_AVAILABLE_SPACE));

    when(metricsReader.readMetrics(testBlobStore)).thenReturn(metricsForTestBlobStore);
  }

  private void setupBlobStoresMetricsOperations() throws Exception {
    OperationMetrics downloadMetricsForDefaultBlobStore = mock(OperationMetrics.class);
    Map<String, Long> expectedDefaultBlobStoreMetrics = fileMetrics.get(DEFAULT_BS_NAME);
    when(downloadMetricsForDefaultBlobStore.getBlobSize()).thenReturn(expectedDefaultBlobStoreMetrics.get(BLOB_SIZE));
    when(downloadMetricsForDefaultBlobStore.getErrorRequests())
        .thenReturn(expectedDefaultBlobStoreMetrics.get(ERROR_REQUESTS_COUNT));
    when(downloadMetricsForDefaultBlobStore.getSuccessfulRequests())
        .thenReturn(expectedDefaultBlobStoreMetrics.get(SUCCESSFUL_REQUESTS_COUNT));
    when(downloadMetricsForDefaultBlobStore.getTimeOnRequests())
        .thenReturn(expectedDefaultBlobStoreMetrics.get(TIME_ON_REQUESTS));

    OperationMetrics uploadMetricsForDefaultBlobStore = mock(OperationMetrics.class);
    when(uploadMetricsForDefaultBlobStore.getBlobSize()).thenReturn(expectedDefaultBlobStoreMetrics.get(BLOB_SIZE));
    when(uploadMetricsForDefaultBlobStore.getErrorRequests())
        .thenReturn(expectedDefaultBlobStoreMetrics.get(ERROR_REQUESTS_COUNT));
    when(uploadMetricsForDefaultBlobStore.getSuccessfulRequests())
        .thenReturn(expectedDefaultBlobStoreMetrics.get(SUCCESSFUL_REQUESTS_COUNT));
    when(uploadMetricsForDefaultBlobStore.getTimeOnRequests())
        .thenReturn(expectedDefaultBlobStoreMetrics.get(TIME_ON_REQUESTS));

    when(metricsReader.readOperationMetrics(defaultBlobStore))
        .thenReturn(ImmutableMap.of(
            OperationType.DOWNLOAD, downloadMetricsForDefaultBlobStore,
            OperationType.UPLOAD, uploadMetricsForDefaultBlobStore));

    OperationMetrics downloadMetricsForTestBlobStore = mock(OperationMetrics.class);
    Map<String, Long> expectedTestBlobStoreMetrics = fileMetrics.get(TEST_BS_NAME);
    when(downloadMetricsForTestBlobStore.getBlobSize()).thenReturn(expectedTestBlobStoreMetrics.get(BLOB_SIZE));
    when(downloadMetricsForTestBlobStore.getErrorRequests())
        .thenReturn(expectedTestBlobStoreMetrics.get(ERROR_REQUESTS_COUNT));
    when(downloadMetricsForTestBlobStore.getSuccessfulRequests())
        .thenReturn(expectedTestBlobStoreMetrics.get(SUCCESSFUL_REQUESTS_COUNT));
    when(downloadMetricsForTestBlobStore.getTimeOnRequests())
        .thenReturn(expectedTestBlobStoreMetrics.get(TIME_ON_REQUESTS));

    OperationMetrics uploadMetricsForTestBlobStore = mock(OperationMetrics.class);
    when(uploadMetricsForTestBlobStore.getBlobSize()).thenReturn(expectedTestBlobStoreMetrics.get(BLOB_SIZE));
    when(uploadMetricsForTestBlobStore.getErrorRequests())
        .thenReturn(expectedTestBlobStoreMetrics.get(ERROR_REQUESTS_COUNT));
    when(uploadMetricsForTestBlobStore.getSuccessfulRequests())
        .thenReturn(expectedTestBlobStoreMetrics.get(SUCCESSFUL_REQUESTS_COUNT));
    when(uploadMetricsForTestBlobStore.getTimeOnRequests())
        .thenReturn(expectedTestBlobStoreMetrics.get(TIME_ON_REQUESTS));

    when(metricsReader.readOperationMetrics(testBlobStore))
        .thenReturn(ImmutableMap.of(
            OperationType.DOWNLOAD, downloadMetricsForTestBlobStore,
            OperationType.UPLOAD, uploadMetricsForTestBlobStore));
  }
}
