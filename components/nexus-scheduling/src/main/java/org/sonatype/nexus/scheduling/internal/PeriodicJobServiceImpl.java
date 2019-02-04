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
package org.sonatype.nexus.scheduling.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.scheduling.PeriodicJobService;
import org.sonatype.nexus.thread.NexusThreadFactory;

import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Default implementation of {@link PeriodicJobService}, based on a ScheduledExecutorService.
 *
 * @since 3.0
 */
@Named
@Singleton
public class PeriodicJobServiceImpl
    extends StateGuardLifecycleSupport
    implements PeriodicJobService
{
  private ScheduledExecutorService executor;

  private int activeClients;

  public synchronized void startUsing() throws Exception {
    if (activeClients == 0) {
      start();
    }
    activeClients++;
  }

  public synchronized void stopUsing() throws Exception {
    checkState(activeClients > 0, "Not started");
    activeClients--;
    if (activeClients == 0) {
      stop();
    }
  }

  @Override
  protected void doStart() throws Exception {
    executor = Executors.newScheduledThreadPool(1, new NexusThreadFactory("blobstore-metrics", "blobstore"));
  }

  @Override
  protected void doStop() throws Exception {
    executor.shutdown();
    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
      log.warn("Failed to terminate thread pool in allotted time");
    }
    executor = null;
  }

  @Override
  @Guarded(by = STARTED)
  public PeriodicJob schedule(final Runnable runnable, final int repeatPeriodSeconds) {
    ScheduledFuture<?> scheduledFuture = executor.scheduleAtFixedRate(
        wrap(runnable),
        repeatPeriodSeconds,
        repeatPeriodSeconds,
        TimeUnit.SECONDS);

    return () -> scheduledFuture.cancel(false);
  }

  private Runnable wrap(Runnable inner) {
    return () -> {
      try {
        inner.run();
      }
      catch (Exception e) {
        // Do not propagate as this will cancel the recurring job
        log.error("Periodic job threw exception", e);
      }
    };
  }
}
