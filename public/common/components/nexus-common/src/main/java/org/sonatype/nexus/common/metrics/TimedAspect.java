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
import java.util.Optional;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.codahale.metrics.annotation.Timed;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;
import static org.sonatype.nexus.common.app.FeatureFlags.METRICS_INTERNAL_ENABLED;
import static org.sonatype.nexus.common.metrics.MetricsConstants.NEXUS_METRICS_REGISTRY_NAME;

/**
 * Aspect to handle {@link Timed} annotation and register timers in the metrics registry.
 */
@Aspect
public class TimedAspect
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(NEXUS_METRICS_REGISTRY_NAME);

  private static final boolean TIMED_ENABLED =
      Boolean.parseBoolean(System.getProperty(METRICS_INTERNAL_ENABLED, "true"));

  @Around("@annotation(timed) && execution(* *(..))")
  public Object around(final ProceedingJoinPoint joinPoint, final Timed timed) throws Throwable {
    if (!TIMED_ENABLED) {
      return joinPoint.proceed();
    }
    Optional<Context> context = getContext(joinPoint, timed);
    try {
      return joinPoint.proceed();
    }
    finally {
      context.ifPresent(Context::stop);
    }
  }

  private Optional<Context> getContext(final ProceedingJoinPoint joinPoint, final Timed timed) {
    try {
      Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
      String metricName = metricName(method, timed);
      Timer timer = metricRegistry.timer(metricName);
      Context context = timer.time();
      return Optional.of(context);
    }
    catch (Exception e) {
      log.warn("Failed to create timer for method: {}. Error: {}", joinPoint.getSignature().toShortString(),
          e.getMessage());
      return Optional.empty();
    }
  }

  public static String metricName(final Method method, final Timed timed) {
    if (timed.absolute()) {
      return timed.name();
    }

    if (timed.name().isEmpty()) {
      return name(method.getDeclaringClass(), method.getName(), "timer");
    }

    return name(method.getDeclaringClass(), timed.name());
  }
}
