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

import com.yammer.metrics.core.Clock
import com.yammer.metrics.core.HealthCheckRegistry
import com.yammer.metrics.core.MetricPredicate
import com.yammer.metrics.core.MetricsRegistry
import com.yammer.metrics.core.VirtualMachineMetrics
import com.yammer.metrics.reporting.ConsoleReporter
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

  private final VirtualMachineMetrics virtualMachineMetrics

  private final MetricsRegistry metricsRegistry

  private final HealthCheckRegistry healthCheckRegistry

  @Inject
  MetricsCustomizer(final Clock clock,
                    final VirtualMachineMetrics virtualMachineMetrics,
                    final MetricsRegistry metricsRegistry,
                    final HealthCheckRegistry healthCheckRegistry)
  {
    this.clock = checkNotNull(clock)
    this.virtualMachineMetrics = checkNotNull(virtualMachineMetrics)
    this.metricsRegistry = checkNotNull(metricsRegistry)
    this.healthCheckRegistry = checkNotNull(healthCheckRegistry)
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
          virtualMachineMetrics.threadDump(it)
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
          def reporter = new ConsoleReporter(
              metricsRegistry,
              new PrintStream(it),
              MetricPredicate.ALL,
              clock,
              TimeZone.getDefault()
          )
          reporter.run()
        }
      }
    }
  }
}