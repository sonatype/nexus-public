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
package org.sonatype.nexus.bootstrap.metrics;

import java.util.List;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.systemchecks.ConditionallyAppliedHealthCheck;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.common.metrics.MetricsConstants.NEXUS_METRICS_REGISTRY_NAME;

@Configuration
public class MetricsConfiguration
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Primary
  @Qualifier(NEXUS_METRICS_REGISTRY_NAME)
  @Bean
  public MetricRegistry nexusMetricRegistry(final List<Metric> metrics) {
    MetricRegistry registry = SharedMetricRegistries.getOrCreate(NEXUS_METRICS_REGISTRY_NAME);

    for (Metric metric : metrics) {
      // Avoid adding registry to itself
      if (!(metric instanceof MetricRegistry)) {
        log.debug("Registering: {}", metric);
        String name = QualifierUtil.value(metric)
            .orElseGet(metric.getClass()::getName);
        registry.register(name, metric);
      }
    }

    return registry;
  }

  @Bean
  public HealthCheckRegistry healthCheckRegistry(final List<HealthCheck> healthChecks) {
    HealthCheckRegistry registry = new HealthCheckRegistry();

    for (HealthCheck healthCheck : healthChecks) {
      String name = QualifierUtil.value(healthCheck)
          .orElseGet(healthCheck.getClass()::getName);
      if (healthCheck instanceof ConditionallyAppliedHealthCheck) {
        log.debug("Delay Registry of {} Until Conditional Registration", name);
      }
      else {
        log.debug("Registering: {}", healthCheck);
        registry.register(name, healthCheck);
      }
    }

    return registry;
  }
}
