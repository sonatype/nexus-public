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
package org.sonatype.nexus.webapp.metrics;

import java.lang.management.ManagementFactory;

import org.sonatype.nexus.guice.FilterChainModule;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.nexus.web.internal.SecurityFilter;

import com.codahale.metrics.Clock;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.PingServlet;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.servlet.ServletModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Metrics guice configuration.
 *
 * Installs servlet endpoints:
 *
 * <ul>
 * <li>/internal/ping</li>
 * <li>/internal/threads</li>
 * <li>/internal/metrics</li>
 * <li>/internal/healthcheck</li>
 * </ul>
 *
 * Protected by {@code nexus:metrics-endpoints} permission.
 *
 * @since 2.5
 */
public class MetricsModule
    extends AbstractModule
{
  private static final Logger log = LoggerFactory.getLogger(MetricsModule.class);

  private static final String MOUNT_POINT = "/internal";

  private static final String METRICS_MBEANS_ENABLED_PROP = "nexus.metrics.mbeans.enabled";

  private static final boolean METRICS_MBEANS_ENABLED = SystemPropertiesHelper
      .getBoolean(METRICS_MBEANS_ENABLED_PROP, true);

  @Override
  protected void configure() {
    // NOTE: AdminServletModule (metrics-guice integration) generates invalid links, so wire up servlets ourselves

    final Clock clock = Clock.defaultClock();
    bind(Clock.class).toInstance(clock);

    bind(HealthCheckRegistry.class).toInstance(new HealthCheckRegistry());

    // non-guicey static reference
    MetricRegistry metricsRegistry = SharedMetricRegistries.getOrCreate("nexus");
    // In the case of in-memory-reload of NXRM webapp in legacy ITs, we need to protect against adding the same metrics
    if(!metricsRegistry.getNames().contains("jvm.vm.name")){
      metricsRegistry.register(name("jvm", "vm"), new JvmAttributeGaugeSet());
      metricsRegistry.register(name("jvm", "memory"), new MemoryUsageGaugeSet());
      metricsRegistry.register(name("jvm", "buffers"), new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
      metricsRegistry.register(name("jvm", "fd_usage"), new FileDescriptorRatioGauge());
      metricsRegistry.register(name("jvm", "thread-states"), new ThreadStatesGaugeSet());
      metricsRegistry.register(name("jvm", "garbage-collectors"), new GarbageCollectorMetricSet());
    }

    if (METRICS_MBEANS_ENABLED){
      log.info("nexus.metrics MBeans available");
      JmxReporter jmxReporter = JmxReporter.forRegistry(metricsRegistry).inDomain("nexus.metrics").build();
      jmxReporter.start();
    } else {
      log.info("nexus.metrics MBeans disabled");
    }

    bind(MetricRegistry.class).toInstance(metricsRegistry);

    final JsonFactory jsonFactory = new JsonFactory(new ObjectMapper());
    bind(JsonFactory.class).toInstance(jsonFactory);

    install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        bind(NexusMetricsServlet.class);
        bind(NexusHealthCheckServlet.class);

        serve(MOUNT_POINT + "/ping").with(new PingServlet());
        serve(MOUNT_POINT + "/threads").with(new NexusThreadDumpServlet());
        serve(MOUNT_POINT + "/metrics").with(NexusMetricsServlet.class);
        serve(MOUNT_POINT + "/healthcheck").with(NexusHealthCheckServlet.class);

        // record metrics for all webapp access
        filter("/*").through(new InstrumentedFilter());

        bind(SecurityFilter.class);

        // configure security
        filter(MOUNT_POINT + "/*").through(SecurityFilter.class);
      }
    });

    // require permission to use endpoints
    install(new FilterChainModule()
    {
      @Override
      protected void configure() {
        addFilterChain(MOUNT_POINT + "/**", "noSessionCreation,authcBasic,perms[nexus:metrics-endpoints]");
      }
    });

    log.info("Metrics support configured");
  }
}
