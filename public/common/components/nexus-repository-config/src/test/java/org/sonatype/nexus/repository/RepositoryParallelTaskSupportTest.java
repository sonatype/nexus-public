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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID;

@ExtendWith(MockitoExtension.class)
class RepositoryParallelTaskSupportTest
{
  @Mock
  RepositoryManager repositoryManager;

  @Mock
  SecurityManager securityManager;

  TaskConfiguration task = new TaskConfiguration();

  final Map<Repository, Stream<Runnable>> runnables = new HashMap<>();

  final TestTask undertest = new TestTask();

  @BeforeEach
  void setup() {
    ThreadContext.bind(securityManager);

    undertest.install(repositoryManager, new GroupType());
    task.setTypeId("typeId");
    task.setId("id");
  }

  @AfterEach
  void tearDown() {
    ThreadContext.unbindSecurityManager();
  }

  @Test
  void testWildcard() throws Exception {
    task.setString(REPOSITORY_NAME_FIELD_ID, "*");

    List<AtomicInteger> counters = run("one", "two");

    assertThat(counters.get(0).get(), is(100));
    assertThat(counters.get(1).get(), is(100));
  }

  @Test
  void testOnePlusWildcard() throws Exception {
    task.setString(REPOSITORY_NAME_FIELD_ID, "one,*");

    List<AtomicInteger> counters = run("one", "two");

    assertThat(counters.get(0).get(), is(100));
    assertThat(counters.get(1).get(), is(100));
  }

  @Test
  void testOne() throws Exception {
    task.setString(REPOSITORY_NAME_FIELD_ID, "one,three");

    List<AtomicInteger> counters = run("one", "two");

    assertThat(counters.get(0).get(), is(100));
    assertThat(counters.get(1).get(), is(0));
  }

  private List<AtomicInteger> run(final String... names) throws Exception {
    List<Repository> repositories = mockRepositories(names);
    List<AtomicInteger> counters = Stream.of(names)
        .map(__ -> new AtomicInteger())
        .toList();

    for (int i = 0; i < repositories.size(); i++) {
      int count = i;
      Runnable runnable = () -> counters.get(count).incrementAndGet();
      runnables.put(repositories.get(i), Stream.generate(() -> runnable).limit(100));
    }

    undertest.configure(task);

    undertest.call();

    return counters;
  }

  private List<Repository> mockRepositories(final String... names) {
    List<Repository> repositories = Stream.of(names)
        .map(name -> {
          Repository repository = mock(Repository.class);
          lenient().when(repository.getName()).thenReturn(name);
          lenient().when(repositoryManager.get(name)).thenReturn(repository);
          return repository;
        })
        .toList();

    lenient().when(repositoryManager.browse()).thenReturn(repositories);

    return repositories;
  }

  private class TestTask
      extends RepositoryParallelTaskSupport
  {
    private TestTask() {
      super(2);
    }

    @Override
    public String getMessage() {
      return "Testing";
    }

    @Override
    protected Stream<Runnable> jobStream(final ProgressLogIntervalHelper progress, final Repository repository) {
      return runnables.get(repository);
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
      return true;
    }

    @Override
    protected Object result() {
      return null;
    }
  }
}
