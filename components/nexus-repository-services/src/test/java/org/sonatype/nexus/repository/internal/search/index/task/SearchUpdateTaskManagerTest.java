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
package org.sonatype.nexus.repository.internal.search.index.task;

import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.index.SearchUpdateService;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SearchUpdateTaskManagerTest
    extends TestSupport
{
  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository repository1;

  @Mock
  private Repository repository2;

  @Mock
  private Repository repository3;

  @Mock
  private PeriodicJobService periodicJobService;

  @Mock
  private SearchUpdateService searchUpdateService;

  private final TaskConfiguration taskConfiguration = new TaskConfiguration();

  private SearchUpdateTaskManager underTest;

  @Before
  public void setup() {
    when(repository1.getName()).thenReturn("repository1");
    when(repository2.getName()).thenReturn("repository2");
    when(repository3.getName()).thenReturn("repository3");
    when(taskScheduler.createTaskConfigurationInstance(any())).thenReturn(taskConfiguration);

    doAnswer(i -> {
      ((Runnable) i.getArgument(0)).run();
      return null;
    }).when(periodicJobService).runOnce(any(), anyInt());

    underTest =
        new SearchUpdateTaskManager(taskScheduler, repositoryManager, searchUpdateService, periodicJobService, true);
  }

  @Test
  public void exceptionDoesNotPreventStartup() {
    when(repositoryManager.browse()).thenThrow(new RuntimeException("exception"));

    try {
      underTest.doStart();
    }
    catch (Exception e) {
      fail("expected startup to catch exceptions");
    }
  }

  @Test
  public void skipProcessingWhenNotEnabled() {
    underTest =
        new SearchUpdateTaskManager(taskScheduler, repositoryManager, searchUpdateService, periodicJobService, false);

    underTest.doStart();

    verifyNoMoreInteractions(repositoryManager);
    verifyNoMoreInteractions(taskScheduler);
  }

  @Test
  public void onStartup_noRepositories() {
    when(repositoryManager.browse()).thenReturn(Collections.emptyList());
    underTest.doStart();
    verifyNoMoreInteractions(taskScheduler);
  }

  @Test
  public void onStartup_noRepositoriesToUpdate() {
    when(searchUpdateService.needsReindex(repository1)).thenReturn(false);
    when(searchUpdateService.needsReindex(repository2)).thenReturn(false);
    when(searchUpdateService.needsReindex(repository3)).thenReturn(false);
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository1, repository2, repository3));
    underTest.doStart();
    verifyNoMoreInteractions(taskScheduler);
  }

  @Test
  public void onStartup_updateOneRepository() {
    when(searchUpdateService.needsReindex(repository1)).thenReturn(false);
    when(searchUpdateService.needsReindex(repository2)).thenReturn(true);
    when(searchUpdateService.needsReindex(repository3)).thenReturn(false);
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository1, repository2, repository3));
    underTest.doStart();
    assertEquals("repository2", taskConfiguration.getString("repositoryNames"));
    verify(taskScheduler).submit(taskConfiguration);
  }

  @Test
  public void onStartup_updateMultipleRepositories() {
    when(searchUpdateService.needsReindex(repository1)).thenReturn(true);
    when(searchUpdateService.needsReindex(repository2)).thenReturn(true);
    when(searchUpdateService.needsReindex(repository3)).thenReturn(false);
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository1, repository2, repository3));
    underTest.doStart();
    assertEquals("repository1,repository2", taskConfiguration.getString("repositoryNames"));
    verify(taskScheduler).submit(taskConfiguration);
  }

  @Test
  public void onStartup_taskAlreadyRunning() {
    when(searchUpdateService.needsReindex(repository1)).thenReturn(true);
    when(searchUpdateService.needsReindex(repository2)).thenReturn(true);
    when(searchUpdateService.needsReindex(repository3)).thenReturn(false);
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository1, repository2, repository3));
    when(taskScheduler.findAndSubmit(any())).thenReturn(true);
    underTest.doStart();
    verify(taskScheduler, never()).submit(any());
  }
}
