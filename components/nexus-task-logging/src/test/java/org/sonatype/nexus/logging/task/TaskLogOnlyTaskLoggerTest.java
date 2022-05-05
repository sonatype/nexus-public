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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.MDC;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.logging.task.SeparateTaskLogTaskLogger.TASK_LOG_LOCATION_PREFIX;
import static org.sonatype.nexus.logging.task.TaskLogger.TASK_LOG_ONLY_MDC;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.NEXUS_LOG_ONLY;

public class TaskLogOnlyTaskLoggerTest
    extends TestSupport
{
  @Mock
  private Logger log;

  @Mock
  private TaskLogInfo taskLogInfo;

  @InjectMocks
  private TaskLogOnlyTaskLogger taskLogOnlyTaskLogger;

  @Test
  public void mdcShouldContainTaskLogOnlyKey() {

    assertThat(MDC.get(TASK_LOG_ONLY_MDC), equalTo("true"));
  }

  @Test
  public void shouldWriteLogLocationToNexusLog() {
    taskLogOnlyTaskLogger.writeLogFileNameToNexusLog();

    verify(log).info(eq(NEXUS_LOG_ONLY), startsWith(TASK_LOG_LOCATION_PREFIX));
    assertThat(MDC.get(TASK_LOG_ONLY_MDC), equalTo("true"));
  }
}
