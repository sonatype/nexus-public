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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.store.ContentStoreEvent;
import org.sonatype.nexus.scheduling.PeriodicJobService;
import org.sonatype.nexus.scheduling.PeriodicJobService.PeriodicJob;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalAssetId;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;

/**
 * Event handler that periodically updates the browse tree based on asset events.
 *
 * This handler runs in the same thread that triggered the event. It avoids doing too much
 * work on that thread by simply marking the asset update as pending in a concurrent map.
 * A scheduled task periodically grabs a page of assets and updates the tree.
 *
 * If the pending map gets too big events will start to be posted to flush additional pages.
 * These events are handled by an asynchronous receiver using threads from the event pool.
 *
 * Trimming of dangling nodes from the browse tree is only done by the scheduled task.
 *
 * @since 3.26
 */
@ManagedLifecycle(phase = SERVICES)
@Named
@Singleton
public class BrowseEventHandler
    extends LifecycleSupport
    implements EventAware
{
  private final PeriodicJobService periodicJobService;

  private final EventManager eventManager;

  private final int flushOnCount;

  private final int flushOnSeconds;

  private final FlushEventReceiver flushEventReceiver = new FlushEventReceiver();

  private final Map<String, Repository> pendingAssets = new ConcurrentHashMap<>();

  private final AtomicInteger pendingCount = new AtomicInteger();

  private final Set<Repository> repositoriesToTrim = ConcurrentHashMap.newKeySet();

  private final AtomicBoolean needsTrim = new AtomicBoolean();

  private PeriodicJob flushTask;

  @Inject
  public BrowseEventHandler(
      final PeriodicJobService periodicJobService,
      final EventManager eventManager,
      @Named("${nexus.browse.event.handler.flushOnCount:-100}") final int flushOnCount,
      @Named("${nexus.browse.event.handler.flushOnSeconds:-2}") final int flushOnSeconds)
  {
    this.periodicJobService = checkNotNull(periodicJobService);
    this.eventManager = checkNotNull(eventManager);
    this.flushOnCount = flushOnCount;
    this.flushOnSeconds = flushOnSeconds;

    eventManager.register(flushEventReceiver);
  }

  @Override
  protected void doStart() throws Exception {
    periodicJobService.startUsing();

    flushTask = periodicJobService.schedule(this::pollBrowseTreeChanges, flushOnSeconds);
  }

  @Override
  protected void doStop() throws Exception {
    flushTask.cancel();

    periodicJobService.stopUsing();
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetCreatedEvent event) {
    markAssetAsPending(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetUploadedEvent event) {
    markAssetAsPending(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentDeletedEvent event) {
    markRepositoryForTrimming(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentPurgedEvent event) {
    markRepositoryForTrimming(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetDeletedEvent event) {
    markRepositoryForTrimming(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetPurgedEvent event) {
    markRepositoryForTrimming(event);
  }

  /**
   * Marks asset as requiring updating in its browse tree.
   */
  private void markAssetAsPending(final AssetEvent event) {

    // bump count if this is the first time we've seen this asset in this batch
    if (pendingAssets.put(requestKey(event), event.getRepository()) == null) {
      pendingCount.getAndIncrement();
    }

    // if we're over the limit then reduce it by a page and trigger an asynchronous flush event
    if (pendingCount.getAndUpdate(c -> c >= flushOnCount ? c - flushOnCount : c) >= flushOnCount) {
      eventManager.post(new FlushEvent());
    }
  }

  /**
   * Marks repository as requiring trimming of its browse tree.
   */
  private void markRepositoryForTrimming(final ContentStoreEvent event) {
    repositoriesToTrim.add(event.getRepository());
    needsTrim.set(true);
  }

  /**
   * Marker event that indicates another page of assets should be indexed.
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
      updatePageOfAssets();
    }
  }

  /**
   * Used by scheduled flush task to only attempt updates when there are pending changes.
   */
  void pollBrowseTreeChanges() {
    if (pendingCount.get() > 0) {
      updatePageOfAssets();
    }
    if (needsTrim.getAndSet(false)) {
      trimRepositories();
    }
  }

  /**
   * Grabs a page of assets and updates the browse tree.
   */
  void updatePageOfAssets() {

    // remove page of assets and invert it to get mapping from repository to assets
    Multimap<Repository, EntityId> requestsByRepository = ArrayListMultimap.create();
    Iterator<Entry<String, Repository>> itr = pendingAssets.entrySet().iterator();
    for (int i = 0; i < flushOnCount && itr.hasNext(); i++) {
      Entry<String, Repository> entry = itr.next();
      // these requests are scoped per-repository so it is safe to drop the format here
      requestsByRepository.put(entry.getValue(), assetId(entry.getKey()));
      itr.remove();
    }

    // deliver requests to the relevant repositories
    requestsByRepository.asMap().forEach(
        (repository, assetIds) -> repository.optionalFacet(BrowseFacet.class).ifPresent(
            browseFacet -> browseFacet.addPathsToAssets(assetIds)));
  }

  /**
   * Trims all pending repositories of dangling nodes.
   */
  void trimRepositories() {
    Iterator<Repository> itr = repositoriesToTrim.iterator();
    while (itr.hasNext()) {
      itr.next().optionalFacet(BrowseFacet.class).ifPresent(BrowseFacet::trimBrowseNodes);
      itr.remove();
    }
  }

  /**
   * Binds the format with the asset id to get a unique request key.
   */
  private String requestKey(final AssetEvent event) {
    return event.getFormat() + ':' + internalAssetId(event.getAsset());
  }

  /**
   * Extracts external asset id from request key; should only be done in the context of a repository/format.
   */
  private EntityId assetId(final String requestKey) {
    return toExternalId(Integer.parseInt(requestKey.substring(requestKey.indexOf(':') + 1)));
  }
}
