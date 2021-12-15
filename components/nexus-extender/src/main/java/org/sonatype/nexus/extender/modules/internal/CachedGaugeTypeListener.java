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
package org.sonatype.nexus.extender.modules.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.CachedGauge;
import com.codahale.metrics.annotation.Gauge;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.palominolabs.metrics.guice.GaugeInjectionListener;
import com.palominolabs.metrics.guice.MetricNamer;
import com.palominolabs.metrics.guice.annotation.AnnotationResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Boolean.parseBoolean;

/**
 * Provides annotation support for {@code CachedGauges} which were missing from the metrics guice package
 *
 * @since 3.26
 */
public class CachedGaugeTypeListener
    implements TypeListener
{
  private static final String CACHE_DISABLE_ALL = "nexus.analytics.cache.disableAll";

  private static final String CACHE_DISABLE_PREFIX = "nexus.analytics.cache.disable.";

  private static final String CACHE_TIMEOUT_SUFFIX = ".cache.timeout";

  private static final String CACHE_TIMEUNIT_SUFFIX = ".cache.timeUnit";

  private static final String GAUGE_DISABLE_SUFFIX = ".disable";

  private final Logger log = LoggerFactory.getLogger(getClass().getName());

  private final MetricRegistry metricRegistry;

  private final MetricNamer metricNamer;

  private final AnnotationResolver annotationResolver;

  private final Map<?, ?> nexusProperties;

  public CachedGaugeTypeListener(
      final MetricRegistry metricRegistry,
      final MetricNamer metricNamer,
      final AnnotationResolver annotationResolver,
      final Map<?, ?> nexusProperties)
  {
    this.metricRegistry = checkNotNull(metricRegistry);
    this.metricNamer = checkNotNull(metricNamer);
    this.annotationResolver = checkNotNull(annotationResolver);
    this.nexusProperties = nexusProperties;
  }

  /**
   * Creates a Gauge, since guice-metrics does not support CachedGauges and they are considered separate types.
   *
   * @param annotation - the annotation to be transformed
   * @return A gauge to be used for interfacing with the helper classes from guice-metrics
   */
  private Gauge fromCachedGauge(final CachedGauge annotation) {
    return new Gauge()
    {
      @Override
      public Class<? extends Annotation> annotationType() {
        return Gauge.class;
      }

      @Override
      public String name() {
        return annotation.name();
      }

      @Override
      public boolean absolute() {
        return annotation.absolute();
      }
    };
  }

  @Override
  public <T> void hear(final TypeLiteral<T> typeLiteral, final TypeEncounter<T> typeEncounter) {
    for (Class<? super T> clazz = typeLiteral.getRawType(); clazz != null; clazz = clazz.getSuperclass()) {
      for (Method method : clazz.getDeclaredMethods()) {
        processAnnotationMethod(method, typeEncounter);
      }
    }
  }

  private <T> void processAnnotationMethod(Method method, final TypeEncounter<T> typeEncounter) {
    if (method.isSynthetic()) {
      return;
    }

    final CachedGauge annotation = annotationResolver.findAnnotation(CachedGauge.class, method);
    if (annotation == null) {
      return;
    }

    if (method.getParameterCount() != 0) {
      typeEncounter.addError("Method %s is annotated with @CachedGauge but requires parameters.", method);
      return;
    }

    final String metricName = metricNamer.getNameForGauge(method, fromCachedGauge(annotation));

    if (parseBoolean((String) nexusProperties.get(metricName + GAUGE_DISABLE_SUFFIX))) {
      log.info("Removed Analytics for {} as directed in nexus.properties", metricName);
      return;
    }

    // deprecated method in java 9, but replacement is not available in java 8
    if (!method.isAccessible()) {
      method.setAccessible(true);
    }

    typeEncounter.register(buildInjectionListener(metricName, method, annotation));
  }

  private <T> InjectionListener<T> buildInjectionListener(String metricName, Method method, CachedGauge annotation) {
    if (parseBoolean((String) nexusProperties.get(CACHE_DISABLE_ALL)) ||
        parseBoolean((String) nexusProperties.get(CACHE_DISABLE_PREFIX + metricName))) {
      log.info("Disabled Analytics Cache for {} as directed in nexus.properties", metricName);
      return new GaugeInjectionListener<>(metricRegistry, metricName, method);
    }
    else {
      long timeout = annotation.timeout();
      Optional<Long> timeoutOverride = getTimeoutOverride(metricName);
      if (timeoutOverride.isPresent()) {
        timeout = timeoutOverride.get();
      }

      TimeUnit timeUnit = annotation.timeoutUnit();
      Optional<TimeUnit> timeUnitOverride = getTimeUnitOverride(metricName);
      if (timeUnitOverride.isPresent()) {
        timeUnit = timeUnitOverride.get();
      }

      if (timeout != annotation.timeout() || !timeUnit.equals(annotation.timeoutUnit())) {
        log.info("Updated Analytics Cache for {} to {} {} as directed in nexus.properties", metricName, timeout,
            timeUnit);
      }
      return new CachedGaugeInjectionListener<>(metricRegistry, metricName, method, timeout, timeUnit);
    }
  }

  private Optional<Long> getTimeoutOverride(String metricName) {
    if (nexusProperties.containsKey(metricName + CACHE_TIMEOUT_SUFFIX)) {
      Object value = nexusProperties.get(metricName + CACHE_TIMEOUT_SUFFIX);
      try {
        return Optional.of(Long.parseLong(value.toString()));
      }
      catch (Exception ex) {
        log.warn("Failed to parse Analytics Cache configuration in nexus.properties: {} = {}",
            metricName + CACHE_TIMEOUT_SUFFIX, value);
        if (log.isDebugEnabled()) {
          log.debug("Stack Trace:", ex);
        }
      }
    }
    return Optional.empty();
  }

  private Optional<TimeUnit> getTimeUnitOverride(String metricName) {
    if (nexusProperties.containsKey(metricName + CACHE_TIMEUNIT_SUFFIX)) {
      Object value = nexusProperties.get(metricName + CACHE_TIMEUNIT_SUFFIX);
      try {
        return Optional.of(TimeUnit.valueOf(value.toString().toUpperCase()));
      }
      catch (Exception ex) {
        log.warn("Failed to parse Analytics Cache configuration in nexus.properties: {} = {}",
            metricName + CACHE_TIMEUNIT_SUFFIX, value);
        if (log.isDebugEnabled()) {
          log.debug("Stack Trace:", ex);
        }
      }
    }
    return Optional.empty();
  }
}