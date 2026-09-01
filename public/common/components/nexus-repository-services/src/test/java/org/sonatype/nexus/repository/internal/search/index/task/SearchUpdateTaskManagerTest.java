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

import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.index.SearchUpdateService;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import com.google.common.collect.ImmutableList;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SearchUpdateTaskManagerTest
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

  private ListAppender<ILoggingEvent> logCaptor;

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

    logCaptor = new ListAppender<>();
    logCaptor.start();
    ((Logger) LoggerFactory.getLogger(SearchUpdateTaskManager.class)).addAppender(logCaptor);
  }

  @After
  public void tearDown() {
    ((Logger) LoggerFactory.getLogger(SearchUpdateTaskManager.class)).detachAppender(logCaptor);
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

  @Test
  public void onStartup_submit_throwsValidationError_doesNotLogError() {
    when(searchUpdateService.needsReindex(repository1)).thenReturn(true);
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository1));
    when(taskScheduler.findAndSubmit(any())).thenReturn(false);
    when(taskScheduler.submit(any()))
        .thenThrow(new ValidationErrorsException("Task repository.search.update already exists, ignoring"));

    // Must not throw — startup should survive the ValidationErrorsException
    try {
      underTest.doStart();
    }
    catch (Exception e) {
      fail("Expected ValidationErrorsException to be swallowed gracefully, but got: " + e);
    }

    // submit() was called — it just threw because another node won the race
    verify(taskScheduler).submit(any());
  }

  @Test
  public void onStartup_missingFacetException_skipsRepoAndContinues() {
    // Construct the exception before the stubbing call: MissingFacetException's constructor invokes
    // repository1.getName(), and calling a mock method inside an in-progress when() would trip
    // Mockito's "unfinished stubbing" detection.
    MissingFacetException missingFacet = new MissingFacetException(repository1, Facet.class);
    when(searchUpdateService.needsReindex(repository1)).thenThrow(missingFacet);
    when(searchUpdateService.needsReindex(repository2)).thenReturn(true);
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository1, repository2));

    underTest.doStart();

    // repository1 was skipped; repository2 was collected and the task scheduled
    assertEquals("repository2", taskConfiguration.getString("repositoryNames"));
    verify(taskScheduler).submit(taskConfiguration);

    // The skipped repo must be logged at WARN, and never at ERROR.
    assertThat("expected a WARN for the concurrently-removed repository",
        logCaptor.list.stream()
            .anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains("Skipping search index check for repository")),
        is(true));
    assertThat("must not log ERROR for repository1",
        logCaptor.list.stream()
            .noneMatch(e -> e.getLevel() == Level.ERROR
                && e.getFormattedMessage().contains("repository1")),
        is(true));
  }

  @Test
  public void onStartup_invalidStateException_skipsRepoAndContinues() {
    when(searchUpdateService.needsReindex(repository1))
        .thenThrow(new InvalidStateException("DELETED", new String[]{}));
    when(searchUpdateService.needsReindex(repository2)).thenReturn(true);
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository1, repository2));

    underTest.doStart();

    // repository1 was skipped; repository2 was collected and the task scheduled
    assertEquals("repository2", taskConfiguration.getString("repositoryNames"));
    verify(taskScheduler).submit(taskConfiguration);

    // The skipped repo must be logged at WARN, and never at ERROR.
    assertThat("expected a WARN for the concurrently-removed repository",
        logCaptor.list.stream()
            .anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains("Skipping search index check for repository")),
        is(true));
    assertThat("must not log ERROR for repository1",
        logCaptor.list.stream()
            .noneMatch(e -> e.getLevel() == Level.ERROR
                && e.getFormattedMessage().contains("repository1")),
        is(true));
  }
}
