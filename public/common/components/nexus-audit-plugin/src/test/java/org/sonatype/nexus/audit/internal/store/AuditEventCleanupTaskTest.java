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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sonatype.nexus.audit.internal.AuditRetentionSettings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditEventCleanupTask}. No database connection is created;
 * {@link AuditEventStore} is mocked.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditEventCleanupTaskTest
{
  private static final Instant FIXED_INSTANT = Instant.parse("2026-08-01T00:00:00Z");

  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

  @Mock
  private AuditEventStore store;

  @Mock
  private AuditRetentionSettings settings;

  @Captor
  private ArgumentCaptor<OffsetDateTime> cutoffCaptor;

  @Test
  void computesCutoffFromRetentionDays() throws Exception {
    when(settings.getRetentionDays()).thenReturn(90);
    when(store.deleteOlderThan(cutoffCaptor.capture(), anyInt())).thenReturn(0);

    AuditEventCleanupTask task = new AuditEventCleanupTask(store, settings, FIXED_CLOCK);
    task.execute();

    OffsetDateTime cutoff = cutoffCaptor.getValue();
    assertThat(cutoff.toInstant(), is(Instant.parse("2026-05-03T00:00:00Z")));
  }

  @Test
  void loopsUntilBatchReturnsFewerThanBatchSize() throws Exception {
    when(settings.getRetentionDays()).thenReturn(90);
    when(store.deleteOlderThan(any(), eq(AuditEventCleanupTask.BATCH_SIZE)))
        .thenReturn(AuditEventCleanupTask.BATCH_SIZE)
        .thenReturn(AuditEventCleanupTask.BATCH_SIZE)
        .thenReturn(250);

    AuditEventCleanupTask task = new AuditEventCleanupTask(store, settings, FIXED_CLOCK);
    task.execute();

    verify(store, times(3)).deleteOlderThan(any(), eq(AuditEventCleanupTask.BATCH_SIZE));
  }

  @Test
  void loopsUntilBatchReturnsZero() throws Exception {
    when(settings.getRetentionDays()).thenReturn(90);
    when(store.deleteOlderThan(any(), eq(AuditEventCleanupTask.BATCH_SIZE)))
        .thenReturn(AuditEventCleanupTask.BATCH_SIZE)
        .thenReturn(0);

    AuditEventCleanupTask task = new AuditEventCleanupTask(store, settings, FIXED_CLOCK);
    task.execute();

    verify(store, times(2)).deleteOlderThan(any(), eq(AuditEventCleanupTask.BATCH_SIZE));
  }

  @Test
  void skipsWhenRetentionDaysBelowMinimum() throws Exception {
    when(settings.getRetentionDays()).thenReturn(0);

    AuditEventCleanupTask task = new AuditEventCleanupTask(store, settings, FIXED_CLOCK);
    task.execute();

    verify(store, never()).deleteOlderThan(any(), anyInt());
  }

  @Test
  void skipsWhenRetentionDaysNegative() throws Exception {
    when(settings.getRetentionDays()).thenReturn(-5);

    AuditEventCleanupTask task = new AuditEventCleanupTask(store, settings, FIXED_CLOCK);
    task.execute();

    verify(store, never()).deleteOlderThan(any(), anyInt());
  }

  @Test
  void stopsWhenCanceled() throws Exception {
    when(settings.getRetentionDays()).thenReturn(90);
    when(store.deleteOlderThan(any(), eq(AuditEventCleanupTask.BATCH_SIZE)))
        .thenReturn(AuditEventCleanupTask.BATCH_SIZE);

    AuditEventCleanupTask realTask = new AuditEventCleanupTask(store, settings, FIXED_CLOCK);
    AuditEventCleanupTask task = spy(realTask);
    when(task.isCanceled()).thenReturn(false).thenReturn(true);

    task.execute();

    verify(store, times(1)).deleteOlderThan(any(), eq(AuditEventCleanupTask.BATCH_SIZE));
  }

  @Test
  void skipsWhenDisabledByFeatureFlag() throws Exception {
    AuditEventCleanupTask task = new AuditEventCleanupTask(store, settings, false, FIXED_CLOCK);

    task.execute();

    verify(store, never()).deleteOlderThan(any(), anyInt());
  }

  @Test
  void getMessageReflectsRetentionDays() {
    when(settings.getRetentionDays()).thenReturn(45);
    AuditEventCleanupTask task = new AuditEventCleanupTask(store, settings, FIXED_CLOCK);
    assertThat(task.getMessage(), is("Deleting audit events older than 45 days"));
  }
}
