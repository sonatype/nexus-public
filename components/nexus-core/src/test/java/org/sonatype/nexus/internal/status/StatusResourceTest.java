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
package org.sonatype.nexus.internal.status;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.common.app.NotReadableException;
import org.sonatype.nexus.common.app.NotWritableException;

import javax.ws.rs.core.Response;
import java.util.SortedMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class StatusResourceTest
{

  @Mock
  private FreezeService freezeService;

  @Mock
  private HealthCheckRegistry registry;

  private StatusResource statusResource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    statusResource = new StatusResource(freezeService, registry);
  }

  @Test
  public void isAvailableIfServerCanExecuteReadCheck() {
    Response response = statusResource.isAvailable();
    verify(freezeService, times(1)).checkReadable(any());
    assertEquals(200, response.getStatus());
  }

  @Test
  public void isNotAvailableIfServerCannotExecuteReadCheck() {
    doThrow(new NotReadableException("mock test error")).when(freezeService).checkReadable(any());
    Response response = statusResource.isAvailable();
    assertEquals(503, response.getStatus());
  }

  @Test
  public void isWritableIfServerCanExecuteWriteCheck() {
    Response response = statusResource.isWritable();
    verify(freezeService, times(1)).checkWritable(any());
    assertEquals(200, response.getStatus());
  }

  @Test
  public void isNotWritableIfServerCannotExecuteWriteCheck() {
    doThrow(new NotWritableException("mock test error")).when(freezeService).checkWritable(any());
    Response response = statusResource.isWritable();
    assertEquals(503, response.getStatus());
  }

  @Test
  public void isNotWritableAndNoWriteCheckIsMadeIfDatabaseIsFrozen() {
    when(freezeService.isFrozen()).thenReturn(true);
    Response response = statusResource.isWritable();
    verify(freezeService, never()).checkWritable(any());
    assertEquals(503, response.getStatus());
  }

  @Test
  public void returnsTheSystemStatusChecks() {
    SortedMap<String, Result> expectedStatusChecks = mock(SortedMap.class);
    when(registry.runHealthChecks()).thenReturn(expectedStatusChecks);
    SortedMap<String, Result> systemStatusChecks = statusResource.getSystemStatusChecks();
    assertEquals(expectedStatusChecks, systemStatusChecks);
  }
}
