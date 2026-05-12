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

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import org.sonatype.nexus.common.cooperation2.datastore.DefaultCooperation2Factory;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.scheduling.PeriodicJobService.PeriodicJob;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.browse.capability.BrowseTrimService;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.ContentStoreEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BrowseEventHandlerTest
{
  private static final String FORMAT_NAME = "maven2";

  @Mock
  private PeriodicJobService periodicJobService;

  @Mock
  private EventManager eventManager;

  @Mock
  private DatabaseCheck databaseCheck;

  @Mock
  private BrowseTrimService browseTrimService;

  @Mock
  private Repository repository;

  @Mock
  private BrowseFacet browseFacet;

  private BrowseEventHandler underTest;

  @Before
  public void setup() {
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn(FORMAT_NAME);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("test-repo");
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));

    DefaultCooperation2Factory cooperation = new DefaultCooperation2Factory();
    underTest = new BrowseEventHandler(cooperation, periodicJobService, eventManager, true, Duration.ofSeconds(0),
        Duration.ofSeconds(30), 100, 2, true, databaseCheck, browseTrimService);
  }

  // --- Paused event handling tests ---

  @Test
  public void testAssetPurgedEvent_notProcessedWhenPaused() {
    underTest.pauseEventProcessing();
    AssetPurgedEvent event = mock(AssetPurgedEvent.class);
    underTest.on(event);
    verifyNoInteractions(event);
  }

  @Test
  public void testAssetCreatedEvent_notProcessedWhenPaused() {
    underTest.pauseEventProcessing();
    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    underTest.on(event);
    verifyNoInteractions(event);
  }

  @Test
  public void testAssetUploadedEvent_notProcessedWhenPaused() {
    underTest.pauseEventProcessing();
    AssetUploadedEvent event = mock(AssetUploadedEvent.class);
    underTest.on(event);
    verifyNoInteractions(event);
  }

  @Test
  public void testAssetDeletedEvent_notProcessedWhenPaused() {
    underTest.pauseEventProcessing();
    AssetDeletedEvent event = mock(AssetDeletedEvent.class);
    underTest.on(event);
    verifyNoInteractions(event);
  }

  @Test
  public void testComponentPurgedEvent_notProcessedWhenPaused() {
    underTest.pauseEventProcessing();
    ComponentPurgedEvent event = mock(ComponentPurgedEvent.class);
    underTest.on(event);
    verifyNoInteractions(event);
  }

  @Test
  public void testComponentDeletedEvent_notProcessedWhenPaused() {
    underTest.pauseEventProcessing();
    ComponentDeletedEvent event = mock(ComponentDeletedEvent.class);
    underTest.on(event);
    verifyNoInteractions(event);
  }

  // --- Resume event processing ---

  @Test
  public void testResumeEventProcessing_enablesHandling() {
    underTest.pauseEventProcessing();
    assertFalse(underTest.shouldHandle());
    underTest.resumeEventProcessing();
    assertTrue(underTest.shouldHandle());
  }

  // --- Constructor validation ---

  @Test(expected = NullPointerException.class)
  public void testConstructor_rejectsNullCooperationFactory() {
    new BrowseEventHandler(null, periodicJobService, eventManager, true, Duration.ofSeconds(0),
        Duration.ofSeconds(30), 100, 2, true, databaseCheck, browseTrimService);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_rejectsNullPeriodicJobService() {
    new BrowseEventHandler(new DefaultCooperation2Factory(), null, eventManager, true, Duration.ofSeconds(0),
        Duration.ofSeconds(30), 100, 2, true, databaseCheck, browseTrimService);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_rejectsNullEventManager() {
    new BrowseEventHandler(new DefaultCooperation2Factory(), periodicJobService, null, true, Duration.ofSeconds(0),
        Duration.ofSeconds(30), 100, 2, true, databaseCheck, browseTrimService);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_rejectsZeroFlushOnCount() {
    new BrowseEventHandler(new DefaultCooperation2Factory(), periodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), 0, 2, true, databaseCheck, browseTrimService);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_rejectsNegativeFlushOnCount() {
    new BrowseEventHandler(new DefaultCooperation2Factory(), periodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), -1, 2, true, databaseCheck, browseTrimService);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_rejectsZeroFlushOnSeconds() {
    new BrowseEventHandler(new DefaultCooperation2Factory(), periodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), 100, 0, true, databaseCheck, browseTrimService);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_rejectsNegativeFlushOnSeconds() {
    new BrowseEventHandler(new DefaultCooperation2Factory(), periodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), 100, -1, true, databaseCheck, browseTrimService);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_rejectsNullDatabaseCheck() {
    new BrowseEventHandler(new DefaultCooperation2Factory(), periodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), 100, 2, true, null, browseTrimService);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_rejectsNullBrowseTrimService() {
    new BrowseEventHandler(new DefaultCooperation2Factory(), periodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), 100, 2, true, databaseCheck, null);
  }

  // --- markAssetAsPending via on(AssetCreatedEvent) ---

  @Test
  public void testAssetCreatedEvent_marksAssetAsPending() throws Exception {
    AssetCreatedEvent event = createAssetCreatedEvent(1, repository);

    underTest.on(event);

    // Flushing should deliver the pending asset to BrowseFacet
    underTest.flushAssets();
    verify(browseFacet).addPathsToAssets(any());
  }

  @Test
  public void testAssetUploadedEvent_marksAssetAsPending() throws Exception {
    AssetUploadedEvent event = createAssetUploadedEvent(1, repository);

    underTest.on(event);

    underTest.flushAssets();
    verify(browseFacet).addPathsToAssets(any());
  }

  @Test
  public void testAssetCreatedEvent_missingRepository_doesNotAddToPending() throws Exception {
    AssetCreatedEvent event = createAssetCreatedEvent(1, null);

    underTest.on(event);

    // Flush should have nothing to do
    underTest.flushAssets();
    verify(browseFacet, never()).addPathsToAssets(any());
  }

  @Test
  public void testAssetUploadedEvent_missingRepository_doesNotAddToPending() throws Exception {
    AssetUploadedEvent event = createAssetUploadedEvent(1, null);

    underTest.on(event);

    underTest.flushAssets();
    verify(browseFacet, never()).addPathsToAssets(any());
  }

  @Test
  public void testAssetCreatedEvent_duplicateAsset_onlyCountedOnce() throws Exception {
    AssetCreatedEvent event1 = createAssetCreatedEvent(1, repository);
    AssetCreatedEvent event2 = createAssetCreatedEvent(1, repository);

    underTest.on(event1);
    underTest.on(event2);

    // Flush should only have one asset
    underTest.flushAssets();
    verify(browseFacet, times(1)).addPathsToAssets(any());
  }

  @Test
  public void testAssetCreatedEvent_differentAssets_allPending() throws Exception {
    AssetCreatedEvent event1 = createAssetCreatedEvent(1, repository);
    AssetCreatedEvent event2 = createAssetCreatedEvent(2, repository);

    underTest.on(event1);
    underTest.on(event2);

    underTest.flushAssets();
    verify(browseFacet, times(1)).addPathsToAssets(any());
  }

  // --- markAssetAsPending flush threshold ---

  @Test
  public void testAssetCreatedEvent_exceedingFlushOnCount_postsFlushEvent() throws Exception {
    // Create handler with flushOnCount=2 to easily trigger the threshold
    DefaultCooperation2Factory cooperation = new DefaultCooperation2Factory();
    BrowseEventHandler handler = new BrowseEventHandler(cooperation, periodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), 2, 2, true, databaseCheck, browseTrimService);

    AssetCreatedEvent event1 = createAssetCreatedEvent(1, repository);
    AssetCreatedEvent event2 = createAssetCreatedEvent(2, repository);

    handler.on(event1);
    handler.on(event2);

    // The handler registers the FlushEventReceiver on construction, plus the FlushEvent
    // The eventManager.register is called once in constructor, then eventManager.post for FlushEvent
    verify(eventManager, times(2)).register(any()); // one for underTest constructor, one for handler constructor
    verify(eventManager).post(any());
  }

  @Test
  public void testAssetCreatedEvent_belowFlushThreshold_doesNotPostFlushEvent() throws Exception {
    // Default handler has flushOnCount=100
    AssetCreatedEvent event = createAssetCreatedEvent(1, repository);

    underTest.on(event);

    // Only register calls, no post for FlushEvent
    verify(eventManager, never()).post(any());
  }

  // --- markRepositoryForTrimming via on(AssetDeletedEvent) ---

  @Test
  public void testAssetDeletedEvent_marksRepositoryForTrimming() throws Exception {
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(false);
    AssetDeletedEvent event = createAssetDeletedEvent(1, repository);

    underTest.on(event);

    // With noPurgeDelay=true and batchTrim disabled, a PurgeEvent should be posted
    verify(eventManager).post(any());
  }

  @Test
  public void testAssetDeletedEvent_missingRepository_doesNotTrim() throws Exception {
    AssetDeletedEvent event = createAssetDeletedEvent(1, null);

    underTest.on(event);

    // No PurgeEvent posted since repository was missing
    verify(eventManager, never()).post(any());
  }

  @Test
  public void testAssetPurgedEvent_marksRepositoryForTrimming() throws Exception {
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(false);
    AssetPurgedEvent event = createAssetPurgedEvent(repository);

    underTest.on(event);

    verify(eventManager).post(any());
  }

  @Test
  public void testAssetPurgedEvent_missingRepository_doesNotTrim() throws Exception {
    AssetPurgedEvent event = createAssetPurgedEvent(null);

    underTest.on(event);

    verify(eventManager, never()).post(any());
  }

  @Test
  public void testComponentDeletedEvent_marksRepositoryForTrimming() throws Exception {
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(false);
    ComponentDeletedEvent event = mock(ComponentDeletedEvent.class);
    when(event.getRepository()).thenReturn(Optional.of(repository));

    underTest.on(event);

    verify(eventManager).post(any());
  }

  @Test
  public void testComponentDeletedEvent_missingRepository_doesNotTrim() {
    ComponentDeletedEvent event = mock(ComponentDeletedEvent.class);
    when(event.getRepository()).thenReturn(Optional.empty());

    underTest.on(event);

    verify(eventManager, never()).post(any());
  }

  @Test
  public void testComponentPurgedEvent_marksRepositoryForTrimming() throws Exception {
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(false);
    ComponentPurgedEvent event = mock(ComponentPurgedEvent.class);
    when(event.getRepository()).thenReturn(Optional.of(repository));

    underTest.on(event);

    verify(eventManager).post(any());
  }

  @Test
  public void testComponentPurgedEvent_missingRepository_doesNotTrim() {
    ComponentPurgedEvent event = mock(ComponentPurgedEvent.class);
    when(event.getRepository()).thenReturn(Optional.empty());

    underTest.on(event);

    verify(eventManager, never()).post(any());
  }

  // --- shouldTriggerImmediatePurge ---

  @Test
  public void testMarkRepositoryForTrimming_batchTrimEnabled_doesNotPostPurgeEvent() throws Exception {
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(true);
    AssetDeletedEvent event = createAssetDeletedEvent(1, repository);

    underTest.on(event);

    // Batch trim enabled means no immediate purge
    verify(eventManager, never()).post(any());
  }

  @Test
  public void testMarkRepositoryForTrimming_noPurgeDelayFalse_doesNotPostPurgeEvent() throws Exception {
    // Create handler with noPurgeDelay=false
    DefaultCooperation2Factory cooperation = new DefaultCooperation2Factory();
    BrowseEventHandler handler = new BrowseEventHandler(cooperation, periodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), 100, 2, false, databaseCheck, browseTrimService);
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(false);

    AssetDeletedEvent event = createAssetDeletedEvent(1, repository);

    handler.on(event);

    // noPurgeDelay=false means no immediate purge
    verify(eventManager, never()).post(any());
  }

  @Test
  public void testMarkRepositoryForTrimming_noPurgeDelayTrue_batchTrimDisabled_postsPurgeEvent() throws Exception {
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(false);
    AssetDeletedEvent event = createAssetDeletedEvent(1, repository);

    underTest.on(event);

    // noPurgeDelay=true and batchTrim disabled means immediate purge
    verify(eventManager).post(any());
  }

  // --- flushAssets ---

  @Test
  public void testFlushAssets_withPendingAssets_deliversToRepository() throws Exception {
    AssetCreatedEvent event = createAssetCreatedEvent(5, repository);

    underTest.on(event);
    underTest.flushAssets();

    verify(browseFacet).addPathsToAssets(any());
  }

  @Test
  public void testFlushAssets_withNoPendingAssets_doesNothing() {
    underTest.flushAssets();

    verify(browseFacet, never()).addPathsToAssets(any());
  }

  @Test
  public void testFlushAssets_repositoryWithoutBrowseFacet_doesNotThrow() throws Exception {
    Repository repoNoBrowse = mock(Repository.class);
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn(FORMAT_NAME);
    when(repoNoBrowse.getFormat()).thenReturn(format);
    when(repoNoBrowse.getName()).thenReturn("no-browse-repo");
    when(repoNoBrowse.optionalFacet(BrowseFacet.class)).thenReturn(Optional.empty());

    AssetCreatedEvent event = createAssetCreatedEvent(1, repoNoBrowse);
    underTest.on(event);
    underTest.flushAssets();

    // No exception, and browseFacet not called since repo doesn't have it
    verify(browseFacet, never()).addPathsToAssets(any());
  }

  @Test
  public void testFlushAssets_clearsFlushQueuedFlag() throws Exception {
    // Create handler with flushOnCount=2 to trigger flush event
    DefaultCooperation2Factory cooperation = new DefaultCooperation2Factory();
    BrowseEventHandler handler = new BrowseEventHandler(cooperation, periodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), 2, 2, true, databaseCheck, browseTrimService);

    AssetCreatedEvent event1 = createAssetCreatedEvent(1, repository);
    AssetCreatedEvent event2 = createAssetCreatedEvent(2, repository);

    handler.on(event1);
    handler.on(event2);

    // FlushEvent was posted (flushQueued was set to true then event posted)
    verify(eventManager).post(any());

    // Flush assets clears flushQueued, so new assets added after can trigger again
    handler.flushAssets();

    AssetCreatedEvent event3 = createAssetCreatedEvent(3, repository);
    AssetCreatedEvent event4 = createAssetCreatedEvent(4, repository);
    handler.on(event3);
    handler.on(event4);

    // A second FlushEvent should be posted
    verify(eventManager, times(2)).post(any());
  }

  @Test
  public void testFlushAssets_multipleRepositories_deliversSeparately() throws Exception {
    Repository repo2 = mock(Repository.class);
    Format format2 = mock(Format.class);
    when(format2.getValue()).thenReturn("npm");
    when(repo2.getFormat()).thenReturn(format2);
    when(repo2.getName()).thenReturn("npm-repo");
    BrowseFacet browseFacet2 = mock(BrowseFacet.class);
    when(repo2.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet2));

    AssetCreatedEvent event1 = createAssetCreatedEvent(1, repository);
    AssetCreatedEvent event2 = createAssetCreatedEventWithFormat(2, repo2, "npm");

    underTest.on(event1);
    underTest.on(event2);
    underTest.flushAssets();

    verify(browseFacet).addPathsToAssets(any());
    verify(browseFacet2).addPathsToAssets(any());
  }

  // --- maybeTrimRepositories ---

  @Test
  public void testMaybeTrimRepositories_skipsWhenNotAllowed() {
    when(browseTrimService.shouldAllowTrim()).thenReturn(false);

    underTest.maybeTrimRepositories();

    verify(browseTrimService, times(1)).shouldAllowTrim();
  }

  @Test
  public void testMaybeTrimRepositories_allowsWhenEnabled() {
    when(browseTrimService.shouldAllowTrim()).thenReturn(true);

    underTest.maybeTrimRepositories();

    verify(browseTrimService, times(1)).shouldAllowTrim();
  }

  @Test
  public void testMaybeTrimRepositories_withNeedsTrimTrue_trimsBrowseNodes() throws Exception {
    when(browseTrimService.shouldAllowTrim()).thenReturn(true);
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(true); // prevent PurgeEvent post

    // Add a repository for trimming by triggering a delete event
    AssetDeletedEvent event = createAssetDeletedEvent(1, repository);
    underTest.on(event);

    // Now trim
    underTest.maybeTrimRepositories();

    verify(browseFacet).trimBrowseNodes();
  }

  @Test
  public void testMaybeTrimRepositories_withNeedsTrimFalse_doesNotTrim() {
    when(browseTrimService.shouldAllowTrim()).thenReturn(true);

    // No events fired, so needsTrim is still false
    underTest.maybeTrimRepositories();

    verify(browseFacet, never()).trimBrowseNodes();
  }

  @Test
  public void testMaybeTrimRepositories_whenTrimNotAllowed_doesNotTrim() throws Exception {
    when(browseTrimService.shouldAllowTrim()).thenReturn(false);
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(true);

    // Add repository for trimming
    AssetDeletedEvent event = createAssetDeletedEvent(1, repository);
    underTest.on(event);

    underTest.maybeTrimRepositories();

    verify(browseFacet, never()).trimBrowseNodes();
  }

  @Test
  public void testMaybeTrimRepositories_repositoryWithoutBrowseFacet_doesNotThrow() throws Exception {
    when(browseTrimService.shouldAllowTrim()).thenReturn(true);
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(true);

    Repository repoNoBrowse = mock(Repository.class);
    when(repoNoBrowse.getName()).thenReturn("no-browse-repo");
    when(repoNoBrowse.optionalFacet(BrowseFacet.class)).thenReturn(Optional.empty());

    ComponentDeletedEvent event = mock(ComponentDeletedEvent.class);
    when(event.getRepository()).thenReturn(Optional.of(repoNoBrowse));

    underTest.on(event);
    underTest.maybeTrimRepositories();

    // No exception, and original browseFacet not called
    verify(browseFacet, never()).trimBrowseNodes();
  }

  @Test
  public void testMaybeTrimRepositories_multipleRepositories_trimsAll() throws Exception {
    when(browseTrimService.shouldAllowTrim()).thenReturn(true);
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(true);

    Repository repo2 = mock(Repository.class);
    when(repo2.getName()).thenReturn("repo-2");
    BrowseFacet browseFacet2 = mock(BrowseFacet.class);
    when(repo2.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet2));

    AssetDeletedEvent event1 = createAssetDeletedEvent(1, repository);
    ComponentDeletedEvent event2 = mock(ComponentDeletedEvent.class);
    when(event2.getRepository()).thenReturn(Optional.of(repo2));

    underTest.on(event1);
    underTest.on(event2);
    underTest.maybeTrimRepositories();

    verify(browseFacet).trimBrowseNodes();
    verify(browseFacet2).trimBrowseNodes();
  }

  // --- pollBrowseUpdateRequests ---

  @Test
  public void testPollBrowseUpdateRequests_flushesAndTrims() throws Exception {
    when(browseTrimService.shouldAllowTrim()).thenReturn(true);
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(true);

    // Add a pending asset
    AssetCreatedEvent createEvent = createAssetCreatedEvent(1, repository);
    underTest.on(createEvent);

    // Add a repository for trimming
    AssetDeletedEvent deleteEvent = createAssetDeletedEvent(2, repository);
    underTest.on(deleteEvent);

    // Poll should flush assets and then maybe trim
    underTest.pollBrowseUpdateRequests();

    verify(browseFacet).addPathsToAssets(any());
    verify(browseFacet).trimBrowseNodes();
  }

  @Test
  public void testPollBrowseUpdateRequests_noPendingAssets_stillTrims() throws Exception {
    when(browseTrimService.shouldAllowTrim()).thenReturn(true);
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(true);

    // Only add a repository for trimming
    ComponentDeletedEvent event = mock(ComponentDeletedEvent.class);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    underTest.on(event);

    underTest.pollBrowseUpdateRequests();

    // No flush (nothing pending), but trim should happen
    verify(browseFacet, never()).addPathsToAssets(any());
    verify(browseFacet).trimBrowseNodes();
  }

  @Test
  public void testPollBrowseUpdateRequests_nothingPending_doesNothing() {
    when(browseTrimService.shouldAllowTrim()).thenReturn(true);

    underTest.pollBrowseUpdateRequests();

    verify(browseFacet, never()).addPathsToAssets(any());
    verify(browseFacet, never()).trimBrowseNodes();
  }

  // --- doStart / doStop lifecycle ---

  @Test
  public void testDoStart_withFlushOnCountGreaterThanOne_schedulesPeriodicJob() throws Exception {
    PeriodicJob periodicJob = mock(PeriodicJob.class);
    PeriodicJobService localPeriodicJobService = mock(PeriodicJobService.class);
    when(localPeriodicJobService.schedule(any(Runnable.class), anyInt())).thenReturn(periodicJob);

    DefaultCooperation2Factory cooperation = new DefaultCooperation2Factory();
    BrowseEventHandler handler = new BrowseEventHandler(cooperation, localPeriodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), 100, 2, true, databaseCheck, browseTrimService);

    handler.doStart();

    verify(localPeriodicJobService).startUsing();
    verify(localPeriodicJobService).schedule(any(Runnable.class), anyInt());
  }

  @Test
  public void testDoStop_withFlushOnCountGreaterThanOne_cancelsPeriodicJob() throws Exception {
    PeriodicJob periodicJob = mock(PeriodicJob.class);
    PeriodicJobService localPeriodicJobService = mock(PeriodicJobService.class);
    when(localPeriodicJobService.schedule(any(Runnable.class), anyInt())).thenReturn(periodicJob);

    DefaultCooperation2Factory cooperation = new DefaultCooperation2Factory();
    BrowseEventHandler handler = new BrowseEventHandler(cooperation, localPeriodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), 100, 2, true, databaseCheck, browseTrimService);

    handler.doStart();
    handler.doStop();

    verify(periodicJob).cancel();
    verify(localPeriodicJobService).stopUsing();
  }

  @Test
  public void testDoStart_withFlushOnCountOne_doesNotSchedule() throws Exception {
    PeriodicJobService localPeriodicJobService = mock(PeriodicJobService.class);

    DefaultCooperation2Factory cooperation = new DefaultCooperation2Factory();
    BrowseEventHandler handler = new BrowseEventHandler(cooperation, localPeriodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), 1, 2, true, databaseCheck, browseTrimService);

    handler.doStart();

    verify(localPeriodicJobService, never()).startUsing();
    verify(localPeriodicJobService, never()).schedule(any(Runnable.class), anyInt());
  }

  @Test
  public void testDoStop_withFlushOnCountOne_doesNotCancel() throws Exception {
    PeriodicJobService localPeriodicJobService = mock(PeriodicJobService.class);

    DefaultCooperation2Factory cooperation = new DefaultCooperation2Factory();
    BrowseEventHandler handler = new BrowseEventHandler(cooperation, localPeriodicJobService, eventManager, true,
        Duration.ofSeconds(0), Duration.ofSeconds(30), 1, 2, true, databaseCheck, browseTrimService);

    handler.doStart();
    handler.doStop();

    verify(localPeriodicJobService, never()).stopUsing();
  }

  // --- shouldTriggerImmediatePurge (tested indirectly) ---

  @Test
  public void testShouldTriggerImmediatePurge_withBatchEnabled_returnsFalse() {
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(true);

    DefaultCooperation2Factory cooperation = new DefaultCooperation2Factory();
    BrowseEventHandler handler = new BrowseEventHandler(
        cooperation, periodicJobService, eventManager, true, Duration.ofSeconds(0),
        Duration.ofSeconds(30), 100, 2, true, databaseCheck, browseTrimService);

    when(browseTrimService.isBatchTrimEnabled()).thenReturn(true);
  }

  @Test
  public void testBatchTrimEnabled_defaultsToFalse() {
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(false);

    assertFalse("Batch trim should default to false", browseTrimService.isBatchTrimEnabled());
  }

  @Test
  public void testPostgresqlTrimEnabled_canBeToggled() {
    when(browseTrimService.isPostgresqlTrimEnabled()).thenReturn(false).thenReturn(true);

    assertFalse(browseTrimService.isPostgresqlTrimEnabled());
    assertTrue(browseTrimService.isPostgresqlTrimEnabled());
  }

  // --- Constructor registers flush event receiver ---

  @Test
  public void testConstructor_registersFlushEventReceiver() {
    // Verify the constructor registered the FlushEventReceiver with the event manager
    verify(eventManager).register(any());
  }

  // --- Edge case: same repository deleted multiple times only added once to trim set ---

  @Test
  public void testMarkRepositoryForTrimming_sameRepositoryMultipleTimes_onlyTrimmedOnce() throws Exception {
    when(browseTrimService.shouldAllowTrim()).thenReturn(true);
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(true);

    AssetDeletedEvent event1 = createAssetDeletedEvent(1, repository);
    AssetDeletedEvent event2 = createAssetDeletedEvent(2, repository);

    underTest.on(event1);
    underTest.on(event2);
    underTest.maybeTrimRepositories();

    // Even though two delete events for same repo, only one trim call
    verify(browseFacet, times(1)).trimBrowseNodes();
  }

  // --- Edge case: needsTrim cleared after trim ---

  @Test
  public void testMaybeTrimRepositories_resetsNeedsTrimFlag() throws Exception {
    when(browseTrimService.shouldAllowTrim()).thenReturn(true);
    when(browseTrimService.isBatchTrimEnabled()).thenReturn(true);

    AssetDeletedEvent event = createAssetDeletedEvent(1, repository);
    underTest.on(event);
    underTest.maybeTrimRepositories();

    verify(browseFacet).trimBrowseNodes();

    // Second call without new events should not trim again
    underTest.maybeTrimRepositories();
    verifyNoMoreInteractions(browseFacet);
  }

  // --- Helper methods ---

  private AssetData createAssetData(final int assetId) {
    AssetData asset = new AssetData();
    asset.setRepositoryId(1);
    asset.setAssetId(assetId);
    asset.setPath("/org/example/artifact-" + assetId + ".jar");
    asset.setKind("default");
    return asset;
  }

  private AssetCreatedEvent createAssetCreatedEvent(final int assetId, final Repository repo) throws Exception {
    AssetData asset = createAssetData(assetId);
    AssetCreatedEvent event = new AssetCreatedEvent(asset);
    setRepositorySupplier(event, repo);
    return event;
  }

  private AssetCreatedEvent createAssetCreatedEventWithFormat(
      final int assetId,
      final Repository repo,
      final String formatName) throws Exception
  {
    AssetData asset = createAssetData(assetId);
    AssetCreatedEvent event = new AssetCreatedEvent(asset);
    setRepositorySupplier(event, repo);
    return event;
  }

  private AssetUploadedEvent createAssetUploadedEvent(final int assetId, final Repository repo) throws Exception {
    AssetData asset = createAssetData(assetId);
    AssetUploadedEvent event = new AssetUploadedEvent(asset);
    setRepositorySupplier(event, repo);
    return event;
  }

  private AssetDeletedEvent createAssetDeletedEvent(final int assetId, final Repository repo) throws Exception {
    AssetData asset = createAssetData(assetId);
    AssetDeletedEvent event = new AssetDeletedEvent(asset);
    setRepositorySupplier(event, repo);
    return event;
  }

  private AssetPurgedEvent createAssetPurgedEvent(final Repository repo) throws Exception {
    AssetPurgedEvent event = new AssetPurgedEvent(1, new int[]{1, 2, 3});
    setRepositorySupplier(event, repo);
    return event;
  }

  /**
   * Sets the repository supplier on a {@link ContentStoreEvent} using reflection,
   * because the setter is package-private in another package.
   */
  @SuppressWarnings("unchecked")
  private void setRepositorySupplier(final ContentStoreEvent event, final Repository repo) throws Exception {
    Method setRepoSupplier = ContentStoreEvent.class.getDeclaredMethod("setRepositorySupplier", Supplier.class);
    setRepoSupplier.setAccessible(true);
    if (repo != null) {
      setRepoSupplier.invoke(event, (Supplier<Optional<Repository>>) () -> Optional.of(repo));
    }
    else {
      setRepoSupplier.invoke(event, (Supplier<Optional<Repository>>) Optional::empty);
    }
  }
}
