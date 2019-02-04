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
package org.sonatype.nexus.blobstore.file.internal;

import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.scheduling.internal.PeriodicJobServiceImpl;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.property.PropertiesFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FileBlobStore} integration tests.
 */
public class FileBlobStoreMetricsStoreIT
    extends TestSupport
{
  private FileBlobStoreMetricsStore underTest;

  private Path blobStoreDirectory;

  private static final int METRICS_FLUSH_TIMEOUT = 5;

  private static final int QUOTA_CHECK_INTERVAL = 5;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  BlobStoreQuotaService quotaService;

  @Mock
  BlobStore blobStore;

  @Mock
  FileOperations fileOperations;

  @Before
  public void setUp() throws Exception {
    blobStoreDirectory = util.createTempDir().toPath();
    when(nodeAccess.getId()).thenReturn(UUID.randomUUID().toString());
    underTest = new FileBlobStoreMetricsStore(new PeriodicJobServiceImpl(), nodeAccess, quotaService,
        QUOTA_CHECK_INTERVAL, fileOperations);
    underTest.setStorageDir(blobStoreDirectory);
    underTest.setBlobStore(blobStore);
  }

  @After
  public void tearDown() throws Exception {
    if (underTest != null) {
      underTest.stop();
    }
  }

  @Test
  public void metricsCanCount() throws Exception {
    underTest.start();

    underTest.recordAddition(1000);
    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS).until(() -> underTest.getMetrics().getBlobCount(), is(1L));
    underTest.recordDeletion(1000);
    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS).until(() -> underTest.getMetrics().getBlobCount(), is(0L));
  }

  @Test
  public void metricsLoadsExistingPropertyFile() throws Exception {
    PropertiesFile props = new PropertiesFile(
        blobStoreDirectory.resolve(nodeAccess.getId() + "-" + FileBlobStoreMetricsStore.METRICS_FILENAME).toFile());

    props.put(FileBlobStoreMetricsStore.BLOB_COUNT_PROP_NAME, "32");
    props.put(FileBlobStoreMetricsStore.TOTAL_SIZE_PROP_NAME, "200");

    props.store();

    underTest.start();

    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS).until(() -> underTest.getMetrics().getBlobCount(), is(32L));
    await().atMost(METRICS_FLUSH_TIMEOUT, SECONDS).until(() -> underTest.getMetrics().getTotalSize(), is(200L));
  }

  @Test
  public void listBackingFiles() throws Exception {
    underTest = new FileBlobStoreMetricsStore(new PeriodicJobServiceImpl(), nodeAccess, quotaService, 5,
        fileOperations);
    Stream backingFiles = underTest.backingFiles();
    assertThat("backing files is empty", backingFiles.count(), is(0L));

    underTest.setStorageDir(blobStoreDirectory);
    underTest.start();

    backingFiles = underTest.backingFiles();
    assertThat("backing files contains the data file", backingFiles.count(), is(1L));
  }

  @Test
  public void invokeQuotaServiceOnFlush() throws Exception {
    underTest.start();

    underTest.recordAddition(1000);
    Thread.sleep(QUOTA_CHECK_INTERVAL * 1000);
    await().atMost(QUOTA_CHECK_INTERVAL, SECONDS).until(() -> underTest.getMetrics().getBlobCount(), is(1L));
    verify(quotaService, times(1)).checkQuota(blobStore);
  }
}
