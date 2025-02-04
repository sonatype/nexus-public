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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaResult;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;

import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class BlobStoreQuotaHealthCheckTest
    extends TestSupport
{
  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStore blobStore;

  @Mock
  private BlobStoreMetrics blobStoreMetrics;

  @Mock
  private BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  private BlobStoreQuotaService quotaService;

  @Mock
  private BlobStoreQuotaResult quotaResult;

  private BlobStoreQuotaHealthCheck blobStoreHealthCheck =
      new BlobStoreQuotaHealthCheck(() -> blobStoreManager, () -> quotaService);

  @Before
  public void setup() {
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStoreConfiguration.getName()).thenReturn("test");
  }

  @Test
  public void testCheck_noneConfigured() {
    when(blobStoreManager.browse()).thenReturn(List.of());
    Result result = blobStoreHealthCheck.check();

    assertTrue(result.isHealthy());
  }

  @Test
  public void testCheck_noQuotaViolation() {
    when(blobStoreManager.browse()).thenReturn(List.of(blobStore));
    when(quotaService.checkQuota(blobStore)).thenReturn(quotaResult);
    when(quotaResult.isViolation()).thenReturn(false);

    Result result = blobStoreHealthCheck.check();

    assertTrue(result.isHealthy());

  }

  @Test
  public void testCheck_violatingQuota() {
    when(blobStoreManager.browse()).thenReturn(List.of(blobStore));
    when(quotaService.checkQuota(blobStore)).thenReturn(quotaResult);
    when(quotaResult.isViolation()).thenReturn(true);
    Result result = blobStoreHealthCheck.check();

    assertFalse(result.isHealthy());
  }

  @Test
  public void testCheck_noQuota() {
    when(blobStoreManager.browse()).thenReturn(List.of(blobStore));
    Result result = blobStoreHealthCheck.check();

    assertTrue(result.isHealthy());
  }
}
