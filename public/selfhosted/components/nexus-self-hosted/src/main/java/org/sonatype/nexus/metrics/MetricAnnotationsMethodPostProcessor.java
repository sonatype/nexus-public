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
package org.sonatype.nexus.metrics;

import java.lang.reflect.Method;
import java.util.Optional;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.common.metrics.ExceptionMeteredAspect.metricName;
import static org.sonatype.nexus.common.metrics.MetricsConstants.NEXUS_METRICS_REGISTRY_NAME;
import static org.sonatype.nexus.common.metrics.TimedAspect.metricName;
import static org.springframework.util.ReflectionUtils.getAllDeclaredMethods;

/**
 * Ensures {@link Timed} and {@link ExceptionMetered} are registered during startup as opposed to waiting for first
 * use.
 */
@Component
public class MetricAnnotationsMethodPostProcessor
    implements BeanDefinitionRegistryPostProcessor
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(NEXUS_METRICS_REGISTRY_NAME);

  @Override
  public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {
    String[] beanNames = registry.getBeanDefinitionNames();
    for (String beanName : beanNames) {
      BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
      Optional<String> className = getClassName(beanDefinition);
      try {
        className.ifPresent(this::registerMetricsOfClassMethods);
      }
      catch (Exception e) {
        log.warn("Error registering bean metrics: {}", beanName, e);
      }
    }
  }

  private void registerMetricsOfClassMethods(final String className) {
    try {
      Class<?> clazz = Class.forName(className);
      // Use ReflectionUtils to get all declared methods, including private/protected and inherited
      for (Method method : getAllDeclaredMethods(clazz)) {
        registerMetrics(method);
      }
    }
    catch (ClassNotFoundException e) {
      log.warn("Error registering bean metrics, class not found: {}", className, e);
    }
  }

  private Optional<String> getClassName(BeanDefinition beanDefinition) {
    return Optional.ofNullable(beanDefinition.getBeanClassName());
  }

  private void registerMetrics(final Method method) {
    Timed codahaleTimed = method.getAnnotation(Timed.class);
    ExceptionMetered codahaleExceptionMetered = method.getAnnotation(ExceptionMetered.class);
    if (codahaleTimed != null) {
      String metricName = metricName(method, codahaleTimed);
      metricRegistry.timer(metricName);
    }
    if (codahaleExceptionMetered != null) {
      String metricName = metricName(method, codahaleExceptionMetered);
      metricRegistry.meter(metricName);
    }
  }
}
