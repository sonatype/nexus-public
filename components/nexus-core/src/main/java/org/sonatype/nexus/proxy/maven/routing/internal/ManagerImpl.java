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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.nexus.proxy.events.RepositoryItemEvent;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.AbstractMavenRepository;
import org.sonatype.nexus.proxy.maven.AbstractMavenRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.MavenGroupRepository;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.MavenShadowRepository;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.maven.routing.Config;
import org.sonatype.nexus.proxy.maven.routing.DiscoveryConfig;
import org.sonatype.nexus.proxy.maven.routing.DiscoveryStatus;
import org.sonatype.nexus.proxy.maven.routing.DiscoveryStatus.DStatus;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.maven.routing.PublishingStatus;
import org.sonatype.nexus.proxy.maven.routing.PublishingStatus.PStatus;
import org.sonatype.nexus.proxy.maven.routing.RoutingStatus;
import org.sonatype.nexus.proxy.maven.routing.discovery.DiscoveryResult;
import org.sonatype.nexus.proxy.maven.routing.discovery.DiscoveryResult.Outcome;
import org.sonatype.nexus.proxy.maven.routing.discovery.LocalContentDiscoverer;
import org.sonatype.nexus.proxy.maven.routing.discovery.RemoteContentDiscoverer;
import org.sonatype.nexus.proxy.maven.routing.discovery.RemoteStrategy;
import org.sonatype.nexus.proxy.maven.routing.events.PrefixFilePublishedRepositoryEvent;
import org.sonatype.nexus.proxy.maven.routing.events.PrefixFileUnpublishedRepositoryEvent;
import org.sonatype.nexus.proxy.maven.routing.internal.task.LoggingProgressListener;
import org.sonatype.nexus.proxy.maven.routing.internal.task.executor.ConstrainedExecutor;
import org.sonatype.nexus.proxy.maven.routing.internal.task.executor.ConstrainedExecutorImpl;
import org.sonatype.nexus.proxy.maven.routing.internal.task.executor.Statistics;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.threads.FakeAlmightySubject;
import org.sonatype.nexus.threads.NexusScheduledExecutorService;
import org.sonatype.nexus.threads.NexusThreadFactory;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.common.SimpleFormat;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation.
 * <p>
 * Notes on synchronization. As of 2.4,
 * <ul>
 * <li>periodic autorouting metadata updates are performed without holding any locks on path prefix file (i.e.
 * .meta/prefixes.txt). Instead, ConstrainedExecutor is used to avoid multiple concurrent execution of periodic
 * autorouting metadata updates. If new periodic update is requested while the previous one is already running, which
 * is
 * theoretically possible if update takes longer than update check period (1 hour as of 2.4), ConstrainedExecutor will
 * prevent second concurrent execution.</li>
 * <li>during period update existing metadata, if any, is kept available throughout update and is changed as one
 * relatively short prefix file update performed while holding write, i.e. exclusive, lock on the prefix file.</li>
 * <li>contents of the prefix file is cached in memory on first request and the cache is refreshed on each change to
 * the
 * prefix file. reading of the prefix file into the memory cache is performed while holding read, i.e. shared, lock,
 * which is necessary to prevent reading prefix file partially written by concurrently running prefix file update.</li>
 * <li>during repository deploy/delete operations, prefix file contents is first read while holding read lock, then, if
 * the operation results in changes to the prefix file, the prefix file is read-updated-written while holding write
 * long. This allows concurrent execution of multiple repository deploy/delete operations that do not require changes
 * to
 * the contents of the prefix file.</li>
 * </ul>
 * </p>
 *
 * @author cstamas
 * @since 2.4
 */
