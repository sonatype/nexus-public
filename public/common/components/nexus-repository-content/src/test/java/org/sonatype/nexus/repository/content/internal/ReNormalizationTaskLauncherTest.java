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
package org.sonatype.nexus.repository.content.internal;

import java.time.Duration;
import java.util.Optional;

import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.scheduling.PeriodicJobService.PeriodicJob;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.KeyValueEvent;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.kv.ValueType;
import org.sonatype.nexus.node.datastore.NodeHeartbeatManager;
import org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTask.CURSOR_KEY;
import static org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTask.GATE_KEY_FORMAT;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ReNormalizationTaskLauncherTest
{
  @Mock
  private GlobalKeyValueStore globalKeyValueStore;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private TaskConfiguration taskConfiguration;

  @Mock
  private NodeHeartbeatManager heartbeatManager;

  @Mock
  private PeriodicJobService jobService;

  private ReNormalizationTaskLauncher underTest;

  @Before
  public void setUp() {
    underTest = new ReNormalizationTaskLauncher(globalKeyValueStore, taskScheduler, heartbeatManager, jobService);
    when(globalKeyValueStore.getKey(CURSOR_KEY)).thenReturn(Optional.empty());
    when(taskScheduler.createTaskConfigurationInstance(ReNormalizeComponentVersionTaskDescriptor.TYPE_ID))
        .thenReturn(taskConfiguration);
    when(jobService.schedule(any(Runnable.class), any(Duration.class))).thenReturn(mock(PeriodicJob.class));
  }

  @Test
  public void testNoSubmissionWhenNoGateRaisedAndNoCursorPresent() throws Exception {
    underTest.doStart();

    verify(taskScheduler, never()).submit(taskConfiguration);
  }

  @Test
  public void testSubmitsDirectlyWhenAnyTargetFormatGateIsRaised() throws Exception {
    mockGateRaised("npm", true);

    underTest.doStart();

    verify(taskScheduler).createTaskConfigurationInstance(eq(ReNormalizeComponentVersionTaskDescriptor.TYPE_ID));
    verify(taskScheduler).submit(taskConfiguration);
  }

  @Test
  public void testSubmitsDirectlyWhenCursorIsPresentEvenIfNoGateIsRaised() throws Exception {
    mockCursorPresent();

    underTest.doStart();

    verify(taskScheduler).submit(taskConfiguration);
  }

  @Test
  public void testDoesNotDoubleScheduleWhenTaskAlreadyScheduledOrRunning() throws Exception {
    mockCursorPresent();
    when(taskScheduler.getTaskByTypeId(ReNormalizeComponentVersionTaskDescriptor.TYPE_ID))
        .thenReturn(mock(TaskInfo.class));

    underTest.doStart();

    verify(taskScheduler, never()).submit(taskConfiguration);
  }

  @Test
  public void testNeverInspectsFormatsOutsideRenormalizeFormats() throws Exception {
    underTest.doStart();

    verify(globalKeyValueStore, never()).getKey(GATE_KEY_FORMAT.formatted("maven2"));
    verify(globalKeyValueStore, never()).getKey(GATE_KEY_FORMAT.formatted("docker"));
  }

  @Test
  public void testSubmissionFailureIsSwallowedNotPropagated() throws Exception {
    mockCursorPresent();
    when(taskScheduler.submit(taskConfiguration)).thenThrow(new IllegalStateException("scheduler not started"));

    underTest.doStart();
  }

  @Test
  public void testDefersSubmissionWhileNodesReportMixedVersions() throws Exception {
    mockCursorPresent();
    when(heartbeatManager.hasVersionSkew()).thenReturn(true);

    underTest.doStart();

    verify(taskScheduler, never()).submit(taskConfiguration);
  }

  @Test
  public void testResumesSubmissionOnceVersionSkewClearsOnARecheck() throws Exception {
    mockCursorPresent();
    when(heartbeatManager.hasVersionSkew()).thenReturn(true);

    ArgumentCaptor<Runnable> recheck = ArgumentCaptor.forClass(Runnable.class);
    underTest.doStart();
    verify(jobService).schedule(recheck.capture(), any(Duration.class));
    verify(taskScheduler, never()).submit(taskConfiguration);

    // the rolling restart completes: every node now reports the same version/build
    when(heartbeatManager.hasVersionSkew()).thenReturn(false);
    recheck.getValue().run();

    verify(taskScheduler).submit(taskConfiguration);
  }

  @Test
  public void testSubmitsNormallyWhenHeartbeatManagerIsAbsent() throws Exception {
    // OSS/single-node Spring contexts have no NodeHeartbeatManager bean at all - a missing manager must not
    // block submission, since there's no cluster to see version skew in the first place
    ReNormalizationTaskLauncher launcherWithoutHeartbeatManager =
        new ReNormalizationTaskLauncher(globalKeyValueStore, taskScheduler, null, jobService);
    mockCursorPresent();

    launcherWithoutHeartbeatManager.doStart();

    verify(taskScheduler).submit(taskConfiguration);
  }

  @Test
  public void testDoStartRegistersPeriodicRecheckAndCancelsItOnStop() throws Exception {
    underTest.doStart();
    verify(jobService).startUsing();

    underTest.doStop();
    verify(jobService).stopUsing();
  }

  @Test
  public void testEventForGateKeyTriggersSubmissionEvenWhenNoGateWasRaisedAtBoot() throws Exception {
    // nothing outstanding at boot: the launcher's own doStart() finds no gate/cursor and submits nothing
    underTest.doStart();
    verify(taskScheduler, never()).submit(taskConfiguration);

    // the gate is raised afterwards (e.g. by ScheduleReNormalizationMigrationStep on another node) and this
    // node picks it up via the KeyValueEvent, not by re-running doStart()
    mockGateRaised("npm", true);
    underTest.on(new KeyValueEvent(GATE_KEY_FORMAT.formatted("npm"), true));

    verify(taskScheduler).submit(taskConfiguration);
  }

  @Test
  public void testEventForCursorKeyTriggersSubmission() throws Exception {
    underTest.doStart();
    verify(taskScheduler, never()).submit(taskConfiguration);

    mockCursorPresent();
    underTest.on(new KeyValueEvent(CURSOR_KEY, true));

    verify(taskScheduler).submit(taskConfiguration);
  }

  @Test
  public void testEventForUnrelatedKeyIsIgnored() throws Exception {
    underTest.on(new KeyValueEvent("some.unrelated.key", true));

    verify(taskScheduler, never()).createTaskConfigurationInstance(ReNormalizeComponentVersionTaskDescriptor.TYPE_ID);
  }

  private void mockGateRaised(final String format, final boolean raised) {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey(GATE_KEY_FORMAT.formatted(format));
    kv.setType(ValueType.BOOLEAN);
    kv.setValue(raised);
    when(globalKeyValueStore.getKey(GATE_KEY_FORMAT.formatted(format))).thenReturn(Optional.of(kv));
  }

  private void mockCursorPresent() {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey(CURSOR_KEY);
    kv.setType(ValueType.BOOLEAN);
    kv.setValue(true);
    when(globalKeyValueStore.getKey(CURSOR_KEY)).thenReturn(Optional.of(kv));
  }
}
