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

import java.util.Arrays;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.ClusteredTaskStateStore;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.events.TaskBlockedEvent;
import org.sonatype.nexus.scheduling.events.TaskDeletedEvent;
import org.sonatype.nexus.scheduling.events.TaskEventCanceled;
import org.sonatype.nexus.scheduling.events.TaskEventStarted;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedDone;
import org.sonatype.nexus.scheduling.events.TaskScheduledEvent;
import org.sonatype.nexus.scheduling.events.TaskStartedRunningEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link LocalTaskStateTracker}.
 */
public class LocalTaskStateTrackerTest
    extends TestSupport
{
  @Mock
  private ClusteredTaskStateStore store;

  @Mock
  private TaskScheduler scheduler;

  @Mock
  private TaskInfo taskInfo;

  private LocalTaskStateTracker tracker;

  @Before
  public void setUp() {
    tracker = new LocalTaskStateTracker(store, scheduler);
    when(taskInfo.getId()).thenReturn("task-id");
  }

  @Test
  public void testStart() throws Exception {
    when(scheduler.listsTasks()).thenReturn(Arrays.asList(taskInfo));
    tracker.start();
    verify(store).setLocalState(taskInfo);
  }

  @Test
  public void testOn_Deleted() {
    tracker.on(new TaskDeletedEvent(taskInfo));
    verify(store).removeClusteredState("task-id");
  }

  @Test
  public void testOn_Scheduled() {
    tracker.on(new TaskScheduledEvent(taskInfo));
    verify(store).setLocalState(taskInfo);
  }

  @Test
  public void testOn_Started() {
    tracker.on(new TaskEventStarted(taskInfo));
    verify(store).setLocalState(taskInfo);
  }

  @Test
  public void testOn_Stopped() {
    tracker.on(new TaskEventStoppedDone(taskInfo));
    verify(store).setLocalState(taskInfo);
  }

  @Test
  public void testOn_Canceled() {
    tracker.on(new TaskEventCanceled(taskInfo));
    verify(store).setLocalState(taskInfo);
  }

  @Test
  public void testOn_Blocked() {
    tracker.on(new TaskBlockedEvent(taskInfo));
    verify(store).setLocalState(taskInfo);
  }

  @Test
  public void testOn_StartedRunning() {
    tracker.on(new TaskStartedRunningEvent(taskInfo));
    verify(store).setLocalState(taskInfo);
  }
}
