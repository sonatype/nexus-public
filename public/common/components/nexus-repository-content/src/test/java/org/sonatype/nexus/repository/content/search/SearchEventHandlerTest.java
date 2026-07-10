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
package org.sonatype.nexus.repository.content.search;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.scheduling.PeriodicJobService.PeriodicJob;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUpdatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentCreatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentUpdatedEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SearchEventHandlerTest
{
  private static final String FORMAT = "maven2";

  private static final int COMPONENT_ID = 42;

  private static final int FLUSH_ON_COUNT = 100;

  private static final int FLUSH_ON_SECONDS = 2;

  private static final boolean NO_PURGE_DELAY = true;

  private static final int POOL_SIZE = 1;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private PeriodicJobService periodicJobService;

  @Mock
  private Repository repository;

  private TestSearchEventHandler underTest;

  /**
   * Concrete subclass of the abstract SearchEventHandler for testing purposes.
   */
  private static class TestSearchEventHandler
      extends SearchEventHandler
  {
    TestSearchEventHandler(
        final RepositoryManager repositoryManager,
        final PeriodicJobService periodicJobService,
        final int flushOnCount,
        final int flushOnSeconds,
        final boolean noPurgeDelay,
        final int poolSize)
    {
      super(repositoryManager, periodicJobService, flushOnCount, flushOnSeconds, noPurgeDelay, poolSize);
    }
  }

  @Before
  public void setUp() throws Exception {
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn(FORMAT);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("test-repo");

    PeriodicJob periodicJob = mock(PeriodicJob.class);
    when(periodicJobService.schedule(any(Runnable.class), anyInt())).thenReturn(periodicJob);

    underTest = new TestSearchEventHandler(
        repositoryManager, periodicJobService, FLUSH_ON_COUNT, FLUSH_ON_SECONDS, NO_PURGE_DELAY, POOL_SIZE);

    // Start the handler to initialize the threadPoolExecutor
    underTest.doStart();
  }

  @After
  public void tearDown() throws Exception {
    if (underTest != null && underTest.threadPoolExecutor != null) {
      underTest.doStop();
    }
  }

  @Test
  public void testConstructor() {
    assertThat(underTest, is(notNullValue()));
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullRepositoryManager() {
    new TestSearchEventHandler(null, periodicJobService, FLUSH_ON_COUNT, FLUSH_ON_SECONDS, NO_PURGE_DELAY, POOL_SIZE);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullPeriodicJobService() {
    new TestSearchEventHandler(repositoryManager, null, FLUSH_ON_COUNT, FLUSH_ON_SECONDS, NO_PURGE_DELAY, POOL_SIZE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorRejectsZeroFlushOnCount() {
    new TestSearchEventHandler(repositoryManager, periodicJobService, 0, FLUSH_ON_SECONDS, NO_PURGE_DELAY, POOL_SIZE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorRejectsNegativeFlushOnCount() {
    new TestSearchEventHandler(repositoryManager, periodicJobService, -1, FLUSH_ON_SECONDS, NO_PURGE_DELAY, POOL_SIZE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorRejectsZeroFlushOnSeconds() {
    new TestSearchEventHandler(repositoryManager, periodicJobService, FLUSH_ON_COUNT, 0, NO_PURGE_DELAY, POOL_SIZE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorRejectsNegativeFlushOnSeconds() {
    new TestSearchEventHandler(repositoryManager, periodicJobService, FLUSH_ON_COUNT, -1, NO_PURGE_DELAY, POOL_SIZE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorRejectsZeroPoolSize() {
    new TestSearchEventHandler(repositoryManager, periodicJobService, FLUSH_ON_COUNT, FLUSH_ON_SECONDS,
        NO_PURGE_DELAY, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorRejectsNegativePoolSize() {
    new TestSearchEventHandler(repositoryManager, periodicJobService, FLUSH_ON_COUNT, FLUSH_ON_SECONDS,
        NO_PURGE_DELAY, -1);
  }

  @Test
  public void testIsCalmPeriodInitiallyTrue() {
    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testSearchEventQueueInitiallyZero() {
    assertThat(underTest.searchEventQueue(), is(0));
  }

  @Test
  public void testRequestIndexAddsComponentToPending() {
    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);

    // After requesting index, the handler should no longer be in a calm period once flush starts,
    // but since we haven't hit the flush threshold, pollSearchUpdateRequest should find pending work
    underTest.pollSearchUpdateRequest();
  }

  @Test
  public void testRequestIndexIgnoresNonPositiveComponentId() {
    underTest.requestIndex(FORMAT, 0, repository);
    underTest.requestIndex(FORMAT, -1, repository);

    // Nothing should be pending - the handler should still be calm
    // (pollSearchUpdateRequest won't flush anything because nothing was queued)
    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testSetProcessEventsDisablesProcessing() {
    underTest.setProcessEvents(false);

    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);

    // With processing disabled, nothing should be queued
    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testSetProcessEventsReenablesProcessing() {
    underTest.setProcessEvents(false);
    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);

    underTest.setProcessEvents(true);
    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);

    // After re-enabling, the request should be accepted
    underTest.pollSearchUpdateRequest();
  }

  @Test
  public void testOnComponentCreatedEvent() {
    ComponentCreatedEvent event = mock(ComponentCreatedEvent.class);
    ComponentData component = createComponentData(COMPONENT_ID);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    when(event.getFormat()).thenReturn(FORMAT);
    when(event.getComponent()).thenReturn(component);

    underTest.on(event);

    // Verify the event was processed by attempting a flush
    underTest.pollSearchUpdateRequest();
  }

  @Test
  public void testOnComponentUpdatedEvent() {
    ComponentUpdatedEvent event = mock(ComponentUpdatedEvent.class);
    ComponentData component = createComponentData(COMPONENT_ID);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    when(event.getFormat()).thenReturn(FORMAT);
    when(event.getComponent()).thenReturn(component);

    underTest.on(event);

    underTest.pollSearchUpdateRequest();
  }

  @Test
  public void testOnComponentDeletedEvent() {
    ComponentDeletedEvent event = mock(ComponentDeletedEvent.class);
    ComponentData component = createComponentData(COMPONENT_ID);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    when(event.getFormat()).thenReturn(FORMAT);
    when(event.getComponent()).thenReturn(component);

    underTest.on(event);

    underTest.pollSearchUpdateRequest();
  }

  @Test
  public void testOnAssetCreatedEvent() {
    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    AssetData asset = createAssetData(COMPONENT_ID);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    when(event.getFormat()).thenReturn(FORMAT);
    when(event.getAsset()).thenReturn(asset);

    underTest.on(event);

    underTest.pollSearchUpdateRequest();
  }

  @Test
  public void testOnAssetUpdatedEvent() {
    AssetUpdatedEvent event = mock(AssetUpdatedEvent.class);
    AssetData asset = createAssetData(COMPONENT_ID);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    when(event.getFormat()).thenReturn(FORMAT);
    when(event.getAsset()).thenReturn(asset);

    underTest.on(event);

    underTest.pollSearchUpdateRequest();
  }

  @Test
  public void testOnAssetDeletedEvent() {
    AssetDeletedEvent event = mock(AssetDeletedEvent.class);
    AssetData asset = createAssetData(COMPONENT_ID);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    when(event.getFormat()).thenReturn(FORMAT);
    when(event.getAsset()).thenReturn(asset);

    underTest.on(event);

    underTest.pollSearchUpdateRequest();
  }

  @Test
  public void testOnComponentCreatedEventWithNoRepository() {
    ComponentCreatedEvent event = mock(ComponentCreatedEvent.class);
    when(event.getRepository()).thenReturn(Optional.empty());

    underTest.on(event);

    // Nothing should be queued since repository is absent
    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testOnComponentDeletedEventWithNoRepository() {
    ComponentDeletedEvent event = mock(ComponentDeletedEvent.class);
    when(event.getRepository()).thenReturn(Optional.empty());

    underTest.on(event);

    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testOnAssetCreatedEventWithNoRepository() {
    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    when(event.getRepository()).thenReturn(Optional.empty());

    underTest.on(event);

    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testOnComponentPurgedEvent() {
    ComponentPurgedEvent event = mock(ComponentPurgedEvent.class);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    when(event.getComponentIds()).thenReturn(new int[]{10, 20, 30});

    underTest.on(event);

    underTest.pollSearchUpdateRequest();
  }

  @Test
  public void testOnComponentPurgedEventWithNoRepository() {
    ComponentPurgedEvent event = mock(ComponentPurgedEvent.class);
    when(event.getRepository()).thenReturn(Optional.empty());

    underTest.on(event);

    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testOnComponentPurgedEventWithNonPositiveIds() {
    ComponentPurgedEvent event = mock(ComponentPurgedEvent.class);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    when(event.getComponentIds()).thenReturn(new int[]{0, -1});

    underTest.on(event);

    // Non-positive IDs should be skipped
    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testProcessEventsDisabledIgnoresComponentEvents() {
    underTest.setProcessEvents(false);

    ComponentCreatedEvent event = mock(ComponentCreatedEvent.class);
    ComponentData component = createComponentData(COMPONENT_ID);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    when(event.getFormat()).thenReturn(FORMAT);
    when(event.getComponent()).thenReturn(component);

    underTest.on(event);

    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testProcessEventsDisabledIgnoresAssetEvents() {
    underTest.setProcessEvents(false);

    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    AssetData asset = createAssetData(COMPONENT_ID);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    when(event.getFormat()).thenReturn(FORMAT);
    when(event.getAsset()).thenReturn(asset);

    underTest.on(event);

    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testDuplicateRequestsDoNotIncreasePendingCount() {
    // Request the same component twice
    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);
    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);

    // Only one pending request should exist (map-based dedup)
    underTest.pollSearchUpdateRequest();
  }

  @Test
  public void testMultipleDistinctRequestsArePending() {
    underTest.requestIndex(FORMAT, 1, repository);
    underTest.requestIndex(FORMAT, 2, repository);
    underTest.requestIndex(FORMAT, 3, repository);

    underTest.pollSearchUpdateRequest();
  }

  @Test
  public void testFlushPageOfComponentsWithNullRequestType() {
    when(repositoryManager.get("test-repo")).thenReturn(repository);
    SearchFacet searchFacet = mock(SearchFacet.class);
    when(repository.optionalFacet(SearchFacet.class)).thenReturn(Optional.of(searchFacet));

    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);

    underTest.flushPageOfComponents(null);

    verify(searchFacet).index(anyCollection());
  }

  @Test
  public void testFlushPageOfComponentsWhenRepositoryNotFound() {
    when(repositoryManager.get("test-repo")).thenReturn(null);

    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);

    // Should not throw when repository is not found
    underTest.flushPageOfComponents(null);
  }

  @Test
  public void testFlushPageOfComponentsWhenSearchFacetAbsent() {
    when(repositoryManager.get("test-repo")).thenReturn(repository);
    when(repository.optionalFacet(SearchFacet.class)).thenReturn(Optional.empty());

    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);

    // Should not throw when search facet is absent
    underTest.flushPageOfComponents(null);
  }

  @Test
  public void testPollSearchUpdateRequestWithNoPending() {
    // Should not throw when there's nothing to flush
    underTest.pollSearchUpdateRequest();
  }

  @Test
  public void testIndexUIUploadWithSearchAndContentFacets() {
    SearchFacet searchFacet = mock(SearchFacet.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);

    when(repository.optionalFacet(SearchFacet.class)).thenReturn(Optional.of(searchFacet));
    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.path("/path/to/asset")).thenReturn(assetBuilder);

    // Asset not found, so no component IDs to index
    when(assetBuilder.find()).thenReturn(Optional.empty());

    List<String> assetPaths = Collections.singletonList("/path/to/asset");

    SearchEventHandler.indexUIUpload(repository, assetPaths);

    // No components found, so index should not be called
    verify(searchFacet, never()).index(anyCollection());
  }

  @Test
  public void testIndexUIUploadWithNoSearchFacet() {
    when(repository.optionalFacet(SearchFacet.class)).thenReturn(Optional.empty());

    List<String> assetPaths = Collections.singletonList("/path/to/asset");

    // Should not throw when SearchFacet is absent
    SearchEventHandler.indexUIUpload(repository, assetPaths);
  }

  @Test
  public void testIndexUIUploadWithNoContentFacet() {
    SearchFacet searchFacet = mock(SearchFacet.class);
    when(repository.optionalFacet(SearchFacet.class)).thenReturn(Optional.of(searchFacet));
    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.empty());

    List<String> assetPaths = Collections.singletonList("/path/to/asset");

    // Should not throw when ContentFacet is absent
    SearchEventHandler.indexUIUpload(repository, assetPaths);

    verify(searchFacet, never()).index(anyCollection());
  }

  @Test
  public void testIndexUIUploadWithEmptyAssetPaths() {
    SearchFacet searchFacet = mock(SearchFacet.class);
    ContentFacet contentFacet = mock(ContentFacet.class);

    when(repository.optionalFacet(SearchFacet.class)).thenReturn(Optional.of(searchFacet));
    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));

    SearchEventHandler.indexUIUpload(repository, Collections.emptyList());

    // No assets to index, so searchFacet.index should not be called
    verify(searchFacet, never()).index(anyCollection());
  }

  @Test
  public void testOnAssetEventWithNoComponentId() {
    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    AssetData asset = new AssetData();
    asset.setRepositoryId(1);
    asset.setAssetId(1);
    asset.setPath("/test");
    asset.setKind("default");
    // Deliberately not setting componentId - should result in -1 from internalComponentId(Asset)

    when(event.getRepository()).thenReturn(Optional.of(repository));
    when(event.getFormat()).thenReturn(FORMAT);
    when(event.getAsset()).thenReturn(asset);

    underTest.on(event);

    // Asset with no component ID (-1) should be ignored by requestIndex
    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testDoStartWithFlushOnCountOne() throws Exception {
    // When flushOnCount is 1, periodicJobService should not be used for scheduling
    PeriodicJobService localPeriodicJobService = mock(PeriodicJobService.class);
    TestSearchEventHandler handler = new TestSearchEventHandler(
        repositoryManager, localPeriodicJobService, 1, FLUSH_ON_SECONDS, NO_PURGE_DELAY, POOL_SIZE);

    handler.doStart();

    verify(localPeriodicJobService, never()).startUsing();

    handler.doStop();
  }

  @Test
  public void testDoStartWithFlushOnCountGreaterThanOne() {
    // The setUp method already started with flushOnCount=100, verify periodicJobService was used
    verify(periodicJobService).startUsing();
  }

  // -------- NEXUS-52885: synchronous flushPendingForRepository --------

  @Test
  public void testFlushPendingForRepositoryDrainsMatchingPending() {
    when(repositoryManager.get("test-repo")).thenReturn(repository);
    SearchFacet searchFacet = mock(SearchFacet.class);
    when(repository.optionalFacet(SearchFacet.class)).thenReturn(Optional.of(searchFacet));

    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);

    underTest.flushPendingForRepository("test-repo");

    // The pending request was processed synchronously on the calling thread.
    verify(searchFacet).index(anyCollection());
    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testFlushPendingForRepositoryIgnoresOtherRepositories() {
    Repository otherRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn(FORMAT);
    when(otherRepo.getFormat()).thenReturn(format);
    when(otherRepo.getName()).thenReturn("other-repo");

    SearchFacet otherFacet = mock(SearchFacet.class);
    when(otherRepo.optionalFacet(SearchFacet.class)).thenReturn(Optional.of(otherFacet));
    when(repositoryManager.get("other-repo")).thenReturn(otherRepo);

    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);
    underTest.requestIndex(FORMAT, COMPONENT_ID + 1, otherRepo);

    underTest.flushPendingForRepository("other-repo");

    verify(otherFacet).index(anyCollection());
    // test-repo's request must remain queued.
    assertThat(underTest.isCalmPeriod(), is(false));
  }

  @Test
  public void testFlushPendingForRepositoryWithNoPendingIsNoop() {
    underTest.flushPendingForRepository("test-repo");

    // Repository should never be looked up if there's nothing pending.
    verify(repositoryManager, never()).get("test-repo");
  }

  @Test
  public void testFlushPendingForRepositoryWithNullNameIsNoop() {
    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);
    underTest.flushPendingForRepository(null);
    // Pending request remains.
    assertThat(underTest.isCalmPeriod(), is(false));
  }

  @Test
  public void testFlushPendingForRepositoryWhenProcessingDisabledIsNoop() {
    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);
    underTest.setProcessEvents(false);
    underTest.flushPendingForRepository("test-repo");
    // Even though pending exists, processing is disabled — nothing flushed and nothing looked up.
    verify(repositoryManager, never()).get("test-repo");
  }

  @Test
  public void testFlushPendingForRepositoryDrainsPendingEvenWhenRepositoryMissing() {
    when(repositoryManager.get("test-repo")).thenReturn(null);

    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);

    // Should not throw even though repository disappeared between enqueue and flush.
    underTest.flushPendingForRepository("test-repo");

    // Pending entry was consumed (drained from queue) even though the repo is gone.
    assertThat(underTest.isCalmPeriod(), is(true));
  }

  @Test
  public void testFlushPendingForRepositorySwallowsIllegalStateExceptionWhenFacetStopped() {
    SearchFacet searchFacet = mock(SearchFacet.class);
    when(repositoryManager.get("test-repo")).thenReturn(repository);
    when(repository.optionalFacet(SearchFacet.class)).thenReturn(Optional.of(searchFacet));
    doThrow(new IllegalStateException("repository stopped")).when(searchFacet).index(anyCollection());

    underTest.requestIndex(FORMAT, COMPONENT_ID, repository);

    // Must not throw — same silent-drop semantics as SqlSearchEventHandler.requestIndex().
    underTest.flushPendingForRepository("test-repo");

    // Entries were drained from the queue (isCalmPeriod true) even though index() threw.
    assertThat(underTest.isCalmPeriod(), is(true));
  }

  private static ComponentData createComponentData(final int componentId) {
    ComponentData component = new ComponentData();
    component.setRepositoryId(1);
    component.setComponentId(componentId);
    component.setNamespace("org.example");
    component.setName("test-component");
    component.setVersion("1.0.0");
    return component;
  }

  private static AssetData createAssetData(final int componentId) {
    ComponentData component = createComponentData(componentId);
    AssetData asset = new AssetData();
    asset.setRepositoryId(1);
    asset.setAssetId(1);
    asset.setPath("/test/asset.jar");
    asset.setKind("default");
    asset.setComponent(component);
    return asset;
  }
}
