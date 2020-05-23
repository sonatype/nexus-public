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
package org.sonatype.nexus.repository.content.browse.internal;

import java.util.function.BooleanSupplier;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.RebuildBrowseNodeFailedException;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DatastoreRebuildBrowseNodeServiceTest
    extends TestSupport
{
  private static final int REBUILD_PAGE_SIZE = 2;

  @Mock
  private DatastoreBrowseNodeManager browseNodeManager;

  @Mock
  private BrowseNodeConfiguration configuration;

  @Mock
  private Repository repository;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private Continuation<FluentAsset> firstBatch;

  @Mock
  private Continuation<FluentAsset> secondBatch;

  @Mock
  private Continuation<FluentAsset> thirdBatch;

  @Mock
  private Continuation<FluentAsset> emptyBatch;

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private DatastoreRebuildBrowseNodeService rebuildService;

  @Before
  public void setup() {
    when(configuration.getRebuildPageSize()).thenReturn(REBUILD_PAGE_SIZE);

    when(firstBatch.isEmpty()).thenReturn(false);
    when(firstBatch.size()).thenReturn(REBUILD_PAGE_SIZE);

    when(secondBatch.isEmpty()).thenReturn(false);
    when(secondBatch.size()).thenReturn(REBUILD_PAGE_SIZE);

    when(thirdBatch.isEmpty()).thenReturn(false);
    when(thirdBatch.size()).thenReturn(REBUILD_PAGE_SIZE - 1);

    when(emptyBatch.isEmpty()).thenReturn(true);

    when(firstBatch.nextContinuationToken()).thenReturn("secondBatch");
    when(secondBatch.nextContinuationToken()).thenReturn("thirdBatch");
    when(thirdBatch.nextContinuationToken()).thenReturn("emptyBatch");

    when(fluentAssets.browse(REBUILD_PAGE_SIZE, null)).thenReturn(firstBatch);
    when(fluentAssets.browse(REBUILD_PAGE_SIZE, "secondBatch")).thenReturn(secondBatch);
    when(fluentAssets.browse(REBUILD_PAGE_SIZE, "thirdBatch")).thenReturn(thirdBatch);
    when(fluentAssets.browse(REBUILD_PAGE_SIZE, "emptyBatch")).thenReturn(emptyBatch);

    when(contentFacet.assets()).thenReturn(fluentAssets);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);

    rebuildService = new DatastoreRebuildBrowseNodeService(browseNodeManager, configuration);
  }

  @Test
  public void executeWithOfflineRepository() throws RebuildBrowseNodeFailedException {
    Configuration config = mock(Configuration.class);
    when(config.isOnline()).thenReturn(false);

    when(repository.getConfiguration()).thenReturn(config);

    rebuildService.rebuild(repository, () -> false);

    verify(browseNodeManager).deleteByRepository(repository);
    verify(browseNodeManager).createFromAssets(repository, firstBatch);
    verify(browseNodeManager).createFromAssets(repository, secondBatch);
    verify(browseNodeManager).createFromAssets(repository, thirdBatch);
    verifyNoMoreInteractions(browseNodeManager);
  }

  @Test
  public void executeTruncatesNodesForNoAssets() throws RebuildBrowseNodeFailedException {
    when(fluentAssets.browse(REBUILD_PAGE_SIZE, null)).thenReturn(emptyBatch);

    rebuildService.rebuild(repository, () -> false);

    verify(browseNodeManager).deleteByRepository(repository);
    verifyNoMoreInteractions(browseNodeManager);
  }

  @Test
  public void executeProcessesAllAssetsInPages() throws RebuildBrowseNodeFailedException {
    rebuildService.rebuild(repository, () -> false);

    verify(browseNodeManager).deleteByRepository(repository);
    verify(browseNodeManager).createFromAssets(repository, firstBatch);
    verify(browseNodeManager).createFromAssets(repository, secondBatch);
    verify(browseNodeManager).createFromAssets(repository, thirdBatch);
    verifyNoMoreInteractions(browseNodeManager);
  }

  @Test
  public void executionCanCancelDuringTheProcess() throws RebuildBrowseNodeFailedException {
    BooleanSupplier mockCancel = mock(BooleanSupplier.class);
    when(mockCancel.getAsBoolean()).thenReturn(false).thenReturn(true);

    thrown.expect(RebuildBrowseNodeFailedException.class);
    thrown.expectCause(instanceOf(TaskInterruptedException.class));

    rebuildService.rebuild(repository, mockCancel);

    verify(browseNodeManager).deleteByRepository(repository);
    verify(browseNodeManager).createFromAssets(repository, firstBatch);
    verify(browseNodeManager).createFromAssets(repository, secondBatch);
    verifyNoMoreInteractions(browseNodeManager);
  }
}
