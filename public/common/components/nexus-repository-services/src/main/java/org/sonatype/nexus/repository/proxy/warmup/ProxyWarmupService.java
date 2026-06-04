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
package org.sonatype.nexus.repository.proxy.warmup;

import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.RepositoryStartedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;

/**
 * Proxy repository warmup service.
 *
 * Asynchronously establishes HTTP connections to remote repositories when proxy repositories start.
 * This prevents HTTP 503 errors on first client request by pre-warming the connection pool.
 *
 * Features:
 * - Async execution (doesn't block repository startup)
 * - Configurable via system properties
 * - Graceful failure handling (logs warning, doesn't fail startup)
 * - Detailed observability logging
 */
@ManagedLifecycle(phase = SERVICES)
@Component
public class ProxyWarmupService
    extends StateGuardLifecycleSupport
    implements EventAware
{
  private static final String WARMUP_ENABLED_PROPERTY = "nexus.proxy.warmup.enabled";

  private static final String WARMUP_TIMEOUT_PROPERTY = "nexus.proxy.warmup.timeout";

  private static final String WARMUP_THREADPOOL_SIZE_PROPERTY = "nexus.proxy.warmup.threadpool.size";

  private static final boolean DEFAULT_WARMUP_ENABLED = false;

  private static final int DEFAULT_WARMUP_TIMEOUT_MS = 5000;

  private static final int DEFAULT_THREADPOOL_SIZE = 5;

  private static final int MAX_THREADPOOL_SIZE = 20;

  private final boolean warmupEnabled;

  private final int warmupTimeoutMs;

  private final RepositoryManager repositoryManager;

  private ThreadPoolExecutor executorService;

  /**
   * Counter for proxy repositories as they start.
   * Used for observability and to warn if thread pool is undersized.
   * Cannot use repositoryManager.browse() during doStart() due to lifecycle ordering
   * (RepositoryManager not in STARTED state yet).
   * Thread-safe for use with @AllowConcurrentEvents.
   */
  private final AtomicInteger proxyRepoCount = new AtomicInteger(0);

  @Autowired
  public ProxyWarmupService(final RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
    this.warmupEnabled = SystemPropertiesHelper.getBoolean(WARMUP_ENABLED_PROPERTY, DEFAULT_WARMUP_ENABLED);
    this.warmupTimeoutMs = SystemPropertiesHelper.getInteger(WARMUP_TIMEOUT_PROPERTY, DEFAULT_WARMUP_TIMEOUT_MS);
  }

  @Override
  protected void doStart() throws Exception {
    log.info("Starting ProxyWarmupService (enabled={})", warmupEnabled);
    if (warmupEnabled) {
      int threadPoolSize = SystemPropertiesHelper.getInteger(
          WARMUP_THREADPOOL_SIZE_PROPERTY,
          DEFAULT_THREADPOOL_SIZE);

      executorService = new ThreadPoolExecutor(
          threadPoolSize,
          threadPoolSize,
          60L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          new NexusThreadFactory("proxy-warmup", "proxy-warmup"));

      log.info(
          "ProxyWarmupService started successfully (enabled={}, timeout={}ms, threads={}, listening for RepositoryStartedEvent)",
          warmupEnabled, warmupTimeoutMs, threadPoolSize);
    }
    else {
      log.info("ProxyWarmupService started in disabled mode - no warmup will occur (set {}=true to enable)",
          WARMUP_ENABLED_PROPERTY);
    }
  }

  @Override
  protected void doStop() throws Exception {
    if (executorService != null) {
      log.info("Shutting down ProxyWarmupService executor");
      executorService.shutdown();

      try {
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
          log.warn("ProxyWarmupService executor did not terminate within 10 seconds, forcing shutdown");
          executorService.shutdownNow();

          if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            log.error("ProxyWarmupService executor did not terminate after forced shutdown");
          }
        }
        else {
          log.info("ProxyWarmupService executor shutdown complete");
        }
      }
      catch (InterruptedException e) {
        log.warn("ProxyWarmupService shutdown interrupted, forcing immediate shutdown");
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
      finally {
        executorService = null;
      }
    }
  }

  /**
   * Handles repository started events and triggers warmup for proxy repositories.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryStartedEvent event) {
    Repository repository = event.getRepository();
    String repositoryName = repository.getName();

    log.debug("Received RepositoryStartedEvent for '{}' (type: {})", repositoryName, repository.getType().getValue());

    if (!warmupEnabled) {
      log.debug("Skipping warmup for '{}' - ProxyWarmupService is disabled", repositoryName);
      return;
    }

    if (executorService == null) {
      log.warn("Skipping warmup for '{}' - ProxyWarmupService executor not initialized", repositoryName);
      return;
    }

    if (!(repository.getType() instanceof ProxyType)) {
      log.debug("Skipping warmup for '{}' - not a proxy repository (type: {})",
          repositoryName, repository.getType().getValue());
      return;
    }

    int count = proxyRepoCount.incrementAndGet();

    if (count == 5 || count == 10 || count == 20) {
      int threadPoolSize = executorService.getMaximumPoolSize();
      int recommendedSize = Math.min(Math.max(count / 2, DEFAULT_THREADPOOL_SIZE), MAX_THREADPOOL_SIZE);

      if (threadPoolSize < recommendedSize) {
        log.warn(
            "Thread pool size ({}) may be too small for {} proxy repositories - consider increasing {} to {}",
            threadPoolSize, count, WARMUP_THREADPOOL_SIZE_PROPERTY, recommendedSize);
      }
      else {
        log.info("Thread pool size ({}) is appropriate for {} proxy repositories",
            threadPoolSize, count);
      }
    }

    log.info("Scheduling warmup for proxy repository '{}'", repositoryName);
    executorService.submit(() -> warmupProxyRepository(repository));
  }

  private void warmupProxyRepository(final Repository repository) {
    long startTime = System.currentTimeMillis();
    String repositoryName = repository.getName();

    try {
      URI remoteUrl = getRemoteUrl(repository);
      if (remoteUrl == null) {
        log.debug("Skipping warmup for {} - no remote URL configured", repositoryName);
        return;
      }

      log.info("Warming up proxy repository '{}' to remote: {}", repositoryName, remoteUrl);

      HttpClientFacet httpClientFacet = repository.facet(HttpClientFacet.class);
      HttpClient httpClient = httpClientFacet.getHttpClient();

      HttpHead headRequest = new HttpHead(remoteUrl);
      headRequest.setConfig(org.apache.http.client.config.RequestConfig.custom()
          .setConnectTimeout(warmupTimeoutMs)
          .setSocketTimeout(warmupTimeoutMs)
          .setConnectionRequestTimeout(warmupTimeoutMs)
          .build());

      HttpResponse response = httpClient.execute(headRequest);
      int statusCode = response.getStatusLine().getStatusCode();
      long duration = System.currentTimeMillis() - startTime;

      if (statusCode >= 200 && statusCode < 500) {
        log.info("Proxy warmup COMPLETED for '{}' - repository ready to serve requests (status={}, duration={}ms)",
            repositoryName, statusCode, duration);
      }
      else {
        log.warn(
            "Proxy warmup COMPLETED with server error for '{}' (status={}, duration={}ms) - connection will establish on first use",
            repositoryName, statusCode, duration);
      }
    }
    catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.warn("Proxy warmup failed for '{}' after {}ms - connection will establish on first use: {}",
          repositoryName, duration, e.getMessage());
      log.debug("Proxy warmup exception details for '{}'", repositoryName, e);
    }
  }

  @Nullable
  private URI getRemoteUrl(final Repository repository) {
    try {
      ProxyFacet proxyFacet = repository.facet(ProxyFacet.class);
      return proxyFacet.getRemoteUrl();
    }
    catch (Exception e) {
      log.debug("Could not get remote URL for repository '{}': {}",
          repository.getName(), e.getMessage());
      return null;
    }
  }
}
