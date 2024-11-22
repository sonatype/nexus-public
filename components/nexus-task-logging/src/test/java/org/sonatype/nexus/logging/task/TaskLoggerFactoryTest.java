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
package org.sonatype.nexus.logging.task;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;
import org.slf4j.Logger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonatype.nexus.logging.task.TaskLogType.BOTH;
import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;
import static org.sonatype.nexus.logging.task.TaskLogType.REPLICATION_LOGGING;
import static org.sonatype.nexus.logging.task.TaskLogType.TASK_LOG_ONLY;
import static org.sonatype.nexus.logging.task.TaskLogType.TASK_LOG_ONLY_WITH_PROGRESS;

public class TaskLoggerFactoryTest
    extends TestSupport
{
  @Test
  public void testBoth() {
    TaskLogger taskLogger = TaskLoggerFactory.create(new Both(), mock(Logger.class), mock(TaskLogInfo.class));
    assertThat(taskLogger, instanceOf(SeparateTaskLogTaskLogger.class));
  }

  @Test
  public void testTaskLogOnly() {
    TaskLogger taskLogger = TaskLoggerFactory.create(new TaskLogOnly(), mock(Logger.class), mock(TaskLogInfo.class));
    assertThat(taskLogger, instanceOf(TaskLogOnlyTaskLogger.class));
  }

  @Test
  public void testReplicationLogging() {
    TaskLogger taskLogger =
        TaskLoggerFactory.create(new ReplicationLogging(), mock(Logger.class), mock(TaskLogInfo.class));
    assertThat(taskLogger, instanceOf(ReplicationTaskLogger.class));
  }

  @Test
  public void testTaskLogWithProgress() {
    TaskLogger taskLogger =
        TaskLoggerFactory.create(new TaskLogWithProgress(), mock(Logger.class), mock(TaskLogInfo.class));
    assertThat(taskLogger, instanceOf(TaskLogWithProgressLogger.class));
  }

  @Test
  public void testNexusLogOnly() {
    TaskLogger taskLogger = TaskLoggerFactory.create(new NexusLogOnly(), mock(Logger.class), mock(TaskLogInfo.class));
    assertThat(taskLogger, instanceOf(ProgressTaskLogger.class));
  }

  @Test
  public void testDefault() {
    TaskLogger taskLogger = TaskLoggerFactory.create(new Object(), mock(Logger.class), mock(TaskLogInfo.class));
    assertThat(taskLogger, instanceOf(SeparateTaskLogTaskLogger.class));
  }

  @TaskLogging(BOTH)
  private static final class Both
  {
  }

  @TaskLogging(TASK_LOG_ONLY)
  private static final class TaskLogOnly
  {
  }

  @TaskLogging(REPLICATION_LOGGING)
  private static final class ReplicationLogging
  {
  }

  @TaskLogging(TASK_LOG_ONLY_WITH_PROGRESS)
  private static final class TaskLogWithProgress
  {
  }

  @TaskLogging(NEXUS_LOG_ONLY)
  private static final class NexusLogOnly
  {
  }
}
