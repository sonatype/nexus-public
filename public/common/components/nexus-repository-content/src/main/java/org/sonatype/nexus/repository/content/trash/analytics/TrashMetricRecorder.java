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
package org.sonatype.nexus.repository.content.trash.analytics;

/**
 * Write-path SPI for recording trash (Trash) lifecycle counters, keyed by {@code format} x {@code entityType}.
 *
 * <p>
 * This interface lives in {@code nexus-repository-content} so the trash services (in
 * {@code nexus-trash-plugin}) can depend on it without depending on the analytics module. The implementation
 * lives in {@code nexus-analytics-common} and is wired at runtime by Spring; it is intentionally optional so that
 * distributions/editions lacking analytics (e.g. Community Edition) behave normally when no bean is present.
 * </p>
 *
 * <p>
 * Implementations MUST be non-blocking and O(1) at the call site: recording happens on the trash/restore/purge
 * hot path and must never affect correctness or latency of the underlying operation. Callers additionally guard every
 * invocation so a recorder failure can never break the operation being measured.
 * </p>
 *
 * <p>
 * {@code entityType} values are the canonical {@link TrashEntityType} constants
 * ({@code REPOSITORY}/{@code COMPONENT}/{@code ASSET}).
 * </p>
 */
public interface TrashMetricRecorder
{
  /**
   * Records a single trash of an entity of the given format and entity type.
   */
  void recordTrash(String format, String entityType);

  /**
   * Records a single successful recovery (restore) of an entity of the given format and entity type.
   */
  void recordRecovery(String format, String entityType);

  /**
   * Records a single genuinely-failed recovery (restore) attempt of an entity of the given format and entity type.
   *
   * <p>
   * This is reserved for unexpected failures during the restore itself. Expected, user-correctable precondition
   * blocks (e.g. the parent repository is trashed, a live name collision, or the item is already gone) are
   * surfaced to the caller as an error but MUST NOT be recorded here, so the downstream recovery success-rate KPI
   * reflects true restore reliability rather than benign two-step user workflows (NEXUS-54934).
   * </p>
   */
  void recordRecoveryFailure(String format, String entityType);

  /**
   * Records a single manual (user-initiated) purge of an entity of the given format and entity type.
   */
  void recordPurge(String format, String entityType);

  /**
   * Records a single retention-expiry (scheduled task) purge of an entity of the given format and entity type.
   *
   * <p>
   * Tracked separately from {@link #recordPurge} so downstream analytics can distinguish automatic
   * retention-driven cleanup from manual purges (a signal for whether the default retention window is right).
   * </p>
   */
  void recordExpiredPurge(String format, String entityType);

  /**
   * Records {@code count} retention-expiry purges for a {@code format} x {@code entityType} in a single call. The
   * default implementation delegates to {@link #recordExpiredPurge(String, String)} per unit; accumulator-backed
   * implementations should override this to apply the count in one operation (avoids O(count) work when a scheduled
   * purge drains a large batch).
   */
  default void recordExpiredPurge(final String format, final String entityType, final int count) {
    for (int i = 0; i < count; i++) {
      recordExpiredPurge(format, entityType);
    }
  }
}
