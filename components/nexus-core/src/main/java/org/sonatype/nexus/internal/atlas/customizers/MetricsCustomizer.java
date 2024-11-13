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
package org.sonatype.nexus.internal.atlas.customizers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.json.HealthCheckModule;
import com.codahale.metrics.json.MetricsModule;
import com.codahale.metrics.jvm.ThreadDump;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.HIGH;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.OPTIONAL;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.METRICS;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.SYSINFO;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.THREAD;

/**
 * Adds metrics (threads,metrics,healthcheck) to support bundle.
 *
 * @since 2.7
 */
@Named
@Singleton
public class MetricsCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private final MetricRegistry metricRegistry;

  private final HealthCheckRegistry healthCheckRegistry;

  private final ThreadDump threadDump;

  private final ObjectMapper healthCheckObjectMapper;

  private final ObjectMapper metricsObjectMapper;

  @Inject
  public MetricsCustomizer(final MetricRegistry metricRegistry, final HealthCheckRegistry healthCheckRegistry) {
    this.metricRegistry = checkNotNull(metricRegistry);
    this.healthCheckRegistry = checkNotNull(healthCheckRegistry);
    this.threadDump = new ThreadDump(ManagementFactory.getThreadMXBean());
    this.healthCheckObjectMapper = new ObjectMapper().registerModule(new HealthCheckModule());
    this.metricsObjectMapper = new ObjectMapper().registerModule(new MetricsModule(
        TimeUnit.SECONDS, // rate-unit
        TimeUnit.SECONDS, // duration-unit
        false // show-samples
    ));
  }

  @Override
  public void customize(final SupportBundle supportBundle) {
    // add thread-dump
    supportBundle.add(new GeneratedContentSourceSupport(THREAD, "info/threads.txt", HIGH)
    {
      @Override
      protected void generate(final File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
          threadDump.dump(fos);
        }
        catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    });

    // add healthchecks
    supportBundle.add(new GeneratedContentSourceSupport(SYSINFO, "info/healthcheck.json", OPTIONAL)
    {
      @Override
      protected void generate(final File file) {
        SortedMap<String, Result> results = healthCheckRegistry.runHealthChecks();
        try (FileOutputStream fos = new FileOutputStream(file)) {
          healthCheckObjectMapper.writerWithDefaultPrettyPrinter().writeValue(fos, results);
        }
        catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    });

    // add metrics
    supportBundle.add(new GeneratedContentSourceSupport(METRICS, "info/metrics.json", OPTIONAL)
    {
      @Override
      protected void generate(final File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
          metricsObjectMapper.writerWithDefaultPrettyPrinter().writeValue(fos, metricRegistry);
        }
        catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    });
  }
}
