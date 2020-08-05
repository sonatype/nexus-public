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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.CachedGauge;
import com.codahale.metrics.annotation.Gauge;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.palominolabs.metrics.guice.MetricNamer;
import com.palominolabs.metrics.guice.annotation.AnnotationResolver;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides annotation support for {@code CachedGauges} which were missing from the metrics guice package
 * @since 3.26
 */
public class CachedGaugeTypeListener
    implements TypeListener
{
  private final MetricRegistry metricRegistry;

  private final MetricNamer metricNamer;

  private final AnnotationResolver annotationResolver;

  public CachedGaugeTypeListener(
      final MetricRegistry metricRegistry,
      final MetricNamer metricNamer,
      final AnnotationResolver annotationResolver)
  {
    this.metricRegistry = checkNotNull(metricRegistry);
    this.metricNamer = checkNotNull(metricNamer);
    this.annotationResolver = checkNotNull(annotationResolver);
  }

  /**
   * Creates a Gauge, since guice-metrics does not support CachedGauges and they are considered separate types.
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
        if (method.isSynthetic()) {
          continue;
        }

        final CachedGauge annotation = annotationResolver.findAnnotation(CachedGauge.class, method);
        if (annotation == null) {
          continue;
        }

        if (method.getParameterCount() != 0) {
          typeEncounter.addError("Method %s is annotated with @CachedGauge but requires parameters.", method);
          continue;
        }

        final String metricName = metricNamer.getNameForGauge(method, fromCachedGauge(annotation));

        // deprecated method in java 9, but replacement is not available in java 8
        if (!method.isAccessible()) {
          method.setAccessible(true);
        }

        typeEncounter.register(
            new CachedGaugeInjectionListener<>(metricRegistry, metricName, method, annotation.timeout(),
                annotation.timeoutUnit()));
      }
    }
  }
}