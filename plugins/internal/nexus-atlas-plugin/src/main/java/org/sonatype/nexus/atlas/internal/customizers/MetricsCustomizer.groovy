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
package org.sonatype.nexus.atlas.internal.customizers

import com.codahale.metrics.Clock
import com.codahale.metrics.health.HealthCheckRegistry
import com.codahale.metrics.MetricFilter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.jvm.ThreadDump
import org.sonatype.nexus.atlas.GeneratedContentSourceSupport
import org.sonatype.nexus.atlas.SupportBundle
import org.sonatype.nexus.atlas.SupportBundleCustomizer
import org.sonatype.sisu.goodies.common.ComponentSupport

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import static com.google.common.base.Preconditions.checkNotNull
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Priority.HIGH
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Priority.OPTIONAL
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Type.METRICS
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Type.SYSINFO
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Type.THREAD

/**
 * Adds metrics (threads,metrics,healthcheck) to support bundle.
 *
 * @since 2.7
 */
@Named
@Singleton
class MetricsCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private final Clock clock

  private final MetricRegistry metricsRegistry

  private final HealthCheckRegistry healthCheckRegistry

  private final ThreadDump threadDump

  @Inject
  MetricsCustomizer(final Clock clock,
                    final MetricRegistry metricsRegistry,
                    final HealthCheckRegistry healthCheckRegistry)
  {
    this.clock = checkNotNull(clock)
    this.metricsRegistry = checkNotNull(metricsRegistry)
    this.healthCheckRegistry = checkNotNull(healthCheckRegistry)
    this.virtualMachineMetrics = checkNotNull(virtualMachineMetrics)
  }

  @Override
  void customize(final SupportBundle supportBundle) {
    // add thread-dump
    supportBundle << new GeneratedContentSourceSupport(THREAD, 'threads.txt') {
      {
        this.priority = HIGH
      }

      @Override
      protected void generate(final File file) {
        file.withOutputStream {
          threadDump.dump(it)
        }
      }
    }

    // add healthchecks
    supportBundle << new GeneratedContentSourceSupport(SYSINFO, 'healthcheck.txt') {
      {
        this.priority = OPTIONAL
      }

      @Override
      protected void generate(final File file) {
        file.withPrintWriter { out ->
          healthCheckRegistry.runHealthChecks().each { key, result ->
            def token = result.healthy ? '*' : '!'
            def state = result.healthy ? 'OK' : 'ERROR'
            out.println "$token $key: $state"
            if (result.message) {
              out.println "  ${result.message}"
            }
            if (result.error) {
              out.println()
              result.error.printStackTrace out
              out.println()
            }
          }
        }
      }
    }

    // add metrics
    supportBundle << new GeneratedContentSourceSupport(METRICS, 'metrics.txt') {
      {
        this.priority = OPTIONAL
      }

      @Override
      protected void generate(final File file) {
        file.withOutputStream {
          // NOTE: there is no easy way to get out json report, so using the console reporter for now
          def reporter = ConsoleReporter.forRegistry(metricsRegistry)
                           .outputTo(new PrintStream(it))
                           .filter(MetricFilter.ALL)
                           .withClock(clock)
                           .build()
          reporter.report()
        }
      }
    }
  }
}
