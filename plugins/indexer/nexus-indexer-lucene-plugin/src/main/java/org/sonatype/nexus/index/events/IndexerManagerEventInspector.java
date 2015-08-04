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
package org.sonatype.nexus.index.events;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.Asynchronous;
import org.sonatype.nexus.events.Event;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.proxy.events.RepositoryItemEvent;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCache;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDelete;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStore;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Event inspector that maintains indexes.
 *
 * @author cstamas
 */
@Named
@Singleton
public class IndexerManagerEventInspector
    extends ComponentSupport
    implements EventSubscriber, Asynchronous
{
  private final boolean enabled =
      SystemPropertiesHelper.getBoolean("org.sonatype.nexus.events.IndexerManagerEventInspector.enabled", true);

  private final IndexerManager indexerManager;

  @Inject
  public IndexerManagerEventInspector(final IndexerManager indexerManager) {
    this.indexerManager = indexerManager;
  }

  protected IndexerManager getIndexerManager() {
    return indexerManager;
  }

  // listen for STORE, CACHE, DELETE only

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryItemEventCache evt) {
    if (enabled) {
      inspectForIndexerManager(evt);
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryItemEventStore evt) {
    if (enabled) {
      inspectForIndexerManager(evt);
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryItemEventDelete evt) {
    if (enabled) {
      inspectForIndexerManager(evt);
    }
  }

  private void inspectForIndexerManager(final Event<?> evt) {
    RepositoryItemEvent ievt = (RepositoryItemEvent) evt;

    Repository repository = ievt.getRepository();

    // should we sync at all
    if (repository != null && repository.isIndexable()) {
      try {
        if (ievt instanceof RepositoryItemEventCache || ievt instanceof RepositoryItemEventStore) {
          getIndexerManager().addItemToIndex(repository, ievt.getItem());
        }
        else if (ievt instanceof RepositoryItemEventDelete) {
          getIndexerManager().removeItemFromIndex(repository, ievt.getItem());
        }
      }
      catch (Exception e) // TODO be more specific
      {
        log.error("Could not maintain index for repository {}!", repository.getId(), e);
      }
    }
  }

}
