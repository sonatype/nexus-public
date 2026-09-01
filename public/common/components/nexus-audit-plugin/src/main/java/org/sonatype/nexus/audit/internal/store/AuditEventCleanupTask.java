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
package org.sonatype.nexus.audit.internal.store;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.sonatype.nexus.audit.internal.AuditRetentionSettings;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.AUDIT_EVENTS_CLEANUP_TASK_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.AUDIT_EVENTS_CLEANUP_TASK_ENABLED_NAMED_VALUE;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

/**
 * Scheduled task that prunes {@code audit_events} rows whose {@code timestamp} is older than
 * {@code now - retentionDays}. The retention window is read from {@link AuditRetentionSettings}
 * at execution time so schedule and configuration are decoupled.
 * <p>
 * Deletion is performed in bounded batches ({@value #BATCH_SIZE} rows per commit) with a
 * safety cap of {@value #MAX_BATCHES} batches per run to protect against pathological states.
 */
@Component
@Scope(SCOPE_PROTOTYPE)
public class AuditEventCleanupTask
    extends TaskSupport
    implements Cancelable
{
  static final int BATCH_SIZE = 1_000;

  static final int MAX_BATCHES = 10_000;

  private final AuditEventStore store;

  private final AuditRetentionSettings settings;

  private final boolean enabled;

  private final Clock clock;

  @Autowired
  public AuditEventCleanupTask(
      final AuditEventStore store,
      final AuditRetentionSettings settings,
      @Value(AUDIT_EVENTS_CLEANUP_TASK_ENABLED_NAMED_VALUE) final boolean enabled)
  {
    this(store, settings, enabled, Clock.systemUTC());
  }

  AuditEventCleanupTask(final AuditEventStore store, final AuditRetentionSettings settings, final Clock clock) {
    this(store, settings, true, clock);
  }

  AuditEventCleanupTask(
      final AuditEventStore store,
      final AuditRetentionSettings settings,
      final boolean enabled,
      final Clock clock)
  {
    this.store = checkNotNull(store);
    this.settings = checkNotNull(settings);
    this.enabled = enabled;
    this.clock = checkNotNull(clock);
  }

  @Override
  public String getMessage() {
    return "Deleting audit events older than " + settings.getRetentionDays() + " days";
  }

  @Override
  protected Object execute() throws Exception {
    String initiator = describeInitiator();
    if (!enabled) {
      log.info("Skipping audit_events cleanup — disabled by feature flag ({}=false) (initiator={})",
          AUDIT_EVENTS_CLEANUP_TASK_ENABLED, initiator);
      return null;
    }
    int retentionDays = settings.getRetentionDays();
    // Defensive guard for tests that inject AuditRetentionSettings mocks;
    // in production this path is unreachable because setRetentionDays() rejects values < 1.
    if (retentionDays < 1) {
      log.warn("Skipping audit_events cleanup — retentionDays={} is out of range (initiator={})",
          retentionDays, initiator);
      return null;
    }

    OffsetDateTime cutoff = OffsetDateTime.now(clock).minusDays(retentionDays);
    log.info("Starting audit_events cleanup — retention={} days, cutoff={}, initiator={}",
        retentionDays, cutoff, initiator);

    long start = System.nanoTime();
    long totalDeleted = 0;
    int batches = 0;

    while (batches < MAX_BATCHES) {
      if (isCanceled()) {
        log.info("Audit_events cleanup canceled after {} batches ({} rows deleted, initiator={})",
            batches, totalDeleted, initiator);
        return null;
      }
      int deleted = store.deleteOlderThan(cutoff, BATCH_SIZE);
      totalDeleted += deleted;
      batches++;
      if (deleted < BATCH_SIZE) {
        break;
      }
    }

    long durationMs = (System.nanoTime() - start) / 1_000_000L;
    if (batches >= MAX_BATCHES) {
      log.warn(
          "Audit_events cleanup hit safety cap of {} batches ({} rows); remaining rows will be pruned next run "
              + "(initiator={})",
          MAX_BATCHES, totalDeleted, initiator);
    }
    if (totalDeleted == 0) {
      log.info("Audit_events cleanup finished — no rows older than cutoff (initiator={}, duration={} ms)",
          initiator, durationMs);
    }
    else {
      log.info("Audit_events cleanup finished — deleted={} rows across {} batches in {} ms (initiator={})",
          totalDeleted, batches, durationMs, initiator);
    }
    return null;
  }

  private String describeInitiator() {
    TaskInfo info = getTaskInfo();
    if (info == null) {
      return "scheduled";
    }
    String triggerSource = info.getTriggerSource();
    return triggerSource != null ? "manual:" + triggerSource : "scheduled";
  }
}
