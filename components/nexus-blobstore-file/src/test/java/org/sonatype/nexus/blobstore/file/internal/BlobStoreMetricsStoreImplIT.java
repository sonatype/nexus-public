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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.common.property.PropertiesFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * {@link FileBlobStore} integration tests.
 */
public class BlobStoreMetricsStoreImplIT
    extends TestSupport
{
  private BlobStoreMetricsStoreImpl underTest;

  private Path blobStoreDirectory;

  @Before
  public void setUp() throws Exception {
    blobStoreDirectory = util.createTempDir().toPath();
    underTest = new BlobStoreMetricsStoreImpl(new PeriodicJobServiceImpl());
    underTest.setStorageDir(blobStoreDirectory);
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
    assertThat(underTest.getMetrics().getBlobCount(), is(1L));
    underTest.recordDeletion(1000);
    assertThat(underTest.getMetrics().getBlobCount(), is(0L));
  }

  @Test
  public void metricsLoadsExistingPropertyFile() throws Exception {
    PropertiesFile props = new PropertiesFile(
        blobStoreDirectory.resolve(BlobStoreMetricsStoreImpl.METRICS_FILENAME).toFile());

    props.put(BlobStoreMetricsStoreImpl.BLOB_COUNT_PROP_NAME, "32");
    props.put(BlobStoreMetricsStoreImpl.TOTAL_SIZE_PROP_NAME, "200");

    props.store();

    underTest.start();

    BlobStoreMetrics metrics = underTest.getMetrics();
    assertThat(metrics.getBlobCount(), is(32L));
    assertThat(metrics.getTotalSize(), is(200L));
  }
}
