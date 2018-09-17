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
package org.sonatype.nexus.cleanup.internal.task;

import java.util.Date;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.ScheduleFactory;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.cleanup.internal.task.CleanupBootService.TASK_NAME;

public class CleanupBootServiceTest
    extends TestSupport
{
  private TaskConfiguration taskConfig;
  
  @Mock
  private TaskScheduler taskScheduler;
  
  @Mock
  private ScheduleFactory scheduleFactory;
  
  @Mock
  private TaskInfo taskInfo;

  private CleanupBootService underTest;

  @Before
  public void setup() throws Exception {
    underTest = new CleanupBootService(taskScheduler);

    taskConfig = new TaskConfiguration();
    taskConfig.setTypeId(CleanupTaskDescriptor.TYPE_ID);
    when(taskInfo.getConfiguration()).thenReturn(taskConfig);
    when(taskScheduler.listsTasks()).thenReturn(emptyList());

    when(taskScheduler.createTaskConfigurationInstance(CleanupTaskDescriptor.TYPE_ID)).thenReturn(taskConfig);
    when(taskScheduler.getScheduleFactory()).thenReturn(scheduleFactory);
  }

  @Test
  public void shouldCreateTaskOnStart() throws Exception {
    underTest.doStart();
    
    verify(taskScheduler).scheduleTask(any(), any());
  }

  @Test
  public void scheduleTaskDailyAt1am() throws Exception {
    underTest.doStart();
    
    verify(scheduleFactory).cron(any(Date.class), eq("0 0 1 * * ?"));
  }

  @Test
  public void setTaskName() throws Exception {
    underTest.doStart();

    assertThat(taskConfig.getName(), is(equalTo(TASK_NAME)));
  }

  @Test
  public void doNotCreateTaskIfAlreadyExists() throws Exception {
    when(taskScheduler.listsTasks()).thenReturn(ImmutableList.of(taskInfo));
    
    underTest.doStart();

    verify(taskScheduler, never()).scheduleTask(any(), any());
  }
}
