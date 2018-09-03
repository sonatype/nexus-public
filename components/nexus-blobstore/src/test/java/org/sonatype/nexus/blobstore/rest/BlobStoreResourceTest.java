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
package org.sonatype.nexus.blobstore.rest;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaResult;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class BlobStoreResourceTest
    extends TestSupport
{
  @Mock
  BlobStoreManager manager;

  @Mock
  BlobStoreQuotaService quotaService;

  @Mock
  BlobStore noQuota;

  @Mock
  BlobStore passing;

  @Mock
  BlobStore failing;

  BlobStoreResource resource;

  @Before
  public void setup() {
    when(quotaService.checkQuota(noQuota)).thenReturn(null);
    when(quotaService.checkQuota(passing)).thenReturn(new BlobStoreQuotaResult(false, "passing", "test"));
    when(quotaService.checkQuota(failing)).thenReturn(new BlobStoreQuotaResult(true, "failing", "test"));

    when(manager.get(eq("passing"))).thenReturn(passing);
    when(manager.get(eq("noQuota"))).thenReturn(noQuota);
    when(manager.get(eq("failing"))).thenReturn(failing);

    resource = new BlobStoreResource(manager, quotaService);
  }

  @Test
  public void passingTest() {
    BlobStoreQuotaResultXO resultXO = resource.quotaStatus("passing");
    assertFalse(resultXO.getIsViolation());
    assertEquals(resultXO.getBlobStoreName(), "passing");
  }

  @Test
  public void failingTest() {
    BlobStoreQuotaResultXO resultXO = resource.quotaStatus("failing");
    assertTrue(resultXO.getIsViolation());
    assertEquals(resultXO.getBlobStoreName(), "failing");
  }

  @Test
  public void noQuotaTest() {
    BlobStoreQuotaResultXO resultXO = resource.quotaStatus("noQuota");
    assertFalse(resultXO.getIsViolation());
    assertEquals(resultXO.getBlobStoreName(), "noQuota");
  }
}
