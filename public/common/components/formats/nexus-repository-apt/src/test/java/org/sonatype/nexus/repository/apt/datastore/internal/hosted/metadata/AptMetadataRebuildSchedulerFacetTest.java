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
package org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.cooperation2.datastore.DefaultCooperation2Factory;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.time.Clock;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.internal.metadata.AptMetadataFacetSupport;
import org.sonatype.nexus.repository.apt.datastore.internal.metadata.AptMetadataRebuildSchedulerFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.metadata.AutomatedAptMetadataRebuildTaskDescriptor;
import org.sonatype.nexus.repository.apt.internal.AptProperties;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUpdatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.ExternalTaskState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.schedule.Once;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link AptMetadataRebuildSchedulerFacet}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AptMetadataRebuildSchedulerFacetTest

{
  private static final String REPO_NAME = "apt-hosted";

  private static final String OTHER_REPO_NAME = "other-apt-hosted";

  private static final Duration REBUILD_DELAY = Duration.ofSeconds(2);

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private Clock clock;

  @Mock
  private Repository repository;

  @Mock
  private Repository otherRepository;

  @Mock
  private Format aptFormat;

  @Mock
  private Type hostedType;

  @Mock
  private TaskInfo taskInfo;

  @Mock
  private TaskInfo runningTaskInfo;

  @Mock
  private TaskConfiguration taskConfiguration;

  @Mock
  private ExternalTaskState waitingState;

  @Mock
  private ExternalTaskState runningState;

  @Mock
  private ExternalTaskState completedState;

  @Mock
  private AptMetadataFacetSupport metadataFacet;

  @Mock
  private AptSigningFacet signingFacet;

  @Mock
  private EventManager eventManager;

  private AptMetadataRebuildSchedulerFacet underTest;

  @Before
  public void setUp() throws Exception {
    // Setup format and type
    when(aptFormat.getValue()).thenReturn(AptFormat.NAME);
    when(hostedType.getValue()).thenReturn(HostedType.NAME);

    // Set up the repository
    when(repository.getName()).thenReturn(REPO_NAME);
    when(repository.getFormat()).thenReturn(aptFormat);
    when(repository.getType()).thenReturn(hostedType);
    when(repository.facet(AptMetadataFacetSupport.class)).thenReturn(metadataFacet);
    when(repository.facet(AptSigningFacet.class)).thenReturn(signingFacet);
    when(signingFacet.isConfigured()).thenReturn(true);

    // Set up another repository (for cross-repo event tests)
    when(otherRepository.getName()).thenReturn(OTHER_REPO_NAME);
    when(otherRepository.getFormat()).thenReturn(aptFormat);
    when(otherRepository.getType()).thenReturn(hostedType);

    // Setup clock
    when(clock.clusterTime()).thenReturn(OffsetDateTime.now());

    // Setup task scheduler
    when(taskScheduler.createTaskConfigurationInstance(AutomatedAptMetadataRebuildTaskDescriptor.TYPE_ID))
        .thenReturn(new TaskConfiguration());
    when(taskScheduler.listsTasks()).thenReturn(List.of());
    when(taskScheduler.scheduleTask(any(TaskConfiguration.class), any(Once.class))).thenReturn(taskInfo);

    // Setup task info
    when(taskInfo.getTypeId()).thenReturn(AutomatedAptMetadataRebuildTaskDescriptor.TYPE_ID);
    when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);
    when(taskConfiguration.getString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID))
        .thenReturn(REPO_NAME);

    // Setup task states
    when(waitingState.getState()).thenReturn(TaskState.WAITING);
    when(runningState.getState()).thenReturn(TaskState.RUNNING);
    when(completedState.getState()).thenReturn(TaskState.OK);
    when(taskScheduler.toExternalTaskState(taskInfo)).thenReturn(waitingState);

    // Create a facet with real Cooperation2Factory using anonymous subclass to access protected constructor
    Cooperation2Factory cooperationFactory = new DefaultCooperation2Factory();
    underTest = new AptMetadataRebuildSchedulerFacet(
        taskScheduler,
        cooperationFactory,
        clock,
        REBUILD_DELAY,
        true, // cooperation enabled
        Duration.ZERO,
        Duration.ofSeconds(30),
        100)
    {
    };

    // Inject EventManager dependency required by FacetSupport
    underTest.installDependencies(eventManager);

    // Attach to the repository, init, and start (full lifecycle)
    underTest.attach(repository);
    underTest.init();
    underTest.start();
  }

  @Test
  public void testAssetCreatedEvent_SameRepository_SchedulesRebuild() {
    Asset asset = mockDebAsset();
    AssetCreatedEvent event = mockAssetCreatedEvent(repository, asset);

    underTest.on(event);

    verify(taskScheduler).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testAssetCreatedEvent_DifferentRepository_IgnoresEvent() {
    Asset asset = mockDebAsset();
    AssetCreatedEvent event = mockAssetCreatedEvent(otherRepository, asset);

    underTest.on(event);

    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testAssetCreatedEvent_NonDebAsset_IgnoresEvent() {
    Asset asset = mockNonDebAsset();
    AssetCreatedEvent event = mockAssetCreatedEvent(repository, asset);

    underTest.on(event);

    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testAssetDeletedEvent_SameRepository_SchedulesRebuild() {
    Asset asset = mockDebAsset();
    AssetDeletedEvent event = mockAssetDeletedEvent(repository, asset);

    underTest.on(event);

    verify(taskScheduler).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testAssetDeletedEvent_DifferentRepository_IgnoresEvent() {
    Asset asset = mockDebAsset();
    AssetDeletedEvent event = mockAssetDeletedEvent(otherRepository, asset);

    underTest.on(event);

    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testAssetUpdatedEvent_SameRepository_SchedulesRebuild() {
    Asset asset = mockDebAsset();
    AssetUpdatedEvent event = mockAssetUpdatedEvent(repository, asset);

    underTest.on(event);

    verify(taskScheduler).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testAssetUpdatedEvent_DifferentRepository_IgnoresEvent() {
    Asset asset = mockDebAsset();
    AssetUpdatedEvent event = mockAssetUpdatedEvent(otherRepository, asset);

    underTest.on(event);

    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testAssetPurgedEvent_SameRepository_SchedulesRebuild() {
    AssetPurgedEvent event = mockAssetPurgedEvent(repository);

    underTest.on(event);

    verify(taskScheduler).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testAssetPurgedEvent_DifferentRepository_IgnoresEvent() {
    AssetPurgedEvent event = mockAssetPurgedEvent(otherRepository);

    underTest.on(event);

    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testComponentPurgedEvent_SameRepository_SchedulesRebuild() {
    FluentAsset asset = mockFluentDebAsset();
    ComponentPurgedEvent event = mockComponentPurgedEvent(repository, List.of(asset));

    underTest.on(event);

    // Should call removePackageMetadata and schedule rebuild
    verify(metadataFacet).removePackageMetadata(asset);
    verify(taskScheduler).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testComponentPurgedEvent_DifferentRepository_IgnoresEvent() {
    FluentAsset asset = mockFluentDebAsset();
    ComponentPurgedEvent event = mockComponentPurgedEvent(otherRepository, List.of(asset));

    underTest.on(event);

    verify(metadataFacet, never()).removePackageMetadata(any());
    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testMaybeScheduleRebuild_NoExistingTask_SchedulesNew() {
    when(taskScheduler.listsTasks()).thenReturn(List.of());

    underTest.maybeScheduleRebuild();

    verify(taskScheduler).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testMaybeScheduleRebuild_ExistingWaitingTask_ReusesTask() {
    when(taskScheduler.listsTasks()).thenReturn(List.of(taskInfo));
    when(taskScheduler.toExternalTaskState(taskInfo)).thenReturn(waitingState);

    underTest.maybeScheduleRebuild();

    // Should not schedule a new task since one is already waiting
    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testMaybeScheduleRebuild_ExistingRunningTask_SchedulesFollowUp() {
    // Set up a running task (not waiting)
    when(runningTaskInfo.getTypeId()).thenReturn(AutomatedAptMetadataRebuildTaskDescriptor.TYPE_ID);
    TaskConfiguration runningConfig = mock(TaskConfiguration.class);
    when(runningConfig.getString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID))
        .thenReturn(REPO_NAME);
    when(runningTaskInfo.getConfiguration()).thenReturn(runningConfig);
    when(taskScheduler.toExternalTaskState(runningTaskInfo)).thenReturn(runningState);
    when(taskScheduler.listsTasks()).thenReturn(List.of(runningTaskInfo));

    underTest.maybeScheduleRebuild();

    // Should schedule a follow-up task since only a running task exists
    verify(taskScheduler).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testMaybeScheduleRebuild_TaskForDifferentRepository_SchedulesNew() {
    // Set up a task for a different repository
    TaskConfiguration otherConfig = mock(TaskConfiguration.class);
    when(otherConfig.getString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID))
        .thenReturn(OTHER_REPO_NAME);
    when(taskInfo.getConfiguration()).thenReturn(otherConfig);
    when(taskScheduler.listsTasks()).thenReturn(List.of(taskInfo));

    underTest.maybeScheduleRebuild();

    // Should schedule a new task since existing task is for different repo
    verify(taskScheduler).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testMaybeScheduleRebuild_MultipleEvents_OnlyOneTaskScheduled() {
    // The first call will schedule a task
    when(taskScheduler.listsTasks()).thenReturn(List.of());
    underTest.maybeScheduleRebuild();

    // Now simulate the task exists
    when(taskScheduler.listsTasks()).thenReturn(List.of(taskInfo));

    // The second call should not schedule another task
    underTest.maybeScheduleRebuild();
    underTest.maybeScheduleRebuild();
    underTest.maybeScheduleRebuild();

    // Only one task should be scheduled
    verify(taskScheduler, times(1)).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testScheduledTask_HasCorrectConfiguration() {
    ArgumentCaptor<TaskConfiguration> configCaptor = ArgumentCaptor.forClass(TaskConfiguration.class);

    underTest.maybeScheduleRebuild();

    verify(taskScheduler).scheduleTask(configCaptor.capture(), any(Once.class));
    TaskConfiguration config = configCaptor.getValue();

    assertThat(config.getString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID), is(REPO_NAME));
    assertThat(config.getName(), containsString(REPO_NAME));
  }

  @Test
  public void testMaybeScheduleRebuild_WaitingAndRunningTasks_ReusesWaiting() {
    // Set up both a running and a waiting task
    TaskConfiguration waitingConfig = mock(TaskConfiguration.class);
    when(waitingConfig.getString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID))
        .thenReturn(REPO_NAME);
    when(taskInfo.getConfiguration()).thenReturn(waitingConfig);
    when(taskScheduler.toExternalTaskState(taskInfo)).thenReturn(waitingState);

    TaskConfiguration runningConfig = mock(TaskConfiguration.class);
    when(runningConfig.getString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID))
        .thenReturn(REPO_NAME);
    when(runningTaskInfo.getTypeId()).thenReturn(AutomatedAptMetadataRebuildTaskDescriptor.TYPE_ID);
    when(runningTaskInfo.getConfiguration()).thenReturn(runningConfig);
    when(taskScheduler.toExternalTaskState(runningTaskInfo)).thenReturn(runningState);

    when(taskScheduler.listsTasks()).thenReturn(List.of(runningTaskInfo, taskInfo));

    underTest.maybeScheduleRebuild();

    // Should not schedule since a waiting task exists
    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testAssetCreatedEvent_NonAptRepository_IgnoresEvent() {
    // Setup non-APT format
    Format mavenFormat = mock(Format.class);
    when(mavenFormat.getValue()).thenReturn("maven2");
    Repository mavenRepo = mock(Repository.class);
    when(mavenRepo.getName()).thenReturn("maven-repo");
    when(mavenRepo.getFormat()).thenReturn(mavenFormat);
    when(mavenRepo.getType()).thenReturn(hostedType);

    Asset asset = mockDebAsset();
    AssetCreatedEvent event = mockAssetCreatedEvent(mavenRepo, asset);

    underTest.on(event);

    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testAssetCreatedEvent_NonHostedRepository_IgnoresEvent() {
    // Set up a proxy type
    Type proxyType = mock(Type.class);
    when(proxyType.getValue()).thenReturn("proxy");
    Repository proxyRepo = mock(Repository.class);
    when(proxyRepo.getName()).thenReturn("apt-proxy");
    when(proxyRepo.getFormat()).thenReturn(aptFormat);
    when(proxyRepo.getType()).thenReturn(proxyType);

    Asset asset = mockDebAsset();
    AssetCreatedEvent event = mockAssetCreatedEvent(proxyRepo, asset);

    underTest.on(event);

    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testRepositoryUpdatedEvent_SigningAdded_SchedulesRebuild() {
    Configuration oldConfig = mockConfigurationWithoutSigning();
    Configuration newConfig = mockConfigurationWithSigning("test-keypair");

    when(repository.getConfiguration()).thenReturn(newConfig);
    when(signingFacet.isConfigured()).thenReturn(true);
    RepositoryUpdatedEvent event = new RepositoryUpdatedEvent(repository, oldConfig);

    underTest.on(event);

    verify(taskScheduler).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testRepositoryUpdatedEvent_SigningRemoved_NoRebuild() {
    Configuration oldConfig = mockConfigurationWithSigning("test-keypair");
    Configuration newConfig = mockConfigurationWithoutSigning();

    when(repository.getConfiguration()).thenReturn(newConfig);
    when(signingFacet.isConfigured()).thenReturn(false); // Signing now disabled
    RepositoryUpdatedEvent event = new RepositoryUpdatedEvent(repository, oldConfig);

    underTest.on(event);

    // Handler exits early when newKeypair is null (signing removed) - no log, no maybeScheduleRebuild() call
    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testRepositoryUpdatedEvent_KeyChanged_SchedulesRebuild() {
    Configuration oldConfig = mockConfigurationWithSigning("keypair-1");
    Configuration newConfig = mockConfigurationWithSigning("keypair-2");

    when(repository.getConfiguration()).thenReturn(newConfig);
    when(signingFacet.isConfigured()).thenReturn(true);
    RepositoryUpdatedEvent event = new RepositoryUpdatedEvent(repository, oldConfig);

    underTest.on(event);

    verify(taskScheduler).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testRepositoryUpdatedEvent_NoSigningChange_NoRebuild() {
    Configuration oldConfig = mockConfigurationWithSigning("test-keypair");
    Configuration newConfig = mockConfigurationWithSigning("test-keypair");

    when(repository.getConfiguration()).thenReturn(newConfig);
    when(signingFacet.isConfigured()).thenReturn(true);
    RepositoryUpdatedEvent event = new RepositoryUpdatedEvent(repository, oldConfig);

    underTest.on(event);

    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testRepositoryUpdatedEvent_DifferentRepository_IgnoresEvent() {
    Configuration oldConfig = mockConfigurationWithoutSigning();
    Configuration newConfig = mockConfigurationWithSigning("test-keypair");

    RepositoryUpdatedEvent event = new RepositoryUpdatedEvent(otherRepository, oldConfig);

    underTest.on(event);

    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testRepositoryUpdatedEvent_PassphraseOnlyChanged_NoRebuild() {
    Configuration oldConfig = mockConfigurationWithSigning("test-keypair", "pass1");
    Configuration newConfig = mockConfigurationWithSigning("test-keypair", "pass2");

    when(repository.getConfiguration()).thenReturn(newConfig);
    when(signingFacet.isConfigured()).thenReturn(true);
    RepositoryUpdatedEvent event = new RepositoryUpdatedEvent(repository, oldConfig);

    underTest.on(event);

    // Same keypair, so no rebuild needed
    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  @Test
  public void testRepositoryUpdatedEvent_BlankKeypair_NoRebuild() {
    Configuration oldConfig = mockConfigurationWithoutSigning();
    Configuration newConfig = mockConfigurationWithBlankKeypair();

    when(repository.getConfiguration()).thenReturn(newConfig);
    when(signingFacet.isConfigured()).thenReturn(false); // Blank keypair = not configured
    RepositoryUpdatedEvent event = new RepositoryUpdatedEvent(repository, oldConfig);

    underTest.on(event);

    // Blank/whitespace keypair normalized to null, handler exits early
    verify(taskScheduler, never()).scheduleTask(any(TaskConfiguration.class), any(Once.class));
  }

  private Configuration mockConfigurationWithoutSigning() {
    Configuration config = mock(Configuration.class);
    when(config.getAttributes()).thenReturn(Collections.emptyMap());
    return config;
  }

  private Configuration mockConfigurationWithSigning(String keypair) {
    return mockConfigurationWithSigning(keypair, "passphrase");
  }

  private Configuration mockConfigurationWithSigning(String keypair, String passphrase) {
    Configuration config = mock(Configuration.class);
    Map<String, Object> signingAttrs = new HashMap<>();
    signingAttrs.put("keypair", keypair);
    signingAttrs.put("passphrase", passphrase);
    when(config.getAttributes()).thenReturn(
        Collections.singletonMap(AptSigningFacet.CONFIG_KEY, signingAttrs));
    return config;
  }

  private Configuration mockConfigurationWithBlankKeypair() {
    Configuration config = mock(Configuration.class);
    Map<String, Object> signingAttrs = new HashMap<>();
    signingAttrs.put("keypair", "   "); // Whitespace-only keypair
    signingAttrs.put("passphrase", "passphrase");
    when(config.getAttributes()).thenReturn(
        Collections.singletonMap(AptSigningFacet.CONFIG_KEY, signingAttrs));
    return config;
  }

  private Asset mockDebAsset() {
    Asset asset = mock(Asset.class);
    when(asset.kind()).thenReturn(AptProperties.DEB);
    when(asset.path()).thenReturn("/pool/main/h/hello/hello_1.0_amd64.deb");
    return asset;
  }

  private Asset mockNonDebAsset() {
    Asset asset = mock(Asset.class);
    when(asset.kind()).thenReturn("metadata");
    when(asset.path()).thenReturn("/dists/focal/Release");
    return asset;
  }

  private FluentAsset mockFluentDebAsset() {
    FluentAsset asset = mock(FluentAsset.class);
    when(asset.kind()).thenReturn(AptProperties.DEB);
    when(asset.path()).thenReturn("/pool/main/h/hello/hello_1.0_amd64.deb");
    return asset;
  }

  private AssetCreatedEvent mockAssetCreatedEvent(final Repository repo, final Asset asset) {
    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    when(event.getRepository()).thenReturn(Optional.of(repo));
    when(event.getAsset()).thenReturn(asset);
    return event;
  }

  private AssetDeletedEvent mockAssetDeletedEvent(final Repository repo, final Asset asset) {
    AssetDeletedEvent event = mock(AssetDeletedEvent.class);
    when(event.getRepository()).thenReturn(Optional.of(repo));
    when(event.getAsset()).thenReturn(asset);
    return event;
  }

  private AssetUpdatedEvent mockAssetUpdatedEvent(final Repository repo, final Asset asset) {
    AssetUpdatedEvent event = mock(AssetUpdatedEvent.class);
    when(event.getRepository()).thenReturn(Optional.of(repo));
    when(event.getAsset()).thenReturn(asset);
    return event;
  }

  private AssetPurgedEvent mockAssetPurgedEvent(final Repository repo) {
    AssetPurgedEvent event = mock(AssetPurgedEvent.class);
    when(event.getRepository()).thenReturn(Optional.of(repo));
    return event;
  }

  private ComponentPurgedEvent mockComponentPurgedEvent(final Repository repo, final List<FluentAsset> assets) {
    ComponentPurgedEvent event = mock(ComponentPurgedEvent.class);
    when(event.getRepository()).thenReturn(Optional.of(repo));
    when(event.getAssets()).thenReturn(assets);
    when(event.getComponentIds()).thenReturn(new int[]{1});
    return event;
  }
}
