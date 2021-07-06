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
package org.sonatype.nexus.repository.search.index;

import org.sonatype.goodies.testsupport.TestSupport;

import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexStartupRebuildManagerTest
    extends TestSupport
{
  private IndexStartupRebuildManager underTest;

  @Mock
  TaskScheduler taskScheduler;

  @Test
  public void rebuildDoesntRunIfNoVariableIsSet() throws Exception {
    underTest = new IndexStartupRebuildManager(taskScheduler, null);
    underTest.doStart();
    verify(taskScheduler, never()).submit(any());
  }

  @Test
  public void rebuildDoesntRunIfVariableIsFalse() throws Exception {
    underTest = new IndexStartupRebuildManager(taskScheduler, "false");
    underTest.doStart();
    verify(taskScheduler, never()).submit(any());
  }

  @Test
  public void rebuildDoesntRunIfVariableIsInvalid() throws Exception {
    underTest = new IndexStartupRebuildManager(taskScheduler, "this is invalid");
    underTest.doStart();
    verify(taskScheduler, never()).submit(any());
  }

  @Test
  public void rebuildDoesRunIfVariableIsTrue() throws Exception {
    underTest = new IndexStartupRebuildManager(taskScheduler, "true");
    when(taskScheduler.createTaskConfigurationInstance(RebuildIndexTaskDescriptor.TYPE_ID))
        .thenReturn(new TaskConfiguration());
    underTest.doStart();

    ArgumentCaptor<TaskConfiguration> taskConfigurationArgumentCaptor =
        ArgumentCaptor.forClass(TaskConfiguration.class);

    verify(taskScheduler).submit(taskConfigurationArgumentCaptor.capture());

    TaskConfiguration taskConfiguration = taskConfigurationArgumentCaptor.getValue();

    MatcherAssert.assertThat(taskConfiguration.getString("repositoryName"), Matchers.is( "*"));
  }
}
