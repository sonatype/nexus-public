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
package org.sonatype.nexus.yum.internal.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.yum.Yum;
import org.sonatype.nexus.yum.YumRegistry;
import org.sonatype.nexus.yum.YumRepository;
import org.sonatype.nexus.yum.internal.RpmScanner;
import org.sonatype.nexus.yum.internal.support.YumNexusTestSupport;
import org.sonatype.scheduling.DefaultScheduledTask;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.TaskState;
import org.sonatype.scheduling.schedules.OnceSchedule;
import org.sonatype.scheduling.schedules.RunNowSchedule;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.yum.internal.task.GenerateMetadataTask.ID;
import static org.sonatype.scheduling.TaskState.RUNNING;

@SuppressWarnings("unchecked")
public class GenerateMetadataTaskSettingsTest
    extends YumNexusTestSupport
{

  private static final String ANOTHER_REPO = "repo2";

  private static final String ANOTHER_VERSION = "version2";

  private static final String VERSION = "version";

  private static final String NO_VERSION = null;

  private static final String REPO = "REPO1";

  private static final String BASE_URL = "http://foo.bla";

  private static final String RPM_URL = BASE_URL + "/content/repositories/" + REPO;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldNotExecuteIfOperateOnSameRepository()
      throws Exception
  {
    GenerateMetadataTask task = task(REPO, NO_VERSION);
    assertFalse(task.allowConcurrentExecution(createMap(scheduledTask(task),
        scheduledTask(REPO, NO_VERSION, RUNNING))));
  }

  @Test
  public void shouldNotExecuteIfOperateOnSameRepositoryAndSameVersion()
      throws Exception
  {
    GenerateMetadataTask task = task(REPO, VERSION);
    assertFalse(task.allowConcurrentExecution(createMap(scheduledTask(task),
        scheduledTask(REPO, VERSION, RUNNING))));
  }

  @Test
  public void shouldExecuteIfOperateOnSameRepositoryAndAnotherVersion()
      throws Exception
  {
    GenerateMetadataTask task = task(REPO, VERSION);
    assertTrue(task.allowConcurrentExecution(createMap(scheduledTask(task),
        scheduledTask(REPO, ANOTHER_VERSION, RUNNING))));
  }

  @Test
  public void shouldExecuteIfOperateOnAnotherRepository()
      throws Exception
  {
    GenerateMetadataTask task = task(REPO, NO_VERSION);
    assertTrue(task.allowConcurrentExecution(createMap(scheduledTask(task),
        scheduledTask(ANOTHER_REPO, NO_VERSION, RUNNING))));
  }

  @Test
  public void shouldSetDefaultsForRepoParams()
      throws Exception
  {
    // given
    GenerateMetadataTask task = new GenerateMetadataTask(
        mock(EventBus.class),
        repoRegistry(),
        mock(YumRegistry.class),
        mock(RpmScanner.class),
        mock(Manager.class),
        mock(CommandLineExecutor.class)
    );
    task.setRpmDir(rpmsDir().getAbsolutePath());
    // when
    task.setDefaults();
    // then
    assertThat(task.getRepoDir(), is(rpmsDir().getAbsoluteFile()));
  }

  @Test
  public void shouldSetDefaultsIfOnlyRepoWasSet()
      throws Exception
  {
    // given
    GenerateMetadataTask task = new GenerateMetadataTask(
        mock(EventBus.class),
        repoRegistry(),
        mock(YumRegistry.class),
        mock(RpmScanner.class),
        mock(Manager.class),
        mock(CommandLineExecutor.class)
    );
    task.setRepositoryId(REPO);
    // when
    task.setDefaults();
    // then
    assertThat(task.getRpmDir(), is(rpmsDir().getAbsolutePath()));
    assertThat(task.getRepoDir(), is(rpmsDir().getAbsoluteFile()));
  }

  @Test
  public void shouldNotExecuteOnRepositoriesThatAreNotRegistered()
      throws Exception
  {
    GenerateMetadataTask task = new GenerateMetadataTask(
        mock(EventBus.class),
        mock(RepositoryRegistry.class),
        mock(YumRegistry.class),
        mock(RpmScanner.class),
        mock(Manager.class),
        mock(CommandLineExecutor.class)
    );
    task.setRepositoryId(REPO);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("enabled 'Yum: Generate Metadata' capability");
    task.doRun();
  }

  @Test
  public void shouldNotExecuteOnNonMavenHostedRepository()
      throws Exception
  {
    RepositoryKind repositoryKind = mock(RepositoryKind.class);
    when(repositoryKind.isFacetAvailable(HostedRepository.class)).thenReturn(false);
    Repository repository = mock(Repository.class);
    when(repository.getRepositoryKind()).thenReturn(repositoryKind);
    Yum yum = mock(Yum.class);
    when(yum.getNexusRepository()).thenReturn(repository);
    YumRegistry yumRegistry = mock(YumRegistry.class);
    when(yumRegistry.isRegistered(REPO)).thenReturn(true);
    when(yumRegistry.get(REPO)).thenReturn(yum);

    GenerateMetadataTask task = new GenerateMetadataTask(
        mock(EventBus.class),
        mock(RepositoryRegistry.class),
        yumRegistry,
        mock(RpmScanner.class),
        mock(Manager.class),
        mock(CommandLineExecutor.class)
    );
    task.setRepositoryId(REPO);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("hosted repositories");
    task.doRun();
  }

  private RepositoryRegistry repoRegistry()
      throws Exception
  {
    final Repository repo = mock(Repository.class);
    when(repo.getId()).thenReturn(REPO);
    when(repo.getLocalUrl()).thenReturn(osIndependentUri(rpmsDir()));
    final RepositoryRegistry repoRegistry = mock(RepositoryRegistry.class);
    when(repoRegistry.getRepository(anyString())).thenReturn(repo);
    return repoRegistry;
  }

  private ScheduledTask<YumRepository> scheduledTask(String repo, String version, TaskState state, Date scheduledAt) {
    MockScheduledTask<YumRepository> scheduledTask = scheduledTask(task(repo, version));
    scheduledTask.setTaskState(state);
    scheduledTask.setSchedule(new OnceSchedule(new Date(scheduledAt.getTime() + 400)));
    return scheduledTask;
  }

  private ScheduledTask<YumRepository> scheduledTask(String repo, String version, TaskState state) {
    return scheduledTask(repo, version, state, new Date());
  }

  private MockScheduledTask<YumRepository> scheduledTask(GenerateMetadataTask task) {
    return new MockScheduledTask<YumRepository>(task);
  }

  private GenerateMetadataTask task(String repo, String version) {
    final YumRegistry yumRegistry = mock(YumRegistry.class);
    when(yumRegistry.maxNumberOfParallelThreads()).thenReturn(YumRegistry.DEFAULT_MAX_NUMBER_PARALLEL_THREADS);

    GenerateMetadataTask task = new GenerateMetadataTask(
        mock(EventBus.class),
        mock(RepositoryRegistry.class),
        yumRegistry,
        mock(RpmScanner.class),
        mock(Manager.class),
        mock(CommandLineExecutor.class)
    )
    {

      @Override
      protected YumRepository doRun()
          throws Exception
      {
        return null;
      }

    };
    task.setRpmDir(rpmsDir().getAbsolutePath());
    task.setRepoDir(rpmsDir());
    task.setRepositoryId(repo);
    task.setVersion(version);
    task.setAddedFiles(null);
    task.setSingleRpmPerDirectory(true);

    return task;
  }

  private Map<String, List<ScheduledTask<?>>> createMap(ScheduledTask<YumRepository>... scheduledTasks) {
    List<ScheduledTask<?>> list = new ArrayList<ScheduledTask<?>>();
    for (ScheduledTask<YumRepository> task : scheduledTasks) {
      list.add(task);
    }
    return createMap(list);
  }

  private Map<String, List<ScheduledTask<?>>> createMap(List<ScheduledTask<?>> yumTaskList) {
    Map<String, List<ScheduledTask<?>>> activeTasks = new HashMap<String, List<ScheduledTask<?>>>();
    activeTasks.put(ID, yumTaskList);
    return activeTasks;
  }

  private static class MockScheduledTask<T>
      extends DefaultScheduledTask<T>
  {

    public MockScheduledTask(Callable<T> callable) {
      super(ID, "", "", null, callable, new RunNowSchedule());
    }

    @Override
    public void setTaskState(TaskState state) {
      super.setTaskState(state);
    }

  }
}
