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

import org.sonatype.goodies.common.MultipleFailures.MultipleFailuresException;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RepositoryTaskSupportTest
    extends TestSupport
{
  @Mock
  private RepositoryManager repositoryManager;

  private TaskConfiguration configuration;

  private TestTask task;

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
