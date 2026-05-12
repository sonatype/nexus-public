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
package org.sonatype.nexus.blobstore.metrics;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonatype.nexus.blobstore.BlobSupport;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BlobStoreAnalyticsAspectTest
{

  private BlobStoreAnalyticsAspect aspect;

  @Mock
  private ProceedingJoinPoint joinPoint;

  @Mock
  private MethodSignature methodSignature;

  @Mock
  private BlobStore blobStore;

  @Mock
  private OperationMetrics operationMetrics;

  @Mock
  private BlobMetrics blobMetrics;

  @Mock
  private BlobSupport blobSupport;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    aspect = new BlobStoreAnalyticsAspect();
  }

  @Test
  public void testMonitorBlobStoreOperation_SuccessfulRequest() throws Throwable {
    // Mock behavior
    when(joinPoint.getTarget()).thenReturn(blobStore);
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(methodSignature.getMethod()).thenReturn(TestBlobStore.class.getMethod("uploadBlob"));
    when(blobStore.getOperationMetricsDelta()).thenReturn(Map.of(OperationType.UPLOAD, operationMetrics));
    when(joinPoint.proceed()).thenReturn(blobSupport);
    when(blobSupport.getMetrics()).thenReturn(blobMetrics);
    when(blobSupport.getMetrics().getContentSize()).thenReturn(100L);

    // Execute the aspect
    Object result = aspect.monitorBlobStoreOperation(joinPoint,
        TestBlobStore.class.getMethod("uploadBlob").getAnnotation(MonitoringBlobStoreMetrics.class));

    // Verify interactions
    verify(operationMetrics).addSuccessfulRequest();
    verify(operationMetrics).addTimeOnRequests(anyLong());
    verify(operationMetrics).addBlobSize(100L);
    verify(joinPoint).proceed();
    assertEquals(blobSupport, result);
  }

  @Test
  public void testMonitorBlobStoreOperation_ErrorRequest() throws Throwable {
    // Mock behavior
    when(joinPoint.getTarget()).thenReturn(blobStore);
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(methodSignature.getMethod()).thenReturn(TestBlobStore.class.getMethod("uploadBlob"));
    when(blobStore.getOperationMetricsDelta()).thenReturn(Map.of(OperationType.UPLOAD, operationMetrics));
    when(joinPoint.proceed()).thenThrow(new RuntimeException("Test Exception"));

    // Execute the aspect and expect an exception
    try {
      aspect.monitorBlobStoreOperation(joinPoint,
          TestBlobStore.class.getMethod("uploadBlob").getAnnotation(MonitoringBlobStoreMetrics.class));
      fail("Expected exception was not thrown");
    }
    catch (RuntimeException e) {
      assertEquals("Test Exception", e.getMessage());
    }

    // Verify interactions
    verify(operationMetrics).addErrorRequest();
    verify(joinPoint).proceed();
  }

  // Internal test class with annotated method
  private static class TestBlobStore
  {

    @MonitoringBlobStoreMetrics(operationType = OperationType.UPLOAD)
    public void uploadBlob() {
    }
  }
}
