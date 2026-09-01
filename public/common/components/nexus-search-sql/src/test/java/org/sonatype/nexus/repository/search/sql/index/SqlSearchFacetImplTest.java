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
package org.sonatype.nexus.repository.search.sql.index;

import java.util.List;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.search.sql.store.SearchStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlSearchFacetImplTest
{

  @Mock
  private EventManager eventManager;

  @Mock
  private SearchStore searchStore;

  @Mock
  private SqlSearchIndexService searchIndexService;

  @Mock
  private Repository repository;

  private SqlSearchFacetImpl underTest;

  @BeforeEach
  void setUp() throws Exception {
    underTest = new SqlSearchFacetImpl(searchStore, searchIndexService, 100);
    underTest.installDependencies(eventManager);
    underTest.attach(repository);
    underTest.init();
    underTest.start();

  }

  @Test
  void testIndex() {
    List<EntityId> toIndex = List.of(new EntityUUID(), new EntityUUID());
    underTest.index(toIndex);

    verify(searchIndexService).index(toIndex, repository);
  }

  @Test
  void testPurge() {
    List<EntityId> toPurge = List.of(new EntityUUID(), new EntityUUID());
    underTest.purge(toPurge);

    verify(searchIndexService).purge(toPurge, repository);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testRebuildIndex_populatesComponentsThenDeletesOrphans() throws Exception {
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn("maven2");

    ContentFacet contentFacet = mock(ContentFacet.class);
    when(contentFacet.contentRepositoryId()).thenReturn(7);

    // Return an empty continuation so the browse loop exits immediately
    org.sonatype.nexus.common.entity.Continuation<FluentComponent> emptyContinuation =
        mock(org.sonatype.nexus.common.entity.Continuation.class);
    when(emptyContinuation.isEmpty()).thenReturn(true);

    FluentComponents fluentComponents = mock(FluentComponents.class);
    when(fluentComponents.count()).thenReturn(0);
    when(fluentComponents.browse(eq(100), isNull())).thenReturn(emptyContinuation);
    when(contentFacet.components()).thenReturn(fluentComponents);

    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("my-repo");
    when(repository.facet(eq(ContentFacet.class))).thenReturn(contentFacet);

    underTest.rebuildIndex();

    // Must NOT delete upfront — index stays live throughout the rebuild
    verify(searchStore, never()).deleteAllForRepository(any(), any());
    verify(searchStore, never()).deleteAllSearchAssets(any(), any());

    // Must delete orphans after populate using NOT EXISTS (no timestamp parameter)
    verify(searchStore).deleteOrphanedComponents(eq(7), eq("maven2"));
    verify(searchStore).deleteOrphanedAssets(eq(7), eq("maven2"));
  }

  @Test
  void testIndex_WhenNotStarted_ThrowsIllegalState() throws Exception {
    underTest.stop();
    assertThrows(IllegalStateException.class,
        () -> underTest.index(List.of(new EntityUUID())));
  }

  @Test
  void testPurge_WhenNotStarted_ThrowsIllegalState() throws Exception {
    underTest.stop();
    assertThrows(IllegalStateException.class,
        () -> underTest.purge(List.of(new EntityUUID())));
  }

  @Test
  void testConstructor_InvalidBatchSize_ThrowsIllegalState() {
    assertThrows(IllegalStateException.class,
        () -> new SqlSearchFacetImpl(searchStore, searchIndexService, 0));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testRebuildIndex_WithComponents_IndexesThem() throws Exception {
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn("maven2");

    ContentFacet contentFacet = mock(ContentFacet.class);
    when(contentFacet.contentRepositoryId()).thenReturn(7);

    FluentComponent component = mock(FluentComponent.class);

    // First browse returns one component, second returns empty (ends loop)
    org.sonatype.nexus.common.entity.Continuation<FluentComponent> nonEmpty =
        mock(org.sonatype.nexus.common.entity.Continuation.class);
    when(nonEmpty.isEmpty()).thenReturn(false);
    when(nonEmpty.size()).thenReturn(1);
    when(nonEmpty.nextContinuationToken()).thenReturn("token1");

    org.sonatype.nexus.common.entity.Continuation<FluentComponent> empty =
        mock(org.sonatype.nexus.common.entity.Continuation.class);
    when(empty.isEmpty()).thenReturn(true);

    FluentComponents fluentComponents = mock(FluentComponents.class);
    when(fluentComponents.count()).thenReturn(1);
    when(fluentComponents.browse(eq(100), isNull())).thenReturn(nonEmpty);
    when(fluentComponents.browse(eq(100), eq("token1"))).thenReturn(empty);
    when(contentFacet.components()).thenReturn(fluentComponents);

    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("my-repo");
    when(repository.facet(eq(ContentFacet.class))).thenReturn(contentFacet);

    underTest.rebuildIndex();

    // indexBatch should be called once with the non-empty continuation
    verify(searchIndexService).indexBatch(nonEmpty, repository);
  }

  @Test
  void testDeletion() throws Exception {
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn("cool-format");

    ContentFacet contentFacet = mock(ContentFacet.class);
    when(contentFacet.contentRepositoryId()).thenReturn(42);

    when(repository.getFormat()).thenReturn(format);
    when(repository.facet(eq(ContentFacet.class))).thenReturn(contentFacet);

    underTest.stop();
    underTest.delete();

    verify(searchStore).deleteAllForRepository(42, "cool-format");
    verify(searchStore).deleteAllSearchAssets(42, "cool-format");
  }
}
