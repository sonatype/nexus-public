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
package org.sonatype.nexus.repository.storage.internal;

import java.util.Collections;
import java.util.Date;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Cron;
import org.sonatype.nexus.scheduling.schedule.ScheduleFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StorageFacetCleanupTaskManagerTest
    extends TestSupport
{
  static final String TASK_TYPE_ID = "repository.storage-facet-cleanup";

  static final String CRON_EXPRESSION = "0 * * * * ?";

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private ScheduleFactory scheduleFactory;

  @Mock
  private TaskInfo taskInfo;

  @Mock
  private Cron cron;

  private StorageFacetCleanupTaskManager underTest;

  @Before
  public void setUp() {
    underTest = new StorageFacetCleanupTaskManager(
        taskScheduler,
        CRON_EXPRESSION
    );
  }

  @Test
  public void testStart() throws Exception {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setTypeId(TASK_TYPE_ID);

    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);
    when(taskScheduler.listsTasks()).thenReturn(Collections.singletonList(taskInfo));
    when(taskScheduler.getScheduleFactory()).thenReturn(scheduleFactory);
    when(taskScheduler.createTaskConfigurationInstance(TASK_TYPE_ID)).thenReturn(taskConfiguration);
    when(scheduleFactory.cron(any(Date.class), eq(CRON_EXPRESSION))).thenReturn(cron);

    underTest.doStart();

    verify(taskInfo).remove();
    verify(taskScheduler).scheduleTask(taskConfiguration, cron);
  }
}

