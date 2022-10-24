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
package org.sonatype.nexus.repository.content.search.table;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.content.event.component.ComponentCreatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryDeletedEvent;
import org.sonatype.nexus.repository.content.search.SearchEventHandler;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.PeriodicJobService;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_TABLE_SEARCH;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.repository.content.store.InternalIds.contentRepositoryId;

/**
 * <ul>
 *   <li>Deletes all search indexes for a given content repository in response to a ContentRepositoryDeletedEvent</li>
 *   <li>Skips ComponentCreatedEvents</li>
 * </ul>
 */
@ManagedLifecycle(phase = SERVICES)
@Named
@Singleton
@FeatureFlag(name = DATASTORE_TABLE_SEARCH)
public class SqlSearchEventHandler
    extends SearchEventHandler
    implements EventAware
{
  private final SearchTableStore searchTableStore;

  @Inject
  public SqlSearchEventHandler(
      final SearchTableStore searchTableStore,
      final RepositoryManager repositoryManager,
      final PeriodicJobService periodicJobService,
      @Named("${" + FLUSH_ON_COUNT_KEY + ":-100}") final int flushOnCount,
      @Named("${" + FLUSH_ON_SECONDS_KEY + ":-2}") final int flushOnSeconds,
      @Named("${" + NO_PURGE_DELAY_KEY + ":-true}") final boolean noPurgeDelay,
      @Named("${" + FLUSH_POOL_SIZE + ":-128}") final int poolSize)
  {
    super(repositoryManager, periodicJobService, flushOnCount, flushOnSeconds, noPurgeDelay, poolSize);
    this.searchTableStore = checkNotNull(searchTableStore);
  }

  @Override
  protected String getThreadPoolId() {
    return "sqlSearchEventHandler";
  }

  @Override
  protected void requestIndex(final ComponentEvent event) {
    if (event instanceof ComponentCreatedEvent) {
      log.debug("Skipping component created event for {}:{}:{}",
          event.getComponent().namespace(),
          event.getComponent().name(),
          event.getComponent().version());
      return;
    }
    super.requestIndex(event);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ContentRepositoryDeletedEvent event) {
    //As we are operating on a content repository basis rather than components, we can schedule immediately
    threadPoolExecutor.execute(() -> deleteRepositoryComponents(event));
  }

  private boolean deleteRepositoryComponents(final ContentRepositoryDeletedEvent event) {
    int repositoryId = contentRepositoryId(event);
    String format = event.getFormat();
    log.debug("Deleting repository id: {}, format: {} from component_search", repositoryId, format);

    //Delete all records for repository - we don't need to use cooperation.
    //Note: In the SearchTableDao.save SQL we use an INSERT FROM SELECT and EXISTS check via the format_component table
    // to check that a component still exists before saving the record. Thus, if we have a scenario where we have
    // already purged all component search records associated with this repository and another node then
    // processes a delayed re-indexing of a component in the component search table
    // (e.g. in response to a AssetEvent, ComponentEvent which occurred before the ContentRepositoryDeletedEvent), that
    //update will, as expected, not re-insert the record because the component no longer exists.
    return searchTableStore.deleteAllForRepository(repositoryId, format);
  }
}
