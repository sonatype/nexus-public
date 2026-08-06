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
package org.sonatype.nexus.repository.content.browse;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.upgrade.datastore.UpgradeContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RebuildBrowseNodesManagerTest
{
  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private PeriodicJobService periodicJobService;

  @Mock
  private UpgradeContext upgradeContext;

  private RebuildBrowseNodesManager underTest;

  @Before
  public void setUp() {
    underTest = new RebuildBrowseNodesManager(taskScheduler, repositoryManager, periodicJobService, upgradeContext);
  }

  private void rebuildOnStart(final boolean value) {
    when(upgradeContext.isFlagSet(RebuildBrowseNodesManager.REBUILD_BROWSE_NODES_ON_START)).thenReturn(value);
  }

  @Test
  public void testConstruction() {
    assertThat(underTest, is(notNullValue()));
  }

  @Test(expected = NullPointerException.class)
  public void testNullTaskSchedulerRejected() {
    new RebuildBrowseNodesManager(null, repositoryManager, periodicJobService, upgradeContext);
  }

  @Test(expected = NullPointerException.class)
  public void testNullRepositoryManagerRejected() {
    new RebuildBrowseNodesManager(taskScheduler, null, periodicJobService, upgradeContext);
  }

  @Test(expected = NullPointerException.class)
  public void testNullPeriodicJobServiceRejected() {
    new RebuildBrowseNodesManager(taskScheduler, repositoryManager, null, upgradeContext);
  }

  @Test(expected = NullPointerException.class)
  public void testNullUpgradeContextRejected() {
    new RebuildBrowseNodesManager(taskScheduler, repositoryManager, periodicJobService, null);
  }

  @Test
  public void doStart_doesNothingWhenRebuildOnStartIsFalse() throws Exception {
    rebuildOnStart(false);

    underTest.start();

    verifyNoInteractions(periodicJobService);
    verifyNoInteractions(repositoryManager);
    verifyNoInteractions(taskScheduler);
  }

  @Test
  public void doStart_schedulesRunOnceWhenRebuildOnStartIsTrue() throws Exception {
    rebuildOnStart(true);

    underTest.start();

    verify(periodicJobService).runOnce(any(Runnable.class), eq(0));
  }

  @Test
  public void maybeRebuild_submitsTaskWhenRepositoriesHaveAssetsAndNoExistingTask() throws Exception {
    Repository repo1 = createRepositoryWithAssets("repo1", 5);
    Repository repo2 = createRepositoryWithAssets("repo2", 3);
    when(repositoryManager.browse()).thenReturn(Arrays.asList(repo1, repo2));

    when(taskScheduler.findWaitingTask(anyString(), any(Map.class))).thenReturn(false);

    TaskConfiguration taskConfig = mock(TaskConfiguration.class);
    when(taskScheduler.createTaskConfigurationInstance(anyString())).thenReturn(taskConfig);

    // Capture the Runnable passed to runOnce and execute it
    rebuildOnStart(true);
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    underTest.start();
    verify(periodicJobService).runOnce(runnableCaptor.capture(), eq(0));

    runnableCaptor.getValue().run();

    // Verify task was created and submitted
    verify(taskScheduler).createTaskConfigurationInstance("create.browse.nodes");
    verify(taskConfig).setString(eq("repositoryName"), eq("repo1,repo2"));

    ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
    verify(taskConfig).setName(nameCaptor.capture());
    assertThat(nameCaptor.getValue(), containsString("repo1,repo2"));

    verify(taskScheduler).submit(taskConfig);
  }

  @Test
  public void maybeRebuild_skipsRepositoriesWithNoAssets() throws Exception {
    Repository repoWithAssets = createRepositoryWithAssets("has-assets", 1);
    Repository repoWithoutAssets = createRepositoryWithAssets("no-assets", 0);
    when(repositoryManager.browse()).thenReturn(Arrays.asList(repoWithAssets, repoWithoutAssets));

    when(taskScheduler.findWaitingTask(anyString(), any(Map.class))).thenReturn(false);

    TaskConfiguration taskConfig = mock(TaskConfiguration.class);
    when(taskScheduler.createTaskConfigurationInstance(anyString())).thenReturn(taskConfig);

    rebuildOnStart(true);
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    underTest.start();
    verify(periodicJobService).runOnce(runnableCaptor.capture(), eq(0));

    runnableCaptor.getValue().run();

    // Only the repository with assets should be included
    verify(taskConfig).setString(eq("repositoryName"), eq("has-assets"));
    verify(taskScheduler).submit(taskConfig);
  }

  @Test
  public void maybeRebuild_doesNotSubmitTaskWhenAllRepositoriesEmpty() throws Exception {
    Repository emptyRepo = createRepositoryWithAssets("empty", 0);
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(emptyRepo));

    rebuildOnStart(true);
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    underTest.start();
    verify(periodicJobService).runOnce(runnableCaptor.capture(), eq(0));

    runnableCaptor.getValue().run();

    // No task should be created or submitted
    verify(taskScheduler, never()).createTaskConfigurationInstance(anyString());
    verify(taskScheduler, never()).submit(any(TaskConfiguration.class));
  }

  @Test
  public void maybeRebuild_doesNotSubmitTaskWhenNoRepositories() throws Exception {
    when(repositoryManager.browse()).thenReturn(Collections.emptyList());

    rebuildOnStart(true);
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    underTest.start();
    verify(periodicJobService).runOnce(runnableCaptor.capture(), eq(0));

    runnableCaptor.getValue().run();

    verify(taskScheduler, never()).createTaskConfigurationInstance(anyString());
    verify(taskScheduler, never()).submit(any(TaskConfiguration.class));
  }

  @Test
  public void maybeRebuild_skipsSubmitWhenExistingTaskFound() throws Exception {
    Repository repo = createRepositoryWithAssets("repo", 10);
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(repo));

    // Existing task already waiting
    when(taskScheduler.findWaitingTask(anyString(), any(Map.class))).thenReturn(true);

    rebuildOnStart(true);
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    underTest.start();
    verify(periodicJobService).runOnce(runnableCaptor.capture(), eq(0));

    runnableCaptor.getValue().run();

    // findWaitingTask was called but no new task should be created
    verify(taskScheduler).findWaitingTask(eq("create.browse.nodes"), any(Map.class));
    verify(taskScheduler, never()).createTaskConfigurationInstance(anyString());
    verify(taskScheduler, never()).submit(any(TaskConfiguration.class));
  }

  @Test
  public void maybeRebuild_handlesExceptionGracefully() throws Exception {
    when(repositoryManager.browse()).thenThrow(new RuntimeException("simulated failure"));

    rebuildOnStart(true);
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    underTest.start();
    verify(periodicJobService).runOnce(runnableCaptor.capture(), eq(0));

    // Should not throw - exception is caught and logged
    runnableCaptor.getValue().run();

    verify(taskScheduler, never()).submit(any(TaskConfiguration.class));
  }

  @Test
  public void maybeRebuild_checksForWaitingTaskWithWildcardRepositoryName() throws Exception {
    Repository repo = createRepositoryWithAssets("my-repo", 1);
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(repo));

    when(taskScheduler.findWaitingTask(anyString(), any(Map.class))).thenReturn(false);

    TaskConfiguration taskConfig = mock(TaskConfiguration.class);
    when(taskScheduler.createTaskConfigurationInstance(anyString())).thenReturn(taskConfig);

    rebuildOnStart(true);
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    underTest.start();
    verify(periodicJobService).runOnce(runnableCaptor.capture(), eq(0));

    runnableCaptor.getValue().run();

    // Verify it checks for existing task with "*" (ALL_REPOSITORIES)
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);
    verify(taskScheduler).findWaitingTask(eq("create.browse.nodes"), mapCaptor.capture());
    assertThat(mapCaptor.getValue().get("repositoryName"), is("*"));
  }

  private Repository createRepositoryWithAssets(final String name, final int assetCount) {
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn(name);

    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.count()).thenReturn(assetCount);

    return repository;
  }
}
