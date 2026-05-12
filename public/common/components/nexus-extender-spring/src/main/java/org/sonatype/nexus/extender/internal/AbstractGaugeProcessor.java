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
package org.sonatype.nexus.extender.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.annotation.CachedGauge;
import com.codahale.metrics.annotation.Gauge;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.common.metrics.MetricsConstants.NEXUS_METRICS_REGISTRY_NAME;

/**
 * base class to process metrics of methods annotated with {@link Gauge} or {@link CachedGauge} annotations.
 */
public abstract class AbstractGaugeProcessor<A extends Annotation>
    implements BeanPostProcessor
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(NEXUS_METRICS_REGISTRY_NAME);

  /**
   * Scans all methods of a bean (including superclasses) and processes them.
   */
  @Override
  public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
    try {
      Class<?> clazz = bean.getClass();
      for (Method method : clazz.getMethods()) {
        processBeanMethod(bean, method);
      }
    }
    catch (Exception e) {
      log.warn("Error registering {} metrics for bean named: {}", gaugeName(), beanName, e);
    }
    return bean;
  }

  /**
   * Template method for processing a bean method. Handles common logic and delegates to subclass hooks.
   */
  private void processBeanMethod(final Object bean, final Method method) {
    A annotation = getAnnotation(method);
    if (method.isSynthetic() || annotation == null) {
      return;
    }
    if (method.getParameterCount() != 0) {
      log.warn("Method {} is annotated with {} but requires parameters.", method, gaugeName());
      return;
    }
    ensureAccessible(method, bean);
    final String metricName = metricName(method, annotation);
    registerMetric(bean, method, annotation, metricName);
  }

  /**
   * returns the particular metric annotation of the method.
   */
  protected abstract A getAnnotation(Method method);

  /**
   * returns the metric name based on the desired metric annotation
   */
  protected abstract String metricName(Method method, A annotation);

  /**
   * register the metric in the registry with the given name using the appropriate gauge
   */
  protected abstract void registerMetric(Object bean, Method method, A annotation, String metricName);

  /**
   * returns the name of the gauge used
   */
  protected abstract String gaugeName();

  /**
   * Makes the method accessible if needed.
   */
  protected void ensureAccessible(Method method, Object bean) {
    if (!method.canAccess(bean)) {
      method.setAccessible(true);
    }
  }

  /**
   * register the given metric name for the method as a gauge
   */
  protected void registerMetricAsGauge(final Object bean, final String metricName, final Method method) {
    metricRegistry.register(metricName, (com.codahale.metrics.Gauge<Object>) () -> {
      try {
        return method.invoke(bean);
      }
      catch (Exception e) {
        return new RuntimeException(e);
      }
    });
  }
}
