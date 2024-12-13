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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.scheduling.PeriodicJobService.PeriodicJob;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUpdatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentCreatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentUpdatedEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.upload.UploadManager.UIUploadEvent;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.codahale.metrics.annotation.Gauge;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Integer.parseInt;
import static java.lang.Thread.MIN_PRIORITY;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.repository.content.search.SearchEventHandler.RequestType.INDEX;
import static org.sonatype.nexus.repository.content.search.SearchEventHandler.RequestType.PURGE;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;

/**
 * Event handler that periodically updates search indexes based on component/asset events.
 *
 * This handler runs in the same thread that triggered the event. It avoids doing too much
 * work on that thread by simply marking the component id as pending in a concurrent map.
 * A background task periodically grabs a page of components and sends them for indexing.
 *
 * If too many components build up events will start to be posted to flush additional pages.
 * These events are handled by an asynchronous receiver using threads from the event pool.
 *
 * @since 3.26
 */
public abstract class SearchEventHandler
    extends LifecycleSupport
    implements EventAware // warning: don't make this EventAware.Asynchronous
{
  private static final String HANDLER_KEY_PREFIX = "nexus.search.event.handler.";

  protected static final String FLUSH_ON_COUNT_KEY = HANDLER_KEY_PREFIX + "flushOnCount";

  protected static final String FLUSH_ON_SECONDS_KEY = HANDLER_KEY_PREFIX + "flushOnSeconds";

  protected static final String NO_PURGE_DELAY_KEY = HANDLER_KEY_PREFIX + "noPurgeDelay";

  protected static final String FLUSH_POOL_SIZE = HANDLER_KEY_PREFIX + "flushPoolSize";

  enum RequestType
  {
    INDEX,
    PURGE
  }

  private final RepositoryManager repositoryManager;

  private final PeriodicJobService periodicJobService;

  private final int flushOnCount;

  private final int flushOnSeconds;

  private final boolean noPurgeDelay;

  private final Map<String, String> pendingRequests = new ConcurrentHashMap<>();

  private final AtomicInteger pendingCount = new AtomicInteger();

  private final int poolSize;

  protected ThreadPoolExecutor threadPoolExecutor;

  private Object flushMutex = new Object();

  private PeriodicJob flushTask;

  private boolean processEvents = true;

  public SearchEventHandler(
      final RepositoryManager repositoryManager,
      final PeriodicJobService periodicJobService,
      @Named("${" + FLUSH_ON_COUNT_KEY + ":-100}") final int flushOnCount,
      @Named("${" + FLUSH_ON_SECONDS_KEY + ":-2}") final int flushOnSeconds,
      @Named("${" + NO_PURGE_DELAY_KEY + ":-true}") final boolean noPurgeDelay,
      @Named("${" + FLUSH_POOL_SIZE + ":-128}") final int poolSize)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.periodicJobService = checkNotNull(periodicJobService);
    checkArgument(flushOnCount > 0, FLUSH_ON_COUNT_KEY + " must be positive");
    this.flushOnCount = flushOnCount;
    checkArgument(flushOnSeconds > 0, FLUSH_ON_SECONDS_KEY + " must be positive");
    this.flushOnSeconds = flushOnSeconds;
    this.noPurgeDelay = noPurgeDelay;

    checkArgument(poolSize > 0, "Pool size must be greater than zero");
    this.poolSize = poolSize;
  }

  @Override
  protected void doStart() throws Exception {
    if (flushOnCount > 1) {
      periodicJobService.startUsing();
      flushTask = periodicJobService.schedule(this::pollSearchUpdateRequest, flushOnSeconds);
    }

    this.threadPoolExecutor = new ThreadPoolExecutor(
        poolSize, // core-size
        poolSize, // max-size
        0L, // keep-alive
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(), // allow queueing up of requests
        new NexusThreadFactory(getThreadPoolId(), "flushAndPurge", MIN_PRIORITY),
        new AbortPolicy());
  }

  protected String getThreadPoolId() {
    return "searchEventHandler";
  }

  @Override
  protected void doStop() throws Exception {
    if (flushOnCount > 1) {
      flushTask.cancel();
      periodicJobService.stopUsing();
    }

    this.threadPoolExecutor.shutdownNow();
  }

  @Gauge(name = "nexus.search.eventHandler.executor.queueSize")
  public int searchEventQueue() {
    return threadPoolExecutor.getQueue().size();
  }

  /**
   * Allow event processing to be disabled.
   *
   * @since 3.30
   */
  public void setProcessEvents(final boolean processEvents) {
    this.processEvents = processEvents;
  }

  /**
   * Request update search indexes based on component id
   *
   * @param format The repository format
   * @param componentId The component id
   * @param repository The repository
   */
  public void requestIndex(final String format, final int componentId, final Repository repository) {
    if (processEvents && componentId > 0) {
      markComponentAsPending(requestKey(format, componentId), repoTag(INDEX, repository));
      maybeTriggerAsyncFlush();
    }
  }

  /**
   * Request purge search indexes based on component id
   *
   * @param format The repository format
   * @param componentId The component id
   * @param repository The repository
   */
  public void requestPurge(final String format, final int componentId, final Repository repository) {
    if (processEvents && componentId > 0) {
      markComponentAsPending(requestKey(format, componentId), repoTag(PURGE, repository));
      maybeTriggerAsyncPurge();
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentCreatedEvent event) {
    requestIndex(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentUpdatedEvent event) {
    requestIndex(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentDeletedEvent event) {
    requestPurge(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetCreatedEvent event) {
    requestIndex(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetUpdatedEvent event) {
    requestIndex(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetDeletedEvent event) {
    requestIndex(event); // update the component search document on asset delete
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final UIUploadEvent event) {
    indexUIUpload(event.getRepository(), event.getAssetPaths());
  }

  /**
   * Updates the asset(s) created/updated via a UI upload.
   * It doesn't touch the queue of batch operations to avoid any concurrency issues.
   * And since this is only for UI, the superfluous work should be minimal.
   */
  @VisibleForTesting
  static void indexUIUpload(final Repository repository, final List<String> assetPaths) {
    repository.optionalFacet(SearchFacet.class)
        .ifPresent(searchFacet -> repository.optionalFacet(ContentFacet.class)
            .ifPresent(contentFacet -> processUIUpload(assetPaths, contentFacet, searchFacet)));
  }

  private static void processUIUpload(
      final List<String> assetPaths,
      final ContentFacet contentFacet,
      final SearchFacet searchFacet)
  {
    List<EntityId> componentIds = assetPaths.stream()
        .map(assetPath -> getComponentEntityId(assetPath, contentFacet))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());

    if (!componentIds.isEmpty()) {
      searchFacet.index(componentIds);
    }
  }

  private static Optional<EntityId> getComponentEntityId(final String assetPath, final ContentFacet contentFacet) {
    return contentFacet.assets()
        .path(assetPath)
        .find()
        .map(InternalIds::internalComponentId)
        .filter(OptionalInt::isPresent)
        .map(OptionalInt::getAsInt)
        .map(InternalIds::toExternalId);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentPurgedEvent event) {
    Optional<Repository> repository = event.getRepository();

    if (!repository.isPresent()) {
      log.debug("Unable to determine repository for event {}", event);
      return;
    }

    String format = repository.get().getFormat().getValue();
    String repoTag = repoTag(PURGE, repository.get());

    for (int componentId : event.getComponentIds()) {
      markComponentAsPending(requestKey(format, componentId), repoTag);
    }

    maybeTriggerAsyncPurge();
  }

  // no need to watch for AssetPurgeEvent because that's only sent when purging assets without components
  protected void requestIndex(final ComponentEvent event) {
    Optional<Repository> repository = event.getRepository();

    if (!repository.isPresent()) {
      log.debug("Unable to determine repository for event {}", event);
      return;
    }

    requestIndex(event.getFormat(), internalComponentId(event.getComponent()), repository.get());
  }

  private void requestPurge(final ComponentDeletedEvent event) {
    Optional<Repository> repository = event.getRepository();

    if (!repository.isPresent()) {
      log.debug("Unable to determine repository for event {}", event);
      return;
    }

    requestPurge(event.getFormat(), internalComponentId(event.getComponent()), repository.get());
  }

  private void requestIndex(final AssetEvent event) {
    Optional<Repository> repository = event.getRepository();

    if (!repository.isPresent()) {
      log.debug("Unable to determine repository for event {}", event);
      return;
    }

    requestIndex(event.getFormat(), internalComponentId(event.getAsset()).orElse(-1), repository.get());
  }

  private void markComponentAsPending(final String requestKey, final String repoTag) {
    // bump count if this is the first time we've seen this request key in this batch
    if (pendingRequests.put(requestKey, repoTag) == null) {
      pendingCount.getAndIncrement();
    }
  }

  private boolean maybeTriggerAsyncFlush() {
    // if there are lots of pending requests then reduce count by a page and
    // trigger an asynchronous flush event (which will actually do the work)
    if (pendingCount.getAndUpdate(c -> c >= flushOnCount ? c - flushOnCount : c) >= flushOnCount) {
      threadPoolExecutor.execute(() -> flushPageOfComponents(null));
      return true;
    }
    return false;
  }

  private boolean maybeTriggerAsyncPurge() {
    // if it's still too early to flush requests, but we don't want to delay
    // outstanding purge requests then trigger an asynchronous purge event
    if (!maybeTriggerAsyncFlush() && noPurgeDelay) {
      threadPoolExecutor.execute(() -> flushPageOfComponents(PURGE));
      return true;
    }
    return false;
  }

  /**
   * Used by scheduled flush task to poll for work.
   */
  void pollSearchUpdateRequest() {
    if (pendingCount.get() > 0) {
      flushPageOfComponents(null);
    }
  }

  /**
   * Grabs a page of components and sends them to the appropriate {@link SearchFacet}.
   *
   * @param requestType optional request type to filter on
   */
  void flushPageOfComponents(@Nullable final RequestType requestType) {
    Multimap<String, EntityId> requestsByRepository = ArrayListMultimap.create();

    // only allow one thread to remove entries at a time while still allowing other threads to add entries
    synchronized (flushMutex) {

      // remove page and invert it to get mapping from repository to components
      Iterator<Entry<String, String>> itr = pendingRequests.entrySet().iterator();
      for (int i = 0; i < flushOnCount && itr.hasNext(); i++) {
        Entry<String, String> entry = itr.next();

        if (requestType == null || entry.getValue().startsWith(requestType.name())) {
          // requests are scoped per-repository so it's safe to drop the format here
          requestsByRepository.put(entry.getValue(), componentId(entry.getKey()));
          itr.remove();
        }
      }
    }

    // deliver index/purge requests to the relevant repositories
    requestsByRepository.asMap()
        .forEach(
            (repoTag, componentIds) -> ofNullable(repositoryManager.get(repositoryName(repoTag))).ifPresent(
                repository -> repository.optionalFacet(SearchFacet.class)
                    .ifPresent(
                        searchFacet -> {
                          if (repoTag.startsWith(INDEX.name())) {
                            searchFacet.index(componentIds);
                          }
                          else {
                            searchFacet.purge(componentIds);
                          }
                        })));
  }

  @VisibleForTesting
  public boolean isCalmPeriod() {
    return threadPoolExecutor.getQueue().isEmpty() && threadPoolExecutor.getActiveCount() == 0;
  }

  /**
   * Binds the format with the component id to get a unique request key.
   */
  private static String requestKey(final String format, final int componentId) {
    return format + ':' + componentId;
  }

  /**
   * Extracts the external component id from the request key; this should only be done in the context of a
   * repository/format.
   */
  private static EntityId componentId(final String requestKey) {
    return toExternalId(parseInt(requestKey.substring(requestKey.indexOf(':') + 1)));
  }

  /**
   * Binds the request type with the repository name to get the repository tag.
   */
  private static String repoTag(final RequestType requestType, final Repository repository) {
    return requestType.name() + ':' + repository.getName();
  }

  /**
   * Extracts the repository name from the repository tag.
   */
  private static String repositoryName(final String repoTag) {
    return repoTag.substring(repoTag.indexOf(':') + 1);
  }
}
