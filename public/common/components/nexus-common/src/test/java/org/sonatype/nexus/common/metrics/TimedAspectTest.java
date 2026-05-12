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
package org.sonatype.nexus.common.metrics;

import java.lang.reflect.Method;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.codahale.metrics.annotation.Timed;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.metrics.MetricsConstants.NEXUS_METRICS_REGISTRY_NAME;

@ExtendWith(MockitoExtension.class)
public class TimedAspectTest
{
  MockedStatic<SharedMetricRegistries> sharedMetricRegistries = mockStatic(SharedMetricRegistries.class);

  @Mock
  private MetricRegistry metricRegistry;

  @Mock
  private ProceedingJoinPoint joinPoint;

  @Mock
  private MethodSignature methodSignature;

  @Mock
  private Timer timer;

  @Mock
  private Context context;

  private TimedAspect timedAspect;

  @BeforeEach
  public void setUp() {
    sharedMetricRegistries.when(() -> SharedMetricRegistries.getOrCreate(NEXUS_METRICS_REGISTRY_NAME))
        .thenReturn(metricRegistry);

    timedAspect = new TimedAspect();
  }

  @Test
  void testAroundItShouldTimeWithAnnoName() throws Throwable {
    Method method = TestClass.class.getMethod("testMethodWithAbsoluteName");
    Timed timed = method.getAnnotation(Timed.class);
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(methodSignature.getMethod()).thenReturn(method);
    when(metricRegistry.timer(anyString())).thenReturn(timer);
    when(timer.time()).thenReturn(context);
    when(joinPoint.proceed()).thenReturn("result");

    Object result = timedAspect.around(joinPoint, timed);
    verify(metricRegistry)
        .timer("org.sonatype.nexus.common.metrics.TimedAspectTest$TestClass.testMethodWithAbsoluteName.timer");
    assertEquals("result", result);
    verify(context).stop();
  }

  @Test
  void testAroundWhenGetContextThrowsException() throws Throwable {
    Method method = TestClass.class.getMethod("testMethodWithAbsoluteName");
    Timed timed = method.getAnnotation(Timed.class);
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(methodSignature.getMethod()).thenReturn(method);
    when(metricRegistry.timer(anyString())).thenThrow(new RuntimeException("Timer creation failed"));
    when(joinPoint.proceed()).thenReturn("result");

    Object result = timedAspect.around(joinPoint, timed);
    verify(context, never()).stop();
    assertEquals("result", result);
  }

  @AfterEach
  public void tearDown() {
    sharedMetricRegistries.close();
  }

  static class TestClass
  {
    @Timed
    public void testMethodWithAbsoluteName() {
      // no-op
    }
  }
}
