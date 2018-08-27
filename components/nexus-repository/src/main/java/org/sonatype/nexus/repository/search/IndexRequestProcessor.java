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
package org.sonatype.nexus.repository.search;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.entity.EntityBatchEvent;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;
import static org.sonatype.nexus.repository.FacetSupport.State.DELETED;
import static org.sonatype.nexus.repository.FacetSupport.State.DESTROYED;
import static org.sonatype.nexus.repository.FacetSupport.State.STOPPED;

/**
 * Async processor of {@link IndexBatchRequest}s, which are used to trigger search updates.
 *
 * @since 3.0
 */
@Named
@Singleton
public class IndexRequestProcessor
    extends LifecycleSupport
    implements Asynchronous
{
  private final RepositoryManager repositoryManager;

  private final EventManager eventManager;

  private final SearchService searchService;

  private final boolean bulkProcessing;

  private boolean processEvents = true;

  @Inject
  public IndexRequestProcessor(final RepositoryManager repositoryManager,
                               final EventManager eventManager,
                               final SearchService searchService,
                               @Named("${nexus.elasticsearch.bulkProcessing:-true}") final boolean bulkProcessing)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.eventManager = checkNotNull(eventManager);
    this.searchService = checkNotNull(searchService);
    this.bulkProcessing = bulkProcessing;
  }

  @Override
  protected void doStart() {
    eventManager.register(this);
  }

  @Override
  protected void doStop() {
    eventManager.unregister(this);
    searchService.flush(true);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final EntityBatchEvent batchEvent) {
    process(new IndexBatchRequest(batchEvent));
  }

  public void process(final IndexBatchRequest request) {
    if (!processEvents) {
      if (log.isTraceEnabled()) {
        log.trace("Skip processing of EntityBatchEvent, IndexRequestProcessor is disabled");
      }
      return;
    }

    Set<EntityId> pendingDeletes = request.apply(this::maybeUpdateSearchIndex);
    if (!pendingDeletes.isEmpty()) {
      // IndexSyncService can request deletes that have no associated repository,
      // in which case we need to attempt a special bulk delete as the last step
      searchService.bulkDelete(null, transform(pendingDeletes, EntityId::getValue));
    }
  }

  public void setProcessEvents(final boolean processEvents) {
    this.processEvents = processEvents;
  }

  private void maybeUpdateSearchIndex(final String repositoryName, final IndexRequest indexRequest) {
    final Repository repository = repositoryManager.get(repositoryName);
    if (repository != null) {
      try {
        doUpdateSearchIndex(repository, indexRequest);
      }
      catch (InvalidStateException e) {
        switch (e.getInvalidState()) {
          case STOPPED:
          case DELETED:
          case DESTROYED:
            log.debug("Ignoring async search update for {} repository {}", e.getInvalidState(), repositoryName, e);
            break;
          default:
            throw e;
        }
      }
    }
    else {
      log.debug("Ignoring async search update for missing repository {}", repositoryName);
    }
  }

  private void doUpdateSearchIndex(final Repository repository, final IndexRequest indexRequest) {
    repository.optionalFacet(SearchFacet.class).ifPresent(searchFacet -> {
      UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
      try {
        if (bulkProcessing) {
          indexRequest.bulkApply(searchFacet);
        }
        else {
          indexRequest.apply(searchFacet);
        }
      }
      finally {
        UnitOfWork.end();
      }
    });
  }
}
