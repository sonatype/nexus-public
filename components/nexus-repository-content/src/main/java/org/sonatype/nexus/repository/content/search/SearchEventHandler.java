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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
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
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.PeriodicJobService;
import org.sonatype.nexus.scheduling.PeriodicJobService.PeriodicJob;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
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
@FeatureFlag(name = "nexus.datastore.enabled")
@ManagedLifecycle(phase = SERVICES)
@Named
@Singleton
public class SearchEventHandler
    extends LifecycleSupport
    implements EventAware // warning: don't make this EventAware.Asynchronous
{
  private static final String HANDLER_KEY_PREFIX = "nexus.search.event.handler.";

  private static final String FLUSH_ON_COUNT_KEY = HANDLER_KEY_PREFIX + "flushOnCount";

  private static final String FLUSH_ON_SECONDS_KEY = HANDLER_KEY_PREFIX + "flushOnSeconds";

  private static final String NO_PURGE_DELAY_KEY = HANDLER_KEY_PREFIX + "noPurgeDelay";

  enum RequestType
  {
    INDEX, PURGE
  }

  private final RepositoryManager repositoryManager;

  private final PeriodicJobService periodicJobService;

  private final EventManager eventManager;

  private final int flushOnCount;

  private final int flushOnSeconds;

  private final boolean noPurgeDelay;

  private final FlushEventReceiver flushEventReceiver = new FlushEventReceiver();

  private final Map<String, String> pendingRequests = new ConcurrentHashMap<>();

  private final AtomicInteger pendingCount = new AtomicInteger();

  private Object flushMutex = new Object();

  private PeriodicJob flushTask;

  private boolean processEvents = true;

  @Inject
  public SearchEventHandler(
      final RepositoryManager repositoryManager,
      final PeriodicJobService periodicJobService,
      final EventManager eventManager,
      @Named("${" + FLUSH_ON_COUNT_KEY + ":-100}") final int flushOnCount,
      @Named("${" + FLUSH_ON_SECONDS_KEY + ":-2}") final int flushOnSeconds,
      @Named("${" + NO_PURGE_DELAY_KEY + ":-true}") final boolean noPurgeDelay)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.periodicJobService = checkNotNull(periodicJobService);
    this.eventManager = checkNotNull(eventManager);
    checkArgument(flushOnCount > 0, FLUSH_ON_COUNT_KEY + " must be positive");
    this.flushOnCount = flushOnCount;
    checkArgument(flushOnSeconds > 0, FLUSH_ON_SECONDS_KEY + " must be positive");
    this.flushOnSeconds = flushOnSeconds;
    this.noPurgeDelay = noPurgeDelay;

    eventManager.register(flushEventReceiver);
  }

  @Override
  protected void doStart() throws Exception {
    if (flushOnCount > 1) {
      periodicJobService.startUsing();
      flushTask = periodicJobService.schedule(this::pollSearchUpdateRequest, flushOnSeconds);
    }
  }

  @Override
  protected void doStop() throws Exception {
    if (flushOnCount > 1) {
      flushTask.cancel();
      periodicJobService.stopUsing();
    }
  }

  /**
   * Allow event processing to be disabled.
   *
   * @since 3.next
   */
  public void setProcessEvents(final boolean processEvents) {
    this.processEvents = processEvents;
  }

  /**
   * Request update search indexes based on component id
   * @param format        The repository format
   * @param componentId   The component id
   * @param repository    The repository
   */
  public void requestIndex(final String format, final int componentId, final Repository repository) {
    if (processEvents && componentId > 0) {
      markComponentAsPending(requestKey(format, componentId), repoTag(INDEX, repository));
      maybeTriggerAsyncFlush();
    }
  }

  /**
   * Request purge search indexes based on component id
   * @param format        The repository format
   * @param componentId   The component id
   * @param repository    The repository
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
  public void on(final ComponentPurgedEvent event) {
    Repository repository = event.getRepository();

    String format = repository.getFormat().getValue();
    String repoTag = repoTag(PURGE, repository);

    for (int componentId : event.getComponentIds()) {
      markComponentAsPending(requestKey(format, componentId), repoTag);
    }

    maybeTriggerAsyncPurge();
  }

  // no need to watch for AssetPurgeEvent because that's only sent when purging assets without components

  private void requestIndex(final ComponentEvent event) {
    requestIndex(event.getFormat(), internalComponentId(event.getComponent()), event.getRepository());
  }

  private void requestPurge(final ComponentDeletedEvent event) {
    requestPurge(event.getFormat(), internalComponentId(event.getComponent()), event.getRepository());
  }

  private void requestIndex(final AssetEvent event) {
    requestIndex(event.getFormat(), internalComponentId(event.getAsset()).orElse(-1), event.getRepository());
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
      eventManager.post(new FlushEvent());
      return true;
    }
    return false;
  }

  private boolean maybeTriggerAsyncPurge() {
    // if it's still too early to flush requests, but we don't want to delay
    // outstanding purge requests then trigger an asynchronous purge event
    if (!maybeTriggerAsyncFlush() && noPurgeDelay) {
      eventManager.post(new PurgeEvent());
      return true;
    }
    return false;
  }

  /**
   * Marker event that indicates another page of components should be flushed.
   */
  private static class FlushEvent
  {
    // this event is a marker only
  }

  /**
   * Marker event that indicates another page of components should be purged.
   */
  private static class PurgeEvent
  {
    // this event is a marker only
  }

  /**
   * Asynchronous receiver of {@link FlushEvent}s and {@link PurgeEvent}s.
   */
  private class FlushEventReceiver
      implements EventAware.Asynchronous
  {
    @AllowConcurrentEvents
    @Subscribe
    public void on(final FlushEvent event) {
      flushPageOfComponents(null);
    }

    @AllowConcurrentEvents
    @Subscribe
    public void on(final PurgeEvent event) {
      flushPageOfComponents(PURGE);
    }
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
    requestsByRepository.asMap().forEach(
        (repoTag, componentIds) -> ofNullable(repositoryManager.get(repositoryName(repoTag))).ifPresent(
            repository -> repository.optionalFacet(SearchFacet.class).ifPresent(
                searchFacet -> {
                  if (repoTag.startsWith(INDEX.name())) {
                    searchFacet.index(componentIds);
                  }
                  else {
                    searchFacet.purge(componentIds);
                  }
                })));
  }

  /**
   * Binds the format with the component id to get a unique request key.
   */
  private static String requestKey(final String format, final int componentId) {
    return format + ':' + componentId;
  }

  /**
   * Extracts the external component id from the request key; this should only be done in the context of a repository/format.
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
