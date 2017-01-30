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
package org.sonatype.nexus.scheduling.internal

import java.util.concurrent.Future

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.orient.freeze.DatabaseFreezeChangeEvent
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskInfo.CurrentState
import org.sonatype.nexus.scheduling.spi.SchedulerSPI

import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static java.util.Arrays.asList
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.sonatype.nexus.scheduling.TaskInfo.State.RUNNING
import static org.sonatype.nexus.scheduling.TaskInfo.State.WAITING

class TaskActivationTest
    extends TestSupport
{
  @Mock
  SchedulerSPI schedulerSpi

  @Mock
  DatabaseFreezeService databaseFreezeService

  @Mock
  TaskInfo runningTask

  @Mock
  TaskInfo waitingTask

  @Mock
  CurrentState runningTaskCurrentState

  @Mock
  CurrentState waitingTaskCurrentState

  @Mock
  Future runningTaskFuture

  private TaskActivation underTest

  @Before
  void setUp() {
    when(runningTask.getCurrentState()).thenReturn(runningTaskCurrentState)
    when(runningTaskCurrentState.getState()).thenReturn(RUNNING)
    doReturn(runningTaskFuture).when(runningTaskCurrentState).getFuture()

    when(waitingTask.getCurrentState()).thenReturn(waitingTaskCurrentState)
    when(waitingTaskCurrentState.getState()).thenReturn(WAITING)
    doReturn(null).when(waitingTaskCurrentState).getFuture()

    when(schedulerSpi.listsTasks()).thenReturn(asList(runningTask, waitingTask))
    underTest = new TaskActivation(schedulerSpi, databaseFreezeService)
  }

  @Test
  void 'pause scheduler and cancel tasks when database is frozen'() {
    underTest.onDatabaseFreezeChangeEvent(new DatabaseFreezeChangeEvent(true))
    verify(schedulerSpi).pause()
    verify(runningTaskFuture).cancel(false)
  }

  @Test
  void 'restart scheduler when database is unfrozen'() {
    underTest.onDatabaseFreezeChangeEvent(new DatabaseFreezeChangeEvent(false))
    verify(schedulerSpi).resume()
  }

  @Test
  void 'scheduler not resumed on startup when database is frozen'() {
    when(databaseFreezeService.isFrozen()).thenReturn(true)
    underTest.start()
    verify(schedulerSpi, times(0)).resume()
  }
}
