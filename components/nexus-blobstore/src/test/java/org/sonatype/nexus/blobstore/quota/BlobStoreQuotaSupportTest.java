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
package org.sonatype.nexus.blobstore.quota;

import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class BlobStoreQuotaSupportTest
    extends TestSupport
{

  @Mock
  private BlobStore blobStore;

  @Mock
  private BlobStoreQuotaService quotaService;

  @Mock
  private Logger logger;

  @Test
  public void getLimitHandlesNumbersProperly() {
    BlobStoreConfiguration config = new MockBlobStoreConfiguration();
    config.attributes(BlobStoreQuotaSupport.ROOT_KEY).set(BlobStoreQuotaSupport.LIMIT_KEY, 1);
    assertThat(BlobStoreQuotaSupport.getLimit(config), is(1L));

    config.attributes(BlobStoreQuotaSupport.ROOT_KEY).set(BlobStoreQuotaSupport.LIMIT_KEY, 0);
    assertThat(BlobStoreQuotaSupport.getLimit(config), is(0L));

    config.attributes(BlobStoreQuotaSupport.ROOT_KEY).set(BlobStoreQuotaSupport.LIMIT_KEY, -1);
    assertThat(BlobStoreQuotaSupport.getLimit(config), is(-1L));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getLimitHandlesErrorCases() {
    BlobStoreConfiguration config = new MockBlobStoreConfiguration();
    config.attributes(BlobStoreQuotaSupport.ROOT_KEY).set(BlobStoreQuotaSupport.LIMIT_KEY, null);
    BlobStoreQuotaSupport.getLimit(config);
  }

  @Test
  public void passedQuotaLogsNothing() {
    BlobStoreQuotaResult result = new BlobStoreQuotaResult(false, "name", "msg");
    when(quotaService.checkQuota(blobStore)).thenReturn(result);

    BlobStoreQuotaSupport.quotaCheckJob(blobStore, quotaService, logger);

    verify(logger, never()).error(anyString());
    verify(logger, never()).warn("msg");
  }

  @Test
  public void failedQuotaLogsResult() {
    BlobStoreQuotaResult result = new BlobStoreQuotaResult(true, "name", "msg");
    when(quotaService.checkQuota(blobStore)).thenReturn(result);

    BlobStoreQuotaSupport.quotaCheckJob(blobStore, quotaService, logger);

    verify(logger, never()).error(anyString());
    verify(logger).warn("msg");
  }

  @Test
  public void quotaCheckJobExceptionsAreCaught() {
    when(blobStore.getBlobStoreConfiguration()).thenReturn(mock(BlobStoreConfiguration.class));
    when(blobStore.getBlobStoreConfiguration().getName()).thenReturn("testConfig");
    doThrow(new RuntimeException()).when(quotaService).checkQuota(blobStore);

    BlobStoreQuotaSupport.quotaCheckJob(blobStore, quotaService, logger);

    verify(logger).error(anyString(), anyString(), any(RuntimeException.class));
    verify(logger, never()).warn(anyString());
  }
}
