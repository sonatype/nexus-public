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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
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
@ManagedLifecycle(phase = SERVICES)
@Named
@Singleton
public class SearchEventHandler
    extends LifecycleSupport
    implements EventAware
{
  private static final String INDEX_TAG = "index:";

  private static final String PURGE_TAG = "purge:"; // must be same length as index tag

  private static final int TAG_LENGTH = INDEX_TAG.length();

  private final RepositoryManager repositoryManager;

  private final PeriodicJobService periodicJobService;

  private final EventManager eventManager;

  private final int flushOnCount;

  private final int flushOnSeconds;

  private final FlushEventReceiver flushEventReceiver = new FlushEventReceiver();

  private final Map<String, String> pendingRequests = new ConcurrentHashMap<>();

  private final AtomicInteger pendingCount = new AtomicInteger();

  private PeriodicJob flushTask;

  @Inject
  public SearchEventHandler(
      final RepositoryManager repositoryManager,
      final PeriodicJobService periodicJobService,
      final EventManager eventManager,
      @Named("${nexus.search.event.handler.flushOnCount:-100}") final int flushOnCount,
      @Named("${nexus.search.event.handler.flushOnSeconds:-2}") final int flushOnSeconds)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.periodicJobService = checkNotNull(periodicJobService);
    this.eventManager = checkNotNull(eventManager);
    this.flushOnCount = flushOnCount;
    this.flushOnSeconds = flushOnSeconds;

    eventManager.register(flushEventReceiver);
  }

  @Override
  protected void doStart() throws Exception {
    periodicJobService.startUsing();

    flushTask = periodicJobService.schedule(this::pollPendingComponents, flushOnSeconds);
  }

  @Override
  protected void doStop() throws Exception {
    flushTask.cancel();

    periodicJobService.stopUsing();
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentCreatedEvent event) {
    index(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentUpdatedEvent event) {
    index(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentDeletedEvent event) {
    purge(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentPurgedEvent event) {
    Repository repository = event.getRepository();
    String format = repository.getFormat().getValue();
    for (int componentId : event.getComponentIds()) {
      purge(format, componentId, repository);
    }
  }

  // no need to watch for AssetPurgeEvent because that's only sent when purging assets without components

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetCreatedEvent event) {
    index(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetUpdatedEvent event) {
    index(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetDeletedEvent event) {
    index(event);
  }

  private void index(final ComponentEvent event) {
    index(event.getFormat(), internalComponentId(event.getComponent()), event.getRepository());
  }

  private void purge(final ComponentDeletedEvent event) {
    purge(event.getFormat(), internalComponentId(event.getComponent()), event.getRepository());
  }

  private void index(final AssetEvent event) {
    index(event.getFormat(), internalComponentId(event.getAsset()).orElse(-1), event.getRepository());
  }

  private void index(final String format, final int componentId, final Repository repository) {
    if (componentId > 0) {
      markComponentAsPending(requestKey(format, componentId), INDEX_TAG + repository.getName());
    }
  }

  private void purge(final String format, final int componentId, final Repository repository) {
    if (componentId > 0) {
      markComponentAsPending(requestKey(format, componentId), PURGE_TAG + repository.getName());
    }
  }

  /**
   * Marks component format+id as requiring indexing along with a flag based on the content repository id.
   *
   * @param repoTag tagged repository name
   */
  private void markComponentAsPending(final String requestKey, final String repoTag) {

    // bump count if this is the first time we've seen this component formatAndId in this batch
    if (pendingRequests.put(requestKey, repoTag) == null) {
      pendingCount.getAndIncrement();
    }

    // if we're over the limit then reduce it by a page and trigger an asynchronous flush event
    if (pendingCount.getAndUpdate(c -> c >= flushOnCount ? c - flushOnCount : c) >= flushOnCount) {
      eventManager.post(new FlushEvent());
    }
  }

  /**
   * Marker event that indicates another page of components should be indexed.
   */
  private static class FlushEvent
  {
    // this event is a marker only
  }

  /**
   * Asynchronous receiver of {@link FlushEvent}s.
   */
  private class FlushEventReceiver
      implements EventAware.Asynchronous
  {
    @AllowConcurrentEvents
    @Subscribe
    public void on(final FlushEvent event) {
      indexPageOfComponents();
    }
  }

  /**
   * Used by scheduled flush task to only attempt indexing when there are pending components.
   */
  void pollPendingComponents() {
    if (pendingCount.get() > 0) {
      indexPageOfComponents();
    }
  }

  /**
   * Grabs a page of components and sends them to be indexed.
   */
  void indexPageOfComponents() {

    // remove page and invert it to get mapping from repository to components
    Multimap<String, EntityId> requestsByRepository = ArrayListMultimap.create();
    Iterator<Entry<String, String>> itr = pendingRequests.entrySet().iterator();
    for (int i = 0; i < flushOnCount && itr.hasNext(); i++) {
      Entry<String, String> entry = itr.next();
      // these requests are scoped per-repository so it is safe to drop the format here
      requestsByRepository.put(entry.getValue(), componentId(entry.getKey()));
      itr.remove();
    }

    // deliver index/purge requests to the relevant repositories
    requestsByRepository.asMap().forEach(
        (repoTag, componentIds) -> ofNullable(repositoryManager.get(repoTag.substring(TAG_LENGTH))).ifPresent(
            repository -> repository.optionalFacet(SearchFacet.class).ifPresent(
                searchFacet -> {
                  if (repoTag.startsWith(INDEX_TAG)) {
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
  private String requestKey(final String format, final int componentId) {
    return format + ':' + componentId;
  }

  /**
   * Extracts the external component id from the request key; this should only be done in the context of a repository/format.
   */
  private EntityId componentId(final String requestKey) {
    return toExternalId(Integer.parseInt(requestKey.substring(requestKey.indexOf(':') + 1)));
  }
}
