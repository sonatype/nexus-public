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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Integer.parseInt;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
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
 * @since 3.26
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@ManagedLifecycle(phase = SERVICES)
@Named
@Singleton
public class BrowseEventHandler
    extends LifecycleSupport
    implements EventAware // warning: don't make this EventAware.Asynchronous
{
  private static final String HANDLER_KEY_PREFIX = "nexus.browse.event.handler.";

  private static final String FLUSH_ON_COUNT_KEY = HANDLER_KEY_PREFIX + "flushOnCount";

  private static final String FLUSH_ON_SECONDS_KEY = HANDLER_KEY_PREFIX + "flushOnSeconds";

  private static final String NO_PURGE_DELAY_KEY = HANDLER_KEY_PREFIX + "noPurgeDelay";

  private final PeriodicJobService periodicJobService;

  private final EventManager eventManager;

  private final int flushOnCount;

  private final int flushOnSeconds;

  private final boolean noPurgeDelay;

  private final FlushEventReceiver flushEventReceiver = new FlushEventReceiver();

  private final Map<String, Repository> pendingAssets = new ConcurrentHashMap<>();

  private final AtomicInteger pendingCount = new AtomicInteger();

  private final Set<Repository> repositoriesToTrim = ConcurrentHashMap.newKeySet();

  private final AtomicBoolean needsTrim = new AtomicBoolean();

  private Object flushMutex = new Object();

  private PeriodicJob flushTask;

  @Inject
  public BrowseEventHandler(
      final PeriodicJobService periodicJobService,
      final EventManager eventManager,
      @Named("${" + FLUSH_ON_COUNT_KEY + ":-100}") final int flushOnCount,
      @Named("${" + FLUSH_ON_SECONDS_KEY + ":-2}") final int flushOnSeconds,
      @Named("${" + NO_PURGE_DELAY_KEY + ":-true}") final boolean noPurgeDelay)
  {
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
      flushTask = periodicJobService.schedule(this::pollBrowseUpdateRequests, flushOnSeconds);
    }
  }

  @Override
  protected void doStop() throws Exception {
    if (flushOnCount > 1) {
      flushTask.cancel();
      periodicJobService.stopUsing();
    }
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
  public void on(final AssetDeletedEvent event) {
    markRepositoryForTrimming(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetPurgedEvent event) {
    markRepositoryForTrimming(event);
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

  /**
   * Marks asset as requiring updating in its browse tree.
   */
  private void markAssetAsPending(final AssetEvent event) {
    Optional<Repository> repository = event.getRepository();
    if (!repository.isPresent()) {
      log.debug("Missing repository for event {}", event);
      return;
    }
    // bump count if this is the first time we've seen this request key in this batch
    if (pendingAssets.put(requestKey(event), repository.get()) == null) {
      pendingCount.getAndIncrement();
    }

    // if there are lots of pending requests then reduce count by a page and
    // trigger an asynchronous flush event (which will actually do the work)
    if (pendingCount.getAndUpdate(c -> c >= flushOnCount ? c - flushOnCount : c) >= flushOnCount) {
      eventManager.post(new FlushEvent());
    }
  }

  /**
   * Marks repository as requiring trimming of its browse tree.
   */
  private void markRepositoryForTrimming(final ContentStoreEvent event) {
    Optional<Repository> repository = event.getRepository();
    if (!repository.isPresent()) {
      log.debug("Unable to determine repository for trimming for event {}", event);
      return;
    }
    repositoriesToTrim.add(repository.get());
    needsTrim.set(true);

    if (noPurgeDelay) {
      eventManager.post(new PurgeEvent());
    }
  }

  /**
   * Marker event that indicates another page of components should be added to the browse tree.
   */
  private static class FlushEvent
  {
    // this event is a marker only
  }

  /**
   * Marker event that indicates some repository browse trees need trimming of unused nodes.
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
      flushPageOfAssets();
    }

    @AllowConcurrentEvents
    @Subscribe
    public void on(final PurgeEvent event) {
      maybeTrimRepositories();
    }
  }

  /**
   * Used by scheduled flush task to poll for work.
   */
  void pollBrowseUpdateRequests() {
    if (pendingCount.get() > 0) {
      flushPageOfAssets();
    }

    maybeTrimRepositories();
  }

  /**
   * Grabs a page of assets and updates the browse tree.
   */
  void flushPageOfAssets() {
    Multimap<Repository, EntityId> requestsByRepository = ArrayListMultimap.create();

    // only allow one thread to remove entries at a time while still allowing other threads to add entries
    synchronized (flushMutex) {

      // remove page of assets and invert it to get mapping from repository to assets
      Iterator<Entry<String, Repository>> itr = pendingAssets.entrySet().iterator();
      for (int i = 0; i < flushOnCount && itr.hasNext(); i++) {
        Entry<String, Repository> entry = itr.next();

        // these requests are scoped per-repository so it is safe to drop the format here
        requestsByRepository.put(entry.getValue(), assetId(entry.getKey()));
        itr.remove();
      }
    }

    // deliver requests to the relevant repositories
    requestsByRepository.asMap().forEach(
        (repository, assetIds) -> repository.optionalFacet(BrowseFacet.class).ifPresent(
            browseFacet -> browseFacet.addPathsToAssets(assetIds)));
  }

  /**
   * Trims all pending repositories of dangling nodes.
   */
  void maybeTrimRepositories() {
    if (needsTrim.getAndSet(false)) {

      // only allow one thread to remove entries at a time while still allowing other threads to add entries
      synchronized (flushMutex) {

        Iterator<Repository> itr = repositoriesToTrim.iterator();
        while (itr.hasNext()) {
          Repository nextRepository = itr.next();
          itr.remove(); //do the removal first so other threads can add the same repository to the set to trim
          nextRepository.optionalFacet(BrowseFacet.class).ifPresent(BrowseFacet::trimBrowseNodes);
        }
      }
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
    return toExternalId(parseInt(requestKey.substring(requestKey.indexOf(':') + 1)));
  }
}
