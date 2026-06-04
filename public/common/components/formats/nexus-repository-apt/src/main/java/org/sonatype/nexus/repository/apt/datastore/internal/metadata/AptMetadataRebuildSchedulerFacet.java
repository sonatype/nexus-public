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
package org.sonatype.nexus.repository.apt.datastore.internal.metadata;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.time.Clock;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.internal.AptProperties;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUpdatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Once;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for APT metadata rebuild scheduler facets.
 * Provides common event handling, task scheduling, and coordination logic for both hosted and proxy repositories.
 * <p>
 * This facet listens to repository events (asset/component creation, updates, deletions) and automatically
 * schedules metadata rebuild tasks with debouncing and cluster-safe coordination via Cooperation2.
 */
@Component
@Exposed
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AptMetadataRebuildSchedulerFacet
    extends FacetSupport
    implements EventAware.Asynchronous
{
  private final TaskScheduler taskScheduler;

  private final Cooperation2Factory.Builder cooperationBuilder;

  private Cooperation2 cooperation;

  private final Duration rebuildDelay;

  private final Clock clock;

  /**
   * Constructs the scheduler facet with required dependencies.
   *
   * @param taskScheduler the task scheduler for creating and managing rebuild tasks
   * @param cooperationFactory factory for creating cooperation instances
   * @param clock clock for scheduling
   * @param rebuildDelay delay before scheduling rebuild (debouncing)
   * @param cooperationEnabled whether cooperation is enabled
   * @param majorTimeout cooperation major timeout
   * @param minorTimeout cooperation minor timeout
   * @param threadsPerKey cooperation threads per key
   */
  @Autowired
  protected AptMetadataRebuildSchedulerFacet(
      final TaskScheduler taskScheduler,
      final Cooperation2Factory cooperationFactory,
      final Clock clock,
      @Value("${nexus.apt.metadata.rebuild.debounce:2s}") final Duration rebuildDelay,
      @Value("${nexus.apt.metadata.cooperation.enabled:true}") final boolean cooperationEnabled,
      @Value("${nexus.apt.metadata.cooperation.majorTimeout:0s}") final Duration majorTimeout,
      @Value("${nexus.apt.metadata.cooperation.minorTimeout:30s}") final Duration minorTimeout,
      @Value("${nexus.apt.metadata.cooperation.threadsPerKey:100}") final int threadsPerKey)
  {
    this.taskScheduler = checkNotNull(taskScheduler);
    this.clock = checkNotNull(clock);

    checkArgument(!rebuildDelay.isNegative(), "nexus.apt.metadata.rebuild.debounce must be positive");
    this.rebuildDelay = rebuildDelay;

    this.cooperationBuilder = checkNotNull(cooperationFactory).configure()
        .enabled(cooperationEnabled)
        .majorTimeout(majorTimeout)
        .minorTimeout(minorTimeout)
        .threadsPerKey(threadsPerKey);
  }

  @Override
  protected void doStart() {
    String repositoryName = getRepository().getName();
    cooperation = cooperationBuilder.build(getClass(), repositoryName);
    log.info("AptMetadataRebuildSchedulerFacet started for repository: {}", repositoryName);
  }

  /**
   * Event handler for ComponentPurgedEvent.
   * Triggered when components are purged (e.g., by cleanup policies).
   * Removes package metadata from KV store before scheduling rebuild.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void on(final ComponentPurgedEvent event) {
    event.getRepository().ifPresent(repository -> {
      if (isTargetRepository(repository)) {
        if (event.getAssets() != null && !event.getAssets().isEmpty()) {
          log.debug("Removing metadata for {} purged assets", event.getAssets().size());
          AptMetadataFacetSupport metadataFacet = repository.facet(AptMetadataFacetSupport.class);
          event.getAssets().forEach(asset -> {
            if (isDebAsset(asset)) {
              log.debug("Removing metadata for purged asset: {}", asset.path());
              metadataFacet.removePackageMetadata(asset);
            }
          });
        }
        maybeScheduleRebuild();
      }
    });
  }

  /**
   * Event handler for AssetPurgedEvent.
   * This event is fired when assets WITHOUT components are purged in bulk.
   * For .deb packages (which have components), ComponentPurgedEvent is used instead.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetPurgedEvent event) {
    handleAssetEvent("AssetPurgedEvent", event.getRepository(), null);
  }

  /**
   * Event handler for AssetCreatedEvent.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetCreatedEvent event) {
    handleAssetEvent("AssetCreatedEvent", event.getRepository(), event.getAsset());
  }

  /**
   * Event handler for AssetDeletedEvent.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetDeletedEvent event) {
    handleAssetEvent("AssetDeletedEvent", event.getRepository(), event.getAsset());
  }

  /**
   * Event handler for AssetUpdatedEvent.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetUpdatedEvent event) {
    handleAssetEvent("AssetUpdatedEvent", event.getRepository(), event.getAsset());
  }

  /**
   * Event handler for RepositoryUpdatedEvent.
   * Detects changes to APT signing configuration and schedules metadata rebuild when:
   * <ul>
   * <li>Signing is added (passthrough → signing mode)</li>
   * <li>Signing key is changed (re-signing needed)</li>
   * </ul>
   * Does NOT schedule rebuild when signing is removed (signing → passthrough mode)
   * as the system will return to passing through upstream metadata unchanged.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryUpdatedEvent event) {
    final Repository repository = event.getRepository();

    // Only process events for this specific repository
    if (!isTargetRepository(repository)) {
      return;
    }

    Configuration oldConfig = event.getOldConfiguration();
    Configuration newConfig = repository.getConfiguration();

    if (hasSigningConfigChanged(oldConfig, newConfig)) {
      // Only log and schedule rebuild if signing is enabled in NEW config
      // If signing was removed, exit quietly without attempting rebuild
      String newKeypair = getSigningKeypair(newConfig);
      if (newKeypair != null) {
        log.info("APT signing configuration changed for repository: {}", repository.getName());
        maybeScheduleRebuild();
      }
    }
  }

  /**
   * Common handler for asset events (created, deleted, updated).
   */
  protected void handleAssetEvent(
      final String eventType,
      final Optional<Repository> eventRepository,
      final Asset asset)
  {
    eventRepository.ifPresent(repository -> {
      if (isTargetRepository(repository) && (asset == null || isDebAsset(asset))) {
        maybeScheduleRebuild();
      }
    });
  }

  /**
   * Schedule a metadata rebuild task if one is not already scheduled.
   * Uses Cooperation2 to ensure only one scheduler runs at a time.
   * Checks if rebuild should be scheduled (e.g., proxy checks signing configuration).
   */
  public void maybeScheduleRebuild() {
    String repositoryName = getRepository().getName();
    log.debug("Maybe scheduling rebuild for APT repository {}", repositoryName);

    if (!shouldScheduleRebuild()) {
      log.debug("Skipping rebuild schedule for repository {} - conditions not met", repositoryName);
      return;
    }

    try {
      TaskInfo taskInfo = cooperation.on(this::scheduleBuild)
          .checkFunction(this::getScheduledTask)
          .performWorkOnFail(false)
          .cooperate(repositoryName);

      log.debug("Found or scheduled task {}", taskInfo);
    }
    catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.warn("Failed to schedule rebuild of metadata for repository {}", repositoryName, e);
      }
      else {
        log.warn("Failed to schedule rebuild of metadata for repository {} cause: {}",
            repositoryName, e.getMessage());
      }
    }
  }

  private TaskInfo scheduleBuild() {
    log.debug("Attempting to schedule task for APT repository {}", getRepository().getName());

    // Check for existing waiting task
    Optional<TaskInfo> waitingTask = getScheduledTask();
    if (waitingTask.isPresent()) {
      log.debug("Found existing waiting rebuild task - keeping it");
      return waitingTask.get();
    }

    // No waiting task - create a new one
    return createTask();
  }

  private TaskInfo createTask() {
    String repositoryName = getRepository().getName();
    log.debug("Creating new rebuild task for APT repository {}", repositoryName);
    TaskConfiguration taskConfiguration =
        taskScheduler.createTaskConfigurationInstance(AutomatedAptMetadataRebuildTaskDescriptor.TYPE_ID);

    taskConfiguration.setName("Metadata rebuild for " + repositoryName);
    taskConfiguration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, repositoryName);

    long time = clock.clusterTime().plus(rebuildDelay).toInstant().toEpochMilli();
    return taskScheduler.scheduleTask(taskConfiguration, new Once(new Date(time)));
  }

  /**
   * Find an existing waiting task for this repository.
   */
  private Optional<TaskInfo> getScheduledTask() {
    return taskScheduler.listsTasks()
        .stream()
        .filter(task -> AutomatedAptMetadataRebuildTaskDescriptor.TYPE_ID.equals(task.getTypeId()))
        .filter(this::isForSameRepository)
        .filter(this::isTaskWaiting)
        .findAny();
  }

  /**
   * Check whether a task is for this repository.
   */
  private boolean isForSameRepository(final TaskInfo task) {
    return getRepository().getName()
        .equals(task
            .getConfiguration()
            .getString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID));
  }

  /**
   * Check whether the task is waiting to run (not done and not currently running).
   */
  private boolean isTaskWaiting(final TaskInfo task) {
    return taskScheduler.toExternalTaskState(task).getState().isWaiting();
  }

  private static boolean isDebAsset(final Asset asset) {
    return AptProperties.DEB.equals(asset.kind());
  }

  /**
   * Check if the repository is a target repository for this scheduler.
   *
   * @param repository the repository to check
   * @return true if this is a target repository
   */
  protected boolean isTargetRepository(final Repository repository) {
    return getRepository().getName().equals(repository.getName())
        && AptFormat.NAME.equals(repository.getFormat().getValue());
  }

  /**
   * Checks if rebuild should be scheduled.
   * Proxy repositories may check if signing is configured, hosted always returns true.
   *
   * @return true if rebuild should be scheduled
   */
  protected boolean shouldScheduleRebuild() {
    // Proxy repositories only rebuild if signing is configured
    // Without signing, upstream metadata is passed through unchanged
    AptSigningFacet facet = getRepository().facet(AptSigningFacet.class);
    return facet.isConfigured();
  }

  /**
   * Detects if the APT signing configuration has changed between old and new configurations.
   * Only compares the keypair field - passphrase changes do not require metadata rebuild.
   *
   * @param oldConfig the previous repository configuration (may be null)
   * @param newConfig the current repository configuration
   * @return true if signing keypair changed (added, removed, or modified)
   */
  private boolean hasSigningConfigChanged(@Nullable final Configuration oldConfig, final Configuration newConfig) {
    String oldKeypair = getSigningKeypair(oldConfig);
    String newKeypair = getSigningKeypair(newConfig);

    return !Objects.equals(oldKeypair, newKeypair);
  }

  /**
   * Extracts the signing keypair from repository configuration.
   * Normalizes empty/blank values to null.
   *
   * @param configuration the repository configuration (may be null)
   * @return the keypair string, or null if not configured or blank
   */
  @Nullable
  private String getSigningKeypair(@Nullable final Configuration configuration) {
    if (configuration == null) {
      return null;
    }

    Object signingConfigObj = configuration.getAttributes().get(AptSigningFacet.CONFIG_KEY);
    if (!(signingConfigObj instanceof Map)) {
      return null;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> signingConfig = (Map<String, Object>) signingConfigObj;
    String keypair = (String) signingConfig.get("keypair");

    if (keypair == null || keypair.trim().isEmpty()) {
      return null;
    }

    return keypair.trim();
  }
}
