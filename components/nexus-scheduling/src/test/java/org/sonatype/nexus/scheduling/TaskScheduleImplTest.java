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
package org.sonatype.nexus.scheduling;

import java.util.List;
import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.scheduling.TaskSchedulerImpl.REPO_MOVE_TYPE_ID;

/**
 * Test class for {@link TaskSchedulerImpl}.
 *
 */
public class TaskScheduleImplTest
    extends TestSupport
{
  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private EventManager eventManager;

  @Mock
  private TaskFactory taskFactory;

  @Mock
  private Provider<SchedulerSPI> scheduler;

  @Mock
  private TaskInfo taskInfo1;

  @Mock
  private TaskInfo taskInfo2;

  @Mock
  private SchedulerSPI schedulerSPI;

  TaskSchedulerImpl underTest;

  @Before
  public void setup() {
    when(taskInfo1.getTypeId()).thenReturn(REPO_MOVE_TYPE_ID);
    when(taskInfo2.getTypeId()).thenReturn("anytype");

    when(scheduler.get()).thenReturn(schedulerSPI);
    when(schedulerSPI.listsTasks()).thenReturn(asList(taskInfo1, taskInfo2));
    when(schedulerSPI.getTaskById("1")).thenReturn(taskInfo1);
    when(schedulerSPI.getTaskById("2")).thenReturn(taskInfo2);

    underTest = new TaskSchedulerImpl(eventManager, taskFactory, scheduler);
  }

  @Test
  public void listTasks_TaskDisabled() {
    List<TaskInfo> taskList = underTest.listsTasks();
    assertThat(taskList.size(), is(1));
    assertThat(taskList.get(0).getTypeId(), not(REPO_MOVE_TYPE_ID));
  }

  @Test
  public void listTasks_TaskEnabled() {
    underTest.changeRepoBlobstoreTaskEnabled = true;
    List<TaskInfo> taskList = underTest.listsTasks();
    assertThat(taskList.size(), is(2));
  }

  @Test
  public void getTaskById_TaskDisabled() {
    TaskInfo taskInfo = underTest.getTaskById("1");
    assertThat(taskInfo, is(nullValue()));

    taskInfo = underTest.getTaskById("2");
    assertThat(taskInfo, is(taskInfo2));
  }

  @Test
  public void getTaskById_TaskEnabled() {
    underTest.changeRepoBlobstoreTaskEnabled = true;

    TaskInfo taskInfo = underTest.getTaskById("1");
    assertThat(taskInfo, is(taskInfo1));
    assertThat(taskInfo1.getTypeId(), is(REPO_MOVE_TYPE_ID));

    taskInfo = underTest.getTaskById("2");
    assertThat(taskInfo, is(taskInfo2));
  }
}