@Named
@Singleton
public class ManagerImpl
    extends ComponentSupport
    implements Manager
{
  private final EventBus eventBus;

  private final ApplicationStatusSource applicationStatusSource;

  private final ApplicationConfiguration applicationConfiguration;

  private final RepositoryRegistry repositoryRegistry;

  private final Config config;

  private final LocalContentDiscoverer localContentDiscoverer;

  private final RemoteContentDiscoverer remoteContentDiscoverer;

  private final RemoteStrategy quickRemoteStrategy;

  private final EventDispatcher eventDispatcher;

  /**
   * Plain executor for background batch-updates. This executor runs 1 periodic thread (see constructor) that
   * performs
   * periodic remote prefix list update, but also executes background "force" updates (initiated by user over REST or
   * when repository is added). But, as background threads are bounded by presence of proxy repositories, and
   * introduce hard limit of possible max executions, it protects this instance that is basically unbounded.
   */
  private final NexusScheduledExecutorService executor;

  /**
   * Executor used to execute update jobs. It is constrained in a way that no two update jobs will run against one
   * repository.
   */
  private final ConstrainedExecutor constrainedExecutor;

  /**
   * Da constructor.
   */
  @Inject
  public ManagerImpl(final EventBus eventBus, final ApplicationStatusSource applicationStatusSource,
                     final ApplicationConfiguration applicationConfiguration,
                     final RepositoryRegistry repositoryRegistry, final Config config,
                     final LocalContentDiscoverer localContentDiscoverer,
                     final RemoteContentDiscoverer remoteContentDiscoverer,
                     @Named(RemotePrefixFileStrategy.ID) final RemoteStrategy quickRemoteStrategy)
  {
    this.eventBus = checkNotNull(eventBus);
    this.applicationStatusSource = checkNotNull(applicationStatusSource);
    this.applicationConfiguration = checkNotNull(applicationConfiguration);
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
    this.config = checkNotNull(config);
    this.localContentDiscoverer = checkNotNull(localContentDiscoverer);
    this.remoteContentDiscoverer = checkNotNull(remoteContentDiscoverer);
    this.quickRemoteStrategy = checkNotNull(quickRemoteStrategy);
    final ScheduledThreadPoolExecutor target =
        new ScheduledThreadPoolExecutor(5, new NexusThreadFactory("ar", "AR-Updater"),
            new ThreadPoolExecutor.AbortPolicy());
    this.executor = NexusScheduledExecutorService.forFixedSubject(target, FakeAlmightySubject.TASK_SUBJECT);
    this.constrainedExecutor = new ConstrainedExecutorImpl(executor);
    // register event dispatcher
    this.eventDispatcher = new EventDispatcher(this);
    this.eventBus.register(this);
  }

  private volatile boolean periodicUpdaterDidRunAtLeastOnce = false;

  @Override
  public void startup() {
    if (config.isFeatureActive()) {
      // Send events about repositories with existing PrefixSource synchronously as part of #startup method
      // This allows proper initialization of components that need to track state of automatic routing
      for (MavenRepository mavenRepository : repositoryRegistry.getRepositoriesWithFacet(MavenRepository.class)) {
        if (isMavenRepositorySupported(mavenRepository)
            && mavenRepository.getLocalStatus().shouldServiceRequest()) {
          final FilePrefixSource prefixSource = getPrefixSourceFor(mavenRepository);
          if (prefixSource.exists()) {
            log.debug("Initializing prefix file of {}", mavenRepository);
            if (prefixSource.supported()) {
              eventBus.post(new PrefixFilePublishedRepositoryEvent(mavenRepository, prefixSource));
            }
            else {
              eventBus.post(new PrefixFileUnpublishedRepositoryEvent(mavenRepository));
            }
          }
        }
      }

      executor.scheduleAtFixedRate(new Runnable()
      {
        @Override
        public void run() {
          if (!applicationStatusSource.getSystemStatus().isNexusStarted()) {
            // this might happen on periodic call AFTER nexus shutdown was commenced
            // or BEFORE nexus booted, if some other plugin/subsystem delays boot for some
            // reason.
            // None of those is a problem, in latter case we will do what we need in next tick.
            // In former case,
            // we should not do anything anyway, we are being shut down.
            log.debug("Nexus not yet started, bailing out");
            return;
          }
          mayUpdateAllProxyPrefixFiles();
          periodicUpdaterDidRunAtLeastOnce = true;
        }
      }, 0L /* no initial delay */, TimeUnit.HOURS.toMillis(1), TimeUnit.MILLISECONDS);

      // register event dispatcher, to start receiving events
      eventBus.register(eventDispatcher);
    }
  }

  @Override
  public void shutdown() {
    if (config.isFeatureActive()) {
      eventBus.unregister(eventDispatcher);
    }
    executor.shutdown();
    constrainedExecutor.cancelAllJobs();
    try {
      if (!executor.awaitTermination(15L, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    }
    catch (InterruptedException e) {
      log.debug("Could not cleanly shut down", e);
    }
  }

  @Override
  public void initializePrefixFile(final MavenRepository mavenRepository) {
    log.debug("Initializing prefix file of newly added {}", mavenRepository);
    try {
      // spawn update, this will do whatever is needed (and handle cases like blocked, out of service etc),
      // and publish
      updatePrefixFile(mavenRepository);
      log.info("Initializing non-existing prefix file of newly added {}",
          RepositoryStringUtils.getHumanizedNameString(mavenRepository));
    }
    catch (Exception e) {
      log.warn("Problem during prefix file initialisation of newly added {}",
          RepositoryStringUtils.getHumanizedNameString(mavenRepository), e);
      try {
        unpublish(mavenRepository);
      }
      catch (IOException ioe) {
        // silently
      }
    }
  }

  /**
   * Method meant to be invoked on regular periods (like hourly, as we defined "resolution" of prefix list update
   * period in hours too), and will perform prefix list update only on those proxy repositories that needs it.
   */
  protected void mayUpdateAllProxyPrefixFiles() {
    log.trace("mayUpdateAllProxyPrefixFiles started");
    for (MavenRepository mavenRepository : repositoryRegistry.getRepositoriesWithFacet(MavenRepository.class)) {
      if (isMavenRepositorySupported(mavenRepository)) {
        try {
          final FilePrefixSource prefixSource = getPrefixSourceFor(mavenRepository);
          if (!prefixSource.exists()) {
            // automatic routing has not been initialized for this repository yet, for initialization.
            doUpdatePrefixFileAsync(true, mavenRepository);
          }
          else {
            MavenProxyRepository mavenProxyRepository =
                mavenRepository.adaptToFacet(MavenProxyRepository.class);
            if (mavenProxyRepository != null) {
              mayUpdateProxyPrefixFile(mavenProxyRepository);
            }
          }
        }
        catch (IllegalStateException e) {
          // just neglect it and continue, this one might be auto blocked if proxy or put out of service
          log.trace("Repository {} is not in state to be updated", mavenRepository);
        }
        catch (Exception e) {
          // just neglect it and continue, but do log it
          log.warn("Problem during prefix file update of repository {}",
              RepositoryStringUtils.getHumanizedNameString(mavenRepository), e);
        }
      }
    }
  }

  /**
   * Method meant to be invoked on regular periods (like hourly, as we defined "resolution" of prefix list update
   * period in hours too), and will perform prefix list update on proxy repository only if needed (prefix list is
   * stale, or does not exists).
   *
   * @return {@code true} if update has been spawned, {@code false} if no update needed (prefix list is up to date or
   *         remote discovery is disable for repository).
   */
  protected boolean mayUpdateProxyPrefixFile(final MavenProxyRepository mavenProxyRepository) {
    final DiscoveryStatus discoveryStatus = getStatusFor(mavenProxyRepository).getDiscoveryStatus();
    if (discoveryStatus.getStatus().isEnabled()) {
      // only update if any of these below are true:
      // status is ERROR or ENABLED_NOT_POSSIBLE (hit an error during last discovery)
      // status is anything else and prefix list update period is here
      final DiscoveryConfig config = getRemoteDiscoveryConfig(mavenProxyRepository);
      if (discoveryStatus.getStatus() == DStatus.ERROR
          || discoveryStatus.getStatus() == DStatus.ENABLED_NOT_POSSIBLE
          || ((System.currentTimeMillis() - discoveryStatus.getLastDiscoveryTimestamp()) >
          config.getDiscoveryInterval())) {
        if (discoveryStatus.getStatus() == DStatus.ENABLED_IN_PROGRESS) {
          log.debug("Proxy {} has never been discovered before", mavenProxyRepository);
        }
        else if (discoveryStatus.getStatus() == DStatus.ENABLED_NOT_POSSIBLE) {
          log.debug("Proxy {} discovery was not possible before", mavenProxyRepository);
        }
        else if (discoveryStatus.getStatus() == DStatus.ERROR) {
          log.debug("Proxy {} previous discovery hit an error", mavenProxyRepository);
        }
        else {
          log.debug("Proxy {} needs periodic remote discovery update", mavenProxyRepository);
        }
        final boolean updateSpawned = doUpdatePrefixFileAsync(false, mavenProxyRepository);
        if (!updateSpawned) {
          // this means that either remote discovery takes too long or user might pressed Force discovery
          // on UI for moments before this call kicked in. Anyway, warn the user in logs
          log.info(
              "Proxy {} periodic remote discovery skipped as there is an ongoing job updating it, consider raising the update interval for this repository",
              RepositoryStringUtils.getHumanizedNameString(mavenProxyRepository));
        }
        return updateSpawned;
      }
      else {
        log.debug("Proxy {} prefix file is up to date", mavenProxyRepository);
      }
    }
    else {
      log.debug("Proxy {} prefix file update requested, but it's remote discovery is disabled",
          mavenProxyRepository);
    }
    return false;
  }

  @Override
  public boolean updatePrefixFile(final MavenRepository mavenRepository)
      throws IllegalStateException
  {
    checkUpdateConditions(mavenRepository);
    return doUpdatePrefixFileAsync(false, mavenRepository);
  }

  @Override
  public boolean forceUpdatePrefixFile(final MavenRepository mavenRepository)
      throws IllegalStateException
  {
    checkUpdateConditions(mavenRepository);
    return doUpdatePrefixFileAsync(true, mavenRepository);
  }

  @Override
  public void forceProxyQuickUpdatePrefixFile(final MavenProxyRepository mavenProxyRepository)
      throws IllegalStateException
  {
    checkUpdateConditions(mavenProxyRepository);
    try {
      log.debug("Quick updating prefix file of {}", mavenProxyRepository);
      constrainedExecutor.cancelRunningWithKey(mavenProxyRepository.getId());
      final PrefixSource prefixSource =
          updateProxyPrefixFile(mavenProxyRepository, Collections.singletonList(quickRemoteStrategy));

      // this is never null
      final PrefixSource oldPrefixSource = getPrefixSourceFor(mavenProxyRepository);
      // does repo goes from unpublished to published or other way around?
      final boolean stateChanged =
          (oldPrefixSource.supported()) != (prefixSource != null && prefixSource.supported());
      if (prefixSource != null && prefixSource.supported()) {
        if (stateChanged) {
          log.info("Updated and published prefix file of {}",
              RepositoryStringUtils.getHumanizedNameString(mavenProxyRepository));
        }
        publish(mavenProxyRepository, prefixSource);
      }
      else {
        if (stateChanged) {
          log.info("Unpublished prefix file of {} (and is marked for noscrape)",
              RepositoryStringUtils.getHumanizedNameString(mavenProxyRepository));
        }
        unpublish(mavenProxyRepository);
      }
    }
    catch (final Exception e) {
      try {
        unpublish(mavenProxyRepository);
      }
      catch (IOException ioe) {
        // silently
      }
      // propagate original exception
      Throwables.propagate(e);
    }
  }

  @Override
  public boolean isMavenRepositorySupported(final MavenRepository mavenRepository)
      throws IllegalStateException
  {
    final MavenShadowRepository mavenShadowRepository = mavenRepository.adaptToFacet(MavenShadowRepository.class);
    if (mavenShadowRepository != null) {
      return false; // shadows unsupported
    }
    if (!Maven2ContentClass.ID.equals(mavenRepository.getRepositoryContentClass().getId())) {
      return false; // maven2 layout support only, no maven1 support
    }
    return true;
  }

  /**
   * Checks conditions for repository, is it updateable. If not for any reason, {@link IllegalStateException} is
   * thrown.
   *
   * @throws IllegalStateException when passed in repository cannot be updated for some reason. Reason is message of
   *                               the exception being thrown.
   */
  protected void checkUpdateConditions(final MavenRepository mavenRepository)
      throws IllegalStateException
  {
    if (!isMavenRepositorySupported(mavenRepository)) {
      // we should really not see this, it would mean some execution path is buggy as it gets here
      // with unsupported repo
      throw new IllegalStateException(
          "Repository not supported by automatic routing feature (only Maven2 hosted, proxy and group repositories are supported)");
    }
    final LocalStatus localStatus = mavenRepository.getLocalStatus();
    if (!localStatus.shouldServiceRequest()) {
      throw new IllegalStateException(SimpleFormat.format("Repository out of service '%s'", mavenRepository.getId()));
    }
  }

  /**
   * Performs "background" async update. If {@code forced} is {@code true}, it will always schedule an update job
   * (even at cost of cancelling any currently running one). If {@code forced} is {@code false}, job will be spawned
   * only if another job for same repository is not running.
   *
   * @param forced if {@code true} will always schedule update job, and might cancel any existing job, if running.
   * @return if {@code forced=true}, return value of {@code true} means this invocation did cancel previous job. If
   *         {@code forced=false}, return value {@code true} means this invocation did schedule a job, otherwise it
   *         did not, as another job for same repository was already running.
   */
  protected boolean doUpdatePrefixFileAsync(final boolean forced, final MavenRepository mavenRepository) {
    final UpdateRepositoryRunnable updateRepositoryJob =
        new UpdateRepositoryRunnable(new LoggingProgressListener(log), applicationStatusSource, this,
            mavenRepository);
    if (forced) {
      final boolean canceledPreviousJob =
          constrainedExecutor.mustExecute(mavenRepository.getId(), updateRepositoryJob);
      if (canceledPreviousJob) {
        // this is okay, as forced happens rarely, currently only when proxy repo changes remoteURL
        // (reconfiguration happens)
        log.debug("Forced prefix file update on {} canceled currently running discovery job",
            mavenRepository);
      }
      return canceledPreviousJob;
    }
    else {
      return constrainedExecutor.mayExecute(mavenRepository.getId(), updateRepositoryJob);
    }
  }

  /**
   * Is visible to expose over the nexus-it-helper-plugin only, and UTs are using this. Should not be used for other
   * means.
   *
   * @return {@code true} if there are prefix file update jobs running, or boot of feature not yet finished.
   */
  @VisibleForTesting
  public boolean isUpdatePrefixFileJobRunning() {
    if (config.isFeatureActive() && !periodicUpdaterDidRunAtLeastOnce) {
      log.debug("Boot process not done yet, periodic updater did not yet finish!");
      return true;
    }
    final Statistics statistics = constrainedExecutor.getStatistics();
    log.debug("Running update jobs for {}", statistics.getCurrentlyRunningJobKeys());
    return !statistics.getCurrentlyRunningJobKeys().isEmpty();
  }

  protected void updateAndPublishPrefixFile(final MavenRepository mavenRepository)
      throws IOException
  {
    log.debug("Updating prefix file of {}", mavenRepository);
    try {
      final PrefixSource prefixSource;
      if (mavenRepository.getRepositoryKind().isFacetAvailable(MavenGroupRepository.class)) {
        prefixSource = updateGroupPrefixFile(mavenRepository.adaptToFacet(MavenGroupRepository.class));
      }
      else if (mavenRepository.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class)) {
        prefixSource = updateProxyPrefixFile(mavenRepository.adaptToFacet(MavenProxyRepository.class), null);
      }
      else if (mavenRepository.getRepositoryKind().isFacetAvailable(MavenHostedRepository.class)) {
        prefixSource = updateHostedPrefixFile(mavenRepository.adaptToFacet(MavenHostedRepository.class));
      }
      else {
        // we should not get here
        log.info("Repository {} unsupported by automatic routing feature",
            RepositoryStringUtils.getFullHumanizedNameString(mavenRepository));
        return;
      }

      // this is never null
      final PrefixSource oldPrefixSource = getPrefixSourceFor(mavenRepository);
      // does repo goes from unpublished to published or other way around?
      final boolean stateChanged =
          (oldPrefixSource.supported()) != (prefixSource != null && prefixSource.supported());

      if (prefixSource != null && prefixSource.supported()) {
        if (stateChanged) {
          log.info("Updated and published prefix file of {}",
              RepositoryStringUtils.getHumanizedNameString(mavenRepository));
        }
        publish(mavenRepository, prefixSource);
      }
      else {
        if (stateChanged) {
          log.info("Unpublished prefix file of {} (and is marked for noscrape)",
              RepositoryStringUtils.getHumanizedNameString(mavenRepository));
        }
        unpublish(mavenRepository);
      }
    }
    catch (IllegalStateException e) {
      // just ack it, log it and return peacefully
      log.debug(
          "Maven repository {} not in state for prefix file update: {}", mavenRepository, e.getMessage()
      );
    }
  }

  protected PrefixSource updateProxyPrefixFile(final MavenProxyRepository mavenProxyRepository,
                                               final List<RemoteStrategy> remoteStrategies)
      throws IllegalStateException, IOException
  {
    checkUpdateConditions(mavenProxyRepository);

    final PropfileDiscoveryStatusSource discoveryStatusSource =
        new PropfileDiscoveryStatusSource(mavenProxyRepository);

    final ProxyMode proxyMode = mavenProxyRepository.getProxyMode();
    if (!proxyMode.shouldProxy()) {
      final DiscoveryStatus discoveryStatus =
          new DiscoveryStatus(DStatus.ENABLED_NOT_POSSIBLE, "none", "Proxy repository is blocked.",
              System.currentTimeMillis());
      discoveryStatusSource.write(discoveryStatus);
      throw new IllegalStateException("Maven repository "
          + RepositoryStringUtils.getHumanizedNameString(mavenProxyRepository)
          + " not in state to be updated (is blocked).");
    }

    PrefixSource prefixSource = null;
    final DiscoveryConfig config = getRemoteDiscoveryConfig(mavenProxyRepository);
    if (config.isEnabled()) {
      final DiscoveryResult<MavenProxyRepository> discoveryResult;
      if (null == remoteStrategies) {
        discoveryResult = remoteContentDiscoverer.discoverRemoteContent(mavenProxyRepository);
      }
      else {
        discoveryResult =
            remoteContentDiscoverer.discoverRemoteContent(mavenProxyRepository, remoteStrategies);
      }

      log.debug("Results of {} remote discovery: {}", mavenProxyRepository,
          discoveryResult.getAllResults());

      if (discoveryResult.isSuccessful()) {
        final PrefixSource remotePrefixSource = discoveryResult.getPrefixSource();
        if (remotePrefixSource.supported()) {
          // grab local too and merge them
          final DiscoveryResult<MavenRepository> localDiscoveryResult =
              localContentDiscoverer.discoverLocalContent(mavenProxyRepository);
          if (localDiscoveryResult.isSuccessful()) {
            final HashSet<String> mergedEntries = Sets.newHashSet();
            mergedEntries.addAll(remotePrefixSource.readEntries());
            mergedEntries.addAll(localDiscoveryResult.getPrefixSource().readEntries());
            final ArrayListPrefixSource mergedPrefixSource =
                new ArrayListPrefixSource(Lists.newArrayList(mergedEntries),
                    remotePrefixSource.getLostModifiedTimestamp());
            prefixSource = mergedPrefixSource;
          }
          else {
            log.debug("{} local discovery unsuccessful", mavenProxyRepository);
          }
        }
      }
      final Outcome lastOutcome = discoveryResult.getLastResult();

      final DStatus status;
      if (lastOutcome.isSuccessful()) {
        status = DStatus.SUCCESSFUL;
      }
      else {
        if (lastOutcome.getThrowable() == null) {
          status = DStatus.UNSUCCESSFUL;
        }
        else {
          status = DStatus.ERROR;
        }
      }
      final DiscoveryStatus discoveryStatus =
          new DiscoveryStatus(status, lastOutcome.getStrategyId(), lastOutcome.getMessage(),
              System.currentTimeMillis());
      discoveryStatusSource.write(discoveryStatus);
    }
    else {
      log.info("{} remote discovery disabled",
          RepositoryStringUtils.getHumanizedNameString(mavenProxyRepository));
    }
    return prefixSource;
  }

  protected PrefixSource updateHostedPrefixFile(final MavenHostedRepository mavenHostedRepository)
      throws IllegalStateException, IOException
  {
    checkUpdateConditions(mavenHostedRepository);
    PrefixSource prefixSource = null;
    final DiscoveryResult<MavenRepository> discoveryResult =
        localContentDiscoverer.discoverLocalContent(mavenHostedRepository);
    if (discoveryResult.isSuccessful()) {
      prefixSource = discoveryResult.getPrefixSource();
    }
    else {
      log.debug("{} local discovery unsuccessful", mavenHostedRepository);
    }
    return prefixSource;
  }

  protected PrefixSource updateGroupPrefixFile(final MavenGroupRepository mavenGroupRepository)
      throws IllegalStateException, IOException
  {
    checkUpdateConditions(mavenGroupRepository);
    PrefixSource prefixSource = null;
    // save merged prefix list into group's local storage (if all members has prefix list)
    boolean allMembersHavePublished = true;
    final LinkedHashSet<String> entries = new LinkedHashSet<String>();
    for (Repository member : mavenGroupRepository.getMemberRepositories()) {
      if (member.getRepositoryKind().isFacetAvailable(MavenRepository.class)) {
        // neglect completely out of service members
        if (member.getLocalStatus().shouldServiceRequest()) {
          final FilePrefixSource memberEntrySource =
              getPrefixSourceFor(member.adaptToFacet(MavenRepository.class));
          // lock to prevent file being deleted between exists check and reading it up
          final RepositoryItemUidLock lock = memberEntrySource.getRepositoryItemUid().getLock();
          lock.lock(Action.read);
          try {
            if (!memberEntrySource.supported()) {
              allMembersHavePublished = false;
              break;
            }
            entries.addAll(memberEntrySource.readEntries());
          }
          finally {
            lock.unlock();
          }
        }
      }
    }
    if (allMembersHavePublished) {
      prefixSource = new ArrayListPrefixSource(new ArrayList<String>(entries));
    }
    return prefixSource;
  }

  // ==

  @Override
  public RoutingStatus getStatusFor(final MavenRepository mavenRepository) {
    final MavenProxyRepository mavenProxyRepository = mavenRepository.adaptToFacet(MavenProxyRepository.class);
    final boolean remoteDiscoveryEnabled;
    if (mavenProxyRepository != null) {
      final DiscoveryConfig discoveryConfig = getRemoteDiscoveryConfig(mavenProxyRepository);
      remoteDiscoveryEnabled = discoveryConfig.isEnabled();
    }
    else {
      remoteDiscoveryEnabled = false;
    }

    PublishingStatus publishingStatus = null;
    DiscoveryStatus discoveryStatus = null;

    // publish status
    final FilePrefixSource publishedEntrySource = getPrefixSourceFor(mavenRepository);
    if (!publishedEntrySource.supported()) {
      final String message;
      if (isMavenRepositorySupported(mavenRepository)) {
        if (mavenRepository.getRepositoryKind().isFacetAvailable(MavenGroupRepository.class)) {
          final MavenGroupRepository mavenGroupRepository =
              mavenRepository.adaptToFacet(MavenGroupRepository.class);
          final List<String> membersWithoutPrefixFiles = new ArrayList<String>();
          for (Repository member : mavenGroupRepository.getMemberRepositories()) {
            final MavenRepository memberMavenRepository = member.adaptToFacet(MavenRepository.class);
            if (null != memberMavenRepository) {
              final PrefixSource ps = getPrefixSourceFor(memberMavenRepository);
              if (!ps.supported()) {
                membersWithoutPrefixFiles.add(memberMavenRepository.getName());
              }
            }
          }
          message =
              "Publishing not possible, following members have no published prefix file: "
                  + Joiner.on(", ").join(membersWithoutPrefixFiles);
        }
        else if (mavenRepository.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class)) {
          if (remoteDiscoveryEnabled) {
            message = "Discovery in progress or unable to discover remote content (see discovery status).";
          }
          else {
            message = "Remote discovery not enabled.";
          }
        }
        else if (mavenRepository.getRepositoryKind().isFacetAvailable(MavenHostedRepository.class)) {
          message = "Check Nexus logs for more details."; // hosted reposes must be discovered always
        }
        else if (mavenRepository.getRepositoryKind().isFacetAvailable(ShadowRepository.class)) {
          message = "Unsupported repository type (only hosted, proxy and groups are supported).";
        }
        else {
          message = "Check Nexus logs for more details.";
        }
      }
      else {
        message = "Unsupported repository format (only Maven2 format is supported).";
      }
      publishingStatus = new PublishingStatus(PStatus.NOT_PUBLISHED, message, -1, null);
    }
    else {
      publishingStatus =
          new PublishingStatus(PStatus.PUBLISHED, "Prefix file published successfully.",
              publishedEntrySource.getLostModifiedTimestamp(), publishedEntrySource.getFilePath());
    }

    if (mavenProxyRepository == null) {
      discoveryStatus = new DiscoveryStatus(DStatus.NOT_A_PROXY);
    }
    else {
      if (!remoteDiscoveryEnabled) {
        discoveryStatus = new DiscoveryStatus(DStatus.DISABLED);
      }
      else if (constrainedExecutor.hasRunningWithKey(mavenProxyRepository.getId())) {
        // still running or never run yet
        discoveryStatus = new DiscoveryStatus(DStatus.ENABLED_IN_PROGRESS);
      }
      else {
        final PropfileDiscoveryStatusSource discoveryStatusSource =
            new PropfileDiscoveryStatusSource(mavenProxyRepository);
        if (!discoveryStatusSource.exists()) {
          if (!mavenProxyRepository.getLocalStatus().shouldServiceRequest()) {
            // should run but not yet scheduled, or never run yet
            // out of service prevents us to persist ending states, so this
            // is the only place where we actually "calculate" it
            discoveryStatus =
                new DiscoveryStatus(DStatus.ENABLED_NOT_POSSIBLE, "none",
                    "Repository is out of service.", System.currentTimeMillis());
          }
          else {
            // should run but not yet scheduled, or never run yet
            discoveryStatus = new DiscoveryStatus(DStatus.ENABLED_IN_PROGRESS);
          }
        }
        else {
          // all the other "ending" states are persisted
          try {
            discoveryStatus = discoveryStatusSource.read();
          }
          catch (IOException e) {
            Throwables.propagate(e);
          }
        }
      }
    }
    return new RoutingStatus(publishingStatus, discoveryStatus);
  }

  @Override
  public DiscoveryConfig getRemoteDiscoveryConfig(final MavenProxyRepository mavenProxyRepository) {
    // TODO: hacking external config out of repo!
    final AbstractMavenRepositoryConfiguration configuration =
        (AbstractMavenRepositoryConfiguration)((AbstractMavenRepository) mavenProxyRepository).getCurrentCoreConfiguration()
            .getExternalConfiguration().getConfiguration(
                false);

    return new DiscoveryConfig(config.isFeatureActive() && configuration.isRoutingDiscoveryEnabled(),
        configuration.getRoutingDiscoveryInterval());
  }

  @Override
  public void setRemoteDiscoveryConfig(final MavenProxyRepository mavenProxyRepository,
                                       final DiscoveryConfig config)
      throws IOException
  {
    // TODO: hacking external config out of repo!
    final AbstractMavenRepositoryConfiguration configuration =
        (AbstractMavenRepositoryConfiguration) ((AbstractMavenRepository) mavenProxyRepository).getCurrentCoreConfiguration()
            .getExternalConfiguration().getConfiguration(
                false);

    final boolean enabledChanged = configuration.isRoutingDiscoveryEnabled() != config.isEnabled();
    configuration.setRoutingDiscoveryEnabled(config.isEnabled());
    configuration.setRoutingDiscoveryInterval(config.getDiscoveryInterval());
    applicationConfiguration.saveConfiguration();

    if (enabledChanged) {
      updatePrefixFile(mavenProxyRepository);
    }
  }

  @Override
  public FilePrefixSource getPrefixSourceFor(final MavenRepository mavenRepository) {
    return new FilePrefixSource(mavenRepository, config.getLocalPrefixFilePath(), config);
  }

  // ==

  @Override
  public boolean offerEntry(final MavenHostedRepository mavenHostedRepository, final StorageItem item)
      throws IOException
  {
    if (constrainedExecutor.hasRunningWithKey(mavenHostedRepository.getId())) {
      // as of 2.4 this is only possible during initial autorouting configuration of hosted repositories
      // any changes to prefix list performed here will be incomplete, i.e. list single path prefix
      // and it will be overwritten when initial autorouting configuration completes
      // although not 100% bulletproof, this logic reduces the risk of this happening
      return false;
    }
    final FilePrefixSource prefixSource = getPrefixSourceFor(mavenHostedRepository);
    final RepositoryItemUidLock lock = prefixSource.getRepositoryItemUid().getLock();
    lock.lock(Action.read);
    try {
      if (!prefixSource.supported()) {
        return false;
      }
      final String entry;
      if (item.getPathDepth() == 0) {
        entry = item.getPath();
      } else {
        entry = item.getParentPath();
      }
      final WritablePrefixSourceModifier wesm =
          new WritablePrefixSourceModifier(prefixSource, config.getLocalScrapeDepth());
      wesm.offerEntry(entry);
      if (wesm.hasChanges()) {
        boolean changed = false;
        lock.lock(Action.update);
        try {
          wesm.reset();
          wesm.offerEntry(entry);
          changed = wesm.apply();
          if (changed) {
            publish(mavenHostedRepository, prefixSource);
          }
        }
        finally {
          lock.unlock();
        }
        return changed;
      }
    }
    finally {
      lock.unlock();
    }
    return false;
  }

  @Override
  public boolean revokeEntry(final MavenHostedRepository mavenHostedRepository, final StorageItem item)
      throws IOException
  {
    if (constrainedExecutor.hasRunningWithKey(mavenHostedRepository.getId())) {
      // as of 2.4 this is only possible during initial autorouting configuration of hosted repositories
      // any changes to prefix list performed here will be incomplete, i.e. list single path prefix
      // and it will be overwritten when initial autorouting configuration completes
      // although not 100% bulletproof, this logic reduces the risk of this happening
      return false;
    }
    final FilePrefixSource prefixSource = getPrefixSourceFor(mavenHostedRepository);
    final RepositoryItemUidLock lock = prefixSource.getRepositoryItemUid().getLock();
    lock.lock(Action.read);
    try {
      if (!prefixSource.supported()) {
        return false;
      }
      final WritablePrefixSourceModifier wesm =
          new WritablePrefixSourceModifier(prefixSource, config.getLocalScrapeDepth());
      wesm.revokeEntry(item.getPath());
      if (wesm.hasChanges()) {
        boolean changed = false;
        lock.lock(Action.update);
        try {
          wesm.reset();
          wesm.revokeEntry(item.getPath());
          changed = wesm.apply();
          if (changed) {
            publish(mavenHostedRepository, prefixSource);
          }
        }
        finally {
          lock.unlock();
        }
        return changed;
      }
    }
    finally {
      lock.unlock();
    }
    return false;
  }

  // ==

  @Override
  public void publish(final MavenRepository mavenRepository, final PrefixSource prefixSource)
      throws IOException
  {
    // publish prefix file
    final FilePrefixSource prefixesFile = getPrefixSourceFor(mavenRepository);
    try {
      prefixesFile.writeEntries(prefixSource);
    }
    catch (InvalidInputException e) {
      unpublish(mavenRepository);
      throw e;
    }

    // event
    eventBus.post(new PrefixFilePublishedRepositoryEvent(mavenRepository, prefixesFile));

    // propagate
    propagatePrefixFileUpdateOf(mavenRepository);
  }

  @Override
  public void unpublish(final MavenRepository mavenRepository)
      throws IOException
  {
    getPrefixSourceFor(mavenRepository).writeUnsupported();

    // event
    eventBus.post(new PrefixFileUnpublishedRepositoryEvent(mavenRepository));

    // propagate
    propagatePrefixFileUpdateOf(mavenRepository);
  }

  protected void propagatePrefixFileUpdateOf(final MavenRepository mavenRepository) {
    MavenGroupRepository containingGroupRepository = null;
    final List<GroupRepository> groups = repositoryRegistry.getGroupsOfRepository(mavenRepository);
    for (GroupRepository groupRepository : groups) {
      containingGroupRepository = groupRepository.adaptToFacet(MavenGroupRepository.class);
      if (mavenRepository != null) {
        // this method is invoked while holding write lock on mavenRepository prefix file
        // groupRepository prefix file calculation will need read locks on all members prefix files
        // to avoid deadlocks we push group prefix file update to another thread
        doUpdatePrefixFileAsync(true, containingGroupRepository);
      }
    }
  }

  // ==

  @Override
  public boolean isEventAboutPrefixFile(RepositoryItemEvent evt) {
    return evt.getRepository().getRepositoryKind().isFacetAvailable(MavenRepository.class)
        && evt.getItem() instanceof StorageFileItem
        && config.getLocalPrefixFilePath().equals(evt.getItem().getPath());
  }

  // ==

  /**
   * Event handler.
   */
  @Subscribe
  public void onNexusStartedEvent(final NexusStartedEvent evt) {
    startup();
  }

  /**
   * Event handler.
   */
  @Subscribe
  public void onNexusStoppedEvent(final NexusStoppedEvent evt) {
    shutdown();
  }
}
