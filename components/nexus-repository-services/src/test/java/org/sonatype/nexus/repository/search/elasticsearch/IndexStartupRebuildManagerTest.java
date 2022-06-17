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
package org.sonatype.nexus.repository.search.elasticsearch;

import java.util.Arrays;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.index.ElasticSearchIndexService;
import org.sonatype.nexus.repository.search.index.RebuildIndexTaskDescriptor;
import org.sonatype.nexus.repository.search.index.SearchIndexFacet;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexStartupRebuildManagerTest
    extends TestSupport
{
  private IndexStartupRebuildManager underTest;

  @Mock
  TaskScheduler taskScheduler;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  private Repository repository;

  @Mock
  private Repository repository2;

  @Mock
  ElasticSearchIndexService elasticSearchIndexService;

  @Before
  public void start() {
    SearchIndexFacet mockFacet = mock(SearchIndexFacet.class);
    when(repository.optionalFacet(SearchIndexFacet.class)).thenReturn(Optional.of(mockFacet));
    when(repository2.optionalFacet(SearchIndexFacet.class)).thenReturn(Optional.of(mockFacet));
    when(repositoryManager.browse()).thenReturn(Arrays.asList(repository, repository2));
  }

  @Test
  public void rebuildDoesntRunIfNoVariableIsSet() throws Exception {
    underTest = new IndexStartupRebuildManager(taskScheduler, repositoryManager,
        elasticSearchIndexService, null);
    underTest.doStart();
    verify(taskScheduler, never()).submit(any());
  }

  @Test
  public void rebuildDoesntRunIfVariableIsFalse() throws Exception {
    underTest = new IndexStartupRebuildManager(taskScheduler, repositoryManager,
        elasticSearchIndexService, "false");
    underTest.doStart();
    verify(taskScheduler, never()).submit(any());
  }

  @Test
  public void rebuildDoesntRunIfVariableIsInvalid() throws Exception {
    underTest = new IndexStartupRebuildManager(taskScheduler, repositoryManager,
        elasticSearchIndexService, "this is invalid");
    underTest.doStart();
    verify(taskScheduler, never()).submit(any());
  }

  @Test
  public void rebuildIndexesIfVariableIsTrueAndAllIndexesAreEmpty() throws Exception {
    underTest = new IndexStartupRebuildManager(taskScheduler, repositoryManager,
        elasticSearchIndexService, "true");

    when(taskScheduler.createTaskConfigurationInstance(RebuildIndexTaskDescriptor.TYPE_ID))
        .thenReturn(new TaskConfiguration());
    when(elasticSearchIndexService.indexEmpty(any(Repository.class))).thenReturn(true);

    underTest.doStart();

    assertRebuildScheduled();
  }

  @Test
  public void shouldNotRebuildIndexesWhenOneIndexIsNotEmpty() throws Exception {
    underTest = new IndexStartupRebuildManager(taskScheduler, repositoryManager,
        elasticSearchIndexService, "true");

    when(elasticSearchIndexService.indexEmpty(repository)).thenReturn(true);
    when(elasticSearchIndexService.indexEmpty(repository2)).thenReturn(false);
    when(taskScheduler.createTaskConfigurationInstance(RebuildIndexTaskDescriptor.TYPE_ID))
        .thenReturn(new TaskConfiguration());

    underTest.doStart();

    verify(taskScheduler, never()).submit(any());
  }

  private void assertRebuildScheduled() {
    ArgumentCaptor<TaskConfiguration> taskConfigurationArgumentCaptor =
        ArgumentCaptor.forClass(TaskConfiguration.class);

    verify(taskScheduler).submit(taskConfigurationArgumentCaptor.capture());

    TaskConfiguration taskConfiguration = taskConfigurationArgumentCaptor.getValue();

    MatcherAssert.assertThat(taskConfiguration.getString("repositoryName"), Matchers.is( "*"));
  }
}
