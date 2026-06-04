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
package org.sonatype.nexus.internal.jwt.datastore;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.scheduling.PeriodicJobService.PeriodicJob;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.security.jwt.JwtSessionRevocationService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Manages periodic cleanup of expired JWT session revocations.
 *
 * Runs a daily job to remove expired JWT session revocations from the database
 * to prevent table bloat.
 */
@Component
@ManagedLifecycle(phase = TASKS)
@ConditionalOnProperty(name = JWT_ENABLED, havingValue = "true")
public class JwtSessionCleanupTaskManager
    extends StateGuardLifecycleSupport
{
  /**
   * Cleanup interval: 24 hours (once per day).
   *
   * This is less frequent than blob cleanup because JWT session revocations are
   * small records and cleanup is less critical. The default JWT expiry is 30 minutes,
   * so daily cleanup is sufficient.
   */
  static final Duration CLEANUP_INTERVAL = Duration.ofHours(24);

  private final PeriodicJobService periodicJobService;

  private final JwtSessionRevocationService jwtSessionRevocationService;

  private Optional<PeriodicJob> cleanupJob = Optional.empty();

  @Autowired
  public JwtSessionCleanupTaskManager(
      final PeriodicJobService periodicJobService,
      final JwtSessionRevocationService jwtSessionRevocationService)
  {
    this.periodicJobService = checkNotNull(periodicJobService);
    this.jwtSessionRevocationService = checkNotNull(jwtSessionRevocationService);
  }

  @Override
  protected void doStart() throws Exception {
    periodicJobService.startUsing();

    log.info("Scheduling JWT session cleanup job to run every 24 hours");
    cleanupJob = Optional.of(periodicJobService.schedule(this::cleanupExpiredSessions, CLEANUP_INTERVAL));
  }

  @Override
  protected void doStop() throws Exception {
    cleanupJob.ifPresent(PeriodicJob::cancel);

    periodicJobService.stopUsing();
  }

  /**
   * Cleanup method that removes expired JWT session revocations from the database.
   */
  protected void cleanupExpiredSessions() {
    try {
      log.info("Starting cleanup of expired JWT session revocations");

      long start = System.currentTimeMillis();
      int deleted = jwtSessionRevocationService.deleteExpiredSessions();
      long duration = System.currentTimeMillis() - start;

      log.info("Completed cleanup of expired JWT session revocations: deleted {} records in {} ms",
          deleted, duration);
    }
    catch (Exception e) {
      log.error("Failed to cleanup expired JWT session revocations", e);
    }
  }
}
