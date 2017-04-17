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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityBatchEvent;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
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
    extends ComponentSupport
    implements EventAware, Asynchronous
{
  private final RepositoryManager repositoryManager;

  @Inject
  public IndexRequestProcessor(final RepositoryManager repositoryManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final EntityBatchEvent batchEvent) {
    process(new IndexBatchRequest(batchEvent));
  }

  public void process(final IndexBatchRequest request) {
    request.forEach(this::maybeUpdateSearchIndex);
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

  private static void doUpdateSearchIndex(final Repository repository, final IndexRequest indexRequest) {
    final SearchFacet searchFacet = repository.facet(SearchFacet.class);
    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      indexRequest.apply(searchFacet);
    }
    finally {
      UnitOfWork.end();
    }
  }
}
