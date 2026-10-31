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
package org.sonatype.nexus.repository;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.common.failure.MultipleFailures.MultipleFailuresException;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RepositoryTaskSupportTest
{
  @Mock
  private RepositoryManager repositoryManager;

  private TaskConfiguration configuration;

  private TestTask task;

  private ListAppender<ILoggingEvent> logCaptor;

  @Before
  public void startLogCapture() {
    logCaptor = new ListAppender<>();
    logCaptor.start();
    // ComponentSupport derives its logger from getClass(), so the running task logs under the
    // concrete TestTask subclass name, not RepositoryTaskSupport. Attach to the root logger so the
    // capture is independent of the concrete subclass (including the anonymous ones used below).
    ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(logCaptor);
  }

  @After
  public void stopLogCapture() {
    ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).detachAppender(logCaptor);
  }

  /*
   * Verify that repository field must be present in configuration.
   */
  @Test
  public void testRepositoryFieldMustBePresent() {
    configuration = task("test", "test");
    task = new TestTask();
    task.install(repositoryManager, new GroupType());
    task.configure(configuration);

    assertThrows(IllegalArgumentException.class, task::execute);
  }

  /*
   * Verify a TaskInterruptedException Exception is thrown when a repository associated with a task
   * is null. This simulates when a repository (that was the configured target of a task) has been deleted.
   */
  @Test
  public void testRepository_exists() {
    configuration = task("test", "test");
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "foo");
    when(repositoryManager.get("foo")).thenReturn(null);
    task = new TestTask();
    task.install(repositoryManager, new GroupType());
    task.configure(configuration);

    assertThrows(TaskInterruptedException.class, task::execute);
  }

  /*
   * Verify that configured repository satisfies task repository filter (appliesTo).
   */
  @Test
  public void testRepositorySatisifiesFilter() {
    configuration = task("test", "test");
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "foo");
    when(repositoryManager.get("foo")).thenReturn(mock(Repository.class));
    task = new TestTask()
    {
      @Override
      protected boolean appliesTo(final Repository repository) {
        return false;
      }
    };
    task.install(repositoryManager, new GroupType());
    task.configure(configuration);

    assertThrows(IllegalStateException.class, task::execute);
  }

  /*
   * Verify that task is executed for repository.
   */
  @Test
  public void testTaskIsExecutedForRepository() throws Exception {
    configuration = task("test", "test");
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "foo");
    Repository testRepository = mock(Repository.class);
    when(repositoryManager.get("foo")).thenReturn(testRepository);

    Repository[] actualRepository = new Repository[1];
    task = new TestTask()
    {
      @Override
      protected void execute(final Repository repository) {
        assertThat(testRepository, is(repository));
        actualRepository[0] = repository;
      }
    };
    task.install(repositoryManager, new GroupType());
    task.configure(configuration);

    task.execute();
    assertThat(actualRepository[0], notNullValue());
  }

  /*
   * Verify that task is executed for all repositories.
   */
  @Test
  public void testTaskIsExecutedForAllRepositories() throws Exception {
    configuration = task("test", "test");
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "*");
    Repository testRepository1 = mock(Repository.class);
    Repository testRepository2 = mock(Repository.class);
    when(repositoryManager.browse()).thenReturn(List.of(testRepository1, testRepository2));
    List<Repository> actualRepositories = new ArrayList<>();
    task = new TestTask()
    {
      @Override
      protected void execute(final Repository repository) {
        actualRepositories.add(repository);
      }
    };
    task.install(repositoryManager, new GroupType());
    task.configure(configuration);

    task.execute();
    assertThat(actualRepositories, contains(testRepository1, testRepository2));
  }

  /*
   * Verify that task is executed for repositories that satisfy filter (appliesTo).
   */
  @Test
  public void testTaskIsExecutedForFilteredRepositories() throws Exception {
    configuration = task("test", "test");
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "*");
    Repository testRepository1 = mock(Repository.class);
    Repository testRepository2 = mock(Repository.class);
    when(repositoryManager.browse()).thenReturn(List.of(testRepository1, testRepository2));
    List<Repository> actualRepositories = new ArrayList<>();
    task = new TestTask()
    {
      @Override
      protected void execute(final Repository repository) {
        actualRepositories.add(repository);
      }

      @Override
      protected boolean appliesTo(final Repository repository) {
        return repository != testRepository1;
      }
    };
    task.install(repositoryManager, new GroupType());
    task.configure(configuration);

    task.execute();
    assertThat(actualRepositories, contains(testRepository2));
  }

  /*
   * Verify that task is executed for all repositories regardless exception.
   */
  @Test
  public void testTaskIsExecutedForALlRepositoriesRegardlessException() {
    configuration = task("test", "test");
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "*");
    Repository testRepository1 = mock(Repository.class);
    Repository testRepository2 = mock(Repository.class);
    when(repositoryManager.browse()).thenReturn(List.of(testRepository1, testRepository2));
    List<Repository> actualRepositories = new ArrayList<>();
    task = new TestTask()
    {
      @Override
      protected void execute(final Repository repository) {
        actualRepositories.add(repository);
        if (testRepository1 == repository) {
          throw new RuntimeException();
        }
      }
    };
    task.install(repositoryManager, new GroupType());
    task.configure(configuration);

    assertThrows(MultipleFailuresException.class, task::execute);
    assertThat(actualRepositories, contains(testRepository1, testRepository2));

    // A generic RuntimeException (not a concurrent-deletion signal) must still land in the ERROR
    // / failure branch — guards against the multi-catch being widened too far in future.
    assertThat("a plain RuntimeException must be logged at ERROR",
        logCaptor.list.stream()
            .anyMatch(e -> e.getLevel() == Level.ERROR
                && e.getFormattedMessage().contains("Failed to run task")),
        is(true));
  }

  /*
   * Verify that when execute(Repository) throws MissingFacetException (e.g. concurrent repo deletion),
   * the task logs a warning, skips that repository, continues to the next, and does NOT throw
   * (no task failure). NEXUS-53351.
   */
  @Test
  public void testMissingFacetExceptionSkipsRepositoryWithoutFailure() throws Exception {
    configuration = task("test", "test");
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "*");
    Repository testRepository1 = mock(Repository.class);
    Repository testRepository2 = mock(Repository.class);
    when(repositoryManager.browse()).thenReturn(List.of(testRepository1, testRepository2));
    List<Repository> actualRepositories = new ArrayList<>();
    task = new TestTask()
    {
      @Override
      protected void execute(final Repository repository) {
        actualRepositories.add(repository);
        if (testRepository1 == repository) {
          throw new MissingFacetException(repository, Facet.class);
        }
      }
    };
    task.install(repositoryManager, new GroupType());
    task.configure(configuration);

    // Should not throw — MissingFacetException must be swallowed with a WARN
    task.execute();
    assertThat(actualRepositories, contains(testRepository1, testRepository2));

    // The skipped repository must be logged at WARN, never ERROR...
    assertThat("expected a WARN log for the concurrently-removed repository",
        logCaptor.list.stream()
            .anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains("as it appears to have been removed during execution")),
        is(true));
    // ...and no ERROR must be logged, which also proves the exception was NOT accumulated into
    // MultipleFailures (the failures.add(e) path is the only ERROR-logging branch).
    assertThat("MissingFacetException must not reach the ERROR/failure branch",
        logCaptor.list.stream()
            .noneMatch(e -> e.getLevel() == Level.ERROR
                && e.getFormattedMessage().contains("Failed to run task")),
        is(true));
  }

  /*
   * Verify that when execute(Repository) throws InvalidStateException (e.g. @Guarded method on a
   * concurrently-deleted repo), the task logs a warning, skips that repository, continues to the
   * next, and does NOT throw (no task failure). NEXUS-53351.
   */
  @Test
  public void testInvalidStateExceptionSkipsRepositoryWithoutFailure() throws Exception {
    configuration = task("test", "test");
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "*");
    Repository testRepository1 = mock(Repository.class);
    Repository testRepository2 = mock(Repository.class);
    when(repositoryManager.browse()).thenReturn(List.of(testRepository1, testRepository2));
    List<Repository> actualRepositories = new ArrayList<>();
    task = new TestTask()
    {
      @Override
      protected void execute(final Repository repository) {
        actualRepositories.add(repository);
        if (testRepository1 == repository) {
          throw new InvalidStateException("STOPPED", new String[]{"STARTED"});
        }
      }
    };
    task.install(repositoryManager, new GroupType());
    task.configure(configuration);

    // Should not throw — InvalidStateException must be swallowed with a WARN
    task.execute();
    assertThat(actualRepositories, contains(testRepository1, testRepository2));

    // The skipped repository must be logged at WARN, never ERROR...
    assertThat("expected a WARN log for the concurrently-removed repository",
        logCaptor.list.stream()
            .anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains("as it appears to have been removed during execution")),
        is(true));
    // ...and no ERROR must be logged, which also proves the exception was NOT accumulated into
    // MultipleFailures (the failures.add(e) path is the only ERROR-logging branch).
    assertThat("InvalidStateException must not reach the ERROR/failure branch",
        logCaptor.list.stream()
            .noneMatch(e -> e.getLevel() == Level.ERROR
                && e.getFormattedMessage().contains("Failed to run task")),
        is(true));
  }

  /*
   * Verify that TaskInterruptedException still propagates past the new
   * MissingFacetException|InvalidStateException catch (correct catch ordering). NEXUS-53351.
   */
  @Test
  public void testTaskInterruptedExceptionPropagates() {
    configuration = task("test", "test");
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "*");
    Repository testRepository1 = mock(Repository.class);
    when(repositoryManager.browse()).thenReturn(List.of(testRepository1));
    task = new TestTask()
    {
      @Override
      protected void execute(final Repository repository) {
        throw new TaskInterruptedException("interrupted", true);
      }
    };
    task.install(repositoryManager, new GroupType());
    task.configure(configuration);

    assertThrows(TaskInterruptedException.class, task::execute);
    // It must NOT have been swallowed as a concurrent-deletion WARN.
    assertThat(logCaptor.list.stream()
        .noneMatch(e -> e.getLevel() == Level.WARN
            && e.getFormattedMessage().contains("as it appears to have been removed during execution")),
        is(true));
  }

  /*
   * Verify that task stops execution once cancelled.
   */
  @Test
  public void testTaskStopsIfCancelled() throws Exception {
    configuration = task("test", "test");
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "*");
    Repository testRepository1 = mock(Repository.class);
    Repository testRepository2 = mock(Repository.class);
    when(repositoryManager.browse()).thenReturn(List.of(testRepository1, testRepository2));
    List<Repository> actualRepositories = new ArrayList<>();
    task = new TestTask()
    {
      @Override
      protected void execute(final Repository repository) {
        actualRepositories.add(repository);
        if (testRepository1 == repository) {
          cancel();
        }
      }
    };
    task.install(repositoryManager, new GroupType());
    task.configure(configuration);

    task.execute();
    assertThat(actualRepositories, contains(testRepository1));
  }

  private static TaskConfiguration task(final String id, final String typeId) {
    TaskConfiguration task = new TaskConfiguration();
    task.setId(id);
    task.setTypeId(typeId);
    return task;
  }

  private static class TestTask
      extends RepositoryTaskSupport
  {

    @Override
    protected void execute(final Repository repository) {
      // noop
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
      return true;
    }

    @Override
    public String getMessage() {
      return "Test task";
    }
  }
}
