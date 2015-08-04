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

import org.sonatype.nexus.events.Event;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.events.RepositoryConfigurationUpdatedEvent;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.events.RepositoryRegistryRepositoryEvent;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Listens for events and manages IndexerManager by adding and removing indexing contexts.
 * <p>
 * This EventInspector component HAS TO BE sync!
 *
 * @author Toni Menzel
 * @author cstamas
 */
@Named
@Singleton
public class IndexingRepositoryRegistryRepositoryEventInspector
    extends ComponentSupport
    implements EventSubscriber
{
  private final IndexerManager indexerManager;

  private final RepositoryRegistry repoRegistry;

  @Inject
  public IndexingRepositoryRegistryRepositoryEventInspector(final IndexerManager indexerManager,
                                                            final RepositoryRegistry repoRegistry)
  {
    this.indexerManager = indexerManager;
    this.repoRegistry = repoRegistry;
  }

  protected IndexerManager getIndexerManager() {
    return indexerManager;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryRegistryRepositoryEvent evt) {
    inspect(evt);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryConfigurationUpdatedEvent evt) {
    inspect(evt);
  }

  protected void inspect(final Event<?> evt) {
    Repository repository = null;
    if (evt instanceof RepositoryRegistryRepositoryEvent) {
      repository = ((RepositoryRegistryRepositoryEvent) evt).getRepository();
    }
    else if (evt instanceof RepositoryConfigurationUpdatedEvent) {
      repository = ((RepositoryConfigurationUpdatedEvent) evt).getRepository();
    }

    try {
      // check registry for existence, wont be able to do much
      // if doesn't exist yet
      repoRegistry.getRepositoryWithFacet(repository.getId(), MavenRepository.class);
      inspectForIndexerManager(evt, repository);
    }
    catch (NoSuchRepositoryException e) {
      log.debug("Attempted to handle repository that isn't yet in registry");
    }
  }

  private void inspectForIndexerManager(Event<?> evt, Repository repository) {
    try {
      // we are handling repo events, like addition and removal
      if (evt instanceof RepositoryRegistryEventAdd) {
        getIndexerManager().addRepositoryIndexContext(repository.getId());
      }
      else if (evt instanceof RepositoryRegistryEventRemove) {
        getIndexerManager().removeRepositoryIndexContext(
            ((RepositoryRegistryEventRemove) evt).getRepository().getId(), true);
      }
      else if (evt instanceof RepositoryConfigurationUpdatedEvent) {
        getIndexerManager().updateRepositoryIndexContext(repository.getId());
      }
    }
    catch (Exception e) {
      log.error("Could not maintain indexing contexts!", e);
    }
  }
}