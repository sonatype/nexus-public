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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.index.SearchIndexFacet;
import org.sonatype.nexus.repository.search.index.SearchUpdateService;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import org.elasticsearch.cluster.metadata.ProcessClusterEventTimeoutException;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchUpdateTaskTest
    extends TestSupport
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SearchUpdateService searchUpdateService;

  @Mock
  private Repository repository1;

  @Mock
  private Repository repository2;

  @Mock
  private SearchIndexFacet searchIndexFacet1;

  @Mock
  private SearchIndexFacet searchIndexFacet2;

  @Mock
  private TaskScheduler taskScheduler;

  private final TaskConfiguration configuration = new TaskConfiguration();

  private SearchUpdateTask underTest;

  @Before
  public void setup() {
    when(repository1.getName()).thenReturn("repository1");
    when(repository2.getName()).thenReturn("repository1");
    when(repository1.facet(SearchIndexFacet.class)).thenReturn(searchIndexFacet1);
    when(repository2.facet(SearchIndexFacet.class)).thenReturn(searchIndexFacet2);
    when(repositoryManager.get("repository1")).thenReturn(repository1);
    when(repositoryManager.get("repository2")).thenReturn(repository2);

    configuration.setId("test");
    configuration.setTypeId("test");

    underTest = new SearchUpdateTask(repositoryManager, searchUpdateService, taskScheduler);
  }

  @Test
  public void runOnOneRepository() {
    configuration.setString("repositoryNames", "repository1");
    underTest.configure(configuration);
    underTest.execute();
    verify(searchIndexFacet1).rebuildIndex();
    verify(searchUpdateService).doneReindexing(repository1);
  }

  @Test
  public void runOnMultipleRepositories() {
    configuration.setString("repositoryNames", "repository1,repository2");
    underTest.configure(configuration);
    underTest.execute();
    verify(searchIndexFacet1).rebuildIndex();
    verify(searchIndexFacet2).rebuildIndex();
    verify(searchUpdateService).doneReindexing(repository1);
    verify(searchUpdateService).doneReindexing(repository2);
  }

  @Test
  public void unknownRepository() {
    configuration.setString("repositoryNames", "repository1,unknown,repository2");
    underTest.configure(configuration);
    underTest.execute();
    verify(searchIndexFacet1).rebuildIndex();
    verify(searchIndexFacet2).rebuildIndex();
    verify(searchUpdateService).doneReindexing(repository1);
    verify(searchUpdateService).doneReindexing(repository2);
  }

  @Test
  public void runOnMultipleRepositoriesButFailRebuildingIndex() {
    doThrow(new ProcessClusterEventTimeoutException(
        new TimeValue(30000), "failed to process cluster event (delete-index)"))
        .when(searchIndexFacet1).rebuildIndex();
    configuration.setString("repositoryNames", "repository1,repository2");
    underTest.configure(configuration);
    underTest.execute();
    verify(searchIndexFacet2).rebuildIndex();
    verify(searchUpdateService).doneReindexing(repository2);
  }
}
