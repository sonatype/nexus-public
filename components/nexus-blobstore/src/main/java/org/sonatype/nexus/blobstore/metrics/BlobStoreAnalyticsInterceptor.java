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

import java.lang.reflect.Method;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.BlobSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import static com.google.common.base.Preconditions.checkState;

/**
 * A method interceptor which monitor blob store operations (see {@link OperationType}) of the annotated method.
 *
 * @since 3.38
 */
public class BlobStoreAnalyticsInterceptor
    extends ComponentSupport
    implements MethodInterceptor
{
  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {
    String clazz = invocation.getThis().getClass().getSimpleName();
    Method method = invocation.getMethod();
    String methodName = method.getName();

    MonitoringBlobStoreMetrics metricsAnnotation = method.getAnnotation(MonitoringBlobStoreMetrics.class);
    checkState(metricsAnnotation != null);
    OperationType operationType = metricsAnnotation.operationType();

    BlobStore blobStore;
    OperationMetrics operationMetrics;
    if (invocation.getThis() instanceof BlobStore) {
      blobStore = (BlobStore) invocation.getThis();
      operationMetrics = blobStore.getOperationMetricsDelta().get(operationType);
    }
    else {
      log.info("Can't monitor operation metrics for class={}, methodName={}", clazz, methodName);
      return invocation.proceed();
    }

    long start = System.currentTimeMillis();
    try {
      Object result = invocation.proceed();

      // record metrics only in case of successful processing.
      operationMetrics.addSuccessfulRequest();
      operationMetrics.addTimeOnRequests(System.currentTimeMillis() - start);

      if (result instanceof BlobSupport) {
        long totalSize = ((BlobSupport) result).getMetrics().getContentSize();
        operationMetrics.addBlobSize(totalSize);
      }

      return result;
    }
    catch (Exception e) {
      operationMetrics.addErrorRequest();
      throw e;
    }
  }
}
