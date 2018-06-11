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
package org.sonatype.nexus.repository

import org.sonatype.goodies.common.MultipleFailures.MultipleFailuresException
import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInterruptedException

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class RepositoryTaskSupportTest
    extends TestSupport
{
  @Rule
  public final ExpectedException thrown = ExpectedException.none()

  @Mock
  private RepositoryManager repositoryManager

  private TaskConfiguration configuration

  private TestTask task

  /**
   * Verify that repository field must be present in configuration.
   */
  @Test
  void 'repository field must be present'() {
    configuration = new TaskConfiguration(id: 'test', typeId: 'test')
    task = new TestTask()
    task.install(repositoryManager, new GroupType())
    task.configure(configuration)

    thrown.expect(IllegalArgumentException)
    task.execute()
  }

  /**
   * Verify a TaskInterruptedException Exception is thrown when a repository associated with a task
   * is null. This simulates when a repository (that was the configured target of a task) has been deleted.
   */
  @Test
  void 'repository exists'() {
    configuration = new TaskConfiguration(id: 'test', typeId: 'test')
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, 'foo')
    when(repositoryManager.get('foo')).thenReturn(null)
    task = new TestTask()
    task.install(repositoryManager, new GroupType())
    task.configure(configuration)

    thrown.expect(TaskInterruptedException)
    task.execute()
  }

  /**
   * Verify that configured repository satisfies task repository filter (appliesTo).
   */
  @Test
  void 'repository satisfies filter'() {
    configuration = new TaskConfiguration(id: 'test', typeId: 'test')
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, 'foo')
    when(repositoryManager.get('foo')).thenReturn(mock(Repository))
    task = new TestTask() {
      @Override
      protected boolean appliesTo(final Repository repository) {
        return false
      }
    }
    task.install(repositoryManager, new GroupType())
    task.configure(configuration)

    thrown.expect(IllegalStateException)
    task.execute()
  }

  /**
   * Verify that task is executed for repository.
   */
  @Test
  void 'task is executed for repository'() {
    configuration = new TaskConfiguration(id: 'test', typeId: 'test')
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, 'foo')
    def testRepository = mock(Repository)
    when(repositoryManager.get('foo')).thenReturn(testRepository)
    def actualRepository = null
    task = new TestTask() {
      @Override
      protected void execute(final Repository repository) {
        assert testRepository == repository
        actualRepository = repository
      }
    }
    task.install(repositoryManager, new GroupType())
    task.configure(configuration)

    task.execute()
    assert actualRepository != null
  }

  /**
   * Verify that task is executed for all repositories.
   */
  @Test
  void 'task is executed for all repositories'() {
    configuration = new TaskConfiguration(id: 'test', typeId: 'test')
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, '*')
    def testRepository1 = mock(Repository)
    def testRepository2 = mock(Repository)
    when(repositoryManager.browse()).thenReturn([testRepository1, testRepository2])
    def actualRepositories = []
    task = new TestTask() {
      @Override
      protected void execute(final Repository repository) {
        actualRepositories << repository
      }
    }
    task.install(repositoryManager, new GroupType())
    task.configure(configuration)

    task.execute()
    assert [testRepository1, testRepository2] == actualRepositories
  }

  /**
   * Verify that task is executed for repositories that satisfy filter (appliesTo).
   */
  @Test
  void 'task is executed for filtered repositories'() {
    configuration = new TaskConfiguration(id: 'test', typeId: 'test')
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, '*')
    def testRepository1 = mock(Repository)
    def testRepository2 = mock(Repository)
    when(repositoryManager.browse()).thenReturn([testRepository1, testRepository2])
    def actualRepositories = []
    task = new TestTask() {
      @Override
      protected void execute(final Repository repository) {
        actualRepositories << repository
      }

      @Override
      protected boolean appliesTo(final Repository repository) {
        return repository != testRepository1
      }
    }
    task.install(repositoryManager, new GroupType())
    task.configure(configuration)

    task.execute()
    assert [testRepository2] == actualRepositories
  }

  /**
   * Verify that task is executed for all repositories regardless exception.
   */
  @Test
  void 'task is executed for all repositories regardless exception'() {
    configuration = new TaskConfiguration(id: 'test', typeId: 'test')
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, '*')
    def testRepository1 = mock(Repository)
    def testRepository2 = mock(Repository)
    when(repositoryManager.browse()).thenReturn([testRepository1, testRepository2])
    def actualRepositories = []
    task = new TestTask() {
      @Override
      protected void execute(final Repository repository) {
        actualRepositories << repository
        if (testRepository1 == repository) {
          throw new Exception()
        }
      }
    }
    task.install(repositoryManager, new GroupType())
    task.configure(configuration)

    thrown.expect(MultipleFailuresException)
    task.execute()
    assert [testRepository1, testRepository2] == actualRepositories
  }

  /**
   * Verify that task stops execution once cancelled.
   */
  @Test
  void 'task stops if cancelled'() {
    configuration = new TaskConfiguration(id: 'test', typeId: 'test')
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, '*')
    def testRepository1 = mock(Repository)
    def testRepository2 = mock(Repository)
    when(repositoryManager.browse()).thenReturn([testRepository1, testRepository2])
    def actualRepositories = []
    task = new TestTask() {
      @Override
      protected void execute(final Repository repository) {
        actualRepositories << repository
        if (testRepository1 == repository) {
          cancel()
        }
      }
    }
    task.install(repositoryManager, new GroupType())
    task.configure(configuration)

    task.execute()
    assert [testRepository1] == actualRepositories
  }

  private class TestTask
      extends RepositoryTaskSupport
  {

    @Override
    protected void execute(final Repository repository) {

    }

    @Override
    protected boolean appliesTo(final Repository repository) {
      return true
    }

    @Override
    String getMessage() {
      return 'Test task'
    }
  }
}
