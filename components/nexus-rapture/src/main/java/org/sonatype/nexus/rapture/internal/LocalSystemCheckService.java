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
package org.sonatype.nexus.rapture.internal;

import java.util.Collections;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.scheduling.PeriodicJobService.PeriodicJob;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.systemchecks.NodeSystemCheckResult;
import org.sonatype.nexus.systemchecks.SystemCheckService;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Refreshes and caches the state of system {@link com.codahale.metrics.health.HealthCheck}s at the configured time
 * interval.
 *
 * @since 3.16
 */
@Named
@Singleton
@ManagedLifecycle(phase = SERVICES)
public class LocalSystemCheckService
    extends StateGuardLifecycleSupport
    implements SystemCheckService
{
  private final PeriodicJobService jobService;

  private final NodeAccess nodeAccess;

  private String hostname;

  private LoadingCache<String, Result> cache;

  private HealthCheckRegistry registry;

  private int refreshInterval;

  private PeriodicJob metricsWritingJob;

  @Inject
  public LocalSystemCheckService(
      final PeriodicJobService jobService,
      final HealthCheckRegistry registry,
      final NodeAccess nodeAccess,
      @Named("${nexus.healthcheck.refreshInterval:-15}") final int refreshInterval)
  {
    this.jobService = checkNotNull(jobService);
    this.registry = checkNotNull(registry);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.refreshInterval = refreshInterval;
  }

  @Override
  protected void doStart() throws Exception {
    cache = CacheBuilder.newBuilder().build(cacheLoader());
    jobService.startUsing();

    // Retrieving the hostname may be expensive so do this asynchronously
    nodeAccess.getHostName()
        .thenAccept(value -> hostname = value)
        .exceptionally(ex -> {
          hostname = nodeAccess.getId();
          return null;
        });

    metricsWritingJob = jobService.schedule(() -> {
      registry.getNames().forEach(k -> {
        Result oldResult = cache.getUnchecked(k);
        cache.refresh(k);
        // Refresh is lazy and doesn't refresh until the next request so force it to refresh.
        Result newResult = cache.getUnchecked(k);
        if (oldResult.isHealthy() != newResult.isHealthy()) {
          log.info("Health check status changed from {} to {} for {}", oldResult.isHealthy(), newResult.isHealthy(), k);
        }
      });
    }, refreshInterval);
  }

  @Override
  protected void doStop() throws Exception {
    metricsWritingJob.cancel();
    metricsWritingJob = null;
    jobService.stopUsing();
    cache = null;
  }

  private CacheLoader<String, Result> cacheLoader() {
    return new CacheLoader<String, Result>()
    {
      @Override
      public Result load(final String s) {
        return registry.runHealthCheck(s);
      }
    };
  }

  @Override
  @Guarded(by = STARTED)
  public Stream<NodeSystemCheckResult> getResults() {
    return Collections.singleton(new NodeSystemCheckResult(nodeAccess.getId(), hostname, cache.asMap())).stream();
  }
}
