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

import java.util.concurrent.Future;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.stateguard.StateGuardModule;
import org.sonatype.nexus.scheduling.CurrentState;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.scheduling.TaskState.RUNNING;
import static org.sonatype.nexus.scheduling.TaskState.WAITING;

public class TaskActivationTest
    extends TestSupport
{
  @Mock
  private SchedulerSPI schedulerSpi;

  @Mock
  private TaskInfo runningTask;

  @Mock
  private TaskInfo waitingTask;

  @Mock
  private CurrentState runningTaskCurrentState;

  @Mock
  private CurrentState waitingTaskCurrentState;

  @Mock
  private Future<?> runningTaskFuture;

  private TaskActivation underTest;

  @Before
  public void setUp() throws Exception {
    when(runningTask.getCurrentState()).thenReturn(runningTaskCurrentState);
    when(runningTaskCurrentState.getState()).thenReturn(RUNNING);
    doReturn(runningTaskFuture).when(runningTaskCurrentState).getFuture();

    when(waitingTask.getCurrentState()).thenReturn(waitingTaskCurrentState);
    when(waitingTaskCurrentState.getState()).thenReturn(WAITING);
    doReturn(null).when(waitingTaskCurrentState).getFuture();

    when(schedulerSpi.listsTasks()).thenReturn(asList(runningTask, waitingTask));
    underTest = Guice.createInjector(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(SchedulerSPI.class).toInstance(schedulerSpi);
          }
        }, new StateGuardModule()).getInstance(TaskActivation.class);
    underTest.start();
  }

  /*
   * pause scheduler and cancel tasks on freeze
   */
  @Test
  public void testPauseScheduler_cancelTasksOnFreeze() {
    InOrder inOrder = inOrder(schedulerSpi, runningTaskFuture);

    underTest.freeze();
    inOrder.verify(schedulerSpi).pause();
    inOrder.verify(runningTaskFuture).cancel(false);

    inOrder.verifyNoMoreInteractions();
  }

  /*
   * restart scheduler when database on unfreeze
   */
  @Test
  public void testRestartScheduler_onUnfreeze() throws Exception {
    InOrder inOrder = inOrder(schedulerSpi, runningTaskFuture);

    underTest.stop();
    inOrder.verify(schedulerSpi).pause();
    underTest.freeze();
    underTest.start();
    inOrder.verify(schedulerSpi, times(0)).resume();
    underTest.unfreeze();
    inOrder.verify(schedulerSpi).resume();

    inOrder.verifyNoMoreInteractions();
  }

  /*
   * scheduler not resumed on startup if frozen
   */
  @Test
  public void testSchedulerNotResumed_onFrozenStartup() throws Exception {
    InOrder inOrder = inOrder(schedulerSpi, runningTaskFuture);

    underTest.stop();
    inOrder.verify(schedulerSpi).pause();
    underTest.freeze();
    underTest.start();

    inOrder.verifyNoMoreInteractions();
  }
}
